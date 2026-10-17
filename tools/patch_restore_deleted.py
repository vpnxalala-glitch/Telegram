#!/usr/bin/env python3
"""Restore DownloadController from good commit and apply Turbo mode hooks."""
import re
import subprocess
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/messenger/DownloadController.java")
GOOD = "0497eabab4b5dabdb2ba28f1eaa9763e553c60d8"

# Restore from good commit
raw = subprocess.check_output(
    ["git", "show", f"{GOOD}:{TARGET.as_posix()}"],
    text=True,
)
TARGET.write_text(raw, encoding="utf-8")
print("restored", TARGET.stat().st_size)

text = TARGET.read_text(encoding="utf-8")
assert "class DownloadController" in text

if "VichGramPrefs.isTurboMode" not in text:
    text, n1 = re.subn(
        r"(public boolean canDownloadMedia\(MessageObject messageObject\)\s*\{)",
        r"""\1
        // VichGram Turbo: always allow media download
        if (org.telegram.messenger.VichGramPrefs.isTurboMode()) {
            return true;
        }""",
        text,
        count=1,
        flags=re.M,
    )
    print("msg", n1)
    text, n2 = re.subn(
        r"(public boolean canDownloadMedia\(int type, long size\)\s*\{)",
        r"""\1
        // VichGram Turbo: no size/type limits
        if (org.telegram.messenger.VichGramPrefs.isTurboMode()) {
            return true;
        }""",
        text,
        count=1,
        flags=re.M,
    )
    print("type", n2)
    text, n3 = re.subn(
        r"(public int canDownloadMediaType\(MessageObject messageObject\)\s*\{)",
        r"""\1
        // VichGram Turbo
        if (org.telegram.messenger.VichGramPrefs.isTurboMode()) {
            return 2;
        }""",
        text,
        count=1,
        flags=re.M,
    )
    print("mediaType", n3)
    TARGET.write_text(text, encoding="utf-8")

print("final size", TARGET.stat().st_size)
print("hooks", TARGET.read_text(encoding="utf-8").count("isTurboMode"))
print("done")
