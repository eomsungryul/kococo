package kr.co.dwebss.kococo.util;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Yasir on 02/06/16.
 */
public class MyXAxisValueFormatter implements IAxisValueFormatter
{

    private long referenceTimestamp; // minimum timestamp in your data set
    private DateFormat mDataFormat;
    private Date mDate;

    public MyXAxisValueFormatter(long referenceTimestamp) {
        this.referenceTimestamp = referenceTimestamp;
//        this.mDataFormat = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
        this.mDataFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);
        this.mDate = new Date();
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
        long originalTimestamp = referenceTimestamp + convertedTimestamp;
        System.out.println("==========getHour(originalTimestamp)============"+getHour(originalTimestamp));
        // Convert timestamp to hour:minute

        return getHour(originalTimestamp);
    }

    private String getHour(long timestamp){

        try{
            mDate.setTime(timestamp);
            System.out.println("==========mDataFormat.format(mDate)============"+mDataFormat.format(mDate));
            return mDataFormat.format(mDate);
        }
        catch(Exception ex){
            return "xx";
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