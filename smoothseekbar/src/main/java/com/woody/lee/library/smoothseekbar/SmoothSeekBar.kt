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
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Float.max
import kotlin.properties.Delegates

class SmoothSeekBar : View {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
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
    private val seekBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val canvasBounds = Rect()
    private val progressBarBounds = Rect()
    private val backgroundBarRect = RectF()
    private val progressBarRect = RectF()
    private val labelRect = Rect()

    //Dynamic Values
    private var seekBarStatus = SeekBarStatus.NONE
    private var currentThumbX = 0f
    private var currentProgress: Int by Delegates.observable(0) { _, _, newValue ->
        onProgressChanged(newValue)
    }
    private var animationJob: Job? = null

    var onProgressChanged: (Int) -> Unit = {}

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SmoothSeekBar,
            0,
            0
        ).apply {
            try {
                barHeight = getDimension(R.styleable.SmoothSeekBar_barHeight, DEFAULT_BAR_HEIGHT.toPx())
                barColor = getColor(R.styleable.SmoothSeekBar_barColor, DEFAULT_BAR_COLOR)
                barEdgeRadius = getDimension(R.styleable.SmoothSeekBar_barEdgeRadius, DEFAULT_BAR_EDGE_RADIUS.toPx())
                barHorizontalPadding = getDimension(R.styleable.SmoothSeekBar_barHorizontalPadding, DEFAULT_BAR_HORIZONTAL_PADDING.toPx())
                labelContents = getString(R.styleable.SmoothSeekBar_labelContents)?.split(DELIMITERS_OF_LABEL_CONTENTS) ?: emptyList()
                labelTextSize = getDimension(R.styleable.SmoothSeekBar_labelTextSize, DEFAULT_LABEL_TEXT_SIZE.toPx())
                labelTextColor = getColor(R.styleable.SmoothSeekBar_labelTextColor, DEFAULT_LABEL_TEXT_COLOR)
                labelTopMargin = getDimension(R.styleable.SmoothSeekBar_labelTopMargin, DEFAULT_LABEL_TOP_MARGIN.toPx())
                thumbDrawable = ContextCompat.getDrawable(context, getResourceId(R.styleable.SmoothSeekBar_thumbSrc, DEFAULT_THUMB_SRC))
                thumbWidth = getDimension(R.styleable.SmoothSeekBar_thumbWidth, DEFAULT_THUMB_WIDTH.toPx())
                thumbHeight = getDimension(R.styleable.SmoothSeekBar_thumbHeight, DEFAULT_THUMB_HEIGHT.toPx())
                thumbHorizontalPadding = getDimension(R.styleable.SmoothSeekBar_thumbHorizontalPadding, DEFAULT_THUMB_HORIZONTAL_PADDING.toPx())
                max = getInt(R.styleable.SmoothSeekBar_max, DEFAULT_MAX)
                currentProgress = getInt(R.styleable.SmoothSeekBar_progress, DEFAULT_PROGRESS)
                progressColor = getInt(R.styleable.SmoothSeekBar_progressColor, DEFAULT_PROGRESS_COLOR)
                animationSpeed = getInt(R.styleable.SmoothSeekBar_animationSpeed, DEFAULT_ANIMATION_SPEED).toLong()
            } finally {
                recycle()
            }
        }
    }

    fun setProgress(progress: Int, withAnimation: Boolean = true) {
        currentProgress = progress
        if (withAnimation) {
            animateThumb()
        } else {
            seekBarStatus = SeekBarStatus.NONE
            setThumbX(progress)
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val viewWidthMode = MeasureSpec.getMode(widthMeasureSpec)
        val viewHeightMode = MeasureSpec.getMode(heightMeasureSpec)

        val viewWidthSize = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeightSize = MeasureSpec.getSize(heightMeasureSpec)

        val labelBottom = labelTopMargin + labelTextSize

        if (viewWidthMode == MeasureSpec.EXACTLY && viewHeightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(viewWidthSize, viewHeightSize)
        } else {
            setMeasuredDimension(viewWidthSize, (max(labelBottom, max(thumbHeight, barHeight)) + DEFAULT_BOTTOM_PADDING.toPx()).toInt())
        }
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.getClipBounds(canvasBounds)
        canvas.getProgressBarBounds(progressBarBounds)
        initThumbX(currentProgress)
        drawBackgroundBar(canvas)
        drawProgressBar(canvas)
        drawThumb(canvas)
        drawLabels(canvas)
    }

    private fun Canvas.getProgressBarBounds(progressBarBounds: Rect) {
        getClipBounds(progressBarBounds)
        progressBarBounds.left += barHorizontalPadding.toInt()
        progressBarBounds.right -= barHorizontalPadding.toInt()
    }

    private fun initThumbX(progress: Int) {
        if (seekBarStatus != SeekBarStatus.NONE) {
            return
        }

        setThumbX(progress)
    }

    private fun setThumbX(progress: Int) {
        val sectionWidth = progressBarBounds.width() / max.toFloat()

        currentThumbX = when (progress) {
            0 -> thumbHorizontalPadding
            max -> canvasBounds.width() - thumbHorizontalPadding
            else -> progressBarBounds.left.toFloat() + (sectionWidth * currentProgress)
        }
    }

    private fun drawBackgroundBar(canvas: Canvas) {
        val top = progressBarBounds.centerY() - (barHeight / 2).toInt()
        val bottom = progressBarBounds.centerY() + (barHeight / 2).toInt()
        backgroundBarRect.set(
            progressBarBounds.left.toFloat(),
            top.toFloat(),
            progressBarBounds.right.toFloat(),
            bottom.toFloat()
        )

        seekBarPaint.color = barColor
        canvas.drawRoundRect(backgroundBarRect, barEdgeRadius, barEdgeRadius, seekBarPaint)
    }

    private fun drawProgressBar(canvas: Canvas) {
        val top = progressBarBounds.centerY() - (barHeight / 2).toInt()
        val bottom = progressBarBounds.centerY() + (barHeight / 2).toInt()
        val right = currentThumbX.toInt()
        progressBarRect.set(
            progressBarBounds.left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat()
        )

        seekBarPaint.color = progressColor
        canvas.drawRoundRect(progressBarRect, barEdgeRadius, barEdgeRadius, seekBarPaint)
    }

    private fun drawThumb(canvas: Canvas) {
        val rect = Rect(
            thumbLeft(thumbWidth).toInt(),
            thumbTop(thumbHeight).toInt(),
            thumbRight(thumbWidth).toInt(),
            thumbBottom(thumbHeight).toInt()
        )
        thumbDrawable?.let { thumbDrawable ->
            thumbDrawable.draw(canvas)
            canvas.drawBitmap(thumbDrawable.toBitmap(), null, rect, thumbPaint)
        }
    }

    private fun drawLabels(canvas: Canvas) {
        labelPaint.isAntiAlias = true
        labelPaint.textSize = labelTextSize
        labelPaint.color = labelTextColor

        labelContents.forEachIndexed { index, label ->
            drawLabel(canvas, index, labelContents.size - 1, label)
        }
    }

    private fun drawLabel(canvas: Canvas, index: Int, sectionCount: Int, content: String) {
        labelPaint.getTextBounds(content, 0, content.length, labelRect)

        val sectionWidth = progressBarBounds.width() / if (sectionCount == 0) 1 else sectionCount

        val point = when (index) {
            0 -> PointF(progressBarBounds.left.toFloat(), labelTopMargin + labelTextSize)
            sectionCount -> PointF(
                (progressBarBounds.right - labelRect.width()).toFloat(),
                labelTopMargin + labelTextSize
            )
            else -> PointF(
                progressBarBounds.left.toFloat() + (sectionWidth * index - labelRect.width() / 2),
                labelTopMargin + labelTextSize
            )
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
        postInvalidate()
    }

    private fun finishDragging(event: MotionEvent) {
        dragThumb(event)

        if (seekBarStatus == SeekBarStatus.DRAGGING) {
            checkDraggingAreas(event.x)
            animateThumb()
        } else {
            if (isTouchArea(event)) {
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
        seekBarStatus = SeekBarStatus.ANIMATING
        val sectionWidth = progressBarBounds.width() / max

        animationJob?.cancel()
        animationJob = findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
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
    private fun thumbBottom(drawableHeight: Float) =
        progressBarBounds.centerY() + drawableHeight / 2f

    private fun isThumbTouched(event: MotionEvent, thumbWidth: Float, thumbHeight: Float): Boolean {
        if (thumbDrawable == null) {
            return false
        }

        return event.x >= thumbLeft(thumbWidth)
                && event.x <= thumbRight(thumbWidth)
                && event.y >= thumbTop(thumbHeight)
                && event.y <= thumbBottom(thumbHeight)
    }

    private fun Int.toPx(): Float = (this * Resources.getSystem().displayMetrics.density)

    private enum class SeekBarStatus {
        ANIMATING,
        DRAGGING,
        NONE
    }

    companion object {
        private const val DELIMITERS_OF_LABEL_CONTENTS = "|"
        private const val DELAY_ANIMATION = 10L
        private const val TOUCH_AREA_WIDTH = 24

        private const val DEFAULT_BAR_COLOR = Color.GRAY
        private const val DEFAULT_BAR_EDGE_RADIUS = 0
        private const val DEFAULT_BAR_HEIGHT = 13
        private const val DEFAULT_BAR_HORIZONTAL_PADDING = 15
        private const val DEFAULT_LABEL_TEXT_COLOR = Color.BLACK
        private const val DEFAULT_LABEL_TEXT_SIZE = 12
        private const val DEFAULT_LABEL_TOP_MARGIN = 0
        private const val DEFAULT_MAX = 100
        private const val DEFAULT_PROGRESS = 0
        private const val DEFAULT_PROGRESS_COLOR = Color.BLACK
        private const val DEFAULT_THUMB_HEIGHT = 30
        private const val DEFAULT_THUMB_WIDTH = 30
        private const val DEFAULT_THUMB_HORIZONTAL_PADDING = 18
        private const val DEFAULT_THUMB_SRC = android.R.drawable.star_big_on
        private const val DEFAULT_ANIMATION_SPEED = 10
        private const val DEFAULT_BOTTOM_PADDING = 10
    }
}
