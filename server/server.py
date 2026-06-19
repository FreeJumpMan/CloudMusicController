"""
网易云音乐 Windows 遥控服务端
=============================
用法（以管理员身份运行）：
    pip install -r requirements.txt
    python server.py

然后在同一局域网下的手机浏览器中访问 http://电脑IP:8888
"""

import json
import socket
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse

try:
    import keyboard
except ImportError:
    print("请先安装依赖: pip install -r requirements.txt")
    print("并确保以管理员身份运行此脚本。")
    raise SystemExit(1)

HOST = "0.0.0.0"
PORT = 8888

# 网易云音乐全局快捷键映射
# 可以在网易云音乐「设置 → 快捷键」中查看/修改
COMMANDS = {
    "play_pause":  "ctrl+alt+p",      # 播放/暂停
    "next":        "ctrl+alt+right",  # 下一曲
    "prev":        "ctrl+alt+left",   # 上一曲
    "vol_up":      "ctrl+alt+up",     # 音量加
    "vol_down":    "ctrl+alt+down",   # 音量减
    "like":        "ctrl+alt+l",      # 喜欢歌曲
    "toggle_mode": "ctrl+alt+m",      # mini/完整模式
    "toggle_lyrics": "ctrl+alt+d",    # 打开/关闭歌词
}

# ── 手机端 Web 控制面板（内嵌在代码中，无需额外文件） ──
WEB_PAGE = """\
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<title>网易云遥控器</title>
<style>
  *{margin:0;padding:0;box-sizing:border-box}
  body{font-family:-apple-system,system-ui,sans-serif;background:#121212;color:#fff;
       display:flex;justify-content:center;padding:24px 16px;min-height:100vh}
  .container{max-width:400px;width:100%}
  h1{text-align:center;font-size:22px;margin:20px 0 8px;color:#1db954;font-weight:700}
  .sub{text-align:center;font-size:13px;color:#666;margin-bottom:24px}
  .card{background:#1e1e1e;border-radius:20px;padding:20px;margin-bottom:16px}
  .row{display:grid;gap:12px}
  .row-3{grid-template-columns:1fr 1fr 1fr}
  .row-2{grid-template-columns:1fr 1fr}
  .btn{display:flex;flex-direction:column;align-items:center;justify-content:center;
       padding:20px 8px;border-radius:16px;border:none;color:#fff;text-decoration:none;
       cursor:pointer;transition:all .15s;font-size:13px;gap:6px;user-select:none;
       -webkit-tap-highlight-color:transparent}
  .btn:active{transform:scale(.92);opacity:.7}
  .btn svg{width:32px;height:32px;fill:currentColor}
  .btn-green{background:#1db954}
  .btn-gray{background:#333}
  .btn-red{background:#e74c3c}
  .btn-full{grid-column:1/-1;flex-direction:row;gap:8px;padding:16px}
  .badge{display:inline-block;background:#1db954;color:#fff;border-radius:20px;
         padding:4px 14px;font-size:12px;margin-top:16px}
  .footer{text-align:center;font-size:12px;color:#444;margin:24px 0}
</style>
</head>
<body>
<div class="container">
  <h1>🎵 网易云遥控器</h1>
  <div class="sub" id="status">正在连接...</div>
  <div class="card">
    <div class="row row-3">
      <a href="/prev" class="btn btn-gray" onclick="cmd(event,'prev')">
        <svg viewBox="0 0 24 24"><path d="M6 6h2v12H6V6zm3.5 6l8.5 6V6l-8.5 6z"/></svg>上一曲
      </a>
      <a href="/play_pause" class="btn btn-green" onclick="cmd(event,'play_pause')">
        <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>播放/暂停
      </a>
      <a href="/next" class="btn btn-gray" onclick="cmd(event,'next')">
        <svg viewBox="0 0 24 24"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>下一曲
      </a>
    </div>
    <div class="row row-2" style="margin-top:12px">
      <a href="/vol_down" class="btn btn-gray" onclick="cmd(event,'vol_down')">
        <svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/></svg>音量－
      </a>
      <a href="/vol_up" class="btn btn-gray" onclick="cmd(event,'vol_up')">
        <svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/></svg>音量＋
      </a>
    </div>
    <div class="row" style="margin-top:12px">
      <a href="/like" class="btn btn-red btn-full" onclick="cmd(event,'like')">♥ 喜欢</a>
    </div>
    <div class="row row-2" style="margin-top:12px">
      <a href="/toggle_mode" class="btn btn-gray" onclick="cmd(event,'toggle_mode')">⛶ 切换模式</a>
      <a href="/toggle_lyrics" class="btn btn-gray" onclick="cmd(event,'toggle_lyrics')">📃 歌词</a>
    </div>
  </div>
  <div class="footer" id="ipInfo"></div>
</div>
<script>
function cmd(e,action){e.preventDefault();fetch('/'+action).catch(function(){})}
fetch('/local_ip').then(function(r){return r.text()}).then(function(ip){
  document.getElementById('status').textContent='已连接 · 点击按钮控制';
  document.getElementById('ipInfo').textContent='电脑IP: '+ip+' :""" + str(PORT) + """';
}).catch(function(){
  document.getElementById('status').textContent='⚠ 连接失败';
});
</script>
</body>
</html>"""


class RequestHandler(BaseHTTPRequestHandler):
    """处理手机端发来的 HTTP 请求"""

    def _ok(self, body: dict):
        self.send_response(200)
        self._headers()
        self.wfile.write(json.dumps(body, ensure_ascii=False).encode())

    def _err(self, msg: str, code=400):
        self.send_response(code)
        self._headers()
        self.wfile.write(json.dumps({"ok": False, "error": msg}, ensure_ascii=False).encode())

    def _headers(self):
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()

    def do_GET(self):
        path = urlparse(self.path).path.strip("/")

        # 网页控制面板
        if path in ("", "index.html"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(WEB_PAGE.encode())
            return

        # 查询本机局域网 IP（供页面显示）
        if path == "local_ip":
            ip = self._get_local_ip()
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.end_headers()
            self.wfile.write(ip.encode())
            return

        # 执行控制指令
        if path in COMMANDS:
            try:
                keyboard.send(COMMANDS[path])
                print(f"[执行] {path}")
                self._ok({"ok": True, "command": path})
            except Exception as e:
                self._err(str(e))
        else:
            self._err(f"未知指令: {path}")

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.end_headers()

    def log_message(self, fmt: str, *args):
        print(f"  [{self.address_string()}] {fmt % args}")

    @staticmethod
    def _get_local_ip() -> str:
        """获取本机局域网 IP 地址"""
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(("8.8.8.8", 80))
            return s.getsockname()[0]
        except OSError:
            return "无法获取"
        finally:
            s.close()


def main():
    print("=" * 54)
    print("  网易云音乐 Windows 遥控服务端")
    print("=" * 54)
    print(f"""
  监听地址: http://0.0.0.0:{PORT}
  注: 必须「以管理员身份运行」才能模拟键盘操作

  在同一 WiFi 下用手机浏览器访问:
  ┌─────────────────────────────┐
  │  http://本机IP:{PORT}  │
  └─────────────────────────────┘

  按 Ctrl+C 停止服务
    """)

    server = HTTPServer((HOST, PORT), RequestHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n服务已停止。")
        server.server_close()


if __name__ == "__main__":
    main()
