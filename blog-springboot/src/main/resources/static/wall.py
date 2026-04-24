import argparse
import os
import random
import sys
import time

import requests
from lxml import etree
from requests.exceptions import RequestException

headers = {
    "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36"
}

DEFAULT_SAVE_DIR = r"D:\IdeaProjects\blog\blog-springboot\src\main\resources\static\Wallhaven"
SAVE_DIR = DEFAULT_SAVE_DIR
sys.stdout = open(sys.stdout.fileno(), mode="w", buffering=1, encoding="utf-8")


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--save-dir", default=DEFAULT_SAVE_DIR)
    parser.add_argument("--pages", type=int, default=3)
    return parser.parse_args()


def requests_with_retry(url, max_retries=3, delay_range=(2, 5)):
    retries = 0
    while retries < max_retries:
        try:
            resp = requests.get(url, headers=headers, timeout=10)
            if resp.status_code == 200:
                return resp
            if resp.status_code == 429:
                wait_time = random.uniform(delay_range[0] * (retries + 1), delay_range[1] * (retries + 1))
                print(f"请求过于频繁，{wait_time:.2f} 秒后重试")
                time.sleep(wait_time)
                retries += 1
                continue
            print(f"请求失败，状态码: {resp.status_code}")
            return None
        except RequestException as e:
            print(f"请求异常: {e}，准备重试")
            time.sleep(random.uniform(*delay_range))
            retries += 1

    print(f"达到最大重试次数，请求失败: {url}")
    return None


def download_image(final_url, max_retries=2):
    retries = 0
    while retries < max_retries:
        time.sleep(random.uniform(1, 2))
        pic_resp = requests_with_retry(final_url)
        if not pic_resp:
            retries += 1
            continue

        if pic_resp.content:
            return pic_resp.content

        retries += 1
        print("图片内容为空，准备重试")
        time.sleep(random.uniform(2, 3))

    print(f"多次重试后仍无法获取有效图片内容: {final_url}")
    return None


def get_html_info(page):
    url = f"https://wallhaven.cc/toplist?page={page}"
    print(f"正在抓取第 {page} 页")
    resp = requests_with_retry(url)
    if resp:
        return etree.HTML(resp.text)
    return None


def get_pic(resp_html):
    if resp_html is None:
        return

    os.makedirs(SAVE_DIR, exist_ok=True)
    existing_files = set(os.listdir(SAVE_DIR))

    pic_url_list = []
    lis = resp_html.xpath('//*[@id="thumbs"]/section[1]/ul/li')
    for li in lis:
        links = li.xpath("./figure/a/@href")
        if links:
            pic_url_list.append(links[0])

    for pic_url in pic_url_list:
        print(f"正在检查: {pic_url}")
        resp2 = requests_with_retry(pic_url)
        if not resp2:
            continue

        r_html2 = etree.HTML(resp2.text)
        try:
            pic_size = r_html2.xpath('//*[@id="showcase-sidebar"]/div/div[1]/h3/text()')[0].strip()
            final_url = r_html2.xpath('//*[@id="wallpaper"]/@src')[0]
        except IndexError:
            print("无法获取图片详情，跳过")
            continue

        file_name = f"{pic_size}{final_url[-10:]}"
        file_path = os.path.join(SAVE_DIR, file_name)
        if file_name in existing_files:
            print(f"{file_name} 已存在，跳过")
            continue

        pic = download_image(final_url)
        if pic:
            with open(file_path, mode="wb") as f:
                f.write(pic)
            existing_files.add(file_name)
            print(f"{file_name} 下载完成，累计 {len(existing_files)} 张")
        else:
            if os.path.exists(file_path):
                os.remove(file_path)
            print(f"{file_name} 下载失败，已跳过")


def main():
    args = parse_args()
    global SAVE_DIR
    SAVE_DIR = args.save_dir
    total_pages = max(1, args.pages)
    print(f"TOTAL_COUNT: {total_pages * 24}")
    sys.stdout.flush()

    first_time = time.time()
    for page in range(1, total_pages + 1):
        html = get_html_info(page)
        if html is not None:
            get_pic(html)
            print(f"=============== 第 {page} 页处理完毕 ===============")

        if page < total_pages:
            page_delay = random.uniform(3, 4)
            print(f"等待 {page_delay:.2f} 秒后处理下一页")
            time.sleep(page_delay)

    print(time.time() - first_time)


if __name__ == "__main__":
    main()
