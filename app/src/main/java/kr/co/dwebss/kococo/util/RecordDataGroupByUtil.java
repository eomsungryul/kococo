package kr.co.dwebss.kococo.util;

import com.github.mikephil.charting.data.BarEntry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordDataGroupByUtil {

    int standardMinite =0;
    float standardDb =0;

    public RecordDataGroupByUtil() {
    }

    public JsonArray groupByMinites(JsonArray aList) {
        JsonArray result = new JsonArray();
        aList.size();
        //리스트의값을 뽑는다
        //1~60초까지의 데이터중에 가장 큰 값을 가져온다.
        if(aList.size()>0){
            for(int i =0; i<aList.size(); i++) {
                JsonObject analysisRawData = (JsonObject) aList.get(i);
                //데이터 넣는 부분
                //타임은 recordStartDt 이후의 시간
                int time = analysisRawData.get("TIME").getAsInt();
                float db = analysisRawData.get("DB").getAsFloat();
                int minute = time / 60;
                //0에서는 무조건 기준값을 넣어줌
                if(i==0){
                    standardMinite =minute;
                    standardDb =db;
                }else{
                    //같으면 서로 비교
                    if(standardMinite==minute){
                        if(db>standardDb){
                            standardDb =db;
                        }
                        //하다가 끝이나면 값을 넣어줌
                        if(i==aList.size()){
                            JsonObject obj = new JsonObject();
                            obj.addProperty("TIME",minute);
                            obj.addProperty("DB",db);
                            result.add(obj);
                        }
                    }else{
                        JsonObject obj = new JsonObject();
                        obj.addProperty("TIME",minute);
                        obj.addProperty("DB",db);
                        result.add(obj);
                        standardMinite=minute;
                    }
                }
            }
        }
        return  result;
    }

}
