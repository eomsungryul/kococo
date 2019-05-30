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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.adapter.RecordListAdapter;
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출
        setContentView(R.layout.activity_result);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

//        initializeData();
        super.onCreate(savedInstanceState);

        //녹음 기록 갱신
//        FragmentManager fragmentManager = getFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        Fragment fragment = fragmentManager.findFragmentById(R.id.diaryFr);
//        fragmentTransaction.remove(fragment);
//        fragmentTransaction.commit();


        //데이터 수신
        Intent intent = getIntent();
        if(getIntent().hasExtra("responseData")) responseData = new JsonParser().parse(getIntent().getStringExtra("responseData")).getAsJsonObject();


//        System.out.println("=============레알 되라===================="+responseData);

        //        responseData.getAsJsonArray("e");

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
        try {
            recordStartD =  stringtoDateFormat.parse(responseData.get("recordStartD").toString().replace("\"",""));
            recordEndD =  stringtoDateFormat.parse(responseData.get("recordEndD").toString().replace("\"",""));

            recordStartDT =  stringtoDateTimeFormat.parse(responseData.get("recordStartDt").toString().replace("\"",""));
            recordEndDT =  stringtoDateTimeFormat.parse(responseData.get("recordEndDt").toString().replace("\"",""));
            recordTerm = recordEndDT.getTime()-recordStartDT.getTime();

        } catch (ParseException e) {
            e.printStackTrace();
        }

//        String recordStartD =  responseData.get("recordStartD").toString().replace("\"","");
//        String recordEndD =  responseData.get("recordEndD").toString().replace("\"","");
        if(recordStartD.equals(recordEndD)){
            dateTxtHeader.setText(transFormat.format(recordStartD));

        }else{
            dateTxtHeader.setText(transFormat.format(recordStartD)+"~"+transFormat.format(recordEndD));
        }

        // 하단에 녹음 검출리스트 파일 리스트  Adapter 생성
        RecordListAdapter adapter = new RecordListAdapter() ;
        //listView 생성
        ListView listview = (ListView) findViewById(R.id.recordListview);
        listview.setAdapter(adapter);

        // 녹음 검출리스트 추가.
       JsonArray analysisList = responseData.getAsJsonArray("analysisList");

        if(analysisList.size()>0){
            for(int i=0; i<analysisList.size(); i++){
                JsonObject analysisObj = (JsonObject) analysisList.get(i);
                analysisObj.get("analysisStartDt");
                recordData = new RecordData();
                recordData.setAnalysisFileNm(analysisObj.get("analysisFileNm").toString().replace("\"",""));
                recordData.setAnalysisFileAppPath(analysisObj.get("analysisFileAppPath").toString().replace("\"",""));
                recordData.setAnalysisId(analysisObj.get("analysisId").getAsInt());
                recordData.setAnalysisStartDt(analysisObj.get("analysisStartDt").toString().replace("\"",""));
                recordData.setAnalysisEndDt(analysisObj.get("analysisEndDt").toString().replace("\"",""));
                JsonArray analysisDetailsList = analysisObj.getAsJsonArray("analysisDetailsList");
                if(analysisDetailsList.size()>0){
                    for(int j=0; j<analysisDetailsList.size(); j++){
                        JsonObject analysisDetailsObj = (JsonObject) analysisDetailsList.get(i);
                        int termTypeCd =  analysisDetailsObj.get("termTypeCd").getAsInt();
                        if(termTypeCd==200101){
                            recordData.setTitle("코골이"+(i+j+1));
                        }else if(termTypeCd==200102){
                            recordData.setTitle("이갈이"+(i+j+1));
                        }else{
                            recordData.setTitle("무호흡"+(i+j+1));
                        }
                        recordData.setAnalysisDetailsId(analysisDetailsObj.get("analysisDetailsId").getAsInt());
                        recordData.setTermStartDt(analysisDetailsObj.get("termStartDt").toString().replace("\"",""));
                        recordData.setTermEndDt(analysisDetailsObj.get("termEndDt").toString().replace("\"",""));
                        recordData.setTermTypeCd(termTypeCd);
                        adapter.addItem(recordData) ;

                        try {
                            kococoTerm += (stringtoDateTimeFormat.parse(recordData.getTermEndDt()).getTime()
                                    -stringtoDateTimeFormat.parse(recordData.getTermStartDt()).getTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
//                System.out.println("=============레알 analysisStartDt=========="+analysisObj.get("analysisStartDt"));
            }
        }

        //test용
//        dateTxtHeader.setText(transFormat.format(recordStartD)+"===="+recordData.getAnalysisId());


        //점수 구하는법
        //공식은 전체 녹음시간 분의 검출된 시간으로 퍼센트로 구한다.
        sleepScore=100-(((float)kococoTerm/(float)recordTerm)*100);
        TextView scoreTextView = findViewById(R.id.scoreTextView);
        scoreTextView.setText(Math.round(sleepScore)+"점");

        //시간 HH:mm ~ HH:mm
//        System.out.println("=============레알 kococoTerm=========="+kococoTerm);
//        System.out.println("=============레알 recordTerm=========="+recordTerm);
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

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        chart.getAxisLeft().setDrawGridLines(false);
        // setting data
        // add a nice and smooth animation
        chart.animateY(1500);
        chart.getLegend().setEnabled(false);

        chart = (BarChart)findViewById(R.id.chart1);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            float multi = (100 + 1);
            float val = (float) (Math.random() * multi) + multi / 3;
            entries.add(new BarEntry(i, val));
        }
        BarDataSet set1;
        set1 = new BarDataSet(entries, "Data Set");
        set1.setColors(ColorTemplate.VORDIPLOM_COLORS);
        set1.setDrawValues(false);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

        BarData data = new BarData(dataSets);
        chart.setData(data);
        chart.setFitBars(true);
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
//    //녹음 리스트 더미 데이터
//    private void initializeData() {
//        ArrayList<Record> resultList = new ArrayList<Record>();
//        for(int i=0; i<10; i++){
//            resultList.add(new Record("녹음 파일"+i,"i"));
//        }
////        resultListSection = new Section(resultList, "recodes", false);
//    }
}
