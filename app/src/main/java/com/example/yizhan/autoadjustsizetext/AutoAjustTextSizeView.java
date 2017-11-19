package com.example.yizhan.autoadjustsizetext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class AutoAjustTextSizeView extends View {

    /**
     * 默认的"给定最大字号"
     */
    private float mDefualtMaxTextSize = 30;
    private float mMaxTextSize;
    private float mDiffTextSizeScale;
    private int mTextColor;
    private String mText;

    //控件的宽高
    private int mWidth;
    private int mHeight;

    private Paint mPaint;

    public AutoAjustTextSizeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoAdjustView);
        mMaxTextSize = typedArray.getFloat(R.styleable.AutoAdjustView_maxTextSize, mDefualtMaxTextSize * getFontScale(context));
        mDiffTextSizeScale = typedArray.getFloat(R.styleable.AutoAdjustView_diffTextSizeScale, 0);
        mTextColor = typedArray.getColor(R.styleable.AutoAdjustView_textColor, Color.BLACK);
        mText = typedArray.getText(R.styleable.AutoAdjustView_text).toString();

        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);//画笔模式为填充
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setColor(mTextColor);//默认颜色
        mPaint.setTextAlign(Paint.Align.RIGHT);//默认为右侧
    }

    /**
     * 获取字体缩放比例
     *
     * @param context
     * @return 字体缩放比例
     */
    private float getFontScale(Context context) {
        return context.getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //做判定性处理
        if (mDiffTextSizeScale <= 0 || mDiffTextSizeScale > 1) {
            mDiffTextSizeScale = 1;
        }

        int length = mText.length();
        if (length < 2) {
            return;
        }

        float fontScale = getFontScale(getContext());

        int width = 0;//用来记录测量出来的字体宽度

        float textSize = this.mMaxTextSize + 1f;//加1，为了第一次的textSize为mMaxTextSize
        Rect rect = new Rect();//用来记录测量出来left、top、right、bottom
        Rect rect1 = new Rect();//用来记录测量出来left、top、right、bottom
        do {
            textSize = textSize - 1f;
            mPaint.setTextSize(textSize * fontScale);
            mPaint.getTextBounds(mText, 0, length - 2, rect);//计算大字体的边界
            mPaint.setTextSize(textSize * mDiffTextSizeScale * fontScale);
            mPaint.getTextBounds(mText, length - 2, length, rect1);//计算小字体的边界
            width = (rect.right - rect.left) + (rect1.right - rect1.left);//测量出来的宽度
        } while (width > mWidth);

        //经过上述步骤，就已经得到最适合当前控件宽度的textSize了
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        //因为传进来的是元，所以后两位一定是小数位
        if (length >= 2) {

            //小字体的宽度
            int w = rect1.right - rect1.left;

            mPaint.setTextSize(textSize * mDiffTextSizeScale * fontScale);
            //画出小数位并居中
            canvas.drawText(mText.substring(length - 2), mWidth, (mHeight - fontMetrics.top - fontMetrics.bottom) / 2, mPaint);
            //画出整数位并做好偏移
            mPaint.setTextSize(textSize * fontScale);
            canvas.drawText(mText.substring(0, length - 2), mWidth - w, (mHeight - fontMetrics.top - fontMetrics.bottom) / 2, mPaint);
        } else {
            canvas.drawText(mText, mWidth, (mHeight - fontMetrics.top - fontMetrics.bottom) / 2, mPaint);
        }
    }

    public void setText(String text) {
        this.mText = text;
        invalidate();
    }

    public void setTextSize(float textSize) {
        this.mMaxTextSize = textSize;
        invalidate();
    }

    public void setTextColor(int textColor) {
        mPaint.setColor(textColor);
        invalidate();
    }

    public void setDiffTextSizeScale(float diff) {
        this.mDiffTextSizeScale = diff;
        invalidate();
    }
}
