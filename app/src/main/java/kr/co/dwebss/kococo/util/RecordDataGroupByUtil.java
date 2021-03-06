package kr.co.dwebss.kococo.util;

import com.github.mikephil.charting.data.BarEntry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecordDataGroupByUtil {

    int standardMinite =0;
    float standardDb =0;


    int standardSecend =0;

    int endMinite =0;

    public RecordDataGroupByUtil() {
    }

    public JsonArray groupByMinites(JsonArray aList,String standardDate) {

        standardSecend = Integer.parseInt(standardDate.substring(standardDate.length()-2,standardDate.length()));
        System.out.println("================== standardDate.substring(standardDate.length()-2,standardDate.length())=="+ standardDate.substring(standardDate.length()-2,standardDate.length()));

        JsonArray result = new JsonArray();
        //리스트의값을 뽑는다
        //1~60초까지의 데이터중에 가장 큰 값을 가져온다.
        Map<Integer,Float> list = new LinkedHashMap<>();
        if(aList.size()>0){
            for(int i =0; i<aList.size(); i++) {
                JsonObject analysisRawData = (JsonObject) aList.get(i);
                //데이터 넣는 부분
                //타임은 recordStartDt 이후의 시간
                int time = analysisRawData.get("TIME").getAsInt();
                float db = analysisRawData.get("DB").getAsFloat();
                int minute = (standardSecend+time) / 60;
                if(list.containsKey(minute)){
                    float currentDb= list.get(minute);
                    if(currentDb<db){
                        list.put(minute,db);
                    }
                }else{
                    list.put(minute,db);
                }
            }

            Set key = list.keySet();
            for (Iterator iterator = key.iterator(); iterator.hasNext();) {
                Integer keyName = (Integer) iterator.next();
                Float valueName = (Float) list.get(keyName);

                JsonObject obj = new JsonObject();
                obj.addProperty("TIME",keyName);
                obj.addProperty("DB",valueName);
                result.add(obj);
            }

        }

        return  result;

    }


    public List<BarEntry> addZeroData(float startMinites ,float endMinites,List<BarEntry> soundEntries, List<BarEntry> snoreEntries, List<BarEntry> osaEntries, List<BarEntry> grindEntries) {
        List<BarEntry> result = new ArrayList<BarEntry>();
        int end = (int) endMinites;
        for(int i = (int) startMinites; i<=end; i++){
            result.add(new BarEntry(i,0.1f));
        }
        return  result;
    }

    public JsonArray groupByMinitesInAnalysisRange(JsonArray aList,Date recordStartDT, Date analysisStartDt) {

        //타임값은 recordTime의 기준으로 흘러가기 때문에 분석범위는 분석시작시간부터 끝으로 그래프를 나타내기 때문에 맞지않다.
        //그렇기에 해당 TIME을 두 시작 점을 뺀 차이로 계산해야함
        standardSecend = analysisStartDt.getSeconds();
        int delaySecend = (int) ((analysisStartDt.getTime()-recordStartDT.getTime())/1000);
        System.out.println("============standardSecend=="+standardSecend);
        System.out.println("============delaySecend=="+delaySecend);

        JsonArray result = new JsonArray();
        //리스트의값을 뽑는다
        //1~60초까지의 데이터중에 가장 큰 값을 가져온다.
        Map<Integer,Float> list = new HashMap<>();
        if(aList.size()>0){
            for(int i =0; i<aList.size(); i++) {
                JsonObject analysisRawData = (JsonObject) aList.get(i);
                //데이터 넣는 부분
                //타임은 recordStartDt 이후의 시간
                int time = analysisRawData.get("TIME").getAsInt();
                float db = analysisRawData.get("DB").getAsFloat();
                int minute = ((standardSecend+time)-delaySecend) / 60;
                if(list.containsKey(minute)){
                    float currentDb= list.get(minute);
                    if(currentDb<db){
                        list.put(minute,db);
                    }
                }else{
                    list.put(minute,db);
                }
            }

            Set key = list.keySet();
            for (Iterator iterator = key.iterator(); iterator.hasNext();) {
                Integer keyName = (Integer) iterator.next();
                Float valueName = (Float) list.get(keyName);

                JsonObject obj = new JsonObject();
                obj.addProperty("TIME",keyName);
                obj.addProperty("DB",valueName);
                result.add(obj);
            }

        }

        return  result;

    }

}
