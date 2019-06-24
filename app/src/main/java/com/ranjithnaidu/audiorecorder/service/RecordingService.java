package com.ranjithnaidu.audiorecorder.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.ranjithnaidu.audiorecorder.utils.Utils;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;

/**
 * Service used to record audio. This class implements an hybrid Service (bound and started
 * Service).
 * Compared with the original Service, this class adds a new feature to
 * bound and connect Service to an Activity
 */

public class RecordingService extends Service {
    private static final String TAG = "AUDIO_RECORDER_TAG";
    private final String CLASS_NAME = getClass().getSimpleName();
    private static final String EXTRA_ACTIVITY_STARTER = "com.ranjithnaidu.audiorecorder.EXTRA_ACTIVITY_STARTER";

    private String mFileName = null;
    private String mFilePath = null;
    private MediaRecorder mRecorder = null;
    private long mStartingTimeMillis = 0;
    private long mElapsedMillis = 0;

    private TimerTask mIncrementTimerTask = null;

    private final IBinder myBinder = new LocalBinder();
    private boolean isRecording = false;

    public static int onCreateCalls = 0;
    public static int onDestroyCalls = 0;
    public static int onStartCommandCalls = 0;

    public static Intent makeIntent(Context context, boolean activityStarter) {
        Intent intent = new Intent(context.getApplicationContext(), RecordingService.class);
        intent.putExtra(EXTRA_ACTIVITY_STARTER, activityStarter);
        return intent;
    }

    /**
     * The following code implements a bound Service used to connect this Service to an Activity.
     */
    public class LocalBinder extends Binder {
        public RecordingService getService() {
            return RecordingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    /**
     * Interface used to communicate to a connected component changes in the status of a
     * recording:
     * - recording started
     * - recording stopped (with file path)
     * - seconds elapsed and max amplitude (useful for graphical effects)
     */
    public interface OnRecordingStatusChangedListener {
        void onRecordingStarted();

        void onTimerChanged(int seconds);

        void onAmplitudeInfo(int amplitude);

        void onRecordingStopped(String filePath, Long elapsedMillis);
    }

    private OnRecordingStatusChangedListener onRecordingStatusChangedListener = null;

    public void setOnRecordingStatusChangedListener(OnRecordingStatusChangedListener onRecordingStatusChangedListener) {
        this.onRecordingStatusChangedListener = onRecordingStatusChangedListener;
    }

    /**
     * The following code implements a started Service.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStartCommandCalls++;
        return START_NOT_STICKY;
    }

    /**
     * The following code is shared by both started and bound Service.
     */
    @Override
    public void onCreate() {
        onCreateCalls++;
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        onDestroyCalls++;
        super.onDestroy();
        if (mRecorder != null) {
            stopRecording();
        }

        if (onRecordingStatusChangedListener != null) onRecordingStatusChangedListener = null;
    }

    public void startRecording(int duration) {
        setFileNameAndPath();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setOutputFile(mFilePath);

        mRecorder.setMaxDuration(duration); // set the max duration, after which the Service is stopped
        mRecorder.setAudioChannels(1);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setAudioEncodingBitRate(192000);

        // Called only if a max duration has been set.
        mRecorder.setOnInfoListener((mediaRecorder, what, extra) -> {
            if (what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                stopRecording();
            }
        });

        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartingTimeMillis = System.currentTimeMillis();
            isRecording = true;

            startTimer();
        } catch (IOException e) {
            Log.e(TAG, CLASS_NAME + " - startRecording(): " + "prepare() failed" + e.toString());
        }

        if (onRecordingStatusChangedListener != null) {
            onRecordingStatusChangedListener.onRecordingStarted();
        }
    }

    private void setFileNameAndPath() {
        mFileName = "myrec" + System.currentTimeMillis();
        mFilePath = Utils.getDirectoryPath(this) + "/" + mFileName;
        Log.d(TAG, "mFilePath =  " + mFilePath);
    }

    private void startTimer() {
        Timer mTimer = new Timer();

        // Increment seconds.
        mElapsedMillis = 0;
        mIncrementTimerTask = new TimerTask() {
            @Override
            public void run() {
                mElapsedMillis += 100;
                if (onRecordingStatusChangedListener != null) {
                    onRecordingStatusChangedListener.onTimerChanged((int) mElapsedMillis / 1000);
                }
                if (onRecordingStatusChangedListener != null && mRecorder != null) {
                    try {
                        onRecordingStatusChangedListener.onAmplitudeInfo(mRecorder.getMaxAmplitude());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mTimer.scheduleAtFixedRate(mIncrementTimerTask, 0, 100);
    }

    public void stopRecording() {
        mRecorder.stop();
        long mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
        mRecorder.release();
        isRecording = false;
        mRecorder = null;

        // Communicate the file path to the connected Activity.
        if (onRecordingStatusChangedListener != null) {
            onRecordingStatusChangedListener.onRecordingStopped(mFilePath, mElapsedMillis);
        }

        // Stop timer.
        if (mIncrementTimerTask != null) {
            mIncrementTimerTask.cancel();
            mIncrementTimerTask = null;
        }

        if (onRecordingStatusChangedListener == null)
            stopSelf();

        stopForeground(true);
    }

    public boolean isRecording() {
        return isRecording;
    }
}
