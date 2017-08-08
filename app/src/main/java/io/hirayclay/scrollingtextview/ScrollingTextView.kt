package io.hirayclay.scrollingtextview

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.view.VelocityTrackerCompat
import android.support.v4.widget.ScrollerCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import io.hirayclay.scrollingtextview.BuildConfig.DEBUG

/**
 * Created by CJJ on 2017/8/4.
 *@author CJJ
 */
class ScrollingTextView : View {

    val TAG = "$javaClass.name"


    //    var attArr = arrayOf(R.attr.text_color, R.attr.text_size, R.attr.default_line_space, R.attr.line_space)
    val mViewFlinger: ViewFlinger = ViewFlinger()

    var textList: List<String>? = null
    var mOffset = 0
        get() {
            return field
        }
        set(value) {
            field = value
            invalidate()
        }
    lateinit var textPaint: TextPaint
    var mHasDefaultLineSpace: Boolean = true
    var mTextLineSpace = 0
    var mTextColor = Color.LTGRAY
    var mTextLine = 3
    var mTextSize: Int = 18
    var initialIndex = 0
    var recycle = false

    var startBaseLine = 0
    var headerSpace = 0
    var itemHeight = 0
    var lastY = 0
    var pointerId: Int = 0
    lateinit var scroller: ScrollerCompat
    lateinit var mVelocityTracker: VelocityTracker


    constructor(context: Context?) : super(context)


    @SuppressLint("Recycle")
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val t = context.obtainStyledAttributes(attrs, R.styleable.ScrollingTextView)
        mTextColor = t.getColor(R.styleable.ScrollingTextView_text_color, Color.LTGRAY)
        mTextLine = t.getInt(R.styleable.ScrollingTextView_text_lines, 3)
        mTextSize = t.getDimensionPixelOffset(R.styleable.ScrollingTextView_text_size, 18)
        mTextLineSpace = t.getDimensionPixelOffset(R.styleable.ScrollingTextView_line_space, 0)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = mTextColor
        paint.textSize = mTextSize.toFloat()
        textPaint = TextPaint(paint)
        t.recycle()
        mVelocityTracker = VelocityTracker.obtain()
        scroller = ScrollerCompat.create(getContext())
    }


    fun bindText(textList: List<String>) {
        this.textList = textList
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val modeW = MeasureSpec.getMode(widthMeasureSpec)
        val modeH = MeasureSpec.getMode(heightMeasureSpec)

        if (modeH == AT_MOST && modeW == AT_MOST)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        }
        computeBaseInfo()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mVelocityTracker.addMovement(event)
//        val actionIndex = event.actionIndex
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (mViewFlinger.isFlinging())
                    mViewFlinger.stop()

                pointerId = event.getPointerId(0)
                lastY = event.y.toInt()
//                Log.i(TAG, "down ")
            }
            MotionEvent.ACTION_MOVE -> {
                mOffset += (event.y - lastY).toInt()
                lastY = event.y.toInt()
                invalidate()
//                Log.i(TAG, "move ")
            }
            MotionEvent.ACTION_UP -> {
//                Log.i(TAG, "up ")
                lastY = 0
                val vc = ViewConfiguration.get(context)
                val maxVelocity = vc.scaledMaximumFlingVelocity
                mVelocityTracker.computeCurrentVelocity(1000, maxVelocity.toFloat())
                val yVel = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId)
                if (yVel != 0f)
                    mViewFlinger.fling(yVel)
            }

        }
        return true
    }

    //模仿RV ViewFlinger，简化版

    inner class ViewFlinger : Runnable {


        private var mLastFlingY: Int = 0

        override fun run() {

            if (scroller.computeScrollOffset()) {
                val y = scroller.currY
                val dy = y - mLastFlingY
                mLastFlingY = y
                mOffset += dy
                invalidate()
                continueAnimation()
            } else stop()

        }

        fun fling(yVel: Float) {
            scroller.fling(0, 0, 0, yVel.toInt(), Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
            postOnAnimation(this@ViewFlinger)

        }

        fun continueAnimation() {
            postOnAnimation(this@ViewFlinger)
        }

        fun isFlinging(): Boolean {
            return !scroller.isFinished
        }

        public fun stop() {
            removeCallbacks(this@ViewFlinger)
            scroller.abortAnimation()
        }
    }

    private var baseH = 0f

    private fun computeBaseInfo() {
        val fm = textPaint.fontMetrics
        itemHeight = (fm.bottom - fm.top).toInt()
        baseH = -fm.top
        if (DEBUG)
            Log.i(TAG, "baseH:$baseH bottom:${fm.bottom}")
//        headerSpace = measuredHeight / 2 - itemHeight / 2
        //anchor the pivot
        startBaseLine = (measuredHeight / 2 - fm.top).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        drawText(canvas)
    }

    private fun drawText(canvas: Canvas) {
        if (textList == null || textList!!.isEmpty())
            return
        var remainingSpace = measuredHeight
        var curItem = 0
        //figure out which is the first visual item with the given scroll distance

        var start = baseH
        val o = mOffset.toFloat() / itemHeight % textList!!.size
        val n = o % 1
        if (o > 0) {
            start = baseH - (1 - n) * itemHeight
            curItem = (textList!!.size - trunc(o))
        } else if (o < 0) {
            start = baseH + n * itemHeight
            curItem = -trunc(o)
        } else {
            curItem = (o.toInt() + textList!!.size) % textList!!.size
            start = baseH
        }

        if (DEBUG)
            Log.i(TAG, "offset:$mOffset  itemHeight:$itemHeight  o:$o       n:$n      start:$start     curItem:$curItem")

//        val tail = (mOffset / itemHeight) % textList!!.size
//        if (tail < 0)
//            curItem = textList!!.size - 1
//        else curItem = textList!!.size - (1 + tail)


        val size = textList!!.size
        var index = 0
        var ih = itemHeight
//        if (mHasDefaultLineSpace) {
//            ih = (itemHeight * 1.2f).toInt()
//        } else if (mTextLineSpace != 0) ih = itemHeight + mTextLineSpace

        while (index < size) {
            val s = textList!![(curItem) % size]
            val textLen = textPaint.measureText(s)
            val y = start + (index * ih)
            canvas.drawText(s, width / 2 - textLen / 2, y, textPaint)
            remainingSpace = (remainingSpace - itemHeight + start).toInt()
            index++
            curItem++
        }
    }

    fun trunc(f: Float): Int {
        if (f < 0)
            return f.toInt()
        if (f > 0)
            return f.toInt()
        return 0
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mVelocityTracker.recycle()
    }

    fun reset(smooth: Boolean) {
        if (!smooth) {
            mOffset = 0
            invalidate()
        } else {
            val animator = ObjectAnimator.ofInt(this, "mOffset", mOffset, 0).setDuration(400)
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.start()
        }

    }

}