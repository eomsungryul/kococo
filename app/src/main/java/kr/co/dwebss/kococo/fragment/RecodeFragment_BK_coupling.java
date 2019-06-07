package kr.co.dwebss.kococo.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.util.AudioAnalysisUtil;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


//        1. 마이크를 킨다.
//        2. 녹음 분석을 시작한다.
//        3. 마이크에서 오는 소리를 분석한다.
//        3-1. 마이크의 소리가 평균데시벨보다 높을 시 녹음을 시작한다.
//        3-2 마이크의 소리가 평균데시벨로 왔을경우 녹음을 중지하고
//        3-3 녹음파일코골이가 발생했는지 체크해서 녹음된 파일의 코골이 유무를 결정한다.
//        3-4 이갈이, 무호흡이 발생하면 녹음된 구간을 저장한다.
//        4. 분석종료시  마이크를 끈다.
//        5. 녹음파일 데이터를 기기에 저장한다.
//        6. 녹음분석 데이터를 서버에 보낸다.

public class RecodeFragment_BK_coupling extends Fragment  {
//
//    Boolean recodeFlag = false;
//    private String LOG_TAG = "Audio_Recording";
//
//    private int requestCodeP = 0;
//
//    private static final int REQUEST_MICROPHONE = 3;
//    private static final int REQUEST_EXTERNAL_STORAGE = 2;
//    private static final int REQUEST_CAMERA = 1;
//
//    //재생할때 필요한
//    MediaPlayer mediaPlayer;
//    Boolean testFlag = false;
//
//
//    Retrofit retrofit;
//    ApiService apiService;
//
//    //request 데이터 모음
//    JsonObject recordData;
//    String userAppId;
//    String recordStartDt;
//    String recordEndDt;
//    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//
//    //녹음 모듈
//    AudioAnalysisUtil aau;
//
//    public RecodeFragment_BK_coupling() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//
//        View v = inflater.inflate(R.layout.fragment_record, container, false);
//        Button recodeBtn = (Button) v.findViewById(R.id.recodeBtn) ;
//        recodeFlag = false;
//        recodeBtn.setText("녹음 시작");
//
//
//        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
//        apiService = retrofit.create(ApiService.class);
//
//
//
//        String path = getContext().getFilesDir().getAbsolutePath();
//        //path 부분엔 파일 경로를 지정해주세요.
//        File files = new File(path+"/appId.txt");
//        //get app Id
//        StringBuffer buffer = new StringBuffer();
//        String data = null;
//        FileInputStream fis = null;
//        try {
//            fis = getContext().openFileInput("appId.txt");
//            BufferedReader iReader = new BufferedReader(new InputStreamReader((fis)));
//            userAppId = iReader.readLine();
//            iReader.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Toast.makeText(getActivity(), "userAppId :  "+userAppId, Toast.LENGTH_SHORT).show();
//
//
//        aau = new AudioAnalysisUtil();
//        //xml 내에서 onclick으로 가능하다. 하지만 그건 activity 내에서만 가능하고 프래그먼트에서는 onclickListener()로 해야함
//
//        //녹음 시작과 종료 기능
//        recodeBtn.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                //녹음 시작일 경우
//                if( recodeFlag == false){
//                    String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};
//
//                    int permissionReadStorage = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
//                    int permissionWriteStorage = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                    int permissionAudio = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO);
//                    if(permissionReadStorage == PackageManager.PERMISSION_DENIED || permissionWriteStorage == PackageManager.PERMISSION_DENIED||permissionAudio == PackageManager.PERMISSION_DENIED) {
//                        Toast.makeText(getActivity(), " 권한 허가를 해야합니다.", Toast.LENGTH_SHORT).show();
//                        requestPermissions(permissions, REQUEST_EXTERNAL_STORAGE);
//                        return;
//                    } else {
////                        Toast.makeText(getActivity(), "permission 승인", Toast.LENGTH_SHORT).show();
//                        Log.e(LOG_TAG, "permission 승인");
//                        recodeBtn.setText("녹음 종료");
//                        recodeFlag = true;
//                        //1. 녹음을 시작한다.
//                        aau.start(v);
//                        // 2. 녹음분석 쓰레드를 실행. 쓰레드에서는 녹음분석을 한다.
//                        //어떻게 쪼갤건지
//                        aau.Audio_Recording(v);
//
//                    }
//                }else{
//
//                    recodeFlag = false;
//                    // 3. 녹음 종료버튼 클릭 시 녹음된파일이 저장됨
//                    recordData = aau.stop(v);
//                    // 4. 반환된 데이터를 녹음기록데이터들을 서버에 보냄
//                    RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
//
//                    System.out.println(" ================2123123=======response: "+recordData.toString());
//                    //POST /api/record를 호출한다.
////                    apiService.addRecord(requestData).enqueue(new Callback<RequestBody>() {
////                        @Override
////                        public void onResponse(Call<RequestBody> call, Response<RequestBody> response) {
////                            // 5.  리턴 후에 녹음 결과창을 띄움
////                            startActivity(new Intent(getActivity(), ResultActivity.class));
////                            System.out.println(" ================녹음종료=======response: "+response.body());
////                        }
////                        @Override
////                        public void onFailure(Call<RequestBody> call, Throwable t) {
////
////                        }
////                    });
//
//
//                    recodeBtn.setText("녹음 시작");
//                }
//            }
//        });
//
//        Button testBtn = (Button) v.findViewById(R.id.testBtn) ;
//        testBtn.setText("시작");
//        //xml 내에서 onclick으로 가능하다. 하지만 그건 activity 내에서만 가능하고 프래그먼트에서는 onclickListener()로 해야함
//        testBtn.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if( testFlag == false){
//                    testBtn.setText("종료");
//                    testFlag = true;
//                    mediaPlayer = MediaPlayer.create(getActivity(), R.raw.queen);
//                    //구간 재생
//                    mediaPlayer.seekTo(15000);
//                    mediaPlayer.getCurrentPosition();
//                    mediaPlayer.start();
//                    int endTime =21000;
//                    //카운트 다운
//                    new CountDownTimer(endTime, 100) {
//                        public void onTick(long millisUntilFinished) {
//
//                            if(MediaPlayerUtility.getTime(mediaPlayer)>=endTime){
//                                mediaPlayer.stop();
//                                // 초기화
//                                mediaPlayer.reset();
//                                testFlag = false;
//                                testBtn.setText("시작");
//                            }
//                        }
//                        public void onFinish() {
//                            testBtn.setText("시작2");
//                        }
//                    }.start();
//                }else{
//                    testFlag = false;
//                    testBtn.setText("시작");
//                    // 정지버튼
//                    mediaPlayer.stop();
//                    // 초기화
//                    mediaPlayer.reset();
//                }
//            }
//        });
//
//        return v;
//    }
//
//
//
//    // MediaPlayer는 시스템 리소스를 잡아먹는다.
//    // MediaPlayer는 필요이상으로 사용하지 않도록 주의해야 한다.
//    //Fragment에서는 onDestoryView , activity에서는 onDestory
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        // MediaPlayer 해지
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case REQUEST_CAMERA:
//                for (int i = 0; i < permissions.length; i++) {
//                    String permission = permissions[i];
//                    int grantResult = grantResults[i];
//                    if (permission.equals(Manifest.permission.CAMERA)) {
//                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
//                            Toast.makeText(getActivity(), "camera permission 승인", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getActivity(), "camera permission denied", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
//                break;
//            case REQUEST_EXTERNAL_STORAGE:
//                for (int i = 0; i < permissions.length; i++) {
//                    String permission = permissions[i];
//                    int grantResult = grantResults[i];
//                    if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
//                            Toast.makeText(getActivity(), "read/write storage permission 승인", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getActivity(), "read/write storage permission denied", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
//                break;
//            case REQUEST_MICROPHONE:
//                for (int i = 0; i < permissions.length; i++) {
//                    String permission = permissions[i];
//                    int grantResult = grantResults[i];
//                    if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
//                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
//                            Toast.makeText(getActivity(), "audio permission 승인", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getActivity(), "audio permission denied", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
//                break;
//        }
//    }
//


}
