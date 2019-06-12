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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
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
import com.google.gson.GsonBuilder;
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
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    JsonObject responseData;
    Spinner pfAge;
    Spinner pfGender;
    Spinner pfHeight;
    Spinner pfWeight;

    Retrofit retrofit;
    ApiService apiService;

    FindAppIdUtil fau = new FindAppIdUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출
        setContentView(R.layout.activity_profile);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

        super.onCreate(savedInstanceState);

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //뒤로가기 버튼
        ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
        bt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileActivity.super.onBackPressed();
            }
        });

        ArrayList<Integer> ageList = new ArrayList();
        for(int i=1;i<100;i++){
            ageList.add(i);
        }
        pfAge = (Spinner)findViewById(R.id.pf_age_val);
        ArrayAdapter<Integer> ageAdapter = new ArrayAdapter<Integer>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                ageList);
        pfAge.setAdapter(ageAdapter);
        pfAge.setSelection(20);

        String[] genderList = {"남","여"};
        pfGender = (Spinner)findViewById(R.id.pf_gender_val);
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                genderList);
        pfGender.setAdapter(genderAdapter);
        pfGender.setSelection(0);


        ArrayList<Integer> heightList = new ArrayList();
        for(int i=130;i<200;i++){
            heightList.add(i);
        }
        pfHeight = (Spinner)findViewById(R.id.pf_height_val);
        ArrayAdapter<Integer> heightAdapter = new ArrayAdapter<Integer>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                heightList);
        pfHeight.setAdapter(heightAdapter);
        pfHeight.setSelection(50);



        ArrayList<Integer> weightList = new ArrayList();
        for(int i=30;i<200;i++){
            weightList.add(i);
        }
        pfWeight = (Spinner)findViewById(R.id.pf_weight_val);
        ArrayAdapter<Integer> weightAdapter = new ArrayAdapter<Integer>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                weightList);
        pfWeight.setAdapter(weightAdapter);
        pfWeight.setSelection(60);

        Button saveBtn = (Button) findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDialog();
            }
        });

        //데이터 수신
        Intent intent = getIntent();
        if(getIntent().hasExtra("responseData")){
            responseData = new JsonParser().parse(getIntent().getStringExtra("responseData")).getAsJsonObject();
        }else{
            Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_LONG);
        }
    }

    //얼럿 다이얼로그 띄우기
    void showSaveDialog()
    {
            //Style을 넣어서 커스텀 가능
            // 타이틀이 없으면 안나오고 메세지 길이에 따라 경고창길이가 달라진다.
            AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.MyAlertDialogStyle);
            builder.setTitle("저장하기");
            builder.setMessage("저장하시겠습니까?");
            //setView()를 이용하여 view를 넣고 커스텀 할 수 있다.
            //예일 경우에는 신고하기를 한다.
            //1. 파이어베이스에 업로드를 한다.
            //2. 업로드가 되면 신고하기 제출을 한다.
            builder.setPositiveButton("예",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(),"등록중입니다 잠시만기다려주세요. ",Toast.LENGTH_LONG).show();
                            addProfile();
                        }
                    });
            builder.setNegativeButton("아니오",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(getApplicationContext(),"아니오를 선택했습니다.", Toast.LENGTH_LONG).show();
                        }
                    });
            builder.show();
            //이런식으로 높이와 길이를 지정할수있지만 비율에 맞게 버튼위치가 늘어나지않음 비추.
//        builder.show().getWindow().setLayout(600, 400);

    }


    private void addProfile() {
        //형태
        // 항목 중 값이 없는 경우 업데이트 안함
        //{
        //  "userAge" : 32,
        //  "userGender" : "M",
        //  "userWeight" : 78,
        //  "userHeight" : 173
        //}
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("userAge",pfAge.getSelectedItem().toString());
        requestJson.addProperty("userGender",pfGender.getSelectedItem().toString());
        requestJson.addProperty("userWeight",pfHeight.getSelectedItem().toString());
        requestJson.addProperty("userHeight",pfWeight.getSelectedItem().toString());

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestJson));
        System.out.println(" =============ddd===========Throwable: "+ requestJson.toString());
        System.out.println(" ============eeee============Throwable: "+ requestData.toString());

        apiService.addProfile(fau.getAppid(this),requestData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
//                System.out.println(" ========================response: "+response.body());
                JsonObject jsonObject = response.body();
                    //저장 시에 뒤로가기
                    Toast.makeText(getApplicationContext(),"프로필이 저장되었습니다.",Toast.LENGTH_LONG).show();
                    ProfileActivity.super.onBackPressed();
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
//                System.out.println(" ========================Throwable: "+ t);
                Toast.makeText(getApplicationContext(),"프로필 저장이 실패햇습니다.",Toast.LENGTH_LONG).show();
            }
        });
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
    public void onDestroy() {
        super.onDestroy();
    }

}
