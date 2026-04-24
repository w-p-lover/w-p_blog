import argparse
import csv
import os
import re
import sys
import time

import requests
from bs4 import BeautifulSoup

DEFAULT_DOWNLOAD_DIR = r"D:\IdeaProjects\blog\blog-springboot\src\main\resources\static\book"
CSV_FILE = "知轩藏书下载链接.csv"

sys.stdout = open(sys.stdout.fileno(), mode="w", buffering=1, encoding="utf-8")


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--save-dir", default=DEFAULT_DOWNLOAD_DIR)
    parser.add_argument("--pages", type=int, default=1)
    return parser.parse_args()


def save_to_csv(data):
    file_exists = os.path.isfile(CSV_FILE)
    with open(CSV_FILE, mode="a", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        if not file_exists:
            writer.writerow(["书名", "作者", "内容简介", "字数", "状态", "校对", "TXT大小", "分类", "标签", "下载链接"])
        writer.writerow(data)
    print(f"已保存到 CSV: {data}")


def get_novel_links(url):
    response = requests.get(url, timeout=15)
    response.encoding = "utf-8"
    soup = BeautifulSoup(response.text, "html.parser")

    novel_links = []
    for a_tag in soup.find_all("a", href=True):
        if "/book/" in a_tag["href"]:
            novel_links.append("https://zxcs.zip" + a_tag["href"])
    return novel_links


def get_download_link(novel_url):
    response = requests.get(novel_url, timeout=15)
    response.encoding = "utf-8"
    soup = BeautifulSoup(response.text, "html.parser")

    download_link = soup.find("a", id="downloadtxt", href=True)
    author = None
    description = None
    word_count = None
    status = None
    proofing = None
    txt_size = None
    category = None
    tags = None
    book_title = None

    if not download_link:
        return None

    match = re.search(r"作者：([^/]+)）?", download_link["href"])
    if match:
        author = match.group(1)
    match = re.search(r"/([^/]+).txt$", download_link["href"])
    if match:
        book_title = match.group(1)

    p_tags = soup.find_all("p", class_="ng-star-inserted")
    for p_tag in p_tags:
        text = p_tag.get_text(strip=True)
        if "【作者】：" in text:
            author = text.split("：")[-1]
        elif "【内容简介】：" in text:
            description = text.split("：")[-1]
        elif "【字数】：" in text:
            word_count = text.split("：")[-1]
        elif "【状态】：" in text:
            status = text.split("：")[-1]
        elif "【校对】：" in text:
            proofing = text.split("：")[-1]
        elif "【TXT大小】：" in text:
            txt_size = text.split("：")[-1]
        elif "【分类】：" in text:
            category = text.split("：")[-1]
        elif "【标签】：" in text:
            tags = text.split("：")[-1]

    return {
        "book_title": book_title,
        "download_link": download_link["href"],
        "author": author,
        "description": description,
        "word_count": word_count,
        "status": status,
        "proofing": proofing,
        "txt_size": txt_size,
        "category": category,
        "tags": tags,
    }


def download_novel(download_url, download_dir):
    response = requests.get(download_url, timeout=30)
    response.encoding = "utf-8"

    match = re.search(r"/([^/]+).txt$", download_url)
    if not match:
        print("无法从 URL 提取书名")
        return

    book_title = match.group(1)
    print(f"书名：{book_title}")
    filename = os.path.join(download_dir, f"{book_title}.txt")
    if os.path.exists(filename):
        print(f"文件 {book_title}.txt 已存在，跳过下载")
        return

    with open(filename, "wb") as file:
        file.write(response.content)
    print(f"下载完成: {filename}")


def main():
    args = parse_args()
    download_dir = args.save_dir
    total_pages = max(1, args.pages)
    os.makedirs(download_dir, exist_ok=True)

    print(f"TOTAL_COUNT: {total_pages * 10}")
    for page in range(1, total_pages + 1):
        base_url = f"https://zxcs.zip/rank/topdownload?page={page}"
        novel_links = get_novel_links(base_url)

        for novel_url in novel_links:
            print(f"访问: {novel_url}")
            novel_info = get_download_link(novel_url)
            if not novel_info:
                print(f"未找到下载链接: {novel_url}")
                continue

            download_novel(novel_info["download_link"], download_dir)
            time.sleep(3)


if __name__ == "__main__":
    main()
