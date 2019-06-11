package kr.co.dwebss.kococo.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import kr.co.dwebss.kococo.R;

public class MediaPlayerUtility {

    private String LOG_TAG = "MediaPlayerUtility";
    MediaPlayer mediaPlayer;
    CountDownTimer cdt;

    public MediaPlayerUtility(){
        mediaPlayer = new MediaPlayer();
    }

    public static long getTime(MediaPlayer mediaPlayer) {
        long totalDuration = mediaPlayer.getDuration(); // to get total duration in milliseconds
        long currentDuration = mediaPlayer.getCurrentPosition(); // to Gets the current playback position in milliseconds
        return currentDuration;
    }

    public void playMp(int startTime, int endTime , String filePath, Context context) throws IOException {
//        mediaPlayer = MediaPlayer.create(context, Uri.parse("/data/data/kr.co.dwebss.kococo/files/rec_data/23/snoring-20191029_0410-29_0410_1559113854914.wav"));
//        mediaPlayer = MediaPlayer.create(context, R.raw.queen);
        Log.v(LOG_TAG,( "======== play() startTime : " +startTime+" /endTime : "+endTime+"/filePath : "+filePath));

        File file = new File(filePath);
        if(file.exists()){
            //만일 지금 재생중이라면 멈추고 버튼도 재생버튼으로 바꾼다
            mediaPlayer = MediaPlayer.create(context, Uri.parse(filePath));
            //구간 재생
            mediaPlayer.seekTo(startTime);
            mediaPlayer.getCurrentPosition();
            mediaPlayer.start();

            //구간 재생을 위한 카운트 다운 타이머
            cdt = new CountDownTimer(endTime-startTime, 100) {
                public void onTick(long millisUntilFinished) {
//                        System.out.println("=============================onTick"+millisUntilFinished);
                    if(MediaPlayerUtility.getTime(mediaPlayer)>=endTime){
                        stopMp();
                        cancel();
                    }
                }
                public void onFinish() {
                    stopMp();
                }
            }.start();
        }else{
            Toast.makeText(context,"파일이 존재하지않습니다.",Toast.LENGTH_SHORT).show();
        }
    }

    public void stopMp(){
        mediaPlayer.stop();
        mediaPlayer.reset();
    }

    public void endMp(){
        cdt.cancel();
        mediaPlayer.release();
        mediaPlayer=null;
    }

}
