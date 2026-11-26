"""Execute actual mapper statements against temporary SQLite; no live database.

SQLite adapts MySQL time/numeric functions. This verifies conditional state
transitions and two-connection contention, not MySQL isolation or query plans.
"""
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from datetime import datetime, timedelta
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Barrier
import uuid
import math
import re
import sqlite3
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
MAPPER = ROOT / "src/main/resources/mapper/AiTaskMapper.xml"
clock = datetime(2026, 10, 25, 10, 0, 0)
statements = {}
for node in ET.parse(MAPPER).getroot():
    if node.get("id") in {"selectDuePendingTasks", "reserveDispatch", "claimPendingTask",
                           "completeExecution", "resetFailedTask", "saveGeneratedPayload",
                           "renewExecution", "expireExecution", "resumeCheckpointTask", "selectRecoverableTasks"}:
        sql = " ".join("".join(node.itertext()).split())
        sql = re.sub(r"#\{([\w.]+)\}", lambda m: ":" + m[1].replace(".", "_"), sql)
        sql = sql.replace("NOW(6)", "NOW()").replace("TIMESTAMPADD(SECOND,", "TIMESTAMPADD('SECOND',")
        statements[node.get("id")] = sql


for node in ET.parse(ROOT / "src/main/resources/mapper/ArticleMapper.xml").getroot():
    if node.get("id") in {"updateArticleAiResult", "resetArticleAiResult", "publishArticleVector"}:
        sql = " ".join("".join(node.itertext()).split())
        statements[node.get("id")] = re.sub(r"#\{([\w.]+)\}", lambda m: ":" + m[1].replace(".", "_"), sql)


def stamp(value):
    return value.strftime("%Y-%m-%d %H:%M:%S.%f")


def connection(path):
    db = sqlite3.connect(path, timeout=10, isolation_level=None)
    db.create_function("UUID", 0, lambda: str(uuid.uuid4()))
    db.create_function("NOW", 0, lambda: stamp(clock))
    db.create_function("LEAST", -1, min)
    db.create_function("POW", 2, math.pow)
    db.create_function("TIMESTAMPADD", 3,
                       lambda unit, seconds, at: stamp(datetime.fromisoformat(at) + timedelta(seconds=seconds)))
    return db


def competing(path, statement, parameters):
    gate = Barrier(2)
    def run(index):
        with closing(connection(path)) as db:
            gate.wait(timeout=5)
            return db.execute(statements[statement], parameters(index)).rowcount
    with ThreadPoolExecutor(max_workers=2) as pool:
        return list(pool.map(run, range(2)))


def main():
    global clock
    with TemporaryDirectory(prefix="blog-task-q1-") as folder:
        path = str(Path(folder) / "task.db")
        with closing(connection(path)) as db:
            db.execute("""CREATE TABLE t_ai_task (
                id INTEGER PRIMARY KEY, biz_type TEXT, task_type TEXT, status TEXT,
                dispatch_attempts INTEGER DEFAULT 0, next_dispatch_at TEXT, execution_token TEXT,
                retry_count INTEGER DEFAULT 0, max_retry_count INTEGER DEFAULT 3,
                started_at TEXT, finished_at TEXT, cost_time INTEGER, result_payload TEXT,
                error_message TEXT, update_time TEXT, biz_id INTEGER, source_version TEXT, generated_payload TEXT)""")
            for task_id, state in [(1, "PENDING"), (2, "PENDING"), (3, "RUNNING"), (4, "FAILED")]:
                db.execute("INSERT INTO t_ai_task(id,biz_type,task_type,status) VALUES(?, 'ARTICLE',"
                           " 'ARTICLE_SUMMARY_TAG_VECTOR', ?)", (task_id, state))
            db.execute("INSERT INTO t_ai_task(id,biz_type,task_type,status) VALUES(5,'OTHER','OTHER','PENDING')")
            due = lambda: [row[0] for row in db.execute(statements["selectDuePendingTasks"], {"size": 100})]
            assert due() == [1, 2]
            # Two actual DB connections, same due task: only one reserves this interval.
            assert sorted(competing(path, "reserveDispatch", lambda _: {"taskId": 1})) == [0, 1]
            assert due() == [2]
            for delay in [30, 60, 120, 240, 300, 300]:
                attempts, next_at = db.execute("SELECT dispatch_attempts,next_dispatch_at FROM t_ai_task WHERE id=1").fetchone()
                assert datetime.fromisoformat(next_at) == clock + timedelta(seconds=delay)
                clock = datetime.fromisoformat(next_at)
                assert 1 in due(), "reserved but unsent work must return to the scan"
                assert db.execute(statements["reserveDispatch"], {"taskId": 1}).rowcount == 1
            assert sorted(competing(path, "claimPendingTask",
                                    lambda i: {"taskId": 2, "token": f"execution-{i}"})) == [0, 1]
            token = db.execute("SELECT execution_token FROM t_ai_task WHERE id=2").fetchone()[0]
            complete = {"task_id": 2, "task_status": "SUCCESS", "task_retryCount": 0,
                        "task_resultPayload": "ok", "task_errorMessage": None,
                        "task_finishedAt": stamp(clock), "task_costTime": 1, "token": "stale"}
            assert db.execute(statements["completeExecution"], complete).rowcount == 0
            complete["token"] = token
            assert db.execute(statements["completeExecution"], complete).rowcount == 1
            assert db.execute(statements["completeExecution"], complete).rowcount == 0
            # A repeated message cannot execute SUCCESS or RUNNING again.
            assert db.execute(statements["claimPendingTask"], {"taskId": 2, "token": "again"}).rowcount == 0
            assert db.execute(statements["claimPendingTask"], {"taskId": 3, "token": "again"}).rowcount == 0
            assert db.execute(statements["resetFailedTask"], {"taskId": 3, "retryCount": 0}).rowcount == 0
            db.execute("UPDATE t_ai_task SET retry_count=1, execution_token='old', error_message='failed',"
                       " result_payload='partial', generated_payload='checkpoint', started_at='old' WHERE id=4")
            assert sorted(competing(path, "resetFailedTask", lambda _: {"taskId": 4, "retryCount": 1})) == [0, 1]
            assert db.execute("SELECT status,retry_count,execution_token,error_message,result_payload,"
                              "started_at FROM t_ai_task WHERE id=4").fetchone() == ("PENDING", 1, None, None, None, None)
            db.execute("UPDATE t_ai_task SET status='FAILED',retry_count=2 WHERE id=4")
            assert db.execute(statements["resetFailedTask"], {"taskId": 4, "retryCount": 1}).rowcount == 0
            db.execute("UPDATE t_ai_task SET retry_count=3 WHERE id=4")
            assert db.execute(statements["resetFailedTask"], {"taskId": 4, "retryCount": 3}).rowcount == 0
            assert db.execute(statements["reserveDispatch"], {"taskId": 5}).rowcount == 0
            assert db.execute(statements["claimPendingTask"], {"taskId": 5, "token": "wrong-type"}).rowcount == 0
            # Q2: actual source/token publication statements, no live DB/vector calls.
            db.execute("""CREATE TABLE t_article (id INTEGER PRIMARY KEY, source_version TEXT,
                content_version TEXT, indexed_source_version TEXT, status INTEGER, is_delete INTEGER,
                ai_summary TEXT, ai_suggest_tags TEXT, ai_indexed_at TEXT)""")
            db.execute("INSERT INTO t_article VALUES(7,'v1','body-1',NULL,1,0,NULL,NULL,NULL)")
            db.execute("UPDATE t_ai_task SET biz_id=7,source_version='v1',status='RUNNING',"
                       "execution_token='exec-old' WHERE id=1")
            publish = dict(articleId=7, sourceVersion="v1", taskId=1, token="exec-old",
                           aiSummary="summary-v1", aiSuggestTags="tags", aiIndexedAt=stamp(clock))
            assert db.execute(statements["saveGeneratedPayload"],
                              dict(taskId=1, token="wrong", payload="bad")).rowcount == 0
            assert db.execute(statements["saveGeneratedPayload"],
                              dict(taskId=1, token="exec-old", payload="checkpoint-v1")).rowcount == 1
            assert db.execute(statements["updateArticleAiResult"], publish).rowcount == 1
            assert db.execute("SELECT source_version,indexed_source_version,ai_summary FROM t_article").fetchone() == ("v1", "v1", "summary-v1")
            # Author edit wins row state, newer task publishes; late old task cannot overwrite it.
            db.execute("UPDATE t_article SET source_version='v2',content_version='body-2' WHERE id=7")
            assert db.execute(statements["resetArticleAiResult"], dict(articleId=7, sourceVersion="v2")).rowcount == 1
            assert db.execute("SELECT ai_summary,indexed_source_version FROM t_article").fetchone() == (None, None)
            db.execute("UPDATE t_ai_task SET biz_id=7,source_version='v2',status='RUNNING',execution_token='exec-new' WHERE id=2")
            new = dict(publish, sourceVersion="v2", taskId=2, token="exec-new", aiSummary="summary-v2")
            assert db.execute(statements["updateArticleAiResult"], new).rowcount == 1
            assert db.execute(statements["updateArticleAiResult"], publish).rowcount == 0
            assert db.execute(statements["publishArticleVector"], dict(articleId=7, sourceVersion="v1")).rowcount == 0
            assert db.execute("SELECT source_version,indexed_source_version,ai_summary FROM t_article").fetchone() == ("v2", "v2", "summary-v2")
            assert db.execute(statements["updateArticleAiResult"], dict(new, token="wrong")).rowcount == 0
            db.execute("UPDATE t_article SET status=3 WHERE id=7")
            assert db.execute(statements["updateArticleAiResult"], new).rowcount == 0
            assert db.execute(statements["publishArticleVector"], dict(articleId=7, sourceVersion="v2")).rowcount == 0
            db.execute("UPDATE t_article SET status=1,is_delete=1 WHERE id=7")
            assert db.execute(statements["updateArticleAiResult"], new).rowcount == 0
            # Failure/reset/reclaim preserve generated checkpoint across a new execution token.
            db.execute("UPDATE t_ai_task SET status='RETRYING',retry_count=1 WHERE id=1")
            assert db.execute(statements["resetFailedTask"], dict(taskId=1, retryCount=1)).rowcount == 1
            assert db.execute(statements["claimPendingTask"], dict(taskId=1, token="next-exec")).rowcount == 1
            assert db.execute("SELECT generated_payload FROM t_ai_task WHERE id=1").fetchone()[0] == "checkpoint-v1"
            # Q3: expiration invalidates token before re-entry. No-checkpoint work is never replayed.
            for task_id, checkpoint in [(8, None), (9, "checkpoint-v3"), (10, "checkpoint-v3")]:
                db.execute("INSERT INTO t_ai_task(id,biz_type,task_type,status,execution_token,generated_payload,"
                           "next_dispatch_at,retry_count) VALUES(?,'ARTICLE','ARTICLE_SUMMARY_TAG_VECTOR',"
                           "'RUNNING','expired',?,?,?)", (task_id, checkpoint, stamp(clock), 2 if task_id == 10 else 0))
            assert db.execute(statements["renewExecution"], dict(taskId=9, token="expired")).rowcount == 0
            assert sorted(competing(path, "expireExecution", lambda _: dict(taskId=9))) == [0, 1]
            assert db.execute(statements["expireExecution"], dict(taskId=8)).rowcount == 1
            assert db.execute(statements["expireExecution"], dict(taskId=10)).rowcount == 1
            assert db.execute("SELECT status FROM t_ai_task WHERE id=8").fetchone()[0] == "UNKNOWN"
            assert db.execute("SELECT status FROM t_ai_task WHERE id=10").fetchone()[0] == "FAILED"
            assert db.execute(statements["saveGeneratedPayload"], dict(taskId=9, token="expired", payload="late")).rowcount == 0
            assert db.execute(statements["resumeCheckpointTask"], dict(taskId=9)).rowcount == 0
            clock += timedelta(seconds=60)
            assert sorted(competing(path, "resumeCheckpointTask", lambda _: dict(taskId=9))) == [0, 1]
            assert db.execute(statements["claimPendingTask"], dict(taskId=9, token="new-lease")).rowcount == 1
            assert db.execute(statements["renewExecution"], dict(taskId=9, token="expired")).rowcount == 0
            assert db.execute(statements["renewExecution"], dict(taskId=9, token="new-lease")).rowcount == 1
            assert db.execute(statements["completeExecution"], dict(complete, task_id=9, token="expired")).rowcount == 0
            assert db.execute("SELECT generated_payload,retry_count FROM t_ai_task WHERE id=9").fetchone() == ("checkpoint-v3", 1)
            for task_id in (8, 10):
                assert db.execute(statements["resumeCheckpointTask"], dict(taskId=task_id)).rowcount == 0
                assert db.execute(statements["resetFailedTask"], dict(taskId=task_id, retryCount=1 if task_id == 8 else 3)).rowcount == 0
            # Both article publication and terminal write roll back in one explicit transaction.
            db.execute("UPDATE t_article SET status=1,is_delete=0 WHERE id=7")
            db.execute("UPDATE t_ai_task SET biz_id=7,source_version='v2' WHERE id=9")
            before = db.execute("SELECT ai_summary,content_version FROM t_article WHERE id=7").fetchone()
            db.execute("BEGIN IMMEDIATE")
            assert db.execute(statements["updateArticleAiResult"], dict(new, taskId=9, token="new-lease", aiSummary="rolled-back")).rowcount == 1
            db.execute("ROLLBACK")
            assert db.execute("SELECT ai_summary,content_version FROM t_article WHERE id=7").fetchone() == before
            db.execute("BEGIN IMMEDIATE")
            assert db.execute(statements["updateArticleAiResult"], dict(new, taskId=9, token="new-lease", aiSummary="committed")).rowcount == 1
            assert db.execute(statements["completeExecution"], dict(complete, task_id=9, token="new-lease")).rowcount == 1
            db.execute("COMMIT")
            clock += timedelta(seconds=901)
            assert db.execute(statements["expireExecution"], dict(taskId=9)).rowcount == 0
            assert db.execute(statements["claimPendingTask"], dict(taskId=9, token="redelivery")).rowcount == 0
            # Q4: legacy/ineligible RETRYING rows must not occupy all scan slots.
            for task_id in range(100, 200):
                db.execute("INSERT INTO t_ai_task(id,biz_type,task_type,status,generated_payload,retry_count,"
                           "next_dispatch_at) VALUES(?,'ARTICLE','ARTICLE_SUMMARY_TAG_VECTOR','RETRYING',?,?,?)",
                           (task_id, None if task_id % 2 else "checkpoint", 1 if task_id % 2 else 3, stamp(clock)))
            db.execute("INSERT INTO t_ai_task(id,biz_type,task_type,status,generated_payload,retry_count,"
                       "next_dispatch_at) VALUES(200,'ARTICLE','ARTICLE_SUMMARY_TAG_VECTOR','RETRYING',"
                       "'checkpoint',1,?)", (stamp(clock),))
            selected = [row[0] for row in db.execute(statements["selectRecoverableTasks"], {"size": 100})]
            assert 200 in selected
            assert not set(range(100, 200)).intersection(selected)
            assert db.execute(statements["resumeCheckpointTask"], dict(taskId=200)).rowcount == 1
            # Missing-checkpoint RUNNING must still be selected, so expiry can isolate UNKNOWN.
            db.execute("UPDATE t_ai_task SET status='RUNNING',next_dispatch_at=? WHERE id=100", (stamp(clock),))
            assert 100 in [row[0] for row in db.execute(statements["selectRecoverableTasks"], {"size": 100})]
    print("PASS: Q4 bounded recovery skips ineligible RETRYING while retaining expired RUNNING.")
    print("PASS: Q3 deadline fencing, two-connection expire/resume, checkpoint-only budget, UNKNOWN gate, atomic publication rollback/commit.")
    print("PASS: Q2 source/token publication, late old result, withdrawal/deletion, checkpoint retained across retry.")
    print("PASS: actual mapper SQL with SQLite time-function adaptation; two-connection reservation/claim/retry,")
    print("backoff and abandoned reservation recovery, terminal replay, token fencing and retry limit.")
    print("NOT VERIFIED: MySQL transactions/row locks/index plan, live RabbitMQ, model/vector side effects.")


if __name__ == "__main__":
    main()
