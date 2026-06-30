#!/usr/bin/env python3
"""Apply FunLife SuperTux Android patches after bootstrap rsync."""
from __future__ import annotations

import sys
from pathlib import Path

FAST_PATH_MARKER = "Mounted pre-staged Android data archive"
FAST_PATH_ANCHOR = "  newzip.append(zippath);\n\n  size_t zipsz;"
FAST_PATH_BLOCK = """
  // FunLife: reuse pre-extracted archive (SuperTuxClassicDataPreparer writes to
  // getExternalFilesDir()/data.zip). Avoids loading the entire APK asset into RAM.
  if (FileSystem::exists(newzip)) {
    try {
      const auto size = std::filesystem::file_size(newzip);
      if (size > 32 * 1024 * 1024) {
        if (PHYSFS_mount(newzip.c_str(), nullptr, 1)) {
          log_info << "Mounted pre-staged Android data archive: " << newzip << std::endl;
          return true;
        }
        log_warning << "Pre-staged data archive mount failed: "
                    << PHYSFS_getErrorByCode(PHYSFS_getLastErrorCode()) << std::endl;
      }
    } catch (const std::exception& e) {
      log_warning << "Pre-staged data archive check failed: " << e.what() << std::endl;
    }
  }

"""


def apply_android_datadir_fast_path(main_cpp: Path) -> None:
    text = main_cpp.read_text(encoding="utf-8")
    if FAST_PATH_MARKER in text:
        print(f"skip (already applied): {main_cpp.name}")
        return
    if FAST_PATH_ANCHOR not in text:
        raise SystemExit(f"anchor not found in {main_cpp}")
    text = text.replace(
        FAST_PATH_ANCHOR,
        f"  newzip.append(zippath);{FAST_PATH_BLOCK}\n  size_t zipsz;",
        1,
    )
    main_cpp.write_text(text, encoding="utf-8", newline="\n")
    print(f"applied android datadir fast-path -> {main_cpp}")


def main() -> None:
    fork = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("engine/supertux-fork")
    apply_android_datadir_fast_path(fork / "src/supertux/main.cpp")


if __name__ == "__main__":
    main()
