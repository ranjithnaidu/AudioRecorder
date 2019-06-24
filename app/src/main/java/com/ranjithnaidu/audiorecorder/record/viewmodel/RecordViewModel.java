package com.ranjithnaidu.audiorecorder.record.viewmodel;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ranjithnaidu.audiorecorder.R;
import com.ranjithnaidu.audiorecorder.model.Recording;
import com.ranjithnaidu.audiorecorder.service.RecordingService;
import com.ranjithnaidu.audiorecorder.utils.SingleLiveEvent;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * View model for RecordFragment.
 * Manages the connection with RecordingService and the related data.
 */
public class RecordViewModel extends AndroidViewModel {

    private static final String TAG = "AUDIO_RECORDER_TAG";

    private static final int RECORDING_TIME = 20;

    private static final int RECORDING_TIME_MILLS = RECORDING_TIME * 1000;

    public final ObservableBoolean serviceConnected = new ObservableBoolean(false);
    public final ObservableBoolean serviceRecording = new ObservableBoolean(false);
    public final ObservableInt secondsElapsed = new ObservableInt(0);
    private final SingleLiveEvent<Integer> toastMsg = new SingleLiveEvent<>();
    private final MutableLiveData<Integer> amplitudeLive = new MutableLiveData<>();

    public MutableLiveData<String> timeRemaining = new MutableLiveData<>();

    public final ObservableBoolean showPlayBack = new ObservableBoolean(false);
    private RecordingService recordingService;

    public Recording recording;

    public RecordViewModel(@NonNull Application application) {
        super(application);
    }

    @VisibleForTesting
    public RecordViewModel(Application application, RecordingService recordingService) {
        super(application);
        this.recordingService = recordingService;
    }

    public void connectService(Intent intent) {
        getApplication().startService(intent);
        getApplication().bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void disconnectAndStopService(Intent intent) {
        if (!serviceConnected.get()) return;

        getApplication().unbindService(serviceConnection);
        if (!serviceRecording.get())
            getApplication().stopService(intent);
        recordingService.setOnRecordingStatusChangedListener(null);
        recordingService = null;
        serviceConnected.set(false);
    }

    public void startRecording() {
        recordingService.startRecording(RECORDING_TIME_MILLS);
        serviceRecording.set(true);
    }

    public void stopRecording() {
        recordingService.stopRecording();
    }

    public SingleLiveEvent<Integer> getToastMsg() {
        return toastMsg;
    }

    public LiveData<Integer> getAmplitudeLive() {
        return amplitudeLive;
    }

    /**
     * Implementation of ServiceConnection interface.
     * The interaction with the Service is managed by this view model.
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            recordingService = ((RecordingService.LocalBinder) iBinder).getService();
            serviceConnected.set(true);
            recordingService.setOnRecordingStatusChangedListener(onRecordingStatusChangedListener);
            serviceRecording.set(recordingService.isRecording());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (recordingService != null) {
                recordingService.setOnRecordingStatusChangedListener(null);
                recordingService = null;
            }
            serviceConnected.set(false);
        }
    };

    /**
     * Implementation of RecordingService.OnRecordingStatusChangedListener interface.
     * The Service uses this interface to communicate to the connected component that a
     * recording has started/stopped, and the seconds elapsed, so that the UI can be updated
     * accordingly.
     */
    private final RecordingService.OnRecordingStatusChangedListener onRecordingStatusChangedListener =
            new RecordingService.OnRecordingStatusChangedListener() {
                @Override
                public void onRecordingStarted() {
                    serviceRecording.set(true);
                    toastMsg.postValue(R.string.toast_recording_start);
                    timeRemaining.postValue(String.valueOf(RECORDING_TIME));
                    showPlayBack.set(false);
                }

                @Override
                public void onRecordingStopped(String filePath, Long elapsedMillis) {
                    serviceRecording.set(false);
                    secondsElapsed.set(0);
                    timeRemaining.postValue(getApplication().getString(R.string.ready));
                    toastMsg.postValue(R.string.toast_recording_saved);

                    // Save the recording data in the database.
                    recording = new Recording(filePath, elapsedMillis);

                    showPlayBack.set(true);
                }

                // This method is called from a separate thread.
                @Override
                public void onTimerChanged(int seconds) {
                    secondsElapsed.set(seconds);
                    timeRemaining.postValue(String.valueOf(RECORDING_TIME - seconds));
                }

                @Override
                public void onAmplitudeInfo(int amplitude) {
                    amplitudeLive.postValue(amplitude);
                }
            };

    @VisibleForTesting
    public RecordingService getRecordingService() {
        return recordingService;
    }

}
