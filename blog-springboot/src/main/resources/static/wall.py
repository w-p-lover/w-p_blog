import requests
from lxml import etree
import os
import time
import random
from requests.exceptions import RequestException
import sys

headers = {
    'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36'
}

SAVE_DIR = r"D:\IdeaProjects\blog\blog-springboot\src\main\resources\static\Wallhaven"
sys.stdout = open(sys.stdout.fileno(), mode='w', buffering=1, encoding='utf-8')


# 添加带重试机制的请求函数
def requests_with_retry(url, max_retries=3, delay_range=(2, 5)):
    """带重试机制的请求函数，处理429等错误"""
    retries = 0
    while retries < max_retries:
        try:
            resp = requests.get(url, headers=headers, timeout=10)
            if resp.status_code == 200:
                return resp
            elif resp.status_code == 429:
                # 遇到429错误，等待更长时间
                wait_time = random.uniform(delay_range[0] * (retries + 1), delay_range[1] * (retries + 1))
                print(f"请求过于频繁，将在{wait_time:.2f}秒后重试...")
                time.sleep(wait_time)
                retries += 1
            else:
                print(f"请求失败，状态码: {resp.status_code}")
                return None
        except RequestException as e:
            print(f"请求发生错误: {e}，将重试...")
            time.sleep(random.uniform(*delay_range))
            retries += 1

    print(f"达到最大重试次数，请求{url}失败")
    return None


# 下载图片并处理空内容情况
def download_image(final_url, max_retries=2):
    """下载图片并处理空内容情况，添加重试机制"""
    retries = 0
    while retries < max_retries:
        # 下载图片
        time.sleep(random.uniform(1, 2))  # 下载前的延迟
        pic_resp = requests_with_retry(final_url)

        if not pic_resp:
            retries += 1
            if retries < max_retries:
                print(f"图片下载失败，将进行第{retries + 1}次重试...")
                time.sleep(random.uniform(2, 3))
            continue

        # 检查内容是否为空
        if pic_resp.content:
            return pic_resp.content
        else:
            retries += 1
            if retries < max_retries:
                print(f"获取到空图片内容，将进行第{retries + 1}次重试...")
                time.sleep(random.uniform(2, 3))  # 空内容重试前等待更长时间

    print(f"达到最大重试次数，无法获取有效的图片内容: {final_url}")
    return None


# 定义获取每页html信息的函数
def get_html_info(page):
    url = f'https://wallhaven.cc/hot?page={page}'
    print(f"正在获取第{page}页的内容...")
    resp = requests_with_retry(url)
    if resp:
        return etree.HTML(resp.text)
    return None


def get_pic(resp_html):
    if resp_html is None:
        return

    # 创建保存目录
    if not os.path.exists(SAVE_DIR):
        os.makedirs(SAVE_DIR)
    # 获取已下载的文件名列表，用于检查重复
    existing_files = set(os.listdir(SAVE_DIR))

    pic_url_list = []
    lis = resp_html.xpath('//*[@id="thumbs"]/section[1]/ul/li')  # 获取该页所有缩略图包含的信息
    for li in lis:
        try:
            pic_url = li.xpath('./figure/a/@href')[0]  # 获取存放在缩略图信息中的原图页面网址
            pic_url_list.append(pic_url)
        except IndexError:
            print("无法获取图片链接，跳过...")
            continue

    for pic_url in pic_url_list:
        # 先获取图片信息以确定文件名
        print(f"正在检查: {pic_url}")
        resp2 = requests_with_retry(pic_url)
        if not resp2:
            continue

        r_html2 = etree.HTML(resp2.text)

        try:
            # 获取图片分辨率作为名称一部分
            pic_size = r_html2.xpath('//*[@id="showcase-sidebar"]/div/div[1]/h3/text()')[0].strip()
            # 获取原图下载地址
            final_url = r_html2.xpath('//*[@id="wallpaper"]/@src')[0]
        except IndexError:
            print("无法获取图片信息，跳过...")
            continue

        # 构建文件名并检查是否已存在
        file_name = f"{pic_size}{final_url[-10:]}"
        file_path = os.path.join(SAVE_DIR, file_name)

        if file_name in existing_files:
            print(f"{file_name} 已存在，跳过下载")
            continue  # 如果已存在，直接跳过后续步骤，包括延迟

        # 图片不存在才执行延迟和下载
        time.sleep(random.uniform(1, 2))
        print(f"正在处理: {pic_url}")

        # 下载图片（带空内容重试机制）
        pic = download_image(final_url)

        if pic:  # 确保获取到有效内容
            with open(file_path, mode='wb') as f:
                f.write(pic)

            # 更新已下载文件列表
            existing_files.add(file_name)
            # 显示下载进度
            print(f"{file_name} 下载完毕，已下载 {len(existing_files)} 张壁纸")
        else:
            # 清理可能创建的空文件
            if os.path.exists(file_path):
                os.remove(file_path)
            print(f"{file_name} 多次尝试后仍无法获取有效内容，已跳过")


def main():
    final_page = 3
    print(f"TOTAL_COUNT: {final_page * 24 - 24}")
    sys.stdout.flush()
    first_time = time.time()
    page_range = range(1, final_page)  # 爬取1-3页的壁纸
    for i in page_range:
        r = get_html_info(i)
        if r is not None:
            get_pic(r)
            print(f'===============第{i}页处理完毕=============')

        # 页面之间添加更长的随机延迟
        if i < len(page_range):
            page_delay = random.uniform(3, 4)
            print(f"等待{page_delay:.2f}秒后处理下一页...")
            time.sleep(page_delay)
    end_time = time.time()
    print(end_time - first_time)


if __name__ == '__main__':
    main()
