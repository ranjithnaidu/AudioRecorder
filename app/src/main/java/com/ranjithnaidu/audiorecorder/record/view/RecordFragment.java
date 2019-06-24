package com.ranjithnaidu.audiorecorder.record.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.ranjithnaidu.audiorecorder.playback.view.PlaybackFragment;
import com.ranjithnaidu.audiorecorder.R;
import com.ranjithnaidu.audiorecorder.record.viewmodel.RecordViewModel;
import com.ranjithnaidu.audiorecorder.databinding.FragmentRecordBinding;
import com.ranjithnaidu.audiorecorder.model.Recording;
import com.ranjithnaidu.audiorecorder.utils.AudioLevelView;
import com.ranjithnaidu.audiorecorder.utils.PermissionsManager;

import java.util.Objects;

public class RecordFragment extends Fragment {
    private static final String TAG = "AUDIO_RECORDER_TAG";

    private static final int REQUEST_DANGEROUS_PERMISSIONS = 0;
    private final boolean marshmallow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    private RecordViewModel recordViewModel;
    private AudioLevelView audioView;

    private TextView timeRemaining;

    private boolean firstCallback = true;
    private Observable.OnPropertyChangedCallback secsCallback;

    public static RecordFragment newInstance() {

        return new RecordFragment();
    }

    public RecordFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recordViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(RecordViewModel.class);
        recordViewModel.getToastMsg().observe(this, msgId ->
                Toast.makeText(getActivity(), getString(msgId), Toast.LENGTH_SHORT).show());
        recordViewModel.getAmplitudeLive().observe(this, integer -> {
            audioView.addAmplitude(integer);
        });

        recordViewModel.timeRemaining.observe(this, time ->
                timeRemaining.setText(time));

        recordViewModel.serviceRecording.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                boolean isRecording = ((ObservableBoolean) sender).get();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FragmentRecordBinding binding = FragmentRecordBinding.inflate(inflater, container, false);
        binding.setViewModel(recordViewModel);
        View rootView = binding.getRoot();

        rootView.findViewById(R.id.btnRecord).setOnClickListener(v -> checkPermissionsAndRecord());

        rootView.findViewById(R.id.btnPlay).setOnClickListener(v -> startPlaying(recordViewModel.recording));

        timeRemaining = rootView.findViewById(R.id.time_remaining);

        audioView = rootView.findViewById(R.id.audio_view);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        firstCallback = true;
        // When receiving the first second, adjust the line of times.
        secsCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (firstCallback) {
                    firstCallback = false;

                    int secs = ((ObservableInt) sender).get();
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> audioView.startRecording(secs));
                }
            }
        };
        recordViewModel.secondsElapsed.addOnPropertyChangedCallback(secsCallback);
    }

    @Override
    public void onPause() {
        super.onPause();

        recordViewModel.secondsElapsed.removeOnPropertyChangedCallback(secsCallback);
    }

    private void startPlaying(Recording recording) {
        try {
            PlaybackFragment playbackFragment = new PlaybackFragment().newInstance(recording);
            playbackFragment.show(Objects.requireNonNull(getFragmentManager()), "dialog_playback");
        } catch (Exception e) {
            Log.e(TAG, "error in playing the recording" + e.toString());
        }
    }

    // Check dangerous permissions for Android Marshmallow+.
    private void checkPermissionsAndRecord() {
        if (!marshmallow) {
            startStopRecording();
            return;
        }

        String[] permissionsToAsk = PermissionsManager.checkPermissions(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO);
        if (permissionsToAsk.length > 0)
            requestPermissions(permissionsToAsk, REQUEST_DANGEROUS_PERMISSIONS);
        else
            startStopRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED)
                granted = false;
        }

        if (granted)
            startStopRecording();
        else
            Toast.makeText(getActivity(), getString(R.string.toast_permissions_denied), Toast.LENGTH_LONG).show();
    }

    private void startStopRecording() {
        firstCallback = false;
        if (!recordViewModel.serviceRecording.get()) { // start recording
            recordViewModel.startRecording();
            audioView.startRecording(0);
        } else { //stop recording
            recordViewModel.stopRecording();
            audioView.stopRecording();
        }
    }
}