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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.adapter.RecordListAdapter;
import kr.co.dwebss.kococo.model.RecodeData;
import kr.co.dwebss.kococo.model.Section;

public class ResultActivity extends AppCompatActivity implements OnSeekBarChangeListener {

    private static final String TAG = "ResultActivity";
    private BarChart chart;

    private Section resultListSection;
    private RecyclerView resultListRv;
    private ListView recordLv ;
    JsonObject responseData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출

        setContentView(R.layout.activity_result);

        //데이터 수신
        Intent intent = getIntent();
        if(getIntent().hasExtra("responseData"))
        responseData = new JsonParser().parse(getIntent().getStringExtra("responseData")).getAsJsonObject();

        System.out.println("=============레알 되라===================="+responseData);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

        initializeData();

        super.onCreate(savedInstanceState);
//        getSupportActionBar().setElevation(0);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
        bt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResultActivity.super.onBackPressed();
            }
        });
        TextView dateTxtHeader = (TextView) findViewById(R.id.date_txt_header);
        Date from = new Date();
        SimpleDateFormat transFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
        String dateVal = transFormat.format(from);
        dateTxtHeader.setText(dateVal);

        // Adapter 생성
        RecordListAdapter adapter = new RecordListAdapter() ;
        //listView 생성
        ListView listview = (ListView) findViewById(R.id.recordListview);
        listview.setAdapter(adapter);
        // 첫 번째 아이템 추가.
        for(int i=0; i<10; i++){
            adapter.addItem("녹음파일"+i) ;
        }
        // 두 번째 아이템 추가.
//        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.ic_account_circle_black_36dp),
//                "Circle", "Account Circle Black 36dp") ;


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


    //녹음 리스트 더미 데이터
    private void initializeData() {
        ArrayList<RecodeData> resultList = new ArrayList<RecodeData>();
        for(int i=0; i<10; i++){
            resultList.add(new RecodeData("녹음 파일"+i,"i"));
        }
//        resultListSection = new Section(resultList, "recodes", false);
    }
}
