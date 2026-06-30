#!/usr/bin/env python3
"""Apply FunLife SuperTux Android patches after bootstrap rsync."""
from __future__ import annotations

import sys
from pathlib import Path

FAST_PATH_MARKER = "Mounted pre-staged Android data archive"
PHYSFS_INIT_MARKER = "PHYSFS_AndroidInit physfsAndroidInit"
WRONG_PHYSFS_MARKER = "physfs_android_base"
SDL_SYSTEM_INCLUDE_ANCHOR = "#include <SDL_ttf.h>"
SDL_SYSTEM_INCLUDE = "#include <SDL_ttf.h>\n#ifdef __ANDROID__\n#include <SDL_system.h>\n#endif"
FAST_PATH_ANCHOR = "  newzip.append(zippath);\n\n  size_t zipsz;"
PHYSFS_INIT_ANCHOR = (
    "  if (!PHYSFS_init(argv0))\n"
    "  {\n"
    "    std::stringstream msg;\n"
    '    msg << "Couldn\'t initialize physfs: " << physfsutil::get_last_error();'
)
WRONG_PHYSFS_BLOCK = (
    "#ifdef __ANDROID__\n"
    "  const char* physfs_init_arg = argv0;\n"
    "  std::string physfs_android_base;\n"
    "  // SDL passes argv[0]=\"app_process\" on Android; PHYSFS_init() dereferences it\n"
    "  // as a filesystem path and SIGSEGVs when embedded in a host app (FunLife).\n"
    "  if (m_forced_userdir && !m_forced_userdir->empty()) {\n"
    "    physfs_android_base = *m_forced_userdir;\n"
    "    physfs_init_arg = physfs_android_base.c_str();\n"
    "  }\n"
    "#else\n"
    "  const char* physfs_init_arg = argv0;\n"
    "#endif\n"
    "  if (!PHYSFS_init(physfs_init_arg))\n"
    "  {\n"
    "    std::stringstream msg;\n"
    '    msg << "Couldn\'t initialize physfs: " << physfsutil::get_last_error();'
)
PHYSFS_INIT_REPLACEMENT = (
    "#ifdef __ANDROID__\n"
    "  PHYSFS_AndroidInit physfsAndroidInit{};\n"
    "  physfsAndroidInit.jnienv = SDL_AndroidGetJNIEnv();\n"
    "  physfsAndroidInit.context = SDL_AndroidGetActivity();\n"
    "  const char* physfs_init_arg = reinterpret_cast<const char*>(&physfsAndroidInit);\n"
    "#else\n"
    "  const char* physfs_init_arg = argv0;\n"
    "#endif\n"
    "  if (!PHYSFS_init(physfs_init_arg))\n"
    "  {\n"
    "    std::stringstream msg;\n"
    '    msg << "Couldn\'t initialize physfs: " << physfsutil::get_last_error();'
)
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


def apply_sdl_system_include(main_cpp: Path) -> None:
    text = main_cpp.read_text(encoding="utf-8")
    if "SDL_AndroidGetJNIEnv" in text or SDL_SYSTEM_INCLUDE in text:
        print(f"skip (already applied): SDL_system include in {main_cpp.name}")
        return
    if SDL_SYSTEM_INCLUDE_ANCHOR not in text:
        raise SystemExit(f"SDL include anchor not found in {main_cpp}")
    text = text.replace(SDL_SYSTEM_INCLUDE_ANCHOR, SDL_SYSTEM_INCLUDE, 1)
    main_cpp.write_text(text, encoding="utf-8", newline="\n")
    print(f"applied SDL_system include -> {main_cpp}")


def apply_physfs_init_android(main_cpp: Path) -> None:
    text = main_cpp.read_text(encoding="utf-8")
    if PHYSFS_INIT_MARKER in text:
        print(f"skip (already applied): physfs init in {main_cpp.name}")
        return
    if WRONG_PHYSFS_BLOCK in text:
        text = text.replace(WRONG_PHYSFS_BLOCK, PHYSFS_INIT_REPLACEMENT, 1)
    elif PHYSFS_INIT_ANCHOR in text:
        text = text.replace(PHYSFS_INIT_ANCHOR, PHYSFS_INIT_REPLACEMENT, 1)
    else:
        raise SystemExit(f"physfs init anchor not found in {main_cpp}")
    main_cpp.write_text(text, encoding="utf-8", newline="\n")
    print(f"applied physfs AndroidInit fix -> {main_cpp}")


def main() -> None:
    fork = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("engine/supertux-fork")
    main_cpp = fork / "src/supertux/main.cpp"
    apply_android_datadir_fast_path(main_cpp)
    apply_sdl_system_include(main_cpp)
    apply_physfs_init_android(main_cpp)


if __name__ == "__main__":
    main()
