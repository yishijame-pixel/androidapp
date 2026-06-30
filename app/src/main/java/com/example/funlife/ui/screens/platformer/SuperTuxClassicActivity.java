package com.example.funlife.ui.screens.platformer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.libsdl.app.SDLActivity;

/**
 * SuperTux 经典模式 — SDL 壳，加载 CI 产出的 {@code libsupertux2.so}。
 * <p>
 * 运行在独立进程 {@code :supertux}：原生引擎退出时调用 {@code _Exit()}，避免杀掉 FunLife 主进程。
 */
public class SuperTuxClassicActivity extends SDLActivity {

    public static final String TAG = "SuperTuxClassic";
    public static final String EXTRA_LEVEL_STL = "level_stl";
    public static final String EXTRA_PLAYER_SPRITE = "player_sprite";
    public static final String EXTRA_SAVE_SLOT = "save_slot";

    private static final String DEFAULT_LEVEL = "levels/world1/welcome_antarctica.stl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        org.supertux.supertux2.MainActivity.syncLocale();
        Intent intent = getIntent();
        String level = intent != null ? intent.getStringExtra(EXTRA_LEVEL_STL) : null;
        Log.i(TAG, "onCreate pid=" + android.os.Process.myPid()
                + " level=" + (level != null ? level : DEFAULT_LEVEL));
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String[] getLibraries() {
        return new String[] { "supertux2" };
    }

    @Override
    protected String getMainSharedObject() {
        return "libsupertux2.so";
    }

    @Override
    protected String getMainFunction() {
        return "SDL_main";
    }

    @Override
    protected String[] getArguments() {
        Intent intent = getIntent();
        String level = intent != null ? intent.getStringExtra(EXTRA_LEVEL_STL) : null;
        if (level == null || level.isEmpty()) {
            level = DEFAULT_LEVEL;
        }
        Log.i(TAG, "SDL argv: " + level);
        return new String[] { level };
    }
}
