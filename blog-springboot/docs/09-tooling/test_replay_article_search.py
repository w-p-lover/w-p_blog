"""Focused offline checks for the executable recovery client's data-loss boundaries."""
from contextlib import redirect_stdout
import io
import json
import os
from pathlib import Path
import subprocess
import sys
from types import SimpleNamespace
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import replay_article_search as client
import pika

BODY = b'{"table":"t_article","type":"UPDATE","data":[{"id":11},{"id":"12"}]}'


class MemoryAudit:
    def __init__(self, failure=None):
        self.events = []
        self.failure = failure

    def record(self, stage, **values):
        self.events.append(dict(stage=stage, **values))
        if stage == self.failure:
            raise OSError("Controlled audit failure")


class Channel:
    def __init__(self, body=BODY, failure=None):
        self.body = body
        self.failure = failure
        self.sent = []
        self.acked = []
        self.gets = 0
        self.properties = pika.BasicProperties(headers={"x-exception-message": "synthetic"},
                                               message_id="original", expiration="1", user_id="another-user")

    def confirm_delivery(self):
        pass

    def basic_get(self, **kwargs):
        assert kwargs == dict(queue=client.ERROR_QUEUE, auto_ack=False)
        self.gets += 1
        return SimpleNamespace(delivery_tag=self.gets, redelivered=False), self.properties, self.body

    def basic_publish(self, **kwargs):
        self.sent.append(kwargs)
        if self.failure:
            raise self.failure

    def basic_ack(self, delivery_tag):
        self.acked.append(delivery_tag)


class ReplayTest(unittest.TestCase):
    def test_confirm_before_ack_preserves_bytes_context_and_persistence_without_old_ttl(self):
        channel, audit = Channel(), MemoryAudit()
        result = client.replay(channel, 2, audit)
        self.assertEqual([1, 2], channel.acked)
        self.assertEqual(2, result["publish_confirmed"])
        self.assertEqual("not_checked", result["index_completed"])
        self.assertEqual(["acquired", "publish_confirmed", "source_ack_sent"] * 2,
                         [event["stage"] for event in audit.events])
        published = channel.sent[0]
        self.assertEqual(BODY, published["body"])
        self.assertTrue(published["mandatory"])
        properties = published["properties"]
        self.assertEqual(2, properties.delivery_mode)
        self.assertEqual("original", properties.message_id)
        self.assertEqual("synthetic", properties.headers["x-exception-message"])
        self.assertIsNone(properties.expiration)
        self.assertIsNone(properties.user_id)

    def test_bad_tail_or_unrelated_batch_never_publishes_or_acks_any_row(self):
        bad_ids = [BODY.replace(b'"12"', value) for value in (b'true', b'0', b'1.5', b'2147483648', b'null')]
        for body in [*bad_ids, BODY.replace(b'{"id":"12"}', b'{}'), BODY.replace(b't_article', b't_comment'),
                     BODY.replace(b'"UPDATE"', b'"TRUNCATE"'), b'null', b'{bad json']:
            with self.subTest(body=body):
                channel = Channel(body)
                with self.assertRaises(ValueError):
                    client.replay(channel, 10, MemoryAudit())
                self.assertEqual([], channel.sent)
                self.assertEqual([], channel.acked)
                self.assertEqual(1, channel.gets)

    def test_return_nack_or_connection_unknown_stops_without_source_ack(self):
        for error in [pika.exceptions.UnroutableError([]), pika.exceptions.NackError([]), OSError("unknown")]:
            with self.subTest(error=type(error).__name__):
                channel, audit = Channel(failure=error), MemoryAudit()
                with self.assertRaises(type(error)):
                    client.replay(channel, 10, audit)
                self.assertEqual([], channel.acked)
                self.assertEqual(1, channel.gets)
                self.assertEqual("publish", audit.events[-1]["failed_stage"])

    def test_audit_failure_before_ack_keeps_source_even_if_publish_confirmed(self):
        channel, audit = Channel(), MemoryAudit("publish_confirmed")
        with self.assertRaises(OSError):
            client.replay(channel, 1, audit)
        self.assertEqual(1, len(channel.sent))
        self.assertEqual([], channel.acked)
        self.assertEqual(1, audit.events[-1]["publish_confirmed"])

    def test_default_only_passively_counts_and_never_gets_a_message(self):
        channel = unittest.mock.Mock()
        channel.queue_declare.return_value.method = SimpleNamespace(consumer_count=0, message_count=7)
        connection = unittest.mock.Mock()
        connection.channel.return_value = channel
        with patch.dict(os.environ, self.environment(), clear=True), patch.object(sys, "argv", self.command()), \
                patch("pika.BlockingConnection", return_value=connection), redirect_stdout(io.StringIO()) as output:
            self.assertEqual(0, client.main())
        channel.basic_get.assert_not_called()
        channel.basic_publish.assert_not_called()
        self.assertEqual(7, json.loads(output.getvalue())["ready"])
        for call in channel.queue_declare.call_args_list:
            self.assertTrue(call.kwargs["passive"])

    def test_execute_supervisor_times_out_and_does_not_report_success(self):
        with patch.dict(os.environ, self.environment(), clear=True), \
                patch.object(sys, "argv", self.command() + ["--execute"]), \
                patch("subprocess.run", side_effect=subprocess.TimeoutExpired("synthetic", 120)) as run, \
                redirect_stdout(io.StringIO()) as output:
            self.assertEqual(1, client.main())
        self.assertEqual(120, run.call_args.kwargs["timeout"])
        self.assertEqual("unknown", json.loads(output.getvalue())["delivery_outcome"])

    @staticmethod
    def environment():
        return dict(BLOG_RABBITMQ_HOST="synthetic.invalid", BLOG_RABBITMQ_VIRTUAL_HOST="isolated",
                    BLOG_RABBITMQ_USERNAME="synthetic", BLOG_RABBITMQ_PASSWORD="not-a-real-password")

    @staticmethod
    def command():
        return [str(Path(client.__file__)), "--expect-host", "synthetic.invalid", "--expect-vhost", "isolated"]


if __name__ == "__main__":
    unittest.main()
