"""
audio_metadata_injector.py
━━━━━━━━━━━━━━━━━━━━━━━━━
Standalone audio metadata injection service for Milla.

Embeds into any downloaded audio file (.flac, .m4a, .mp3):
  • Cover Art (JPEG, front-cover picture block)
  • Title, Artist, Album
  • BPM
  • Syllable-Synced Lyrics JSON  ← key feature

The injected file is 100 % self-sufficient for offline playback —
no network call or database query is needed for artwork, tags, or
animated lyrics.

Format → tag mapping
─────────────────────────────────────────────────────────────────────
FLAC  │ VorbisComment:  TITLE, ARTIST, ALBUM, BPM, LYRICS (plain)
      │                 SYLLABLE_LYRICS (Milla custom JSON field)
      │ FLAC Picture block (type 3 = front cover)
──────┼──────────────────────────────────────────────────────────────
M4A   │ MP4 atoms:  ©nam, ©ART, ©alb, tmpo, ©lyr (plain)
      │             ----:com.apple.iTunes:SYLLABLE_LYRICS (JSON)
      │             covr (JPEG)
──────┼──────────────────────────────────────────────────────────────
MP3   │ ID3v2.4:  TIT2, TPE1, TALB, TBPM, APIC (cover)
      │           USLT (unsync JSON, lang=eng, desc=SYLLABLE_LYRICS)
      │           SYLT (sync tuples extracted from JSON, ms format)
─────────────────────────────────────────────────────────────────────

Lyrics JSON canonical format (list of dicts):
  [{"time": 4210, "text": "Primera línea"}, ...]
  • time  — milliseconds from start
  • text  — lyric line / syllable text

Deezer LYRICS_SYNC_JSON is auto-converted:
  [{"lrc_timestamp": "[00:04.21]", "line": "..."}, ...]

Install:  pip install mutagen
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Union

# ── mutagen ──────────────────────────────────────────────────────────────────
from mutagen.flac import FLAC, Picture
from mutagen.mp4 import MP4, MP4Cover, MP4FreeForm
from mutagen.id3 import (
    ID3, ID3NoHeaderError,
    TIT2, TPE1, TALB, TBPM, APIC, USLT, SYLT,
    Encoding, PictureType,
)

# ─────────────────────────────────────────────────────────────────────────────

_LyricsInput = Union[list, str, None]


class AudioMetadataInjector:
    """
    Inject cover art, tags, and syllable-synced lyrics JSON into audio files.

    Usage
    -----
    injector = AudioMetadataInjector()
    injector.inject(
        audio_path   = "Danny Ocean - Corazón.flac",
        cover_bytes  = jpeg_bytes,
        lyrics_json  = lyrics_list_or_json_string,
        title        = "Corazón",
        artist       = "Danny Ocean",
        album        = "No Lo Había Dicho",
        bpm          = 95,
    )
    """

    # Custom iTunes reverse-DNS key used in M4A files
    _M4A_SYLLABLE_ATOM = "----:com.apple.iTunes:SYLLABLE_LYRICS"

    # ── public API ────────────────────────────────────────────────────────────

    def inject(
        self,
        audio_path: Union[str, Path],
        cover_bytes: bytes | None = None,
        lyrics_json: _LyricsInput = None,
        title: str = "",
        artist: str = "",
        album: str = "",
        bpm: int = 0,
    ) -> None:
        """
        Main entry point.  Detects format from extension and dispatches.

        Parameters
        ----------
        audio_path   : Path to the target audio file.
        cover_bytes  : Raw JPEG bytes for front-cover art.
        lyrics_json  : Syllable-synced lyrics — list[dict] or JSON string.
                       Accepts canonical {"time": ms, "text": str} OR
                       Deezer {"lrc_timestamp": "[mm:ss.xx]", "line": str}.
        title        : Track title.
        artist       : Artist name.
        album        : Album / release title.
        bpm          : Beats-per-minute (0 = omit tag).
        """
        path = Path(audio_path)
        if not path.is_file():
            raise FileNotFoundError(f"Audio file not found: {path}")

        ext = path.suffix.lower().lstrip(".")
        lyrics_str = self._normalize_lyrics(lyrics_json)

        print(f"[MetaInjector] Injecting → {path.name}  (format: {ext})")

        dispatch = {
            "flac":  self._inject_flac,
            "m4a":   self._inject_m4a,
            "mp4":   self._inject_m4a,
            "aac":   self._inject_m4a,
            "mp3":   self._inject_mp3,
        }
        handler = dispatch.get(ext)
        if handler is None:
            print(f"[MetaInjector] ⚠  Unsupported format '{ext}' — skipping.")
            return

        handler(path, cover_bytes, lyrics_str, title, artist, album, bpm)
        print(f"[MetaInjector] ✅ Done: {path.name}")

    # ── FLAC ──────────────────────────────────────────────────────────────────

    def _inject_flac(
        self, path: Path,
        cover_bytes: bytes | None, lyrics_str: str | None,
        title: str, artist: str, album: str, bpm: int,
    ) -> None:
        audio = FLAC(str(path))

        # VorbisComment text tags
        if title:   audio["title"]  = title
        if artist:  audio["artist"] = artist
        if album:   audio["album"]  = album
        if bpm > 0: audio["bpm"]    = str(bpm)

        # Standard LYRICS field (broad player compatibility)
        if lyrics_str:
            audio["lyrics"] = lyrics_str
            # Custom SYLLABLE_LYRICS field read by Milla's syllable renderer
            audio["syllable_lyrics"] = lyrics_str
            print(f"  [FLAC] LYRICS + SYLLABLE_LYRICS set ({len(lyrics_str):,} chars)")

        # Cover art — FLAC Picture block (type 3 = front cover)
        if cover_bytes:
            pic = Picture()
            pic.type = PictureType.COVER_FRONT
            pic.mime = "image/jpeg"
            pic.desc = "Cover"
            pic.data = cover_bytes
            audio.clear_pictures()
            audio.add_picture(pic)
            print(f"  [FLAC] Cover art embedded ({len(cover_bytes):,} bytes)")

        audio.save()
        print("  [FLAC] VorbisComment tags saved.")

    # ── M4A / MP4 ─────────────────────────────────────────────────────────────

    def _inject_m4a(
        self, path: Path,
        cover_bytes: bytes | None, lyrics_str: str | None,
        title: str, artist: str, album: str, bpm: int,
    ) -> None:
        audio = MP4(str(path))

        # Standard MP4 atoms
        if title:   audio["\xa9nam"] = [title]     # ©nam — title
        if artist:  audio["\xa9ART"] = [artist]    # ©ART — artist
        if album:   audio["\xa9alb"] = [album]     # ©alb — album
        if bpm > 0: audio["tmpo"]    = [bpm]       # tmpo — BPM (integer atom)

        if lyrics_str:
            # ©lyr — standard iTunes lyrics (plain text, widely read)
            audio["\xa9lyr"] = [lyrics_str]
            # ----:com.apple.iTunes:SYLLABLE_LYRICS — JSON for Milla's renderer
            audio[self._M4A_SYLLABLE_ATOM] = [
                MP4FreeForm(lyrics_str.encode("utf-8"))
            ]
            print(f"  [M4A] ©lyr + SYLLABLE_LYRICS atom set ({len(lyrics_str):,} chars)")

        # Cover art — covr atom (JPEG)
        if cover_bytes:
            audio["covr"] = [MP4Cover(cover_bytes, imageformat=MP4Cover.FORMAT_JPEG)]
            print(f"  [M4A] Cover art embedded ({len(cover_bytes):,} bytes)")

        audio.save()
        print("  [M4A] MP4 atoms saved.")

    # ── MP3 / ID3v2 ───────────────────────────────────────────────────────────

    def _inject_mp3(
        self, path: Path,
        cover_bytes: bytes | None, lyrics_str: str | None,
        title: str, artist: str, album: str, bpm: int,
    ) -> None:
        try:
            audio = ID3(str(path))
        except ID3NoHeaderError:
            audio = ID3()

        if title:   audio.add(TIT2(encoding=Encoding.UTF8, text=title))
        if artist:  audio.add(TPE1(encoding=Encoding.UTF8, text=artist))
        if album:   audio.add(TALB(encoding=Encoding.UTF8, text=album))
        if bpm > 0: audio.add(TBPM(encoding=Encoding.UTF8, text=str(bpm)))

        if lyrics_str:
            # USLT — unsynchronized lyrics frame; stores the raw JSON verbatim
            audio.add(USLT(
                encoding=Encoding.UTF8,
                lang="eng",
                desc="SYLLABLE_LYRICS",
                text=lyrics_str,
            ))

            # SYLT — synchronized lyrics frame; (text, timestamp_ms) tuples
            sylt_entries = self._json_to_sylt_entries(lyrics_str)
            if sylt_entries:
                audio.add(SYLT(
                    encoding=Encoding.UTF8,
                    lang="eng",
                    format=2,           # 2 = milliseconds
                    type=1,             # 1 = lyrics
                    desc="SYLLABLE_SYNC",
                    text=sylt_entries,
                ))
                print(f"  [MP3] SYLT set ({len(sylt_entries)} entries)")

            print(f"  [MP3] USLT set ({len(lyrics_str):,} chars)")

        # Cover art — APIC frame (type 3 = front cover)
        if cover_bytes:
            audio.add(APIC(
                encoding=Encoding.UTF8,
                mime="image/jpeg",
                type=PictureType.COVER_FRONT,
                desc="Cover",
                data=cover_bytes,
            ))
            print(f"  [MP3] Cover art embedded ({len(cover_bytes):,} bytes)")

        audio.save(str(path))
        print("  [MP3] ID3v2.4 tags saved.")

    # ── Helpers ───────────────────────────────────────────────────────────────

    @staticmethod
    def _normalize_lyrics(raw: _LyricsInput) -> str | None:
        """
        Coerce any lyrics input to a canonical JSON string.

        Canonical format:  [{"time": <int ms>, "text": "<line>"}, ...]

        Accepts
        -------
        None                                  → None
        str                                   → returned as-is
        list[{"time": int, "text": str}]      → JSON-encoded directly
        list[{"lrc_timestamp": str, "line": str}]  ← Deezer format, converted
        """
        if raw is None:
            return None
        if isinstance(raw, str):
            return raw

        normalized: list[dict] = []
        for entry in raw:
            if "time" in entry:
                # Already canonical
                normalized.append({"time": int(entry["time"]), "text": str(entry.get("text", ""))})
            elif "lrc_timestamp" in entry:
                # Deezer: "[mm:ss.xx]"
                ts_raw = entry.get("lrc_timestamp", "").strip("[]")
                try:
                    mm, ss = ts_raw.split(":")
                    ms = int(float(mm) * 60_000 + float(ss) * 1_000)
                except Exception:
                    ms = 0
                normalized.append({"time": ms, "text": str(entry.get("line", ""))})

        return json.dumps(normalized, ensure_ascii=False)

    @staticmethod
    def _json_to_sylt_entries(lyrics_str: str) -> list[tuple[str, int]]:
        """Convert canonical JSON string to SYLT (text, timestamp_ms) tuples."""
        try:
            entries = json.loads(lyrics_str)
            return [
                (str(e.get("text", "")), int(e.get("time", 0)))
                for e in entries
                if "time" in e
            ]
        except Exception:
            return []
