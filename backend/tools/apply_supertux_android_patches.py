#!/usr/bin/env python3
"""Apply FunLife SuperTux Android patches after bootstrap rsync."""
from __future__ import annotations

import sys
from pathlib import Path

FAST_PATH_MARKER = "Mounted pre-staged Android data archive"
DIR_MOUNT_MARKER = "Mounted pre-extracted Android data directory"
ANDROID_JNI_MARKER = "android_notify_loading"
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
  // FunLife: prefer extracted directory (SuperTuxClassicDataPreparer -> supertux_data/).
  {
    std::string datadir = m_forced_userdir.value();
    datadir.append("/supertux_data");
    std::string data_probe = datadir + "/images/engine/icons/supertux-nightly-256x256.png";
    if (FileSystem::is_directory(datadir) && FileSystem::exists(data_probe)) {
      if (PHYSFS_mount(datadir.c_str(), nullptr, 1)) {
        log_info << "Mounted pre-extracted Android data directory: " << datadir << std::endl;
        return true;
      }
      log_warning << "Pre-extracted data directory mount failed: "
                  << PHYSFS_getErrorByCode(PHYSFS_getLastErrorCode()) << std::endl;
    }
  }

  // FunLife: reuse pre-staged archive (SuperTuxClassicDataPreparer writes to
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
    old_probe = 'std::string images_probe = datadir + "/images";'
    new_probe = 'std::string data_probe = datadir + "/images/engine/icons/supertux-nightly-256x256.png";'
    if old_probe in text:
        text = text.replace(old_probe, new_probe, 1)
        text = text.replace("FileSystem::exists(images_probe)", "FileSystem::exists(data_probe)", 1)
        main_cpp.write_text(text, encoding="utf-8", newline="\n")
        print(f"upgraded datadir probe (icon sanity check) -> {main_cpp}")
        return
    if DIR_MOUNT_MARKER in text and FAST_PATH_MARKER in text:
        print(f"skip (already applied): datadir fast-path in {main_cpp.name}")
        return
    if FAST_PATH_MARKER in text and DIR_MOUNT_MARKER not in text:
        idx = text.find("  // FunLife: reuse pre-")
        if idx < 0:
            raise SystemExit(f"zip fast-path present but anchor missing in {main_cpp}")
        dir_prefix = FAST_PATH_BLOCK.split("  // FunLife: reuse pre-staged archive")[0]
        text = text[:idx] + dir_prefix + text[idx:]
        main_cpp.write_text(text, encoding="utf-8", newline="\n")
        print(f"upgraded datadir fast-path (directory mount) -> {main_cpp}")
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


ANDROID_JNI_ANCHOR = "PhysfsSubsystem::PhysfsSubsystem(const char* argv0,"
ANDROID_JNI_BLOCK = """
#ifdef __ANDROID__
#include <jni.h>

static void android_notify_loading(int progress, const char* stage)
{
  JNIEnv* env = static_cast<JNIEnv*>(SDL_AndroidGetJNIEnv());
  jobject activity = static_cast<jobject>(SDL_AndroidGetActivity());
  if (!env || !activity) return;
  jclass cls = env->GetObjectClass(activity);
  if (!cls) return;
  jmethodID mid = env->GetMethodID(cls, "onEngineLoadingProgress", "(ILjava/lang/String;)V");
  if (mid) {
    jstring jstage = env->NewStringUTF(stage ? stage : "");
    env->CallVoidMethod(activity, mid, progress, jstage);
    env->DeleteLocalRef(jstage);
  }
  env->DeleteLocalRef(cls);
}

void android_notify_ready()
{
  JNIEnv* env = static_cast<JNIEnv*>(SDL_AndroidGetJNIEnv());
  jobject activity = static_cast<jobject>(SDL_AndroidGetActivity());
  if (!env || !activity) return;
  jclass cls = env->GetObjectClass(activity);
  if (!cls) return;
  jmethodID mid = env->GetMethodID(cls, "onEngineReady", "()V");
  if (mid) env->CallVoidMethod(activity, mid);
  env->DeleteLocalRef(cls);
}
#endif

"""

LAUNCH_GAME_READY_ANCHOR = "  m_screen_manager->run();"
LAUNCH_GAME_READY_REPLACEMENT = """#ifdef __ANDROID__
  android_notify_loading(90, "\\xe5\\x90\\xaf\\xe5\\x8a\\xa8\\xe6\\xb8\\xb8\\xe6\\x88\\x8f\\xe2\\x80\\xa6");
#endif
  m_screen_manager->run();"""

SKIP_INTRO_MARKER = "session->skip_intro();"
SKIP_INTRO_ANCHOR = (
    "        std::unique_ptr<GameSession> session = "
    "std::make_unique<GameSession>(start_level, *m_savegame);"
)
SKIP_INTRO_REPLACEMENT = (
    "        std::unique_ptr<GameSession> session = "
    "std::make_unique<GameSession>(start_level, *m_savegame);\n"
    "#ifdef __ANDROID__\n"
    "        session->skip_intro();\n"
    "#endif"
)

GAME_SESSION_DRAW_MARKER = "s_android_ready_sent"
GAME_SESSION_DRAW_ANCHOR = (
    "void\nGameSession::draw(Compositor& compositor)\n{\n"
    "  auto& context = compositor.make_context();"
)
GAME_SESSION_DRAW_REPLACEMENT = (
    "void\nGameSession::draw(Compositor& compositor)\n{\n"
    "#ifdef __ANDROID__\n"
    "  extern void android_notify_ready();\n"
    "  static bool s_android_ready_sent = false;\n"
    "  if (!s_android_ready_sent) {\n"
    "    s_android_ready_sent = true;\n"
    "    android_notify_ready();\n"
    "  }\n"
    "#endif\n"
    "  auto& context = compositor.make_context();"
)

LAUNCH_PROGRESS_ANCHORS = [
    ('  s_timelog.log("addons");', '  s_timelog.log("addons");\n#ifdef __ANDROID__\n  android_notify_loading(12, "\\xe5\\x88\\x9d\\xe5\\xa7\\x8b\\xe5\\x8c\\x96\\xe9\\x99\\x84\\xe5\\x8a\\xa0\\xe5\\x86\\x85\\xe5\\xae\\xb9\\xe2\\x80\\xa6");\n#endif'),
    ('  s_timelog.log("video");', '  s_timelog.log("video");\n#ifdef __ANDROID__\n  android_notify_loading(45, "\\xe5\\x88\\x9d\\xe5\\xa7\\x8b\\xe5\\x8c\\x96\\xe6\\x98\\xbe\\xe7\\xa4\\xba\\xe2\\x80\\xa6");\n#endif'),
    ('  s_timelog.log("audio");', '  s_timelog.log("audio");\n#ifdef __ANDROID__\n  android_notify_loading(62, "\\xe5\\x88\\x9d\\xe5\\xa7\\x8b\\xe5\\x8c\\x96\\xe9\\x9f\\xb3\\xe9\\xa2\\x91\\xe2\\x80\\xa6");\n#endif'),
    ('  s_timelog.log("resources");', '  s_timelog.log("resources");\n#ifdef __ANDROID__\n  android_notify_loading(78, "\\xe5\\x8a\\xa0\\xe8\\xbd\\xbd\\xe5\\x9b\\xbe\\xe5\\x9d\\x97\\xe4\\xb8\\x8e\\xe7\\xb2\\xbe\\xe7\\x81\\xb5\\xe2\\x80\\xa6");\n#endif'),
]


def apply_android_engine_loading_jni(main_cpp: Path) -> None:
    text = main_cpp.read_text(encoding="utf-8")
    if ANDROID_JNI_MARKER in text:
        upgrade_android_engine_loading_jni(main_cpp)
        return
    if ANDROID_JNI_ANCHOR not in text:
        raise SystemExit(f"android jni anchor not found in {main_cpp}")
    text = text.replace(ANDROID_JNI_ANCHOR, ANDROID_JNI_BLOCK + ANDROID_JNI_ANCHOR, 1)
    for anchor, replacement in LAUNCH_PROGRESS_ANCHORS:
        if anchor in text and replacement not in text:
            text = text.replace(anchor, replacement, 1)
    if LAUNCH_GAME_READY_ANCHOR in text and "android_notify_loading(90" not in text:
        text = text.replace(LAUNCH_GAME_READY_ANCHOR, LAUNCH_GAME_READY_REPLACEMENT, 1)
    main_cpp.write_text(text, encoding="utf-8", newline="\n")
    print(f"applied android engine loading jni -> {main_cpp}")


def upgrade_android_engine_loading_jni(main_cpp: Path) -> None:
    text = main_cpp.read_text(encoding="utf-8")
    changed = False
    old_ready = (
        "#ifdef __ANDROID__\n"
        "  android_notify_loading(95,"
    )
    if old_ready in text and "android_notify_loading(90" not in text:
        text = text.replace(
            """#ifdef __ANDROID__
  android_notify_loading(95, "\\xe5\\x8d\\xb3\\xe5\\xb0\\x86\\xe8\\xbf\\x9b\\xe5\\x85\\xa5\\xe5\\x85\\xb3\\xe5\\x8d\\xa1\\xe2\\x80\\xa6");
  android_notify_ready();
#endif
  m_screen_manager->run();""",
            LAUNCH_GAME_READY_REPLACEMENT,
            1,
        )
        changed = True
    if "static void android_notify_ready()" in text:
        text = text.replace("static void android_notify_ready()", "void android_notify_ready()", 1)
        changed = True
    if changed:
        main_cpp.write_text(text, encoding="utf-8", newline="\n")
        print(f"upgraded android engine loading jni -> {main_cpp}")
    else:
        print(f"skip (already applied): android loading jni in {main_cpp.name}")


def apply_android_skip_intro(main_cpp: Path) -> None:
    text = main_cpp.read_text(encoding="utf-8")
    if SKIP_INTRO_MARKER in text:
        print(f"skip (already applied): skip intro in {main_cpp.name}")
        return
    if SKIP_INTRO_ANCHOR not in text:
        raise SystemExit(f"skip intro anchor not found in {main_cpp}")
    text = text.replace(SKIP_INTRO_ANCHOR, SKIP_INTRO_REPLACEMENT, 1)
    main_cpp.write_text(text, encoding="utf-8", newline="\n")
    print(f"applied android skip intro -> {main_cpp}")


def apply_android_first_frame_ready(game_session_cpp: Path) -> None:
    text = game_session_cpp.read_text(encoding="utf-8")
    if GAME_SESSION_DRAW_MARKER in text:
        print(f"skip (already applied): first frame ready in {game_session_cpp.name}")
        return
    if GAME_SESSION_DRAW_ANCHOR not in text:
        raise SystemExit(f"game session draw anchor not found in {game_session_cpp}")
    text = text.replace(GAME_SESSION_DRAW_ANCHOR, GAME_SESSION_DRAW_REPLACEMENT, 1)
    game_session_cpp.write_text(text, encoding="utf-8", newline="\n")
    print(f"applied first frame ready notify -> {game_session_cpp}")


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
    apply_android_engine_loading_jni(main_cpp)
    apply_android_skip_intro(main_cpp)
    apply_android_first_frame_ready(fork / "src/supertux/game_session.cpp")


if __name__ == "__main__":
    main()
