/* Copyright (C) 2012 The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.efidroid.efidroidmanager.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.Util;

/**
 * Custom view that shows a pie chart and, optionally, a label.
 */
public class ProgressCircle extends ViewGroup {
    private float mBaseStrokeWidth;
    private float mProgressStrokeWidth;
    private int mBaseStrokeColor;
    private int mProgressStrokeColor;

    private float mMinValue;
    private float mMaxValue;
    private float mCurrentValue;

    private int mStartAngle;
    private int mSweepAngle;

    private int mFillColor;
    private String mContentText;
    private float mContentTextSize;
    private int mContentTextColor;
    private boolean mHideProgress;

    // generated
    private int mCurrentSweepAngle;
    private PieView mPieView;
    private RectF mPieBounds = new RectF();
    private RectF mShadowBounds = new RectF();
    private Paint mBaseStrokePaint;
    private Paint mProgressStrokePaint;
    private Paint mShadowPaint;
    private Paint mBackgroundPaint;
    private Paint mTextPaint;

    /**
     * Class constructor taking only a context. Use this constructor to create
     * {@link ProgressCircle} objects from your own code.
     *
     * @param context
     */
    public ProgressCircle(Context context) {
        super(context);
        init();
    }

    /**
     * Class constructor taking a context and an attribute set. This constructor
     * is used by the layout engine to construct a {@link ProgressCircle} from a set of
     * XML attributes.
     *
     * @param context
     * @param attrs   An attribute set which can contain attributes from
     *                {@link org.efidroid.efidroidmanager.R.styleable} as well as attributes inherited
     *                from {@link android.view.View}.
     */
    public ProgressCircle(Context context, AttributeSet attrs) {
        super(context, attrs);

        // attrs contains the raw values for the XML attributes
        // that were specified in the layout, which don't include
        // attributes set by styles or themes, and which may have
        // unresolved references. Call obtainStyledAttributes()
        // to get the final values for each attribute.
        //
        // This call uses R.styleable.PieChart, which is an array of
        // the custom attributes that were declared in attrs.xml.
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ProgressCircle,
                0, 0
        );

        try {
            // Retrieve the values from the TypedArray and store into
            // fields of this class.
            //
            // The R.styleable.PieChart_* constants represent the index for
            // each custom attribute in the R.styleable.PieChart array.
            mBaseStrokeWidth = a.getDimension(R.styleable.ProgressCircle_baseStrokeWidth, 0.0f);
            mProgressStrokeWidth = a.getDimension(R.styleable.ProgressCircle_progressStrokeWidth, 0.0f);
            mBaseStrokeColor = a.getColor(R.styleable.ProgressCircle_baseStrokeColor, 0xff000000);
            mProgressStrokeColor = a.getColor(R.styleable.ProgressCircle_progressStrokeColor, 0xff000000);

            mMinValue = a.getFloat(R.styleable.ProgressCircle_minValue, 0);
            mMaxValue = a.getFloat(R.styleable.ProgressCircle_maxValue, 100);
            mCurrentValue = a.getFloat(R.styleable.ProgressCircle_currentValue, 0);
            mStartAngle = a.getInt(R.styleable.ProgressCircle_startAngle, -225);
            mSweepAngle = a.getInt(R.styleable.ProgressCircle_sweepAngle, 360);

            mFillColor = a.getColor(R.styleable.ProgressCircle_fillColor, 0x00000000);
            mContentText = a.getString(R.styleable.ProgressCircle_contentText);
            mContentTextSize = a.getDimension(R.styleable.ProgressCircle_contentTextSize, 0.0f);
            mContentTextColor = a.getColor(R.styleable.ProgressCircle_contentTextColor, 0xff000000);
            mHideProgress = a.getBoolean(R.styleable.ProgressCircle_hideProgress, false);

            setValue(mCurrentValue, false, 0);
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }

        init();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do nothing. Do not call the superclass method--that would start a layout pass
        // on this view's children. PieChart lays out its children in onSizeChanged().
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the shadow
        canvas.drawOval(mShadowBounds, mShadowPaint);
    }


    //
    // Measurement functions. This example uses a simple heuristic: it assumes that
    // the pie chart should be at least as wide as its label.
    //
    @Override
    protected int getSuggestedMinimumWidth() {
        return (int) 0f;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return (int) 0f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for a width based on our minimum
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();

        int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));

        // Whatever the width ends up being, ask for a height that would let the pie
        // get as big as it can     *                {@link org.efidroid.efidroidmanager.R.styleable.} as well as attributes inherited
        int minh = w + getPaddingBottom() + getPaddingTop();
        int h = Math.min(MeasureSpec.getSize(heightMeasureSpec), minh);

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //
        // Set dimensions for text, pie chart, etc
        //
        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        float ww = (float) w - xpad;
        float hh = (float) h - ypad;

        // Figure out how big we can make the pie.
        float diameter = Math.min(ww, hh);
        mPieBounds = new RectF(
                0.0f,
                0.0f,
                diameter,
                diameter);
        mPieBounds.offsetTo(getPaddingLeft(), getPaddingTop());

        mShadowBounds = new RectF(
                mPieBounds.left + 10,
                mPieBounds.bottom + 10,
                mPieBounds.right - 10,
                mPieBounds.bottom + 20);

        // Lay out the child view that actually draws the pie.
        mPieView.layout((int) mPieBounds.left,
                (int) mPieBounds.top,
                (int) mPieBounds.right,
                (int) mPieBounds.bottom);
        mPieView.setPivot(mPieBounds.width() / 2, mPieBounds.height() / 2);
    }

    /**
     * Do all of the recalculations needed when the data array changes.
     */
    private void onDataChanged() {
        mCurrentSweepAngle = (int) (mCurrentValue * (mSweepAngle / Math.abs(mMaxValue - mMinValue)));

        invalidate();
    }

    /**
     * Initialize the control. This code is in a separate method so that it can be
     * called from both constructors.
     */
    private void init() {
        // Force the background to software rendering because otherwise the Blur
        // filter won't work.
        setLayerToSW(this);

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(mFillColor);

        mBaseStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBaseStrokePaint.setStyle(Paint.Style.STROKE);
        mBaseStrokePaint.setStrokeWidth(mBaseStrokeWidth);
        mBaseStrokePaint.setColor(mBaseStrokeColor);

        // Set up the paint for the pie slices
        mProgressStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressStrokePaint.setStyle(Paint.Style.STROKE);
        mProgressStrokePaint.setStrokeWidth(mProgressStrokeWidth);
        mProgressStrokePaint.setColor(mProgressStrokeColor);
        mProgressStrokePaint.setStrokeCap(Paint.Cap.SQUARE);

        // Set up the paint for the shadow
        mShadowPaint = new Paint(0);
        mShadowPaint.setColor(0xff101010);
        mShadowPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(mContentTextColor);
        if (mContentTextSize > 0)
            mTextPaint.setTextSize(mContentTextSize);

        // Add a child view to draw the pie. Putting this in a child view
        // makes it possible to draw it on a separate hardware layer that rotates
        // independently
        mPieView = new PieView(getContext());
        addView(mPieView);
    }

    private void setLayerToSW(View v) {
        if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    private void setLayerToHW(View v) {
        if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    @SuppressWarnings("unused")
    public void setValue(float value, boolean animate, long duration) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        if (value < 0) {
            value = 0;
        }

        if (animate) {
            ValueAnimator anim = ValueAnimator.ofFloat(mCurrentValue, value);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float val = (Float) valueAnimator.getAnimatedValue();
                    mCurrentValue = val;
                    onDataChanged();
                }
            });
            anim.setDuration(duration);
            anim.start();
        } else {
            mCurrentValue = value;
            onDataChanged();
        }
    }

    public void setProgressStrokeColor(int color, boolean animate, long duration) {
        if (animate) {
            ValueAnimator anim = Util.CompatAnimatorOfArgb(mProgressStrokeColor, color);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    mProgressStrokeColor = val;
                    mProgressStrokePaint.setColor(mProgressStrokeColor);
                    invalidate();
                }
            });
            anim.setDuration(duration);
            anim.start();
        } else {
            mProgressStrokeColor = color;
            mProgressStrokePaint.setColor(mProgressStrokeColor);
            invalidate();
        }
    }

    public void setFillColor(int color, boolean animate, long duration) {
        if (animate) {
            ValueAnimator anim = Util.CompatAnimatorOfArgb(mFillColor, color);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    mFillColor = val;
                    mBackgroundPaint.setColor(mFillColor);
                    invalidate();
                }
            });
            anim.setDuration(duration);
            anim.start();
        } else {
            mFillColor = color;
            mBackgroundPaint.setColor(mFillColor);
            invalidate();
        }
    }

    public void setContentText(String text) {
        mContentText = text;
        invalidate();
    }

    public void setContentText(int id) {
        setContentText(getContext().getString(id));
    }

    public void setContentTextColor(int color, boolean animate, long duration) {
        if (animate) {
            ValueAnimator anim = Util.CompatAnimatorOfArgb(mContentTextColor, color);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    mContentTextColor = val;
                    mTextPaint.setColor(mContentTextColor);
                    invalidate();
                }
            });
            anim.setDuration(duration);
            anim.start();
        } else {
            mContentTextColor = color;
            mTextPaint.setColor(mContentTextColor);
            invalidate();
        }
    }


    public void setProgressHidden(final boolean hidden, boolean animate, long duration) {
        if (mHideProgress == hidden)
            return;

        if (animate) {
            ValueAnimator anim = ValueAnimator.ofInt(mHideProgress ? 0 : 255, hidden ? 0 : 255);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    mBaseStrokePaint.setAlpha(val);
                    mProgressStrokePaint.setAlpha(val);
                    invalidate();
                }
            });
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mBaseStrokePaint.setAlpha(255);
                    mProgressStrokePaint.setAlpha(255);

                    mHideProgress = hidden;
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            anim.setDuration(duration);

            mHideProgress = false;
            anim.start();
        } else {
            mHideProgress = hidden;
            invalidate();
        }
    }

    /**
     * Internal child class that draws the pie chart onto a separate hardware layer
     * when necessary.
     */
    private class PieView extends View {
        // Used for SDK < 11
        private PointF mPivot = new PointF();

        /**
         * Construct a PieView
         *
         * @param context
         */
        public PieView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawArc(mBounds,
                    0,
                    360,
                    true, mBackgroundPaint);

            if (!mHideProgress) {
                canvas.drawArc(mBounds,
                        mStartAngle,
                        mSweepAngle,
                        false, mBaseStrokePaint);

                canvas.drawArc(mBounds,
                        mStartAngle,
                        mCurrentSweepAngle,
                        false, mProgressStrokePaint);
            }

            if (mContentText != null)
                canvas.drawText(mContentText, mBounds.centerX(), mBounds.centerY() + mContentTextSize / 3.0f, mTextPaint);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mMaxStrokeWidth = Math.max(mBaseStrokeWidth, mProgressStrokeWidth);
            mBounds = new RectF(mMaxStrokeWidth / 2f, mMaxStrokeWidth / 2f, w - mMaxStrokeWidth / 2f, h - mMaxStrokeWidth / 2f);
        }

        RectF mBounds;
        float mMaxStrokeWidth;

        public void setPivot(float x, float y) {
            mPivot.x = x;
            mPivot.y = y;
            if (Build.VERSION.SDK_INT >= 11) {
                setPivotX(x);
                setPivotY(y);
            } else {
                invalidate();
            }
        }
    }

}