package com.example.funlife.ui.screens.platformer;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.funlife.R;
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
    private static final long MIN_OVERLAY_MS = 500L;
    private static final long MAX_OVERLAY_MS = 120_000L;
    private static final long TIP_ROTATE_MS = 3_500L;

    private static final String[] FALLBACK_STATUS = {
            "正在挂载游戏资源…",
            "正在加载图块与精灵…",
            "正在初始化显示与音频…",
            "正在加载关卡…",
    };

    private static final String[] LOADING_TIPS = {
            "💡 方向键控制 Tux 移动与跳跃",
            "⭐ 收集金币可以获得更高分数",
            "🧊 小心冰面，Tux 会滑得更远",
            "👾 踩扁敌人可以得分",
            "🎁 找到隐藏区域可能有奖励",
            "⏱ 首次加载约 15～30 秒，请稍候",
    };

    private View loadingOverlay;
    private ProgressBar loadingProgress;
    private TextView loadingStatus;
    private TextView loadingPercent;
    private TextView loadingTip;
    private TextView loadingMascot;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private long overlayShownAtMs;
    private int fallbackIndex;
    private int tipIndex;
    private int lastNativeProgress;
    private boolean engineReady;
    private boolean overlayDismissed;

    private final Runnable fallbackTicker = new Runnable() {
        @Override
        public void run() {
            if (overlayDismissed || loadingOverlay == null) return;
            if (lastNativeProgress < 10) {
                int synthetic = Math.min(85, (int) ((System.currentTimeMillis() - overlayShownAtMs) / 900L));
                applyProgress(synthetic, FALLBACK_STATUS[fallbackIndex % FALLBACK_STATUS.length]);
                fallbackIndex++;
            }
            loadingHandler.postDelayed(this, 1200L);
        }
    };

    private final Runnable tipTicker = new Runnable() {
        @Override
        public void run() {
            if (overlayDismissed || loadingTip == null) return;
            tipIndex = (tipIndex + 1) % LOADING_TIPS.length;
            loadingTip.setText(LOADING_TIPS[tipIndex]);
            loadingHandler.postDelayed(this, TIP_ROTATE_MS);
        }
    };

    private final Runnable mascotBounce = new Runnable() {
        @Override
        public void run() {
            if (overlayDismissed || loadingMascot == null) return;
            loadingMascot.animate().translationY(-12f).setDuration(400).withEndAction(() -> {
                if (loadingMascot != null) {
                    loadingMascot.animate().translationY(0f).setDuration(400).start();
                }
            }).start();
            loadingHandler.postDelayed(this, 900L);
        }
    };

    private final Runnable forceDismissRunnable = () -> {
        Log.w(TAG, "Loading overlay timeout — dismissing");
        dismissLoadingOverlay();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        org.supertux.supertux2.MainActivity.syncLocale();
        applyLoadingWindow();
        Intent intent = getIntent();
        String level = intent != null ? intent.getStringExtra(EXTRA_LEVEL_STL) : null;
        Log.i(TAG, "onCreate pid=" + android.os.Process.myPid()
                + " level=" + (level != null ? level : DEFAULT_LEVEL)
                + " staged=" + SuperTuxClassicDataPreparer.isStaged(this)
                + " extracted=" + SuperTuxClassicDataPreparer.isExtracted(this)
                + " stagedPath=" + SuperTuxClassicDataPreparer.targetFile(this).getAbsolutePath());
        if (!SuperTuxClassicDataPreparer.isStaged(this)) {
            Log.e(TAG, "data.zip not staged — aborting SDL (need prepareAndStart first)");
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
        attachLoadingOverlay();
    }

    private void applyLoadingWindow() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    private void attachLoadingOverlay() {
        ViewGroup root = (ViewGroup) SDLActivity.getContentView();
        if (root == null) {
            Log.w(TAG, "SDL layout null — skip loading overlay");
            return;
        }
        loadingOverlay = getLayoutInflater().inflate(R.layout.overlay_supertux_classic_loading, root, false);
        loadingProgress = loadingOverlay.findViewById(R.id.supertux_loading_progress);
        loadingStatus = loadingOverlay.findViewById(R.id.supertux_loading_status);
        loadingPercent = loadingOverlay.findViewById(R.id.supertux_loading_percent);
        loadingTip = loadingOverlay.findViewById(R.id.supertux_loading_tip);
        loadingMascot = loadingOverlay.findViewById(R.id.supertux_loading_mascot);
        loadingOverlay.setElevation(100f);
        loadingOverlay.setClickable(true);
        loadingOverlay.setFocusable(true);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(loadingOverlay, lp);
        overlayShownAtMs = System.currentTimeMillis();
        applyProgress(3, "正在启动 SuperTux 引擎…");
        loadingHandler.postDelayed(fallbackTicker, 800L);
        loadingHandler.postDelayed(tipTicker, TIP_ROTATE_MS);
        loadingHandler.postDelayed(mascotBounce, 600L);
        loadingHandler.postDelayed(forceDismissRunnable, MAX_OVERLAY_MS);
    }

    /** Called from native via JNI when loading stage advances. */
    @SuppressWarnings("unused")
    public void onEngineLoadingProgress(int percent, String stage) {
        lastNativeProgress = percent;
        runOnUiThread(() -> {
            applyProgress(percent, stage);
            if (percent >= 92 && !engineReady) {
                applyProgress(percent, "即将进入关卡…");
            }
            // 若 native 未触发 onEngineReady，进度到 95% 后自动淡出
            if (percent >= 95 && !overlayDismissed) {
                loadingHandler.postDelayed(() -> {
                    if (!overlayDismissed) {
                        engineReady = true;
                        applyProgress(100, "进入关卡！");
                        dismissLoadingOverlay();
                    }
                }, 1800L);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boostSdlThreadPriority();
    }

    private void boostSdlThreadPriority() {
        Thread sdl = SDLActivity.mSDLThread;
        if (sdl != null && sdl.isAlive()) {
            try {
                sdl.setPriority(Thread.MAX_PRIORITY);
            } catch (Exception e) {
                Log.w(TAG, "boostSdlThreadPriority failed", e);
            }
        }
    }

    /** Called from native via JNI when the first frame is drawn. */
    @SuppressWarnings("unused")
    public void onEngineReady() {
        engineReady = true;
        runOnUiThread(() -> {
            applyProgress(100, "进入关卡！");
            dismissLoadingOverlay();
        });
    }

    private void applyProgress(int percent, String stage) {
        if (loadingOverlay == null) return;
        int clamped = Math.max(0, Math.min(100, percent));
        loadingProgress.setProgress(clamped);
        loadingPercent.setText(clamped + "%");
        if (stage != null && !stage.isEmpty()) {
            loadingStatus.setText(stage);
        }
    }

    private void dismissLoadingOverlay() {
        if (overlayDismissed || loadingOverlay == null) return;
        long elapsed = System.currentTimeMillis() - overlayShownAtMs;
        if (engineReady && elapsed < MIN_OVERLAY_MS) {
            loadingHandler.postDelayed(this::dismissLoadingOverlay, MIN_OVERLAY_MS - elapsed);
            return;
        }
        overlayDismissed = true;
        loadingHandler.removeCallbacks(fallbackTicker);
        loadingHandler.removeCallbacks(tipTicker);
        loadingHandler.removeCallbacks(mascotBounce);
        loadingHandler.removeCallbacks(forceDismissRunnable);
        View overlay = loadingOverlay;
        ObjectAnimator fade = ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f);
        fade.setDuration(400);
        fade.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ViewGroup parent = (ViewGroup) overlay.getParent();
                if (parent != null) parent.removeView(overlay);
            }
        });
        fade.start();
        loadingOverlay = null;
    }

    @Override
    protected void onDestroy() {
        loadingHandler.removeCallbacks(fallbackTicker);
        loadingHandler.removeCallbacks(tipTicker);
        loadingHandler.removeCallbacks(mascotBounce);
        loadingHandler.removeCallbacks(forceDismissRunnable);
        super.onDestroy();
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
