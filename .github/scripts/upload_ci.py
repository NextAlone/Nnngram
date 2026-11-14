"""
Telegram release uploader for Nnngram CI.

Flow (all routed through the local telegram-bot-api on 127.0.0.1:8081):
  1. POST grouped HTML changelog (all commits in this push) to METADATA_CHANNEL.
  2. POST arm64 APK with a short HTML caption to CHAT_ID.
  3. POST same APK to APK_CHANNEL and capture `message_id - 1` as start_id.
  4. POST `<version>,<code>,<start_id>,<changes_id>,false` to METADATA_CHANNEL
     as the tracking record consumed by the in-app update checker.

Message format:
  Commits are bucketed by Conventional Commits type (fix/feat/docs/...) so
  the channel post reads like a release note instead of a raw `git log`.
  HTML parse_mode keeps escaping rules trivial — only `< > &` need swapping.

Inputs (env vars):
  TELEGRAM_TOKEN          bot token
  CHAT_ID                 main chat that receives the APK + caption
  METADATA_CHANNEL_ID     optional override (default: legacy channel)
  APK_CHANNEL_ID          optional override (default: legacy channel)
  VERSION_NAME            e.g. "12.7.3-da90be4"
  VERSION_CODE            e.g. "1779541780"
  LAST_SENT_SHA           optional; SHA of the previous successful upload.
                          When present, valid locally, AND an ancestor of
                          HEAD, commits are computed from
                          `git log LAST_SENT_SHA..HEAD` — covering any
                          intermediate commits whose own CI runs failed.
                          Falls back to COMMITS_JSON otherwise (the typical
                          force-push case is handled by that fallback,
                          since GitHub's `event.commits` already encodes
                          compare(before, after)).
  COMMITS_JSON            JSON array from github.event.commits (push event)
  HEAD_COMMIT_MESSAGE     fallback when COMMITS_JSON is empty (workflow_dispatch)
  REPO_URL                e.g. "https://github.com/NextAlone/Nnngram"; if set,
                          commit shas are rendered as deep-links into GitHub
  APK_DIR                 optional APK source dir (default ./apks)
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple

import requests

API_BASE = "http://127.0.0.1:8081/bot"
DEFAULT_METADATA_CHANNEL = -1002135305446
DEFAULT_APK_CHANNEL = -1001848519901

# Telegram caps: caption 1024, message text 4096. Reserve headroom for the
# header + group labels + sha codes + truncation marker.
CAPTION_BUDGET = 900
MESSAGE_BUDGET = 3800

# Conventional Commits buckets. Ordering here drives the rendered group order.
GROUPS: List[Tuple[str, str, Tuple[str, ...]]] = [
    ("feat",     "✨ <b>Features</b>",    ("feat",)),
    ("fix",      "🔧 <b>Fixes</b>",       ("fix",)),
    ("perf",     "⚡ <b>Performance</b>", ("perf",)),
    ("refactor", "♻️ <b>Refactor</b>",   ("refactor", "refa")),
    ("docs",     "📝 <b>Docs</b>",        ("docs", "doc")),
    ("test",     "✅ <b>Tests</b>",       ("test",)),
    ("build",    "📦 <b>Build</b>",       ("build",)),
    ("ci",       "⚙️ <b>CI</b>",         ("ci",)),
    ("style",    "💄 <b>Style</b>",       ("style",)),
    ("chore",    "🧹 <b>Chore</b>",       ("chore",)),
    ("revert",   "⏪ <b>Revert</b>",      ("revert",)),
    ("merge",    "🔀 <b>Merge</b>",       ("merge",)),
    ("other",    "📌 <b>Other</b>",       ()),
]
GROUP_INDEX: Dict[str, int] = {key: i for i, (key, _label, _) in enumerate(GROUPS)}
GROUP_EMOJI: Dict[str, str] = {
    "feat": "✨", "fix": "🔧", "perf": "⚡", "refactor": "♻️", "docs": "📝",
    "test": "✅", "build": "📦", "ci": "⚙️", "style": "💄", "chore": "🧹",
    "revert": "⏪", "merge": "🔀", "other": "📌",
}
TYPE_TO_GROUP: Dict[str, str] = {t: key for key, _label, types in GROUPS for t in types}

# `<type>(<scope>)?: <subject>` per Conventional Commits.
SUBJECT_RE = re.compile(r"^(?P<type>[A-Za-z]+)(?:\([^)]*\))?!?:\s*(?P<rest>.+)$")

# Trailer that points readers at the in-app toggle for a feature commit.
# Render mode: caption shows it inline so the build channel becomes a clickable
# "quick-jump" index to every new setting.
SETTING_PATH_RE = re.compile(r"^\s*Setting-Path\s*:\s*(.+?)\s*$", re.IGNORECASE)


@dataclass
class SettingPath:
    """A setting-path trailer entry — `anchor` is the human-readable UI path,
    `url` is an optional deep-link (rendered as a clickable href when set)."""
    anchor: str
    url: str = ""


@dataclass
class Commit:
    sha: str
    subject: str
    body: str
    group_key: str = field(default="other")
    type_label: str = field(default="")
    setting_paths: List[SettingPath] = field(default_factory=list)

    @property
    def short_sha(self) -> str:
        return self.sha[:7] if self.sha else ""

    @classmethod
    def from_event(cls, payload: Dict[str, Any]) -> "Commit":
        msg = (payload.get("message") or "").replace("\r", "").strip()
        subject, _, body = msg.partition("\n")
        return cls._classify(payload.get("id") or "", subject.strip(), body.strip())

    @classmethod
    def from_raw(cls, raw: str) -> "Commit":
        text = raw.replace("\r", "").strip()
        subject, _, body = text.partition("\n")
        return cls._classify("", subject.strip(), body.strip())

    @classmethod
    def _classify(cls, sha: str, subject: str, body: str) -> "Commit":
        body, setting_paths = _extract_setting_paths(body)
        m = SUBJECT_RE.match(subject)
        if m:
            t = m.group("type").lower()
            group_key = TYPE_TO_GROUP.get(t, "other")
            type_label = t
        elif subject.lower().startswith("merge "):
            group_key, type_label = "merge", "merge"
        else:
            group_key, type_label = "other", ""
        return cls(sha=sha, subject=subject, body=body, group_key=group_key,
                   type_label=type_label, setting_paths=setting_paths)


def _extract_setting_paths(body: str) -> Tuple[str, List[SettingPath]]:
    if not body:
        return body, []
    kept: List[str] = []
    paths: List[SettingPath] = []
    for line in body.split("\n"):
        m = SETTING_PATH_RE.match(line)
        if m:
            raw = m.group(1).strip()
            # Format: "<anchor> | <url>"  →  clickable link
            #         "<anchor>"          →  plain italic
            anchor, sep, url = raw.partition("|")
            paths.append(SettingPath(anchor=anchor.strip(), url=url.strip() if sep else ""))
        else:
            kept.append(line)
    return "\n".join(kept).strip(), paths


def _setting_path_html(p: SettingPath) -> str:
    text = html_escape(p.anchor)
    if p.url:
        # Telegram client routes tg:// schemes through its own intent handler;
        # fork-side `tg://nnn/<cat>?r=<key>` is wired up in LaunchActivity →
        # SettingsHelper.processDeepLink → opens the SettingActivity + scrolls to row.
        return f'<a href="{html_escape(p.url)}">{text}</a>'
    return f"<i>{text}</i>"


def url(method: str) -> str:
    token = os.environ.get("TELEGRAM_TOKEN", "")
    return f"{API_BASE}{token}/{method}"


def env_int(name: str, default: int) -> int:
    raw = os.environ.get(name)
    if not raw:
        return default
    try:
        return int(raw)
    except ValueError:
        print(f"warn: {name}={raw!r} is not an int, falling back to {default}")
        return default


def _git(*args: str) -> Optional[str]:
    """Return stdout of `git <args>` or None on non-zero exit / missing binary."""
    try:
        return subprocess.check_output(["git", *args], stderr=subprocess.DEVNULL).decode("utf-8")
    except (FileNotFoundError, subprocess.CalledProcessError):
        return None


def _commits_from_git_range(last_sent: str) -> Optional[List[Commit]]:
    """Build the commits list from `git log <last_sent>..HEAD`.

    Returns None when (a) git isn't available, (b) `last_sent` isn't a known
    object in this clone, or (c) `last_sent` isn't an ancestor of HEAD (e.g.
    after a force-push that rewrote history past the last successful upload).
    The caller falls back to COMMITS_JSON / HEAD_COMMIT_MESSAGE in those cases."""
    last_sent = last_sent.strip()
    if not last_sent:
        return None
    if _git("cat-file", "-e", last_sent) is None:
        print(f"warn: LAST_SENT_SHA {last_sent} not present locally; ignoring")
        return None
    if _git("merge-base", "--is-ancestor", last_sent, "HEAD") is None:
        print(f"warn: LAST_SENT_SHA {last_sent} is not an ancestor of HEAD (force-push?); ignoring")
        return None
    out = _git("log", f"{last_sent}..HEAD", "--format=%H%x00%B%x01")
    if out is None:
        return None
    commits: List[Commit] = []
    for chunk in out.split("\x01"):
        chunk = chunk.strip()
        if not chunk:
            continue
        sha, _, msg = chunk.partition("\x00")
        commits.append(Commit.from_event({"id": sha.strip(), "message": msg.strip()}))
    # git log emits newest→oldest; flip so the rendered order matches push-event order.
    commits.reverse()
    return commits


def load_commits() -> List[Commit]:
    last_sent = os.environ.get("LAST_SENT_SHA") or ""
    if last_sent.strip():
        from_range = _commits_from_git_range(last_sent)
        if from_range:
            print(f"loaded {len(from_range)} commits via git log {last_sent[:7]}..HEAD")
            return from_range
        if from_range == []:
            # last_sent == HEAD: no new commits. Fall through to event.commits so
            # a re-run (workflow_dispatch / retry) still surfaces the HEAD commit
            # instead of rendering an empty caption.
            print(f"info: no new commits since last sent ({last_sent[:7]}); using event payload")
    raw_json = os.environ.get("COMMITS_JSON") or ""
    if raw_json:
        try:
            arr = json.loads(raw_json) or []
        except json.JSONDecodeError as e:
            print(f"warn: COMMITS_JSON parse failed ({e}); falling back to HEAD")
            arr = []
        commits = [Commit.from_event(c) for c in arr if isinstance(c, dict)]
        if commits:
            return commits
    head = os.environ.get("HEAD_COMMIT_MESSAGE") or ""
    if head.strip():
        return [Commit.from_raw(head)]
    return []


def group_commits(commits: List[Commit]) -> List[Tuple[str, str, List[Commit]]]:
    """Return [(group_key, label, [commits]), ...] in canonical group order."""
    buckets: Dict[str, List[Commit]] = {}
    for c in commits:
        buckets.setdefault(c.group_key, []).append(c)
    out: List[Tuple[str, str, List[Commit]]] = []
    for key, label, _types in GROUPS:
        if key in buckets:
            out.append((key, label, buckets[key]))
    return out


def html_escape(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def repo_url() -> str:
    return (os.environ.get("REPO_URL") or "").rstrip("/")


def sha_chip(full_sha: str) -> str:
    """Render `[short_sha]` — linked to GitHub when REPO_URL is configured."""
    if not full_sha:
        return ""
    short = full_sha[:7]
    chip = f"[{html_escape(short)}]"
    base = repo_url()
    if not base:
        return f"<code>{chip}</code>"
    return f'<a href="{base}/commit/{full_sha}">{chip}</a>'


def truncate_plain(s: str, budget: int) -> str:
    """Byte-slice truncation — only safe for plain text (no markup)."""
    if len(s) <= budget:
        return s
    return s[:budget].rstrip() + "\n…(truncated)"


def header_html(version_name: str, version_code: str) -> str:
    return (
        f"<b>Nnngram</b> <code>{html_escape(version_name)}</code> "
        f"<i>({html_escape(version_code)})</i>"
    )


def metadata_message_url(channel_id: int, message_id: int) -> str:
    """Telegram private-channel message link: t.me/c/<id-without-100>/<msg>."""
    if not channel_id or not message_id:
        return ""
    raw = str(channel_id)
    short = raw[4:] if raw.startswith("-100") else raw.lstrip("-")
    return f"https://t.me/c/{short}/{message_id}"


def _footer(dropped: int, dropped_by_group: Optional[Dict[str, int]] = None, metadata_url: str = "") -> str:
    """Render the trailing 'quick-jump' line: optional 'N more (...)' prefix
    when commits got truncated, followed by up to two clickable links
    (GitHub compare/commit + metadata-channel full changelog).

    Returns '' when there's nothing to show (no drops AND no links)."""
    base = repo_url()
    head = (os.environ.get("GITHUB_SHA") or "").strip()
    last_sent = (os.environ.get("LAST_SENT_SHA") or "").strip()
    gh_url = ""
    if base and head:
        if last_sent and last_sent != head:
            gh_url = f"{base}/compare/{last_sent}...{head}"
        else:
            gh_url = f"{base}/commit/{head}"

    parts: List[str] = []
    if dropped > 0:
        plural = "" if dropped == 1 else "s"
        text = f"+ {dropped} more commit{plural}"
        if dropped_by_group:
            ordered = sorted(dropped_by_group.items(), key=lambda kv: GROUP_INDEX.get(kv[0], 99))
            breakdown = ", ".join(f"{n} {GROUP_EMOJI.get(k, '·')} {k}" for k, n in ordered if n > 0)
            if breakdown:
                text += f" ({breakdown})"
        parts.append(f"<i>{text}</i>")
    if gh_url:
        parts.append(f'<a href="{html_escape(gh_url)}">commits</a>')
    if metadata_url:
        parts.append(f'<a href="{html_escape(metadata_url)}">full changelog</a>')
    return " · ".join(parts)


def _assemble(header: str, sections: List[Tuple[str, List[str]]], dropped: int,
              dropped_by_group: Optional[Dict[str, int]] = None, metadata_url: str = "") -> str:
    """Glue header + populated sections + always-on quick-jump footer."""
    blocks = [header]
    body_blocks = [lines[0] + "\n" + "\n".join(lines[1:]) for _key, lines in sections if len(lines) > 1]
    if body_blocks:
        blocks.append("\n\n".join(body_blocks))
    footer = _footer(dropped, dropped_by_group, metadata_url)
    if footer:
        blocks.append(footer)
    return "\n\n".join(blocks)


def _shrink(header: str, sections: List[Tuple[str, List[str]]], budget: int, metadata_url: str = "") -> str:
    """Repeatedly drop the trailing entry until the rendered string fits the budget.

    Never slices inside an HTML tag (each entry is a self-contained string).
    Tracks per-group drop counts so the footer can show a breakdown."""
    dropped = 0
    dropped_by_group: Dict[str, int] = {}
    while True:
        rendered = _assemble(header, sections, dropped, dropped_by_group, metadata_url)
        if len(rendered) <= budget:
            return rendered
        # Find the last group that still has entries and drop its last entry.
        for i in range(len(sections) - 1, -1, -1):
            group_key, lines = sections[i]
            if len(lines) > 1:
                lines.pop()
                dropped += 1
                dropped_by_group[group_key] = dropped_by_group.get(group_key, 0) + 1
                break
        else:
            # Nothing left to drop — header alone is the floor.
            return rendered


def render_caption(version_name: str, version_code: str, commits: List[Commit], metadata_url: str = "") -> str:
    header = header_html(version_name, version_code)
    if not commits:
        return header + "\n\nNo commit metadata."
    sections: List[Tuple[str, List[str]]] = []
    for key, label, items in group_commits(commits):
        lines = [label]
        for c in items:
            chip = sha_chip(c.sha)
            prefix = f"• {chip} " if chip else "• "
            entry = prefix + html_escape(c.subject)
            for path in c.setting_paths:
                entry += f"\n  ↳ ⚙ {_setting_path_html(path)}"
            lines.append(entry)
        sections.append((key, lines))
    return _shrink(header, sections, CAPTION_BUDGET, metadata_url)


def render_full_changelog(version_name: str, version_code: str, commits: List[Commit]) -> str:
    header = header_html(version_name, version_code)
    if not commits:
        return header + "\n\nNo commit metadata."
    sections: List[Tuple[str, List[str]]] = []
    for key, label, items in group_commits(commits):
        lines = [label]
        for c in items:
            chip = sha_chip(c.sha)
            entry = (f"{chip} " if chip else "") + html_escape(c.subject)
            for path in c.setting_paths:
                entry += f"\n  ↳ ⚙ {_setting_path_html(path)}"
            if c.body:
                entry += "\n" + html_escape(c.body)
            lines.append(entry)
        sections.append((key, lines))
    dropped = 0
    dropped_by_group: Dict[str, int] = {}
    while True:
        rendered = _assemble_changelog(header, sections, dropped, dropped_by_group)
        if len(rendered) <= MESSAGE_BUDGET:
            return rendered
        for i in range(len(sections) - 1, -1, -1):
            group_key, lines = sections[i]
            if len(lines) > 1:
                lines.pop()
                dropped += 1
                dropped_by_group[group_key] = dropped_by_group.get(group_key, 0) + 1
                break
        else:
            return rendered


def _assemble_changelog(header: str, sections: List[Tuple[str, List[str]]], dropped: int,
                        dropped_by_group: Optional[Dict[str, int]] = None) -> str:
    blocks = [header]
    for _key, lines in sections:
        if len(lines) <= 1:
            continue
        label, *entries = lines
        blocks.append(label + "\n\n" + "\n\n".join(entries))
    if dropped > 0:
        blocks.append(_footer(dropped, dropped_by_group))
    return "\n\n".join(blocks)


def find_arm64_apk(apk_dir: str) -> Optional[str]:
    if not os.path.isdir(apk_dir):
        return None
    for root, _dirs, files in os.walk(apk_dir):
        for name in files:
            if "arm64" in name and name.endswith(".apk"):
                return os.path.join(root, name)
    return None


def prepare_apk_payload(apk_dir: str) -> Dict[str, Any]:
    src = find_arm64_apk(apk_dir)
    if not src:
        raise FileNotFoundError(f"no arm64 APK found under {apk_dir!r}")
    stamp = time.strftime("%Y%m%d%H%M%S", time.localtime())
    dst = os.path.join(apk_dir, f"{stamp}.apk")
    shutil.copyfile(src, dst)
    print(f"apk: {src} -> {dst}")
    return {"document": (os.path.basename(src), open(dst, "rb"))}


def check_response(method: str, resp: requests.Response) -> None:
    # The Bot API explains a rejection in the body; the URL carries the token.
    if not resp.ok:
        raise RuntimeError(f"{method} failed with HTTP {resp.status_code}: {resp.text[:2000]}")


def post(method: str, data: Dict[str, Any], files: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    data = {**data, "parse_mode": "HTML"}
    print(f"-> {method} payload keys: {sorted(data.keys())}")
    resp = requests.post(url(method), data=data, files=files, timeout=600)
    check_response(method, resp)
    body = resp.json()
    if not body.get("ok", False):
        raise RuntimeError(f"{method} returned not-ok: {body}")
    return body


def send_full_changelog(channel: int, text: str) -> int:
    body = post("sendMessage", {"chat_id": channel, "text": text, "disable_web_page_preview": "true"})
    msg_id = body["result"]["message_id"]
    print(f"changelog message_id={msg_id}")
    return msg_id


def send_apk(chat_id: int, caption: str, apk_dir: str) -> int:
    files = prepare_apk_payload(apk_dir)
    body = post("sendDocument", {"chat_id": chat_id, "caption": caption}, files=files)
    msg_id = body["result"]["message_id"]
    print(f"apk message_id={msg_id} (chat={chat_id})")
    return msg_id


def send_tracking(channel: int, version_name: str, version_code: str, start_id: int, changes_id: int) -> None:
    text = f"{version_name},{version_code},{start_id},{changes_id},false"
    # Tracking record is consumed by the in-app updater as plain CSV; bypass HTML.
    resp = requests.post(
        url("sendMessage"),
        data={"chat_id": channel, "text": text},
        timeout=60,
    )
    check_response("sendMessage", resp)
    print(f"tracking: {text}")


def main() -> int:
    version_name = os.environ.get("VERSION_NAME", "Unknown")
    version_code = os.environ.get("VERSION_CODE", "Unknown")
    chat_id = env_int("CHAT_ID", 0)
    metadata_channel = env_int("METADATA_CHANNEL_ID", DEFAULT_METADATA_CHANNEL)
    apk_channel = env_int("APK_CHANNEL_ID", DEFAULT_APK_CHANNEL)
    apk_dir = os.environ.get("APK_DIR", "./apks")

    if not chat_id:
        print("error: CHAT_ID is required", file=sys.stderr)
        return 2
    if not os.environ.get("TELEGRAM_TOKEN"):
        print("error: TELEGRAM_TOKEN is required", file=sys.stderr)
        return 2

    commits = load_commits()
    print(f"version: {version_name} ({version_code})")
    print(f"commits: {len(commits)}")
    for c in commits:
        print(f"  - [{c.short_sha}] ({c.group_key}) {c.subject}")

    changelog = render_full_changelog(version_name, version_code, commits)
    print("--- changelog ---\n" + changelog + "\n--- /changelog ---\n")

    print("[1/4] full changelog -> metadata channel")
    changes_id = send_full_changelog(metadata_channel, changelog)

    caption = render_caption(
        version_name, version_code, commits,
        metadata_message_url(metadata_channel, changes_id),
    )
    print("--- caption ---\n" + caption + "\n--- /caption ---\n")

    print("[2/4] APK -> main chat")
    send_apk(chat_id, caption, apk_dir)

    print("[3/4] APK -> APK channel")
    apk_msg_id = send_apk(apk_channel, caption, apk_dir)
    start_id = apk_msg_id - 1

    print("[4/4] tracking record -> metadata channel")
    send_tracking(metadata_channel, version_name, version_code, start_id, changes_id)

    print("done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
