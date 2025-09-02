import requests
from bs4 import BeautifulSoup
import sqlite3
import redis
import json
from datetime import datetime
import time
from urllib.parse import urljoin, parse_qs, urlparse, urlencode
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading


import pymysql  # 替换 sqlite3
from datetime import datetime
import argparse
import sys

sys.stdout.reconfigure(encoding='utf-8')  # Python 3.7+ 支持
# MySQL 配置
DB_HOST = "121.41.87.40"
DB_PORT = 3306
DB_USER = "root"
DB_PASS = "1224"
DB_NAME = "blog"

# Redis 配置（可选）
REDIS_HOST = "localhost"
REDIS_PORT = 6379
REDIS_DB = 0

# 基础URL
BASE_URL = "https://gitee.com"

# 线程安全的数据库连接锁（避免多线程同时写入冲突）
db_lock = threading.Lock()


# -------------------------- 获取参数 --------------------------
parser = argparse.ArgumentParser()
parser.add_argument("--language", type=str, default="")
parser.add_argument("--category", type=str, default="")
args = parser.parse_args()

language = args.language
category = args.category

print(f"爬取语言: {language}, 分类: {category}")

# 你原来的爬虫逻辑


# -------------------------- 数据库初始化 --------------------------
def init_db():
    with db_lock:
        conn = pymysql.connect(
            host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASS, database=DB_NAME,
            charset="utf8mb4"
        )
        conn.commit()
        conn.close()
    print("✅ MySQL 数据库表已初始化")



# -------------------------- 提取全部分类映射 --------------------------
def extract_all_categories(html):
    soup = BeautifulSoup(html, "html.parser")
    categories = []
    top_category_items = soup.select("li.explore-categories__item")

    for top_item in top_category_items:
        top_a_tag = top_item.select_one("a")
        if not top_a_tag:
            continue

        # 提取顶层分类名
        top_category_name = "".join([
            node.strip() for node in top_a_tag.contents
            if isinstance(node, str) and node.strip()
        ]).strip()

        # 提取顶层分类URL
        top_category_href = top_a_tag.get("href", "")
        top_category_url = urljoin(BASE_URL, top_category_href) if top_category_href else ""

        # 检查子分类
        sub_category_popup = top_item.select_one("div.ui.popup.explore-categories__second")
        if not sub_category_popup:
            if top_category_url:
                categories.append({
                    "top_category": top_category_name,
                    "sub_category": top_category_name,
                    "category_url": top_category_url
                })
            continue

        # 提取子分类
        sub_a_tags = sub_category_popup.select("table a")
        for sub_a in sub_a_tags:
            sub_category_name = sub_a.text.strip()
            sub_category_href = sub_a.get("href", "")
            if not sub_category_href:
                continue

            sub_category_url = urljoin(BASE_URL, sub_category_href)
            parsed_url = urlparse(sub_category_url)
            query_params = parse_qs(parsed_url.query)
            filtered_params = {k: v[0] for k, v in query_params.items() if k in ["lang", "order"]}
            new_query = urlencode(filtered_params)
            sub_category_url = parsed_url._replace(query=new_query).geturl()

            categories.append({
                "top_category": top_category_name,
                "sub_category": sub_category_name,
                "category_url": sub_category_url
            })

    # 去重
    unique_categories = []
    seen_urls = set()
    for cat in categories:
        if cat["category_url"] not in seen_urls:
            seen_urls.add(cat["category_url"])
            unique_categories.append(cat)
    return unique_categories


# -------------------------- 抓取分类项目页面 --------------------------
def fetch_category_projects(category_url, language=None, order="starred"):
    parsed_url = urlparse(category_url)
    query_params = parse_qs(parsed_url.query)

    if language:
        query_params["lang"] = [language]
    if order:
        query_params["order"] = [order]

    new_query = urlencode(query_params, doseq=True)
    final_url = parsed_url._replace(query=new_query).geturl()

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0 Safari/537.36",
        "Referer": BASE_URL
    }

    try:
        response = requests.get(final_url, headers=headers, timeout=10)
        response.raise_for_status()
        return response.text
    except Exception as e:
        print(f"❌ 抓取失败 {final_url}：{str(e)}")
        return ""


# -------------------------- 解析项目页面 --------------------------
def parse_projects(html):
    soup = BeautifulSoup(html, "html.parser")
    items = []
    repo_items = soup.select("div.ui.relaxed.divided.items > div.item")

    for repo in repo_items:
        # 项目名和URL
        name_tag = repo.select_one("h3 a.title.project-namespace-path")
        repo_name = name_tag.text.strip() if name_tag else ""
        repo_url = urljoin(BASE_URL, name_tag["href"]) if (name_tag and "href" in name_tag.attrs) else ""

        # 作者
        author = repo.get("data-username", "").strip()
        if not author and "/" in repo_name:
            author = repo_name.split("/")[0].strip()

        # 描述
        desc_tag = repo.select_one("div.project-desc.mb-1")
        description = desc_tag.text.strip() if desc_tag else ""

        # 编程语言
        lang_tag = repo.select_one("a.project-language.project-item-bottom__item")
        language = lang_tag.text.strip() if lang_tag else ""

        # Stars
        stars_tag = repo.select_one("div.stars-count")
        stars = int(stars_tag.get("data-count", 0)) if (stars_tag and stars_tag.get("data-count")) else 0

        # Forks
        forks_tag = repo.select_one("div.forks-count")
        forks = int(forks_tag.get("data-count", 0)) if (forks_tag and forks_tag.get("data-count")) else 0

        # 项目图片
        img_tag = repo.select_one("img.js-popover-card")
        project_image = img_tag["src"] if (img_tag and "src" in img_tag.attrs) else ""
        if project_image.startswith("https://gitee.com"):
            project_image="/giteefavicon.ico"
        items.append({
            "repo_name": repo_name,
            "author": author,
            "stars": stars,
            "forks": forks,
            "language": language,
            "description": description,
            "url": repo_url,
            "project_image": project_image
        })
    return items


# -------------------------- 存储到 MySQL（线程安全） --------------------------
def store_to_db(projects, top_category, sub_category, order_by):
    if not projects:
        return
    with db_lock:
        conn = pymysql.connect(
            host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASS, database=DB_NAME,
            charset="utf8mb4"
        )
        cursor = conn.cursor()
        scraped_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        for item in projects:
            if not item["repo_name"]:
                continue
            if not item["sub_category"]:
                continue
            try:
                cursor.execute('''
                               INSERT INTO t_gitee_trending
                               (repo_name, author, stars, language, description, url,
                                top_category, sub_category, project_image, order_by, scraped_at)
                               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                               ''', (
                                   item["repo_name"], item["author"], item["stars"], item["language"],
                                   item["description"], item["url"], top_category, sub_category,
                                   item["project_image"], order_by, scraped_time
                               ))
            except pymysql.err.IntegrityError:
                continue
        conn.commit()
        conn.close()
    print(f"✅ 已存储 {len(projects)} 个项目 ({top_category} > {sub_category})")



# -------------------------- Redis缓存 --------------------------
"""def get_redis_client():
    try:
        r = redis.StrictRedis(host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB)
        r.ping()
        return r
    except Exception as e:
        print(f"⚠️ Redis连接失败：{str(e)}")
        return None


def cache_to_redis(r, category_key, projects):
    if not r or not projects:
        return
    cache_key = f"gitee:category:{category_key}:{projects[0]['order_by']}"
    r.setex(cache_key, 1800, json.dumps(projects, ensure_ascii=False))"""


# -------------------------- 单个分类爬取任务（供线程池调用） --------------------------
def crawl_category_task(cat_info, language, order="starred"):
    """线程池中的单个任务：爬取一个分类"""
    try:
        # 1. 抓取页面
        html = fetch_category_projects(
            category_url=cat_info["category_url"],
            language=language,
            order=order
        )
        if not html:
            return cat_info, False

        # 2. 解析项目
        projects = parse_projects(html)
        if not projects:
            print(f"ℹ️ 分类 {cat_info['top_category']} > {cat_info['sub_category']} 无项目数据")
            return cat_info, False

        # 3. 补充分类信息
        for proj in projects:
            proj["top_category"] = cat_info["top_category"]
            proj["sub_category"] = cat_info["sub_category"]
            proj["order_by"] = order

        # 4. 存储数据
        store_to_db(projects, cat_info["top_category"], cat_info["sub_category"], order)

        # 5. 缓存（可选）
        """r = get_redis_client()
        if r:
            category_key = f"{cat_info['top_category']}-{cat_info['sub_category']}"
            cache_to_redis(r, category_key, projects)"""

        time.sleep(1)  # 单个任务完成后延迟，避免请求过于密集
        return cat_info, True

    except Exception as e:
        print(f"❌ 分类任务失败 {cat_info['top_category']} > {cat_info['sub_category']}：{str(e)}")
        return (cat_info, False)


# -------------------------- 线程池批量爬取 --------------------------
def batch_crawl_with_threadpool(category_list, language, order="starred",
                                filter_keywords=None, max_workers=5):
    """
    使用线程池批量爬取分类
    :param order:
    :param max_workers: 线程池大小（建议5-10，避免反爬）
    """
    # 筛选分类
    if filter_keywords:
        filtered_cats = [
            cat for cat in category_list
            if any(keyword in cat["top_category"] or keyword in cat["sub_category"]
                   for keyword in filter_keywords)
        ]
        print(f"筛选后待爬分类：{len(filtered_cats)} 个（原分类：{len(category_list)}）")
    else:
        filtered_cats = category_list
        print(f"待爬分类总数：{len(filtered_cats)} 个")

    # 初始化线程池
    success_count = 0
    fail_count = 0

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # 提交所有任务
        futures = [
            executor.submit(
                crawl_category_task,
                cat_info=cat,
                language=language,
                order=order
            ) for cat in filtered_cats
        ]

        # 监控任务完成情况
        for future in as_completed(futures):
            cat_info, success = future.result()
            if success:
                success_count += 1
            else:
                fail_count += 1
            print(f"进度：成功 {success_count} / 失败 {fail_count} / 总 {len(filtered_cats)}")

    print(f"\n===== 爬取完成 =====")
    print(f"总分类：{len(filtered_cats)}，成功：{success_count}，失败：{fail_count}")


# -------------------------- 入口函数 --------------------------
if __name__ == "__main__":
    # 1. 初始化数据库
    init_db()

    # 2. 获取分类列表
    category_list_url = "https://gitee.com/explore/all?order=starred"  # 只保留排序参数
    try:
        category_html = requests.get(
            category_list_url,
            headers={
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0 Safari/537.36"}
        ).text
        all_categories = extract_all_categories(category_html)
        print(f"成功提取 {len(all_categories)} 个分类")
    except Exception as e:
        print(f"获取分类列表失败：{str(e)}")
        exit(1)

    # 3. 线程池批量爬取（示例：爬取程序开发和人工智能分类的Java项目）
    batch_crawl_with_threadpool(
        category_list=all_categories,
        language=language,
        order="starred",
        filter_keywords=[category],  # 可删除此参数爬全部分类
        max_workers=5  # 线程数（根据网络情况调整）
    )
