"""Bounded, opt-in replay of article.sync.error.queue; default is count-only.

Credentials/target come only from BLOG_RABBITMQ_* environment variables.
No application startup, topology changes, database access or body rewriting.
Requires pika (also looks in the existing target/q4-python directory).
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "target/q4-python"))
ERROR_QUEUE = "article.sync.error.queue"
EXCHANGE = "article.topic"
ROUTING_KEY = "article.key"
TARGET_QUEUE = "article.queue"


def article_ids(body):
    """Reject the entire batch before publishing, including unrelated messages."""
    event = json.loads(body.decode("utf-8"))
    if (not isinstance(event, dict) or event.get("isDdl") not in (None, False)
            or event.get("table") != "t_article" or event.get("type") not in ("INSERT", "UPDATE", "DELETE")
            or not isinstance(event.get("data"), list) or not event["data"]):
        raise ValueError("Not an article DML batch")
    ids = []
    for row in event["data"]:
        value = row.get("id") if isinstance(row, dict) else None
        if (isinstance(value, bool) or not isinstance(value, (int, str))
                or not re.fullmatch(r"[0-9]+", str(value)) or not 0 < int(value) <= 2147483647):
            raise ValueError("Invalid article id")
        ids.append(int(value))
    return ids


class Audit:
    def __init__(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        self.file = path.open("a", encoding="utf-8")

    def record(self, stage, **values):
        self.file.write(json.dumps(dict(time=datetime.now(timezone.utc).isoformat(), stage=stage, **values),
                                   ensure_ascii=False) + "\n")
        self.file.flush()
        os.fsync(self.file.fileno())

    def close(self):
        self.file.close()


def connection_parameters(values):
    import pika
    return pika.ConnectionParameters(
        host=values["BLOG_RABBITMQ_HOST"], port=int(values.get("BLOG_RABBITMQ_PORT", "5672")),
        virtual_host=values["BLOG_RABBITMQ_VIRTUAL_HOST"],
        credentials=pika.PlainCredentials(values["BLOG_RABBITMQ_USERNAME"], values["BLOG_RABBITMQ_PASSWORD"]),
        heartbeat=30, socket_timeout=10, stack_timeout=15, blocked_connection_timeout=10,
        connection_attempts=1)


def preflight(channel):
    # Passive checks only: a typo must not silently create a new queue or exchange.
    source = channel.queue_declare(queue=ERROR_QUEUE, passive=True).method
    channel.queue_declare(queue=TARGET_QUEUE, passive=True)
    channel.exchange_declare(exchange=EXCHANGE, passive=True)
    if source.consumer_count:
        raise ValueError("The error queue has an active consumer")
    return source.message_count


def replay(channel, limit, audit):
    import pika
    channel.confirm_delivery()
    confirmed = ack_sent = 0
    for _ in range(limit):
        method, properties, body = channel.basic_get(queue=ERROR_QUEUE, auto_ack=False)
        if method is None:
            break
        digest = hashlib.sha256(body).hexdigest()
        stage = "validate"
        try:
            ids = article_ids(body)
            audit.record("acquired", sha256=digest, article_ids=ids,
                         message_id=properties.message_id, redelivered=method.redelivered)
            headers = dict(properties.headers or {})
            headers["x-search-replay-sha256"] = digest
            # Keep original bytes/context, but never carry an expired TTL or another publisher's user_id.
            outgoing = pika.BasicProperties(
                content_type=properties.content_type, content_encoding=properties.content_encoding,
                headers=headers, delivery_mode=2, message_id=properties.message_id or digest,
                correlation_id=properties.correlation_id, type=properties.type, app_id=properties.app_id)
            stage = "publish"
            channel.basic_publish(exchange=EXCHANGE, routing_key=ROUTING_KEY, body=body,
                                  properties=outgoing, mandatory=True)
            # In BlockingChannel confirm mode, NACK/mandatory return raise; no ACK on those paths.
            confirmed += 1
            stage = "audit_confirm"
            audit.record("publish_confirmed", sha256=digest)
            stage = "ack"
            channel.basic_ack(delivery_tag=method.delivery_tag)
            ack_sent += 1
            stage = "audit_ack"
            # basic_ack has no broker reply; this records sending it, not confirmed deletion.
            audit.record("source_ack_sent", sha256=digest)
        except Exception as error:
            # Stop at the first failure. Closing the connection requeues any unacknowledged source.
            # A publish may have succeeded already: subsequent replay is at least once.
            audit.record("stopped", sha256=digest, failed_stage=stage, error=type(error).__name__,
                         publish_confirmed=confirmed, source_ack_sent=ack_sent)
            raise
    return dict(publish_confirmed=confirmed, source_ack_sent=ack_sent, index_completed="not_checked")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--expect-host", required=True)
    parser.add_argument("--expect-vhost", required=True)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--limit", type=int, default=1, help="1..100 messages; execution stops at first failure")
    args = parser.parse_args()
    values = os.environ
    if not 1 <= args.limit <= 100:
        parser.error("--limit must be 1..100")
    required = ("HOST", "VIRTUAL_HOST", "USERNAME", "PASSWORD")
    if any(not values.get("BLOG_RABBITMQ_" + field) for field in required):
        parser.error("Set BLOG_RABBITMQ_HOST/VIRTUAL_HOST/USERNAME/PASSWORD (PORT optional)")
    if (values["BLOG_RABBITMQ_HOST"] != args.expect_host
            or values["BLOG_RABBITMQ_VIRTUAL_HOST"] != args.expect_vhost):
        parser.error("Explicit expected host/vhost do not match the configured target")
    if args.execute and not values.get("_BLOG_SEARCH_REPLAY_WORKER"):
        # Blocking publisher-confirm wait is not bounded by socket_timeout. A separate
        # process gives this one-shot command a real 120s deadline; exit leaves unacked data recoverable.
        env = dict(values)
        env["_BLOG_SEARCH_REPLAY_WORKER"] = "1"
        env["_BLOG_SEARCH_REPLAY_RUN"] = uuid.uuid4().hex
        try:
            return subprocess.run([sys.executable, str(Path(__file__).resolve()), *sys.argv[1:]],
                                  env=env, timeout=120).returncode
        except subprocess.TimeoutExpired:
            print(json.dumps(dict(outcome="timeout", delivery_outcome="unknown",
                                  audit=str(ROOT / "logs/search-replay" / (env["_BLOG_SEARCH_REPLAY_RUN"] + ".jsonl")))))
            return 1
    connection = audit = path = None
    try:
        import pika
        connection = pika.BlockingConnection(connection_parameters(values))
        channel = connection.channel()
        pending = preflight(channel)
        if not args.execute:
            print(json.dumps(dict(mode="count_only", ready=pending, source=ERROR_QUEUE,
                                  target=TARGET_QUEUE, consumed=0)))
            return 0
        path = ROOT / "logs/search-replay" / (values["_BLOG_SEARCH_REPLAY_RUN"] + ".jsonl")
        audit = Audit(path)
        audit.record("start", host=args.expect_host, vhost=args.expect_vhost, limit=args.limit, ready=pending)
        result = replay(channel, args.limit, audit)
        audit.record("finished", **result)
        print(json.dumps(dict(outcome="republished", audit=str(path), **result)))
        return 0
    except Exception as error:
        # Never dump credentials, raw bodies, exception messages or connection arguments.
        print(json.dumps(dict(outcome="stopped", error=type(error).__name__,
                              audit=str(path) if path else None, index_completed="not_checked")))
        return 1
    finally:
        try:
            if connection is not None and connection.is_open:
                connection.close()
        finally:
            if audit is not None:
                audit.close()


if __name__ == "__main__":
    raise SystemExit(main())
