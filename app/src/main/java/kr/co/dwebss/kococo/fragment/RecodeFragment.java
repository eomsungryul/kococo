package kr.co.dwebss.kococo.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.MainActivity;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.fragment.recorder.Analysis;
import kr.co.dwebss.kococo.fragment.recorder.AnalysisDetails;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.ApiCode;
import kr.co.dwebss.kococo.util.AudioCalculator;
import kr.co.dwebss.kococo.util.MediaPlayerUtility;
import kr.co.dwebss.kococo.util.WaveFormatConverter;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RecodeFragment extends Fragment  {

    Boolean recodeFlag = false;
    private String LOG_TAG = "Audio_Recording";
    private static AudioRecord record;

    //오디오 품질 고정값
    private int sampleRate = 44100;
    private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private int requestCodeP = 0;

    private static final int REQUEST_MICROPHONE = 3;
    private static final int REQUEST_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_CAMERA = 1;

    //녹음 관련
    private String LOG_TAG2 = "Audio_Recording2";
    int state = 0;
    private boolean mShouldContinue = true;
    private AudioCalculator audioCalculator;
    int frameByteSize = 1024;
    static List<StartEnd> snoringTermList;
    public static List<StartEnd> osaTermList;
    static List<StartEnd> grindingTermList;

    boolean isRecording = false;
    ByteArrayOutputStream baos;
    //재생할때 필요한
    MediaPlayer mediaPlayer;
    Boolean testFlag = false;

    byte[] audioData = new byte[frameByteSize];

    Retrofit retrofit;
    ApiService apiService;


    //request 데이터 모음
    JsonObject recordData;
    String userAppId;
    String recordStartDt;
    String recordEndDt;
    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public RecodeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_recode, container, false);
        Button recodeBtn = (Button) v.findViewById(R.id.recodeBtn) ;
        recodeFlag = false;
        recodeBtn.setText("녹음 시작");


        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);



        String path = getContext().getFilesDir().getAbsolutePath();
        //path 부분엔 파일 경로를 지정해주세요.
        File files = new File(path+"/appId.txt");
        //get app Id
        StringBuffer buffer = new StringBuffer();
        String data = null;
        FileInputStream fis = null;
        try {
            fis = getContext().openFileInput("appId.txt");
            BufferedReader iReader = new BufferedReader(new InputStreamReader((fis)));
            userAppId = iReader.readLine();
            iReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(getActivity(), "userAppId :  "+userAppId, Toast.LENGTH_SHORT).show();


        //xml 내에서 onclick으로 가능하다. 하지만 그건 activity 내에서만 가능하고 프래그먼트에서는 onclickListener()로 해야함
        recodeBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( recodeFlag == false){
                    String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};

                    int permissionReadStorage = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
                    int permissionWriteStorage = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    int permissionAudio = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO);
                    if(permissionReadStorage == PackageManager.PERMISSION_DENIED || permissionWriteStorage == PackageManager.PERMISSION_DENIED||permissionAudio == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(getActivity(), " permission x", Toast.LENGTH_SHORT).show();
                        requestPermissions(permissions, REQUEST_EXTERNAL_STORAGE);
                        return;
                    } else {
                        //녹음 시작 클릭
                        Toast.makeText(getActivity(), "permission 승인", Toast.LENGTH_SHORT).show();
                        Log.e(LOG_TAG, "permission 승인");
                        recodeBtn.setText("녹음 종료");
                        recodeFlag = true;
                        recordStartDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
                        start();
                    }
                }else{
                    //창 띄우기
                    startActivity(new Intent(getActivity(), ResultActivity.class));
                    recordEndDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
                    recodeFlag = false;
                    stop(v);
                    recodeBtn.setText("녹음 시작");
                }
            }
        });
        // Inflate the layout for this




        Button testBtn = (Button) v.findViewById(R.id.testBtn) ;

        testBtn.setText("시작");

        //xml 내에서 onclick으로 가능하다. 하지만 그건 activity 내에서만 가능하고 프래그먼트에서는 onclickListener()로 해야함
        testBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( testFlag == false){
                    testBtn.setText("종료");
                    testFlag = true;
                    mediaPlayer = MediaPlayer.create(getActivity(), R.raw.queen);
                    //구간 재생
                    mediaPlayer.seekTo(15000);
                    mediaPlayer.getCurrentPosition();
                    mediaPlayer.start();
                    int endTime =21000;
                    //카운트 다운
                    new CountDownTimer(endTime, 100) {
                        public void onTick(long millisUntilFinished) {

                            if(MediaPlayerUtility.getTime(mediaPlayer)>=endTime){
                                mediaPlayer.stop();
                                // 초기화
                                mediaPlayer.reset();
                                testFlag = false;
                                testBtn.setText("시작");
                            }
                        }
                        public void onFinish() {
                            testBtn.setText("시작2");
                        }
                    }.start();
                }else{
                    testFlag = false;
                    testBtn.setText("시작");
                    // 정지버튼
                    mediaPlayer.stop();
                    // 초기화
                    mediaPlayer.reset();
                }
            }
        });
        // Inflate the layout for this


        return v;


//        java.lang.IllegalStateException: Could not find method recordClick(View) in a parent or ancestor Context for android:onClick attribute defined on view class android.support.v7.widget.AppCompatButton with id recodeBtn

    }



    // MediaPlayer는 시스템 리소스를 잡아먹는다.
    // MediaPlayer는 필요이상으로 사용하지 않도록 주의해야 한다.
    //Fragment에서는 onDestoryView , activity에서는 onDestory
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // MediaPlayer 해지
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }



    public void start() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        //int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        int recBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding);;
        /*if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }*/
        //short[] audioBuffer = new short[bufferSize / 2];
        //record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, frameByteSize);
        //record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        record = new AudioRecord(MediaRecorder.AudioSource.MIC,  sampleRate, channelConfiguration, audioEncoding, recBufSize);
        Log.e(LOG_TAG, record.getState()+" ");
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Cannot be Recorded");
            return;
        }
        String permission = "android.permission.RECORD_AUDIO";
        int result  = getActivity().checkCallingOrSelfPermission(permission);
        record.startRecording();
        int recordingState = record.getRecordingState();
        Log.e(RecodeFragment.class.getSimpleName(), "RecordingState() after startRecording() = " + String.valueOf(recordingState));
        if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(RecodeFragment.class.getSimpleName(), "AudioRecord error has occured. Reopen app.");
            //System.exit(0);
        }
        Log.v(LOG_TAG, "Recording has started");
        mShouldContinue = true;

        Audio_Recording();
        state = 1;
//        Toast.makeText(getActivity(), "Started Recording", Toast.LENGTH_SHORT).show();
    }

    public void stop(View v) {
        state = 0;
        mShouldContinue = false;
        record.stop();
        record.release();
        record = null;
//        Toast.makeText(getActivity(), "stopped Recording", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getActivity(), "camera permission 승인", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "camera permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            case REQUEST_EXTERNAL_STORAGE:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getActivity(), "read/write storage permission 승인", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "read/write storage permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            case REQUEST_MICROPHONE:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getActivity(), "audio permission 승인", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "audio permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }



    void Audio_Recording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long shortsRead = 0;

                byte[] frameBytes = new byte[frameByteSize];
                audioCalculator = new AudioCalculator();

                SleepCheck.checkTerm = 0;
                SleepCheck.checkTermSecond = 0;
                int osaCnt = 0;
                boolean grindingStart = false;
                boolean grindingContinue = false;
                int grindingRecordingContinueCnt = 0;
                boolean osaStart = false;
                boolean osaContinue = false;
                int osaRecordingExit = 0;
                int osaRecordingContinueCnt = 0;
                double osaStartTimes = 0.0;
                SleepCheck.grindingContinueAmpCnt = 0;
                SleepCheck.grindingContinueAmpOppCnt = 0;
                SleepCheck.grindingRepeatAmpCnt = 0;
                @SuppressWarnings("unused")
                long recordStartingTIme = 0L;
                snoringTermList = new ArrayList<StartEnd>();
                grindingTermList = new ArrayList<StartEnd>();
                osaTermList = new ArrayList<StartEnd>();
//                List<Analysis> ansList = new ArrayList<Analysis>();
                JsonArray ansList = new JsonArray();
                int read = 0;

                double times=0.0;
                int i = 1;
                while (mShouldContinue) {
                    int numberOfShort = record.read(audioData, 0, audioData.length);
                    shortsRead += numberOfShort;
                    frameBytes = audioData;

                    try {
                        audioCalculator.setBytes(frameBytes);
                        // 소리가 발생하면 녹음을 시작하고, 1분이상 소리가 발생하지 않으면 녹음을 하지 않는다.
                        int amplitude = audioCalculator.getAmplitude();
                        double decibel = audioCalculator.getDecibel();
                        double frequency = audioCalculator.getFrequency();
                        double sefrequency = audioCalculator.getFrequencySecondMax();
                        int sefamplitude = audioCalculator.getAmplitudeNth(audioCalculator.getFreqSecondN());

                        times = (((double) (frameBytes.length / (44100d * 16 * 1))) * 8) * i;
                        i++;
                        SleepCheck.curTermSecond = (int) Math.floor(times);

                        final String amp = String.valueOf(amplitude + "Amp");
                        final String db = String.valueOf(decibel + "db");
                        final String hz = String.valueOf(frequency + "Hz");
                        final String sehz = String.valueOf(sefrequency + "Hz(2th)");
                        final String seamp = String.valueOf(sefamplitude + "Amp(2th)");

                        System.out.println(String.format("%.2f", times)+"s "+hz +" "+db+" "+amp+" "+sehz+" "+seamp);
                        // 소리의 발생은 특정 db 이상으로한다. 데시벨은 -31.5~0 으로 수치화 하고 있음.
                        // -10db에 안걸릴 수도 잇으니까, 현재 녹음 상태의 평균 데시벨값을 지속적으로 갱신하면서 평균 데시벨보다 높은 소리가 발생했는지 체크
                        // 한다.
                        // 평균 데시벨 체크는 3초 동안한다.
                        if (decibel > SleepCheck.NOISE_DB_INIT_VALUE && isRecording == false
                                && Math.floor((double) (audioData.length / (44100d * 16 * 1)) * 8) != Math.floor(times) //사운드 파일 테스트용
                        ) {
                            Log.v(LOG_TAG2,("녹음 시작! "));
                            Log.v(LOG_TAG2,(String.format("%.2f", times)+"s~"));
                            recordStartingTIme = System.currentTimeMillis();
                            baos = new ByteArrayOutputStream();
                            isRecording = true;
                        } else if (isRecording == true && (SleepCheck.noiseCheck(decibel)==0 || recodeFlag==false) ) {
                            Log.v(LOG_TAG2,("녹음 종료! "));
                            Log.v(LOG_TAG2,(String.format("%.2f", times)+"s "));
                            SimpleDateFormat dayTime = new SimpleDateFormat("yyyymmdd_hhmm");
                            String fileName = dayTime.format(new Date(recordStartingTIme));
                            dayTime = new SimpleDateFormat("dd_hhmm");
                            long time = System.currentTimeMillis();
                            fileName += "-" + dayTime.format(new Date(time));
                            byte[] waveData = baos.toByteArray();

                            //TODO 녹음된 파일이 저장되는 시점
                            WaveFormatConverter wfc = new WaveFormatConverter(44100, (short)1, waveData, 0, waveData.length-1);
                            String filePath = wfc.saveLongTermWave(fileName, getContext());

                            Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 시작====="));
                            Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (waveData.length / (44100d * 16 * 1))) * 8));

                            JsonObject ans = new JsonObject();
                            //ans.setAnalysisStartDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(recordStartingTIme), ZoneId.systemDefault()));
                            //ans.setAnalysisEndDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
                            ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                            ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                            ans.addProperty("analysisFileAppPath",filePath);
                            ans.addProperty("analysisFileNm","event-"+fileName+"_"+System.currentTimeMillis()+".wav");
                            JsonArray ansDList = new JsonArray();
                            JsonObject ansd = new JsonObject();
                            for(StartEnd se : snoringTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200101);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                ansDList.add(ansd);
                            }
                            for(StartEnd se : grindingTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200102);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                ansDList.add(ansd);
                            }
                            for(StartEnd se : osaTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200103);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                ansDList.add(ansd);
                            }
                            ans.add("analysisDetailsList", ansDList);
                            ansList.add(ans);
                            //여기까지 AnswerList를 계속 생성한다.
                            //녹음 중단 했을 떄 AnswerList를 Record의 AnswerList필드에 담는다.
                            //Record의 recordStartDt, recordEndDt에 녹음 시작시간과 녹음 종료 시간을, userAppId에는 사용자 앱 ID를 입력 해서 VO를 완성한다.
                            //최종 완료 vo 형태
                            /*
                            {
                                "userAppId" : "c0362dd4-97f4-488c-b31c-12cb23b534cf",
                                    "recordStartDt" : "2019-05-24T12:00:16.614",
                                    "recordEndDt" : "2019-05-24T20:00:16.614",
                                    "analysisList" : [ {
                                "analysisStartDt" : "2019-05-24T12:00:16.613",
                                        "analysisEndDt" : "2019-05-24T15:00:16.613",
                                        "analysisFileNm" : "2019-05-24T12:00:16.613_testFileNm.wav",
                                        "analysisFileAppPath" : "/rec_data/",
                                        "analysisDetailsList" : [ {
                                    "termTypeCd" : 200101,
                                            "termStartDt" : "2019-05-24T12:00:26.612",
                                            "termEndDt" : "2019-05-24T12:02:20.613"
                                }, {
                                    "termTypeCd" : 200102,
                                            "termStartDt" : "2019-05-24T12:08:48.613",
                                            "termEndDt" : "2019-05-24T13:33:48.613"
                                }, {
                                    "termTypeCd" : 200103,
                                            "termStartDt" : "2019-05-24T14:21:10.613",
                                            "termEndDt" : "2019-05-24T15:22:40.613"
                                } ]
                            } ]
                            }
                            */
                            recordData = new JsonObject();
                            recordData.addProperty("userAppId",userAppId);
                            recordData.addProperty("recordStartDt",recordStartDt);
                            recordData.addProperty("recordEndDt",recordEndDt);
                            recordData.add("analysisList", ansList);

                            System.out.println(" ========================recordData: "+recordData.toString());
                            RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
                            //POST /api/record를 호출한다.
                            apiService.addRecord(requestData).enqueue(new Callback<RequestBody>() {
                                @Override
                                public void onResponse(Call<RequestBody> call, Response<RequestBody> response) {
                                    Toast.makeText(getActivity(), "sucess"+response,
                                            Toast.LENGTH_SHORT).show();
                                    System.out.println(" ========================response: "+response.body().toString());
                                }

                                @Override
                                public void onFailure(Call<RequestBody> call, Throwable t) {

                                }
                            });

							/*
							System.out.println("analysisStartDt: "+dayTimeT.format(new Date(recordStartingTIme)));
							System.out.println("analysisEndDt: "+dayTimeT.format(new Date(time)));
							System.out.println("analysisFileNm: "+"event-"+fileName+"_"+System.currentTimeMillis()+".wav");
							System.out.println("analysisFileAppPath: raw/raw_convert/");
							System.out.println("analysisDetailsList 시작, 리스트, 길이: "+snoringTermList.size()+ grindingTermList.size()+osaTermList.size());
							for(StartEnd se : snoringTermList) {
								System.out.println(se.getTermForRequest(200101, recordStartingTIme));
							}
							for(StartEnd se : grindingTermList) {
								System.out.println(se.getTermForRequest(200102, recordStartingTIme));
							}
							for(StartEnd se : osaTermList) {
								System.out.println(se.getTermForRequest(200103, recordStartingTIme));
							}
							*/
                            Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 끝====="));
                            recordStartingTIme = 0;
                            isRecording = false;
                        }
                        else if(isRecording == true && Math.floor((double) (audioData.length / (44100d * 16 * 1)) * 8) == Math.floor(times)){
                            Log.v(LOG_TAG2,("녹음 종료!(사운드 파일 테스트용) "));
                            Log.v(LOG_TAG2,(String.format("%.2f", times)+"s "));
                            SimpleDateFormat dayTime = new SimpleDateFormat("yyyymmdd_hhmm");
                            String fileName = dayTime.format(new Date(recordStartingTIme));
                            dayTime = new SimpleDateFormat("dd_hhmm");
                            long time = System.currentTimeMillis();
                            fileName += "-" + dayTime.format(new Date(time));
                            byte[] waveData = baos.toByteArray();

                            WaveFormatConverter wfc = new WaveFormatConverter(44100, (short)1, waveData, 0, waveData.length);
                            String filePath = wfc.saveLongTermWave(fileName,getContext());
                            SimpleDateFormat dayTimeT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 시작====="));
                            Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (waveData.length / (44100d * 16 * 1))) * 8));

                            JsonObject ans = new JsonObject();
                            //ans.setAnalysisStartDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(recordStartingTIme), ZoneId.systemDefault()));
                            //ans.setAnalysisEndDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
                            ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                            ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                            ans.addProperty("analysisFileAppPath",filePath);
                            ans.addProperty("analysisFileNm","event-"+fileName+"_"+System.currentTimeMillis()+".wav");
                            JsonArray ansDList = new JsonArray();
                            JsonObject ansd = new JsonObject();
                            for(StartEnd se : snoringTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200101);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                ansDList.add(ansd);
                            }
                            for(StartEnd se : grindingTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200102);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                ansDList.add(ansd);
                            }
                            for(StartEnd se : osaTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200103);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                ansDList.add(ansd);
                            }
                            ans.add("analysisDetailsList", ansDList);
                            ansList.add(ans);
                            //여기까지 AnswerList를 계속 생성한다.
                            //녹음 중단 했을 떄 AnswerList를 Record의 AnswerList필드에 담는다.
                            //Record의 recordStartDt, recordEndDt에 녹음 시작시간과 녹음 종료 시간을, userAppId에는 사용자 앱 ID를 입력 해서 VO를 완성한다.
                            //최종 완료 vo 형태
                            /*
                            {
                                "userAppId" : "c0362dd4-97f4-488c-b31c-12cb23b534cf",
                                    "recordStartDt" : "2019-05-24T12:00:16.614",
                                    "recordEndDt" : "2019-05-24T20:00:16.614",
                                    "analysisList" : [ {
                                "analysisStartDt" : "2019-05-24T12:00:16.613",
                                        "analysisEndDt" : "2019-05-24T15:00:16.613",
                                        "analysisFileNm" : "2019-05-24T12:00:16.613_testFileNm.wav",
                                        "analysisFileAppPath" : "/rec_data/",
                                        "analysisDetailsList" : [ {
                                    "termTypeCd" : 200101,
                                            "termStartDt" : "2019-05-24T12:00:26.612",
                                            "termEndDt" : "2019-05-24T12:02:20.613"
                                }, {
                                    "termTypeCd" : 200102,
                                            "termStartDt" : "2019-05-24T12:08:48.613",
                                            "termEndDt" : "2019-05-24T13:33:48.613"
                                }, {
                                    "termTypeCd" : 200103,
                                            "termStartDt" : "2019-05-24T14:21:10.613",
                                            "termEndDt" : "2019-05-24T15:22:40.613"
                                } ]
                            } ]
                            }
                            */
                            //POST /api/record를 호출한다.
                            recordData = new JsonObject();
                            recordData.addProperty("userAppId",userAppId);
                            recordData.addProperty("recordStartDt",recordStartDt);
                            recordData.addProperty("recordEndDt",recordEndDt);
                            recordData.add("analysisList", ansList);

//                            System.out.println(" ========================recordData: "+recordData.toString());
                            RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
                            //POST /api/record를 호출한다.
                            apiService.addRecord(requestData).enqueue(new Callback<RequestBody>() {
                                @Override
                                public void onResponse(Call<RequestBody> call, Response<RequestBody> response) {
                                    Toast.makeText(getActivity(), "sucess"+response,
                                            Toast.LENGTH_SHORT).show();
//                                    System.out.println(" ========================response: "+response.body().toString());
                                }

                                @Override
                                public void onFailure(Call<RequestBody> call, Throwable t) {

                                }
                            });



							/*
                            System.out.println("analysisStartDt: "+dayTimeT.format(new Date(recordStartingTIme)));
                            System.out.println("analysisEndDt: "+dayTimeT.format(new Date(time)));
                            System.out.println("analysisFileNm: "+"event-"+fileName+"_"+System.currentTimeMillis()+".wav");
                            System.out.println("analysisFileAppPath: raw/raw_convert/");
                            System.out.println("analysisDetailsList 시작, 리스트, 길이: "+snoringTermList.size()+ grindingTermList.size()+osaTermList.size());
                            for(StartEnd se : snoringTermList) {
                                System.out.println(se.getTermForRequest(200101, recordStartingTIme));
                            }
                            for(StartEnd se : grindingTermList) {
                                System.out.println(se.getTermForRequest(200102, recordStartingTIme));
                            }
                            for(StartEnd se : osaTermList) {
                                System.out.println(se.getTermForRequest(200103, recordStartingTIme));
                            }
							*/
                            Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 끝====="));
                            recordStartingTIme = 0;
                            isRecording = false;
                        }

                        if (i == 1 || isRecording == false) {
                            continue;
                        }
                        baos.write(frameBytes);
						/*
						System.out.print("녹음 중! ");
						Log.v(LOG_TAG2,(String.format("%.2f", times)+"s ");
						*/

                        // 녹음이 끝나고 나면 코골이가 발생했는지를 체크해서 녹음된 파일의 코골이 유무를 결정한다. X
                        // 코골이 여부를 체크한다.
                        int snoreChecked = SleepCheck.snoringCheck(decibel, frequency, sefrequency);
                        if(snoreChecked==1) {
                            if(snoringTermList.size()>0) {
                                double beforeTime = snoringTermList.get(snoringTermList.size()-1).start;
                                if(Math.floor(beforeTime)+100<Math.floor(times)) {
                                    snoringTermList.add(new StartEnd());
                                    snoringTermList.get(snoringTermList.size()-1).start=times;
                                    snoringTermList.get(snoringTermList.size()-1).end=times;
                                }
                            }else {
                                snoringTermList.add(new StartEnd());
                                snoringTermList.get(0).start=times;
                                snoringTermList.get(0).end=times;
                            }
                        }
                        // 이갈이는 기존 로직대로 체크해서, 어디 구간에서 발생했는지 체크한다.
                        SleepCheck.grindingCheck(times, decibel, sefamplitude, frequency, sefrequency);
                        // 이갈이 신호가 발생하고, 이갈이 체크 상태가 아니면 이갈이 체크를 시작한다.
                        if (SleepCheck.grindingRepeatAmpCnt == 1 && grindingStart == false) {
							/*
							System.out.print("이갈이 체크를 시작한다.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
									+ "s " + SleepCheck.grindingContinueAmpCnt + " "
									+ SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
							*/
                            grindingTermList.add(new StartEnd());
                            grindingTermList.get(grindingTermList.size()-1).start=times;
                            grindingStart = true;
                            grindingContinue = false;
                            // 이갈이 체크 중에 1초간격으로 유효 카운트가 연속적으로 발생했으면 계속 체크한다.
                        } else if (SleepCheck.curTermSecond - SleepCheck.checkTermSecond == 1
                                && SleepCheck.grindingRepeatAmpCnt >= 3 && grindingStart == true) {
                            if (((double) (audioData.length / (44100d * 16 * 1))) * 8 < times + 1) {
								/*
								System.out.print("이갈이 종료.");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
										+ "s " + SleepCheck.grindingContinueAmpCnt + " "
										+ SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
								*/
                                grindingTermList.get(grindingTermList.size()-1).end=times;
                                grindingStart = false;
                                grindingContinue = false;
                                grindingRecordingContinueCnt = 0;
                            }
							/*
							System.out.print("이갈이 중.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
									+ "s " + SleepCheck.grindingContinueAmpCnt + " "
									+ SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
							*/
                            grindingRecordingContinueCnt = 0;
                            grindingContinue = true;
                            // 이갈이 체크 중에 1초간격으로 유효 카운트가 연속적으로 발생하지 않으면 체크를 취소한다.
                        } else if (SleepCheck.curTermSecond - SleepCheck.checkTermSecond == 1
                                && SleepCheck.grindingRepeatAmpCnt == 0 && grindingStart == true
                                && grindingContinue == false) {
                            // 1초 단위 발생하는 이갈이도 잡기위해 유예 카운트를 넣는다. 1초만 한번더 체크함.
                            if (grindingRecordingContinueCnt >= SleepCheck.GRINDING_RECORDING_CONTINUE_CNT) {
								/*
								System.out.print("이갈이 아님, 체크 취소.");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
										+ "s " + SleepCheck.grindingContinueAmpCnt + " "
										+ SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
								*/
                                grindingTermList.remove(grindingTermList.size()-1);
                                grindingStart = false;
                                grindingRecordingContinueCnt = 0;
                            } else {
								/*
								System.out.print("이갈이 체크를 취소하지 않고 진행한다.(1초 유예)");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
										+ "s " + SleepCheck.grindingContinueAmpCnt + " "
										+ SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
								*/
                                grindingRecordingContinueCnt++;
                            }
                            // 이갈이 체크 중에 1초간격으로 유효카운트가 더이상 발생하지 않으나 이전에 발생했더라면 현재 체크하는 이갈이는 유효함.
                        } else if (SleepCheck.curTermSecond - SleepCheck.checkTermSecond == 1
                                && SleepCheck.grindingRepeatAmpCnt == 0 && grindingContinue == true) {
							/*
							System.out.print("이갈이 종료.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
									+ "s " + SleepCheck.grindingContinueAmpCnt + " "
									+ SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
							*/
                            grindingTermList.get(grindingTermList.size()-1).end=times;
                            grindingStart = false;
                            grindingContinue = false;
                            grindingRecordingContinueCnt = 0;
                        } else if (SleepCheck.curTermSecond - SleepCheck.checkTermSecond == 1) {
                            if (grindingStart) {
								/*
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "s 이갈이 중 " + grindingStart + " "
										+ grindingContinue + " " + grindingRecordingContinueCnt);
								*/
                            }
                        }
                        // 무호흡도 기존 로직대로 체크해서, 어디 구간에서 발생했는지 체크한다.
                        osaCnt = SleepCheck.OSACheck(times, decibel, sefamplitude, frequency, sefrequency);
                        osaRecordingContinueCnt += osaCnt;
                        // 무호흡 카운트가 발생하고, 체크 상태가 아니면 체크를 시작한다.
                        if (osaRecordingExit > 0) {
                            osaRecordingExit--;
                        }
                        if (osaCnt > 0 && osaStart == false) {
							/*
							System.out.print("무호흡 체크를 시작한다.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "s~" + SleepCheck.isOSATerm + " "
									+ SleepCheck.isBreathTerm + " " + SleepCheck.isOSATermCnt);
							*/
                            osaStart = true;
                            osaContinue = false;
                            osaRecordingExit = 0;
                            osaStartTimes = times;
                        } else if (times - osaStartTimes < 5 && osaStart == true) {
                            // 무호흡 녹음 중 5초 이내에 호흡이 발생하면, 무호흡이 아닌 것으로 본다.
                            if (osaRecordingContinueCnt < 5) {
								/*
								System.out.print("무호흡 체크 취소. " + osaRecordingContinueCnt + ", ");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheck.isOSATerm + " "
										+ SleepCheck.isBreathTerm + " " + SleepCheck.isOSATermCnt);
								*/
                                osaStart = false;
                                osaRecordingContinueCnt = 0;
                            } else {
                                if (((double) (audioData.length / (44100d * 16 * 1))) * 8 < times + 1) {
									/*
									System.out.print("무호흡 끝.");
									Log.v(LOG_TAG2,(
											String.format("%.2f", times) + "~" + String.format("%.2f", times + 1) + "s "
													+ SleepCheck.grindingContinueAmpCnt + " "
													+ SleepCheck.grindingContinueAmpOppCnt + " "
													+ SleepCheck.grindingRepeatAmpCnt);
									*/
                                    osaStart = false;
                                    osaRecordingContinueCnt = 0;
                                }
                                osaContinue = true;
								/*
								System.out.print("무호흡 중.");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheck.isOSATerm + " "
										+ SleepCheck.isBreathTerm + " " + SleepCheck.isOSATermCnt);
								*/
                            }
                            // 무호흡 녹음 중 5초 이 후에 소리가 발생하면, 다음 소리가 발생한 구간까지 체크한다.
                        } else if (times - osaStartTimes > 5 && osaStart == true) {
                            if (SleepCheck.isBreathTerm == true) { // 숨쉬는 구간이 되었으면, 체크 계속 플래그를 업데이트
                                if (((double) (audioData.length / (44100d * 16 * 1))) * 8 < times + 1) {
									/*
									System.out.print("무호흡 끝.");
									Log.v(LOG_TAG2,(
											String.format("%.2f", times) + "~" + String.format("%.2f", times + 1) + "s "
													+ SleepCheck.grindingContinueAmpCnt + " "
													+ SleepCheck.grindingContinueAmpOppCnt + " "
													+ SleepCheck.grindingRepeatAmpCnt);
									*/
                                    osaStart = false;
                                    osaRecordingContinueCnt = 0;
                                }
                                osaContinue = true;
								/*
								System.out.print("무호흡 중.2 ");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheck.isOSATerm + " "
										+ SleepCheck.isBreathTerm + " " + SleepCheck.isOSATermCnt);
								*/
                            } else {
                                if (osaContinue == true && osaRecordingExit == 1) {
									/*
									System.out.print("무호흡 끝.");
									Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
											+ String.format("%.2f", times + 0.01) + "s " + SleepCheck.isOSATerm + " "
											+ SleepCheck.isBreathTerm + " " + SleepCheck.isOSATermCnt);
									*/
                                    osaStart = false;
                                    osaRecordingContinueCnt = 0;
                                }
                                if (osaCnt > 0) {
                                    osaRecordingExit = 1000;
                                }
                                osaCnt = 0;
                            }
                        } else {
                            if (osaStart) {
								/*
								System.out.print("무호흡 중");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheck.isOSATerm + " "
										+ SleepCheck.isBreathTerm + " " + SleepCheck.isOSATermCnt);
								*/
                            }
                        }
                        SleepCheck.curTermTime = times;
                        SleepCheck.curTermDb = decibel;
                        SleepCheck.curTermAmp = amplitude;
                        SleepCheck.curTermHz = frequency;
                        SleepCheck.curTermSecondHz = sefrequency;

                        SleepCheck.checkTerm++;
                        SleepCheck.checkTermSecond = (int) Math.floor(times);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }

                //Log.v(LOG_TAG2,("audio length(s): " + ((double) (audioData.length / (44100d * 16 * 1))) * 8));
                Log.v(LOG_TAG2,("audio length(s): " + String.format("%.2f", times)));

                Log.v(LOG_TAG2,( "코골이 여부 " + SleepCheck.snoringContinue));
                Log.v(LOG_TAG2,( "이갈이 " + grindingTermList.size()+"회 발생 "));
                Log.v(LOG_TAG2,( "이갈이 구간=========="));
                for(StartEnd se : grindingTermList) {
                    Log.v(LOG_TAG2,(se.getTerm()));
                }
                Log.v(LOG_TAG2,( "=================="));
                Log.v(LOG_TAG2,( "무호흡" + osaTermList.size()+"회 발생 "));
                Log.v(LOG_TAG2,( "무호흡 구간=========="));
                for(StartEnd se : osaTermList) {
                    Log.v(LOG_TAG2,(se.getTerm()));
                }
                Log.v(LOG_TAG2,( "=================="));

                Log.v(LOG_TAG, String.format("Recording  has stopped. Samples read: %d", shortsRead));
            }
        }).start();
    }

}


class StartEnd {
    double start;
    double end;
    public String getTerm() {
        return String.format("%.0f", start)+"~"+String.format("%.0f", end);
    }
    public String getTermForRequest(int termCd, long recordStartingTIme) {
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return "termTypeCd: "+termCd +", termStartDt: "+dayTime.format(new Date((long) (recordStartingTIme+this.start*1000)))+", termEndDt"+dayTime.format(new Date((long) (recordStartingTIme+this.end*1000)));
    }
}