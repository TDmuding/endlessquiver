# -*- coding: utf-8 -*-
"""极简 Minecraft RCON 客户端（纯标准库，无需第三方包）。

用法:
    python rcon.py <命令> [<命令> ...]
    python rcon.py --port 25575 --password test list

用途: 在没有客户端的情况下，用服务端控制台身份执行命令来验证模组注册情况。
"""
import socket
import struct
import sys

HOST = '127.0.0.1'
PORT = 25575
PASSWORD = 'test'


def pack(rid, typ, payload):
    data = struct.pack('<ii', rid, typ) + payload.encode('utf-8') + b'\x00\x00'
    return struct.pack('<i', len(data)) + data


def read_exact(sock, n):
    buf = b''
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise EOFError('connection closed')
        buf += chunk
    return buf


def read_packet(sock):
    (length,) = struct.unpack('<i', read_exact(sock, 4))
    body = read_exact(sock, length)
    rid, typ = struct.unpack('<ii', body[:8])
    payload = body[8:-2].decode('utf-8', 'replace')
    return rid, typ, payload


def run(commands, host=HOST, port=PORT, password=PASSWORD):
    sock = socket.create_connection((host, port), timeout=15)
    sock.sendall(pack(1, 3, password))
    rid, typ, _ = read_packet(sock)
    if rid == -1:
        print('[rcon] 认证失败')
        return 1
    out = []
    for i, cmd in enumerate(commands, start=2):
        sock.sendall(pack(i, 2, cmd))
        sock.settimeout(4)
        while True:
            try:
                _rid, _typ, payload = read_packet(sock)
            except (socket.timeout, EOFError):
                break
            out.append('>>> ' + cmd)
            out.append(payload)
    sock.close()
    print('\n'.join(out))
    return 0


def main(argv):
    global PORT, PASSWORD
    args = list(argv)
    while args and args[0].startswith('--'):
        flag = args.pop(0)
        if flag == '--port':
            PORT = int(args.pop(0))
        elif flag == '--password':
            PASSWORD = args.pop(0)
        else:
            print('未知参数 ' + flag)
            return 2
    # 支持 @文件：从文本文件逐行读取命令（避免 PowerShell 把命令里的双引号吃掉）
    commands = []
    for a in args:
        if a.startswith('@'):
            with open(a[1:], 'r', encoding='utf-8') as fh:
                for line in fh:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        commands.append(line)
        else:
            commands.append(a)
    if not commands:
        print(__doc__)
        return 2
    return run(commands)


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
