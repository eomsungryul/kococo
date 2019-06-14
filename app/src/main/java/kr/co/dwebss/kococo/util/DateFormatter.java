package kr.co.dwebss.kococo.util;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateFormatter{

    SimpleDateFormat stringtoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    SimpleDateFormat transFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
    SimpleDateFormat DateTimeToStringFormat = new SimpleDateFormat("HH:mm");
    SimpleDateFormat DateSecondToStringFormat = new SimpleDateFormat("H'시간'm'분's'초'", Locale.KOREA);

    public DateFormatter() {
    }

    public Date stringtoDateFormat(String date) {
        Date result = null;
        try {
            result = stringtoDateFormat.parse(date);

        } catch (ParseException e) {

            e.printStackTrace();
        }
        return result;
    }

    public String returnStringISO8601ToHHmmssFormat(String date) {
        Date dt = null;
        String result = new String();
        try {
            dt = stringtoDateFormat.parse(date);
            result = DateSecondToStringFormat.format(dt);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }


    public String longToStringFormat(Long date) {
        String result = new String();

        int seconds = (int) (date / 1000) % 60 ;
        int minutes = (int) ((date / (1000*60)) % 60);
        int hours   = (int) ((date / (1000*60*60)) % 24);
//
//        String.format("%d시간 %d분 %d 초",
//                TimeUnit.MILLISECONDS.toHours(date),
//                TimeUnit.MILLISECONDS.toMinutes(date) -
//                        TimeUnit.MINUTES.toMinutes(TimeUnit.MILLISECONDS.toHours(date)),
//                TimeUnit.MILLISECONDS.toSeconds(date) -
//                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(date))
//        );
//        String.format("%d시간 %d분 %d 초",hours, minutes, seconds)
        if(hours==0){
            result = String.format("%d분 %d 초", minutes, seconds);
        }else{
            result = String.format("%d시간 %d분",hours, minutes);
        }


        return  result;
    }

}
