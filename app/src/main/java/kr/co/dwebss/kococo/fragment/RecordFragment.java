package kr.co.dwebss.kococo.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.AnalysisRawData;
import kr.co.dwebss.kococo.util.AudioCalculator;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import kr.co.dwebss.kococo.util.SimpleLame;
import kr.co.dwebss.kococo.util.WaveFormatConverter;
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
public class RecordFragment extends Fragment  {

    static {
        System.loadLibrary("mp3lame");
    }

    Boolean recodeFlag = false;
    private String LOG_TAG = "RecodeFragment";
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
    private String LOG_TAG2 = "Audio_Recording";
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

    short[] audioData = new short[frameByteSize];

    Retrofit retrofit;
    ApiService apiService;


    //request 데이터 모음
    JsonObject recordData;
    String userAppId;
    String recordStartDt;
    String recordEndDt;
    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    byte[] mp3buffer;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_record, container, false);
        Button recodeBtn = (Button) v.findViewById(R.id.recodeBtn) ;
        recodeFlag = false;
        recodeBtn.setText("녹음 시작");

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

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
//                        Toast.makeText(getActivity(), "permission 승인", Toast.LENGTH_SHORT).show();
                        Log.e(LOG_TAG, "permission 승인");
                        recodeBtn.setText("녹음 종료");
                        recodeFlag = true;
                        recordStartDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
                        start();
                    }
                }else{
                    Toast.makeText(getActivity(), "분석중입니다 잠시만 기다려주세요...", Toast.LENGTH_LONG).show();
                    recordEndDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
                    recodeFlag = false;
                    stop(v);
                    recodeBtn.setText("녹음 시작");

                    Handler delayHandler = new Handler();
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // TODO
                            RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
                            System.out.println(" ================녹음 종료 시 DB 저장========requestData: "+requestData.toString());
                            addRecord(requestData);
                        }
                    }, 3000);

                }
            }
        });

        FindAppIdUtil fau = new FindAppIdUtil();
        userAppId = fau.getAppid(getContext());

        Button testBtn = (Button) v.findViewById(R.id.testBtn) ;
        testBtn.setText("테스트");

        //xml 내에서 onclick으로 가능하다. 하지만 그건 activity 내에서만 가능하고 프래그먼트에서는 onclickListener()로 해야함
        testBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                String testDt = "{\"userAppId\":\"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f\",\"recordId\":86,\"recordStartD\":\"2019-05-29\",\"recordStartDt\":\"2019-05-29T16:10:31\",\"recordEndD\":\"2019-05-29\",\"recordEndDt\":\"2019-05-29T16:10:54\",\"consultingYn\":\"N\",\"consultingReplyYn\":\"N\",\"analysisList\":[{\"analysisId\":63,\"analysisStartD\":\"2019-05-29T16:10:34\",\"analysisStartDt\":\"2019-05-29T16:10:34\",\"analysisEndD\":\"2019-05-29T16:10:54\",\"analysisEndDt\":\"2019-05-29T16:10:54\",\"analysisFileNm\":\"snoring-20191029_0410-29_0410_1559113854914.wav\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/23\",\"analysisServerUploadYn\":\"N\",\"claimYn\":\"N\",\"analysisDetailsList\":[{\"analysisDetailsId\":65,\"termTypeCd\":200102,\"termStartDt\":\"2019-05-29T16:10:36\",\"termEndDt\":\"2019-05-29T16:10:40\"}],\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"}}}],\"_links\":{\"self\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"admin\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/admin\"},\"user\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/user\"}}}";
                Intent intent = new Intent(getActivity(), ResultActivity.class);
                intent.putExtra("responseData",testDt); /*송신*/
                startActivity(intent);

            }
        });
        return v;
    }

    public void addRecord(RequestBody requestData) {
        //POST /api/record를 호출한다.
        apiService.addRecord(requestData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(" ============녹음 종료 시 DB 저장============response: "+response.body().toString());
                //창 띄우기
//                                    startActivity(new Intent(getActivity(), ResultActivity.class));
                Intent intent = new Intent(getActivity(), ResultActivity.class);
                intent.putExtra("responseData",response.body().toString()); /*송신*/
                startActivity(intent);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                System.out.println(" ============녹음 종료 시 DB 저장============Throwable: "+ t);

            }
        });
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

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        System.out.println("=============="+LOG_TAG+"================"+isVisibleToUser);
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            // Refresh your fragment here
            refresh();
        }
    }

    //프래그먼트 초기화 방법
    private  void refresh(){
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.detach(this).attach(this).commit();
    }



    public void start() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        //int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        int recBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding);;
        /* mp3 저장 jni 사용위한 초기화 시작 */
        mp3buffer = new byte[(int) (7200 + recBufSize * 2 * 1.25)];
        SimpleLame.init(sampleRate, 1, sampleRate, 32);
        /* mp3 저장 jni 사용위한 초기화 끝 */
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
        Log.e(RecordFragment.class.getSimpleName(), "RecordingState() after startRecording() = " + String.valueOf(recordingState));
        if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(RecordFragment.class.getSimpleName(), "AudioRecord error has occured. Reopen app.");
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
                    frameBytes = shortToByte(audioData,numberOfShort);

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
                    SleepCheck.GrindingCheckTermSecond = times;

                    final String amp = String.valueOf(amplitude + "Amp");
                    final String db = String.valueOf(decibel + "db");
                    final String hz = String.valueOf(frequency + "Hz");
                    final String sehz = String.valueOf(sefrequency + "Hz(2th)");
                    final String seamp = String.valueOf(sefamplitude + "Amp(2th)");
                    SleepCheck.setMaxDB(decibel);
                    SleepCheck.setMinDB(decibel);
                    //실제로는 3초 이후 분석한다.
                    if (i < 300) {
                        continue;
                    }
                    Log.v(LOG_TAG2,(String.format("%.2f", times)+"s "+hz +" "+db+" "+amp+" "+sehz+" "+seamp+" "+decibel+"vs"+SleepCheck.getMaxDB())+","+SleepCheck.getMinDB());
                    // 소리의 발생은 특정 db 이상으로한다. 데시벨은 -31.5~0 으로 수치화 하고 있음.
                    // -10db에 안걸릴 수도 잇으니까, 현재 녹음 상태의 평균 데시벨값을 지속적으로 갱신하면서 평균 데시벨보다 높은 소리가 발생했는지 체크
                    // 한다.
                    // 평균 데시벨 체크는 3초 동안한다.
                    if (decibel > SleepCheck.getMaxDB() && isRecording == false
                            && Math.floor((double) (audioData.length / (44100d * 16 * 1)) * 8) != Math.floor(times) //사운드 파일 테스트용
                    ) {
                        Log.v(LOG_TAG2,("녹음 시작! "));
                        Log.v(LOG_TAG2,(String.format("%.2f", times)+"s~"));
                        recordStartingTIme = System.currentTimeMillis();
                        baos = new ByteArrayOutputStream();
                        isRecording = true;
                        snoringTermList = new ArrayList<StartEnd>();
                        grindingTermList = new ArrayList<StartEnd>();
                        osaTermList = new ArrayList<StartEnd>();
                    } else if (isRecording == true && (SleepCheck.noiseCheck(decibel)==0 || recodeFlag==false) ) {
//                    } else if (isRecording == true && (SleepCheck.noiseCheck(decibel)==0 ) ) {
                        Log.v(LOG_TAG2,("녹음 종료! "));
                        Log.v(LOG_TAG2,(String.format("%.2f", times)+"s "));
                        SimpleDateFormat dayTime = new SimpleDateFormat("yyyymmdd_HHmm");
                        String fileName = dayTime.format(new Date(recordStartingTIme));
                        dayTime = new SimpleDateFormat("dd_HHmm");
                        //long time = System.currentTimeMillis();
                        long time = recordStartingTIme+(long)times*1000;
                        fileName += "-" + dayTime.format(new Date(time));
                        byte[] waveData = baos.toByteArray();

                        //TODO 녹음된 파일이 저장되는 시점
                        //WaveFormatConverter wfc = new WaveFormatConverter(44100, (short)1, waveData, 0, waveData.length-1);
                        WaveFormatConverter wfc = new WaveFormatConverter();
                        //String[] fileInfo = wfc.saveLongTermWave(fileName, getContext());
                        String[] fileInfo = wfc.saveLongTermMp3(fileName, getContext(), waveData);

                        Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 시작====="));
                        Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (waveData.length / (44100d * 16 * 1))) * 8));

                        JsonObject ans = new JsonObject();
                        //ans.setAnalysisStartDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(recordStartingTIme), ZoneId.systemDefault()));
                        //ans.setAnalysisEndDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
                        ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                        ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                        ans.addProperty("analysisFileAppPath",fileInfo[0]);
                        ans.addProperty("analysisFileNm",fileInfo[1]);
                        JsonArray ansDList = new JsonArray();
                        JsonObject ansd = new JsonObject();
                        for(StartEnd se : snoringTermList) {
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200101);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                            try {
                                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                String andRList = gson.toJson(se.getAnalysisRawDataList());
                                ansd.addProperty("analysisData", andRList);

                            }catch(NullPointerException e){
                                e.getMessage();
                            }
                            ansDList.add(ansd);
                        }
                        for(StartEnd se : grindingTermList) {
                            if(se.end!=0){
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200102);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                try {
                                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                    String andRList = gson.toJson(se.getAnalysisRawDataList());
                                    ansd.addProperty("analysisData", andRList);

                                }catch(NullPointerException e){
                                    e.getMessage();
                                }
                                ansDList.add(ansd);
                            }
                        }
                        for(StartEnd se : osaTermList) {
                            if(se.end!=0){
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200103);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                try {
                                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                    String andRList = gson.toJson(se.getAnalysisRawDataList());
                                    ansd.addProperty("analysisData", andRList);

                                }catch(NullPointerException e){
                                    e.getMessage();
                                }
                                ansDList.add(ansd);
                            }
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
                    //baos.write(frameBytes);
                    int encResult = SimpleLame.encode(audioData, audioData, numberOfShort, mp3buffer);
                    if (encResult != 0) {
                        baos.write(mp3buffer, 0, encResult);
                    }
						/*
						System.out.print("녹음 중! ");
						Log.v(LOG_TAG2,(String.format("%.2f", times)+"s ");
						*/

                    // 녹음이 끝나고 나면 코골이가 발생했는지를 체크해서 녹음된 파일의 코골이 유무를 결정한다. X
                    // 코골이 여부를 체크한다.
                    int snoreChecked = SleepCheck.snoringCheck(decibel, frequency, sefrequency);
                    //snorChecked = 1이면 0.01초에 해당하는 주파수만 탐지됨
                    //snorChecked = 2는 1분동안 코골이가 탐지된 상태
                    if(snoreChecked==2) {
                        if(SleepCheck.isSnoringStart == true) {
                            //코골이로 탐지해서 분석을 진행하고 있는 중
                        }else {
                            snoringTermList.add(new StartEnd());
                            snoringTermList.get(snoringTermList.size()-1).start=times;
                            snoringTermList
                                    .get(snoringTermList.size() - 1).AnalysisRawDataList = new ArrayList<AnalysisRawData>();
                            snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(
                                    times, amplitude, decibel, frequency, sefrequency, sefamplitude));
                            SleepCheck.isSnoringStart = true;
                        }
                    }else if(snoreChecked == 3) {
                        if(SleepCheck.isSnoringStart == true) {
                            snoringTermList.get(snoringTermList.size()-1).end=times;
                            SleepCheck.isSnoringStart = false;
                        }else {
                            //코골이로 미탐지, 처리할 내용 없음.
                        }
                    }else {
                        //0일 때는 아직 분석하고 1분이 안된 상태, 즉 각 1분이 안된 때마다 이곳을 탄다.
                    }
                    if (SleepCheck.isSnoringStart == true) {
                        try {
                            String tmpTime =
                                    String.format("%.0f",
                                            snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.get(snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.size()-1).getTimes()
                                    );
                            if(!tmpTime.equals(String.format("%.0f",times))){
                                snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(
                                        times, amplitude, decibel, frequency, sefrequency, sefamplitude));
                            }
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println("293");// log4j로 하면 라인넘버를 남길 수 있다. 여기서는 하드 코딩.
                        }
                    }
                    // 이갈이는 기존 로직대로 체크해서, 어디 구간에서 발생했는지 체크한다.
                    SleepCheck.grindingCheck(times, decibel, sefamplitude, frequency, sefrequency);
                    // 이갈이 신호가 발생하고, 이갈이 체크 상태가 아니면 이갈이 체크를 시작한다.
                    if (SleepCheck.grindingRepeatAmpCnt == 2 && grindingStart == false) {
                        /*
                        System.out.print("이갈이 체크를 시작한다.");
                        Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
                                + "s " + SleepCheck.grindingContinueAmpCnt + " "
                                + SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
                        */
                        SleepCheck.GrindingCheckStartTermSecond = times;
                        grindingTermList.add(new StartEnd());
                        grindingTermList.get(grindingTermList.size()-1).start=times-2;
                        grindingTermList.get(grindingTermList.size() - 1).AnalysisRawDataList = new ArrayList<AnalysisRawData>();
                        grindingTermList.get(grindingTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(times, amplitude, decibel, frequency, sefrequency, sefamplitude));
                        grindingStart = true;
                        grindingContinue = false;
                        // 이갈이 체크 중에 1초간격으로 유효 카운트가 연속적으로 발생했으면 계속 체크한다.
                    } else if (Math.floor((SleepCheck.GrindingCheckTermSecond - SleepCheck.GrindingCheckStartTermSecond)*100) == 101
                            && SleepCheck.grindingRepeatAmpCnt >= 3 && grindingStart == true) {
                        if (((double) (audioData.length / (44100d * 16 * 1))) * 8 < times + 1) {
                            /*
                            System.out.print("이갈이 종료.");
                            Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
                                    + "s " + SleepCheck.grindingContinueAmpCnt + " "
                                    + SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
                            */
                            SleepCheck.grindingRepeatAmpCnt = 0;
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
                    } else if (Math.floor((SleepCheck.GrindingCheckTermSecond - SleepCheck.GrindingCheckStartTermSecond)*100) == 101
                            && SleepCheck.grindingRepeatAmpCnt <= 1000 && grindingStart == true
                            && grindingContinue == false) {
                        // 1초 단위 발생하는 이갈이도 잡기위해 유예 카운트를 넣는다. 1초만 한번더 체크함.
                        if (grindingRecordingContinueCnt >= SleepCheck.GRINDING_RECORDING_CONTINUE_CNT) {
                            /*
                            System.out.print("이갈이 아님, 체크 취소.");
                            Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
                                    + "s " + SleepCheck.grindingContinueAmpCnt + " "
                                    + SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
                            */
                            SleepCheck.grindingRepeatAmpCnt = 0;
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
                    } else if (Math.floor((SleepCheck.GrindingCheckTermSecond - SleepCheck.GrindingCheckStartTermSecond)*100) == 101
                            && SleepCheck.grindingRepeatAmpCnt == 0 && grindingContinue == true) {
                        /*
                        System.out.print("이갈이 종료.");
                        Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
                                + "s " + SleepCheck.grindingContinueAmpCnt + " "
                                + SleepCheck.grindingContinueAmpOppCnt + " " + SleepCheck.grindingRepeatAmpCnt);
                        */
                        SleepCheck.grindingRepeatAmpCnt = 0;
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

                    if (grindingStart) {
                        try {

                            String tmpTime =
                                    String.format("%.0f",
                                            grindingTermList.get(grindingTermList.size() - 1).AnalysisRawDataList.get(grindingTermList.get(grindingTermList.size() - 1).AnalysisRawDataList.size()-1).getTimes()
                                    );
                            if(!tmpTime.equals(String.format("%.0f",times))){
                                grindingTermList.get(grindingTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(
                                        times, amplitude, decibel, frequency, sefrequency, sefamplitude));
                            }
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println("397");// log4j로 하면 라인넘버를 남길 수 있다. 여기서는 하드 코딩.
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
                            osaTermList.remove(osaTermList.size() - 1);
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
                    if(SleepCheck.isOSAAnsStart == true) {
                        try {
                            String tmpTime =
                                    String.format("%.0f",
                                            osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.get(osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.size()-1).getTimes()
                                    );
                            if(!tmpTime.equals(String.format("%.0f",times))){
                                osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.add(
                                        new AnalysisRawData(times, amplitude, decibel, frequency, sefrequency, sefamplitude));
                            }
                        } catch (IndexOutOfBoundsException e) {
                            //System.out.println("540");// log4j로 하면 라인넘버를 남길 수 있다. 여기서는 하드 코딩.
                        }
						/*catch (NullPointerException e1) {
							System.out.println("NULL="+times);
							//System.out.println(SleepCheck.isOSATermTimeOccur+" "+);
						}*/
                    }else {
                        //System.out.println(times);
                    }
                    SleepCheck.curTermTime = times;
                    SleepCheck.curTermDb = decibel;
                    SleepCheck.curTermAmp = amplitude;
                    SleepCheck.curTermHz = frequency;
                    SleepCheck.curTermSecondHz = sefrequency;

                    SleepCheck.checkTerm++;
                    SleepCheck.checkTermSecond = (int) Math.floor(times);

                    //                    catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }
                if (isRecording == true && recodeFlag==false) {
                    Log.v(LOG_TAG2,("녹음 종료! "));
                    Log.v(LOG_TAG2,(String.format("%.2f", times)+"s "));
                    SimpleDateFormat dayTime = new SimpleDateFormat("yyyyMMdd_HHmm");
                    String fileName = dayTime.format(new Date(recordStartingTIme));
                    dayTime = new SimpleDateFormat("dd_HHmm");
                    //long time = System.currentTimeMillis();
                    long time = recordStartingTIme+(long)times*1000;
                    fileName += "-" + dayTime.format(new Date(time));
                    byte[] waveData = baos.toByteArray();

                    //TODO 녹음된 파일이 저장되는 시점
                    //WaveFormatConverter wfc = new WaveFormatConverter(44100, (short)1, waveData, 0, waveData.length-1);
                    WaveFormatConverter wfc = new WaveFormatConverter();
                    String[] fileInfo = wfc.saveLongTermMp3(fileName, getContext(), waveData);

                    Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 시작====="));
                    Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (waveData.length / (44100d * 16 * 1))) * 8));

                    JsonObject ans = new JsonObject();
                    ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                    ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                    ans.addProperty("analysisFileAppPath",fileInfo[0]);
                    ans.addProperty("analysisFileNm",fileInfo[1]);
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
                        if(se.end!=0){
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200102);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                            ansDList.add(ansd);
                        }
                    }
                    for(StartEnd se : osaTermList) {
                        if(se.end!=0){
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200103);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                            ansDList.add(ansd);
                        }
                    }
                    ans.add("analysisDetailsList", ansDList);
                    ansList.add(ans);

                    Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 끝====="));
                    isRecording = false;
                }

                recordData = new JsonObject();
                recordData.addProperty("userAppId",userAppId);
                recordData.addProperty("recordStartDt",recordStartDt);
                recordData.addProperty("recordEndDt",recordEndDt);
                recordData.add("analysisList", ansList);

                System.out.println(" =============녹음 종료버튼  ===========recordData: "+recordData.toString());

                //Log.v(LOG_TAG2,("audio length(s): " + ((double) (audioData.length / (44100d * 16 * 1))) * 8));
                Log.v(LOG_TAG2,("녹음시작-종료(s): " + String.format("%.2f", times)));

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

    private byte[] shortToByte(short[] input, int elements) {
        int short_index, byte_index;
        int iterations = elements; //input.length;
        byte[] buffer = new byte[iterations * 2];
        short_index = byte_index = 0;
        for (/*NOP*/; short_index != iterations; /*NOP*/) {
            buffer[byte_index] = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);
            ++short_index;
            byte_index += 2;
        }
        return buffer;
    }
}

class StartEnd {
    double start;
    double end;
    List<AnalysisRawData> AnalysisRawDataList;

    public String getTerm() {
        return String.format("%.0f", start) + "~" + String.format("%.0f", end);
    }

    public String getTermForRequest(int termCd, long recordStartingTIme) {
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return "termTypeCd: " + termCd + ", termStartDt: "
                + dayTime.format(new Date((long) (recordStartingTIme + this.start * 1000))) + ",termEndDt: "
                + dayTime.format(new Date((long) (recordStartingTIme + this.end * 1000)));
    }

    public String getAnalysisRawDataList() {
        String rtn = "";
        if(this.AnalysisRawDataList!=null) {
            for(AnalysisRawData d : this.AnalysisRawDataList) {
                rtn+=d.toString()+"\r\n";
            }
        }
        return rtn;
    }
}