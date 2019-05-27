package kr.co.dwebss.kococo.util;

import android.media.MediaPlayer;

public class MediaPlayerUtility {

    public static long getTime(MediaPlayer mediaPlayer) {


        long totalDuration = mediaPlayer.getDuration(); // to get total duration in milliseconds

        long currentDuration = mediaPlayer.getCurrentPosition(); // to Gets the current playback position in milliseconds

        return currentDuration;

    }
}
