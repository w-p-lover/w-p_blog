"""Opt-in search recovery acceptance in a new RabbitMQ vhost only.

Runs the actual Java retry/recovery/consumer chain with a controlled ES boundary,
then checks the executable replay client's publish-before-source-ACK failure gap.
Never consumes existing queues or connects to MySQL/Elasticsearch/model services.
"""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.error
import urllib.parse
import uuid
import xml.etree.ElementTree as ET

from verify_queue_dependencies import config, request
from replay_article_search import Audit, TARGET_QUEUE, article_ids, connection_parameters, preflight, replay

ROOT = Path(__file__).resolve().parents[2]


def verify_ack_gap(values, folder, name):
    import pika
    body = b'{"table":"t_article","type":"DELETE","data":[{"id":901}]}'
    connection = pika.BlockingConnection(connection_parameters(values))

    class InterruptedAudit(Audit):
        def record(self, stage, **fields):
            super().record(stage, **fields)
            if stage == "publish_confirmed":
                raise OSError("Controlled interruption before source ACK")

    audit = InterruptedAudit(folder / (name + "-ack-gap.jsonl"))
    try:
        channel = connection.channel()
        channel.confirm_delivery()
        channel.basic_publish(exchange="article.sync.error", routing_key="article.sync.error.key", body=body,
                              properties=pika.BasicProperties(delivery_mode=2), mandatory=True)
        assert preflight(channel) == 1
        try:
            replay(channel, 1, audit)
        except OSError:
            pass
        else:
            raise AssertionError("Expected interruption before source ACK")
    finally:
        connection.close()
        audit.close()
    connection = pika.BlockingConnection(connection_parameters(values))
    audit = Audit(folder / (name + "-ack-gap-recovery.jsonl"))
    try:
        channel = connection.channel()
        assert preflight(channel) == 1
        assert channel.queue_declare(queue=TARGET_QUEUE, passive=True).method.message_count == 1
        assert replay(channel, 1, audit)["source_ack_sent"] == 1
        assert preflight(channel) == 0
        for _ in range(2):
            method, _, received = channel.basic_get(TARGET_QUEUE, auto_ack=False)
            assert method is not None and received == body
            channel.basic_ack(method.delivery_tag)
        assert channel.queue_declare(queue=TARGET_QUEUE, passive=True).method.message_count == 0
    finally:
        connection.close()
        audit.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--expect-host")
    args = parser.parse_args()
    if not args.execute or not args.expect_host:
        parser.error("Requires --execute --expect-host HOST; creates a unique synthetic vhost")
    values = config()
    if values["BLOG_RABBITMQ_HOST"] != args.expect_host:
        parser.error("Configured RabbitMQ host differs from --expect-host")
    maven = shutil.which("mvn")
    if not maven:
        parser.error("Requires mvn on PATH and a compatible JAVA_HOME")
    name = "blog_search_" + uuid.uuid4().hex
    base = "http://" + args.expect_host + ":15672"
    auth = (values["BLOG_RABBITMQ_USERNAME"], values["BLOG_RABBITMQ_PASSWORD"])
    path = base + "/api/vhosts/" + name
    folder = ROOT / "logs/search-replay"
    folder.mkdir(parents=True, exist_ok=True)
    report = dict(run=name, started=datetime.now().isoformat(), host=args.expect_host, checks=[])
    created = False
    failed = False
    stage = "management_identity"
    try:
        report["rabbitmq_version"] = request(base + "/api/overview", auth=auth)["rabbitmq_version"]
        stage = "create_isolated_vhost"
        request(path, "PUT", {}, auth)
        created = True
        request(base + "/api/permissions/" + name + "/" + urllib.parse.quote(auth[0], safe=""), "PUT",
                dict(configure=".*", write=".*", read=".*"), auth)
        env = os.environ.copy()
        env.update({key: str(value) for key, value in values.items() if key.startswith("BLOG_RABBITMQ_")})
        env.update(BLOG_RABBITMQ_VIRTUAL_HOST=name, BLOG_SEARCH_REPLAY_TEST_VHOST=name,
                   BLOG_SEARCH_REPLAY_PYTHON=sys.executable)
        # Only these invalid/safe forms; no need to duplicate the Java consumer suite.
        assert article_ids(b'{"table":"t_article","type":"UPDATE","data":[{"id":"1"}]}') == [1]
        for row in (b'true', b'0', b'1.5', b'2147483648', b'null'):
            try:
                article_ids(b'{"table":"t_article","type":"UPDATE","data":[{"id":' + row + b'}]}')
            except ValueError:
                continue
            raise AssertionError("Invalid ID accepted")
        log = folder / (name + "-java.log")
        stage = "actual_java_search_chain"
        with log.open("w", encoding="utf-8") as output:
            result = subprocess.run([maven, "-q", "-Dtest=ArticleSearchRecoveryIntegrationTest", "test"],
                                    cwd=ROOT, env=env, stdout=output, stderr=subprocess.STDOUT, timeout=240)
        report["java_log"] = str(log.relative_to(ROOT))
        assert result.returncode == 0, "Inspect the local Java log"
        suite = ET.parse(ROOT / "target/surefire-reports/TEST-com.ican.config.ArticleSearchRecoveryIntegrationTest.xml").getroot()
        counts = {key: int(suite.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
        assert counts == dict(tests=1, failures=0, errors=0, skipped=0), counts
        report["checks"].append(dict(name="actual_search_retry_quarantine_and_replay", **counts))
        stage = "confirmed_publish_source_ack_gap"
        verify_ack_gap(env, folder, name)
        report["checks"].append(dict(name="confirmed_publish_then_interruption_preserves_source_and_duplicate_replay"))
    except Exception as error:
        failed = True
        report["error"] = type(error).__name__
        report["failed_stage"] = stage
        if isinstance(error, urllib.error.URLError):
            report["reason"] = type(error.reason).__name__
    finally:
        if created:
            try:
                request(path, "DELETE", auth=auth)
                try:
                    request(path, auth=auth)
                except urllib.error.HTTPError as error:
                    if error.code != 404:
                        raise
                else:
                    raise AssertionError("Vhost still exists")
                report["cleanup"] = "deleted_and_read_back_absent"
            except Exception as error:
                failed = True
                report["cleanup"] = type(error).__name__
        report["finished"] = datetime.now().isoformat()
        report["resource_creation_confirmed"] = created
        output = folder / (name + ".json")
        output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(dict(evidence=str(output), passed=not failed, **report), ensure_ascii=False))
    return int(failed)


if __name__ == "__main__":
    raise SystemExit(main())
