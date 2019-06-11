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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.fragment.recorderUtil.DoubleValues;
import kr.co.dwebss.kococo.fragment.recorderUtil.Radix2FFT;
import kr.co.dwebss.kococo.fragment.recorderUtil.ShortValues;
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
    private String LOG_TAG2 = "Audio_Recording2";
    private String LOG_TAG3 = "Audio_Recording3";
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

    short[] audioData = new short[frameByteSize/2];

    int frameByteSizePer = 16;
    int frameByteSizeForSnoring = 1024*frameByteSizePer;
    byte[] buffer;
    byte[] totalBuf;
    int cnt;

    double tmpMinDb = 99999;
    double tmpMaxDb = 0;

    int l = 0;


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
                        Log.e(LOG_TAG2, "permission 승인");
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

                    Handler delayHandler = new Handler();
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // TODO
                            RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
                            Log.v(LOG_TAG2,(" ================녹음 종료 시 DB 저장========requestData: "+requestData.toString()));
                            addRecord(requestData);
                        }
                    }, 5000);
                    recodeBtn.setText("녹음 시작");
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
                //정상 테스트 데이터
//                String testDt = "{\"userAppId\":\"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f\",\"recordId\":86,\"recordStartD\":\"2019-05-29\",\"recordStartDt\":\"2019-05-29T16:10:31\",\"recordEndD\":\"2019-05-29\",\"recordEndDt\":\"2019-05-29T16:10:54\",\"consultingYn\":\"N\",\"consultingReplyYn\":\"N\",\"analysisList\":[{\"analysisId\":63,\"analysisStartD\":\"2019-05-29T16:10:34\",\"analysisStartDt\":\"2019-05-29T16:10:34\",\"analysisEndD\":\"2019-05-29T16:10:54\",\"analysisEndDt\":\"2019-05-29T16:10:54\",\"analysisFileNm\":\"snoring-20190605_1708-05_1709_1559722170788.mp3\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/8\",\"analysisServerUploadYn\":\"N\",\"claimYn\":\"N\",\"analysisDetailsList\":[{\"analysisDetailsId\":65,\"termTypeCd\":200102,\"termStartDt\":\"2019-05-29T16:10:36\",\"termEndDt\":\"2019-05-29T16:10:40\"}],\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"}}}],\"_links\":{\"self\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"admin\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/admin\"},\"user\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/user\"}}}";
                String testDt = "{\"userAppId\":\"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f\",\"recordId\":86,\"recordStartD\":\"2019-05-29\",\"recordStartDt\":\"2019-05-29T16:10:31\",\"recordEndD\":\"2019-05-29\",\"recordEndDt\":\"2019-05-29T16:10:54\",\"consultingYn\":\"N\",\"consultingReplyYn\":\"N\",\"analysisList\":[{\"analysisId\":63,\"analysisStartD\":\"2019-05-29T16:10:34\",\"analysisStartDt\":\"2019-05-29T16:10:34\",\"analysisEndD\":\"2019-05-29T16:10:54\",\"analysisEndDt\":\"2019-05-29T16:10:54\",\"analysisFileNm\":\"snoring-20190605_1708-05_1709_1559722170788.mp3\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/8\",\"analysisServerUploadYn\":\"N\",\"claimYn\":\"N\",\"analysisDetailsList\":[{\"analysisDetailsId\":65,\"termTypeCd\":200102,\"termStartDt\":\"2019-05-29T16:10:36\",\"termEndDt\":\"2019-05-29T16:10:40\"},{\"analysisDetailsId\":66,\"termTypeCd\":200101,\"termStartDt\":\"2019-05-29T16:10:50\",\"termEndDt\":\"2019-05-29T16:10:53\"}],\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"}}}],\"_links\":{\"self\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"admin\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/admin\"},\"user\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/user\"}}}";
                //mp3파일이 삭제됬거나  이상하게 저장되어있는 테스트 데이터
//                String testDt = "{\"userAppId\":\"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f\",\"recordId\":86,\"recordStartD\":\"2019-05-29\",\"recordStartDt\":\"2019-05-29T16:10:31\",\"recordEndD\":\"2019-05-29\",\"recordEndDt\":\"2019-05-29T16:10:54\",\"consultingYn\":\"N\",\"consultingReplyYn\":\"N\",\"analysisList\":[{\"analysisId\":63,\"analysisStartD\":\"2019-05-29T16:10:34\",\"analysisStartDt\":\"2019-05-29T16:10:34\",\"analysisEndD\":\"2019-05-29T16:10:54\",\"analysisEndDt\":\"2019-05-29T16:10:54\",\"analysisFileNm\":\"snoring-20190605_1708-05_1709_1559712312312312322170788.mp3\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/8\",\"analysisServerUploadYn\":\"N\",\"claimYn\":\"N\",\"analysisDetailsList\":[{\"analysisDetailsId\":65,\"termTypeCd\":200102,\"termStartDt\":\"2019-05-29T16:10:36\",\"termEndDt\":\"2019-05-29T16:10:40\"}],\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"}}}],\"_links\":{\"self\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"admin\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/admin\"},\"user\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/user\"}}}";
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
                Log.v(LOG_TAG2,(" ============녹음 종료 시 DB 저장============response: "+response.body()));
                //창 띄우기
//                                    startActivity(new Intent(getActivity(), ResultActivity.class));
                Intent intent = new Intent(getActivity(), ResultActivity.class);
                intent.putExtra("responseData",response.body().toString()); /*송신*/
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
                byte[] frameBytesForSnoring = new byte[frameByteSizeForSnoring];
                audioCalculator = new AudioCalculator();

                SleepCheck.checkTerm = 0;
                SleepCheck.checkTermSecond = 0;
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
                int i = 0;
                int[] tmpArray = null;
                boolean isRecording = false;
                boolean soundStartInRecording = false;
                double soundStartInRecordingTimes = 0.0;
                int soundStartAndSnroingCnt = 0;
                int soundStartAndSnroingOppCnt = 0;
                double maxDecibelAvg = 0.0;
                double lowFHDecibelAvg = 0.0;
                double firstDecibelAvg = 0.0;
                double secondDecibelAvg = 0.0;
                int snoringBufferFilledCnt = 0;
                double[] allFHAndDB = null;
                double snoringDbChkCnt = 0;
                int grindingRepeatOnceAmpCnt = 0;
                int grindingRepeatAmpCnt = 0;
                int grindingContinueAmpCnt = 0;
                int grindingContinueAmpOppCnt = 0;
                double GrindingCheckTermSecond = 0;
                double GrindingCheckStartTermSecond = 0;
                double GrindingCheckStartTermDecibel = 0;
                boolean grindingStart = false;
                boolean grindingContinue = false;
                int grindingRecordingContinueCnt = 0;
                int GRINDING_RECORDING_CONTINUE_CNT = 1;

                double chkDBAgainInRecording = 0.0;
                int continueCntInChkTermForGrinding = 0;
                int continueCntInChkTermForGrindingChange = 0;

                int osaCnt = 0;
                int osaRecordingContinueCnt = 0;
                int osaRecordingExit = 0;
                boolean osaStart = false;
                boolean osaContinue = false;
                double osaStartTimes = 0.0;
                while (mShouldContinue) {
                    times = (((double) (frameBytes.length / (44100d * 16 * 1))) * 8) * i;
                    int numberOfShort = record.read(audioData, 0, audioData.length);
                    shortsRead += numberOfShort;
                    frameBytes = shortToByte(audioData,numberOfShort);

                    audioCalculator.setBytes(frameBytes);
                    // 소리가 발생하면 녹음을 시작하고, 1분이상 소리가 발생하지 않으면 녹음을 하지 않는다.
                    int amplitude = audioCalculator.getAmplitude();
                    double decibel = audioCalculator.getDecibel();
                    double frequency = audioCalculator.getFrequency();

                    //전체 진폭을 가져온다.
                    //전체 진폭에 대한 주파수, 주파수의 갭=hzPerDataPoint
                    //전체 진폭에 대한 주파수 리스트 길이=fftSize
                    if(snoringBufferFilledCnt < frameByteSizePer) {
                        System.arraycopy(frameBytes,0,frameBytesForSnoring,frameBytes.length*snoringBufferFilledCnt,frameBytes.length);
                        snoringBufferFilledCnt++;
                    }

                    if(snoringBufferFilledCnt == frameByteSizePer) {
                        snoringBufferFilledCnt = 0;
                        short[] tmpBytes = getAmplitudesFromBytesShort(frameBytesForSnoring);
                        int bufferSize = frameBytesForSnoring.length/2;
                        Radix2FFT fft = new Radix2FFT(bufferSize);
                        double hzPerDataPoint = 44100d / bufferSize;
                        int fftSize = (int) ((44100d / 2) / (44100d / bufferSize))	;
                        tmpArray = new int[fftSize];
                        for (int k = 0; k < fftSize; k ++) {
                            tmpArray[k] = (int) (k * hzPerDataPoint);
                        }
                        DoubleValues fftData = new DoubleValues();
                        ShortValues shortValues = new ShortValues(tmpBytes);
                        fft.run(shortValues, fftData);
                        allFHAndDB = fftData.getItemsArray();
                        //전체 주파수/데시벨 표시 시작
                        //TODO
                        //전체 주파수/데시벨 표시 끝

                    }
                    i++; //시간 증가

                    //소리 임계치로 소리의 발생 여부를 감지한다.
                    //초기화 설정
                    SleepCheck.setMaxDB(decibel);
                    SleepCheck.setMinDB(decibel);

                    final String amp = String.valueOf(amplitude + "Amp");
                    final String db = String.valueOf(decibel + "db");
                    final String hz = String.valueOf(frequency + "Hz");
                    Log.v(LOG_TAG3,(calcTime(times)+" "+hz +" "+db+" "+amp+" "+decibel+"vs"+SleepCheck.getMaxDB())+","+SleepCheck.getMinDB());

                    //실제로는 1초 이후 분석한다.
                    if (i < 100) {
                        continue;
                    }

                    // 소리가 발생하면 녹음을 시작하고, 1분이상 소리가 발생하지 않으면 녹음을 하지 않는다.
                    if (SleepCheck.noiseCheckForStart(decibel) >= 30 && isRecording == false
                            && Math.floor((double) (audioData.length / (44100d * 16 * 1)) * 8) != Math.floor(times) ) {
                        Log.v(LOG_TAG2,(calcTime(times)+"("+String.format("%.2f", times) + "s) 녹음 시작!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
                        //recordStartingTIme = times;
                        recordStartingTIme = System.currentTimeMillis();
                        baos = new ByteArrayOutputStream();
                        isRecording = true;
                        snoringTermList = new ArrayList<StartEnd>();
                        grindingTermList = new ArrayList<StartEnd>();
                        osaTermList = new ArrayList<StartEnd>();
//                    } else if (isRecording == true && (SleepCheck.noiseCheck(decibel)==0 || recodeFlag==false) ) {
                    } else if (isRecording == true && SleepCheck.noiseCheck(decibel) <= 500) {
                        Log.v(LOG_TAG2,(calcTime(times)+"("+String.format("%.2f", times) + "s) 녹음 종료!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
                        SimpleDateFormat dayTime = new SimpleDateFormat("yyyyMM_dd_HHmm");
                        String fileName = dayTime.format(new Date(recordStartingTIme));
                        dayTime = new SimpleDateFormat("dd_HHmm");
                        //long time = System.currentTimeMillis();
                        long time = System.currentTimeMillis();
                        fileName += "~" + dayTime.format(new Date(time));
                        byte[] waveData = baos.toByteArray();

                        //TODO 녹음된 파일이 저장되는 시점
                        //WaveFormatConverter wfc = new WaveFormatConverter(44100, (short)1, waveData, 0, waveData.length-1);
                        WaveFormatConverter wfc = new WaveFormatConverter();
                        //String[] fileInfo = wfc.saveLongTermWave(fileName, getContext());
                        String[] fileInfo = wfc.saveLongTermMp3(fileName, getContext(), waveData);

                        Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 시작====="));
                        Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (waveData.length / (44100d * 16 * 1))) * 8));
                        Log.v(LOG_TAG2,("tmpMinDb: "+tmpMinDb));
                        Log.v(LOG_TAG2,("tmpMaxDb: "+tmpMaxDb));

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
                                String andRList = gson.toJson(se.printAnalysisRawDataList());
                                ansd.addProperty("analysisData", andRList);

                            }catch(NullPointerException e){
                                e.getMessage();
                            }
                            ansDList.add(ansd);
                        }
                        for(StartEnd se : grindingTermList) {
//                            if(se.end!=0){
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200102);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                try {
                                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                    String andRList = gson.toJson(se.printAnalysisRawDataList());
                                    ansd.addProperty("analysisData", andRList);

                                }catch(NullPointerException e){
                                    e.getMessage();
                                }
                                ansDList.add(ansd);
//                            }
                        }
                        for(StartEnd se : osaTermList) {
//                            if(se.end!=0){
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200103);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                                try {
                                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                    String andRList = gson.toJson(se.printAnalysisRawDataList());
                                    ansd.addProperty("analysisData", andRList);

                                }catch(NullPointerException e){
                                    e.getMessage();
                                }
                                ansDList.add(ansd);
//                            }
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

                    if(isRecording==false) {
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

                    //이갈이 음파가 매우 짧기 때문에, 코골이의 로직과 분리해야한다. 코골이는 0.16초 단위로 분석, 이갈이는 0.01초로 분석해야함
                    //코골이의 음파 길이 및 음파가 아닌 경우의 1초 범위까지 기록 하고 있음으로, 코골이가 아닌 경우에 이갈이인지 체크하도록 한다.
                    //이갈이는 1초 이내에 여러번 발생하며, 발생시에 0.02~0.03초의 연속된 짧고 높은 진폭이 발생한다.이 카운트가 1초에 5회 미만인 것만 뽑아낸다. //
                    //그렇다면 시간 대비 코골이 횟수를 비례해서 계산하면 된다.
                    double chkGrindingDb = SleepCheck.getMinDB();
                    if(chkGrindingDb<=-30) {
                        chkGrindingDb = SleepCheck.getMinDB()/1.5;
                    }else if(chkGrindingDb<=-20) {
                        chkGrindingDb = SleepCheck.getMinDB()/1.25;
                    }else if(chkGrindingDb<=-10) {
                        chkGrindingDb = SleepCheck.getMinDB()/1.1;
                    }
                    if(decibel > chkGrindingDb) {
                        grindingRepeatOnceAmpCnt++;
                        //System.out.print(calcTime(times)+"s ");
                        //System.out.println(" "+decibel+"vs"+chkGrindingDb+" "+grindingRepeatOnceAmpCnt);
                    }else {
                        if( grindingRepeatOnceAmpCnt >= continueCntInChkTermForGrinding) {
                            continueCntInChkTermForGrinding += grindingRepeatOnceAmpCnt;
                            continueCntInChkTermForGrindingChange++;
                        }
                        grindingRepeatOnceAmpCnt = 0;
                    }
                    //음파가 발생하는 구간,
                    //음파가 발생하는 구간이란 코골이가 발생해서 코골이 1회의 시작과 끝을 의미한다.
                    //음파가 발생하는 구간동안 코골이가 발생했는지를 체크 해야 한다.
                    //플래그로 음파가 발생하고 있는지를 관리한다.
                    //소리가 발생하고, 0.5초 간격으로 연속되고 있는지 체크한다.
                    //연속되지 않는 순간 음파가 끝난 것으로 간주한다.
                    //음파가 끝나는 순간 코골이가 발생했는지 체크하고, 코골이가 발생하지 않은 것은 이갈이로 구분한다.
                    double chkSnoringDb = SleepCheck.getMinDB();
                    if(chkSnoringDb<=-30) {
                        chkSnoringDb = SleepCheck.getMinDB()/2;
                    }else if(chkSnoringDb<=-20) {
                        chkSnoringDb = SleepCheck.getMinDB()/1.75;
                    }else if(chkSnoringDb<=-10) {
                        chkSnoringDb = SleepCheck.getMinDB()/1.5;
                    }
                    //코골이 기록용 vo 생성
                    if(snoringTermList.size()>0 && isRecording == true){
                        snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(
                                times, amplitude, decibel, frequency));
                    }
                    if(allFHAndDB!=null) {
                        //코골이는 임계치를 보정해서 코골이의 음파 여부를 판단한다.
                        if(decibel > chkSnoringDb) {
                            //tmpMaxDb = 0;
                            //tmpMinDb = 99999;
                            for(int m = 0 ; m < allFHAndDB.length ; m++){
                                if(allFHAndDB[m] > tmpMaxDb){
                                    tmpMaxDb = allFHAndDB[m];
                                }
                                if(allFHAndDB[m] > tmpMinDb){
                                    tmpMinDb = allFHAndDB[m];
                                }
                            }
                            /*
                            Arrays.stream(allFHAndDB).forEach(e ->{
                                        if( e > tmpMaxDb) {
                                            tmpMaxDb = e;
                                        }
                                        if( e < tmpMinDb) {
                                            tmpMinDb = e;
                                        }
                                    }
                            );
                            */
                            //코골이 음파가 발생했음.
                            if(soundStartInRecording==false) {
                                //코골이 분석 중 이갈이 구별 하기위한 카운트 초기화, 이갈이라면 이 카운트가 매우 높아선 안된다.
                                continueCntInChkTermForGrinding = 0;
                                continueCntInChkTermForGrindingChange = 0;
                                //TODO 음파 진행중일 떄의 평균 데시벨을 가지고, 음파로 인정할 소리를 한번더 구별 한다.
                                chkDBAgainInRecording = decibel;
                                //녹음 중에 소리가 발생했고 음파 시작은 아닌 상태, 음파 시작 상태로 변환
                                soundStartInRecording = true;
                                //코골이 카운트를 초기화(음파 진행 중에 카운트 증가)
                                soundStartAndSnroingCnt = 0;
                                //낮은 주파수 평균이 데시벨의 절반보다 낮다면 코골이 카운트 증가
                                //음파 진행 시간 동안 얼만큼 체크가 안되었는지 카운트를 해서 비교할 수 있다.
                                soundStartAndSnroingOppCnt = 0;
                                //음파시작상태를 0.3초 간격으로 체크하기 위해 변수 할당(초기화)
                                //0.3초 이내에 지속적으로 음파가 발생한다면 이후 음파 발생시 아래 변수와 0.3초 이상 차이가 나지 않아야 한다.
                                soundStartInRecordingTimes = times;
                                //음파시작시간을 보관하기 위해 기록vo를 생성
                                StartEnd st = new StartEnd();
                                st.start = times;
                                st.AnalysisRawDataList = new ArrayList<AnalysisRawData>();
                                st.AnalysisRawDataList.add(new AnalysisRawData(times, amplitude, decibel, frequency));
                                snoringTermList.add(st);
                                //음파가 진행되는 동안 최대 데시벨과 저주파수의 데시벨의 평균을 계산하기 위해 값을 초기화 한다.
                                //최대 데시벨 값과 저주파수 데시벨 값을 저장한다.(초기화)
                                firstDecibelAvg = 0;
                                secondDecibelAvg = 0;
                                snoringDbChkCnt = 0;
                            }else {
                                chkDBAgainInRecording = (chkDBAgainInRecording + decibel) /2;
                                if(firstDecibelAvg == 0 || secondDecibelAvg == 0) {
                                    firstDecibelAvg = calcforChkSnoringDbNotNomarlize(allFHAndDB, 2, 40);
                                    secondDecibelAvg = calcforChkSnoringDbNotNomarlize(allFHAndDB, 10, 18);
                                    snoringDbChkCnt = 0;
                                }else {
                                    if(Math.floor(decibel) >= Math.floor(chkDBAgainInRecording) &&
                                            calcforChkSnoringDbNotNomarlize(allFHAndDB, 10, 18)>calcforChkSnoringDbNotNomarlize(allFHAndDB, 2, 40)) {
                                        //평균으로만 비교하긴 할건데, 평균낼때까지 얼마나 차이가 있었나도 비교해봄.. 값을 쓸 수도 있다.
                                        snoringDbChkCnt++;
                                    }
                                    firstDecibelAvg = (firstDecibelAvg+calcforChkSnoringDbNotNomarlize(allFHAndDB, 2, 40))/2;
                                    secondDecibelAvg = (secondDecibelAvg+calcforChkSnoringDbNotNomarlize(allFHAndDB, 10, 18))/2;
                                }
                            }
                        }else {
                            //소리가 발생하지 않았으면, 현재 코골이 음파 발생중인지 체크 한다.
                            if(soundStartInRecording==true) {
                                //음파 진행 중이라면, 지금 체크중인 체크 시작시간이 1초를 넘었는지 체크한다.
                                if(times-snoringTermList.get(snoringTermList.size()-1).start>0.16*7){
                                    //음파시작시간과는 1초가 벌어졌다면 , 분석을 중단하고, 이후 코골이 발생 카운트를 체크하여 기록한다.
                                    soundStartInRecording = false;
                                    //두번째 데시벨이 더 크게 나타난다.
                                    double  diffMaxToLow = Math.abs(secondDecibelAvg) - Math.abs(firstDecibelAvg);
                                    //차이가 맥시멈 데시벨의 절반 이상인가
                                    if(diffMaxToLow > 0 ) {
                                        //1초가 벌어졌다면, 음파 진행된 동안의 최대 데시벨 평균과 평균 데시벨의 차이를 비교한다.
                                        //낮은 주파수 평균이 데시벨의 절반보다 낮다면 코골이 카운트 증가
                                        //음파 진행 시간 동안 얼만큼 체크가 안되었는지 카운트를 해서 비교할 수 있다.
                                        soundStartAndSnroingCnt++;
                                    }else {
                                        //진행 카운트 증가 안하고 통과
                                        //-> 진행된 카운트 대신 반대 카운트 증가
                                        soundStartAndSnroingOppCnt++;
                                    }
                                    //1. 5~200 주파수의 평균 데시벨보다 43~80 주파수의 평균 데시벨이 더 커야함
                                    //2. 코골이 긍장 카운트 1 당, 부정카운트가 3보다 크면 안된다.(
                                    if(soundStartAndSnroingCnt > 0 && soundStartAndSnroingOppCnt<soundStartAndSnroingCnt*3) {
                                        //코골이 카운트가 증가했었고, 코골이 기록vo에 종료 시간을 기록
                                        snoringTermList.get(snoringTermList.size()-1).end = times;
                                        snoringTermList.get(snoringTermList.size()-1).first = firstDecibelAvg;
                                        snoringTermList.get(snoringTermList.size()-1).second = secondDecibelAvg;
                                        snoringTermList.get(snoringTermList.size()-1).chk = snoringDbChkCnt;
                                        snoringTermList.get(snoringTermList.size()-1).positiveCnt = soundStartAndSnroingCnt;
                                        snoringTermList.get(snoringTermList.size()-1).negitiveCnt = soundStartAndSnroingOppCnt;
                                    }else {
                                        //코골이 카운트가 증가한 적이 없었다.
                                        //코골이 기록 vo 대신 이갈이 기록 vo로 넣는다.
                                        //이갈이는 원본 로직대로 한다.
                                        if(continueCntInChkTermForGrindingChange > 0 && continueCntInChkTermForGrinding> 0 &&
                                                firstDecibelAvg > tmpMaxDb/2 &&
                                                Math.abs(firstDecibelAvg - secondDecibelAvg)<5 &&
                                                //grindingChange가 3이상일 때는, / 가 10보다 크고 12보다 작아야함
                                                ((continueCntInChkTermForGrindingChange >= 3 && continueCntInChkTermForGrinding/continueCntInChkTermForGrindingChange >= 10 && continueCntInChkTermForGrinding/continueCntInChkTermForGrindingChange <= 12)
                                                        ||
                                                        //2이하일 때는, / 가 9보다 작아야함
                                                        (continueCntInChkTermForGrindingChange <=2 && continueCntInChkTermForGrinding/continueCntInChkTermForGrindingChange >= 6 && continueCntInChkTermForGrinding/continueCntInChkTermForGrindingChange <= 9)
                                                )) {
                                            StartEnd st = new StartEnd();
                                            st.start = snoringTermList.get(snoringTermList.size()-1).start;
                                            st.AnalysisRawDataList = snoringTermList.get(snoringTermList.size()-1).AnalysisRawDataList;
                                            st.end = times;
                                            st.second = secondDecibelAvg;
                                            st.first = firstDecibelAvg;
                                            st.chk = secondDecibelAvg-firstDecibelAvg;
                                            st.positiveCnt = continueCntInChkTermForGrinding;
                                            st.negitiveCnt = continueCntInChkTermForGrindingChange;
                                            snoringTermList.remove(snoringTermList.size()-1);
                                            grindingTermList.add(st);
                                        }else {
                                            snoringTermList.remove(snoringTermList.size()-1);
                                        }
                                    }
                                }else {
                                    //음파 진행 중이고, 소리가 발생하지 않았으나 아직 1초가 지나지 않았다.
                                    //진행 카운트 증가 안하고 통과
                                    //-> 진행된 카운트 대신 반대 카운트 증가
                                    //음파 진행 시간 동안 얼만큼 체크가 안되었는지 카운트를 해서 비교할 수 있다.
                                    soundStartAndSnroingOppCnt++;
                                    //snoringTermList.remove(snoringTermList.size()-1);
                                    //soundStartInRecording = false;
                                }
                            }
                            //소리가 발생하지 않았고, 음파가 진행 중인 상태가 아니다.

                            // baos.write(frameBytes);

                        }
                        allFHAndDB = null;
                    }else {
                    }

                    // 무호흡도 기존 로직대로 체크해서, 어디 구간에서 발생했는지 체크한다.
                    osaCnt = SleepCheck.OSACheck(times, decibel, amplitude, frequency);
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
                                        new AnalysisRawData(times, amplitude, decibel, frequency));
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

                    //무호흡의 앞뒤 구간에 코골이가 있는지 체크해서 없으면 삭제
                    if(osaTermList.size()>0){
                        if(snoringTermList.size()<2){
                            //코골이가 2개가 아니고 무호흡 종료 시간으로 부터 5초가 지났으면 일단 삭제
                            double tmpOsaEndTimes = osaTermList.get(osaTermList.size()-1).end;
                            if(times - tmpOsaEndTimes > 5) {
                                osaTermList.remove(osaTermList.get(osaTermList.size() - 1));
                            }
                        }else{
                            //코골이가 2개 이상이라면 무호흡의 시작과 끝에 코골이 분석이 걸리는지 체크
                            double tmpOsaStartTimes = osaTermList.get(osaTermList.size()-1).start;
                            double tmpOsaEndTimes = osaTermList.get(osaTermList.size()-1).end;
                            double tmpBeforeSnoringTImes = snoringTermList.get(snoringTermList.size()-1).end;
                            double tmpAfterSnoringTImes = snoringTermList.get(snoringTermList.size()-1).start;
                            if(tmpOsaStartTimes - tmpBeforeSnoringTImes < 5 && tmpAfterSnoringTImes - tmpOsaEndTimes < 5 ){
                                //무호흡 시작시간 5초 이내에 코골이가 종료가 발생하고, 무호흡 종료시간 5초 이내에 코골이가 시작이 발생했어야 한다.
                            }else{
                                //아니면 삭제
                                osaTermList.remove(osaTermList.get(osaTermList.size()-1));
                            }
                        }

                    }
                    SleepCheck.curTermTime = times;
                    SleepCheck.curTermDb = decibel;
                    SleepCheck.curTermAmp = amplitude;
                    SleepCheck.curTermHz = frequency;

                    SleepCheck.checkTerm++;
                    SleepCheck.checkTermSecond = (int) Math.floor(times);

                    //                    catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }
                if (isRecording == true && recodeFlag==false) {
                    Log.v(LOG_TAG2,(calcTime(times)+"("+String.format("%.2f", times) + "s) 녹음 종료 버튼을 눌러서 현재 진행되던 녹음을 종료!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
                    SimpleDateFormat dayTime = new SimpleDateFormat("yyyyMM_dd_HHmm");
                    String fileName = dayTime.format(new Date(recordStartingTIme));
                    dayTime = new SimpleDateFormat("dd_HHmm");
                    //long time = System.currentTimeMillis();
                    long time = System.currentTimeMillis();
                    fileName += "~" + dayTime.format(new Date(time));
                    byte[] waveData = baos.toByteArray();

                    //TODO 녹음된 파일이 저장되는 시점
                    //WaveFormatConverter wfc = new WaveFormatConverter(44100, (short)1, waveData, 0, waveData.length-1);
                    WaveFormatConverter wfc = new WaveFormatConverter();
                    String[] fileInfo = wfc.saveLongTermMp3(fileName, getContext(), waveData);

                    Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 시작====="));
                    Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (waveData.length / (44100d * 16 * 1))) * 8));
                    Log.v(LOG_TAG2,("tmpMinDb: "+tmpMinDb));
                    Log.v(LOG_TAG2,("tmpMaxDb: "+tmpMaxDb));

                    JsonObject ans = new JsonObject();
                    ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                    ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                    ans.addProperty("analysisFileAppPath",fileInfo[0]);
                    ans.addProperty("analysisFileNm",fileInfo[1]);
                    JsonArray ansDList = new JsonArray();
                    JsonObject ansd = new JsonObject();
                    for(StartEnd se : snoringTermList) {
                        if(se.end!=0){
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200101);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                            try {
                                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                String andRList = gson.toJson(se.printAnalysisRawDataList());
                                ansd.addProperty("analysisData", andRList);

                            }catch(NullPointerException e){
                                e.getMessage();
                            }
                            ansDList.add(ansd);
                        }else{
                            snoringTermList.remove(se);
                        }
                    }
                    for(StartEnd se : grindingTermList) {
                        if(se.end!=0){
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200102);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.end*1000))));
                            try {
                                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                String andRList = gson.toJson(se.printAnalysisRawDataList());
                                ansd.addProperty("analysisData", andRList);

                            }catch(NullPointerException e){
                                e.getMessage();
                            }
                            ansDList.add(ansd);
                        }else{
                            grindingTermList.remove(se);
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
                                String andRList = gson.toJson(se.printAnalysisRawDataList());
                                ansd.addProperty("analysisData", andRList);

                            }catch(NullPointerException e){
                                e.getMessage();
                            }
                            ansDList.add(ansd);
                        }else{
                            osaTermList.remove(se);
                        }
                    }
                    ans.add("analysisDetailsList", ansDList);
                    ansList.add(ans);

                    Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 끝====="));
                    recordStartingTIme = 0;
                    isRecording = false;
                }

                recordData = new JsonObject();
                recordData.addProperty("userAppId",userAppId);
                recordData.addProperty("recordStartDt",recordStartDt);
                recordData.addProperty("recordEndDt",recordEndDt);
                recordData.add("analysisList", ansList);

                Log.v(LOG_TAG2,(" =============녹음 종료버튼  ===========recordData: "+recordData.toString()));

                //Log.v(LOG_TAG2,("audio length(s): " + ((double) (audioData.length / (44100d * 16 * 1))) * 8));
                Log.v(LOG_TAG2,("녹음시작-종료(s): " + String.format("%.2f", times)));

                Log.v(LOG_TAG2,( "코골이 구간 시작=========="));
                Log.v(LOG_TAG2,( "코골이 " + snoringTermList.size()+"회 발생 "));
                for(StartEnd se : snoringTermList) {
                    Log.v(LOG_TAG2,(se.getTerm()));
                }
                Log.v(LOG_TAG2,( "코골이 구간 끝=========="));
                Log.v(LOG_TAG2,( "이갈이 구간 시작=========="));
                Log.v(LOG_TAG2,( "이갈이 " + grindingTermList.size()+"회 발생 "));
                for(StartEnd se : grindingTermList) {
                    Log.v(LOG_TAG2,(se.getTerm()));
                }
                Log.v(LOG_TAG2,( "이갈이 구간 끝=========="));
                Log.v(LOG_TAG2,( "이갈이 구간 시작=========="));
                Log.v(LOG_TAG2,( "무호흡" + osaTermList.size()+"회 발생 "));
                for(StartEnd se : osaTermList) {
                    Log.v(LOG_TAG2,(se.getTerm()));
                }
                Log.v(LOG_TAG2,( "이갈이 구간 끝=========="));

                Log.v(LOG_TAG2, String.format("Recording  has stopped. Samples read: %d", shortsRead));
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

    private double calcforChkSnoringDbNotNomarlize(double[] allFHAndDB, int startN, int endN) {
        double forChkSnroingDb = 0;
        for (int i = 0; i <= endN - startN; i++) {
            forChkSnroingDb += allFHAndDB[startN+i];
        }
        forChkSnroingDb = Math.abs((forChkSnroingDb) / (endN - startN + 1));
        return forChkSnroingDb;
    }
    private double calcforChkSnoringDb(double[] allFHAndDB, int startN, int endN) {
        double forChkSnroingDb = 0;
        for (int i = 0; i <= endN - startN; i++) {
            //소리 발생체크하는 fft로직과 전체 주파수 데시벨을 가져오는 fft 로직이 달라서, 후자의 fft 데시벨 수치를 -31.5에 맞게 보정한다.
            //샘플 fft 예제 및 실제 측정 결과 -75~87까지의 수치가 발생하는 것까지 확인함.
            //이를 평준화 하기 위해 90을 임계치로 -31.5 db로 변환한다.
            if(Math.abs(allFHAndDB[startN+i])>90) {
                allFHAndDB[startN+i] = 90;
            }
            forChkSnroingDb += allFHAndDB[startN+i];
        }
        forChkSnroingDb = -(31.5 - (Math.abs((forChkSnroingDb) / (endN - startN + 1)) / 90) * 31.5);
        return forChkSnroingDb;
    }
    private short[] getAmplitudesFromBytesShort(byte[] bytes) {
        short[] amps = new short[bytes.length / 2];
        for (int i = 0; i < bytes.length; i += 2) {
            short buff = bytes[i + 1];
            short buff2 = bytes[i];

            buff = (short) ((buff & 0xFF) << 8);
            buff2 = (short) (buff2 & 0xFF);

            short res = (short) (buff | buff2);
            amps[i == 0 ? 0 : i / 2] = res;
        }
        return amps;
    }
    private String calcTime(double times) {
        int seconds;
        int minutes ;
        int hours;
        seconds =  (int)times;
        hours = seconds / 3600;
        minutes = (seconds%3600)/60;
        double seconds_output = (times% 3600)%60;
        seconds_output = Math.floor(seconds_output*1000)/1000;
        return hours  + ":" + minutes + ":" + seconds_output +"";
    }
}

class StartEnd {
    public int negitiveCnt;
    public int positiveCnt;
    double start;
    double end;
    List<AnalysisRawData> AnalysisRawDataList;
    double second;
    double first;
    double chk;

    public String getTerm() {
        return
                String.format("%.2f", start)
                        + "~" + String.format("%.2f", end)
                        + " second: " + String.format("%.2f", second)
                        + " first: " + String.format("%.2f", first)
                        + " chk: " + String.format("%.2f", chk)
                        + " positiveCnt: " + positiveCnt
                        + " negitiveCnt: " + negitiveCnt;
    }

    public String getTermForRequest(int termCd, long recordStartingTIme) {
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return "termTypeCd: " + termCd + ", termStartDt: "
                + dayTime.format(new Date((long) (recordStartingTIme + this.start * 1000))) + ",termEndDt: "
                + dayTime.format(new Date((long) (recordStartingTIme + this.end * 1000)));
    }

//    public String printAnalysisRawDataList() {
//        String rtn = "";
//        if(this.AnalysisRawDataList!=null) {
//            for(AnalysisRawData d : this.AnalysisRawDataList) {
//                rtn+=d.toString()+"\r\n";
//            }
//        }
//        return rtn;
//    }

    public JsonArray printAnalysisRawDataList() {
        JsonArray rtn = new JsonArray();
        if(this.AnalysisRawDataList!=null) {
            for(AnalysisRawData d : this.AnalysisRawDataList) {
//                rtn+=d.toString()+"\r\n";
                rtn.add(d.toString());
            }
        }
        return rtn;
    }
}


