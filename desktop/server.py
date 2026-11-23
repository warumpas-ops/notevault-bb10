import os
import sys
import json
import socket
import webbrowser
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse

PORT = 5050
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.path.join(BASE_DIR, "static")
NOTES_DIR = os.path.join(os.path.expanduser("~"), "NoteVault")

os.makedirs(STATIC_DIR, exist_ok=True)
os.makedirs(NOTES_DIR, exist_ok=True)

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

LOCAL_IP = get_local_ip()

def load_all_notes():
    notes = []
    if not os.path.exists(NOTES_DIR):
        return notes
    for fname in os.listdir(NOTES_DIR):
        if fname.endswith(".json"):
            fpath = os.path.join(NOTES_DIR, fname)
            try:
                with open(fpath, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    notes.append(data)
            except Exception:
                pass
    notes.sort(key=lambda x: x.get("updatedAt", 0), reverse=True)
    return notes

def save_note(note_data):
    note_id = note_data.get("id")
    if not note_id:
        return False
    fpath = os.path.join(NOTES_DIR, f"{note_id}.json")
    with open(fpath, "w", encoding="utf-8") as f:
        json.dump(note_data, f, indent=2, ensure_ascii=False)
    return True

def delete_note(note_id):
    fpath = os.path.join(NOTES_DIR, f"{note_id}.json")
    if os.path.exists(fpath):
        os.remove(fpath)
        return True
    return False

class NoteVaultHandler(BaseHTTPRequestHandler):
    def _send_json(self, data, code=200):
        body = json.dumps(data).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/api/config":
            self._send_json({
                "syncUrl": f"http://{LOCAL_IP}:{PORT}",
                "notesDir": NOTES_DIR
            })
            return

        if path == "/api/notes":
            notes = load_all_notes()
            self._send_json(notes)
            return

        if path == "/icon.png":
            file_path = os.path.join(STATIC_DIR, "icon.png")
            if os.path.exists(file_path):
                with open(file_path, "rb") as f:
                    content = f.read()
                self.send_response(200)
                self.send_header("Content-Type", "image/png")
                self.send_header("Content-Length", str(len(content)))
                self.end_headers()
                self.wfile.write(content)
                return

        # Serve static HTML/assets
        if path == "/app":
            file_path = os.path.join(STATIC_DIR, "bb_app.html")
            if os.path.exists(file_path):
                with open(file_path, "rb") as f:
                    content = f.read()
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(content)))
                self.end_headers()
                self.wfile.write(content)
                return

        if path == "/" or path == "/index.html":
            file_path = os.path.join(STATIC_DIR, "index.html")
            if os.path.exists(file_path):
                with open(file_path, "rb") as f:
                    content = f.read()
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(content)))
                self.end_headers()
                self.wfile.write(content)
                return

        self.send_error(404, "Not Found")

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        content_len = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_len).decode("utf-8") if content_len > 0 else "{}"
        try:
            payload = json.loads(body)
        except Exception:
            payload = {}

        if path == "/api/note":
            save_note(payload)
            self._send_json({"status": "ok"})
            return

        if path == "/api/sync":
            # BlackBerry Wi-Fi Sync endpoint
            incoming_notes = payload.get("notes", [])
            pc_notes = {n.get("id"): n for n in load_all_notes()}
            incoming_map = {n.get("id"): n for n in incoming_notes}

            # 1. Update PC with newer/missing notes from BlackBerry
            for in_note in incoming_notes:
                n_id = in_note.get("id")
                if not n_id:
                    continue
                pc_note = pc_notes.get(n_id)
                if not pc_note or in_note.get("updatedAt", 0) > pc_note.get("updatedAt", 0):
                    save_note(in_note)
                    pc_notes[n_id] = in_note

            # 2. Determine notes to send back to BlackBerry
            server_notes = []
            for pc_id, pc_note in pc_notes.items():
                in_note = incoming_map.get(pc_id)
                if not in_note or pc_note.get("updatedAt", 0) > in_note.get("updatedAt", 0):
                    server_notes.append(pc_note)

            self._send_json({
                "status": "ok",
                "synced": len(incoming_notes),
                "serverNotes": server_notes
            })
            return

        if path == "/api/usb-sync":
            target_folder = payload.get("folder", "").strip()
            if not target_folder or not os.path.exists(target_folder):
                self._send_json({"status": "error", "message": "Folder does not exist"}, 400)
                return

            # Bidirectional diff with folder
            pushed = 0
            pulled = 0
            pc_notes = {n.get("id"): n for n in load_all_notes()}
            device_notes = {}

            for fname in os.listdir(target_folder):
                if fname.endswith(".json"):
                    fpath = os.path.join(target_folder, fname)
                    try:
                        with open(fpath, "r", encoding="utf-8") as f:
                            d = json.load(f)
                            device_notes[d.get("id")] = d
                    except Exception:
                        pass

            # Sync from device to PC
            for d_id, d_note in device_notes.items():
                pc_note = pc_notes.get(d_id)
                if not pc_note or d_note.get("updatedAt", 0) > pc_note.get("updatedAt", 0):
                    save_note(d_note)
                    pc_notes[d_id] = d_note
                    pulled += 1

            # Sync from PC to device
            for p_id, p_note in pc_notes.items():
                d_note = device_notes.get(p_id)
                if not d_note or p_note.get("updatedAt", 0) > d_note.get("updatedAt", 0):
                    dest = os.path.join(target_folder, f"{p_id}.json")
                    with open(dest, "w", encoding="utf-8") as f:
                        json.dump(p_note, f, indent=2, ensure_ascii=False)
                    pushed += 1

            self._send_json({"status": "ok", "pushed": pushed, "pulled": pulled})
            return

        self.send_error(404, "Not Found")

    def do_DELETE(self):
        parsed = urlparse(self.path)
        path = parsed.path
        if path.startswith("/api/note/"):
            note_id = path.replace("/api/note/", "")
            delete_note(note_id)
            self._send_json({"status": "ok"})
            return
        self.send_error(404, "Not Found")

    def log_message(self, format, *args):
        # Quiet logger
        return

def run_server():
    server = HTTPServer(("0.0.0.0", PORT), NoteVaultHandler)
    print("=" * 60)
    print("           NOTEVAULT PC COMPANION & WI-FI SYNC SERVER")
    print("=" * 60)
    print(f"  Web Interface:           http://localhost:{PORT}")
    print(f"  BlackBerry Sync Address: http://{LOCAL_IP}:{PORT}")
    print(f"  Notes Storage Folder:    {NOTES_DIR}")
    print("=" * 60)
    print("  Keep this window open while syncing with your BlackBerry.")
    print("=" * 60)

    try:
        webbrowser.open(f"http://localhost:{PORT}")
    except Exception:
        pass

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped.")

if __name__ == "__main__":
    run_server()
