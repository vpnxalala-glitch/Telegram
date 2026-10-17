#!/usr/bin/env python3
import re
from pathlib import Path

path = Path("TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java")
text = path.read_text(encoding="utf-8")
if "VICHGRAM_RESTORE_DELETED" in text:
    print("already patched")
    raise SystemExit(0)

helper = '''
    // VICHGRAM_RESTORE_DELETED
    private void vichgramRestoreDeletedMessages() {
        if (!VichGramPrefs.isWatchDeletedMessages()) {
            return;
        }
        try {
            java.util.List<Integer> ids = VichGramMessageStore.getDeletedIds(currentAccount, dialog_id);
            if (ids == null || ids.isEmpty()) {
                return;
            }
            boolean added = false;
            for (int i = 0; i < ids.size(); i++) {
                int mid = ids.get(i);
                if (messagesDict[0].indexOfKey(mid) >= 0) {
                    MessageObject existing = messagesDict[0].get(mid);
                    if (existing != null) {
                        existing.deleted = true;
                    }
                    continue;
                }
                MessageObject restored = VichGramMessageStore.restoreDeletedMessage(currentAccount, dialog_id, mid);
                if (restored == null) {
                    continue;
                }
                messagesDict[0].put(mid, restored);
                int insertAt = messages.size();
                for (int j = 0; j < messages.size(); j++) {
                    MessageObject m = messages.get(j);
                    if (m != null && m.getId() > 0 && m.getId() < mid) {
                        insertAt = j;
                        break;
                    }
                }
                messages.add(insertAt, restored);
                added = true;
            }
            if (added && chatAdapter != null) {
                chatAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

'''

anchor = "    private void processDeletedMessages(ArrayList<Integer> markAsDeletedMessages, long channelId, boolean sent) {"
if anchor not in text:
    print("processDeletedMessages not found")
    raise SystemExit(1)
text = text.replace(anchor, helper + anchor, 1)
print("helper inserted")

# Call at end of didReceivedNotification_messagesDidLoad
start = text.find("private void didReceivedNotification_messagesDidLoad")
if start < 0:
    print("messagesDidLoad not found")
    raise SystemExit(1)
next_priv = text.find("\n    private void ", start + 80)
if next_priv < 0:
    print("end of messagesDidLoad not found")
    raise SystemExit(1)
call = "\n        vichgramRestoreDeletedMessages();\n"
if "vichgramRestoreDeletedMessages();" not in text[start:next_priv]:
    text = text[:next_priv] + call + text[next_priv:]
    print("call inserted")
else:
    print("call already present")

path.write_text(text, encoding="utf-8")
print("done")
