package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Local persistence for VichGram features:
 * - deleted message snapshots (text + metadata)
 * - edit history (previous texts)
 */
public final class VichGramMessageStore {

    private static final String PREFS = "vichgram_msg_store";
    private static final int MAX_EDITS_PER_MSG = 20;
    private static final int MAX_DELETED_PER_DIALOG = 500;

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
                while (arr.length() > MAX_DELETED_PER_DIALOG) {
                    int removeId = arr.optInt(0);
                    arr.remove(0);
                    prefs().edit().remove(delKey(account, dialogId, removeId)).apply();
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

    /**
     * Rebuild a MessageObject from a saved deleted snapshot (text-only).
     */
    public static MessageObject restoreDeletedMessage(int account, long dialogId, int msgId) {
        try {
            String raw = prefs().getString(delKey(account, dialogId, msgId), null);
            if (raw == null) {
                return null;
            }
            JSONObject o = new JSONObject(raw);
            TLRPC.TL_message msg = new TLRPC.TL_message();
            msg.id = msgId;
            msg.message = o.optString("text", "");
            msg.date = o.optInt("date", (int) (System.currentTimeMillis() / 1000));
            msg.dialog_id = dialogId;
            msg.out = o.optBoolean("out", false);
            msg.flags |= TLRPC.MESSAGE_FLAG_HAS_FROM_ID;

            long fromId = o.optLong("fromId", 0);
            if (fromId != 0) {
                msg.from_id = MessagesController.getInstance(account).getPeer(fromId);
            } else if (msg.out) {
                msg.from_id = MessagesController.getInstance(account).getPeer(UserConfig.getInstance(account).getClientUserId());
            } else if (dialogId > 0) {
                msg.from_id = MessagesController.getInstance(account).getPeer(dialogId);
            }

            if (dialogId > 0) {
                msg.peer_id = new TLRPC.TL_peerUser();
                msg.peer_id.user_id = dialogId;
            } else {
                msg.peer_id = new TLRPC.TL_peerChannel();
                msg.peer_id.channel_id = -dialogId;
            }

            MessageObject mo = new MessageObject(account, msg, false, false);
            mo.deleted = true;
            return mo;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    /** Delete all saved deleted-message snapshots (all accounts/dialogs). */
    public static void clearAllDeleted() {
        try {
            SharedPreferences p = prefs();
            SharedPreferences.Editor ed = p.edit();
            Map<String, ?> all = p.getAll();
            for (String key : all.keySet()) {
                if (key != null && (key.startsWith("del_") || key.startsWith("del_idx_"))) {
                    ed.remove(key);
                }
            }
            ed.apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static int countDeletedSnapshots() {
        try {
            int n = 0;
            for (String key : prefs().getAll().keySet()) {
                if (key != null && key.startsWith("del_") && !key.startsWith("del_idx_")) {
                    n++;
                }
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

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
