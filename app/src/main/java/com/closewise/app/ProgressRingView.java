package com.closewise.app;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class ProgressRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint number = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caption = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;

    ProgressRingView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setStrokeWidth(dp(13));
        track.setColor(0xFF142641);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setStrokeWidth(dp(13));
        arc.setColor(0xFF42DFFF);
        arc.setShadowLayer(dp(10), 0, 0, 0x9942DFFF);
        number.setColor(0xFFF8FBFF);
        number.setTextAlign(Paint.Align.CENTER);
        number.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        number.setTextSize(dp(45));
        caption.setColor(0xFF9FB5D2);
        caption.setTextAlign(Paint.Align.CENTER);
        caption.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        caption.setTextSize(dp(12));
        setContentDescription("Cleanup progress");
    }

    public float getProgress() { return progress; }

    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(100f, value));
        setContentDescription("Cleanup progress " + Math.round(progress) + " percent");
        invalidate();
    }

    void animateTo(int value) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "progress", progress, value);
        animator.setDuration(240L);
        animator.start();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = dp(20);
        RectF oval = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(oval, -90f, 360f, false, track);
        canvas.drawArc(oval, -90f, progress * 3.6f, false, arc);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        Paint.FontMetrics metrics = number.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2f - dp(8);
        canvas.drawText(String.valueOf(Math.round(progress)), centerX, baseline, number);
        canvas.drawText("PERCENT", centerX, centerY + dp(40), caption);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
