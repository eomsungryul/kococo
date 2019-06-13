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
import android.support.design.widget.FloatingActionButton;
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
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.adapter.RecordListAdapter;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.DateFormatter;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import kr.co.dwebss.kococo.util.JsonNullCheckUtil;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;
import kr.co.dwebss.kococo.util.MyXAxisValueFormatter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ResultActivity extends AppCompatActivity implements OnSeekBarChangeListener {
    Retrofit retrofit;
    ApiService apiService;

    private static final String TAG = "ResultActivity";
    private BarChart chart;

    JsonObject responseData;

    Date recordStartD;
    Date recordEndD;
    Date recordStartDT;
    Date recordEndDT;
    Long recordTerm;

    //검출된 시간 초기화
    Long kococoTerm =0L;
    double sleepScore;

    RecordData recordData;
    JsonNullCheckUtil jncu = new JsonNullCheckUtil();;

    FindAppIdUtil fau = new FindAppIdUtil();

    List<BarEntry> entries = new ArrayList<>();
    List<BarEntry> osaEntries = new ArrayList<>();
    List<BarEntry> grindEntries = new ArrayList<>();
    long referenceTimestamp; // minimum timestamp in your data set;

    MediaPlayerUtility mpu;
    DateFormatter df = new DateFormatter();

    int playingDetailId;

    int recordId;
    int analysisId;
    String analysisServerUploadPath;

    int snoreCnt=0;
    int osaCnt=0;
    int grindCnt=0;

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

        //데이터 수신
        Intent intent = getIntent();
        if(getIntent().hasExtra("responseData")){
            responseData = new JsonParser().parse(getIntent().getStringExtra("responseData")).getAsJsonObject();

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
            Date from = new Date();
            SimpleDateFormat stringtoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat transFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
            SimpleDateFormat stringtoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat DateTimeToStringFormat = new SimpleDateFormat("HH:mm");
            SimpleDateFormat DateSecondToStringFormat = new SimpleDateFormat("HH:mm:ss");
            try {
                recordStartD =  stringtoDateFormat.parse(responseData.get("recordStartD").toString().replace("\"",""));
                recordEndD =  stringtoDateFormat.parse(responseData.get("recordEndD").toString().replace("\"",""));

                recordStartDT =  stringtoDateTimeFormat.parse(responseData.get("recordStartDt").toString().replace("\"",""));
                recordEndDT =  stringtoDateTimeFormat.parse(responseData.get("recordEndDt").toString().replace("\"",""));
                recordTerm = recordEndDT.getTime()-recordStartDT.getTime();

                recordId = responseData.get("recordId").getAsInt();

                referenceTimestamp = recordStartDT.getTime();

            } catch (ParseException e) {
                e.printStackTrace();
            }

            if(recordStartD.equals(recordEndD)){
                dateTxtHeader.setText(transFormat.format(recordStartD));
            }else{
                dateTxtHeader.setText(transFormat.format(recordStartD)+"~"+transFormat.format(recordEndD));
            }

            // 하단에 녹음 검출리스트 파일 리스트  Adapter 생성
            RecordListAdapter adapter = new RecordListAdapter(this, new RecordListAdapter.GraphClickListener() {
                @Override
                public void clickBtn() {
                    System.out.println("===============된다...");
                }
            }) ;


            //listView 생성
            ListView listview = (ListView) findViewById(R.id.recordListview);
            listview.setAdapter(adapter);

            // 녹음 검출리스트 추가.
            JsonArray analysisList = responseData.getAsJsonArray("analysisList");

            if(analysisList.size()>0){
                for(int i=0; i<analysisList.size(); i++){
                    JsonObject analysisObj = (JsonObject) analysisList.get(i);
                    analysisObj.get("analysisStartDt");
                    JsonArray analysisDetailsList = analysisObj.getAsJsonArray("analysisDetailsList");
                    //초기화
                    snoreCnt=0;
                    osaCnt=0;
                    grindCnt=0;
                    if(analysisDetailsList.size()>0){
                        for(int j=0; j<analysisDetailsList.size(); j++){
                            recordData = new RecordData();
                            recordData.setAnalysisFileNm(jncu.JsonStringNullCheck(analysisObj,"analysisFileNm"));
                            recordData.setAnalysisFileAppPath(jncu.JsonStringNullCheck(analysisObj,"analysisFileAppPath"));
                            recordData.setAnalysisId(analysisObj.get("analysisId").getAsInt());
                            recordData.setAnalysisStartDt(jncu.JsonStringNullCheck(analysisObj,"analysisStartDt"));
                            recordData.setAnalysisEndDt(jncu.JsonStringNullCheck(analysisObj,"analysisEndDt"));

                            analysisId = analysisObj.get("analysisId").getAsInt();
                            analysisServerUploadPath = jncu.JsonStringNullCheck(analysisObj,"analysisFileAppPath")+"/"+jncu.JsonStringNullCheck(analysisObj,"analysisFileNm");

                            JsonObject analysisDetailsObj = (JsonObject) analysisDetailsList.get(j);
                            System.out.println("================analysisDetailsObj==========================="+analysisDetailsObj);
                            int termTypeCd =  analysisDetailsObj.get("termTypeCd").getAsInt();
                            recordData.setAnalysisDetailsId(analysisDetailsObj.get("analysisDetailsId").getAsInt());
                            recordData.setTermStartDt(analysisDetailsObj.get("termStartDt").toString().replace("\"",""));
                            recordData.setTermEndDt(analysisDetailsObj.get("termEndDt").toString().replace("\"",""));
                            recordData.setTermTypeCd(termTypeCd);
                            //증상 시작시간(HH:mm:ss) ~ 종료시간(HH:mm:ss) 으로 표기
                            try {
                            if(termTypeCd==200101){
//                                recordData.setTitle("코골이 : "+df.returnStringISO8601ToHHmmssFormat(recordData.getTermStartDt())
//                                        +" ~ "+ df.returnStringISO8601ToHHmmssFormat(recordData.getTermEndDt()));
                                snoreCnt++;
                            }else if(termTypeCd==200102){
//                                recordData.setTitle("이갈이 : "+df.returnStringISO8601ToHHmmssFormat(recordData.getTermStartDt())
//                                        +" ~ "+ df.returnStringISO8601ToHHmmssFormat(recordData.getTermEndDt()));
                                grindCnt++;
                            }else{
//                                recordData.setTitle("무호흡 : "+df.returnStringISO8601ToHHmmssFormat(recordData.getTermStartDt())
//                                        +" ~ "+ df.returnStringISO8601ToHHmmssFormat(recordData.getTermEndDt()));
                                osaCnt++;
                            }
//                                adapter.addItem(recordData) ;
                            //데이터를 넣을때는 발생시간 - 최초시간을 넣어야함
                            long date = 0;
                                date = stringtoDateTimeFormat.parse(recordData.getTermStartDt()).getTime();

                                //데이터 넣을 시에 재생 파일 패스 및 재생 구간을 넣으면된다.
                                JsonObject recordEntryData = new JsonObject();
                                Date analysisStartDt =  stringtoDateTimeFormat.parse(recordData.getAnalysisStartDt());
                                Date analysisEndDt =  stringtoDateTimeFormat.parse(recordData.getAnalysisEndDt());
                                Date termStartDt =  stringtoDateTimeFormat.parse(recordData.getTermStartDt());
                                Date termEndDt =  stringtoDateTimeFormat.parse(recordData.getTermEndDt());

                                recordEntryData.addProperty("filePath",recordData.getAnalysisFileAppPath()+"/"+recordData.getAnalysisFileNm());
                                //그래프 클릭 시에 구간만 재생하는
//                                recordEntryData.addProperty("termStartDt",termStartDt.getTime()-analysisStartDt.getTime());
//                                recordEntryData.addProperty("termEndDt",termEndDt.getTime()-analysisStartDt.getTime());
                                //그래프 클릭 시에 전체 재생
                                recordEntryData.addProperty("termStartDt",0);
                                recordEntryData.addProperty("termEndDt",analysisEndDt.getTime()-analysisStartDt.getTime());
                                recordEntryData.addProperty("analysisDetailsId",recordData.getAnalysisDetailsId());
//                                System.out.println("=================analysisDetailsObj=======recordEntryData================"+recordEntryData);
                                if(analysisDetailsObj.has("analysisData")){
                                    String analysisRawStr =analysisDetailsObj.get("analysisData").getAsString();
//                                    System.out.println("=================analysisRawStr======================"+analysisRawStr);
                                    JsonArray analysisRawDataArr = new JsonParser().parse(analysisRawStr).getAsJsonArray();
                                    if(analysisRawDataArr.size()>0){
                                        for(int k =0; k<analysisRawDataArr.size(); k++){
                                            JsonObject analysisRawData = (JsonObject) analysisRawDataArr.get(k);
                                            //데이터 넣는 부분
                                            //타임은 recordStartDt 이후의 시간

//                                                if(termTypeCd==200101){
//                                                    entries.add(new BarEntry((analysisRawData.get("TIME").getAsInt()*1000), analysisRawData.get("DB").getAsFloat(), recordEntryData));
//                                                }else if(termTypeCd==200102){
//                                                    grindEntries.add(new BarEntry((analysisRawData.get("TIME").getAsInt()*1000), analysisRawData.get("DB").getAsFloat(), recordEntryData));
//                                                }else{
//                                                    osaEntries.add(new BarEntry((analysisRawData.get("TIME").getAsInt()*1000), analysisRawData.get("DB").getAsFloat(), recordEntryData));
//                                                }

                                                int time = analysisRawData.get("TIME").getAsInt();
                                                int minute =time/60;

                                                if(termTypeCd==200101){
                                                    entries.add(new BarEntry(minute*60*1000, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                                }else if(termTypeCd==200102){
                                                    grindEntries.add(new BarEntry(minute*60*1000, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                                }else{
                                                    osaEntries.add(new BarEntry(minute*60*1000, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                                }

//                                            entries.add(new BarEntry((analysisRawData.get("TIME").getAsInt()*1000), analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                        }
                                    }
                                }

                                kococoTerm += (stringtoDateTimeFormat.parse(recordData.getTermEndDt()).getTime()
                                        -stringtoDateTimeFormat.parse(recordData.getTermStartDt()).getTime());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                    }
                        recordData.setTitle("코골이 : "+snoreCnt+"회"+"이갈이 : "+grindCnt+"회"+"무호흡 : "+osaCnt+"회 /"
                                        +df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisStartDt())
                                        +" ~ "+ df.returnStringISO8601ToHHmmssFormat(recordData.getAnalysisEndDt()));
                        adapter.addItem(recordData) ;
                    }
                }
            }

            //점수 구하는법
            //공식은 전체 녹음시간 분의 검출된 시간으로 퍼센트로 구한다.
            sleepScore=100-(((float)kococoTerm/(float)recordTerm)*100);
            TextView scoreTextView = findViewById(R.id.scoreTextView);
            //i18n 적용
            Resources res = getResources();
            String scoreText = String.format(res.getString(R.string.score),Math.round(sleepScore));
            scoreTextView.setText(scoreText);

            //시간 HH:mm ~ HH:mm
            String recodeText = DateTimeToStringFormat.format(recordStartDT)+"~"+DateTimeToStringFormat.format(recordEndDT);
            TextView recodeTextView = findViewById(R.id.recodeTextView);
            recodeTextView.setText(recodeText);

            //차트 시작
            chart = findViewById(R.id.chart1);
            chart.getDescription().setEnabled(false);
            // if more than 60 entries are displayed in the chart, no values will be
            // drawn
//            chart.setMaxVisibleValueCount(60);

            // scaling can now only be done on x- and y-axis separately
            chart.setPinchZoom(false);


//            chart.setDrawBarShadow(false);
            chart.setDrawGridBackground(false);
            chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
//                    System.out.println("=========클릭했다~~~"+e.getData());
                    //데이터 넣을 시에 재생 파일 패스 및 재생 구간을 넣으면된다.
                    JsonObject graphData= (JsonObject) e.getData();
                    playingDetailId = graphData.get("analysisDetailsId").getAsInt();
                    try {
//                        adapter.playActivityMp(graphData.get("analysisDetailsId").getAsInt(),graphData.get("termStartDt").getAsInt(),graphData.get("termEndDt").getAsInt(),graphData.get("filePath").getAsString(),getApplicationContext());
                        adapter.playGraphMp(graphData.get("termStartDt").getAsInt(),graphData.get("termEndDt").getAsInt(),graphData.get("filePath").getAsString(),getApplicationContext());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                @Override
                public void onNothingSelected() {
                    if(adapter.getPlayBtnFlag()){
                        adapter.stopActivityMp(playingDetailId);
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

            //시작과 끝점을 정해줘야 그래프가 제대로잘 나옴
            xAxis.setAxisMinimum(0);
            xAxis.setAxisMaximum((float)(recordEndDT.getTime()-referenceTimestamp+(30*1000)));
            System.out.println("=========recordEndDT.getTime()-referenceTimestamp===recordTerm============"+recordTerm);

            chart.getAxisLeft().setDrawGridLines(false);
            //Y좌표 폰트
            chart.getAxisLeft().setTextColor(Color.WHITE);
            //Y좌표 오른쪽 안보이게 처리
            chart.getAxisRight().setEnabled(false);
            // setting data
            // add a nice and smooth animation
            chart.animateY(1500);
            chart.getLegend().setEnabled(false);
            chart = (BarChart) findViewById(R.id.chart1);

            BarDataSet set1;
            set1 = new BarDataSet(entries, "Data Set");
            set1.setColors(Color.rgb(255, 104, 89));
            //값을 보여줄건지 말건지 여부
            set1.setDrawValues(false);
            set1.setValueTextColor(Color.WHITE);

            BarDataSet grindSet;
            grindSet = new BarDataSet(grindEntries, "Data Set");
            grindSet.setColors(Color.rgb(255, 207, 68));
            //값을 보여줄건지 말건지 여부
            grindSet.setDrawValues(false);
            grindSet.setValueTextColor(Color.WHITE);

            BarDataSet osaSet;
            osaSet = new BarDataSet(osaEntries, "Data Set");
            osaSet.setColors(Color.rgb(177, 93, 255));
            //값을 보여줄건지 말건지 여부
            osaSet.setDrawValues(false);
            osaSet.setValueTextColor(Color.WHITE);

//            chart.setViewPortOffsets(0f, 60f, 30f, 0f);
            //X 간격을 조정 할 수있지만 줌 기능이 정지됨
//            chart.setVisibleXRange(0f,30000f);
//            chart.zoom(chart.getScaleX(),chart.getScaleY(),0f,30000f);

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            dataSets.add(grindSet);
            dataSets.add(osaSet);

            BarData data = new BarData(set1,grindSet,osaSet);
            //바의 두께를 바꿀수 있는
            data.setBarWidth(3000f);
            chart.setData(data);
//            chart.setFitBars(true);

//            chart.getBarData().setBarWidth(800f);
//            chart.groupBars(0,32f ,12f);
            chart.invalidate();

            FloatingActionButton consultBtn = (FloatingActionButton) findViewById(R.id.consultBtn);
            consultBtn.setOnClickListener(new FloatingActionButton.OnClickListener() {
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
                                intent.putExtra("analysisServerUploadPath",analysisServerUploadPath); /*송신*/
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


//
//            //뒤로가기 버튼
//            Button allBtn = (Button) findViewById(R.id.allBtn);
//            bt.setOnClickListener(new Button.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    System.out.println("==========allBtnallBtnallBtnallBtn=======");
//                    chart.setScaleMinima(0f, 0f);
//                    chart.fitScreen();
//                }
//            });


        }else{
            Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_LONG);
        }





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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        System.out.println("============onProgressChanged=============");
        ArrayList<BarEntry> values = new ArrayList<>();
        BarDataSet set1;
        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
            set1 = (BarDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            set1 = new BarDataSet(values, "Data Set");
            set1.setColors(ColorTemplate.VORDIPLOM_COLORS);
            set1.setDrawValues(false);

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            BarData data = new BarData(dataSets);
            chart.setData(data);
//            chart.setFitBars(true);
        }
        chart.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    // MediaPlayer는 시스템 리소스를 잡아먹는다.
    // MediaPlayer는 필요이상으로 사용하지 않도록 주의해야 한다.
    //Fragment에서는 onDestoryView , activity에서는 onDestory
    @Override
    public void onDestroy() {
        super.onDestroy();
//         MediaPlayer 해지
        mpu.endMp();
    }
}
