package kr.co.dwebss.kococo.util;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Yasir on 02/06/16.
 */
public class MyXAxisValueFormatter implements IAxisValueFormatter
{

    private long referenceTimestamp; // minimum timestamp in your data set
    private long standardMinite; // minimum timestamp in your data set
    String oldVal;

    //시작시간은 기존 HH:mm를 받고 그걸로
    public MyXAxisValueFormatter(long referenceTimestamp) {
        this.referenceTimestamp = referenceTimestamp;
        DateFormat mDataFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);
        Date mDate = new Date(referenceTimestamp);
        int hours = mDate.getHours();
        int minutes = mDate.getMinutes();
        oldVal = new String();
        this.standardMinite= (hours*60)+minutes;

    }

    /**
     * Called when a value from an axis is to be formatted
     * before being drawn. For performance reasons, avoid excessive calculations
     * and memory allocations inside this method.
     *
     * @param value the value to be formatted
     * @param axis  the axis the value belongs to
     * @return
     */
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        // convertedTimestamp = originalTimestamp - referenceTimestamp
        long convertedTimestamp = (long) value;
        // Retrieve original timestamp
        long originalTimestamp = standardMinite+convertedTimestamp;
//        System.out.println("========MyXAxisValueFormatter==convertedTimestamp============"+convertedTimestamp);
        return getHour(originalTimestamp);
    }

    private String getHour(long timestamp){
        long minutes = (long) ((timestamp) % 60);
        long hours   = (long) ((timestamp /60) % 24);
//        System.out.println("=======MyXAxisValueFormatter===hours========"+hours+"==========minutes========"+minutes);
        String now = String.format("%02d:%02d",hours, minutes);
        if("".equals(oldVal)){
            oldVal=now;
            return now;
        }else{
            if(oldVal.equals(now)){
                oldVal=now;
                return "";
            }else{
                oldVal=now;
                return now;
            }
        }
    }
}

//public class MyXAxisValueFormatter implements IAxisValueFormatter {
//    private String[] mValues;
//
////    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd:hh:mm:ss");
//    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
//
//    public MyXAxisValueFormatter(String[] values) {
//        this.mValues = values; }
//
//    @Override
//    public String getFormattedValue(float value, AxisBase axis) {
//        return sdf.format(new Date((long) value));
//    }
//}