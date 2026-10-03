package com.zaka.injector.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.zaka.injector.MainActivity;
import com.zaka.injector.R;
import com.zaka.injector.core.ChannelManager;
import com.zaka.injector.core.ShellRunner;
import com.zaka.injector.inject.DefaultInjectEngine;
import com.zaka.injector.inject.StepEvent;
import com.zaka.injector.log.InjectLog;

import java.util.List;

/** 悬浮球 + 面板的前台服务 */
public class FloatingService extends Service {

    private static final String CHANNEL = "zaka_floating";
    private static final int NOTI_ID = 0x5A4B;

    public static void show(Context c) {
        Intent i = new Intent(c, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            c.startForegroundService(i);
        } else {
            c.startService(i);
        }
    }

    public static void hide(Context c) {
        c.stopService(new Intent(c, FloatingService.class));
    }

    private WindowManager wm;
    private View ballView;
    private PanelView panelView;
    private WindowManager.LayoutParams ballLp;
    private WindowManager.LayoutParams panelLp;
    private DefaultInjectEngine engine;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        engine = new DefaultInjectEngine(this);
        startForegroundCompat();
        addBall();
        addPanel();
        refreshChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (ballView != null) {
                wm.removeView(ballView);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (panelView != null) {
                wm.removeView(panelView);
            }
        } catch (Throwable ignored) {
        }
        ballView = null;
        panelView = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ============================ 通知 ============================ */

    private void startForegroundCompat() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "注入器运行中",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("Zaka 注入器")
                .setContentText("悬浮球已就绪 · 点一下开始")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        startForeground(NOTI_ID, n);
    }

    /* ============================ 悬浮球 ============================ */

    /**
     * 背景模糊（安卓 12+）。setBackgroundBlurRadius 是系统 API，公开 SDK 里没暴露，
     * 只能反射调；设备/系统不支持就静静跳过，不影响任何功能。
     */
    private static void applyBlur(WindowManager.LayoutParams lp, int radius) {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            java.lang.reflect.Method m = WindowManager.LayoutParams.class
                    .getMethod("setBackgroundBlurRadius", int.class);
            m.invoke(lp, radius);
        } catch (Throwable ignored) {
        }
    }

    private void addBall() {
        ballView = new BallView();
        int size = Theme.dp(this, 52);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        if (Build.VERSION.SDK_INT >= 31) {
            flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        }

        ballLp = new WindowManager.LayoutParams(size, size, type, flags,
                PixelFormat.TRANSLUCENT);
        ballLp.gravity = Gravity.TOP | Gravity.START;
        ballLp.x = getResources().getDisplayMetrics().widthPixels - size - Theme.dp(this, 10);
        ballLp.y = getResources().getDisplayMetrics().heightPixels / 3;
        applyBlur(ballLp, Theme.dp(this, 18));

        ballView.setOnTouchListener(new View.OnTouchListener() {
            float downRawX, downRawY;
            int startX, startY;
            boolean moved;
            long downAt;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = e.getRawX();
                        downRawY = e.getRawY();
                        startX = ballLp.x;
                        startY = ballLp.y;
                        moved = false;
                        downAt = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - downRawX);
                        int dy = (int) (e.getRawY() - downRawY);
                        if (Math.abs(dx) > Theme.dp(FloatingService.this, 4)
                                || Math.abs(dy) > Theme.dp(FloatingService.this, 4)) {
                            moved = true;
                        }
                        if (moved) {
                            ballLp.x = startX + dx;
                            ballLp.y = startY + dy;
                            try {
                                wm.updateViewLayout(ballView, ballLp);
                            } catch (Throwable ignored) {
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!moved && System.currentTimeMillis() - downAt < 500) {
                            togglePanel();
                        } else if (moved) {
                            snapToEdge();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        wm.addView(ballView, ballLp);
    }

    /** 往左右边缘吸附 */
    private void snapToEdge() {
        int screen = getResources().getDisplayMetrics().widthPixels;
        int size = ballLp.width;
        ballLp.x = ballLp.x + size / 2 < screen / 2 ? Theme.dp(this, 6) : screen - size - Theme.dp(this, 6);
        try {
            wm.updateViewLayout(ballView, ballLp);
        } catch (Throwable ignored) {
        }
    }

    private class BallView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        BallView() {
            super(FloatingService.this);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(getWidth(), getHeight()) / 2f - Theme.dp(getContext(), 2);

            fill.setColor(Theme.ballBg(getContext()));
            canvas.drawCircle(cx, cy, r, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Theme.dp(getContext(), 1f));
            stroke.setColor(Theme.ballStroke(getContext()));
            canvas.drawCircle(cx, cy, r - stroke.getStrokeWidth() / 2f, stroke);

            XLLogo.draw(canvas, cx, cy, r * 1.9f, Theme.ballLogo(getContext()));
        }
    }

    /* ============================ 面板 ============================ */

    private void addPanel() {
        panelView = new PanelView(this, new PanelView.Callback() {
            @Override
            public void onInject(List<String> payloads) {
                startInject(payloads);
            }

            @Override
            public void onRollback() {
                startRollback();
            }

            @Override
            public void onClose() {
                hidePanel();
            }
        });

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        panelLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        panelLp.gravity = Gravity.TOP | Gravity.START;

        panelView.setVisibility(View.GONE);
        wm.addView(panelView, panelLp);
    }

    private void togglePanel() {
        if (panelView.getVisibility() == View.VISIBLE) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void showPanel() {
        panelView.showSwitches();
        panelView.show();
        refreshChannel();
    }

    private void hidePanel() {
        panelView.hide();
    }

    /* ============================ 通道 / 注入 ============================ */

    private void refreshChannel() {
        ShellRunner cached = ChannelManager.cached();
        if (cached != null && cached.available()) {
            panelView.setChannel(cached.name() + " 已就绪", true);
            return;
        }
        panelView.setChannel("检测中…", false);
        ChannelManager.detect((runner, error) -> {
            if (runner != null) {
                panelView.setChannel(runner.name() + " 已就绪", true);
            } else {
                panelView.setChannel("无通道", false);
            }
        });
    }

    private void startInject(List<String> payloads) {
        panelView.setBusy(true);
        panelView.setStatus("执行中…", Theme.waitColor());
        InjectLog.step(this, "开始注入，共 " + payloads.size() + " 个文件");
        engine.run(payloads, new com.zaka.injector.inject.InjectEngine.Listener() {
            @Override
            public void onStep(StepEvent e) {
                panelView.onStep(e);
            }

            @Override
            public void onFinished(boolean ok, String summary) {
                panelView.setBusy(false);
                panelView.setStatus(summary, ok ? Theme.okColor() : Theme.failColor());
            }
        });
    }

    private void startRollback() {
        panelView.setBusy(true);
        panelView.resetSteps();
        panelView.setStatus("回滚中…", Theme.waitColor());
        engine.rollback(new com.zaka.injector.inject.InjectEngine.Listener() {
            @Override
            public void onStep(StepEvent e) {
                panelView.onStep(e);
            }

            @Override
            public void onFinished(boolean ok, String summary) {
                panelView.setBusy(false);
                panelView.setStatus(summary, ok ? Theme.okColor() : Theme.failColor());
            }
        });
    }
}
