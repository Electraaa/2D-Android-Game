package android.support.design.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.support.design.R;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.drawable.DrawableWrapper;

class ShadowDrawableWrapper extends DrawableWrapper {
    static final double COS_45 = Math.cos(Math.toRadians(45.0d));
    static final float SHADOW_BOTTOM_SCALE = 1.0f;
    static final float SHADOW_HORIZ_SCALE = 0.5f;
    static final float SHADOW_MULTIPLIER = 1.5f;
    static final float SHADOW_TOP_SCALE = 0.25f;
    private boolean mAddPaddingForCorners = true;
    final RectF mContentBounds;
    float mCornerRadius;
    final Paint mCornerShadowPaint;
    Path mCornerShadowPath;
    private boolean mDirty = true;
    final Paint mEdgeShadowPaint;
    float mMaxShadowSize;
    private boolean mPrintedShadowClipWarning = false;
    float mRawMaxShadowSize;
    float mRawShadowSize;
    private float mRotation;
    private final int mShadowEndColor;
    private final int mShadowMiddleColor;
    float mShadowSize;
    private final int mShadowStartColor;

    public ShadowDrawableWrapper(Context context, Drawable content, float radius, float shadowSize, float maxShadowSize) {
        super(content);
        this.mShadowStartColor = ContextCompat.getColor(context, R.color.design_fab_shadow_start_color);
        this.mShadowMiddleColor = ContextCompat.getColor(context, R.color.design_fab_shadow_mid_color);
        this.mShadowEndColor = ContextCompat.getColor(context, R.color.design_fab_shadow_end_color);
        this.mCornerShadowPaint = new Paint(5);
        this.mCornerShadowPaint.setStyle(Style.FILL);
        this.mCornerRadius = (float) Math.round(radius);
        this.mContentBounds = new RectF();
        this.mEdgeShadowPaint = new Paint(this.mCornerShadowPaint);
        this.mEdgeShadowPaint.setAntiAlias(false);
        setShadowSize(shadowSize, maxShadowSize);
    }

    private static int toEven(float value) {
        int i = Math.round(value);
        return i % 2 == 1 ? i - 1 : i;
    }

    public void setAddPaddingForCorners(boolean addPaddingForCorners) {
        this.mAddPaddingForCorners = addPaddingForCorners;
        invalidateSelf();
    }

    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        this.mCornerShadowPaint.setAlpha(alpha);
        this.mEdgeShadowPaint.setAlpha(alpha);
    }

    /* Access modifiers changed, original: protected */
    public void onBoundsChange(Rect bounds) {
        this.mDirty = true;
    }

    /* Access modifiers changed, original: 0000 */
    public void setShadowSize(float shadowSize, float maxShadowSize) {
        if (shadowSize < 0.0f || maxShadowSize < 0.0f) {
            throw new IllegalArgumentException("invalid shadow size");
        }
        shadowSize = (float) toEven(shadowSize);
        maxShadowSize = (float) toEven(maxShadowSize);
        if (shadowSize > maxShadowSize) {
            shadowSize = maxShadowSize;
            if (!this.mPrintedShadowClipWarning) {
                this.mPrintedShadowClipWarning = true;
            }
        }
        if (this.mRawShadowSize != shadowSize || this.mRawMaxShadowSize != maxShadowSize) {
            this.mRawShadowSize = shadowSize;
            this.mRawMaxShadowSize = maxShadowSize;
            this.mShadowSize = (float) Math.round(SHADOW_MULTIPLIER * shadowSize);
            this.mMaxShadowSize = maxShadowSize;
            this.mDirty = true;
            invalidateSelf();
        }
    }

    public boolean getPadding(Rect padding) {
        int vOffset = (int) Math.ceil((double) calculateVerticalPadding(this.mRawMaxShadowSize, this.mCornerRadius, this.mAddPaddingForCorners));
        int hOffset = (int) Math.ceil((double) calculateHorizontalPadding(this.mRawMaxShadowSize, this.mCornerRadius, this.mAddPaddingForCorners));
        padding.set(hOffset, vOffset, hOffset, vOffset);
        return true;
    }

    public static float calculateVerticalPadding(float maxShadowSize, float cornerRadius, boolean addPaddingForCorners) {
        if (addPaddingForCorners) {
            return (float) (((double) (SHADOW_MULTIPLIER * maxShadowSize)) + ((1.0d - COS_45) * ((double) cornerRadius)));
        }
        return SHADOW_MULTIPLIER * maxShadowSize;
    }

    public static float calculateHorizontalPadding(float maxShadowSize, float cornerRadius, boolean addPaddingForCorners) {
        if (addPaddingForCorners) {
            return (float) (((double) maxShadowSize) + ((1.0d - COS_45) * ((double) cornerRadius)));
        }
        return maxShadowSize;
    }

    public int getOpacity() {
        return -3;
    }

    public void setCornerRadius(float radius) {
        radius = (float) Math.round(radius);
        if (this.mCornerRadius != radius) {
            this.mCornerRadius = radius;
            this.mDirty = true;
            invalidateSelf();
        }
    }

    public void draw(Canvas canvas) {
        if (this.mDirty) {
            buildComponents(getBounds());
            this.mDirty = false;
        }
        drawShadow(canvas);
        super.draw(canvas);
    }

    /* Access modifiers changed, original: final */
    public final void setRotation(float rotation) {
        if (this.mRotation != rotation) {
            this.mRotation = rotation;
            invalidateSelf();
        }
    }

    private void drawShadow(Canvas canvas) {
        int saved;
        float shadowScaleBottom;
        float shadowScaleTop;
        float shadowScaleHorizontal;
        Canvas canvas2 = canvas;
        int rotateSaved = canvas.save();
        canvas2.rotate(this.mRotation, this.mContentBounds.centerX(), this.mContentBounds.centerY());
        float edgeShadowTop = (-this.mCornerRadius) - this.mShadowSize;
        float shadowOffset = this.mCornerRadius;
        boolean z = false;
        boolean drawHorizontalEdges = this.mContentBounds.width() - (2.0f * shadowOffset) > 0.0f;
        if (this.mContentBounds.height() - (2.0f * shadowOffset) > 0.0f) {
            z = true;
        }
        boolean drawVerticalEdges = z;
        float shadowOffsetTop = this.mRawShadowSize - (this.mRawShadowSize * SHADOW_TOP_SCALE);
        float shadowOffsetHorizontal = this.mRawShadowSize - (this.mRawShadowSize * SHADOW_HORIZ_SCALE);
        float shadowScaleHorizontal2 = shadowOffset / (shadowOffset + shadowOffsetHorizontal);
        float shadowScaleTop2 = shadowOffset / (shadowOffset + shadowOffsetTop);
        float shadowScaleBottom2 = shadowOffset / (shadowOffset + (this.mRawShadowSize - (this.mRawShadowSize * SHADOW_BOTTOM_SCALE)));
        int saved2 = canvas.save();
        canvas2.translate(this.mContentBounds.left + shadowOffset, this.mContentBounds.top + shadowOffset);
        canvas2.scale(shadowScaleHorizontal2, shadowScaleTop2);
        canvas2.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawHorizontalEdges) {
            canvas2.scale(SHADOW_BOTTOM_SCALE / shadowScaleHorizontal2, SHADOW_BOTTOM_SCALE);
            float width = this.mContentBounds.width() - (2.0f * shadowOffset);
            float f = -this.mCornerRadius;
            Paint paint = this.mEdgeShadowPaint;
            float f2 = f;
            Canvas canvas3 = canvas2;
            saved = saved2;
            float f3 = 0.0f;
            shadowScaleBottom = shadowScaleBottom2;
            shadowScaleBottom2 = edgeShadowTop;
            shadowScaleTop = shadowScaleTop2;
            shadowScaleTop2 = width;
            shadowScaleHorizontal = shadowScaleHorizontal2;
            shadowScaleHorizontal2 = f2;
            shadowOffsetHorizontal = SHADOW_BOTTOM_SCALE;
            canvas3.drawRect(f3, shadowScaleBottom2, shadowScaleTop2, shadowScaleHorizontal2, paint);
        } else {
            shadowScaleBottom = shadowScaleBottom2;
            shadowScaleTop = shadowScaleTop2;
            shadowScaleHorizontal = shadowScaleHorizontal2;
            float f4 = shadowOffsetTop;
            float f5 = shadowOffsetHorizontal;
            saved = saved2;
            shadowOffsetHorizontal = SHADOW_BOTTOM_SCALE;
        }
        canvas2.restoreToCount(saved);
        saved = canvas.save();
        canvas2.translate(this.mContentBounds.right - shadowOffset, this.mContentBounds.bottom - shadowOffset);
        float shadowScaleHorizontal3 = shadowScaleHorizontal;
        canvas2.scale(shadowScaleHorizontal3, shadowScaleBottom);
        canvas2.rotate(180.0f);
        canvas2.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawHorizontalEdges) {
            canvas2.scale(shadowOffsetHorizontal / shadowScaleHorizontal3, shadowOffsetHorizontal);
            shadowOffsetHorizontal = shadowScaleHorizontal3;
            canvas2.drawRect(0.0f, edgeShadowTop, this.mContentBounds.width() - (2.0f * shadowOffset), (-this.mCornerRadius) + this.mShadowSize, this.mEdgeShadowPaint);
        } else {
            shadowOffsetHorizontal = shadowScaleHorizontal3;
        }
        canvas2.restoreToCount(saved);
        saved = canvas.save();
        canvas2.translate(this.mContentBounds.left + shadowOffset, this.mContentBounds.bottom - shadowOffset);
        canvas2.scale(shadowOffsetHorizontal, shadowScaleBottom);
        canvas2.rotate(270.0f);
        canvas2.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawVerticalEdges) {
            canvas2.scale(SHADOW_BOTTOM_SCALE / shadowScaleBottom, SHADOW_BOTTOM_SCALE);
            canvas2.drawRect(0.0f, edgeShadowTop, this.mContentBounds.height() - (2.0f * shadowOffset), -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas2.restoreToCount(saved);
        saved = canvas.save();
        canvas2.translate(this.mContentBounds.right - shadowOffset, this.mContentBounds.top + shadowOffset);
        shadowScaleHorizontal3 = shadowScaleTop;
        canvas2.scale(shadowOffsetHorizontal, shadowScaleHorizontal3);
        canvas2.rotate(90.0f);
        canvas2.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (drawVerticalEdges) {
            canvas2.scale(SHADOW_BOTTOM_SCALE / shadowScaleHorizontal3, SHADOW_BOTTOM_SCALE);
            canvas2.drawRect(0.0f, edgeShadowTop, this.mContentBounds.height() - (2.0f * shadowOffset), -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas2.restoreToCount(saved);
        canvas2.restoreToCount(rotateSaved);
    }

    private void buildShadowCorners() {
        int i;
        RectF innerBounds = new RectF(-this.mCornerRadius, -this.mCornerRadius, this.mCornerRadius, this.mCornerRadius);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-this.mShadowSize, -this.mShadowSize);
        if (this.mCornerShadowPath == null) {
            this.mCornerShadowPath = new Path();
        } else {
            this.mCornerShadowPath.reset();
        }
        this.mCornerShadowPath.setFillType(FillType.EVEN_ODD);
        this.mCornerShadowPath.moveTo(-this.mCornerRadius, 0.0f);
        this.mCornerShadowPath.rLineTo(-this.mShadowSize, 0.0f);
        this.mCornerShadowPath.arcTo(outerBounds, 180.0f, 90.0f, false);
        this.mCornerShadowPath.arcTo(innerBounds, 270.0f, -90.0f, false);
        this.mCornerShadowPath.close();
        float shadowRadius = -outerBounds.top;
        if (shadowRadius > 0.0f) {
            float startRatio = this.mCornerRadius / shadowRadius;
            float midRatio = startRatio + ((SHADOW_BOTTOM_SCALE - startRatio) / 2.0f);
            Shader shader = r8;
            Paint paint = this.mCornerShadowPaint;
            i = 3;
            Shader radialGradient = new RadialGradient(0.0f, 0.0f, shadowRadius, new int[]{0, this.mShadowStartColor, this.mShadowMiddleColor, this.mShadowEndColor}, new float[]{0.0f, startRatio, midRatio, SHADOW_BOTTOM_SCALE}, TileMode.CLAMP);
            paint.setShader(shader);
        } else {
            i = 3;
        }
        Paint paint2 = this.mEdgeShadowPaint;
        float f = innerBounds.top;
        float f2 = outerBounds.top;
        int[] iArr = new int[i];
        iArr[0] = this.mShadowStartColor;
        iArr[1] = this.mShadowMiddleColor;
        iArr[2] = this.mShadowEndColor;
        paint2.setShader(new LinearGradient(0.0f, f, 0.0f, f2, iArr, new float[]{0.0f, SHADOW_HORIZ_SCALE, SHADOW_BOTTOM_SCALE}, TileMode.CLAMP));
        this.mEdgeShadowPaint.setAntiAlias(false);
    }

    private void buildComponents(Rect bounds) {
        float verticalOffset = this.mRawMaxShadowSize * SHADOW_MULTIPLIER;
        this.mContentBounds.set(((float) bounds.left) + this.mRawMaxShadowSize, ((float) bounds.top) + verticalOffset, ((float) bounds.right) - this.mRawMaxShadowSize, ((float) bounds.bottom) - verticalOffset);
        getWrappedDrawable().setBounds((int) this.mContentBounds.left, (int) this.mContentBounds.top, (int) this.mContentBounds.right, (int) this.mContentBounds.bottom);
        buildShadowCorners();
    }

    public float getCornerRadius() {
        return this.mCornerRadius;
    }

    public void setShadowSize(float size) {
        setShadowSize(size, this.mRawMaxShadowSize);
    }

    public void setMaxShadowSize(float size) {
        setShadowSize(this.mRawShadowSize, size);
    }

    public float getShadowSize() {
        return this.mRawShadowSize;
    }

    public float getMaxShadowSize() {
        return this.mRawMaxShadowSize;
    }

    public float getMinWidth() {
        return (this.mRawMaxShadowSize * 2.0f) + (Math.max(this.mRawMaxShadowSize, this.mCornerRadius + (this.mRawMaxShadowSize / 2.0f)) * 2.0f);
    }

    public float getMinHeight() {
        return ((this.mRawMaxShadowSize * SHADOW_MULTIPLIER) * 2.0f) + (Math.max(this.mRawMaxShadowSize, this.mCornerRadius + ((this.mRawMaxShadowSize * SHADOW_MULTIPLIER) / 2.0f)) * 2.0f);
    }
}
