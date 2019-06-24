package com.ranjithnaidu.audiorecorder.playback.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ranjithnaidu.audiorecorder.R;
import com.ranjithnaidu.audiorecorder.databinding.FragmentMediaPlaybackBinding;
import com.ranjithnaidu.audiorecorder.model.Recording;
import com.ranjithnaidu.audiorecorder.playback.viewmodel.PlaybackViewModel;
import com.ranjithnaidu.audiorecorder.utils.AudioLevelView;

import java.util.Objects;

public class PlaybackFragment extends BottomSheetDialogFragment {

    private static final String ARG_ITEM = "recording_item";

    private PlaybackViewModel playbackViewModel;

    private AudioLevelView audioView;

    public PlaybackFragment newInstance(Recording recording) {
        PlaybackFragment playbackFragment = new PlaybackFragment();
        Bundle b = new Bundle();
        b.putParcelable(ARG_ITEM, recording);
        playbackFragment.setArguments(b);

        return playbackFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playbackViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(PlaybackViewModel.class);
        playbackViewModel.setRecordingData(Objects.requireNonNull(getArguments()).getParcelable(ARG_ITEM));

        playbackViewModel.getAmplitudeLive().observe(this, integer ->
                audioView.addAmplitude(integer));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        FragmentMediaPlaybackBinding binding = FragmentMediaPlaybackBinding.inflate(inflater, container, false);
        binding.setViewModel(playbackViewModel);
        View view = binding.getRoot();

        audioView = view.findViewById(R.id.audio_view);

        view.findViewById(R.id.fab_play).setOnClickListener(v -> playbackViewModel.onPlay());

        return view;
    }

    private boolean firstCallback = true;
    private Observable.OnPropertyChangedCallback secsCallback;

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
        playbackViewModel.secondsElapsed.addOnPropertyChangedCallback(secsCallback);
    }

    @Override
    public void onPause() {
        super.onPause();

        playbackViewModel.secondsElapsed.removeOnPropertyChangedCallback(secsCallback);
    }

    @Override
    public void onStop() {
        super.onStop();

        playbackViewModel.release();
    }
}

