package kr.co.dwebss.kococo.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ProgressActivity;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.application.DataHolderApplication;
import kr.co.dwebss.kococo.fragment.recorder.RecordingThread;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.util.FileUtil;
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
@SuppressLint("ValidFragment")
public class RecordFragment extends Fragment  {

    static {
        System.loadLibrary("mp3lame");
    }

    Boolean recodeFlag = false;
    public Boolean getRecordeFlag(){
        return this.recodeFlag;
    }
    private String LOG_TAG = "RecodeFragment";

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
    private String LOG_TAG3 = "Audio_Recording3";
    int state = 0;

    Retrofit retrofit;
    ApiService apiService;


    //request 데이터 모음
    JsonObject recordData;
    public JsonObject getRecordData(){
        return this.recordData;
    }
    public void setRecordData(JsonObject recordData){
        this.recordData = recordData;
        this.recordData .addProperty("userAppId",this.userAppId);
        this.recordData .addProperty("recordStartDt",this.recordStartDt);
    }
    String userAppId;
    String recordStartDt;
    Long recordStartDtL;
    public Long getRecordStartDtl(){
        return this.recordStartDtL;
    }
    public Context getThisContext(){
        return this.getContext();
    }
    String recordEndDt;
    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    //사용자의 밝기 저장
    int now_bright_status;

    Button recodeBtn;
    TextView recodeTxt;
    TextView recordTimer;
    ImageView logo;
    int recordTime=0;

    Handler timerMessegeHandler;
    Timer mTimer;

    //재생할때 필요한
    MediaPlayer mediaPlayer;

    private boolean mShouldContinue = true;
    private static AudioRecord record;
    byte[] mp3buffer;
    public byte[] getMp3buffer(){
        return this.mp3buffer;
    }

    public boolean getShouldContinue(){
        return this.mShouldContinue;
    }

    public AudioRecord getAudioRecord(){
        return this.record;
    }
    RecordingThread recordingThread;

    private TabEventUtil tabEventUtil;
    public interface TabEventUtil{
        void tabEvent(boolean flag);
    }

    @SuppressLint("ValidFragment")
    public RecordFragment(TabEventUtil tabEventUtil, InterstitialAd mInterstitialAd) {
        // Required empty public constructor
        this.tabEventUtil = tabEventUtil;
        this.mInterstitialAd = mInterstitialAd;
    }

    Button testBtn;
    boolean testFlag=false;

    //전면광고
    private InterstitialAd mInterstitialAd;
    private AdView adView;
    private AdRequest adRequest;
    WindowManager.LayoutParams params;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //화면이 자동으로 꺼지는것을 방지한다.
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        params = getActivity().getWindow().getAttributes();

        View v = inflater.inflate(R.layout.fragment_record, container, false);
        //배너광고
        adView = (AdView)v.findViewById(R.id.publisherAdView);
        adRequest = new AdRequest.Builder().build();

        recodeBtn = (Button) v.findViewById(R.id.recodeBtn) ;
        recodeFlag = false;
        recodeBtn.setText("녹음 시작");
        recodeBtn.setEnabled(true);

        recodeTxt = (TextView) v.findViewById(R.id.recordTxt);
        logo = (ImageView) v.findViewById(R.id.imageView);
        recordTimer = (TextView) v.findViewById(R.id.recordTimer);
        recodeTxt.setVisibility(View.INVISIBLE);
        recordTimer.setVisibility(View.INVISIBLE);
        logo.setVisibility(View.VISIBLE);

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

//        file 삭제 테스트
//        FileUtil fu = new FileUtil();
//        fu.removeFiles(getContext().getFilesDir().getAbsolutePath()+"/rec_data/"+"1/snoring-20190405_1604-05_1605_1559718321865.mp3");

//        testBtn = (Button) v.findViewById(R.id.testBtn) ;
//        testFlag = false;
//        testBtn.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if( testFlag == false){
//                    testFlag= true;
//                    testBtn.setText("탭 막기 해제");
//                    //탭막기 시작
//                    tabEventUtil.tabEvent(testFlag);
//                }else{
//                    testFlag= false;
//                    testBtn.setText("탭 막기");
//                    //탭막기 해제
//                    tabEventUtil.tabEvent(testFlag);
//                }
//            }
//        });

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                System.out.println("==================onAdFailedToLoad==============="+errorCode);
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
            @Override
            public void onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                recordStart();
            }
        });


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
                        Log.e(LOG_TAG2, "permission 승인");
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        } else {
                            Log.d("TAG", "The interstitial wasn't loaded yet.");
                            Toast.makeText(getActivity(), "예기치 않은 에러가 발생하였습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                        }
                    }
                }else{
                    Toast.makeText(getActivity(), "분석중입니다 잠시만 기다려주세요...", Toast.LENGTH_LONG).show();
                    adView.destroy();
                    adView.setVisibility(View.GONE);
                    stop(v);

                    //기존 밝기로 복귀
                    params.screenBrightness = (float) now_bright_status/100;
                    getActivity().getWindow().setAttributes(params);
                    Handler delayHandler = new Handler();
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
                            Log.v(LOG_TAG2,(" ================녹음 종료 시 DB 저장========requestData: "+requestData.toString()));
                            addRecord(requestData);
                        }
                    }, 5000);

                    //타이머 종료
                    mTimer.cancel();
                    recodeBtn.setEnabled(false);
                    recodeBtn.setText("녹음 시작");
                    recodeTxt.setVisibility(View.INVISIBLE);
                    recordTimer.setVisibility(View.INVISIBLE);
                    logo.setVisibility(View.VISIBLE);
                    recordTime = 0;
                    recodeFlag = false;
                    //탭 막기 해제
                    tabEventUtil.tabEvent(recodeFlag);
                }
            }
        });

        FindAppIdUtil fau = new FindAppIdUtil();
        userAppId = fau.getAppid(getContext());

        timerMessegeHandler = new Handler(){
            public void handleMessage(Message msg){
                int sec = (recordTime) % 60;
                int min = (recordTime/60) % 60;
                int hour = (recordTime / (60*60)) % 24;
                recordTimer.setText(String.format("%02d:%02d:%02d", hour, min, sec));
            }
        };
        return v;
    }
    @Override
    public void onResume() {
        super.onResume();
        recodeBtn.setEnabled(true);
    }

    class CustomTimer extends TimerTask {
        @Override
        public void run() {
            recordTime++;
            Message msg = new Message();
            msg.arg1 = recordTime;
            timerMessegeHandler.sendMessage(msg);
        }
    }

    public void recordStart(){
        adView.loadAd(adRequest);
        adView.setVisibility(View.VISIBLE);

        recodeBtn.setText("녹음 종료");
        //녹음버튼 누를 시에 밝기 최대한 줄이기
        try{
            now_bright_status = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
            params.screenBrightness = (float) 1 / 100;
            getActivity().getWindow().setAttributes(params);
        }catch(Exception e){
            Log.e("Exception e "+e.getMessage(), null);
        }
        //녹음버튼 누를 시에 타이머 START
        recodeTxt.setVisibility(View.VISIBLE);
        recordTimer.setVisibility(View.VISIBLE);
        logo.setVisibility(View.INVISIBLE);
        recordTime=0;
        mTimer =  new Timer();
        mTimer.schedule(new CustomTimer(), 2000, 1000);

        recodeFlag = true;
        recordStartDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
        recordStartDtL= System.currentTimeMillis();
        start();
        //녹음시에는 탭 막기
        tabEventUtil.tabEvent(recodeFlag);
    }

    public void addRecord(RequestBody requestData) {
        //POST /api/record를 호출한다.
        apiService.addRecord(requestData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.v(LOG_TAG2,(" ============녹음 종료 시 DB 저장============response: "+response.body()));
                //창 띄우기
//                                    startActivity(new Intent(getActivity(), ResultActivity.class));
                Intent intent = new Intent(getActivity(), ResultActivity.class);
//                intent.putExtra("responseData",response.body().toString()); /*송신*/
                String holderId = DataHolderApplication.getInstance().putDataHolder(response.body().toString());
                intent.putExtra("holderId", holderId);
                startActivity(intent);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.v(LOG_TAG2,(" ============녹음 종료 시 DB 저장============Throwable: "+ t));
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
        Log.v(LOG_TAG2,("=============="+LOG_TAG2+"================"+isVisibleToUser));
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
        Log.e(LOG_TAG2, record.getState()+" ");
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG2, "Audio Cannot be Recorded");
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
        Log.v(LOG_TAG2, "Recording has started");
        mShouldContinue = true;

        String savePath = getContext().getFilesDir().getAbsolutePath(); // 이경로는 adb pull 이 안됨.

        WaveFormatConverter wfc = new WaveFormatConverter();
        int folderNm = wfc.subDirList(savePath+"/rec_data/");
        System.out.println("=======파일 저장 ======초기=="+folderNm);

        //Audio_Recording();
        recordingThread = new RecordingThread(this, String.valueOf(folderNm));
        recordingThread.setPriority(Thread.MAX_PRIORITY);
        //recordingThread.start(getContext(), mShouldContinue, record, recordStartDtL, recodeFlag, recordData, mp3buffer);
        recordingThread.start();

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

    /*
    * 권한 요청이 완료 된 후에 이벤트
    * */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]==0&&grantResults[1]==0&&grantResults[2]==0){
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.");
                Toast.makeText(getActivity(), "예기치 않은 에러가 발생하였습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            }
        }
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
    }
}