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
package kr.co.dwebss.kococo.application;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.StaticVariables;
import kr.co.dwebss.kococo.http.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static kr.co.dwebss.kococo.activity.StaticVariables.isCorrectPatch;
import static kr.co.dwebss.kococo.activity.StaticVariables.patchCnt;
import static kr.co.dwebss.kococo.activity.StaticVariables.patchDownloadInProgress;
import static kr.co.dwebss.kococo.activity.StaticVariables.patchDownloadSuccessful;


public class VersionProgressApplication extends Application {

    private static VersionProgressApplication versionProgressApplication;
    AppCompatDialog progressDialog;

    private static final String TAG_VERSION = "version";

    Retrofit retrofit;
    ApiService apiService;
    TextView noBtn;
    TextView yesBtn;
    TextView confirmBtn;

    TextView progressBarTxt;

    //인터넷 연결 유형
    ConnectivityManager cm;
    NetworkInfo activeNetwork;

    boolean isWiFi=false;

    public static VersionProgressApplication getInstance() {
        return versionProgressApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        versionProgressApplication = this;
    }

    public void progressON(Activity activity) {

        if (activity == null || activity.isFinishing()) {
            return;
        }

        if (progressDialog != null && progressDialog.isShowing()) {

        } else {
            progressDialog = new AppCompatDialog(activity);
            progressDialog.setCancelable(false);
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            progressDialog.setContentView(R.layout.progress_version);

            chkVersion(activity);
        }
        //인터넷 연결 유형
        cm = (ConnectivityManager)activity.getSystemService(activity.CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();

        noBtn = (TextView) progressDialog.findViewById(R.id.noBtn);
        yesBtn = (TextView) progressDialog.findViewById(R.id.yesBtn);
        noBtn.setVisibility(View.INVISIBLE);
        yesBtn.setVisibility(View.INVISIBLE);

        confirmBtn = (TextView) progressDialog.findViewById(R.id.confirmBtn);
        confirmBtn.setVisibility(View.INVISIBLE);
        confirmBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressOFF();
            }
        });

    }

    public void progressSET(String message) {

        if (progressDialog == null || !progressDialog.isShowing()) {
            return;
        }

        TextView tv_progress_message = (TextView) progressDialog.findViewById(R.id.tv_progress_message);
        if (!TextUtils.isEmpty(message)) {
            tv_progress_message.setText(message);
        }

    }

    public void progressOFF() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    public void chkVersion(Activity activity){

        isCorrectPatch = false;
        progressBarTxt = (TextView) progressDialog.findViewById(R.id.progressTxt) ;
//        progressBarTxt.setVisibility(View.INVISIBLE);

//        progressBarTxt.setText("패치데이터를 확인합니다.");
//        progressBarTxt.setVisibility(View.VISIBLE);
        //http 통신
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        apiService.getApiCode().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                JsonArray codeList = response.body().getAsJsonObject("_embedded").getAsJsonArray("code");
                Gson gson = new Gson();
                Map<String, Object> map = new HashMap<String, Object>();
                //StaticVariables.version = "";
                //StaticVariables.size = 0;
                boolean isChangedVersion = false;
                for (JsonElement je : codeList) {
                    map = (Map<String, Object>) gson.fromJson(je.getAsJsonObject(), map.getClass());
                    if (((Double) map.get("code")).intValue() == 999999) {
                        String value = String.valueOf(map.get("codeValue"));
                        if(!StaticVariables.version.equals(value)){
                            isChangedVersion = true;
                        }
                        StaticVariables.version = value;
                        Log.e(TAG_VERSION, "Version: " + StaticVariables.version);
                    }else
                    if (((Double) map.get("code")).intValue() == 999998) {
                        try {
                            int value = Integer.parseInt(((String) map.get("codeValue")));
                            if(StaticVariables.size!=value){
                                isChangedVersion = true;
                            }
                            StaticVariables.size = value;
                        }catch(ClassCastException e){
                            e.printStackTrace();
                            StaticVariables.size = 0;
                        }
                        Log.e(TAG_VERSION, "Size: " + StaticVariables.size);
                    }
                }
                if(isChangedVersion){
                    patchCnt = 0;
                }
                System.out.println(" ============getApiCode2============result: " + codeList);

                FirebaseStorage storage = FirebaseStorage.getInstance("gs://kococo-2996f.appspot.com/");
                storage.setMaxDownloadRetryTimeMillis(60000);  // 1분 지나면 실패
                StorageReference storageRef = storage.getReference();
                StorageReference pathReference = storageRef.child("libs/SoundAnalysis_" + StaticVariables.version + ".jar");
                File path = new File(activity.getFilesDir()+"/libs");
                Log.e(TAG_VERSION,"jar가 다운받아질 경로: "+path.getAbsolutePath());
                File[] files = path.listFiles();
                String filename = "";
                int tmpSizeForChk = 0;

                final File tmpDir = activity.getDir("dex", 0);
                File[] files2 = tmpDir.listFiles();
                for (int i = 0; i < files2.length; i++) {
                    Log.e(TAG_VERSION,files2[i].getAbsolutePath()+" asdasd");
                }

                if (StaticVariables.version == null || StaticVariables.version.equals("")) {

                } else {
                    if (files != null) {
                        for (int i = 0; i < files.length; i++) {
                            filename = files[i].getName();
                            Log.e(TAG_VERSION, new File(path + "/" + filename).getAbsolutePath());
                            if (filename.indexOf("jar") > -1) {
                                Log.e(TAG_VERSION, "jar checking: " + filename);
                                if (filename.indexOf(StaticVariables.version) > -1) {
                                    Log.e(TAG_VERSION, "name checking: " + filename);
                                    tmpSizeForChk = (int) new File(path + "/" + filename).length();
                                    Log.e(TAG_VERSION, "size checking: " + tmpSizeForChk + "vs" + StaticVariables.size);
                                    if (tmpSizeForChk == StaticVariables.size) {
                                        isCorrectPatch = true;
                                        break;
                                    }
                                }
                            }

                            if (!isCorrectPatch) {
                                if (!filename.equals("")) {
                                    new File(path + "/" + filename).delete();

                                }
                                filename = "";
                            }
                        }
                    }
                }
                progressBarTxt.setVisibility(View.VISIBLE);
                if(isCorrectPatch==false && patchCnt>3){
                    progressBarTxt.setText("패치 서버에 오류가 있어 구 버전의 앱을 실행합니다. 녹음결과 분석 중 수정안된 오차가 있을 수 있습니다.");
                    confirmBtn.setVisibility(View.VISIBLE);
                }else {
                    if (isCorrectPatch) {
                        Log.e(TAG_VERSION, "정상 버전임(version: " + StaticVariables.version + "): " + filename);
//                        progressBarTxt.setText("최신 버전입니다");
//                        confirmBtn.setVisibility(View.VISIBLE);
                        progressOFF();
                    } else {
                        Log.e(TAG_VERSION, "최신 버전이 아닙니다.(version: " + StaticVariables.version + "): " + (filename.equals("") ? "jar가 없음":filename));
                        progressDialog.show();
                        progressBarTxt.setText("최신 버전이 아님으로 업데이트를 진행합니다.");
                        patchCnt++;
                        //데이터 와이파이인지의 여부를 물어봐야하는 곳
                        wifiCheck(path, pathReference, StaticVariables.version,activity);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }


    private void patchDownload(File path, StorageReference pathReference, String version, Activity activity){
        progressBarTxt = (TextView) progressDialog.findViewById(R.id.progressTxt) ;
        try {
            File file = new File(path, "SoundAnalysis_" + version + ".jar");
            if (!path.exists()) {
                path.mkdir();
            }
            FileDownloadTask fileDownloadTask = pathReference.getFile(file);

            //file size를 미리 가져와서 다운로드된 file size와 비교한다.
            fileDownloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                    try {
                        progressBarTxt.setVisibility(View.VISIBLE);
                        //long total = task.getResult().getTotalByteCount();
                        long total = StaticVariables.size;
                        long trans = task.getResult().getBytesTransferred();
                        Log.e(TAG_VERSION, String.format("onComplete: bytes=%d total=%d", trans, total));
                        if (task.isSuccessful() && total == trans) {
                            patchDownloadSuccessful = true;
                            Log.e(TAG_VERSION, "다운로드 완료: 성공" + file.getPath());
                        } else {
                            patchDownloadSuccessful = false;
                            Log.e(TAG_VERSION, "다운로드 완료: 실패 " + task!=null && task.getException()!=null ? task.getException().getMessage(): "사이즈가 맞지 않음. 다운받은 사이즈: "+trans+", 정상 사이즈: " +total);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        progressBarTxt.setText("업데이트 중 오류가 발생했습니다.");
                    }
                    Log.e(TAG_VERSION, "patchCnt: "+patchCnt+" patchDownloadSuccessful: "+patchDownloadSuccessful);
                    if (patchDownloadSuccessful == true) {
                        Log.e(TAG_VERSION, "patch가 성공했습니다. ");
                        progressBarTxt.setText("업데이트가 성공하였습니다. 적용을 위해 앱을 재시작 합니다."); //재시작은 아니고 현재 activity를 재시작함
                    } else {
                        Log.e(TAG_VERSION, "patch가 실패했습니다. ");
                        progressBarTxt.setText("업데이트가 실패하였습니다. 업데이트를 재시도 합니다. 재시도 횟수: "+ patchCnt);
                    }
                    Intent intent = activity.getIntent();
                    activity.finish();
                    activity.startActivity(intent);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //다운로드 실패
                    patchDownloadSuccessful = false;
                    Log.e(TAG_VERSION, "다운로드 실패: " + exception.getMessage());
                    exception.printStackTrace();
                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                //진행상태 표시
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    patchDownloadInProgress = true;
                    int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                    Log.e(TAG_VERSION, "다운로드 진행 중: " + progress + String.format("(bytes=%d total=%d)", taskSnapshot.getBytesTransferred(), taskSnapshot.getTotalByteCount()));

//                    progressBarTxt.setText("다운로드 진행 중: " + progress + " "+String.format("(bytes=%d total=%d)", taskSnapshot.getBytesTransferred(), taskSnapshot.getTotalByteCount()));
                    if(isWiFi){
                        progressBarTxt.setText(
                                "패치 데이터 다운로드 중입니다.\n"
                                +"("+getFileSize(taskSnapshot.getBytesTransferred())+"/"+getFileSize((long) StaticVariables.size)+")"
                        );
                    }else{
                        progressBarTxt.setText(
                                "패치 받을 데이터가 있습니다. 데이터의 용량은 "+getFileSize((long) StaticVariables.size)+"입니다. "
                                        +"Wifi연결이 안되어 있는데 그래도 데이터 다운로드 하시겠습니까?\n"
                                        +"("+getFileSize(taskSnapshot.getBytesTransferred())+"/"+getFileSize((long) StaticVariables.size)+")"
                        );
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    void wifiCheck(File path, StorageReference pathReference, String version, Activity activity)
    {
        isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        confirmBtn.setVisibility(View.INVISIBLE);

//        if(!isWiFi){
        //TODO 테스트데이터
        if(isWiFi){
            progressBarTxt.setText(
                    "패치 받을 데이터가 있습니다. 데이터의 용량은 "+getFileSize((long) StaticVariables.size)+"입니다. "
                            +"Wifi연결이 안되어 있는데 그래도 데이터 다운로드 하시겠습니까?\n"
                            +"("+0+"/"+getFileSize((long) StaticVariables.size)+")"
            );
            noBtn.setVisibility(View.VISIBLE);
            yesBtn.setVisibility(View.VISIBLE);
            yesBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread() {
                        public void run() {
                            patchDownload(path, pathReference, StaticVariables.version,activity);
                        }
                    }.start();
                }
            });
            noBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noBtn.setVisibility(View.INVISIBLE);
                    yesBtn.setVisibility(View.INVISIBLE);
                    progressBarTxt.setText("기존 패치 데이터 그대로 사용합니다.");
                    confirmBtn.setVisibility(View.VISIBLE);
                    patchCnt = 0;
                }
            });
        }else{
            new Thread() {
                public void run() {
                    patchDownload(path, pathReference, StaticVariables.version,activity);
                }
            }.start();
        }
    }

    public String getFileSize(Long size)
    {
        DecimalFormat df = new DecimalFormat("0.00");

        float sizeKb = 1024.0f;
        float sizeMb = sizeKb * sizeKb;
        float sizeGb = sizeMb * sizeKb;
        float sizeTerra = sizeGb * sizeKb;

        if(size < sizeMb)
            return df.format(size / sizeKb)+ " Kb";
        else if(size < sizeGb)
            return df.format(size / sizeMb) + " Mb";
        else if(size < sizeTerra)
            return df.format(size / sizeGb) + " Gb";

        return "";
    }


}
