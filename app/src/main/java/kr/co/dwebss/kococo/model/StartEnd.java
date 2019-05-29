package kr.co.dwebss.kococo.model;

import java.text.SimpleDateFormat;
import java.util.Date;


/*
* 시작시간과 종료시간관련 VO
*
* */
public class StartEnd {

    double start;
    double end;
    public String getTerm() {
        return String.format("%.0f", start)+"~"+String.format("%.0f", end);
    }
    public String getTermForRequest(int termCd, long recordStartingTIme) {
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return "termTypeCd: "+termCd +", termStartDt: "+dayTime.format(new Date((long) (recordStartingTIme+this.start*1000)))+", termEndDt"+dayTime.format(new Date((long) (recordStartingTIme+this.end*1000)));
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }
}
