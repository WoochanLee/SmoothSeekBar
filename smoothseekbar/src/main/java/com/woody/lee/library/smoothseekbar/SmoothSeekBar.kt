package com.woody.lee.library.smoothseekbar


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class SmoothSeekBar : View {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initAttrs(context, attrs)
    }

    //Initial Attributes
    private var barHeight = 0f
    private var barColor = 0
    private var barEdgeRadius = 0f
    private var barHorizontalPadding = 0f
    private var labelContents = listOf<String>()
    private var labelTextSize = 0f
    private var labelTextColor = 0
    private var labelTopMargin = 0f
    private var thumbDrawable: Drawable? = null
    private var thumbWidth = 0f
    private var thumbHeight = 0f
    private var thumbHorizontalPadding = 0f
    private var max = 0
    private var progressColor = 0
    private var animationSpeed = 0L

    //Drawing Component
    private val seekBarPaint = Paint()
    private val labelPaint = Paint()
    private val canvasBounds = Rect()
    private val progressBarBounds = Rect()
    private val backgroundBarRect = RectF()
    private val progressBarRect = RectF()
    private lateinit var labelRects: List<Rect>

    //Dynamic Values
    private var seekBarStatus = SeekBarStatus.NONE
    private var currentThumbX = 0f
    private var currentProgress: Int by Delegates.observable(0) { _, _, newValue ->
        onProgressChanged(newValue)
    }

    var onProgressChanged: (Int) -> Unit = {}

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SmoothSeekBar,
            0,
            0
        ).apply {
            try {
                barHeight = getDimension(R.styleable.SmoothSeekBar_barHeight, 0f)
                barColor = getColor(R.styleable.SmoothSeekBar_barColor, 0)
                barEdgeRadius = getDimension(R.styleable.SmoothSeekBar_barEdgeRadius, 0f)
                barHorizontalPadding = getDimension(R.styleable.SmoothSeekBar_barHorizontalPadding, 0f)
                labelContents = getString(R.styleable.SmoothSeekBar_labelContents)?.split(DELIMITERS_OF_LABEL_CONTENTS) ?: emptyList()
                labelTextSize = getDimension(R.styleable.SmoothSeekBar_labelTextSize, 0f)
                labelTextColor = getColor(R.styleable.SmoothSeekBar_labelTextColor, 0)
                labelTopMargin = getDimension(R.styleable.SmoothSeekBar_labelTopMargin, 0f)
                thumbDrawable = ContextCompat.getDrawable(context, getResourceId(R.styleable.SmoothSeekBar_thumbSrc, 0))
                thumbWidth = getDimension(R.styleable.SmoothSeekBar_thumbWidth, 0f)
                thumbHeight = getDimension(R.styleable.SmoothSeekBar_thumbHeight, 0f)
                thumbHorizontalPadding = getDimension(R.styleable.SmoothSeekBar_thumbHorizontalPadding, 0f)
                max = getFloat(R.styleable.SmoothSeekBar_max, 0f).toInt()
                currentProgress = getFloat(R.styleable.SmoothSeekBar_progress, 0f).toInt()
                progressColor = getInt(R.styleable.SmoothSeekBar_progressColor, 0)
                animationSpeed = getInt(R.styleable.SmoothSeekBar_animationSpeed, 0).toLong()

                labelRects = List(labelContents.size) { Rect() }
            } finally {
                recycle()
            }
        }
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.getClipBounds(canvasBounds)
        createProgressBarBounds(canvas, progressBarBounds)
        initThumbX(canvas)
        drawBackgroundBar(canvas)
        drawProgressBar(canvas)
        drawThumb(canvas)
        drawLabels(canvas)
    }

    private fun createProgressBarBounds(canvas: Canvas, progressBarBounds: Rect) {
        canvas.getClipBounds(progressBarBounds)
        progressBarBounds.left += barHorizontalPadding.toInt()
        progressBarBounds.right -= barHorizontalPadding.toInt()
    }

    private fun initThumbX(canvas: Canvas) {
        if (seekBarStatus == SeekBarStatus.NONE) {
            val sectionWidth = progressBarBounds.width() / max

            currentThumbX = when (currentProgress) {
                0 -> thumbHorizontalPadding
                max -> canvas.width - thumbHorizontalPadding
                else -> progressBarBounds.left.toFloat() + (sectionWidth * currentProgress)
            }
        }
    }

    private fun drawBackgroundBar(canvas: Canvas) {
        val top = progressBarBounds.centerY() - (barHeight / 2).toInt()
        val bottom = progressBarBounds.centerY() + (barHeight / 2).toInt()
        backgroundBarRect.set(progressBarBounds.left.toFloat(), top.toFloat(), progressBarBounds.right.toFloat(), bottom.toFloat())

        seekBarPaint.color = barColor
        canvas.drawRoundRect(backgroundBarRect, barEdgeRadius, barEdgeRadius, seekBarPaint)
    }

    private fun drawProgressBar(canvas: Canvas) {
        val top = progressBarBounds.centerY() - (barHeight / 2).toInt()
        val bottom = progressBarBounds.centerY() + (barHeight / 2).toInt()
        val right = currentThumbX.toInt()
        progressBarRect.set(progressBarBounds.left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

        seekBarPaint.color = progressColor
        canvas.drawRoundRect(progressBarRect, barEdgeRadius, barEdgeRadius, seekBarPaint)
    }

    private fun drawThumb(canvas: Canvas) {
        thumbDrawable?.setBounds(
            thumbLeft(thumbWidth).toInt(),
            thumbTop(thumbHeight).toInt(),
            thumbRight(thumbWidth).toInt(),
            thumbBottom(thumbHeight).toInt()
        )
        thumbDrawable?.draw(canvas)
    }

    private fun drawLabels(canvas: Canvas) {
        labelContents.forEachIndexed { index, label ->
            drawLabel(canvas, labelRects[index], index, labelContents.size, label)
        }
    }

    private fun drawLabel(canvas: Canvas, labelRect: Rect, index: Int, size: Int, content: String) {
        labelPaint.isAntiAlias = true
        labelPaint.textSize = labelTextSize
        labelPaint.color = labelTextColor
        labelPaint.getTextBounds(content, 0, content.length, labelRect)

        val sectionWidth = progressBarBounds.width() / (size - 1)

        val point = when (index) {
            0 -> PointF(progressBarBounds.left.toFloat(), labelTopMargin + labelRect.height())
            size - 1 -> PointF((progressBarBounds.right - labelRect.width()).toFloat(), labelTopMargin + labelRect.height())
            else -> PointF(progressBarBounds.left.toFloat() + (sectionWidth * index - labelRect.width() / 2), labelTopMargin + labelRect.height())
        }

        canvas.drawText(content, point.x, point.y, labelPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> startDragging(event)
            MotionEvent.ACTION_MOVE -> dragThumb(event)
            MotionEvent.ACTION_UP -> finishDragging(event)
            MotionEvent.ACTION_CANCEL -> finishDragging(event)
        }
        return true
    }

    private fun startDragging(event: MotionEvent) {
        if (isThumbTouched(event, thumbWidth, thumbHeight)) {
            seekBarStatus = SeekBarStatus.DRAGGING
        }
    }

    private fun dragThumb(event: MotionEvent) {
        if (seekBarStatus != SeekBarStatus.DRAGGING) {
            return
        }
        currentThumbX = when {
            (event.x < thumbHorizontalPadding) -> thumbHorizontalPadding
            (event.x > canvasBounds.right - thumbHorizontalPadding) -> canvasBounds.right - thumbHorizontalPadding
            else -> event.x
        }
        checkDraggingAreas(currentThumbX)
        invalidate()
    }

    private fun finishDragging(event: MotionEvent) {
        dragThumb(event)

        if (seekBarStatus == SeekBarStatus.DRAGGING) {
            checkDraggingAreas(event.x)
            seekBarStatus = SeekBarStatus.ANIMATING
            animateThumb()
        } else {
            if (isTouchArea(event)) {
                seekBarStatus = SeekBarStatus.ANIMATING
                animateThumb()
            }
        }
    }

    private fun checkDraggingAreas(x: Float) {
        val sectionWidth = (canvasBounds.width() - thumbHorizontalPadding * 2) / max

        (0..max).forEach { index ->
            if (x < thumbHorizontalPadding + sectionWidth * index + sectionWidth / 2) {
                currentProgress = index
                return
            } else {
                max

            }
        }
    }

    private fun isTouchArea(event: MotionEvent): Boolean {
        val sectionWidth = progressBarBounds.width() / max

        (0..max).forEach { index ->
            if (event.x > barHorizontalPadding + sectionWidth * index - TOUCH_AREA_WIDTH.toPx() / 2
                && event.x < barHorizontalPadding + sectionWidth * index + TOUCH_AREA_WIDTH.toPx() / 2
                && event.y > progressBarBounds.centerY() - TOUCH_AREA_WIDTH.toPx() / 2
                && event.y < progressBarBounds.centerY() + TOUCH_AREA_WIDTH.toPx() / 2
            ) {
                currentProgress = index
                return true
            }
        }
        return false
    }

    private fun animateThumb() {
        val sectionWidth = progressBarBounds.width() / max

        findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            while (seekBarStatus == SeekBarStatus.ANIMATING) {
                val targetX = when (currentProgress) {
                    0 -> thumbHorizontalPadding
                    max -> canvasBounds.width() - thumbHorizontalPadding
                    else -> progressBarBounds.left.toFloat() + (sectionWidth * currentProgress)
                }
                when {
                    (targetX < currentThumbX - animationSpeed) -> currentThumbX -= animationSpeed
                    (targetX > currentThumbX + animationSpeed) -> currentThumbX += animationSpeed
                    else -> {
                        seekBarStatus = SeekBarStatus.NONE
                        break
                    }
                }
                postInvalidate()
                delay(DELAY_ANIMATION)
            }
        }
    }

    private fun thumbLeft(drawableWidth: Float) = currentThumbX - drawableWidth / 2f
    private fun thumbTop(drawableHeight: Float) = progressBarBounds.centerY() - drawableHeight / 2f
    private fun thumbRight(drawableWidth: Float) = currentThumbX + drawableWidth / 2f
    private fun thumbBottom(drawableHeight: Float) = progressBarBounds.centerY() + drawableHeight / 2f

    private fun isThumbTouched(event: MotionEvent, thumbWidth: Float, thumbHeight: Float): Boolean {
        if (thumbDrawable == null) {
            return false
        }

        return event.x >= thumbLeft(thumbWidth)
                && event.x <= thumbRight(thumbWidth)
                && event.y >= thumbTop(thumbHeight)
                && event.y <= thumbBottom(thumbHeight)
    }

    private fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private enum class SeekBarStatus {
        ANIMATING,
        DRAGGING,
        NONE
    }

    companion object {
        private const val DELIMITERS_OF_LABEL_CONTENTS = "|"
        private const val DELAY_ANIMATION = 10L
        private const val TOUCH_AREA_WIDTH = 24
    }
}
