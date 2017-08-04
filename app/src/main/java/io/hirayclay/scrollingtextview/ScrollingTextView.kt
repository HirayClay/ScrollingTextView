package io.hirayclay.scrollingtextview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec.AT_MOST

/**
 * Created by CJJ on 2017/8/4.
 *@author CJJ
 */
class ScrollingTextView : View {


    var textList: List<String>? = null
    var mOffset = 0
    lateinit var textPaint: TextPaint
    var mTextColor = Color.LTGRAY
    var mTextLine = 3
    var mTextSize: Int = 18
    var initialIndex = 0
    var recycle = false
    var startBaseLine = 0
    var headerSpace = 0
    var itemHeight = 0

    var lastY = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val t = context.obtainStyledAttributes(attrs, R.styleable.ScrollingTextView)
        mTextColor = t.getColor(R.styleable.ScrollingTextView_text_color, Color.LTGRAY)
        mTextLine = t.getInt(R.styleable.ScrollingTextView_text_lines, 3)
        mTextSize = t.getDimensionPixelOffset(R.styleable.ScrollingTextView_text_size, 18)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = mTextColor!!
        paint.textSize = mTextSize.toFloat()
        textPaint = TextPaint(paint)
        t.recycle()
        touch()
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

    fun touch() {
        setOnTouchListener { _, event ->

            Log.i("ScrollingTextView", event.action.toString())
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    lastY = event.y.toInt()
                MotionEvent.ACTION_MOVE -> {
                    mOffset -= (event.y - lastY).toInt()
                    lastY = event.y.toInt()
                    invalidate()
                }
                MotionEvent.ACTION_UP -> lastY = 0

            }

            return@setOnTouchListener true
        }
    }

    private fun computeBaseInfo() {
        val fm = textPaint.fontMetrics
        itemHeight = (fm.bottom - fm.top).toInt()
        headerSpace = measuredHeight / 2 - itemHeight / 2
        startBaseLine = (measuredHeight / 2 - itemHeight / 2 - fm.top).toInt()
    }

    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
        if (textList == null || textList!!.isEmpty())
            return
        var remainingSpace = measuredHeight
        var curItem = 0
        if (mOffset < headerSpace)
            curItem = 0
        else {
            val N = (mOffset - headerSpace) / itemHeight
            curItem = N + 1
        }

        //循环获取最近的元素

        var size = textList!!.size
        var index = 0
        while (remainingSpace > 0 && index < size) {
            var s = textList!![(index + curItem) % size]
            var textLen = textPaint.measureText(s)
            canvas.drawText(s, width / 2 - textLen / 2, (startBaseLine + (index * itemHeight) % measuredHeight).toFloat(), textPaint)
            remainingSpace -= itemHeight
            index++

        }

    }
}