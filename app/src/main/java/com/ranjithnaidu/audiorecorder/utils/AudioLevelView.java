package com.ranjithnaidu.audiorecorder.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AudioLevelView extends View {
    private static final String TAG = "AUDIO_RECORDER";
    private static final int LINE_WIDTH_DP = 1;
    private static final int TIME_INTERVAL_WIDTH_DP = 25;
    private static final int LINES_SPACE = 2; // space between 2 consecutive lines
    private static final float UPDATE_FREQ = 0.1f;
    private static final int MAX_AMPLITUDE = 32767;
    private static final int MIN_AMPLITUDE = 1500;
    private static final int TOP_PADDING = 12;

    private final Context context;
    private List<Float> amplitudes = new ArrayList<>(); // amplitudes for line lengths
    private boolean isRecording;
    private int startTime, start, secsPerMark, longMarksPerScreen;
    private int width, height;
    private float midWidth, startHeight, centralHeight, lineWidth, timeIntervalWidth, startGrid, timeScaleWidth;
    private Rect boxRect;
    private Paint linePaint, lineRedPaint, boxPaint, shortMarkPaint, longMarkPaint, horLinePaint;
    private Path horLinePath;
    private Paint numPaint;

    // constructor
    public AudioLevelView(Context context) {
        super(context); // call superclass constructor
        this.context = context;
    }

    // constructor
    public AudioLevelView(Context context, AttributeSet attrs) {
        super(context, attrs); // call superclass constructor
        this.context = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        setup();
    }

    // Initial setup of the view.
    private void setup() {
        float scale = getResources().getDisplayMetrics().density;

        // Lines of amplitudes.
        lineWidth = convertDpToPixel(LINE_WIDTH_DP);
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#2DA9E0"));
        linePaint.setStrokeWidth(lineWidth);

        // Red line indicating the current position.
        lineRedPaint = new Paint();
        lineRedPaint.setColor(Color.parseColor("#E00707"));
        lineRedPaint.setStrokeWidth(lineWidth);

        // Surrounding box.
        boxPaint = new Paint();
        boxPaint.setColor(Color.parseColor("#F8F8F8"));
        boxPaint.setStyle(Paint.Style.FILL);

        // Grid.
        startGrid = 0; // the grid is drawn starting from x = 0
        timeIntervalWidth = convertDpToPixel(TIME_INTERVAL_WIDTH_DP);

        // Short time marks.
        shortMarkPaint = new Paint();
        shortMarkPaint.setColor(Color.parseColor("#999999"));
        shortMarkPaint.setStrokeWidth(lineWidth);

        // Long time marks.
        longMarkPaint = new Paint();
        longMarkPaint.setColor(Color.parseColor("#666666"));
        longMarkPaint.setStrokeWidth(lineWidth);

        // Horizontal dotted line at the center of the view.
        horLinePath = new Path();
        horLinePaint = new Paint();
        horLinePaint.setColor(Color.parseColor("#2DA9E0"));
        horLinePaint.setStrokeWidth(lineWidth);
        horLinePaint.setStyle(Paint.Style.STROKE);
        horLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

        // Numbers of the grid.
        secsPerMark = (int) (((TIME_INTERVAL_WIDTH_DP * 4) / (LINE_WIDTH_DP * LINES_SPACE)) * UPDATE_FREQ); // seconds for each mark in the time scale (small marks)
        numPaint = new Paint();
        numPaint.setTextSize(8 * scale);
        numPaint.setColor(Color.parseColor("#2DA9E0"));
        numPaint.setAntiAlias(true);
        numPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        amplitudes = new ArrayList<>((int) (width / lineWidth * LINES_SPACE));

        width = w;
        height = h;
        startHeight = convertDpToPixel(TOP_PADDING); // padding at the top to show times
        midWidth = width / 2;
        centralHeight = height * 0.6f; // amplitudes are centered at this height
        longMarksPerScreen = (int) (width / (timeIntervalWidth * 4));
        timeScaleWidth = timeIntervalWidth * 4 * longMarksPerScreen * 2; // we draw a time scale 2 times the width of the screen and then move it to the left as times goes by
        boxRect = new Rect(0, (int) startHeight, width, height);
    }

    // Start and stop recording.
    public void startRecording(int secondsElapsed) {
        setup();
        amplitudes.clear();
        startTime = secondsElapsed;
        start = startTime;
        isRecording = true;
        invalidate();
    }

    public void stopRecording() {
        setup();
        amplitudes.clear();
        startTime = 0;
        start = startTime;
        isRecording = false;
        invalidate();
    }

    // Add the given amplitude to the amplitudes ArrayList.
    public void addAmplitude(float amplitude) {
        amplitude = Math.min((float) (amplitude * 1.2), MAX_AMPLITUDE); // increase sensitivity
        amplitude = amplitude < MIN_AMPLITUDE ? MIN_AMPLITUDE : amplitude + MIN_AMPLITUDE;

        amplitudes.add(amplitude);
        updateAmplitudesAndGrid();
        invalidate();
    }

    private void updateAmplitudesAndGrid() {
        if (amplitudes.size() * (lineWidth * LINES_SPACE) >= midWidth) { // red lines reaches half view
            amplitudes.remove(0); // remove oldest amplitude
            // Start moving the grid left by 1 line.
            startGrid -= lineWidth * LINES_SPACE;
            if (startGrid <= -timeScaleWidth) {
                startGrid = 0;
                start += longMarksPerScreen * 2 * secsPerMark;
            }
        }
    }

    // Draw the visualizer with scaled lines representing the amplitudes.
    @Override
    public void onDraw(Canvas canvas) {
        drawBox(canvas);

        if (isRecording)
            drawAmplitudes(canvas);
    }

    // Draw the the box.
    private void drawBox(Canvas canvas) {
        // Draw the surrounding box.
        canvas.drawRect(boxRect, boxPaint);

        // Draw the time scale.
        int count = 0;
        int time;
        for (float x = startGrid; x < timeScaleWidth; x += timeIntervalWidth, count++) {
            if (count % 5 == 0) { // long mark
                canvas.drawLine(x, startHeight, x, height / 5, shortMarkPaint);
                time = start + (count / 5) * secsPerMark;
                if (count > 0 || start > startTime)
                    canvas.drawText(formatDuration(time), x, startHeight - convertDpToPixel(4), numPaint);
            } else { // short mark
                canvas.drawLine(x, startHeight, x, height / 7, longMarkPaint);
            }
        }

        // Draw dashed line at the middle.
        horLinePath.moveTo(0, centralHeight);
        horLinePath.quadTo(width / 2, centralHeight, width, centralHeight);
        canvas.drawPath(horLinePath, horLinePaint);
    }

    private void drawAmplitudes(Canvas canvas) {
        // Draw the amplitudes.
        float curX = 0; // the x position where to draw the lines
        for (float amplitude : amplitudes) {
            float scaledHeight = (float) ((amplitude / MAX_AMPLITUDE) * centralHeight * 0.55);
            curX += lineWidth * LINES_SPACE;
            canvas.drawLine(curX, centralHeight + scaledHeight, curX, centralHeight
                    - scaledHeight, linePaint);
        }

        // Draw the red line at the end marking the current time.
        if (amplitudes.size() > 0) {
            curX += lineWidth * LINES_SPACE;
            canvas.drawLine(curX, startHeight, curX, height, lineRedPaint);
        }
    }

    // Utility functions.
    private int convertDpToPixel(float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // Format duration (hh:mm:ss).
    private static String formatDuration(long duration) {
        String hms;
        if (TimeUnit.SECONDS.toHours(duration) > 0)
            hms = String.format(Locale.getDefault(), "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(duration),
                    TimeUnit.SECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.SECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1));
        else
            hms = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.SECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.SECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1));

        return hms;
    }
}
