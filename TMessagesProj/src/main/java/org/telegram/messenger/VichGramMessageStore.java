package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Local persistence for VichGram features:
 * - deleted message snapshots (text + metadata)
 * - edit history (previous texts)
 */
public final class VichGramMessageStore {

    private static final String PREFS = "vichgram_msg_store";
    private static final int MAX_EDITS_PER_MSG = 20;
    private static final int MAX_DELETED_GLOBAL = 500;

    private VichGramMessageStore() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String delKey(int account, long dialogId, int msgId) {
        return "del_" + account + "_" + dialogId + "_" + msgId;
    }

    private static String editKey(int account, long dialogId, int msgId) {
        return "edit_" + account + "_" + dialogId + "_" + msgId;
    }

    private static String delIndexKey(int account, long dialogId) {
        return "del_idx_" + account + "_" + dialogId;
    }

    public static void saveDeleted(int account, MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        try {
            long dialogId = messageObject.getDialogId();
            int msgId = messageObject.getId();
            if (msgId <= 0) {
                return;
            }
            JSONObject o = new JSONObject();
            o.put("id", msgId);
            o.put("dialogId", dialogId);
            o.put("date", messageObject.messageOwner.date);
            o.put("out", messageObject.isOut());
            o.put("fromId", messageObject.getFromChatId());
            String text = messageObject.messageOwner.message;
            if (text == null && messageObject.messageText != null) {
                text = messageObject.messageText.toString();
            }
            o.put("text", text != null ? text : "");
            o.put("savedAt", System.currentTimeMillis());
            prefs().edit().putString(delKey(account, dialogId, msgId), o.toString()).apply();

            // index of deleted ids per dialog
            String idxKey = delIndexKey(account, dialogId);
            JSONArray arr;
            try {
                arr = new JSONArray(prefs().getString(idxKey, "[]"));
            } catch (Exception e) {
                arr = new JSONArray();
            }
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                if (arr.optInt(i) == msgId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                arr.put(msgId);
                // trim
                while (arr.length() > MAX_DELETED_GLOBAL) {
                    arr.remove(0);
                }
                prefs().edit().putString(idxKey, arr.toString()).apply();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static String getDeletedText(int account, long dialogId, int msgId) {
        try {
            String raw = prefs().getString(delKey(account, dialogId, msgId), null);
            if (raw == null) {
                return null;
            }
            return new JSONObject(raw).optString("text", null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasDeleted(int account, long dialogId, int msgId) {
        return prefs().contains(delKey(account, dialogId, msgId));
    }

    public static List<Integer> getDeletedIds(int account, long dialogId) {
        try {
            JSONArray arr = new JSONArray(prefs().getString(delIndexKey(account, dialogId), "[]"));
            ArrayList<Integer> out = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                out.add(arr.optInt(i));
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Save previous text before an edit is applied. */
    public static void saveEdit(int account, long dialogId, int msgId, String previousText) {
        if (msgId <= 0 || TextUtils.isEmpty(previousText)) {
            return;
        }
        try {
            String key = editKey(account, dialogId, msgId);
            JSONArray arr;
            try {
                arr = new JSONArray(prefs().getString(key, "[]"));
            } catch (Exception e) {
                arr = new JSONArray();
            }
            JSONObject entry = new JSONObject();
            entry.put("text", previousText);
            entry.put("at", System.currentTimeMillis());
            arr.put(entry);
            while (arr.length() > MAX_EDITS_PER_MSG) {
                arr.remove(0);
            }
            prefs().edit().putString(key, arr.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static List<String> getEditHistory(int account, long dialogId, int msgId) {
        try {
            JSONArray arr = new JSONArray(prefs().getString(editKey(account, dialogId, msgId), "[]"));
            ArrayList<String> out = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    String t = o.optString("text", null);
                    if (!TextUtils.isEmpty(t)) {
                        out.add(t);
                    }
                }
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static boolean hasEditHistory(int account, long dialogId, int msgId) {
        return !getEditHistory(account, dialogId, msgId).isEmpty();
    }
}
