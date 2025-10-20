import csv
import re
import time
import sys
import requests
from bs4 import BeautifulSoup
import os

# 设置基本URL和下载目录
DOWNLOAD_DIR = r"D:\IdeaProjects\blog\blog-springboot\src\main\resources\static\book"
CSV_FILE = "知轩藏书下载连接.csv"
sys.stdout = open(sys.stdout.fileno(), mode='w', buffering=1, encoding='utf-8')
# 创建下载目录，如果不存在
if not os.path.exists(DOWNLOAD_DIR):
    os.makedirs(DOWNLOAD_DIR)

# 将数据保存到 CSV 文件
def save_to_csv(data):
    file_exists = os.path.isfile(CSV_FILE)

    with open(CSV_FILE, mode='a', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)

        # 如果文件不存在，写入表头
        if not file_exists:
            writer.writerow([    '书名', '作者', '内容简介', '字数', '状态', '校对版', 'TXT大小', '分类', '标签', '下载链接'    ])

        # 写入数据
        writer.writerow(data)
    print(f"已保存到CSV: {data}")


# 第一步：请求并解析主页面
def get_novel_links(url):
    response = requests.get(url)
    response.encoding = 'utf-8'
    soup = BeautifulSoup(response.text, 'html.parser')

    # 提取每个小说详情页面的链接
    novel_links = []
    for a_tag in soup.find_all('a', href=True):
        if '/book/' in a_tag['href']:
            novel_links.append("https://zxcs.zip" + a_tag['href'])
    return novel_links


# 第二步：从小说详情页面获取下载链接
def get_download_link(novel_url):
    response = requests.get(novel_url)
    response.encoding = 'utf-8'
    soup = BeautifulSoup(response.text, 'html.parser')

    # 找到下载链接
    download_link = soup.find('a', id='downloadtxt', href=True)
    author = None
    description = None
    word_count = None
    status = None
    proofing = None
    txt_size = None
    category = None
    tags = None
    book_title = None

    if download_link:
        # 使用正则提取书名中的作者
        match = re.search(r'作者：([^/]+)（$', download_link['href'])
        if match:
            author = match.group(1)
        match = re.search(r'/([^/]+).txt$', download_link['href'])
        if match:
            book_title = match.group(1)  # 提取正则表达式匹配到的部分

        # 获取其他信息（通过解析<p>标签）
        p_tags = soup.find_all('p', class_='ng-star-inserted')

        # 解析出每个<p>标签中的内容
        for p_tag in p_tags:
            text = p_tag.get_text(strip=True)

            if "【作者】：" in text:
                author = text.split('：')[-1]
            elif "【内容简介】：" in text:
                description = text.split('：')[-1]
            elif "【字数】：" in text:
                word_count = text.split('：')[-1]
            elif "【状态】：" in text:
                status = text.split('：')[-1]
            elif "【校对】：" in text:
                proofing = text.split('：')[-1]
            elif "【TXT大小】：" in text:
                txt_size = text.split('：')[-1]
            elif "【分类】：" in text:
                category = text.split('：')[-1]
            elif "【标签】：" in text:
                tags = text.split('：')[-1]

        # 返回下载链接及其它相关信息
        return {
            "book_title": book_title,
            "download_link": download_link['href'],
            "author": author,
            "description": description,
            "word_count": word_count,
            "status": status,
            "proofing": proofing,
            "txt_size": txt_size,
            "category": category,
            "tags": tags
        }

    return None

def download_novel(download_url):
    # 发送请求获取文件内容
    response = requests.get(download_url)
    response.encoding = 'utf-8'

    # 使用正则表达式提取书名部分
    match = re.search(r'/([^/]+).txt$', download_url)
    if match:
        book_title = match.group(1)  # 提取正则表达式匹配到的部分
        print(f"书名：{book_title}")

        # 获取文件保存路径
        filename = os.path.join(DOWNLOAD_DIR, f"{book_title}.txt")

        # 检查文件是否已经存在
        if os.path.exists(filename):
            print(f"文件 {book_title}.txt 已经存在，跳过下载。")
        else:
            # 文件不存在时下载并保存
            with open(filename, 'wb') as file:
                file.write(response.content)
            print(f"下载完成: {filename}")
    else:
        print("无法从URL提取书名。")


def main():
    final_page = 2
    print(f"TOTAL_COUNT: {final_page * 10 - 10}")
    for page in range(1, final_page):  # 假设我们要抓取前4页
        base_url = f"https://zxcs.zip/rank/topdownload?page={page}"
        # 获取所有小说的链接
        novel_links = get_novel_links(base_url)

        # 遍历每个小说链接并下载
        for novel_url in novel_links:
            print(f"访问: {novel_url}")
            novel_info = get_download_link(novel_url)

            if novel_info:
                # 获取小说标题并下载
                title = novel_info['book_title']
                download_url = novel_info['download_link']
                author = novel_info['author']
                description = novel_info['description']
                word_count = novel_info['word_count']
                status = novel_info['status']
                proofing = novel_info['proofing']
                txt_size = novel_info['txt_size']
                category = novel_info['category']
                tags = novel_info['tags']

                download_novel(download_url)
                # save_to_csv(
                #     [title, author, description, word_count, status, proofing, txt_size, category, tags, download_url])
                time.sleep(3)  # 这里是3秒的延迟，你可以根据需求调整
            else:
                print(f"未找到下载链接: {novel_url}")

if __name__ == '__main__':
    main()
