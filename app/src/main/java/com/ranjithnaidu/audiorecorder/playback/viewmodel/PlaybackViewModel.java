package com.ranjithnaidu.audiorecorder.playback.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ranjithnaidu.audiorecorder.model.Recording;
import com.ranjithnaidu.audiorecorder.playback.MediaPlayerHolder;
import com.ranjithnaidu.audiorecorder.playback.PlaybackInfoListener;
import com.ranjithnaidu.audiorecorder.playback.PlayerAdapter;

import java.util.concurrent.TimeUnit;

public class PlaybackViewModel extends AndroidViewModel {

    private static final String TAG = "PlaybackViewModel";

    public PlaybackViewModel(@NonNull Application application) {
        super(application);
    }

    public final ObservableBoolean isPlaying = new ObservableBoolean(true);

    public final ObservableInt secondsElapsed = new ObservableInt(0);
    private final MutableLiveData<Integer> amplitudeLive = new MutableLiveData<>();

    public LiveData<Integer> getAmplitudeLive() {
        return amplitudeLive;
    }

    private Recording recording;

    private PlayerAdapter mPlayerAdapter;

    public void setRecordingData(Recording recording) {
        this.recording = recording;

        initializePlaybackController();

        mPlayerAdapter.loadMedia(recording.getPath());
        mPlayerAdapter.play();
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder();
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
    }

    public void release() {
        mPlayerAdapter.release();
        secondsElapsed.set(0);
        isPlaying.set(true);
    }

    public void onPlay() {
        mPlayerAdapter.play();
        isPlaying.set(mPlayerAdapter.isPlaying());
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {

        }

        @Override
        public void onPositionChanged(int position) {

            int time = (int) TimeUnit.MILLISECONDS.toSeconds(position);

            secondsElapsed.set(time);
            Log.e("Time", String.valueOf(time));
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
            isPlaying.set(mPlayerAdapter.isPlaying());
        }

        @Override
        public void onLogUpdated(String message) {

        }
    }
}