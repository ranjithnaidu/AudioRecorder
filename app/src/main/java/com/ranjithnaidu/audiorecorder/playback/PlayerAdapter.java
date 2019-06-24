package com.ranjithnaidu.audiorecorder.playback;

import com.ranjithnaidu.audiorecorder.playback.viewmodel.PlaybackViewModel;

/**
 * Allows {@link PlaybackViewModel} to control media playback of {@link MediaPlayerHolder}.
 */
public interface PlayerAdapter {

    void loadMedia(String path);

    void release();

    boolean isPlaying();

    void play();

    void reset();

    void pause();

    void initializeProgressCallback();

    void seekTo(int position);
}
