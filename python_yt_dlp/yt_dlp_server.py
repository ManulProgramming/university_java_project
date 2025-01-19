import socket
import yt_dlp
import threading
import json
import queue
import os
if not os.path.exists('videos/'):
    os.mkdir('videos')
task_queue = queue.Queue()
def handle_client(client_socket):
    try:
        data = client_socket.recv(4096)
        print(data)
        if data:
            raw=data.decode('utf-8').replace("'",'"')
            received_dict=json.loads(raw)
            print(f"Received from Java: {received_dict}")
            task_queue.put((client_socket,received_dict))
        else:
            print("No data received from client.")
    except Exception as e:
        print(f"Error: {e}")
def process_queue():
    while True:
        client_socket,task=task_queue.get()
        quality=task.get('quality')
        format=task.get('format')
        url=task.get('url')
        if 'best' in quality:
            yt_opts={
                'format': quality,
                'recode_video': format,
                'postprocessors': [{
                    'key': 'FFmpegVideoConvertor',
                    'preferedformat': format,
                }],
                'outtmpl': 'videos/%(title)s%(id)s.%(ext)s'
                }
        else:
            print('"res:'+quality+'"')
            yt_opts={
                'format_sort':["res:"+quality],
                'recode_video': format,
                'postprocessors': [{
                    'key': 'FFmpegVideoConvertor',
                    'preferedformat': format,
                }],
                'outtmpl': 'videos/%(title)s%(id)s.%(ext)s'
                }
        try:
            print(yt_opts)
            with yt_dlp.YoutubeDL(yt_opts) as ydl:
                info_dict = ydl.extract_info(url, download=False)
                video_url = info_dict.get("url", None)
                video_id = info_dict.get("id", None)
                video_title = info_dict.get('title', None)
                ydl.download(url)
            print("Done!")
            if task_queue.qsize()!=0:
                msg=f"Done,{video_title}"
            else:
                msg=f"Done queue empty,{video_title}"
            client_socket.sendall(msg.encode('utf-8'))
        except:
            print("!!!ERROR!!!")
            msg="Error,None"
            client_socket.sendall(msg.encode('utf-8'))
        client_socket.close()
        print("Connection closed.")
        task_queue.task_done()
def start_server(host='localhost', port=5000):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((host, port))
    server.listen(100)
    print(f"Python Server listening on {host}:{port}...")
    threading.Thread(target=process_queue, daemon=True).start()
    while True:
        client_socket, addr = server.accept()
        print(f"Connection established with {addr}")
        client_handler = threading.Thread(target=handle_client, args=(client_socket,))
        client_handler.start()

if __name__ == "__main__":
    start_server()
