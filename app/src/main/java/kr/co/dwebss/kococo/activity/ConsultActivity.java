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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
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
    JsonArray analysisList;

    int recordId;
    int analysisId;
    String analysisServerUploadPath;

    private static final String TAG = "ConsultActivity";

    EditText consultContents;
    EditText consultTitle;

    ScrollView sv;
    int uploadFileCnt=0;

    ProgressDialog uploadDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

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

            analysisList = new JsonParser().parse(getIntent().getStringExtra("analysisList")).getAsJsonArray();

            TextView consultTxtHeader = (TextView) findViewById(R.id.consultTxtHeader);
            consultTxtHeader.setText(getIntent().getStringExtra("headerTextDate")+" \n분석된 내용을 바탕으로 전문가 상담을 의뢰합니다.");

            //뒤로가기 버튼
            ImageButton bt = (ImageButton) findViewById(R.id.previousButton);
            bt.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imm.hideSoftInputFromWindow(consultTitle.getWindowToken(),0);
                    ConsultActivity.super.onBackPressed();
                }
            });

            consultTitle = (EditText)findViewById(R.id.title_val);
//            consultTitle.setText("analysisId : "+analysisId +"/recordId : "+recordId            );
            consultContents = (EditText)findViewById(R.id.contents_val);
//            consultContents.setText("analysisServerUploadPath : "+analysisServerUploadPath
//                    +"responseData : "+responseData
//            );

            Button declareBtn = (Button) findViewById(R.id.declareBtn);
            declareBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showConsultDialog();
                }
            });
            sv = (ScrollView) findViewById(R.id.scrollVew);
//            consultContents.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    sv.fullScroll(sv.FOCUS_DOWN);
//                    return false;
//                }
//            });
            consultTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {

                    } else {
                        sv.fullScroll(sv.FOCUS_DOWN);
                    }
                }
            });
            consultContents.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        sv.fullScroll(sv.FOCUS_DOWN);
                    } else {
                    }
                }
            } );





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
//                            Toast.makeText(getApplicationContext(),"등록중입니다 잠시만기다려주세요. ",Toast.LENGTH_LONG).show();
                            chechUploadFile();
//                            addConsult();
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


    private void chechUploadFile() {
        boolean deleteFlag = true;
        JsonArray uploadList = new JsonArray();
        for(int i = 0; i<analysisList.size(); i++){
            JsonObject obj = analysisList.get(i).getAsJsonObject();
            String filePath = obj.get("analysisServerUploadPath").getAsString();
            File file = new File(filePath);
            if(file.exists()){
                uploadList.add(analysisList.get(i));
            }else{
                deleteFlag = false;
            }
        }

        if(!deleteFlag){
            //파일이 없는 경우 삭제된 데이터가 있는데 상담을 진행 하겠습니까? 라는 것을 띄운다.
            AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.MyAlertDialogStyle);
            builder.setTitle("상담하기");
            builder.setMessage("삭제된 데이터가 있는데 상담을 진행 하겠습니까?");
            //setView()를 이용하여 view를 넣고 커스텀 할 수 있다.
            //예일 경우에는 상담하기를 한다.
            //1. 파이어베이스에 업로드를 한다.
            //2. 업로드가 되면 상담하기 제출을 한다.
            builder.setPositiveButton("예",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            uploadAllFile(uploadList);
                        }
                    });
            builder.setNegativeButton("아니오",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
            builder.show();
        }else{
            uploadAllFile(uploadList);
        }

    }


    private void uploadAllFile(JsonArray uploadList) {
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

//        {"analysisId":1943,"analysisServerUploadPath":"/data/user/0/kr.co.dwebss.kococo/files/rec_data/633/snoring-201907_02_1118~02_1121_1562034101327.mp3"}

        //내 실제 경로를 입력한다.
        uploadFileCnt= 0;
        uploadDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);
        uploadDialog.setMessage("파일 전송중입니다. ("+uploadFileCnt+"/"+uploadList.size()+")");
        uploadDialog.show();

        for(int i = 0; i<uploadList.size(); i++){

            JsonObject obj = uploadList.get(i).getAsJsonObject();
            String filePath = obj.get("analysisServerUploadPath").getAsString();
            String[] filePathSplit = filePath.split("/");
            String fileNm = filePathSplit[filePathSplit.length-1];

            String uploadFirebasePath = "rec_data/"+appId+"/"+sdf.format(date)+"/"+fileNm;
            StorageReference spaceRef = storageRef.child(uploadFirebasePath);

            File file = new File(filePath);
            if(file.exists()){
                Uri putFile = Uri.fromFile(file);
                UploadTask uploadTask = spaceRef.putFile(putFile);

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
//                        addConsult();
                        uploadFileCnt++;
                        uploadDialog.setMessage("파일 전송중입니다. ("+uploadFileCnt+"/"+analysisList.size()+")");
                        uploadDialog.show();
                        if(uploadFileCnt==uploadList.size()){
                            //끝나면 전문가 상담하기를 한다.
//                            System.out.println("======================성공====");
                            uploadDialog.dismiss();
                            addConsult();
                        }
                    }
                });
            }
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
