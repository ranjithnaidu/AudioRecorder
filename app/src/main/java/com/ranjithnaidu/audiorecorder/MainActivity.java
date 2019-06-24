package com.ranjithnaidu.audiorecorder;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

import com.ranjithnaidu.audiorecorder.record.view.RecordFragment;
import com.ranjithnaidu.audiorecorder.record.viewmodel.RecordViewModel;
import com.ranjithnaidu.audiorecorder.service.RecordingService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AUDIO_RECORDER_TAG";

    private RecordViewModel recordViewModel; // manages connection with RecordingService

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordViewModel = ViewModelProviders.of(this).get(RecordViewModel.class);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        fragmentTransaction.add(R.id.fragment_container, RecordFragment.newInstance())
                .commitAllowingStateLoss();
    }

    // Connection with local Service through the view model.
    @Override
    protected void onStart() {
        super.onStart();

        recordViewModel.connectService(RecordingService.makeIntent(this, true));
    }

    // Disconnection from local Service.
    @Override
    protected void onStop() {
        super.onStop();

        recordViewModel.disconnectAndStopService(new Intent(this, RecordingService.class));
    }
}
