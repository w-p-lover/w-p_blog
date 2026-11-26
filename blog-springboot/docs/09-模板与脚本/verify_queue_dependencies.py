"""Opt-in Q4 dependency checks. Synthetic data only; never starts the application.

Requires PyMySQL and pika (also looks in target/q4-python). Credentials stay in
the existing local configuration. No network access without --execute and an
explicit --expect-host. Creates and finally removes a unique database, vhost
and Qdrant collection; it never consumes existing queues or copies business rows.
"""
import argparse
import base64
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from threading import Event
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "target/q4-python"))


def config():
    values = {}
    for line in (ROOT / ".env").read_text(encoding="utf-8-sig").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip().strip("\"'")
    # Only the known credential overrides; do not print this file or its values.
    private = ROOT / "config/application-private.yml"
    if private.exists():
        text = private.read_text(encoding="utf-8-sig")
        for section, prefix in [("datasource", "BLOG_DB"), ("rabbitmq", "BLOG_RABBITMQ")]:
            match = re.search(r"(?m)^  " + section + r":\s*\n((?:[ \t]+.*\n|\s*\n)*)", text)
            for field in ("username", "password"):
                entry = re.search(r"(?m)^    " + field + r":\s*(.*)$", match[1]) if match else None
                if entry:
                    value = entry[1].strip().strip("\"'")
                    if value.startswith("${") and value.endswith("}"):
                        key, _, default = value[2:-1].partition(":")
                        value = values.get(key, default)
                    if value.startswith("ENC("):
                        raise ValueError("Encrypted credential requires an explicit local decryption setup")
                    values[prefix + "_" + field.upper()] = value
    return values


def request(url, method="GET", body=None, auth=None):
    headers = {"Content-Type": "application/json"}
    if auth:
        headers["Authorization"] = "Basic " + base64.b64encode(":".join(auth).encode()).decode()
    req = urllib.request.Request(url, data=None if body is None else json.dumps(body).encode(),
                                 method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=8) as response:
        data = response.read()
        return json.loads(data) if data else None


def mapper_sql(file, name):
    node = next(n for n in ET.parse(ROOT / "src/main/resources/mapper" / file).getroot()
                if n.get("id") == name)
    # These are static mapper statements; preserve MySQL dialect and predicates.
    sql = " ".join("".join(node.itertext()).split())
    return re.sub(r"#\{([\w.]+)\}", lambda m: "%(" + m[1] + ")s", sql)


class Checks:
    def __init__(self, values, host):
        import pymysql
        self.pymysql = pymysql
        self.values = values
        self.address = urllib.parse.urlparse(values["BLOG_DB_URL"].removeprefix("jdbc:"))
        if any(value != host for value in [self.address.hostname, values["BLOG_RABBITMQ_HOST"],
                                           values["BLOG_QDRANT_HOST"]]):
            raise ValueError("Configured dependency hosts do not match --expect-host")
        self.name = "blog_q4_" + uuid.uuid4().hex
        self.db_created = self.vhost_created = self.collection_created = False
        self.rabbit = "http://" + host + ":15672"
        self.qdrant = "http://" + host + ":6333/collections/" + self.name
        self.auth = (values["BLOG_RABBITMQ_USERNAME"], values["BLOG_RABBITMQ_PASSWORD"])
        self.report = {"run": self.name, "started": datetime.now().isoformat(),
                       "host": host, "checks": [], "cleanup": {}}

    def connect(self, database=None):
        db = self.pymysql.connect(host=self.address.hostname, port=self.address.port or 3306,
                                  user=self.values["BLOG_DB_USERNAME"], password=self.values["BLOG_DB_PASSWORD"],
                                  database=database or self.name, charset="utf8mb4", autocommit=True,
                                  connect_timeout=5, read_timeout=10, write_timeout=10)
        with db.cursor() as cursor:
            cursor.execute("SET SESSION innodb_lock_wait_timeout=3")
        return db

    @staticmethod
    def execute(db, sql, args=None):
        with db.cursor() as cursor:
            cursor.execute(sql, args)
            return cursor.fetchall() if cursor.description else cursor.rowcount

    def sql(self, db, name, **args):
        file = "ArticleMapper.xml" if name == "updateArticleAiResult" else "AiTaskMapper.xml"
        return self.execute(db, mapper_sql(file, name), args)

    def passed(self, name, **evidence):
        self.report["checks"].append({"name": name, **evidence})
        print("PASS:", name, flush=True)

    def create_database(self):
        with closing(self.connect(self.address.path.lstrip("/"))) as db:
            self.report["mysql"] = self.execute(db, "SELECT VERSION(),@@tx_isolation")[0]
            self.execute(db, "CREATE DATABASE `" + self.name + "` CHARACTER SET utf8mb4")
            self.db_created = True
            # Structure only, no INSERT SELECT or reads of business rows.
            for table in ("t_ai_task", "t_article"):
                self.execute(db, "CREATE TABLE `" + self.name + "`.`" + table + "` LIKE `"
                             + self.address.path.lstrip("/") + "`.`" + table + "`")
        with closing(self.connect()) as db:
            self.execute(db, "INSERT INTO t_article(id,user_id,category_id,article_cover,article_title,"
                         "article_content,create_time,status,is_delete,source_version,content_version) "
                         "VALUES(1,1,1,'synthetic','Q4 synthetic','Synthetic body',NOW(),1,0,'source-v1',UUID())")

    def seed(self, db, task_id, status="PENDING", checkpoint=True, failures=0):
        payload = json.dumps(dict(sourceVersion="source-v1", summary="synthetic summary", tags="synthetic"))
        self.execute(db, "INSERT INTO t_ai_task(id,biz_type,biz_id,task_type,status,source_version,"
                     "generated_payload,retry_count,next_dispatch_at,execution_token) "
                     "VALUES(%s,'ARTICLE',1,'ARTICLE_SUMMARY_TAG_VECTOR',%s,'source-v1',%s,%s,"
                     "TIMESTAMPADD(SECOND,-60,NOW(6)),'old-token')",
                     (task_id, status, payload if checkpoint else None, failures))

    def publish(self, db, task_id, token):
        return self.sql(db, "updateArticleAiResult", articleId=1, sourceVersion="source-v1",
                        taskId=task_id, token=token, aiSummary="Q4 published", aiSuggestTags="synthetic",
                        aiIndexedAt=datetime.now())

    def complete(self, db, task_id, token):
        return self.sql(db, "completeExecution", **{"task.id": task_id, "task.status": "SUCCESS",
                        "task.retryCount": 0, "task.resultPayload": "synthetic", "task.errorMessage": None,
                        "task.finishedAt": datetime.now(), "task.costTime": 1, "token": token})

    def observe_lock_wait(self, blocker):
        with closing(self.connect()) as observer:
            deadline = time.monotonic() + 2
            while time.monotonic() < deadline:
                waiting = self.execute(observer, "SELECT COUNT(*) FROM information_schema.INNODB_LOCK_WAITS w "
                                       "JOIN information_schema.INNODB_TRX t ON t.trx_id=w.blocking_trx_id "
                                       "WHERE t.trx_mysql_thread_id=%s", (blocker.thread_id(),))[0][0]
                if waiting:
                    return
                time.sleep(0.05)
        raise AssertionError("Did not observe the scoped task-row lock wait")

    def verify_mysql(self):
        with closing(self.connect()) as db:
            self.seed(db, 1)
            assert self.sql(db, "claimPendingTask", taskId=1, token="publication") == 1
            db.begin()
            assert self.sql(db, "lockExecution", taskId=1, token="publication")
            assert self.sql(db, "renewExecution", taskId=1, token="publication") == 1
            assert self.publish(db, 1, "publication") == 1
            assert self.complete(db, 1, "publication") == 1
            db.rollback()
            assert self.execute(db, "SELECT ai_summary,indexed_source_version FROM t_article WHERE id=1")[0] == (None, None)
            assert self.execute(db, "SELECT status FROM t_ai_task WHERE id=1")[0][0] == "RUNNING"
            self.passed("MySQL publication and terminal rollback together")

            # Publisher already owns the task row; expiry must wait, then see SUCCESS.
            db.begin()
            assert self.sql(db, "lockExecution", taskId=1, token="publication")
            started = Event()
            with ThreadPoolExecutor(max_workers=1) as pool:
                def expire():
                    with closing(self.connect()) as other:
                        started.set()
                        return self.sql(other, "expireExecution", taskId=1)
                result = pool.submit(expire)
                assert started.wait(3)
                # Require the actual InnoDB lock wait, rather than assuming thread scheduling.
                self.observe_lock_wait(db)
                assert self.sql(db, "renewExecution", taskId=1, token="publication") == 1
                assert self.publish(db, 1, "publication") == 1
                assert self.complete(db, 1, "publication") == 1
                db.commit()
                assert result.result(timeout=5) == 0
            self.passed("MySQL expiry waits for publication task-row lock then cannot change SUCCESS",
                        observed_innodb_lock_wait=True)

            self.seed(db, 2, "RUNNING")
            assert self.sql(db, "expireExecution", taskId=2) == 1
            db.begin()
            assert not self.sql(db, "lockExecution", taskId=2, token="old-token")
            assert self.publish(db, 2, "old-token") == 0
            assert self.complete(db, 2, "old-token") == 0
            db.commit()
            self.passed("MySQL expired execution rejects late publication and terminal write")

            # Reverse ordering: uncommitted recovery owns the row first; old finish waits.
            self.seed(db, 3, "RUNNING")
            db.begin()
            assert self.sql(db, "expireExecution", taskId=3) == 1
            ready = Event()
            with ThreadPoolExecutor(max_workers=1) as pool:
                def late_finish():
                    with closing(self.connect()) as other:
                        other.begin()
                        ready.set()
                        row = self.sql(other, "lockExecution", taskId=3, token="old-token")
                        other.rollback()
                        return row
                result = pool.submit(late_finish)
                assert ready.wait(5), "Old finisher connection did not become ready"
                self.observe_lock_wait(db)
                db.commit()
                assert not result.result(timeout=5)
            self.passed("MySQL recovery winner prevents old finisher from reacquiring ownership",
                        observed_innodb_lock_wait=True)
            self.execute(db, "UPDATE t_ai_task SET next_dispatch_at=TIMESTAMPADD(SECOND,-1,NOW(6)) WHERE id=3")
            def resume(_):
                with closing(self.connect()) as other:
                    return self.sql(other, "resumeCheckpointTask", taskId=3)
            with ThreadPoolExecutor(max_workers=2) as pool:
                assert sorted(pool.map(resume, range(2))) == [0, 1]
            self.passed("MySQL concurrent recovery resumes one checkpoint task once")

            self.seed(db, 4, "RUNNING", checkpoint=False)
            self.seed(db, 5, "RUNNING", failures=2)
            assert self.sql(db, "expireExecution", taskId=4) == 1
            assert self.sql(db, "expireExecution", taskId=5) == 1
            assert self.execute(db, "SELECT id,status FROM t_ai_task WHERE id IN (4,5) ORDER BY id") == ((4, "UNKNOWN"), (5, "FAILED"))
            self.passed("MySQL expiry isolates missing checkpoints and exhausted failure budgets")

            # Terminal commit happened, but the caller does not get its outcome.
            with closing(self.connect()) as reader:
                assert self.execute(reader, "SELECT status FROM t_ai_task WHERE id=1")[0][0] == "SUCCESS"
                assert self.sql(reader, "claimPendingTask", taskId=1, token="redelivery") == 0
                assert self.execute(reader, "SELECT ai_summary,indexed_source_version FROM t_article WHERE id=1")[0] == ("Q4 published", "source-v1")
            self.passed("MySQL committed result survives fresh connection and duplicate claim")

            # Reproduce the bounded scan starvation with 100 legacy/ineligible rows first.
            for task_id in range(100, 200):
                self.seed(db, task_id, "RETRYING", checkpoint=task_id % 2 == 0,
                          failures=3 if task_id % 2 == 0 else 1)
            self.seed(db, 200, "RETRYING", failures=1)
            # The expired task above is not due in this scenario.
            self.execute(db, "UPDATE t_ai_task SET next_dispatch_at=TIMESTAMPADD(SECOND,300,NOW(6)) WHERE id=2")
            selected = [row[0] for row in self.sql(db, "selectRecoverableTasks", size=100)]
            self.report["recovery_query_plan"] = self.execute(db, "EXPLAIN " + mapper_sql("AiTaskMapper.xml", "selectRecoverableTasks"), {"size": 100})
            resumed = sum(self.sql(db, "resumeCheckpointTask", taskId=task_id) for task_id in selected)
            self.report["recovery_scan"] = dict(selected=selected, resumed=resumed,
                                                 eligible_task_selected=200 in selected)
            if 200 not in selected:
                print("REPRODUCED: 100 ineligible RETRYING rows starve eligible task 200", flush=True)
            else:
                assert resumed == 1
                self.passed("MySQL bounded recovery scan skips exhausted/missing-checkpoint tasks",
                            selected=len(selected), resumed=resumed)

    def amqp(self):
        import pika
        return pika.BlockingConnection(pika.ConnectionParameters(
            self.values["BLOG_RABBITMQ_HOST"], int(self.values["BLOG_RABBITMQ_PORT"]),
            virtual_host=self.name, credentials=pika.PlainCredentials(*self.auth),
            socket_timeout=5, stack_timeout=10, blocked_connection_timeout=5, heartbeat=10))

    def verify_rabbit(self):
        import pika
        encoded = urllib.parse.quote(self.name, safe="")
        request(self.rabbit + "/api/vhosts/" + encoded, "PUT", {}, self.auth)
        self.vhost_created = True
        request(self.rabbit + "/api/permissions/" + encoded + "/" + urllib.parse.quote(self.auth[0], safe=""),
                "PUT", dict(configure=".*", write=".*", read=".*"), self.auth)
        self.report["rabbitmq_version"] = request(self.rabbit + "/api/overview", auth=self.auth)["rabbitmq_version"]
        with closing(self.amqp()) as conn, closing(self.connect()) as db:
            channel = conn.channel()
            channel.exchange_declare("wake", "direct", durable=True)
            channel.queue_declare("tasks", durable=True)
            channel.confirm_delivery()
            self.seed(db, 300)
            assert self.sql(db, "reserveDispatch", taskId=300) == 1
            try:
                channel.basic_publish("wake", "task", b"300", mandatory=True,
                                      properties=pika.BasicProperties(delivery_mode=2))
                raise AssertionError("Unroutable publish was incorrectly accepted")
            except pika.exceptions.UnroutableError:
                pass
            assert self.execute(db, "SELECT status FROM t_ai_task WHERE id=300")[0][0] == "PENDING"
            channel.queue_bind("tasks", "wake", "task")
            self.execute(db, "UPDATE t_ai_task SET next_dispatch_at=TIMESTAMPADD(SECOND,-1,NOW(6)) WHERE id=300")
            assert (300,) in self.sql(db, "selectDuePendingTasks", size=100)
            assert self.sql(db, "reserveDispatch", taskId=300) == 1
            # Ignore the first successful publish outcome, then resend the same identity.
            for _ in range(2):
                channel.basic_publish("wake", "task", b"300", mandatory=True,
                                      properties=pika.BasicProperties(delivery_mode=2))
            method, _, body = channel.basic_get("tasks", auto_ack=False)
            assert method and body == b"300"
            assert self.sql(db, "claimPendingTask", taskId=300, token="mq-worker") == 1
            # Synthetic processor: no model/vector request. Commit, then close without ACK.
            db.begin()
            assert self.sql(db, "lockExecution", taskId=300, token="mq-worker")
            assert self.sql(db, "renewExecution", taskId=300, token="mq-worker") == 1
            assert self.publish(db, 300, "mq-worker") == 1
            assert self.complete(db, 300, "mq-worker") == 1
            db.commit()
        with closing(self.amqp()) as conn, closing(self.connect()) as db:
            channel = conn.channel()
            redelivered = 0
            for _ in range(2):
                method, _, body = channel.basic_get("tasks", auto_ack=False)
                assert method and body == b"300"
                redelivered += int(method.redelivered)
                assert self.sql(db, "claimPendingTask", taskId=300, token="duplicate") == 0
                channel.basic_ack(method.delivery_tag)
            assert redelivered >= 1
            assert channel.queue_declare("tasks", passive=True).method.message_count == 0
        self.passed("RabbitMQ mandatory return leaves durable pending; repaired route supports resend",
                    persistent_messages=2)
        self.passed("RabbitMQ no-ACK disconnect redelivers committed task without second claim",
                    redelivered=redelivered, duplicate_claims=0)

    def verify_qdrant(self):
        request(self.qdrant, "PUT", dict(vectors=dict(size=2, distance="Cosine")))
        self.collection_created = True
        def points(source):
            result = []
            for index in range(13):
                # Java UUID.nameUUIDFromBytes uses MD5 with UUID v3 bits, without namespace.
                identity = f"article-chunks-v1:1:{source}:{index}".encode()
                point_id = str(uuid.UUID(bytes=hashlib.md5(identity).digest(), version=3))
                result.append(dict(id=point_id, vector=[1.0, 0.1],
                                   payload=dict(articleId=1, sourceVersion=source, chunkIndex=index)))
            return result
        old, new = points("source-v1"), points("source-v2")
        request(self.qdrant + "/points?wait=true", "PUT", dict(points=old[:10]))
        # Controlled interruption before batch 2: publication must not yet happen.
        count = request(self.qdrant + "/points/count", "POST", dict(exact=True))["result"]["count"]
        assert count == 10
        # Write succeeds but its result is deliberately not used; retry both batches.
        request(self.qdrant + "/points?wait=true", "PUT", dict(points=old[10:]))
        for batch in (old[:10], old[10:]):
            request(self.qdrant + "/points?wait=true", "PUT", dict(points=batch))
        assert request(self.qdrant + "/points/count", "POST", dict(exact=True))["result"]["count"] == 13
        request(self.qdrant + "/points?wait=true", "PUT", dict(points=new))
        request(self.qdrant + "/points?wait=true", "PUT", dict(points=old))  # late old network write
        rows = request(self.qdrant + "/points/scroll", "POST", dict(limit=50, with_payload=True))["result"]["points"]
        assert len(rows) == 26
        assert sum(p["payload"]["sourceVersion"] == "source-v2" for p in rows) == 13
        self.passed("Qdrant stable source-specific point IDs survive partial and ignored-response replay",
                    partial_count=count, replay_count=13, two_source_count=len(rows))
        # This is storage evidence only. The actual RAG SQL/permission gates remain separate.

    def verify_java(self):
        maven = shutil.which("mvn")
        if not maven:
            raise ValueError("--java requires mvn on PATH and a compatible JAVA_HOME")
        env = os.environ.copy()
        env.update({
            "BLOG_Q4_SCHEMA": self.name,
            "BLOG_Q4_JDBC_URL": f"jdbc:mysql://{self.address.hostname}:{self.address.port or 3306}/{self.name}"
                                "?useSSL=false&serverTimezone=Asia%2FShanghai&connectTimeout=5000&socketTimeout=10000",
            "BLOG_Q4_DB_USER": self.values["BLOG_DB_USERNAME"],
            "BLOG_Q4_DB_PASSWORD": self.values["BLOG_DB_PASSWORD"],
            "BLOG_Q4_MQ_HOST": self.values["BLOG_RABBITMQ_HOST"],
            "BLOG_Q4_MQ_PORT": self.values["BLOG_RABBITMQ_PORT"],
            "BLOG_Q4_MQ_USER": self.auth[0], "BLOG_Q4_MQ_PASSWORD": self.auth[1],
        })
        folder = ROOT / "logs/queue-acceptance"
        folder.mkdir(parents=True, exist_ok=True)
        log = folder / (self.name + "-java.log")
        with log.open("w", encoding="utf-8") as output:
            result = subprocess.run([maven, "-q", "-Dtest=AiTaskDependencyIntegrationTest", "test"],
                                    cwd=ROOT, env=env, stdout=output, stderr=subprocess.STDOUT, timeout=240)
        self.report["java_log"] = str(log.relative_to(ROOT))
        assert result.returncode == 0, "Java integration failed; inspect the local run log"
        report = ET.parse(ROOT / "target/surefire-reports/TEST-com.ican.service.impl.AiTaskDependencyIntegrationTest.xml").getroot()
        counts = {key: int(report.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
        assert counts == dict(tests=1, failures=0, errors=0, skipped=0), counts
        self.passed("Actual Java transaction proxy, afterCommit dispatch, recovery and dedicated AUTO consumer", **counts)

    def cleanup(self):
        operations = [
            ("qdrant", self.collection_created, lambda: request(self.qdrant, "DELETE")),
            ("rabbit_vhost", self.vhost_created,
             lambda: request(self.rabbit + "/api/vhosts/" + urllib.parse.quote(self.name, safe=""), "DELETE", auth=self.auth)),
        ]
        if self.db_created:
            def drop_database():
                with closing(self.connect("information_schema")) as db:
                    self.execute(db, "DROP DATABASE `" + self.name + "`")
            operations.append(("mysql_database", True, drop_database))
        for name, created, operation in operations:
            if created:
                try:
                    operation()
                    self.report["cleanup"][name] = "deleted"
                except Exception as error:
                    self.report["cleanup"][name] = type(error).__name__
                    print("CLEANUP FAILED:", name, self.name, type(error).__name__, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--expect-host")
    parser.add_argument("--java", action="store_true", help="Also run the opt-in actual Java call-chain check")
    args = parser.parse_args()
    if not args.execute or not args.expect_host:
        parser.error("Requires --execute --expect-host HOST; creates temporary synthetic resources")
    checks = Checks(config(), args.expect_host)
    print("Temporary resource name:", checks.name, flush=True)
    failed = None
    try:
        checks.create_database()
        checks.verify_mysql()
        checks.verify_rabbit()
        checks.verify_qdrant()
        if args.java:
            checks.verify_java()
        if not checks.report["recovery_scan"]["eligible_task_selected"]:
            raise AssertionError("Recovery scan starvation reproduced; eligible task not selected")
    except Exception as error:
        failed = error
        checks.report["error"] = type(error).__name__
        print("FAILED:", type(error).__name__, flush=True)
        # Stack locations only: never dump connection arguments or configuration.
        print("".join(traceback.format_tb(error.__traceback__)), flush=True)
    finally:
        checks.cleanup()
        checks.report["finished"] = datetime.now().isoformat()
        folder = ROOT / "logs/queue-acceptance"
        folder.mkdir(parents=True, exist_ok=True)
        path = folder / (checks.name + ".json")
        path.write_text(json.dumps(checks.report, ensure_ascii=False, indent=2), encoding="utf-8")
        print("Evidence:", path, flush=True)
    if failed or any(v != "deleted" for v in checks.report["cleanup"].values()):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
