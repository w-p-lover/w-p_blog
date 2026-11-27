"""Offline capacity/rollback checks against actual mapper statements.

SQLite serializes writers: this is not MySQL row-lock/deadlock or MQ evidence.
The fixed Beijing clock is mapped to CURRENT_TIMESTAMP; temporary synthetic rows only.
"""
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from pathlib import Path
import sqlite3
from tempfile import TemporaryDirectory
import xml.etree.ElementTree as ET
import re

ROOT = Path(__file__).resolve().parents[2]
NODES = ET.parse(ROOT / "src/main/resources/mapper/ActivityMapper.xml").getroot()


def sql(name):
    node = next(n for n in NODES if n.get("id") == name)
    value = node.text or ""
    for child in node:
        if child.tag != "include" or child.get("refid") != "activityNow":
            raise ValueError("Only static statements with the activity clock are supported")
        value += "CURRENT_TIMESTAMP" + (child.tail or "")
    value = " ".join(value.split())
    return re.sub(r"#\{(\w+)\}", r":\1", value)


def apply(db, name, **args):
    return db.execute(sql(name), args).rowcount


def main():
    with TemporaryDirectory(prefix="blog-registration-") as folder:
        path = Path(folder) / "synthetic.db"
        with closing(sqlite3.connect(path)) as db, db:
            db.executescript("""
                CREATE TABLE t_activity(id INTEGER PRIMARY KEY,status TEXT,remaining INT,capacity INT,
                    pending_count INT,update_time TEXT);
                CREATE TABLE t_activity_registration(request_id TEXT PRIMARY KEY,activity_id INT,user_id INT,
                    status TEXT,dispatch_token TEXT,next_dispatch_at TEXT,last_error TEXT,update_time TEXT,
                    UNIQUE(activity_id,user_id));
                INSERT INTO t_activity VALUES(1,'PUBLISHED',1,1,2,NULL);
                INSERT INTO t_activity_registration VALUES('a',1,7,'PENDING',NULL,NULL,NULL,NULL);
                INSERT INTO t_activity_registration VALUES('b',1,8,'PENDING',NULL,NULL,NULL,NULL);
            """)
        def confirm(identity):
            with closing(sqlite3.connect(path, timeout=5)) as db, db:
                db.execute("BEGIN IMMEDIATE")
                status = db.execute("SELECT status FROM t_activity_registration WHERE request_id=?", (identity,)).fetchone()[0]
                if status != "PENDING":
                    return status
                result = "CONFIRMED" if apply(db, "consumeCapacity", id=1) == 1 else "SOLD_OUT"
                if result == "SOLD_OUT":
                    assert apply(db, "releasePending", id=1) == 1
                assert apply(db, "changeStatus", requestId=identity, **{"from": "PENDING", "to": result}) == 1
                return result
        with ThreadPoolExecutor(max_workers=2) as workers:
            result = list(workers.map(confirm, ["a", "b"]))
        assert sorted(result) == ["CONFIRMED", "SOLD_OUT"]
        winner = ["a", "b"][result.index("CONFIRMED")]
        assert confirm(winner) == "CONFIRMED"
        with closing(sqlite3.connect(path)) as db, db:
            assert db.execute("SELECT remaining,pending_count FROM t_activity").fetchone() == (0, 0)
            db.execute("BEGIN IMMEDIATE")
            assert apply(db, "restoreCapacity", id=1) == 1
            assert apply(db, "changeStatus", requestId=winner, **{"from": "CONFIRMED", "to": "CANCELED"}) == 1
            db.commit()
            assert apply(db, "changeStatus", requestId=winner, **{"from": "CONFIRMED", "to": "CANCELED"}) == 0
            db.execute("UPDATE t_activity SET pending_count=1")
            db.execute("INSERT INTO t_activity_registration VALUES('c',1,9,'PENDING',NULL,NULL,NULL,NULL)")
            db.commit()
            db.execute("CREATE TRIGGER reject_terminal BEFORE UPDATE ON t_activity_registration "
                       "WHEN NEW.status='CONFIRMED' BEGIN SELECT RAISE(ABORT,'synthetic terminal failure'); END")
            try:
                confirm("c")
            except sqlite3.IntegrityError:
                pass
            else:
                raise AssertionError("Expected terminal failure")
            assert db.execute("SELECT remaining,pending_count FROM t_activity").fetchone() == (1, 1)
            assert db.execute("SELECT status FROM t_activity_registration WHERE request_id='c'").fetchone()[0] == "PENDING"
            assert apply(db, "releasePending", id=1) == 1
            assert apply(db, "changeStatus", requestId="c", **{"from": "PENDING", "to": "CANCELED"}) == 1
            assert db.execute("SELECT remaining,pending_count FROM t_activity").fetchone() == (1, 0)
            try:
                db.execute("INSERT INTO t_activity_registration VALUES('other',1,9,'PENDING',NULL,NULL,NULL,NULL)")
            except sqlite3.IntegrityError:
                pass
            else:
                raise AssertionError("Duplicate activity/user accepted")
        print("PASS: last seat, duplicate processing, cancellation, terminal-failure rollback and unique identity")
        print("Boundary: this script supplies SQLite writer-serialization evidence only; it does not verify MySQL or MQ")


if __name__ == "__main__":
    main()
