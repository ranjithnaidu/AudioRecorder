package com.ranjithnaidu.audiorecorder.playback;

import android.media.MediaPlayer;
import android.util.Log;

import com.ranjithnaidu.audiorecorder.playback.viewmodel.PlaybackViewModel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Exposes the functionality of the {@link MediaPlayer} and implements the {@link PlayerAdapter}
 * so that {@link PlaybackViewModel} can control music playback.
 */
public final class MediaPlayerHolder implements PlayerAdapter {

    public static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;

    private MediaPlayer mMediaPlayer;
    private String path;
    private PlaybackInfoListener mPlaybackInfoListener;
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekbarPositionUpdateTask;

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {@link PlaybackViewModel} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {@link PlaybackViewModel} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private void initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopUpdatingCallbackWithPosition(true);
                    if (mPlaybackInfoListener != null) {
                        mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.COMPLETED);
                        mPlaybackInfoListener.onPlaybackCompleted();
                    }
                }
            });

//            createVisualizer(mMediaPlayer);
        }
    }

//    private Visualizer audioOutput = null;
//    private int intensity = 0; //intensity is a value between 0 and 1. The intensity in this case is the system output volume
//
//    private void createVisualizer(MediaPlayer mediaPlayer){
//        int rate = Visualizer.getMaxCaptureRate();
//        audioOutput = new Visualizer(mediaPlayer.getAudioSessionId()); // get output audio stream
//        audioOutput.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
//            @Override
//            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
//                Log.e("waveform_raw", String.valueOf(waveform.length));
//                Log.e("visualizer", visualizer.toString());
//                intensity = (int) (((float) waveform[0] + 128f) / 256);
//                Log.e("intensity", String.valueOf(intensity));
//            }
//
//            @Override
//            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) { }
//        },rate , true, false); // waveform not freq data
//        audioOutput.setEnabled(true);
//    }

    public void setPlaybackInfoListener(PlaybackInfoListener listener) {
        mPlaybackInfoListener = listener;
    }

    // Implements PlaybackControl.
    @Override
    public void loadMedia(String path) {
        this.path = path;

        initializeMediaPlayer();

        try {
            mMediaPlayer.setDataSource(path);
        } catch (Exception e) {
            Log.e("load()", e.toString());
        }

        try {
            mMediaPlayer.prepare();
        } catch (Exception e) {
            Log.e("prepare()", e.toString());
        }

        initializeProgressCallback();
    }

    @Override
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void play() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);
            }
            startUpdatingCallbackWithPosition();
        }
    }

    @Override
    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            loadMedia(path);
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.RESET);
            }
            stopUpdatingCallbackWithPosition(true);
        }
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);
            }
        }
    }

    @Override
    public void seekTo(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    /**
     * Syncs the mMediaPlayer position with mPlaybackProgressCallback via recurring task.
     */
    private void startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if (mSeekbarPositionUpdateTask == null) {
            mSeekbarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    updateProgressCallbackTask();
                }
            };
        }
        mExecutor.scheduleAtFixedRate(
                mSeekbarPositionUpdateTask,
                0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    // Reports media playback position to mPlaybackProgressCallback.
    private void stopUpdatingCallbackWithPosition(boolean resetUIPlaybackPosition) {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
            mSeekbarPositionUpdateTask = null;
            if (resetUIPlaybackPosition && mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(0);
            }
        }
    }

    private void updateProgressCallbackTask() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(currentPosition);
            }
        }
    }

    @Override
    public void initializeProgressCallback() {
        final int duration = mMediaPlayer.getDuration();
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener.onDurationChanged(duration);
            mPlaybackInfoListener.onPositionChanged(0);
        }
    }
}
