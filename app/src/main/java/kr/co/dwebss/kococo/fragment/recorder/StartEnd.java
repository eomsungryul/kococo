package kr.co.dwebss.kococo.fragment.recorder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.model.AnalysisRawData;

public class StartEnd {
    public int negitiveCnt;
    public int positiveCnt;
    double start;
    double end;
    List<AnalysisRawData> AnalysisRawDataList;
    double second;
    double first;
    double chk;

    public String getTerm() {
        return
                String.format("%.2f", start)
                        + "~" + String.format("%.2f", end)
                        + " second: " + String.format("%.2f", second)
                        + " first: " + String.format("%.2f", first)
                        + " chk: " + String.format("%.2f", chk)
                        + " positiveCnt: " + positiveCnt
                        + " negitiveCnt: " + negitiveCnt;
    }

    public String getTermForRequest(int termCd, long recordStartDtL) {
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return "termTypeCd: " + termCd + ", termStartDt: "
                + dayTime.format(new Date((long) (recordStartDtL + this.start * 1000))) + ",termEndDt: "
                + dayTime.format(new Date((long) (recordStartDtL + this.end * 1000)));
    }

//    public String printAnalysisRawDataList() {
//        String rtn = "";
//        if(this.AnalysisRawDataList!=null) {
//            for(AnalysisRawData d : this.AnalysisRawDataList) {
//                rtn+=d.toString()+"\r\n";
//            }
//        }
//        return rtn;
//    }

    public JsonArray printAnalysisRawDataList() {
        JsonArray rtn = new JsonArray();
        if(this.AnalysisRawDataList!=null) {
            for(AnalysisRawData d : this.AnalysisRawDataList) {

                JsonObject data = new JsonObject();
                data.addProperty("TIME", String.format("%.0f", d.getTimes()));
                data.addProperty("DB", String.format("%.2f", d.getDecibel()));
                //data.addProperty("HZ", d.getFrequency());
                //data.addProperty("AMP", d.getAmplitude());

//                rtn+=d.toString()+"\r\n";
                rtn.add(data);
            }
        }
        return rtn;
    }
}


