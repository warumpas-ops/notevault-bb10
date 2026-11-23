package com.notevault.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncManager {

    private static final String PREFS_SYNC = "notevault_sync_prefs";
    private static final String KEY_SERVER_IP = "server_ip";

    public interface SyncListener {
        void onProgress(String message);
        void onSuccess(int pushed, int pulled);
        void onError(String error);
    }

    private final Context ctx;
    private final NoteRepository repo;

    public SyncManager(Context ctx, NoteRepository repo) {
        this.ctx = ctx;
        this.repo = repo;
    }

    public String getLastServerIp() {
        return ctx.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
                .getString(KEY_SERVER_IP, "192.168.1.100:5050");
    }

    public void saveServerIp(String ip) {
        ctx.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
                .edit().putString(KEY_SERVER_IP, ip.trim()).commit();
    }

    public void syncAsync(final String serverAddress, final SyncListener listener) {
        saveServerIp(serverAddress);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    postProgress(listener, "Preparing notes...");
                    List<Note> localNotes = repo.loadAll();
                    Map<String, Note> localMap = new HashMap<String, Note>();
                    JSONArray notesArray = new JSONArray();

                    for (Note n : localNotes) {
                        localMap.put(n.id, n);
                        notesArray.put(repo.toJson(n));
                    }

                    JSONObject payload = new JSONObject();
                    payload.put("device", "BlackBerry");
                    payload.put("timestamp", System.currentTimeMillis());
                    payload.put("notes", notesArray);

                    String endpoint = serverAddress.trim();
                    if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                        endpoint = "http://" + endpoint;
                    }
                    if (!endpoint.endsWith("/api/sync")) {
                        if (endpoint.endsWith("/")) {
                            endpoint += "api/sync";
                        } else {
                            endpoint += "/api/sync";
                        }
                    }

                    postProgress(listener, "Connecting to " + serverAddress + "...");
                    URL url = new URL(endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(12000);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    byte[] outBytes = payload.toString().getBytes("UTF-8");
                    conn.setFixedLengthStreamingMode(outBytes.length);
                    OutputStream os = conn.getOutputStream();
                    os.write(outBytes);
                    os.flush();
                    os.close();

                    postProgress(listener, "Exchanging notes...");
                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200 && responseCode != 201) {
                        throw new Exception("Server returned HTTP " + responseCode);
                    }

                    InputStream is = conn.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    conn.disconnect();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray serverNotes = res.optJSONArray("serverNotes");
                    int pulledCount = 0;

                    if (serverNotes != null) {
                        for (int i = 0; i < serverNotes.length(); i++) {
                            JSONObject snObj = serverNotes.getJSONObject(i);
                            Note sn = repo.fromJson(snObj);
                            Note ln = localMap.get(sn.id);

                            // If not on local, or server version is newer
                            if (ln == null || sn.updatedAt > ln.updatedAt) {
                                repo.saveRaw(sn);
                                pulledCount++;
                            }
                        }
                    }

                    final int finalPushed = localNotes.size();
                    final int finalPulled = pulledCount;
                    postSuccess(listener, finalPushed, finalPulled);

                } catch (final Exception e) {
                    postError(listener, e.getMessage() != null ? e.getMessage() : "Connection failed");
                }
            }
        }).start();
    }

    private void postProgress(final SyncListener l, final String msg) {
        if (l == null) return;
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    l.onProgress(msg);
                }
            });
        }
    }

    private void postSuccess(final SyncListener l, final int pushed, final int pulled) {
        if (l == null) return;
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    l.onSuccess(pushed, pulled);
                }
            });
        }
    }

    private void postError(final SyncListener l, final String err) {
        if (l == null) return;
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    l.onError(err);
                }
            });
        }
    }
}
