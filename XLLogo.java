package com.zaka.injector.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** XL 标 —— 纯代码画，不引任何图片资源 */
public final class XLLogo {

    private XLLogo() {
    }

    /** 在 (cx, cy) 画一个 size 见方的 XL，基于 108 单位坐标等比缩放 */
    public static void draw(Canvas canvas, float cx, float cy, float size, int color) {
        float k = size / 108f;
        float ox = cx - size / 2f;
        float oy = cy - size / 2f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f * k);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        Path p = new Path();
        // X 左半
        p.moveTo(ox + 30 * k, oy + 36 * k);
        p.lineTo(ox + 43 * k, oy + 54 * k);
        p.lineTo(ox + 30 * k, oy + 72 * k);
        // X 右半
        p.moveTo(ox + 56 * k, oy + 36 * k);
        p.lineTo(ox + 43 * k, oy + 54 * k);
        p.lineTo(ox + 56 * k, oy + 72 * k);
        // L
        p.moveTo(ox + 64 * k, oy + 36 * k);
        p.lineTo(ox + 64 * k, oy + 72 * k);
        p.lineTo(ox + 80 * k, oy + 72 * k);

        canvas.drawPath(p, paint);
    }

    /** 生成一张位图版 XL，给通知 / 需要 bitmap 的地方用 */
    public static Bitmap toBitmap(int sizePx, int color) {
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        draw(c, sizePx / 2f, sizePx / 2f, sizePx, color);
        return bmp;
    }
}
