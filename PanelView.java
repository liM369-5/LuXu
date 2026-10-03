package com.zaka.injector.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.zaka.injector.core.AppConfig;
import com.zaka.injector.inject.StepEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 悬浮面板：顶部目标+通道状态 / 中间开关列表 / 底部通栏注入按钮 */
public class PanelView extends FrameLayout {

    public interface Callback {
        void onInject(List<String> payloads);

        void onRollback();

        void onClose();
    }

    private final Callback cb;
    private final List<TextView> stepTitles = new ArrayList<>();
    private final List<View> stepDots = new ArrayList<>();
    private final List<TextView> stepDetails = new ArrayList<>();
    private final Map<String, Switch> switches = new LinkedHashMap<>();

    private View card;
    private LinearLayout switchBox;
    private LinearLayout stepBox;
    private TextView channelText;
    private TextView statusText;
    private TextView injectBtn;
    private TextView rollbackBtn;
    private boolean busy;

    public PanelView(Context c, Callback cb) {
        super(c);
        this.cb = cb;
        setBackgroundColor(0x59000000);
        setOnClickListener(v -> {
            if (!busy) {
                cb.onClose();
            }
        });
        build(c);
    }

    /* ============================ 构建 ============================ */

    private void build(Context c) {
        card = buildCard(c);
        LayoutParams lp = new LayoutParams(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        addView(card, lp);
        card.setAlpha(0f);
        card.setScaleX(0.88f);
        card.setScaleY(0.88f);
    }

    private View buildCard(Context c) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(14));
        root.setBackground(round(Theme.cardBg(c), dp(26)));
        root.setElevation(dp(14));
        root.setOnClickListener(v -> {
            /* 吞掉，不关面板 */
        });

        /* 头部 */
        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(c);
        title.setText(AppConfig.TARGET_NAME);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Theme.text(c));
        head.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        channelText = new TextView(c);
        channelText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        channelText.setTextColor(Theme.subText(c));
        channelText.setText("通道检测中…");
        head.addView(channelText);
        root.addView(head);

        TextView pkg = new TextView(c);
        pkg.setText(AppConfig.TARGET_PKG);
        pkg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        pkg.setTextColor(Theme.subText(c));
        pkg.setPadding(0, dp(2), 0, 0);
        root.addView(pkg);

        root.addView(line(c));

        /* 可滚动内容区 */
        ScrollView sc = new ScrollView(c);
        sc.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(206)));
        sc.setClipToPadding(false);
        LinearLayout holder = new LinearLayout(c);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setPadding(0, dp(6), 0, dp(6));
        sc.addView(holder, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        switchBox = buildSwitchBox(c);
        holder.addView(switchBox, matchWrap());

        stepBox = buildStepBox(c);
        stepBox.setVisibility(View.GONE);
        holder.addView(stepBox, matchWrap());

        root.addView(sc);
        root.addView(line(c));

        /* 底部 */
        injectBtn = new TextView(c);
        injectBtn.setText("注 入");
        injectBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        injectBtn.setTypeface(Typeface.DEFAULT_BOLD);
        injectBtn.setTextColor(0xFFFFFFFF);
        injectBtn.setGravity(Gravity.CENTER);
        injectBtn.setBackground(round(Theme.accent(), dp(14)));
        injectBtn.setOnClickListener(v -> fireInject());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        blp.topMargin = dp(10);
        root.addView(injectBtn, blp);

        LinearLayout foot = new LinearLayout(c);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        foot.setPadding(dp(2), dp(9), dp(2), 0);

        statusText = new TextView(c);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        statusText.setTextColor(Theme.subText(c));
        statusText.setText("待命中");
        foot.addView(statusText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        rollbackBtn = new TextView(c);
        rollbackBtn.setText("回滚");
        rollbackBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        rollbackBtn.setTextColor(Theme.accent());
        rollbackBtn.setPadding(dp(10), dp(6), dp(2), dp(6));
        rollbackBtn.setOnClickListener(v -> {
            if (!busy) {
                cb.onRollback();
            }
        });
        foot.addView(rollbackBtn);
        root.addView(foot);

        return root;
    }

    private LinearLayout buildSwitchBox(Context c) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        for (AppConfig.SwitchDef def : AppConfig.SWITCHES) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView lb = new TextView(c);
            lb.setText(def.label);
            lb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            lb.setTextColor(Theme.text(c));

            TextView sub = new TextView(c);
            sub.setText(AppConfig.readableName(def.targetName()));
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            sub.setTextColor(Theme.subText(c));
            sub.setSingleLine(true);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            sub.setPadding(0, dp(1), dp(6), 0);

            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(lb);
            col.addView(sub);
            row.addView(col, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Switch sw = new Switch(c);
            sw.setChecked(def.defaultOn);
            row.addView(sw);
            switches.put(def.asset, sw);

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            box.addView(row, rlp);
        }
        return box;
    }

    private LinearLayout buildStepBox(Context c) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        String[] names = {"通道校验", "环境检查", "备份原文件", "释放 payload", "写入目标目录", "回读校验"};
        for (String n : names) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            View dot = new View(c);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(7), dp(7));
            dlp.rightMargin = dp(10);
            dot.setBackground(round(Theme.divider(c), dp(4)));
            row.addView(dot, dlp);

            LinearLayout textCol = new LinearLayout(c);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView t = new TextView(c);
            t.setText(n);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            t.setTextColor(Theme.subText(c));
            textCol.addView(t);

            TextView d = new TextView(c);
            d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            d.setTextColor(Theme.subText(c));
            d.setMaxLines(1);
            d.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textCol.addView(d);

            row.addView(textCol, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            box.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

            stepDots.add(dot);
            stepTitles.add(t);
            stepDetails.add(d);
        }
        return box;
    }

    /* ============================ 对外 ============================ */

    public void show() {
        setVisibility(View.VISIBLE);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(320)
                .setInterpolator(new OvershootInterpolator(0.35f))
                .start();
    }

    public void hide() {
        card.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f)
                .setDuration(160)
                .withEndAction(() -> setVisibility(View.GONE))
                .start();
    }

    public void setChannel(String text, boolean good) {
        channelText.setText(text);
        channelText.setTextColor(good ? Theme.okColor() : Theme.warnColor());
    }

    public void setBusy(boolean b) {
        busy = b;
        injectBtn.setAlpha(b ? 0.45f : 1f);
        rollbackBtn.setAlpha(b ? 0.45f : 1f);
        injectBtn.setText(b ? "执行中…" : "注 入");
    }

    public void setStatus(String s, int color) {
        statusText.setText(s);
        statusText.setTextColor(color);
    }

    public void resetSteps() {
        switchBox.setVisibility(View.GONE);
        stepBox.setVisibility(View.VISIBLE);
        for (int i = 0; i < stepDots.size(); i++) {
            stepDots.get(i).setBackground(round(Theme.divider(context()), dp(4)));
            stepTitles.get(i).setTextColor(Theme.subText(context()));
            stepDetails.get(i).setText("");
        }
    }

    public void showSwitches() {
        stepBox.setVisibility(View.GONE);
        switchBox.setVisibility(View.VISIBLE);
    }

    public void onStep(StepEvent e) {
        int i = e.index - 1;
        if (i < 0 || i >= stepDots.size()) {
            return;
        }
        int color;
        switch (e.state) {
            case OK:
                color = Theme.okColor();
                break;
            case FAIL:
                color = Theme.failColor();
                break;
            case RUNNING:
                color = Theme.waitColor();
                break;
            default:
                color = Theme.divider(context());
                break;
        }
        stepDots.get(i).setBackground(round(color, dp(4)));
        stepTitles.get(i).setTextColor(e.state == StepEvent.State.RUNNING
                || e.state == StepEvent.State.FAIL ? Theme.text(context()) : Theme.subText(context()));
        stepDetails.get(i).setText(e.detail);
    }

    public List<String> enabledPayloads() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Switch> en : switches.entrySet()) {
            if (en.getValue().isChecked()) {
                out.add(en.getKey());
            }
        }
        return out;
    }

    private void fireInject() {
        if (busy) {
            return;
        }
        List<String> ps = enabledPayloads();
        if (ps.isEmpty()) {
            setStatus("至少开一个开关", Theme.failColor());
            return;
        }
        resetSteps();
        cb.onInject(ps);
    }

    /* ============================ 小工具 ============================ */

    private Context context() {
        return getContext();
    }

    private int dp(float v) {
        return Theme.dp(getContext(), v);
    }

    private View line(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Theme.divider(c));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(0.6f)));
        v.setLayoutParams(lp);
        return v;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }
}
