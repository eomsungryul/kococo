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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.RecordData;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
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

    String uploadFirebasePath;

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
        claimContents.setText("recordData : "+recordData.getAnalysisFileAppPath()
                +"/"+recordData.getAnalysisFileNm()
                +"/getAnalysisDetailsId :"+recordData.getAnalysisDetailsId()
                +"/getAnalysisId :"+recordData.getAnalysisId()
        );


        Button declareBtn = (Button) findViewById(R.id.declareBtn);
        declareBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeclareDialog();
            }
        });





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


        //예일 경우에는 신고하기를 한다.
        //1. 파이어베이스에 업로드를 한다.
        //2. 업로드가 되면 신고하기 제출을 한다.
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(getApplicationContext(),"예를 선택했습니다.",Toast.LENGTH_LONG).show();
                        Toast.makeText(getApplicationContext(),"등록중입니다 잠시만기다려주세요. ",Toast.LENGTH_LONG).show();
                        if(claimContents.getText().length()==0){
                            Toast.makeText(getApplicationContext(),"내용을 입력해주세요.",Toast.LENGTH_SHORT).show();
                            return;
                        }else{
                            //firebase 파일 업로드
                            addClaim();
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
        // 아래 값중 값이 하나라도 빠져있으면, 400 bad request 발생
        //{
        //  "analysisServerUploadPath" : "/storage/rec_data",
        //  "analysisDetailsList" : [ {
        //    "claimReasonCd" : 100101, //100201-'코골이가아닙니다.', 100202-'기타'
        //    "claimContents" : "테스트"
        //  } ]
        //}
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("analysisServerUploadPath",uploadFirebasePath);
        JsonArray analysisDetailsList = new JsonArray();
        JsonObject analysisDetailsObj = new JsonObject();
        analysisDetailsObj.addProperty("claimReasonCd",recordData.getTermTypeCd());
        analysisDetailsObj.addProperty("claimContents",claimContents.getText().toString());
        analysisDetailsList.add(analysisDetailsObj);
        requestJson.add("analysisDetailsList",analysisDetailsList);
        RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(requestJson));
        Toast.makeText(getApplicationContext(),"ddd"+requestData.toString(),Toast.LENGTH_LONG).show();
                System.out.println(" =============ddd===========Throwable: "+ requestJson.toString());
                System.out.println(" ============eeee============Throwable: "+ requestData);
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
                Toast.makeText(getApplicationContext(),"신고하기가 실패되었습니다.",Toast.LENGTH_LONG).show();
            }
        });
        return requestClaimFlag;



    }

    private void addClaim() {
        //업로드 파일 시작
        FindAppIdUtil fau = new FindAppIdUtil();
        String appId =  fau.getAppid(getApplicationContext());

        //firebase 업로드 관련
        // 가장 먼저, FirebaseStorage 인스턴스를 생성한다
        // getInstance() 파라미터에 들어가는 값은 firebase console에서
        // storage를 추가하면 상단에 gs:// 로 시작하는 스킴을 확인할 수 있다
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://kococo-2996f.appspot.com/");

        //위에서 생성한 FirebaseStorage 를 참조하는 storage를 생성한다
        StorageReference storageRef = storage.getReference();
        // 위의 저장소를 참조하는 images폴더안의 space.jpg 파일명으로 지정하여
        // 하위 위치를 가리키는 참조를 만든다
        // 이부분은 firebase 스토리지 쪽에 업로드 되는 경로이다.
        //그렇기에 폴더 규칙을 재생데이터/앱아이디/일별날짜/파일 이런식으로 작성
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        uploadFirebasePath = "rec_data/"+appId+"/"+sdf.format(date)+"/"+recordData.getAnalysisFileNm();
        StorageReference spaceRef = storageRef.child(uploadFirebasePath);

        //내 실제 경로를 입력한다.
        Uri file = Uri.fromFile(new File(recordData.getAnalysisFileAppPath()+"/"+recordData.getAnalysisFileNm()));
        UploadTask uploadTask = spaceRef.putFile(file);

        // 파일 업로드의 성공/실패에 대한 콜백 받아 핸들링 하기 위해 아래와 같이 작성한다

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                System.out.println("======================실패"+exception.getMessage());
                Toast.makeText(getApplicationContext(),"파일 업로드에 실패하였습니다. ",Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //신고 전송
                requestClaim();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged " + newConfig.screenWidthDp + "," + newConfig.screenHeightDp);
    }

}
