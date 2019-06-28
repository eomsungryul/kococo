/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.dwebss.kococo.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.adapter.RecordListAdapter;
import kr.co.dwebss.kococo.application.DataHolderApplication;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.DateFormatter;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import kr.co.dwebss.kococo.util.JsonNullCheckUtil;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;
import kr.co.dwebss.kococo.util.MyXAxisValueFormatter;
import kr.co.dwebss.kococo.util.RecordDataGroupByUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ResultActivity extends AppCompatActivity {
    Retrofit retrofit;
    ApiService apiService;

    private static final String TAG = "ResultActivity";
    private BarChart chart;

    JsonObject responseData;

    Date recordStartD,recordEndD,recordStartDT,recordEndDT;
    Long recordTerm;
    String recordStartDTToString;

    //검출된 시간 초기화
    Long kococoTerm =0L;
    double sleepScore;

    RecordData recordData;
    JsonNullCheckUtil jncu = new JsonNullCheckUtil();;

    FindAppIdUtil fau = new FindAppIdUtil();

    List<BarEntry> soundEntries = new ArrayList<>();
    List<BarEntry> snoreEntries = new ArrayList<>();
    List<BarEntry> osaEntries = new ArrayList<>();
    List<BarEntry> grindEntries = new ArrayList<>();
    List<BarEntry> emptyEntries = new ArrayList<>();
    long referenceTimestamp; // minimum timestamp in your data set;

    MediaPlayerUtility mpu;
    DateFormatter df = new DateFormatter();

    int playingId,recordId,analysisId;
    String analysisServerUploadPath;
    int snoreCnt=0,osaCnt=0,grindCnt=0;

    RecordDataGroupByUtil rd = new RecordDataGroupByUtil();
    RecordListAdapter adapter;

    SimpleDateFormat stringtoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    SimpleDateFormat DateTimeToStringFormat = new SimpleDateFormat("HH:mm");

    String  firstData;

    SeekBar seekBar;

    //타이머 관련
    Timer mTimer =  new Timer();
    CustomTimer ct = new CustomTimer();
    float recordTime=0f;
    float barWidth=0f;
    boolean timerFlag=false;

    //인텐트 데이터 전송 대체제
    DataHolderApplication dha;

    String headerTextDate;

    JsonArray analysisFilePathList;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출
        setContentView(R.layout.activity_result);
        mpu = new MediaPlayerUtility();

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

        super.onCreate(savedInstanceState);
        dha = DataHolderApplication.getInstance();

       analysisFilePathList = new JsonArray();

        //데이터 수신
        Intent intent = getIntent();
//        if(getIntent().hasExtra("responseData")){
        if(getIntent().hasExtra("holderId")){
//            responseData = new JsonParser().parse(getIntent().getStringExtra("responseData")).getAsJsonObject();
            responseData = new JsonParser().parse(dha.popDataHolder(getIntent().getStringExtra("holderId")).toString()).getAsJsonObject();

            //뒤로가기 버튼
            ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
            bt.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ResultActivity.super.onBackPressed();
                }
            });

            //상단 헤더 날짜 텍스트 날짜가 넘어가면 시작~종료 아니면 그냥 시작 날짜로 보여준다.
            TextView dateTxtHeader = (TextView) findViewById(R.id.date_txt_header);
            SimpleDateFormat stringtoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat transFormat = new SimpleDateFormat("yy/MM/dd");
            try {
                recordStartD =  stringtoDateFormat.parse(responseData.get("recordStartD").toString().replace("\"",""));
                recordEndD =  stringtoDateFormat.parse(responseData.get("recordEndD").toString().replace("\"",""));
                recordStartDTToString = responseData.get("recordStartDt").toString().replace("\"","");
                recordStartDT =  stringtoDateTimeFormat.parse(responseData.get("recordStartDt").toString().replace("\"",""));
                recordEndDT =  stringtoDateTimeFormat.parse(responseData.get("recordEndDt").toString().replace("\"",""));
                recordTerm = recordEndDT.getTime()-recordStartDT.getTime();
                recordId = responseData.get("recordId").getAsInt();
                referenceTimestamp = recordStartDT.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //시간 HH:mm ~ HH:mm
            String recodeText = DateTimeToStringFormat.format(recordStartDT)+" ~ "+DateTimeToStringFormat.format(recordEndDT);
            if(recordStartD.equals(recordEndD)){
                if(recordStartDT.getHours()==recordEndDT.getHours()&&recordEndDT.getMinutes()==recordStartDT.getMinutes()){
                    dateTxtHeader.setText(transFormat.format(recordStartD)+" "+DateTimeToStringFormat.format(recordStartDT));
                }else{
                    dateTxtHeader.setText(transFormat.format(recordStartD)+" "+recodeText);
                }
                headerTextDate = transFormat.format(recordStartD)+" "
                        + DateTimeToStringFormat.format(recordStartDT)+"부터 "+DateTimeToStringFormat.format(recordEndDT)+"까지";
            }else{
                dateTxtHeader.setText(transFormat.format(recordStartD)+" "
                        + DateTimeToStringFormat.format(recordStartDT)+" ~ "+transFormat.format(recordEndD)+" "+DateTimeToStringFormat.format(recordEndDT));
                headerTextDate = transFormat.format(recordStartD)+" "
                        + DateTimeToStringFormat.format(recordStartDT)+"부터 "+transFormat.format(recordEndD)+" "+DateTimeToStringFormat.format(recordEndDT)+"까지";
            }

            // 하단에 녹음 검출리스트 파일 리스트  Adapter 생성
            adapter = new RecordListAdapter(this, new RecordListAdapter.GraphClickListener() {
                @Override
                public void clickBtn(RecordData listViewItem, Boolean playFlag) {
                    changeGraph(listViewItem);
                    if(playFlag){
                        //재생일 시에 ttt
                        long playTime = 0;
                        try {
                            playTime = stringtoDateTimeFormat.parse(listViewItem.getAnalysisEndDt()).getTime()-stringtoDateTimeFormat.parse(listViewItem.getAnalysisStartDt()).getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        seekBar.setEnabled(true);
                        seekBar.setMax((int) (playTime/1000));
                        recordTime = 0-barWidth;
                        timerFlag =true;
                        mTimer =  new Timer();
                        ct = new CustomTimer();
                        mTimer.schedule(ct, 0, 1000);
                    }else{
                        timerFlag =false;
                        ct.cancel();
                        mTimer.cancel();
//                        chart.getXAxis().removeAllLimitLines();
//                        chart.invalidate();
                        seekBar.setEnabled(false);
                    }
                }
            }) ;

            //listView 생성
            ListView listview = (ListView) findViewById(R.id.recordListview);
            listview.setAdapter(adapter);

            // 녹음 검출리스트 추가.
            JsonArray analysisList = responseData.getAsJsonArray("analysisList");
            if(analysisList.size()>0){
                for(int i=0; i<analysisList.size(); i++){
                    try {
                        JsonObject analysisObj = (JsonObject) analysisList.get(i);

                        analysisObj.get("analysisStartDt");
                        JsonArray analysisDetailsList = analysisObj.getAsJsonArray("analysisDetailsList");
                        //초기화
                        snoreCnt=0;
                        osaCnt=0;
                        grindCnt=0;
                        recordData = new RecordData();
                        recordData.setResponseObj(analysisList.get(i).toString());
                        if(i==0){
                            firstData = analysisList.get(i).toString();
                        }
                        recordData.setAnalysisFileNm(jncu.JsonStringNullCheck(analysisObj,"analysisFileNm"));
                        recordData.setAnalysisFileAppPath(jncu.JsonStringNullCheck(analysisObj,"analysisFileAppPath"));
                        recordData.setAnalysisId(analysisObj.get("analysisId").getAsInt());
                        recordData.setAnalysisStartDt(jncu.JsonStringNullCheck(analysisObj,"analysisStartDt"));
                        recordData.setAnalysisEndDt(jncu.JsonStringNullCheck(analysisObj,"analysisEndDt"));
                        recordData.setAnalysisEndDt(jncu.JsonStringNullCheck(analysisObj,"analysisEndDt"));
                        analysisId = analysisObj.get("analysisId").getAsInt();
                        analysisServerUploadPath = jncu.JsonStringNullCheck(analysisObj,"analysisFileAppPath")+"/"+jncu.JsonStringNullCheck(analysisObj,"analysisFileNm");

                        JsonObject analysisPathList = new JsonObject();
                        analysisPathList.addProperty("analysisId",analysisId);
                        analysisPathList.addProperty("analysisServerUploadPath",analysisServerUploadPath);
                        analysisFilePathList.add(analysisPathList);


                        Date analysisStartDt =  stringtoDateTimeFormat.parse(recordData.getAnalysisStartDt());
                        Date analysisEndDt = stringtoDateTimeFormat.parse(recordData.getAnalysisEndDt());
                        JsonObject recordEntryData = new JsonObject();
                        recordEntryData.addProperty("filePath",recordData.getAnalysisFileAppPath()+"/"+recordData.getAnalysisFileNm());
                        //그래프 클릭 시에 구간만 재생하는
//                                recordEntryData.addProperty("termStartDt",termStartDt.getTime()-analysisStartDt.getTime());
//                                recordEntryData.addProperty("termEndDt",termEndDt.getTime()-analysisStartDt.getTime());
                        //그래프 클릭 시에 전체 재생
                        recordEntryData.addProperty("termStartDt",0);
                        recordEntryData.addProperty("termEndDt",analysisEndDt.getTime()-analysisStartDt.getTime());
                        recordEntryData.addProperty("analysisId",recordData.getAnalysisId());

                        if(analysisObj.has("recordingData")){
                            String analysisDataRawStr =analysisObj.get("recordingData").getAsString();
                            JsonArray analysisDataArr = new JsonParser().parse(analysisDataRawStr).getAsJsonArray();

                            JsonArray analysisDataArrGroupByMinite =rd.groupByMinites(analysisDataArr,jncu.JsonStringNullCheck(analysisObj,"analysisStartDt"));
                            if(analysisDataArrGroupByMinite.size()>0){
                                for(int k =0; k<analysisDataArrGroupByMinite.size(); k++){
                                    JsonObject analysisRawData = (JsonObject) analysisDataArrGroupByMinite.get(k);
                                    int time = analysisRawData.get("TIME").getAsInt();
                                    soundEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                }
                            }
                        }

                        if(analysisDetailsList.size()>0){
                            for(int j=0; j<analysisDetailsList.size(); j++){
                                JsonObject analysisDetailsObj = (JsonObject) analysisDetailsList.get(j);
                                int termTypeCd =  analysisDetailsObj.get("termTypeCd").getAsInt();
                                recordData.setTermStartDt(analysisDetailsObj.get("termStartDt").toString().replace("\"",""));
                                recordData.setTermEndDt(analysisDetailsObj.get("termEndDt").toString().replace("\"",""));
                                if(termTypeCd==200101){
                                    snoreCnt++;
                                }else if(termTypeCd==200102){
                                    grindCnt++;
                                }else{
                                    osaCnt++;
                                }
                                //데이터를 넣을때는 발생시간 - 최초시간을 넣어야함

                                if(analysisDetailsObj.has("analysisData")){
                                    String analysisRawStr =analysisDetailsObj.get("analysisData").getAsString();
//                                    System.out.println("=================analysisRawStr======================"+analysisRawStr);
                                    JsonArray analysisRawDataArr = new JsonParser().parse(analysisRawStr).getAsJsonArray();

                                    if(analysisRawDataArr.size()>0){
                                        JsonArray analysisRawDataArrGroupByMinite =rd.groupByMinites(analysisRawDataArr,jncu.JsonStringNullCheck(analysisObj,"analysisStartDt"));

                                        for(int k =0; k<analysisRawDataArrGroupByMinite.size(); k++){
                                            JsonObject analysisRawData = (JsonObject) analysisRawDataArrGroupByMinite.get(k);
                                            int time = analysisRawData.get("TIME").getAsInt();

                                            if(termTypeCd==200101){
                                                //                                                    snoreEntries.add(new BarEntry(minute*60*1000, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                                snoreEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                            }else if(termTypeCd==200102){
                                                //                                                    grindEntries.add(new BarEntry(minute*60*1000, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                                grindEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                            }else{
                                                //                                                    osaEntries.add(new BarEntry(minute*60*1000, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                                osaEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                            }
                                        }
                                    }
                                }

                                kococoTerm += (stringtoDateTimeFormat.parse(recordData.getTermEndDt()).getTime()
                                        -stringtoDateTimeFormat.parse(recordData.getTermStartDt()).getTime());
                            }
                        }
                        String detectedTxt = "";
//                        if(snoreCnt>0){
//                            detectedTxt = detectedTxt+"코골이가 "+snoreCnt+"회";
//                        }
                        if(osaCnt!=0||grindCnt!=0||snoreCnt!=0){
                            detectedTxt+="(";
                            if(grindCnt>0){
    //                            if(snoreCnt>0){
    //                                detectedTxt = detectedTxt+", ";
    //                            }
                                detectedTxt = detectedTxt+"이갈이 "+grindCnt+"회";
                            }
                            if(osaCnt>0){
    //                            if(grindCnt>0||snoreCnt>0){
                                if(grindCnt>0){
                                    detectedTxt = detectedTxt+", ";
                                }
                                detectedTxt = detectedTxt+"무호흡 "+osaCnt+"회";
                            }
                            detectedTxt+=")";
                        }
                        if("".equals(detectedTxt)){
                            recordData.setTitle(
                                    df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisStartDt())
                                            +"부터 "+ df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisEndDt())+"까지 "
                                            +df.longToStringFormat(((stringtoDateTimeFormat.parse(recordData.getAnalysisEndDt()).getTime()
                                            -stringtoDateTimeFormat.parse(recordData.getAnalysisStartDt()).getTime())))+"동안\n"
                                            +"소음이 감지되었습니다."
                            );
                        }else if(osaCnt==0&&grindCnt==0&&snoreCnt!=0){
                            recordData.setTitle(
                                    df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisStartDt())
                                            +"부터 "+ df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisEndDt())+"까지 "
                                            +df.longToStringFormat(((stringtoDateTimeFormat.parse(recordData.getAnalysisEndDt()).getTime()
                                            -stringtoDateTimeFormat.parse(recordData.getAnalysisStartDt()).getTime())))+"동안\n"
                                            +"코골이가 발생했습니다. ");
                        }else{
                            recordData.setTitle(
                                    df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisStartDt())
                                            +"부터 "+ df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisEndDt())+"까지 "
                                            +df.longToStringFormat(((stringtoDateTimeFormat.parse(recordData.getAnalysisEndDt()).getTime()
                                            -stringtoDateTimeFormat.parse(recordData.getAnalysisStartDt()).getTime())))+"동안\n"
//                                            +detectedTxt +"\n발생했습니다. "
                                            +"코골이가 발생했습니다. \n"+detectedTxt
                            );
                        }
                        //~시~분~초부터 ~시~분~초까지 코를 골았습니다. \n (무호흡 0회, )
                        adapter.addItem(recordData) ;

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                TextView nullTextView = (TextView) findViewById(R.id.nullTextView);
                nullTextView.setText("코를 골지 않으셨어요!!\n건강한 수면을 하셨네요!");
            }

            //점수 구하는법
            //공식은 전체 녹음시간 분의 검출된 시간으로 퍼센트로 구한다.
            sleepScore=100-(((float)kococoTerm/(float)recordTerm)*100);
            TextView scoreTextView = findViewById(R.id.scoreTextView);
            //i18n 적용
            Resources res = getResources();
            String scoreText = String.format(res.getString(R.string.score),Math.round(sleepScore));
            scoreTextView.setText(scoreText);

            //차트 시작
            chart = findViewById(R.id.chart1);
            chart.getDescription().setEnabled(false);
            // if more than 60 snoreEntries are displayed in the chart, no values will be
            // drawn
//            chart.setMaxVisibleValueCount(60);

            // scaling can now only be done on x- and y-axis separately
            chart.setPinchZoom(false);


//            chart.setDrawBarShadow(false);
            chart.setDrawGridBackground(false);
            //터치를 제한시키는 부분인데 값을 어떻게 쓰는지를 모르겠음
//            chart.setMaxHighlightDistance (1.5f);
//            chart.setHighlightFullBarEnabled(true);

            chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
//                    System.out.println("=========클릭했다~~~"+e.getData());
                    if(adapter.getPlayBtnFlag()){
                        seekBar.setEnabled(false);
                        adapter.stopActivityMp(playingId);
                        timerFlag =false;
                        ct.cancel();
                        mTimer.cancel();
//                        chart.getXAxis().removeAllLimitLines();
//                        chart.invalidate();
                    }
                    //데이터 넣을 시에 재생 파일 패스 및 재생 구간을 넣으면된다.
                    if(e.getData()!=null){
                        JsonObject graphData= (JsonObject) e.getData();
                        if(!"/".equals(graphData.get("filePath").getAsString())){
                            seekBar.setEnabled(true);
                            playingId = graphData.get("analysisId").getAsInt();
                            recordTime = ((graphData.get("termStartDt").getAsInt())/(60*1000))-barWidth;
                            timerFlag =true;
                            mTimer =  new Timer();
                            ct = new CustomTimer();
                            mTimer.schedule(ct, 0, 1000);
                            try {
                                adapter.playActivityMp(graphData.get("analysisId").getAsInt(),graphData.get("termStartDt").getAsInt(),graphData.get("termEndDt").getAsInt(),graphData.get("filePath").getAsString(),getApplicationContext());
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }else{
                            Toast.makeText(getApplicationContext(),"파일이 존재하지않습니다.",Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                @Override
                public void onNothingSelected() {
                    if(adapter.getPlayBtnFlag()){
                        seekBar.setEnabled(false);
//                        seekBar.setProgress(0);
                        adapter.stopActivityMp(playingId);
                        timerFlag =false;
                        ct.cancel();
                        mTimer.cancel();
                        chart.getXAxis().removeAllLimitLines();
                        chart.invalidate();
                    }
                }
            });

            MyXAxisValueFormatter xAxisFormatter = new MyXAxisValueFormatter(referenceTimestamp);
            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            //데이터의 분할 정도
//            xAxis.setGranularity(6000f);
            //데이터값 중간에 보이게
//            xAxis.setCenterAxisLabels(true);
            xAxis.setDrawGridLines(false);
            //X좌표 폰트
            xAxis.setTextColor(Color.WHITE);
            xAxis.setValueFormatter(xAxisFormatter);
            xAxis.setXOffset(1f);
            xAxis.setLabelCount(2);

            //시작과 끝점을 정해줘야 그래프가 제대로잘 나옴
            float endMinites = (recordEndDT.getTime()-referenceTimestamp)/(60*1000);
            xAxis.setAxisMinimum(0);
            xAxis.setAxisMaximum(endMinites);

            chart.setScaleYEnabled(false);
            chart.getAxisLeft().setDrawGridLines(false);
            //Y좌표 폰트
            chart.getAxisLeft().setTextColor(Color.WHITE);
            //Y좌표 오른쪽 안보이게 처리
            chart.getAxisRight().setEnabled(false);
            // setting data
            // add a nice and smooth animation
            chart.animateY(1500);
            //범주 보이게 할꺼냐 말꺼냐
            chart.getLegend().setEnabled(true);
            chart = (BarChart) findViewById(R.id.chart1);
            chart.setFitBars(true);

            //마지막 데이터 정제
            emptyEntries = rd.addZeroData(0,endMinites,soundEntries,snoreEntries,osaEntries,grindEntries);
            chart.getAxisLeft().setAxisMinimum(0f);

            BarDataSet set1;
            set1 = new BarDataSet(snoreEntries, "코골이");
            set1.setColors(Color.rgb(255, 104, 89));
            //값을 보여줄건지 말건지 여부
            set1.setDrawValues(false);
            set1.setValueTextColor(Color.WHITE);
            set1.setHighLightColor(Color.rgb(255, 104, 89));

            BarDataSet grindSet;
            grindSet = new BarDataSet(grindEntries, "이갈이");
            grindSet.setColors(Color.rgb(255, 207, 68));
            //값을 보여줄건지 말건지 여부
            grindSet.setDrawValues(false);
            grindSet.setValueTextColor(Color.WHITE);
            grindSet.setHighLightColor(Color.rgb(255, 207, 68));

            BarDataSet osaSet;
            osaSet = new BarDataSet(osaEntries, "무호흡");
            osaSet.setColors(Color.rgb(177, 93, 255));
            //값을 보여줄건지 말건지 여부
            osaSet.setDrawValues(false);
            osaSet.setHighLightColor(Color.rgb(177, 93, 255));

            BarDataSet soundSet;
            soundSet = new BarDataSet(soundEntries, "소리");
            soundSet.setColors(Color.rgb(123, 109, 93));
            //값을 보여줄건지 말건지 여부
            soundSet.setDrawValues(false);
            soundSet.setValueTextColor(Color.WHITE);
            soundSet.setHighLightColor(Color.rgb(123, 109, 93));

            BarDataSet emptySet;
            emptySet = new BarDataSet(emptyEntries, "Data Set");
            emptySet.setColors(Color.TRANSPARENT);
            //값을 보여줄건지 말건지 여부
            emptySet.setDrawValues(false);
            emptySet.setValueTextColor(Color.WHITE);
            emptySet.setVisible(false);
            emptySet.setHighLightColor(Color.TRANSPARENT);

            chart.getLegend().setTextColor(Color.WHITE);
            chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
            chart.getLegend().setDrawInside(false);
//            chart.setViewPortOffsets(0f, 60f, 30f, 0f);
            //X 간격을 조정 할 수있지만 줌 기능이 정지됨
//            chart.setVisibleXRange(0f,30000f);
//            chart.zoom(chart.getScaleX(),chart.getScaleY(),0f,30000f);

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(soundSet);
            dataSets.add(set1);
            dataSets.add(grindSet);
            dataSets.add(osaSet);
            dataSets.add(emptySet);

            BarData data = new BarData(dataSets);
            //바의 두께를 바꿀수 있는
            chart.setFitBars(true);
//            data.setBarWidth(0.5f);

//            chart.getBarData().setBarWidth(800f);
//            chart.groupBars(0,32f ,12f);
            chart.setData(data);
            chart.invalidate();
//            FloatingActionButton consultBtn = (FloatingActionButton) findViewById(R.id.consultBtn);
            Button consultBtn = (Button) findViewById(R.id.consultBtn);
//            consultBtn.setOnClickListener(new FloatingActionButton.OnClickListener() {
            consultBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    apiService.getProfile(fau.getAppid(getApplicationContext())).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            System.out.println(" ===========getProfile=============response: "+response.body());
                            JsonObject result = response.body();
                            if(result.has("userAge")){
                                //프로필 있음 전문가 상담하기 페이지로 이동
                                Intent intent = new Intent(v.getContext(), ConsultActivity.class);
                                intent.putExtra("responseData",response.body().toString()); /*송신*/
                                intent.putExtra("analysisId",analysisId); /*송신*/
                                intent.putExtra("recordId",recordId); /*송신*/
                                intent.putExtra("analysisList",analysisFilePathList.toString()); /*송신*/
                                intent.putExtra("headerTextDate", headerTextDate);
                                v.getContext().startActivity(intent);
                            }else{
                                //프로필 없음 프로필페이지로 이동
                                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                                v.getContext().startActivity(intent);
                                Toast.makeText(getApplicationContext(),"전문가와 상담은 프로필이 필요합니다.",Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            System.out.println(" =============getProfile===========Throwable: "+t.getMessage());
                        }
                    });
                }
            });

            //seekbar start
            seekBar = (SeekBar)findViewById(R.id.seekbar);
            seekBar.setMax(256*7-1);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    timerFlag=false;
                    ct.cancel();
                    mTimer.cancel();
                    adapter.getMediaPlayer().pause();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seekBar.getProgress();
                    adapter.getMediaPlayer().seekTo(seekBar.getProgress()*1000);
                    adapter.getMediaPlayer().start();
                    mTimer =  new Timer();
                    ct = new CustomTimer();
                    mTimer.schedule(ct, 0, 1000);
                    timerFlag=true;
                }
            });

            //재생버튼 누를 때 까지 못 움직이게 한다.
            seekBar.setEnabled(false);

        }else{
            Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_LONG);
        }
        if(firstData!=null){
            clickFirstBtn(firstData);
        }
    }

    private void clickFirstBtn(String firstData) {
        RecordData firstDt = new RecordData();
        firstDt.setResponseObj(firstData);
        changeGraph(firstDt);
    }

    private void changeGraph(RecordData listViewItem) {
        soundEntries = new ArrayList<>();
        snoreEntries = new ArrayList<>();
        osaEntries = new ArrayList<>();
        grindEntries = new ArrayList<>();
        emptyEntries = new ArrayList<>();
        Date analysisStartDt = null,analysisEndDt = null;
        try {
            JsonObject analysisObj = new JsonParser().parse(listViewItem.getResponseObj()).getAsJsonObject();
            JsonArray analysisDetailsList = analysisObj.getAsJsonArray("analysisDetailsList");

            analysisStartDt =  stringtoDateTimeFormat.parse(jncu.JsonStringNullCheck(analysisObj,"analysisStartDt"));
            analysisEndDt = stringtoDateTimeFormat.parse(jncu.JsonStringNullCheck(analysisObj,"analysisEndDt"));

            String filePath = jncu.JsonStringNullCheck(analysisObj,"analysisFileAppPath")+"/"+jncu.JsonStringNullCheck(analysisObj,"analysisFileNm");
            long termEndDt = analysisEndDt.getTime()-analysisStartDt.getTime();
            String analysisId = jncu.JsonStringNullCheck(analysisObj,"analysisId");

            if(analysisObj.has("recordingData")){
                String analysisDataRawStr =analysisObj.get("recordingData").getAsString();
                JsonArray analysisDataArr = new JsonParser().parse(analysisDataRawStr).getAsJsonArray();
                JsonArray analysisDataArrGroupByMinite =rd.groupByMinitesInAnalysisRange(analysisDataArr,recordStartDT,analysisStartDt);
                if(analysisDataArrGroupByMinite.size()>0){
                    for(int k =0; k<analysisDataArrGroupByMinite.size(); k++){
                        JsonObject analysisRawData = (JsonObject) analysisDataArrGroupByMinite.get(k);
                        int time = analysisRawData.get("TIME").getAsInt();

                        JsonObject recordEntryData = new JsonObject();
                        recordEntryData.addProperty("filePath",filePath);
                        //그래프 클릭 시에 전체 재생
                        recordEntryData.addProperty("termStartDt",time*60*1000);
                        recordEntryData.addProperty("termEndDt",termEndDt);
                        recordEntryData.addProperty("analysisId",analysisId);

                        soundEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                    }
                }
            }

            if(analysisDetailsList.size()>0){
                for(int j=0; j<analysisDetailsList.size(); j++){
                    JsonObject analysisDetailsObj = (JsonObject) analysisDetailsList.get(j);
                    int termTypeCd =  analysisDetailsObj.get("termTypeCd").getAsInt();
                    //데이터를 넣을때는 발생시간 - 최초시간을 넣어야함
                    if(analysisDetailsObj.has("analysisData")){
                        String analysisRawStr =analysisDetailsObj.get("analysisData").getAsString();
                        JsonArray analysisRawDataArr = new JsonParser().parse(analysisRawStr).getAsJsonArray();

                        if(analysisRawDataArr.size()>0){
                            JsonArray analysisRawDataArrGroupByMinite =rd.groupByMinitesInAnalysisRange(analysisRawDataArr,recordStartDT,analysisStartDt);

                            for(int k =0; k<analysisRawDataArrGroupByMinite.size(); k++){
                                JsonObject analysisRawData = (JsonObject) analysisRawDataArrGroupByMinite.get(k);
                                int time = analysisRawData.get("TIME").getAsInt();
                                //x 축이 기준이 10:10이고   10:11분에 그려져있으면 1분후의 재생을 해야함

                                JsonObject recordEntryData = new JsonObject();
                                recordEntryData.addProperty("filePath",filePath);
                                //그래프 클릭 시에 전체 재생
                                recordEntryData.addProperty("termStartDt",time*60*1000);
                                recordEntryData.addProperty("termEndDt",termEndDt);
                                recordEntryData.addProperty("analysisId",analysisId);

                                if(termTypeCd==200101){
                                    snoreEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                }else if(termTypeCd==200102){
                                    grindEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                }else{
                                    osaEntries.add(new BarEntry(time, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                }
                            }
                        }
                    }
                }
            }
//            Long startMinites = (analysisStartDt.getTime()-referenceTimestamp)/(60*1000);
//            Long endMinites = (analysisEndDt.getTime()-referenceTimestamp)/(60*1000)+1;

            //X 좌표 보여주기
//            chart.getXAxis().setAxisMinimum(startMinites);
//            chart.getXAxis().setAxisMaximum(endMinites);

        } catch (ParseException e) {
            e.printStackTrace();
        }



        MyXAxisValueFormatter xAxisFormatter = new MyXAxisValueFormatter(analysisStartDt.getTime());
        XAxis xAxis = chart.getXAxis();
        float endMinites = (analysisEndDt.getTime()-analysisStartDt.getTime())/(60*1000);
        int startMinites=0;

        if(endMinites<=2){
            startMinites-=1;
            endMinites+=2;
        }else{
            endMinites+=1;
        }
//        xAxis.setAxisMinimum(startMinites);
//        xAxis.setAxisMaximum(endMinites);
        xAxis.setValueFormatter(xAxisFormatter);
        //마지막 데이터 정제
        emptyEntries = rd.addZeroData(startMinites,endMinites,soundEntries,snoreEntries,osaEntries,grindEntries);

        BarDataSet snoreSet;
        snoreSet = new BarDataSet(snoreEntries, "코골이");
        snoreSet.setColors(Color.rgb(255, 104, 89));
        snoreSet.setDrawValues(false);
        snoreSet.setValueTextColor(Color.WHITE);
        snoreSet.setBarBorderColor(Color.rgb(55,55,65));
        snoreSet.setBarBorderWidth(1f);

        BarDataSet grindSet;
        grindSet = new BarDataSet(grindEntries, "이갈이");
        grindSet.setColors(Color.rgb(255, 207, 68));
        grindSet.setDrawValues(false);
        grindSet.setValueTextColor(Color.WHITE);
        grindSet.setBarBorderColor(Color.rgb(55,55,65));
        grindSet.setBarBorderWidth(1f);

        BarDataSet osaSet;
        osaSet = new BarDataSet(osaEntries, "무호흡");
        osaSet.setColors(Color.rgb(177, 93, 255));
        osaSet.setDrawValues(false);
        osaSet.setValueTextColor(Color.WHITE);
        osaSet.setBarBorderColor(Color.rgb(55,55,65));
        osaSet.setBarBorderWidth(1f);

        BarDataSet soundSet;
        soundSet = new BarDataSet(soundEntries, "소음");
        soundSet.setColors(Color.rgb(123, 109, 93));
        soundSet.setDrawValues(false);
        soundSet.setValueTextColor(Color.WHITE);
        soundSet.setBarBorderColor(Color.rgb(55,55,65));
        soundSet.setBarBorderWidth(1f);

        //하이라이트 못하게 하는 법 BarDataSet emptySet.setHighlightEnabled(false); 이지만 클릭이벤트 기능을 써야되기에 하이라이트 색을 안바뀌게 해야함
        snoreSet.setHighLightColor(Color.rgb(255, 104, 89));
        grindSet.setHighLightColor(Color.rgb(255, 207, 68));
        osaSet.setHighLightColor(Color.rgb(177, 93, 255));
        soundSet.setHighLightColor(Color.rgb(123, 109, 93));

        BarDataSet emptySet;
        emptySet = new BarDataSet(emptyEntries, "Data Set");
        emptySet.setColors(Color.TRANSPARENT);
        //값을 보여줄건지 말건지 여부
        emptySet.setDrawValues(false);
        emptySet.setValueTextColor(Color.WHITE);
        emptySet.setVisible(true);
        emptySet.setHighLightColor(Color.TRANSPARENT);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(soundSet);
        dataSets.add(snoreSet);
        dataSets.add(grindSet);
        dataSets.add(osaSet);
        dataSets.add(emptySet);

        BarData data = new BarData(dataSets);


        setLegend(chart);


        //바를 양옆이 반으로 짤리는 현상을 수정하는 방법
        chart.setFitBars(true);
        //default 가 0.9f란다 그러니까 1f 하면 꽉찬다.
        data.setBarWidth(1f);
        barWidth = data.getBarWidth()/2;
        chart.getXAxis().setAxisMinimum(-barWidth);
        chart.getXAxis().setAxisMaximum(emptyEntries.size()-barWidth);

        chart.setData(data);
        chart.invalidate();
    }

    //getSupportActionBar 사용하려면 추가해야함
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);
    }

    // MediaPlayer는 시스템 리소스를 잡아먹는다.
    // MediaPlayer는 필요이상으로 사용하지 않도록 주의해야 한다.
    //Fragment에서는 onDestoryView , activity에서는 onDestory
    @Override
    public void onDestroy() {
        if(timerFlag){
            ct.cancel();
            mTimer.cancel();
        }
        adapter.destroyMp();
        super.onDestroy();
    }

    public class CustomTimer extends TimerTask {
        Integer currentPosition;
        @Override
        public void run() {
//            recordTime+= (float) (1/60.00);
//            System.out.println("=========recordTime============="+recordTime);
//            LimitLine ll1 = new LimitLine(recordTime);
//            ll1.setLineWidth(1f);
//            //점선으로 그릴 경우
////                    ll1.enableDashedLine(10f, 10f, 0f);
//            //라벨 위치
////                    ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
////                        ll1.setTextSize(10f);
//            chart.getXAxis().removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
//            chart.getXAxis().addLimitLine(ll1);
//            chart.invalidate();

//            while(isPlaying) {
//                sb.setProgress(mp.getCurrentPosition());
//            }
            if(timerFlag){
                if(adapter.getMediaPlayer()!=null){
                    currentPosition = adapter.getMediaPlayer().getCurrentPosition();
                    if(currentPosition != null){
                        seekBar.setProgress(adapter.getMediaPlayer().getCurrentPosition()/1000);
                    }
                }
//                System.out.println("====재생중===="+adapter.getMediaPlayer().getCurrentPosition()/1000);
            }

        }

    }

    //범주를 설정해주는 곳
    public void setLegend(BarChart chart){

        LegendEntry soundLe = new LegendEntry();
        soundLe.label="소음";
        soundLe.formColor=Color.rgb(123, 109, 93);
        LegendEntry snoreLe = new LegendEntry();
        snoreLe.label="코골이";
        snoreLe.formColor=Color.rgb(255, 104, 89);
        LegendEntry grindLe = new LegendEntry();
        grindLe.label="이갈이";
        grindLe.formColor=Color.rgb(255, 207, 68);
        LegendEntry osaLe = new LegendEntry();
        osaLe.label="무호흡";
        osaLe.formColor=Color.rgb(177, 93, 255);


        ArrayList<LegendEntry> legendSets = new ArrayList<>();
        legendSets.add(soundLe);
        legendSets.add(snoreLe);
        legendSets.add(grindLe);
        legendSets.add(osaLe);
        //범주 데이터 차트에 넣기
        chart.getLegend().setCustom(legendSets);
        //범주 크기 키우기
        chart.getLegend().setFormSize(10f);
        chart.getLegend().setTextSize(13f);
    }
}
