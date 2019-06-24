package com.ranjithnaidu.audiorecorder.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static String getDirectoryPath(Context context) {
        if (isExternalStorageWritable()) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS), "AudioRecorder");
            if (file.mkdirs()) {
                return file.getAbsolutePath();
            }
        }

        return context.getFilesDir().getAbsolutePath(); // use internal storage if external storage is not available
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Format seconds elapsed for chronometer in mm:ss format.
    public static String formatSecondsElapsedForChronometer(int seconds) {
        SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
        return mTimerFormat.format(new Date(seconds * 1000L));
    }
}