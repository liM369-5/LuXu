package com.zaka.injector.ui;

import android.content.Context;
import android.content.res.Configuration;

/** 配色 + 尺寸。跟随系统深浅色。 */
public final class Theme {

    private Theme() {
    }

    public static boolean dark(Context c) {
        int mode = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /* ---- 颜色 ---- */

    public static int pageBg(Context c) {
        return dark(c) ? 0xFF000000 : 0xFFF5F5F7;
    }

    public static int cardBg(Context c) {
        return dark(c) ? 0xF21C1C1E : 0xF2FFFFFF;
    }

    public static int text(Context c) {
        return dark(c) ? 0xFFF2F2F7 : 0xFF1C1C1E;
    }

    public static int subText(Context c) {
        return dark(c) ? 0x8CFFFFFF : 0x8C000000;
    }

    public static int divider(Context c) {
        return dark(c) ? 0x1AFFFFFF : 0x14000000;
    }

    /** 卡片里的内嵌框（日志区）底色 —— 比卡片再深/浅一档，形成层次 */
    public static int innerBg(Context c) {
        return dark(c) ? 0xFF0C0C0E : 0xFFF7F7FA;
    }

    public static int accent() {
        return 0xFF0A84FF;
    }

    public static int okColor() {
        return 0xFF30D158;
    }

    public static int failColor() {
        return 0xFFFF453A;
    }

    public static int waitColor() {
        return 0xFFFFD60A;
    }

    /** 警告色（黄）—— 状态没满足时用 */
    public static int warnColor() {
        return 0xFFFF9F0A;
    }

    public static int ballBg(Context c) {
        return dark(c) ? 0xB3101012 : 0xB3FFFFFF;
    }

    public static int ballStroke(Context c) {
        return dark(c) ? 0x33FFFFFF : 0x26000000;
    }

    public static int ballLogo(Context c) {
        return dark(c) ? 0xFFF2F2F7 : 0xFF1C1C1E;
    }

    /* ---- 尺寸 ---- */

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
