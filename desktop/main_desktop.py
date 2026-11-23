import os
import sys
import threading
import time
import webview

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

import server

def start_server():
    httpd = server.HTTPServer(("0.0.0.0", server.PORT), server.NoteVaultHandler)
    httpd.serve_forever()

if __name__ == "__main__":
    t = threading.Thread(target=start_server, daemon=True)
    t.start()
    time.sleep(0.4)

    # Native Windows window: zero lag, instant maximize & snap, hardware accelerated
    window = webview.create_window(
        title="NoteVault",
        url=f"http://localhost:{server.PORT}/app",
        width=960,
        height=720,
        resizable=True,
        frameless=False,
        background_color="#000000"
    )
    webview.start()
