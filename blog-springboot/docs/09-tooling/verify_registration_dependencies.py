"""Opt-in activity integration: unique MySQL schema/Rabbit vhost, synthetic rows only.

Uses existing private credential resolution; never starts the full application or
reads business rows. Requires mvn/JAVA_HOME and --execute --expect-host.
"""
import argparse
from contextlib import closing
from datetime import datetime
import json
import os
import re
from pathlib import Path
import shutil
import signal
import time
import subprocess
import urllib.parse
import urllib.error
import uuid
import xml.etree.ElementTree as ET

from verify_queue_dependencies import config, request

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--expect-host", required=True)
    parser.add_argument("--http", action="store_true", help="Real HTTP/auth/Redis/logging activity slice instead of the A3 service protocol checks")
    args = parser.parse_args()
    if not args.execute:
        parser.error("No access without --execute")
    maven = shutil.which("mvn")
    if not maven:
        parser.error("mvn must be on PATH with compatible JAVA_HOME")
    import pymysql
    values = config()
    address = urllib.parse.urlparse(values["BLOG_DB_URL"].removeprefix("jdbc:"))
    if address.hostname != args.expect_host or values["BLOG_RABBITMQ_HOST"] != args.expect_host:
        parser.error("Configured database/broker must match --expect-host")
    schema = "blog_activity_" + uuid.uuid4().hex
    auth = (values["BLOG_RABBITMQ_USERNAME"], values["BLOG_RABBITMQ_PASSWORD"])
    api = "http://" + args.expect_host + ":15672/api/vhosts/" + schema
    redis_client = None
    if args.http:
        import redis
        # Resolve only the existing private Redis password override, without printing credentials.
        private = ROOT / "config/application-private.yml"
        text = private.read_text(encoding="utf-8-sig") if private.exists() else ""
        block = re.search(r"(?m)^    redis:\s*\n((?:[ \t]+.*\n|\s*\n)*)", text)
        entry = re.search(r"(?m)^      password:\s*(.*)$", block[1]) if block else None
        if entry:
            value = entry[1].strip().strip("\"'")
            if value.startswith("${") and value.endswith("}"):
                key, _, default = value[2:-1].partition(":")
                value = values.get(key, default)
            if value.startswith("ENC("):
                raise ValueError("Encrypted Redis credential requires local decryption")
            values["BLOG_REDIS_PASSWORD"] = value
        if values["BLOG_REDIS_HOST"] != args.expect_host:
            parser.error("Configured Redis must match --expect-host")
        redis_client = redis.Redis(host=values["BLOG_REDIS_HOST"], port=int(values["BLOG_REDIS_PORT"]),
                db=int(values["BLOG_REDIS_DATABASE"]), password=values.get("BLOG_REDIS_PASSWORD") or None,
                socket_connect_timeout=5, socket_timeout=5)
    tests = (["com.ican.controller.ActivityRegistrationHttpIntegrationTest"] if args.http else
             ["com.ican.service.impl.ActivityRegistrationServiceTest", "com.ican.service.impl.ActivityRegistrationDependencyIntegrationTest"])
    created_db = created_vhost = False
    report = {"run": schema, "started": datetime.now().isoformat(), "mode": "http" if args.http else "service", "cleanup": {}}
    folder = ROOT / "logs/activity-acceptance"
    folder.mkdir(parents=True, exist_ok=True)
    def connect(database):
        return pymysql.connect(host=address.hostname, port=address.port or 3306,
                user=values["BLOG_DB_USERNAME"], password=values["BLOG_DB_PASSWORD"],
                database=database, charset="utf8mb4", autocommit=True,
                connect_timeout=5, read_timeout=10, write_timeout=10)
    try:
        # First bounded management request, before provisioning any resources.
        request("http://" + args.expect_host + ":15672/api/overview", auth=auth)
        if redis_client:
            assert redis_client.ping()
        with closing(connect(address.path.lstrip("/"))) as db, db.cursor() as cursor:
            cursor.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4")
            created_db = True
            tables = ["t_activity", "t_activity_registration"]
            if args.http:
                tables += ["t_role", "t_user_role", "t_menu", "t_role_menu", "t_operation_log", "t_exception_log"]
            for table in tables:
                cursor.execute("CREATE TABLE `" + schema + "`.`" + table + "` LIKE `"
                               + address.path.lstrip("/") + "`.`" + table + "`")
        request(api, "PUT", {}, auth)
        created_vhost = True
        env = os.environ.copy()
        env.update({"BLOG_ACTIVITY_SCHEMA": schema,
            "BLOG_ACTIVITY_JDBC_URL": "jdbc:" + address._replace(path="/" + schema).geturl(),
            "BLOG_ACTIVITY_DB_USER": values["BLOG_DB_USERNAME"], "BLOG_ACTIVITY_DB_PASSWORD": values["BLOG_DB_PASSWORD"],
            "BLOG_ACTIVITY_MQ_HOST": values["BLOG_RABBITMQ_HOST"], "BLOG_ACTIVITY_MQ_PORT": values["BLOG_RABBITMQ_PORT"],
            "BLOG_ACTIVITY_MQ_USER": auth[0], "BLOG_ACTIVITY_MQ_PASSWORD": auth[1]})
        if args.http:
            env.update({"BLOG_ACTIVITY_REDIS_" + field: values.get("BLOG_REDIS_" + field, "")
                        for field in ("HOST", "PORT", "DATABASE", "PASSWORD")})
        with (folder / (schema + "-java.log")).open("w", encoding="utf-8") as output:
            started = time.time()
            with subprocess.Popen([maven, "-q", "-Dtest=" + ",".join(tests), "test"],
                    cwd=ROOT, env=env, stdout=output, stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0,
                    start_new_session=os.name != "nt") as process:
                try:
                    exit_code = process.wait(timeout=240)
                except BaseException:
                    # Stop only our Maven/test JVM tree before removing its isolated resources.
                    if process.poll() is None:
                        if os.name == "nt":
                            subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"],
                                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=15, check=True)
                        else:
                            os.killpg(process.pid, signal.SIGKILL)
                        process.wait(timeout=15)
                    raise
        report["exitCode"] = exit_code
        report["tests"] = {}
        for name in tests:
            test_report = ROOT / "target/surefire-reports" / ("TEST-" + name + ".xml")
            assert test_report.stat().st_mtime >= started, "Stale JUnit report"
            node = ET.parse(test_report).getroot()
            report["tests"][name] = {key: int(node.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
        assert exit_code == 0, "Inspect isolated Java log"
        assert report["tests"][tests[-1]] == dict(tests=1, failures=0, errors=0, skipped=0)
        report["outcome"] = "PASSED"
    except BaseException as error:
        report["outcome"] = "FAILED"
        report["error"] = repr(error)
        raise
    finally:
        if redis_client:
            try:
                # Only this run's namespace; never FLUSHDB or broad business key deletion.
                for key in redis_client.scan_iter(match=schema + ":*", count=100):
                    assert key.startswith((schema + ":").encode())
                    redis_client.delete(key)
                redis_client.close()
                with redis.Redis(host=values["BLOG_REDIS_HOST"], port=int(values["BLOG_REDIS_PORT"]),
                        db=int(values["BLOG_REDIS_DATABASE"]), password=values.get("BLOG_REDIS_PASSWORD") or None,
                        socket_connect_timeout=5, socket_timeout=5) as check:
                    assert not list(check.scan_iter(match=schema + ":*", count=100))
                report["cleanup"]["redisNamespace"] = "deleted_verified"
            except Exception as error:
                report["cleanup"]["redisNamespace"] = repr(error)
        if created_vhost:
            try:
                request(api, "DELETE", auth=auth)
                # New request must report absence.
                try:
                    request(api, auth=auth)
                except urllib.error.HTTPError as error:
                    if error.code != 404:
                        raise
                else:
                    raise AssertionError("Isolated vhost still exists")
                report["cleanup"]["vhost"] = "deleted_verified"
            except Exception as error:
                report["cleanup"]["vhost"] = repr(error)
        if created_db:
            try:
                with closing(connect("information_schema")) as db, db.cursor() as cursor:
                    cursor.execute("DROP DATABASE `" + schema + "`")
                with closing(connect("information_schema")) as db, db.cursor() as cursor:
                    cursor.execute("SELECT COUNT(*) FROM SCHEMATA WHERE SCHEMA_NAME=%s", (schema,))
                    assert cursor.fetchone()[0] == 0
                report["cleanup"]["database"] = "deleted_verified"
            except Exception as error:
                report["cleanup"]["database"] = repr(error)
        if any(value != "deleted_verified" for value in report["cleanup"].values()):
            report["outcome"] = "FAILED_CLEANUP"
        (folder / (schema + ".json")).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(report, ensure_ascii=False))
        if report["outcome"] == "FAILED_CLEANUP":
            raise RuntimeError("Isolated resource cleanup could not be verified; inspect local report")


if __name__ == "__main__":
    main()
