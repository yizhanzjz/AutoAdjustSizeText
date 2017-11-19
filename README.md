# AutoAdjustSizeText
　　　最近在项目中遇到一个这样的需求：一个显示金额的view，给定最大的字号，需根据view的宽高使金额字体显示地尽量大、务必完整，且小数位要比整数位小几个字号。<br/>
　　基本的分析思路是这样的：金额其实也就是文本，画文本Canvas有drawText方法可以使用；不同字号的字体，通过调用Paint的setTextSize方法传入不同的参数可以实现；而怎么才能使文本字体尽量大呢？<br/>
　　之前在学自定义控件的时候，记得有一个Paint的getTextBounds方法可以在文本画出之前计算出Text的边界数值(left,top,right,bottom)。<br/>
　　接下来是具体实现：
#### 1. 自定义属性
　　首先根据需求考虑需要哪些自定义属性呢？没必要，一次性就想全，一步一步做，想到什么有必要的就加上。其实也没几个，下面是，我想到的属性及其含义：

属性     | 含义
-------- | ---
maxTextSize | 需求中描述的“给定的最大字号”
diffTextSizeScale    | 小数位字号等于整数位字号*diffTextSizeScale，范围为(0,1]
textColor     | 字体颜色
text     | 字体内容
  
　　怎么自定义属性呢？<br/>
　　　　首先，在values目录下创建属性文件: ```attrs.xml```，然后在该文件中设置如下内容：
``` 
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="AutoAdjustView">
        <attr name="maxTextSize" format="float" />
        <attr name="diffTextSizeScale" format="float" />
        <attr name="textColor" format="color" />
        <attr name="text" format="string" />
    </declare-styleable>
</resources>
```
　　其次，就是在布局文件使用，注意，自定义属性使用的前缀为```app```：
``` 
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.example.yizhan.autoadjustsizetext.AutoAjustTextSizeView
        android:layout_width="150dp"
        android:layout_height="100dp"
        app:diffTextSizeScale="0.618"
        app:maxTextSize="60"
        app:text="1234567890.12"
        app:textColor="#ff454545" />

</RelativeLayout>
```
　　最后，就是在自定义控件的构造方法中去获取布局中这些属性设置的值
``` 
public AutoAjustTextSizeView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoAdjustView);
    mMaxTextSize = typedArray.getFloat(R.styleable.AutoAdjustView_maxTextSize, mDefualtMaxTextSize * getFontScale(context));
    mDiffTextSize = typedArray.getFloat(R.styleable.AutoAdjustView_diffTextSizeScale, 1);
    mTextColor = typedArray.getColor(R.styleable.AutoAdjustView_textColor, Color.BLACK);
    mText = typedArray.getText(R.styleable.AutoAdjustView_text).toString();

    init();
    }
```
#### 2. 设置Paint
　　canvas的drawText方法是要传入Paint参数的，所以，这里说一下Paint的设置。
```
private void init() {
    mPaint = new Paint();
    mPaint.setStyle(Paint.Style.FILL);//画笔模式为填充
    mPaint.setAntiAlias(true);//抗锯齿
    mPaint.setColor(mTextColor);//默认颜色
    mPaint.setTextAlign(Paint.Align.RIGHT);//默认为右侧
}

```
　　**mPaint.setStyle(Paint.Style.FILL)**，表示画笔画的是内容而非边界，如果画边界，需传入Paint.Style.STROKE;
　　**mPaint.setAntiAlias(true)**，表示抗锯齿
　　**mPaint.setColor(mTextColor)**，将上一步获取的颜色属性值设置给paint对象；
　　**mPaint.setTextAlign(Paint.Align.RIGHT)**，字面意思是设置text的对齐方式。Paint.Align有3种取值分别是：LEFT、CENTER、RIGHT，调用了这个方法主要影响的是mPaint.drawText(@NonNull String text, float x, float y, @NonNull Paint paint)方法中的x、y坐标的作用。

参数| 相对于字体看，（x,y）指代的位置
-------- | ---
LEFT | (x,y)表示基线与字体左边界的交点坐标
CENTER |(x,y)表示基线与字体水平中心线的交点坐标 
RIGHT |(x,y)表示基线与字体右边界的交点坐标 
　　至于什么是基线，请查阅getFontMetrics方法
#### 3. 获取当前控件的宽高
　　在这里，获取控件的宽高，主要作用是：宽度用来与计算出来的字体宽度进行比对从而决定当前的textSize是否合适（不大于控件宽度的最大textSize即为合适），宽度还可以协助指定上述x坐标，高度主要是用来是画出来的字体垂直居中的。
``` 
@Override
protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    mWidth = w;
    mHeight = h;
}
```
#### 4. 在onDraw方法里递归出最合适的textSize并垂直居中画出Text
　　1) 循环出控件宽度允许范围内的最大textSize。
```
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
```
　　　　主要的思路是，不断调整textSize的大小，使用getTextBounds计算出当前textSize下整数位字体和小数位字体的宽度，最终得到首次不大于控件宽度的textSize。
　　2) 先画出小数位，再画出整数位
　　　　上面在初始化Paint对象时```setTextAlign```传入的参数为```Paint.Align.RIGHT```表示相对于字体看，(x,y)坐标指的是字体大概右下角的位置（并不是右下角）
``` 
	//小字体的宽度
    int w = rect1.right - rect1.left;

    mPaint.setTextSize(textSize * mDiffTextSizeScale * fontScale);
    //画出小数位并居中
    canvas.drawText(mText.substring(length - 2), mWidth, (mHeight - fontMetrics.top - fontMetrics.bottom) / 2, mPaint);
    //画出整数位并做好偏移
    mPaint.setTextSize(textSize * fontScale);
    canvas.drawText(mText.substring(0, length - 2), mWidth - w, (mHeight - fontMetrics.top - fontMetrics.bottom) / 2, mPaint);
```
　　　　这里比较难理解的一点应该是，在指定(x,y)中的y坐标时使用的是```(mHeight - fontMetrics.top - fontMetrics.bottom) / 2```。如果想具体了解可查阅与getFontMetircs方法相关的资料，不想查可以记住，因为这种写法在使字体垂直居中时具有通用性。这个y值通俗来讲，通俗来讲，就是字体在控件中居中时基线的y坐标值。

#### 最终的效果
　　宽：150dp、高：100dp、diffTextSizeScale：0.618、maxTextSize：60：
![](https://github.com/yizhanzjz/ImageRepo/raw/master/auto_adjust_text_size_0.png)
　　宽：150dp、高：100dp、diffTextSizeScale：0.618、maxTextSize：20：
![](https://github.com/yizhanzjz/ImageRepo/raw/master/auto_adjust_text_size_1.png)
	
#### 总结
　　其实核心代码很简单的，第一次写博客，个人感觉写的有些啰嗦吧，有待提高。有问题的话，多多指教。
