package com.chenwd.bigimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2018/1/30.
 */

public class BigImageView extends View implements GestureDetector.OnGestureListener,View.OnTouchListener{

    private Rect mRect;
    BitmapFactory.Options mOptions;
    int mImageWidth;
    int mImageHeight;
    BitmapRegionDecoder mDecoder;
    private int mViewWidth;
    private int mViewHeight;
    private float mScale;
    private Bitmap mBitmap;
    private GestureDetector mGestureDetector;
    private Scroller mScroller;

    public BigImageView(Context context) {
        this(context,null,0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //指定要加载的矩形区域
        mRect =new Rect();
        //解码图片的配置
        mOptions = new BitmapFactory.Options();

        mGestureDetector = new GestureDetector(context, this);
        setOnTouchListener(this);
        mScroller = new Scroller(context);
    }

    /**
     * 由使用者输入一张图片 输入流
     * @param is
     */
    public void setImage(InputStream is){
        //读取原图片的宽高
        mOptions.inJustDecodeBounds=true;
        BitmapFactory.decodeStream(is,null,mOptions);
        mImageHeight=mOptions.outHeight;
        mImageWidth=mOptions.outWidth;
        //设置内存复用
        mOptions.inMutable=true;
        //设置像素格式为rgb565
        mOptions.inPreferredConfig= Bitmap.Config.RGB_565;
        mOptions.inJustDecodeBounds=false;

        try {
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获得测量的view的大小
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();

        if (null==mDecoder){
            return;
        }
//        确定要加载的图片的区域
        mRect.left=0;
        mRect.top=0;
        mRect.right=mImageWidth;
        //获得缩放因子
        mScale = mViewWidth/(float)mImageWidth;
        mRect.bottom= (int) (mViewHeight/mScale);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null== mDecoder){
            return;
        }
        //复用上一张bitmap
        mOptions.inBitmap=mBitmap;
        //解码指定区域
        mBitmap = mDecoder.decodeRegion(mRect,mOptions);

        //缩放
        Matrix matrix=new Matrix();
        matrix.setScale(mScale,mScale);
        canvas.drawBitmap(mBitmap,matrix,null);

    }

    @Override
    public boolean onDown(MotionEvent e) {
        //如果滑动还没有停止 强制停止
        if (mScroller.isFinished()){
            mScroller.forceFinished(true);
        }

        //继续接收后续事件
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    /**
     * 手指不离开屏幕拖动
     * @param e1 手指按下去的事件
     * @param e2 当前手势事件 获取当前的坐标
     * @param distanceX  x轴 方向移动的距离
     * @param distanceY y轴方向移动的距离
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //手指从下往上 图片也要往上  distanceY 是负数， top和bottom在减
        //改变加载图片的区域
        mRect.offset(0, (int) distanceY);
//        bottom大于图片高，或者top小于0了
        if(mRect.bottom>mImageHeight){
            mRect.bottom=mImageHeight;
            mRect.top=mImageHeight-(int) (mViewHeight/mScale);
        }
        if (mRect.top<0){
            mRect.top=0;
            mRect.bottom=(int) (mViewHeight/mScale);
        }
        invalidate();
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    /**
     * 手指离开屏幕 滑动 惯性
     * @param e1
     * @param e2
     * @param velocityX  速度 每秒x方向 移动的像素
     * @param velocityY
     * @return
     */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        /**
         * Start scrolling based on a fling gesture. The distance travelled will
         * depend on the initial velocity of the fling.
         *
         * @param startX Starting point of the scroll (X)  滑动开始的x坐标
         * @param startY Starting point of the scroll (Y)
         * @param velocityX Initial velocity of the fling (X) measured in pixels per  两个速度
         *        second.
         * @param velocityY Initial velocity of the fling (Y) measured in pixels per
         *        second
         * @param minX Minimum X value. The scroller will not scroll past this  x方向的最小值
         *        point.
         * @param maxX Maximum X value. The scroller will not scroll past this x方向的最大值
         *        point.
         * @param minY Minimum Y value. The scroller will not scroll past this
         *        point.
         * @param maxY Maximum Y value. The scroller will not scroll past this
         *        point.
         */
        mScroller.fling(0,
                        mRect.top,
                0,
                        (int)-velocityY,
                0,
                0,
                0,
                mImageHeight-(int)(mViewHeight/mScale)
                        );
        return false;
    }

    //获取计算结果并且重绘
    @Override
    public void computeScroll() {
        if (mScroller.isFinished())
        {
            return;
    }

    //true 表示动画未结束
    if (mScroller.computeScrollOffset()){
            mRect.top=mScroller.getCurrY();
            mRect.bottom=mRect.top+(int)(mViewHeight/mScale);
            invalidate();
    }

        super.computeScroll();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //给手势片理
        return mGestureDetector.onTouchEvent(event);
    }
}
