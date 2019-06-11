package kr.co.dwebss.kococo.util;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatter{

    SimpleDateFormat stringtoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    SimpleDateFormat transFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
    SimpleDateFormat DateTimeToStringFormat = new SimpleDateFormat("HH:mm");
    SimpleDateFormat DateSecondToStringFormat = new SimpleDateFormat("HH:mm:ss");

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

}
