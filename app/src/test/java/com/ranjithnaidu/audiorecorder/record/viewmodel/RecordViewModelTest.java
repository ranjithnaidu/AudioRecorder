package com.ranjithnaidu.audiorecorder.record.viewmodel;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.ranjithnaidu.audiorecorder.R;
import com.ranjithnaidu.audiorecorder.service.RecordingService;
import com.ranjithnaidu.audiorecorder.testutils.TestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static android.content.Context.BIND_AUTO_CREATE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Unit tests for the implementation of RecordViewModel.
 */
public class RecordViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private RecordingService recordingService;
    @Mock
    private Application context;
    @Mock
    private RecordingService.LocalBinder iBinder;
    @Mock
    private ComponentName componentName;
    @Captor
    private ArgumentCaptor<ServiceConnection> serviceConnectionArgumentCaptor;
    @Captor
    private ArgumentCaptor<RecordingService.OnRecordingStatusChangedListener> onRecordingStatusChangedListenerArgumentCaptor;

    private RecordViewModel recordViewModel;
    private Intent intent;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        recordViewModel = new RecordViewModel(context, recordingService);

        intent = new Intent(context, RecordingService.class);
        intent.putExtra("com.ranjithnaidu.audiorecorder.EXTRA_ACTIVITY_STARTER", true);

        Mockito.when(iBinder.getService()).thenReturn(recordingService);
        Mockito.when(context.getString(R.string.toast_recording_start)).thenReturn("Recording started");
    }

    @Test
    public void testInitialValues() {
        assertFalse(recordViewModel.serviceConnected.get());
        assertFalse(recordViewModel.serviceRecording.get());
        assertEquals(0, recordViewModel.secondsElapsed.get());
    }

    @Test
    public void testStartRecording() {
        recordViewModel.startRecording();
        Mockito.verify(recordingService).startRecording(20000);
        assertTrue(recordViewModel.serviceRecording.get());
    }

    @Test
    public void testStopRecording() {
        recordViewModel.stopRecording();
        Mockito.verify(recordingService).stopRecording();
    }

    @Test
    public void testServiceConnectionAndStop() {

        // Connection to service.
        recordViewModel.connectService(intent);
        Mockito.verify(context).bindService(ArgumentMatchers.eq(intent), serviceConnectionArgumentCaptor.capture(), ArgumentMatchers.eq(BIND_AUTO_CREATE));
        serviceConnectionArgumentCaptor.getValue().onServiceConnected(componentName, iBinder);
        assertTrue(recordViewModel.serviceConnected.get());
        assertFalse(recordViewModel.serviceRecording.get());
        Mockito.verify(recordingService).setOnRecordingStatusChangedListener(onRecordingStatusChangedListenerArgumentCaptor.capture());

        // Start recording.
        Observer<Integer> observer = Mockito.mock(Observer.class);
        recordViewModel.getToastMsg().observe(TestUtils.TEST_OBSERVER, observer);
        recordViewModel.startRecording();
        onRecordingStatusChangedListenerArgumentCaptor.getValue().onRecordingStarted();
        assertTrue(recordViewModel.serviceRecording.get());
        Mockito.verify(observer).onChanged(R.string.toast_recording_start);

        // Change seconds elapsed.
        onRecordingStatusChangedListenerArgumentCaptor.getValue().onTimerChanged(10);
        assertEquals(10, recordViewModel.secondsElapsed.get());

        // Stop recording.
        recordViewModel.stopRecording();
        onRecordingStatusChangedListenerArgumentCaptor.getValue().onRecordingStopped("file_path", 1000L);
        assertFalse(recordViewModel.serviceRecording.get());
        assertEquals(0, recordViewModel.secondsElapsed.get());
        Mockito.verify(observer).onChanged(R.string.toast_recording_saved);

        // Disconnect and stop Service.
        Intent stopIntent = new Intent(context, RecordingService.class);
        recordViewModel.disconnectAndStopService(stopIntent);
        Mockito.verify(context).unbindService(ArgumentMatchers.any(ServiceConnection.class));
        Mockito.verify(context).stopService(stopIntent);
        Mockito.verify(recordingService).setOnRecordingStatusChangedListener(null);
        assertNull(recordViewModel.getRecordingService());
        assertFalse(recordViewModel.serviceConnected.get());
    }

    @Test
    public void testServiceConnectionAndDisconnection() {

        // Connection to service.
        recordViewModel.connectService(intent);
        Mockito.verify(context).bindService(ArgumentMatchers.eq(intent), serviceConnectionArgumentCaptor.capture(), ArgumentMatchers.eq(BIND_AUTO_CREATE));
        serviceConnectionArgumentCaptor.getValue().onServiceConnected(componentName, iBinder);
        assertTrue(recordViewModel.serviceConnected.get());
        assertFalse(recordViewModel.serviceRecording.get());
        Mockito.verify(recordingService).setOnRecordingStatusChangedListener(onRecordingStatusChangedListenerArgumentCaptor.capture());

        // Start recording.
        Observer<Integer> observer = Mockito.mock(Observer.class);
        recordViewModel.getToastMsg().observe(TestUtils.TEST_OBSERVER, observer);
        recordViewModel.startRecording();
        onRecordingStatusChangedListenerArgumentCaptor.getValue().onRecordingStarted();
        assertTrue(recordViewModel.serviceRecording.get());
        Mockito.verify(observer).onChanged(R.string.toast_recording_start);

        // Change seconds elapsed.
        onRecordingStatusChangedListenerArgumentCaptor.getValue().onTimerChanged(10);
        assertEquals(10, recordViewModel.secondsElapsed.get());

        // Stop recording.
        recordViewModel.stopRecording();
        onRecordingStatusChangedListenerArgumentCaptor.getValue().onRecordingStopped("file_path", 1000L);
        assertFalse(recordViewModel.serviceRecording.get());
        assertEquals(0, recordViewModel.secondsElapsed.get());
        Mockito.verify(observer).onChanged(R.string.toast_recording_saved);

        // Disconnect service automatically
        serviceConnectionArgumentCaptor.getValue().onServiceDisconnected(componentName);
        Mockito.verify(recordingService).setOnRecordingStatusChangedListener(null);
        assertNull(recordViewModel.getRecordingService());
        assertFalse(recordViewModel.serviceConnected.get());
    }
}
