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
import android.support.v7.widget.RecyclerView;
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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
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
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.DateFormatter;
import kr.co.dwebss.kococo.util.JsonNullCheckUtil;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;
import kr.co.dwebss.kococo.util.MyXAxisValueFormatter;

public class ResultActivity extends AppCompatActivity implements OnSeekBarChangeListener {

    private static final String TAG = "ResultActivity";
    private BarChart chart;

    private RecyclerView resultListRv;
    private ListView recordLv ;
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
    JsonNullCheckUtil jncu;

    List<BarEntry> entries = new ArrayList<>();
    long referenceTimestamp; // minimum timestamp in your data set;

    MediaPlayerUtility mpu;
    DateFormatter df = new DateFormatter();

    int playingDetailId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출
        setContentView(R.layout.activity_result);
        mpu = new MediaPlayerUtility();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

//        initializeData();
        super.onCreate(savedInstanceState);
        jncu = new JsonNullCheckUtil();
        //녹음 기록 갱신
//        FragmentManager fragmentManager = getFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        Fragment fragment = fragmentManager.findFragmentById(R.id.diaryFr);
//        fragmentTransaction.remove(fragment);
//        fragmentTransaction.commit();

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
                    if(analysisDetailsList.size()>0){
                        for(int j=0; j<analysisDetailsList.size(); j++){
                            recordData = new RecordData();
                            recordData.setAnalysisFileNm(jncu.JsonStringNullCheck(analysisObj,"analysisFileNm"));
                            recordData.setAnalysisFileAppPath(jncu.JsonStringNullCheck(analysisObj,"analysisFileAppPath"));
                            recordData.setAnalysisId(analysisObj.get("analysisId").getAsInt());
                            recordData.setAnalysisStartDt(jncu.JsonStringNullCheck(analysisObj,"analysisStartDt"));
                            recordData.setAnalysisEndDt(jncu.JsonStringNullCheck(analysisObj,"analysisEndDt"));

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
                                recordData.setTitle("코골이 : "+df.returnStringISO8601ToHHmmssFormat(recordData.getTermStartDt())
                                        +" ~ "+ df.returnStringISO8601ToHHmmssFormat(recordData.getTermEndDt()));
                            }else if(termTypeCd==200102){
                                recordData.setTitle("이갈이 : "+df.returnStringISO8601ToHHmmssFormat(recordData.getTermStartDt())
                                        +" ~ "+ df.returnStringISO8601ToHHmmssFormat(recordData.getTermEndDt()));
                            }else{
                                recordData.setTitle("무호흡 : "+df.returnStringISO8601ToHHmmssFormat(recordData.getTermStartDt())
                                        +" ~ "+ df.returnStringISO8601ToHHmmssFormat(recordData.getTermEndDt()));
                            }
                            System.out.println("========================recordData===getTitle============="+recordData.getTitle());

                            adapter.addItem(recordData) ;

                            //데이터를 넣을때는 발생시간 - 최초시간을 넣어야함
                            long date = 0;
                                date = stringtoDateTimeFormat.parse(recordData.getTermStartDt()).getTime();

                                //데이터 넣을 시에 재생 파일 패스 및 재생 구간을 넣으면된다.
                                JsonObject recordEntryData = new JsonObject();
                                Date analysisStartDt =  stringtoDateTimeFormat.parse(recordData.getAnalysisStartDt());
                                Date termStartDt =  stringtoDateTimeFormat.parse(recordData.getTermStartDt());
                                Date termEndDt =  stringtoDateTimeFormat.parse(recordData.getTermEndDt());

                                recordEntryData.addProperty("filePath",recordData.getAnalysisFileAppPath()+"/"+recordData.getAnalysisFileNm());
                                recordEntryData.addProperty("termStartDt",termStartDt.getTime()-analysisStartDt.getTime());
                                recordEntryData.addProperty("termEndDt",termEndDt.getTime()-analysisStartDt.getTime());
                                recordEntryData.addProperty("analysisDetailsId",recordData.getAnalysisDetailsId());

                                if(analysisDetailsObj.has("analysisData")){
                                    String analysisRawStr =analysisDetailsObj.get("analysisData").getAsString();
                                    JsonArray analysisRawDataArr = new JsonParser().parse(analysisRawStr).getAsJsonArray();
                                    if(analysisRawDataArr.size()>0){
                                        for(int k =0; k<analysisRawDataArr.size(); k++){
                                            JsonObject analysisRawData = (JsonObject) analysisRawDataArr.get(k);
                                            //데이터 넣는 부분
                                            entries.add(new BarEntry(analysisRawData.get("TIME").getAsInt()*1000, analysisRawData.get("DB").getAsFloat(), recordEntryData));
                                        }
                                    }
                                }
                                //데이터 넣는 부분
//                                entries.add(new BarEntry((float)(date-referenceTimestamp), 50, recordEntryData));
//                                entries.add(new BarEntry((float)date, 50));


//                                {"analysisId":225,"analysisDetailsId":241,"termTypeCd":200101,"termStartDt":"2019-06-11T15:04:35","termEndDt":"2019-06-11T15:04:37","claimYn":"N","analysisData":"[{\"TIME\":\"46\",\"DB\":37.19878832839144,\"HZ\":112.8,\"AMP\":2271},{\"TIME\":\"47\",\"DB\":25.64299905835125,\"HZ\":105.3,\"AMP\":368},{\"TIME\":\"49\",\"DB\":27.264555962474574,\"HZ\":337.5,\"AMP\":422},{\"TIME\":\"50\",\"DB\":38.348498303132345,\"HZ\":1192.1,\"AMP\":4468},{\"TIME\":\"51\",\"DB\":24.559377698807996,\"HZ\":111.8,\"AMP\":1036}]"}


                                kococoTerm += (stringtoDateTimeFormat.parse(recordData.getTermEndDt()).getTime()
                                        -stringtoDateTimeFormat.parse(recordData.getTermStartDt()).getTime());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                }
            }


            //test용
//        dateTxtHeader.setText(transFormat.format(recordStartD)+"===="+recordData.getAnalysisId());


            //점수 구하는법
            //공식은 전체 녹음시간 분의 검출된 시간으로 퍼센트로 구한다.
            sleepScore=100-(((float)kococoTerm/(float)recordTerm)*100);
            TextView scoreTextView = findViewById(R.id.scoreTextView);
            //i18n 적용
            Resources res = getResources();
            String scoreText = String.format(res.getString(R.string.score),Math.round(sleepScore));
            scoreTextView.setText(scoreText);

            //시간 HH:mm ~ HH:mm
//        System.out.println("=============레알 kococoTerm=========="+kococoTerm);
            String recodeText = DateTimeToStringFormat.format(recordStartDT)+"~"+DateTimeToStringFormat.format(recordEndDT);
            TextView recodeTextView = findViewById(R.id.recodeTextView);
            recodeTextView.setText(recodeText);


            //차트 시작
            chart = findViewById(R.id.chart1);
            chart.getDescription().setEnabled(false);
            // if more than 60 entries are displayed in the chart, no values will be
            // drawn
            chart.setMaxVisibleValueCount(60);

            // scaling can now only be done on x- and y-axis separately
            chart.setPinchZoom(false);

            chart.setDrawBarShadow(false);
            chart.setDrawGridBackground(false);
            chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    System.out.println("=========클릭했다~~~"+e.getData());
                    //데이터 넣을 시에 재생 파일 패스 및 재생 구간을 넣으면된다.
                    JsonObject graphData= (JsonObject) e.getData();
                    playingDetailId = graphData.get("analysisDetailsId").getAsInt();
                    try {
//                        mpu.playMp(graphData.get("termStartDt").getAsInt(),graphData.get("termEndDt").getAsInt(),graphData.get("filePath").getAsString(),getApplicationContext());
//                        if(!adapter.getPlayBtnFlag()){
                            adapter.playActivityMp(graphData.get("analysisDetailsId").getAsInt(),graphData.get("termStartDt").getAsInt(),graphData.get("termEndDt").getAsInt(),graphData.get("filePath").getAsString(),getApplicationContext());
//                        }else{
//                            adapter.stopActivityMp(graphData.get("analysisDetailsId").getAsInt());
//                        }

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
            xAxis.setDrawGridLines(false);
            //X좌표 폰트
            xAxis.setTextColor(Color.WHITE);
            xAxis.setValueFormatter(xAxisFormatter);


            //시작과 끝점을 정해줘야 그래프가 제대로잘 나옴
            xAxis.setAxisMinimum(0);
            xAxis.setAxisMaximum((float)recordEndDT.getTime()-referenceTimestamp);

            System.out.println("=========recordEndDT.getTime()-referenceTimestamp==============="+(recordEndDT.getTime()-referenceTimestamp));

            chart.getAxisLeft().setDrawGridLines(false);
            //Y좌표 폰트
            chart.getAxisLeft().setTextColor(Color.WHITE);
            //Y좌표 오른쪽 안보이게 처리
            chart.getAxisRight().setEnabled(false);
            // setting data
            // add a nice and smooth animation
            chart.animateY(1500);
            chart.getLegend().setEnabled(false);

            chart = (BarChart)findViewById(R.id.chart1);

//            List<BarEntry> entries = new ArrayList<>();

            System.out.println("=================entries.size()================="+entries.size());
            BarDataSet set1;
            set1 = new BarDataSet(entries, "Data Set");
//            set1.setColors(ColorTemplate.VORDIPLOM_COLORS);
//            set1.setDrawValues(false);
            set1.setColors(ColorTemplate.COLORFUL_COLORS);
            set1.setDrawValues(true);
            set1.setValueTextColor(Color.WHITE);

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            BarData data = new BarData(dataSets);
            //바의 두께를 바꿀수 있는
            data.setBarWidth(100f);
            chart.setData(data);
            chart.setFitBars(true);

            chart.invalidate();


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
            chart.setFitBars(true);
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
