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
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.http.ApiService;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConsultActivity extends AppCompatActivity {
    Retrofit retrofit;
    ApiService apiService;

    JsonObject responseData;

    int recordId;
    int analysisId;
    String analysisServerUploadPath;

    private static final String TAG = "ConsultActivity";

    EditText consultContents;
    EditText consultTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출
        setContentView(R.layout.activity_consult);

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;

//        initializeData();
        super.onCreate(savedInstanceState);

        //데이터 수신
        Intent intent = getIntent();
        if(getIntent().hasExtra("responseData")){
            responseData = new JsonParser().parse(getIntent().getStringExtra("responseData")).getAsJsonObject();

            analysisId = getIntent().getIntExtra("analysisId",0);
            recordId = getIntent().getIntExtra("recordId",0);
            analysisServerUploadPath = getIntent().getStringExtra("analysisServerUploadPath");

            //뒤로가기 버튼
            ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
            bt.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConsultActivity.super.onBackPressed();
                }
            });

            consultTitle = (EditText)findViewById(R.id.title_val);
            consultTitle.setText("analysisId : "+analysisId +"/recordId : "+recordId
            );
            consultContents = (EditText)findViewById(R.id.contents_val);
            consultContents.setText("analysisServerUploadPath : "+analysisServerUploadPath
                    +"responseData : "+responseData
            );

            Button declareBtn = (Button) findViewById(R.id.declareBtn);
            declareBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showConsultDialog();
                }
            });


        }else{
            Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_LONG);
        }
    }


    //얼럿 다이얼로그 띄우기
    void showConsultDialog()
    {
        if(TextUtils.isEmpty(consultTitle.getText())){
            Toast.makeText(getApplicationContext(),"제목을 입력해주세요.",Toast.LENGTH_SHORT).show();
            return;
        }else if(TextUtils.isEmpty(consultContents.getText())){
            Toast.makeText(getApplicationContext(),"내용을 입력해주세요.",Toast.LENGTH_SHORT).show();
            return;
        }else{
            //Style을 넣어서 커스텀 가능
            // 타이틀이 없으면 안나오고 메세지 길이에 따라 경고창길이가 달라진다.
            AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.MyAlertDialogStyle);
            builder.setTitle("상담하기");
            builder.setMessage("전문가와 상담하시겠습니까?");
            //setView()를 이용하여 view를 넣고 커스텀 할 수 있다.
            //예일 경우에는 상담하기를 한다.
            //1. 파이어베이스에 업로드를 한다.
            //2. 업로드가 되면 상담하기 제출을 한다.
            builder.setPositiveButton("예",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(getApplicationContext(),"예를 선택했습니다.",Toast.LENGTH_LONG).show();
                            Toast.makeText(getApplicationContext(),"등록중입니다 잠시만기다려주세요. ",Toast.LENGTH_LONG).show();
                            addConsult();
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
    }


    private void addConsult() {
        //형태
        // 아래 값중 값이 하나라도 빠져있으면, 400 bad request 발생
        //{
        //  "analysisServerUploadPath" : "/storage/rec_data",
        //  "analysisDetailsList" : [ {
        //    "claimReasonCd" : 100101, //100201-'코골이가아닙니다.', 100202-'기타'
        //    "consultContents" : "테스트"
        //  } ]
        //}
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("consultingTitle",consultTitle.getText().toString());
        requestJson.addProperty("consultingContents",consultContents.getText().toString());
        JsonArray analysisList = new JsonArray();
        JsonObject analysisObj = new JsonObject();
        analysisObj.addProperty("analysisId",analysisId);
        analysisObj.addProperty("analysisServerUploadPath",analysisServerUploadPath);
        analysisList.add(analysisObj);
        requestJson.add("analysisList",analysisList);

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestJson));
//        System.out.println(" =============ddd===========Throwable: "+ requestJson.toString());
//        System.out.println(" ============eeee============Throwable: "+ requestData.toString());

//        {"analysisServerUploadPath":"rec_data/7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f/2019-05-30/snoring-20191330_1513-30_1514_1559196886407.wav","analysisDetailsList":[{"claimReasonCd":200102,"consultContents":"recordData : /data/user/0/kr.co.dwebss.kococo/files/rec_data/29/snoring-20191330_1513-30_1514_1559196886407.wav/getAnalysisDetailsId :97/getAnalysisId :93"}]}
//        {"userAppId":"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f","recordStartDt":"2019-05-30T18:54:48","recordEndDt":"2019-05-30T18:55:09","analysisList":[{"analysisStartDt":"2019-05-30T18:54:56","analysisEndDt":"2019-05-30T18:55:16","analysisFileAppPath":"/storage/emulated/0/Download/rec_data/1","analysisFileNm":"snoring-20190530_1854-30_1855_1559210109319.mp3","analysisDetailsList":[]}]}

        apiService.addConsult(recordId,requestData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
//                System.out.println(" ========================response: "+response.body());
                JsonObject jsonObject = response.body();
                if(jsonObject==null){
                    Toast.makeText(getApplicationContext(),"상담하기가 실패되었습니다.",Toast.LENGTH_LONG).show();
                }else{
                    //저장 시에 뒤로가기
                    Toast.makeText(getApplicationContext(),"상담하기가 완료되었습니다.",Toast.LENGTH_LONG).show();
                    ConsultActivity.super.onBackPressed();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
//                System.out.println(" ========================Throwable: "+ t);
                Toast.makeText(getApplicationContext(),"상담하기가 실패되었습니다.",Toast.LENGTH_LONG).show();
            }
        });
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
