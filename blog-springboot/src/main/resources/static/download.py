import os
import random
import time
import datetime
import pandas as pd
import requests
import threading
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from concurrent.futures import ThreadPoolExecutor, as_completed

# 配置
csv_file = "80电子书下载链接.csv"
save_dir = "txt_books"
max_workers = 3  # 并发下载数量
download_delay = (0.5, 1.5)  # 下载间隔范围（秒）
chunk_size = 32768  # 分块大小（32KB）
progress_interval = 50  # 每下载多少块显示一次进度

os.makedirs(save_dir, exist_ok=True)

# 创建日志锁，确保日志输出顺序
log_lock = threading.Lock()


# 日志工具函数（带锁）
def log(message, task_id=None, level="INFO"):
    """带时间戳和线程锁的日志输出，保证顺序性"""
    with log_lock:  # 确保同一时间只有一个线程输出日志
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        task_prefix = f"[任务 {task_id}] " if task_id else ""
        print(f"[{timestamp}] [{level}] {task_prefix}{message}")


# 创建带重试机制和浏览器模拟的会话
def create_session():
    """创建带有重试机制和浏览器请求头的会话"""
    session = requests.Session()

    # 配置重试
    retry = Retry(
        total=3,
        read=3,
        connect=3,
        backoff_factor=0.5,
        status_forcelist=(500, 502, 503, 504, 429)
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("http://", adapter)
    session.mount("https://", adapter)

    # 模拟Edge浏览器请求头
    session.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36 Edg/116.0.1938.69",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive",
        "Upgrade-Insecure-Requests": "1",
        "Cache-Control": "max-age=0"
    })

    return session


# 清理文件名
def sanitize_filename(filename):
    """清理文件名中的非法字符"""
    illegal_chars = '/\\:*?"<>|'
    for char in illegal_chars:
        filename = filename.replace(char, "_")
    return filename


# 下载单个文件
def download_book(session, title, urls, save_dir, task_id):
    """下载单本书籍的函数，带任务ID和有序日志"""
    try:
        # 处理文件名
        if not title:
            title = f"未命名书籍_{hash(str(urls))}"
        filename = sanitize_filename(f"{title}.txt")
        save_path = os.path.join(save_dir, filename)

        # 检查是否已存在
        if os.path.exists(save_path):
            return (title, False, "文件已存在", task_id)

        log(f"准备下载: {title}", task_id)

        # 尝试下载链接
        for i, url in enumerate(urls, 1):
            try:
                # 随机延迟避免请求过于密集
                delay = random.uniform(*download_delay)
                log(f"下载前延迟 {delay:.2f}秒 (链接 {i})", task_id)
                time.sleep(delay)

                log(f"开始下载 (链接 {i}): {url}", task_id)
                start_time = time.time()
                response = session.get(url, timeout=(5, 30), stream=True)
                response.raise_for_status()

                # 获取文件大小信息
                content_length = response.headers.get('Content-Length')
                total_size = int(content_length) if content_length else None
                if total_size:
                    log(f"文件大小: {total_size / 1024 / 1024:.2f} MB", task_id)

                # 写入文件并显示进度
                downloaded_size = 0
                chunk_count = 0

                with open(save_path, "wb") as f:
                    for chunk in response.iter_content(chunk_size=chunk_size):
                        if chunk:
                            f.write(chunk)
                            downloaded_size += len(chunk)
                            chunk_count += 1

                            # 定期显示进度
                            if chunk_count % progress_interval == 0 and total_size:
                                progress = (downloaded_size / total_size) * 100
                                elapsed_time = time.time() - start_time
                                speed = downloaded_size / 1024 / elapsed_time if elapsed_time > 0 else 0
                                log(
                                    f"下载进度: {progress:.1f}% "
                                    f"({downloaded_size / 1024 / 1024:.2f} MB/{total_size / 1024 / 1024:.2f} MB) "
                                    f"- 速度: {speed:.2f} KB/s",
                                    task_id
                                )

                # 计算总下载时间和平均速度
                total_time = time.time() - start_time
                avg_speed = downloaded_size / 1024 / total_time if total_time > 0 else 0

                # 验证文件大小
                if content_length and os.path.getsize(save_path) != int(content_length):
                    os.remove(save_path)
                    raise Exception(
                        f"文件不完整 (实际: {os.path.getsize(save_path) / 1024:.2f} KB, "
                        f"预期: {int(content_length) / 1024:.2f} KB)"
                    )

                log(
                    f"下载完成 (链接 {i}) - "
                    f"耗时: {total_time:.2f}秒 - "
                    f"平均速度: {avg_speed:.2f} KB/s - "
                    f"保存路径: {save_path}",
                    task_id
                )
                return (title, True,
                        f"下载成功 (链接 {i}, 耗时 {total_time:.2f}秒, 速度 {avg_speed:.2f} KB/s)",
                        task_id)

            except Exception as e:
                if os.path.exists(save_path):
                    os.remove(save_path)
                error_msg = f"链接 {i} 下载失败: {str(e)}"
                log(error_msg, task_id, level="ERROR")
                if i < len(urls):
                    log(f"尝试下一个链接", task_id)
                    continue  # 尝试下一个链接
                return (title, False, error_msg, task_id)

    except Exception as e:
        error_msg = f"处理过程出错: {str(e)}"
        log(error_msg, task_id, level="ERROR")
        return (title, False, error_msg, task_id)


# 处理单条记录
def process_row(row, task_id):
    """处理DataFrame中的单行记录，带任务ID"""
    title = str(row["标题"]).strip()
    url1 = str(row.get("TXT下载链接1", "")).strip()
    url2 = str(row.get("TXT下载链接2", "")).strip()
    urls = [url for url in [url1, url2] if url and url.lower() != "nan"]

    if not urls:
        log(f"无有效下载链接", task_id)
        return (title, False, "无有效下载链接", task_id)

    # 为每个线程创建独立会话
    session = create_session()
    result = download_book(session, title, urls, save_dir, task_id)
    session.close()  # 关闭会话释放资源
    return result


# 主函数
def down_main():
    try:
        start_time = time.time()
        log("===== 开始电子书下载任务 =====")

        # 读取CSV
        log(f"读取CSV文件: {csv_file}")
        df = pd.read_csv(csv_file, encoding="utf-8-sig")
        total = len(df)
        log(f"成功读取CSV文件，共{total}条记录")

        # 并发处理
        success_count = 0
        fail_count = 0

        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            # 提交所有任务，每个任务分配唯一ID
            futures = [executor.submit(process_row, row, i + 1) for i, (_, row) in enumerate(df.iterrows())]

            # 处理结果
            for future in as_completed(futures):
                title, success, msg, task_id = future.result()
                status = "✅" if success else "❌"
                log(f"{status} 任务完成: {title} - {msg}", task_id)

                if success:
                    success_count += 1
                else:
                    fail_count += 1

        # 统计结果
        total_time = time.time() - start_time
        log("\n===== 下载任务完成 =====")
        log(f"总记录: {total}")
        log(f"成功: {success_count}")
        log(f"失败: {fail_count}")
        log(f"总耗时: {total_time:.2f}秒")
        log(f"平均每个文件下载时间: {total_time / success_count:.2f}秒 (基于成功数)")

    except Exception as e:
        log(f"程序出错: {str(e)}", level="ERROR")


if __name__ == "__main__":
    down_main()
