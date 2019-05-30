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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.RecordData;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ReportActivity extends AppCompatActivity {

    private static final String TAG = "ReportActivity";
    RecordData recordData;
    EditText claimContents;

    Retrofit retrofit;
    ApiService apiService;
    //
    Boolean requestClaimFlag;
    Boolean uploadClaimFileFlag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //세로모드에서 가로모드로 전환 시 onCreate함수가 다시 호출

        setContentView(R.layout.activity_report);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;
        super.onCreate(savedInstanceState);

        TextView test  = findViewById(R.id.declareTxtHeader);

        Intent intent = getIntent();
        recordData = (RecordData) intent.getSerializableExtra("testData");
        test.setText(recordData.getTitle()+" 신고하기");

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
        bt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReportActivity.super.onBackPressed();
            }
        });

        claimContents = (EditText)findViewById(R.id.claimContents);


        Button declareBtn = (Button) findViewById(R.id.declareBtn);
        declareBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeclareDialog();
            }
        });

        //firebase 업로드 관련
        // Create a storage reference from our app
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to "mountains.jpg"
        StorageReference mountainsRef = storageRef.child("mountains.jpg");

        // Create a reference to 'images/mountains.jpg'
        StorageReference mountainImagesRef = storageRef.child("images/mountains.jpg");

        // While the file names are the same, the references point to different files
        mountainsRef.getName().equals(mountainImagesRef.getName());    // true
        mountainsRef.getPath().equals(mountainImagesRef.getPath());    // false


    }

    //얼럿 다이얼로그 띄우기
    void showDeclareDialog()
    {
        //Style을 넣어서 커스텀 가능
        // 타이틀이 없으면 안나오고 메세지 길이에 따라 경고창길이가 달라진다.
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.MyAlertDialogStyle);
        builder.setTitle("제출하기");
        builder.setMessage("제출하시겠습니까?");
        //setView()를 이용하여 view를 넣고 커스텀 할 수 있다.
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(getApplicationContext(),"예를 선택했습니다.",Toast.LENGTH_LONG).show();
                        if(claimContents.getText().length()>0){
                            Toast.makeText(getApplicationContext(),"내용을 입력해주세요.",Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            //firebase 파일 업로드
                            Boolean uploadFlag = uploadClaimFile();
                            if(uploadFlag){
                                //신고 전송
                                Boolean requstFlag =requestClaim();
                                if(requstFlag){

                                }else{
                                    Toast.makeText(getApplicationContext(),"신고하기를 실패하였습니다. ",Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }else{
                                Toast.makeText(getApplicationContext(),"파일 업로드에 실패하였습니다. ",Toast.LENGTH_LONG).show();
                                return;
                            }

                        }
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

    private Boolean requestClaim() {
        requestClaimFlag= false;
        //형태
//        {
//            "analysisServerUploadPath" : "/storage/rec_data", //필수
//                "claimReasonCd" : 100201, //100201-'코골이가아닙니다.', 100202-'기타' 필수임
//                "claimContents" : "테스트" //필수
//        }
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("analysisServerUploadPath","");
        requestJson.addProperty("claimReasonCd",recordData.getTermTypeCd());
        requestJson.addProperty("claimContents",claimContents.getText().toString());
        RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(requestJson));

        apiService.addClaim(recordData.getAnalysisDetailsId(),requestData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
//                System.out.println(" ========================response: "+response.body().toString());
                //저장 시에 뒤로가기
                Toast.makeText(getApplicationContext(),"신고하기가 완료되었습니다.",Toast.LENGTH_LONG).show();
                ReportActivity.super.onBackPressed();
                requestClaimFlag=true;
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
//                System.out.println(" ========================Throwable: "+ t);
            }
        });
        return requestClaimFlag;
    }

    private Boolean uploadClaimFile() {
        uploadClaimFileFlag= false;
        //업로드 파일 시작



        return uploadClaimFileFlag;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);
    }

}
