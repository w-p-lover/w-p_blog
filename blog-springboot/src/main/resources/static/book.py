import os

import requests
from bs4 import BeautifulSoup
import time
import csv
import pandas as pd
import random
from urllib.parse import quote
from download import down_main
# -------------------------- 1. 配置 --------------------------
BASE_LIST_URL = "https://8080txt.com/hot/index_{}.html"
FIRST_PAGES = 6
FINAL_PAGES = 10
TOTAL_PAGES = FINAL_PAGES - FIRST_PAGES + 1

DELAY_MIN = 1
DELAY_MAX = 3
CSV_FILENAME = "80电子书下载链接.csv"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    "Referer": "https://8080txt.com/",
    "Accept-Language": "zh-CN,zh;q=0.9",
    "Connection": "keep-alive"
}


# -------------------------- 2. 工具函数 --------------------------
def sleep_delay():
    """随机延迟，防止被封"""
    time.sleep(random.uniform(DELAY_MIN, DELAY_MAX))


def get_page_soup(url, timeout=15):
    """返回BeautifulSoup对象，失败返回None"""
    try:
        resp = requests.get(url, headers=HEADERS, timeout=timeout)
        resp.encoding = 'utf-8'
        return BeautifulSoup(resp.text, 'html.parser')
    except requests.Timeout:
        print(f"❌ 超时: {url}")
    except requests.ConnectionError:
        print(f"❌ 连接失败: {url}")
    except Exception as e:
        print(f"❌ 异常: {url}, {str(e)[:50]}")
    return None


def get_download_jump_url(detail_url):
    """解析详情页，获取下载跳转页URL"""
    soup = get_page_soup(detail_url)
    if not soup:
        return "详情页访问失败"

    down_div = soup.find("div", class_="down")
    if not down_div:
        return "详情页未找到下载入口"

    a_tag = down_div.find("a", string=lambda text: text and "进入小说下载地址" in text)
    if not a_tag or "href" not in a_tag.attrs:
        return "详情页未找到跳转链接"

    href = a_tag["href"]
    return href if href.startswith("http") else "https://8080txt.com" + href


def get_real_txt_links(jump_url):
    """解析下载跳转页，获取真实TXT下载链接"""
    if "失败" in jump_url or "未找到" in jump_url:
        return jump_url
    soup = get_page_soup(jump_url)
    if not soup:
        return "下载页访问失败"

    links = [
        a['href'] for div in soup.find_all("div", class_="downlist")
        for a in div.find_all("a")
        if
        a.get('href', '').startswith(("https://down.8080txt.com/", "https://down.txt8080.com/")) and a['href'].endswith(
            ".txt")
    ]

    links = list(set(links))
    return links if links else "未找到有效TXT下载链接"


def parse_list_novel(novel_div):
    """解析单个小说div，返回字典"""
    info = {}

    # 标题 + 详情页URL
    title_tag = novel_div.find("h4").find("a")
    title = title_tag.get_text(strip=True) if title_tag else "未知标题"
    start = title.find("《") + 1
    end = title.find("》")
    info["标题"] = title[start:end]
    detail_href = title_tag["href"] if (title_tag and "href" in title_tag.attrs) else ""
    info["小说详情页URL"] = "https://8080txt.com" + detail_href if detail_href else "无"

    # 下载量
    download_tag = novel_div.find("h4").find("span", class_="fr")
    info["下载量"] = download_tag.get_text(strip=True) if download_tag else "未知"

    # 类别 + 作者
    xm_links = novel_div.select("p.xm a")
    info["类别"] = xm_links[0].get_text(strip=True) if len(xm_links) > 0 else "未知"
    info["作者"] = xm_links[-1].get_text(strip=True) if len(xm_links) > 1 else "未知"

    # 简介
    intro_p = novel_div.select("p")[1] if len(novel_div.select("p")) >= 2 else None
    if intro_p:
        text = intro_p.get_text(separator=" ", strip=True)
        info["简介"] = text.split("全集TXT下载")[0] if "全集TXT下载" in text else text
    else:
        info["简介"] = "无"

    # 封面
    cover_tag = novel_div.select_one(".pic img")
    info["封面URL"] = cover_tag["src"] if cover_tag and "src" in cover_tag.attrs else "无"

    # 发布时间、状态、格式、大小
    info_tag = novel_div.select_one("p.l")
    if info_tag:
        parts = [p.strip() for p in info_tag.get_text(separator="|", strip=True).split("|")]
        info["发布时间"] = parts[0].replace("发布时间：", "") if len(parts) >= 1 else "未知"
        info["文件格式"] = parts[2].replace("小说格式：", "") if len(parts) >= 2 else "未知"
        info["文件大小"] = parts[3].replace("小说大小：", "") if len(parts) >= 3 else "未知"
    else:
        info["发布时间"] = info["小说状态"] = info["文件格式"] = info["文件大小"] = "未知"

    # 下载跳转页 + 真实TXT链接
    info["下载跳转页URL"] = get_download_jump_url(info["小说详情页URL"])
    sleep_delay()
    links = get_real_txt_links(info["下载跳转页URL"])

    info["TXT下载链接1"] = links[0]
    info["TXT下载链接2"] = links[1]
    sleep_delay()

    return info


def save_to_csv(data):
    fields = [
        "标题", "作者", "类别", "下载量", "简介", "小说详情页URL",
        "下载跳转页URL", "TXT下载链接1", "TXT下载链接2", "封面URL",
        "发布时间", "小说状态", "文件格式", "文件大小"
    ]

    existing_titles = set()
    file_exists = os.path.exists(CSV_FILENAME)
    file_empty = True

    if file_exists:
        file_empty = os.path.getsize(CSV_FILENAME) == 0
        if not file_empty:
            try:
                df = pd.read_csv(CSV_FILENAME, encoding="utf-8-sig")
                existing_titles = set(df["标题"].astype(str))
            except Exception as e:
                print(f"⚠️ 读取现有 CSV 失败: {e}")

    # 过滤掉标题已存在的记录
    filtered_data = [
        row for row in data
        if str(row.get("标题", "")).strip() not in existing_titles
    ]
    if not filtered_data:
        print("没有新的数据需要写入。")
        return

    # 追加写入
    with open(CSV_FILENAME, "a", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        if not file_exists or file_empty:
            writer.writeheader()
        writer.writerows(filtered_data)

    print(f"新增写入 {len(filtered_data)} 条记录。")


def main_writer():
    print("===== 按作者抓取 80电子书 下载排行榜 =====")
    first_time = time.time()
    all_data = []

    # 这里放你想要抓取的作者列表
    authors = [
        "辰东",
    ]

    for author in authors:
        start_time = time.time()
        url = f"https://8080txt.com/writer/{quote(author)}/"
        print(f"\n===== 作者：{author} -> {url} =====")

        soup = get_page_soup(url)
        if not soup:
            print(f"⚠️ {author} 页面爬取失败，跳过")
            sleep_delay()
            continue

        novels = soup.find_all("div", class_="slist")
        if not novels:
            print(f"⚠️ {author} 未找到小说数据")
            sleep_delay()
            continue

        for idx, novel_div in enumerate(novels, 1):
            try:
                info = parse_list_novel(novel_div)
                all_data.append(info)
                down_time = time.time()
                print(f"✅ {author} - {idx}：《{info['标题']}》 "
                      f"链接：{info['TXT下载链接1'][:50]}... "
                      f"耗时:{round(down_time - start_time, 2)}s")
                start_time = down_time
            except Exception as e:
                print(f"❌ {author} 第{idx}本解析失败: {e}")
            sleep_delay()

    save_to_csv(all_data)
    end_time = time.time()
    print(f"\n===== 全部作者抓取完成，共 {len(all_data)} 本小说 =====")
    print(f"总耗时: {round(end_time - first_time, 2)} 秒")
    print(f"数据保存到: {CSV_FILENAME}")


# -------------------------- 3. 主爬虫 --------------------------
def main():
    print("===== 开始爬取 80电子书 下载排行榜 =====")
    first_time = time.time()
    start_time = first_time
    all_data = []

    for page in range(FIRST_PAGES, FINAL_PAGES + 1):
        url = f"https://8080txt.com/page/{page}/"
        page = page
        url = "https://8080txt.com/hot/" if page == 1 else BASE_LIST_URL.format(page)
        print(f"\n===== 第{page}/{FINAL_PAGES}页: {url} =====")

        soup = get_page_soup(url)
        if not soup:
            print(f"⚠️ 第{page}页列表页爬取失败，跳过")
            sleep_delay()
            continue

        novels = soup.find_all("div", class_="slist")
        if not novels:
            print(f"⚠️ 第{page}页未找到小说数据")
            sleep_delay()
            continue

        for idx, novel_div in enumerate(novels, 1):
            try:
                info = parse_list_novel(novel_div)
                all_data.append(info)
                down_time = time.time()
                print(f"✅ {idx}：《{info['标题']}》"
                      f"链接：{info['TXT下载链接1'][:50]}..."
                      f"耗时:{round(down_time - start_time, 2)}s")
                start_time = down_time
            except Exception as e:
                print(f"❌ 第{idx}本小说解析失败: {e}")
            sleep_delay()

    save_to_csv(all_data)
    end_time = time.time()
    print(f"\n===== 爬取完成！共 {len(all_data)} 本小说 =====")
    print(f"总耗时: {round(end_time - first_time, 2)} 秒")
    print(f"数据保存到: {CSV_FILENAME}")


if __name__ == "__main__":
    # main()
    main_writer()
    down_main()
