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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.adapter.DiaryListAdapter;
import kr.co.dwebss.kococo.adapter.UserConsultListAdapter;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class UserConsultListActivity extends AppCompatActivity {

    private static final String TAG = "UserConsultListActivity";

    JsonObject responseData;
    Retrofit retrofit;
    ApiService apiService;

    FindAppIdUtil fau = new FindAppIdUtil();

    UserConsultListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출
        setContentView(R.layout.activity_user_consult_list);

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
                UserConsultListActivity.super.onBackPressed();
            }
        });

        FindAppIdUtil fau = new FindAppIdUtil();
        String userAppId=fau.getAppid(this);
        if(userAppId!=null){
            retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
            apiService = retrofit.create(ApiService.class);

            apiService.getConsultList(userAppId,"recordId,desc").enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    System.out.println(" ============getConsultList============response: "+response);
                    JsonObject jsonObject = response.body();
                    JsonObject resultData = jsonObject.getAsJsonObject("_embedded");
                    JsonArray recordList = resultData.getAsJsonArray("recordOnly");
                    if(recordList.size()>0){
                        // Adapter 생성
                        adapter = new UserConsultListAdapter() ;
                        //listView 생성
                        ListView listview = (ListView) findViewById(R.id.counsultListview);
                        listview.setAdapter(adapter);
                        adapter.addItems(recordList) ;
                        adapter.notifyDataSetChanged();
                    }else{
                        TextView nullTextView = (TextView) findViewById(R.id.nullTextView);
                        nullTextView.setText("전문가 상담 내역이 없습니다.");
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Toast.makeText(UserConsultListActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
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
//        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
