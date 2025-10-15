#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
pixiv_crawler.py
完善版 Pixiv 排行榜爬虫（多线程 + 断点续传 + 连接池 + 速率限制）
Author: ChatGPT (for user)
"""

import os
import re
import sys
import time
import json
import random
import sqlite3
import logging
import threading
import requests
import datetime
from typing import Set, Iterable, Callable, Dict, Optional, Tuple
from concurrent import futures
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from tqdm import tqdm

# -----------------------------
# CONFIG - 修改这些参数以适配你的环境
# -----------------------------
SAVE_DIR = r"D:\IdeaProjects\blog\blog-springboot\src\main\resources\static\pixiv"
CONFIG = {
    "COOKIE": "first_visit_datetime_pc=2025-10-08%2016%3A22%3A56; cc1=2025-10-08%2016%3A22%3A56; p_ab_id=0; p_ab_id_2=9; p_ab_d_id=2079616813; yuid_b=OGKEInA; _cfuvid=fnAtYueoyZ4iQYlUjom3NRkztOXK70uGw85aq43bo3M-1759908176346-0.0.1.1-604800000; _ga=GA1.1.1242572132.1759908179; PHPSESSID=120524657_1aEmPqBfgZpWAPFnWPIwxKCcjmH8zrJM; device_token=620a129494e3f0758322d42240f68216; privacy_policy_agreement=7; __cf_bm=A5P5fHSkqbak148DGs6UJ73aP5icztehLlVH9PeR6.0-1759908234-1.0.1.1-MwHwqlR8KKGQgsBnW5.VdyWYJPKIs.DWNbNOnGjt6.uonsWjRyx0HMYp0gmfimIcl6RqUvXisOGlz3xBrf1Elpou2VJ.JUhyeXz1RhxyofgAt1f.LhjSI9XjZsCJUureNPd0wITbRcgGJPOYcV5Upw; _ga_MZ1NL4PHH0=GS2.1.s1759908184$o1$g1$t1759908234$j10$l0$h0; c_type=22; privacy_policy_notification=0; a_type=0; b_type=2; _gcl_au=1.1.2038222779.1759908241; login_ever=yes; cf_clearance=omX1sJr4AAtjV42F.MenoWTz4q1IGN_Xy01DEULqbrg-1759908989-1.2.1.1-dS8ap6kxiX0tKSQ.unXmLswVU5z10PO9W7tqNSQ6iuoK3pdb2tZM6Bc0lytSDrQUMpspytbsYEoyIaUqZ4ZuvvIE3mt_.Hmvq4Ze3mt08zh2vKRzLzGLCuE5Q21Ro933ghYCb8zGn7isFZ8tUU9bGbtYuk.FoEBDJy6eWk0USly51.6IQkwcBvpzJKN1oaJCrh.MBrIKk1qHY.d9yZODuRwF0hvPhrvTyeT1sJdagk4; _ga_75BBYNYN9J=GS2.1.s1759908178$o1$g1$t1759908991$j58$l0$h0",
    "USER_AGENT": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
    "USER_ID": "120524657",          # pixiv user id (用于 x-user-id / Referer)
    "DATE": (datetime.datetime.now() - datetime.timedelta(days=12)).strftime("%Y%m%d"),              # 存储子目录
    "THREADS": 12,
    "CAPACITY_MB": 10000,            # 总流量上限 (MB)
    "STANDARD_TIMEOUT": 10,
    "TOP_NUM": 60,
    "TIME_MODE": "monthly",          # daily_air, weekly, monthly, daily_ai ,daily_r18_ai ,weekly_r18...
    "CONTENT": "illust",                # all, illust, manga, etc.
    "STATE_DB": "state.db",
    "LOG_LEVEL": logging.INFO,
}
# -----------------------------

# logging
logging.basicConfig(
    level=CONFIG["LOG_LEVEL"],
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler()]
)
logger = logging.getLogger("pixiv_crawler")
handler = logging.StreamHandler(sys.stdout)
formatter = logging.Formatter("%(asctime)s | %(levelname)s | %(message)s", "%Y-%m-%d %H:%M:%S")
handler.setFormatter(formatter)
logger.addHandler(handler)
logger.setLevel(logging.INFO)

# -----------------------------
# Utilities: DB for state persistence (断点续传)
# -----------------------------
class StateDB:
    def __init__(self, db_path: str = CONFIG["STATE_DB"]):
        self.db_path = db_path
        self._lock = threading.Lock()
        self._init_db()

    def _init_db(self):
        with self._connect() as conn:
            c = conn.cursor()
            c.execute("""
                CREATE TABLE IF NOT EXISTS images (
                    image_name TEXT PRIMARY KEY,
                    url TEXT,
                    size_mb REAL,
                    downloaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            c.execute("""
                CREATE TABLE IF NOT EXISTS illusts (
                    illust_id TEXT PRIMARY KEY,
                    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            conn.commit()

    def _connect(self):
        return sqlite3.connect(self.db_path, check_same_thread=False)

    def image_exists(self, image_name: str) -> bool:
        with self._connect() as conn:
            c = conn.cursor()
            c.execute("SELECT 1 FROM images WHERE image_name = ? LIMIT 1", (image_name,))
            return c.fetchone() is not None

    def mark_image(self, image_name: str, url: str, size_mb: float):
        with self._lock:
            with self._connect() as conn:
                c = conn.cursor()
                c.execute(
                    "INSERT OR REPLACE INTO images(image_name, url, size_mb) VALUES(?,?,?)",
                    (image_name, url, size_mb)
                )
                conn.commit()

    def illust_exists(self, illust_id: str) -> bool:
        with self._connect() as conn:
            c = conn.cursor()
            c.execute("SELECT 1 FROM illusts WHERE illust_id = ? LIMIT 1", (illust_id,))
            return c.fetchone() is not None

    def mark_illust(self, illust_id: str):
        with self._lock:
            with self._connect() as conn:
                c = conn.cursor()
                c.execute("INSERT OR REPLACE INTO illusts(illust_id) VALUES(?)", (illust_id,))
                conn.commit()


# -----------------------------
# Downloader
# -----------------------------
class Downloader:
    def __init__(self, capacity_mb: float, headers: Dict[str, str], threads: int, standard_time: int, date: str, time_mode: str,
                 state_db: StateDB):
        self.url_group: Set[str] = set()
        self.capacity_mb = float(capacity_mb)
        self.store_path = SAVE_DIR
        self.standard_time = standard_time
        self.threads = threads
        self.base_headers = headers.copy()
        self._session = self._build_session()
        self._flow_lock = threading.Lock()
        self._flow_size = 0.0
        self.state_db = state_db

        os.makedirs(self.store_path, exist_ok=True)

    def _build_session(self) -> requests.Session:
        session = requests.Session()
        session.headers.update(self.base_headers)
        retries = Retry(
            total=5,
            backoff_factor=0.5,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["GET", "POST"]
        )
        adapter = HTTPAdapter(max_retries=retries, pool_connections=100, pool_maxsize=100)
        session.mount("https://", adapter)
        session.mount("http://", adapter)
        return session

    def add(self, urls: Iterable[str]):
        for url in urls:
            self.url_group.add(url)

    @staticmethod
    def _get_image_name_and_id(url: str) -> (str, Optional[str]):
        image_name = url[url.rfind("/") + 1:]
        m = re.search(r"/(\d+)_", url)
        image_id = m.group(1) if m else None
        return image_name, image_id

    def _already_downloaded(self, image_name: str) -> bool:
        # check sqlite state first, then file existence
        if self.state_db.image_exists(image_name):
            return True
        path = os.path.join(self.store_path, image_name)
        return os.path.exists(path)

    def _download_one(self, url: str) -> float:
        """
        下载单张图片并返回大小（MB）。失败返回0。
        线程内部不会直接修改 _flow_size（由调用线程统一处理）
        """
        image_name, image_id = self._get_image_name_and_id(url)
        image_path = os.path.join(self.store_path, image_name)

        if self._already_downloaded(image_name):
            logger.debug(f"Skipped already downloaded: {image_name}")
            return 0.0

        headers = self.base_headers.copy()
        if image_id:
            headers.update({"Referer": f"https://www.pixiv.net/artworks/{image_id}"})

        # random small sleep to reduce burst
        time.sleep(random.uniform(0.2, 1.0))

        for attempt in range(1, 8):
            try:
                resp = self._session.get(url, headers=headers, timeout=(4, self.standard_time))
                status = resp.status_code

                if status == 200:
                    # ensure content-length
                    cl = resp.headers.get("content-length")
                    if cl is None:
                        # attempt to write but log
                        logger.warning(f"content-length missing for {url}, writing content and measuring len")
                        content = resp.content
                        size_mb = len(content) / (1 << 20)
                        with open(image_path, "wb") as f:
                            f.write(content)
                        # mark state
                        self.state_db.mark_image(image_name, url, size_mb)
                        return size_mb

                    image_size = int(cl)
                    with open(image_path, "wb") as f:
                        f.write(resp.content)
                    size_mb = image_size / (1 << 20)
                    self.state_db.mark_image(image_name, url, size_mb)
                    logger.info(f"Downloaded {image_name} ({size_mb:.2f} MB)")
                    return size_mb

                elif status in (429, 403):
                    logger.warning(f"Got {status} for {url}. Backing off. Attempt {attempt}")
                    time.sleep(5 + random.random() * 5)
                else:
                    logger.warning(f"Unexpected status {status} for {url}. Attempt {attempt}")
                    time.sleep(1 + random.random() * 2)

            except requests.exceptions.RequestException as e:
                logger.debug(f"[Attempt {attempt}] Request error for {url}: {e}")
                time.sleep(1 + random.random() * 2)
            except Exception as e:
                logger.exception(f"Error while downloading {url}: {e}")
                time.sleep(1)

        logger.error(f"Failed to download after retries: {url}")
        return 0.0

    def download(self) -> float:
        """
        使用 ThreadPoolExecutor + as_completed 来支持在达到流量阈值时取消剩余任务。
        返回总下载流量（MB）
        """
        logger.info("===== downloader start =====")
        total_jobs = len(self.url_group)
        if total_jobs == 0:
            logger.info("No urls to download.")
            return 0.0
        print(f"TOTAL_COUNT: {total_jobs}")
        sys.stdout.flush()
        futures_list = []
        with futures.ThreadPoolExecutor(max_workers=self.threads) as executor:
            # submit all jobs first
            for url in self.url_group:
                futures_list.append(executor.submit(self._download_one, url))

            # iterate as completed and sum sizes; cancel pending if capacity exceeded
            with tqdm(total=total_jobs, desc="downloading") as pbar:
                for fut in futures.as_completed(futures_list):
                    try:
                        size_mb = fut.result()
                    except Exception as e:
                        logger.exception("A future raised an exception during download.")
                        size_mb = 0.0

                    with self._flow_lock:
                        self._flow_size += size_mb
                        current_flow = self._flow_size

                    pbar.update(1)
                    pbar.set_description(f"downloading / flow {current_flow:.2f}MB")

                    if current_flow > self.capacity_mb:
                        logger.warning(f"Capacity reached ({current_flow:.2f}MB > {self.capacity_mb}MB). Cancelling remaining tasks.")
                        # cancel pending futures
                        for f in futures_list:
                            if not f.done():
                                f.cancel()
                        break

        logger.info("===== downloader complete =====")

        return self._flow_size


# -----------------------------
# Collector
# -----------------------------
class Collector:
    def __init__(self, threads: int, user_id: str, headers: Dict[str, str], downloader: Downloader, state_db: StateDB,
                 standard_time: int,time_mode: str ):
        self.id_group: Set[str] = set()
        self.threads = threads
        self.user_id = user_id
        self.base_headers = headers.copy()
        self.downloader = downloader
        self.state_db = state_db
        self.standard_time = standard_time
        self.time_mode = time_mode
        self._session = self._build_session()

    def _build_session(self) -> requests.Session:
        session = requests.Session()
        session.headers.update(self.base_headers)
        adapter = HTTPAdapter(pool_connections=50, pool_maxsize=50, max_retries=3)
        session.mount("https://", adapter)
        return session

    def add(self, image_id: str):
        if not self.state_db.illust_exists(image_id):
            self.id_group.add(image_id)
        else:
            logger.debug(f"Illust {image_id} already collected in DB, skipping add.")

    def select_page(self, response) -> Set[str]:
        """
        从 ajax/illust/{id}/pages 返回的 JSON 中提取原图 URL 集合
        """
        group = set()
        try:
            body = response.json().get("body", [])
            for item in body:
                urls = item.get("urls", {})
                original = urls.get("original")
                if original:
                    group.add(original)
        except Exception as e:
            logger.exception("select_page: parse error")
        return group

    def get_artworks_urls(self, args: Tuple[str, Callable, Optional[Dict]]) -> Optional[Iterable[str]]:
        url, selector, additional_headers = args
        headers = self.base_headers.copy()
        if additional_headers:
            headers.update(additional_headers)
        # rate limiting sleep to avoid bursts
        time.sleep(random.uniform(0.4, 1.0))

        for attempt in range(1, 8):
            try:
                resp = self._session.get(url, headers=headers, timeout=(4, self.standard_time))
                if resp.status_code == 200:
                    urls = selector(resp)
                    return urls
                elif resp.status_code in (429, 403):
                    logger.warning(f"Collector got {resp.status_code} for {url}. Backoff. Attempt {attempt}")
                    time.sleep(5 + random.random() * 5)
                else:
                    logger.warning(f"Collector unexpected status {resp.status_code} for {url}. Attempt {attempt}")
                    time.sleep(1)
            except requests.exceptions.RequestException as e:
                logger.debug(f"Collector request exception: {e}")
                time.sleep(1 + random.random())
            except Exception as e:
                logger.exception("Collector error")
                time.sleep(1)
        logger.error(f"Collector failed to fetch: {url}")
        return None

    def collect(self):
        logger.info("===== collector start =====")
        if not self.id_group:
            logger.info("No illust ids to collect.")
            return set()

        with futures.ThreadPoolExecutor(max_workers=self.threads) as executor:
            with tqdm(total=len(self.id_group), desc="collecting urls") as pbar:
                urls_list = [f"https://www.pixiv.net/ajax/illust/{illust_id}/pages?lang=zh" for illust_id in self.id_group]
                additional_headers_list = [
                    {
                        "Referer": f"https://www.pixiv.net/artworks/{illust_id}",
                        "x-user-id": self.user_id,
                        "x-requested-with": "XMLHttpRequest"
                    }
                    for illust_id in self.id_group
                ]

                args_iter = zip(urls_list, [self.select_page] * len(urls_list), additional_headers_list)

                # map and add to downloader
                for urls in executor.map(self.get_artworks_urls, args_iter):
                    if urls:
                        self.downloader.add(urls)
                    pbar.update(1)

        # mark illusts as collected in DB to avoid re-collecting next time
        for iid in self.id_group:
            self.state_db.mark_illust(iid)

        logger.info("===== collector complete =====")

        return self.id_group


# -----------------------------
# RankingCrawler: 控制流程
# -----------------------------
class RankingCrawler:
    def __init__(self):
        self.top_num = CONFIG["TOP_NUM"]
        self.time_mode = CONFIG["TIME_MODE"]
        self.content = CONFIG["CONTENT"]
        self.headers = {
            "Cookie": CONFIG["COOKIE"],
            "User-Agent": CONFIG["USER_AGENT"]
        }
        self.threads = CONFIG["THREADS"]
        self.capacity = CONFIG["CAPACITY_MB"]
        self.standard_time = CONFIG["STANDARD_TIMEOUT"]
        self.user_id = CONFIG["USER_ID"]
        self.date = CONFIG["DATE"]
        self.time_mode = CONFIG["TIME_MODE"]

        # state db
        self.state_db = StateDB(CONFIG["STATE_DB"])

        # components
        self.downloader = Downloader(self.capacity, self.headers, self.threads, self.standard_time, self.date,self.time_mode,
                                     state_db=self.state_db)
        self.collector = Collector(self.threads, self.user_id, self.headers, self.downloader, self.state_db,
                                   self.standard_time,self.time_mode)

    def get_multi_page_json(self, time_mode: Optional[str] = None, content: Optional[str] = None, date: Optional[str] = None):
        if time_mode:
            self.time_mode = time_mode
        if content:
            self.content = content
        if date:
            self.date = date

        per_page = 50
        pages = max(1, self.top_num // per_page)

        for p in range(1, pages + 1):
            url = f"https://www.pixiv.net/ranking.php?mode={self.time_mode}&content={self.content}&date={self.date}&p={p}&format=json"
            headers = self.headers.copy()
            headers.update({
                "Referer": f"https://www.pixiv.net/ranking.php?mode={self.time_mode}&date={self.date}",
                "x-requested-with": "XMLHttpRequest"
            })

            try:
                resp = requests.get(url, headers=headers, timeout=(4, self.standard_time))
                if resp.status_code == 200:
                    art_works = resp.json().get("contents", [])
                    for item in art_works:
                        illust_id = str(item.get("illust_id"))
                        if illust_id:
                            self.collector.add(illust_id)
                    logger.info(f"Fetched ranking page {p} ({len(art_works)} items)")
                else:
                    logger.warning(f"Failed to get ranking page {p} status {resp.status_code}")
            except Exception as e:
                logger.exception(f"Error fetching ranking page {p}: {e}")

            time.sleep(random.uniform(0.8, 1.6))

    def run(self):
        logger.info("===== RankingCrawler run start =====")
        self.get_multi_page_json()
        self.collector.collect()
        total_flow = self.downloader.download()
        logger.info(f"Total downloaded: {total_flow:.2f} MB")
        logger.info("===== RankingCrawler run complete =====")


# -----------------------------
# Entry point
# -----------------------------
if __name__ == "__main__":
    # Quick sanity check: require a cookie to be provided
    if not CONFIG["COOKIE"] or "<PUT_YOUR_COOKIE_HERE>" in CONFIG["COOKIE"]:
        logger.error("Please set CONFIG['COOKIE'] to a valid Pixiv cookie before running.")
    else:
        crawler = RankingCrawler()
        crawler.run()
