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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.ResultActivity;
import kr.co.dwebss.kococo.fragment.recorder.RecordingThread;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.util.FileUtil;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import kr.co.dwebss.kococo.util.SimpleLame;
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
    public RecordFragment(TabEventUtil tabEventUtil) {
        // Required empty public constructor
        this.tabEventUtil = tabEventUtil;
    }

    Button testBtn;
    boolean testFlag=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //화면이 자동으로 꺼지는것을 방지한다.
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();

        View v = inflater.inflate(R.layout.fragment_record, container, false);
        recodeBtn = (Button) v.findViewById(R.id.recodeBtn) ;
        recodeFlag = false;
        recodeBtn.setText("녹음 시작");

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
                }else{
                    Toast.makeText(getActivity(), "분석중입니다 잠시만 기다려주세요...", Toast.LENGTH_LONG).show();
                    recodeFlag = false;
                    stop(v);

                    //기존 밝기로 복귀
                    params.screenBrightness = (float) now_bright_status/100;
                    getActivity().getWindow().setAttributes(params);

                    //탭 막기 해제
                    tabEventUtil.tabEvent(recodeFlag);
//                //정상 테스트 데이터
////                String testDt = "{\"userAppId\":\"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f\",\"recordId\":86,\"recordStartD\":\"2019-05-29\",\"recordStartDt\":\"2019-05-29T16:10:31\",\"recordEndD\":\"2019-05-29\",\"recordEndDt\":\"2019-05-29T16:10:54\",\"consultingYn\":\"N\",\"consultingReplyYn\":\"N\",\"analysisList\":[{\"analysisId\":63,\"analysisStartD\":\"2019-05-29T16:10:34\",\"analysisStartDt\":\"2019-05-29T16:10:34\",\"analysisEndD\":\"2019-05-29T16:10:54\",\"analysisEndDt\":\"2019-05-29T16:10:54\",\"analysisFileNm\":\"snoring-20190605_1708-05_1709_1559722170788.mp3\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/8\",\"analysisServerUploadYn\":\"N\",\"claimYn\":\"N\",\"analysisDetailsList\":[{\"analysisDetailsId\":65,\"termTypeCd\":200102,\"termStartDt\":\"2019-05-29T16:10:36\",\"termEndDt\":\"2019-05-29T16:10:40\"}],\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"}}}],\"_links\":{\"self\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"admin\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/admin\"},\"user\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/user\"}}}";
//                String testDt = "{\"userAppId\":\"69de163b-9fd0-4a7b-a063-fbe4bd81381f\",\"recordId\":290,\"recordStartD\":\"2019-06-14\",\"recordStartDt\":\"2019-06-14T11:28:33\",\"recordEndD\":\"2019-06-14\",\"recordEndDt\":\"2019-06-14T11:35:24\",\"consultingYn\":\"N\",\"consultingReplyYn\":\"N\",\"analysisList\":[{\"analysisId\":443,\"analysisStartD\":\"2019-06-14T00:00:00\",\"analysisStartDt\":\"2019-06-14T11:28:35\",\"analysisEndD\":\"2019-06-14T00:00:00\",\"analysisEndDt\":\"2019-06-14T11:34:19\",\"analysisFileNm\":\"snoring-201906_14_1128~14_1134_1560479659927.mp3\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/5\",\"analysisServerUploadYn\":\"N\",\"analysisDetailsList\":[{\"analysisId\":443,\"analysisDetailsId\":394,\"termTypeCd\":200101,\"termStartDt\":\"2019-06-14T11:28:53\",\"termEndDt\":\"2019-06-14T11:28:56\",\"analysisData\":\"[{\\\"TIME\\\":\\\"20\\\",\\\"DB\\\":\\\"34.00\\\"},{\\\"TIME\\\":\\\"21\\\",\\\"DB\\\":\\\"55.10\\\"},{\\\"TIME\\\":\\\"22\\\",\\\"DB\\\":\\\"61.89\\\"},{\\\"TIME\\\":\\\"23\\\",\\\"DB\\\":\\\"57.63\\\"}]\"},{\"analysisId\":443,\"analysisDetailsId\":395,\"termTypeCd\":200101,\"termStartDt\":\"2019-06-14T11:28:58\",\"termEndDt\":\"2019-06-14T11:29:00\",\"analysisData\":\"[{\\\"TIME\\\":\\\"25\\\",\\\"DB\\\":\\\"38.20\\\"},{\\\"TIME\\\":\\\"26\\\",\\\"DB\\\":\\\"49.57\\\"},{\\\"TIME\\\":\\\"27\\\",\\\"DB\\\":\\\"49.61\\\"}]\"},{\"analysisId\":443,\"analysisDetailsId\":396,\"termTypeCd\":200101,\"termStartDt\":\"2019-06-14T11:29:14\",\"termEndDt\":\"2019-06-14T11:29:18\",\"analysisData\":\"[{\\\"TIME\\\":\\\"40\\\",\\\"DB\\\":\\\"67.08\\\"},{\\\"TIME\\\":\\\"41\\\",\\\"DB\\\":\\\"62.97\\\"},{\\\"TIME\\\":\\\"42\\\",\\\"DB\\\":\\\"43.33\\\"},{\\\"TIME\\\":\\\"43\\\",\\\"DB\\\":\\\"41.28\\\"},{\\\"TIME\\\":\\\"44\\\",\\\"DB\\\":\\\"64.45\\\"}]\"},{\"analysisId\":443,\"analysisDetailsId\":397,\"termTypeCd\":200101,\"termStartDt\":\"2019-06-14T11:29:29\",\"termEndDt\":\"2019-06-14T11:29:43\",\"analysisData\":\"[{\\\"TIME\\\":\\\"56\\\",\\\"DB\\\":\\\"44.20\\\"},{\\\"TIME\\\":\\\"57\\\",\\\"DB\\\":\\\"51.38\\\"},{\\\"TIME\\\":\\\"58\\\",\\\"DB\\\":\\\"67.85\\\"},{\\\"TIME\\\":\\\"59\\\",\\\"DB\\\":\\\"57.69\\\"},{\\\"TIME\\\":\\\"60\\\",\\\"DB\\\":\\\"62.49\\\"},{\\\"TIME\\\":\\\"61\\\",\\\"DB\\\":\\\"46.97\\\"},{\\\"TIME\\\":\\\"62\\\",\\\"DB\\\":\\\"47.50\\\"},{\\\"TIME\\\":\\\"63\\\",\\\"DB\\\":\\\"45.55\\\"},{\\\"TIME\\\":\\\"64\\\",\\\"DB\\\":\\\"66.78\\\"},{\\\"TIME\\\":\\\"65\\\",\\\"DB\\\":\\\"57.25\\\"},{\\\"TIME\\\":\\\"66\\\",\\\"DB\\\":\\\"61.95\\\"},{\\\"TIME\\\":\\\"67\\\",\\\"DB\\\":\\\"58.09\\\"},{\\\"TIME\\\":\\\"68\\\",\\\"DB\\\":\\\"61.24\\\"},{\\\"TIME\\\":\\\"69\\\",\\\"DB\\\":\\\"58.81\\\"},{\\\"TIME\\\":\\\"70\\\",\\\"DB\\\":\\\"55.81\\\"}]\"}],\"claimYn\":\"N\",\"recordingData\":\"[{\\\"TIME\\\":\\\"1\\\",\\\"DB\\\":\\\"26.32\\\"},{\\\"TIME\\\":\\\"2\\\",\\\"DB\\\":\\\"26.32\\\"},{\\\"TIME\\\":\\\"3\\\",\\\"DB\\\":\\\"59.21\\\"},{\\\"TIME\\\":\\\"4\\\",\\\"DB\\\":\\\"58.29\\\"},{\\\"TIME\\\":\\\"5\\\",\\\"DB\\\":\\\"31.96\\\"},{\\\"TIME\\\":\\\"6\\\",\\\"DB\\\":\\\"25.64\\\"},{\\\"TIME\\\":\\\"7\\\",\\\"DB\\\":\\\"26.48\\\"},{\\\"TIME\\\":\\\"8\\\",\\\"DB\\\":\\\"26.75\\\"},{\\\"TIME\\\":\\\"9\\\",\\\"DB\\\":\\\"26.93\\\"},{\\\"TIME\\\":\\\"10\\\",\\\"DB\\\":\\\"27.49\\\"},{\\\"TIME\\\":\\\"11\\\",\\\"DB\\\":\\\"27.88\\\"},{\\\"TIME\\\":\\\"12\\\",\\\"DB\\\":\\\"54.28\\\"},{\\\"TIME\\\":\\\"13\\\",\\\"DB\\\":\\\"62.81\\\"},{\\\"TIME\\\":\\\"14\\\",\\\"DB\\\":\\\"61.70\\\"},{\\\"TIME\\\":\\\"15\\\",\\\"DB\\\":\\\"33.09\\\"},{\\\"TIME\\\":\\\"16\\\",\\\"DB\\\":\\\"38.62\\\"},{\\\"TIME\\\":\\\"17\\\",\\\"DB\\\":\\\"56.55\\\"},{\\\"TIME\\\":\\\"18\\\",\\\"DB\\\":\\\"62.37\\\"},{\\\"TIME\\\":\\\"19\\\",\\\"DB\\\":\\\"40.34\\\"},{\\\"TIME\\\":\\\"20\\\",\\\"DB\\\":\\\"34.00\\\"},{\\\"TIME\\\":\\\"21\\\",\\\"DB\\\":\\\"55.10\\\"},{\\\"TIME\\\":\\\"22\\\",\\\"DB\\\":\\\"61.89\\\"},{\\\"TIME\\\":\\\"23\\\",\\\"DB\\\":\\\"57.63\\\"},{\\\"TIME\\\":\\\"24\\\",\\\"DB\\\":\\\"35.05\\\"},{\\\"TIME\\\":\\\"25\\\",\\\"DB\\\":\\\"38.20\\\"},{\\\"TIME\\\":\\\"26\\\",\\\"DB\\\":\\\"49.57\\\"},{\\\"TIME\\\":\\\"27\\\",\\\"DB\\\":\\\"49.61\\\"},{\\\"TIME\\\":\\\"28\\\",\\\"DB\\\":\\\"36.60\\\"},{\\\"TIME\\\":\\\"29\\\",\\\"DB\\\":\\\"42.02\\\"},{\\\"TIME\\\":\\\"30\\\",\\\"DB\\\":\\\"35.40\\\"},{\\\"TIME\\\":\\\"31\\\",\\\"DB\\\":\\\"65.43\\\"},{\\\"TIME\\\":\\\"32\\\",\\\"DB\\\":\\\"57.37\\\"},{\\\"TIME\\\":\\\"33\\\",\\\"DB\\\":\\\"37.85\\\"},{\\\"TIME\\\":\\\"34\\\",\\\"DB\\\":\\\"39.50\\\"},{\\\"TIME\\\":\\\"35\\\",\\\"DB\\\":\\\"56.34\\\"},{\\\"TIME\\\":\\\"36\\\",\\\"DB\\\":\\\"64.33\\\"},{\\\"TIME\\\":\\\"37\\\",\\\"DB\\\":\\\"58.14\\\"},{\\\"TIME\\\":\\\"38\\\",\\\"DB\\\":\\\"39.38\\\"},{\\\"TIME\\\":\\\"39\\\",\\\"DB\\\":\\\"46.30\\\"},{\\\"TIME\\\":\\\"40\\\",\\\"DB\\\":\\\"67.08\\\"},{\\\"TIME\\\":\\\"41\\\",\\\"DB\\\":\\\"62.97\\\"},{\\\"TIME\\\":\\\"42\\\",\\\"DB\\\":\\\"43.33\\\"},{\\\"TIME\\\":\\\"43\\\",\\\"DB\\\":\\\"41.28\\\"},{\\\"TIME\\\":\\\"44\\\",\\\"DB\\\":\\\"64.45\\\"},{\\\"TIME\\\":\\\"45\\\",\\\"DB\\\":\\\"59.63\\\"},{\\\"TIME\\\":\\\"46\\\",\\\"DB\\\":\\\"39.32\\\"},{\\\"TIME\\\":\\\"47\\\",\\\"DB\\\":\\\"41.16\\\"},{\\\"TIME\\\":\\\"48\\\",\\\"DB\\\":\\\"67.91\\\"},{\\\"TIME\\\":\\\"49\\\",\\\"DB\\\":\\\"64.98\\\"},{\\\"TIME\\\":\\\"50\\\",\\\"DB\\\":\\\"59.45\\\"},{\\\"TIME\\\":\\\"51\\\",\\\"DB\\\":\\\"39.94\\\"},{\\\"TIME\\\":\\\"52\\\",\\\"DB\\\":\\\"40.74\\\"},{\\\"TIME\\\":\\\"53\\\",\\\"DB\\\":\\\"65.09\\\"},{\\\"TIME\\\":\\\"54\\\",\\\"DB\\\":\\\"64.96\\\"},{\\\"TIME\\\":\\\"55\\\",\\\"DB\\\":\\\"42.82\\\"},{\\\"TIME\\\":\\\"56\\\",\\\"DB\\\":\\\"44.20\\\"},{\\\"TIME\\\":\\\"57\\\",\\\"DB\\\":\\\"51.38\\\"},{\\\"TIME\\\":\\\"58\\\",\\\"DB\\\":\\\"67.85\\\"},{\\\"TIME\\\":\\\"59\\\",\\\"DB\\\":\\\"57.69\\\"},{\\\"TIME\\\":\\\"60\\\",\\\"DB\\\":\\\"62.49\\\"},{\\\"TIME\\\":\\\"61\\\",\\\"DB\\\":\\\"46.97\\\"},{\\\"TIME\\\":\\\"62\\\",\\\"DB\\\":\\\"47.50\\\"},{\\\"TIME\\\":\\\"63\\\",\\\"DB\\\":\\\"45.55\\\"},{\\\"TIME\\\":\\\"64\\\",\\\"DB\\\":\\\"66.78\\\"},{\\\"TIME\\\":\\\"65\\\",\\\"DB\\\":\\\"57.25\\\"},{\\\"TIME\\\":\\\"66\\\",\\\"DB\\\":\\\"61.95\\\"},{\\\"TIME\\\":\\\"67\\\",\\\"DB\\\":\\\"58.09\\\"},{\\\"TIME\\\":\\\"68\\\",\\\"DB\\\":\\\"61.24\\\"},{\\\"TIME\\\":\\\"69\\\",\\\"DB\\\":\\\"58.81\\\"},{\\\"TIME\\\":\\\"70\\\",\\\"DB\\\":\\\"55.81\\\"},{\\\"TIME\\\":\\\"71\\\",\\\"DB\\\":\\\"58.60\\\"},{\\\"TIME\\\":\\\"72\\\",\\\"DB\\\":\\\"44.31\\\"},{\\\"TIME\\\":\\\"73\\\",\\\"DB\\\":\\\"49.59\\\"},{\\\"TIME\\\":\\\"74\\\",\\\"DB\\\":\\\"50.64\\\"},{\\\"TIME\\\":\\\"75\\\",\\\"DB\\\":\\\"50.72\\\"},{\\\"TIME\\\":\\\"76\\\",\\\"DB\\\":\\\"48.64\\\"},{\\\"TIME\\\":\\\"77\\\",\\\"DB\\\":\\\"45.98\\\"},{\\\"TIME\\\":\\\"78\\\",\\\"DB\\\":\\\"48.94\\\"},{\\\"TIME\\\":\\\"79\\\",\\\"DB\\\":\\\"47.56\\\"},{\\\"TIME\\\":\\\"80\\\",\\\"DB\\\":\\\"48.34\\\"},{\\\"TIME\\\":\\\"81\\\",\\\"DB\\\":\\\"48.59\\\"},{\\\"TIME\\\":\\\"82\\\",\\\"DB\\\":\\\"51.70\\\"},{\\\"TIME\\\":\\\"83\\\",\\\"DB\\\":\\\"52.73\\\"},{\\\"TIME\\\":\\\"84\\\",\\\"DB\\\":\\\"52.02\\\"},{\\\"TIME\\\":\\\"85\\\",\\\"DB\\\":\\\"53.50\\\"},{\\\"TIME\\\":\\\"86\\\",\\\"DB\\\":\\\"51.60\\\"},{\\\"TIME\\\":\\\"87\\\",\\\"DB\\\":\\\"54.90\\\"},{\\\"TIME\\\":\\\"88\\\",\\\"DB\\\":\\\"53.79\\\"},{\\\"TIME\\\":\\\"89\\\",\\\"DB\\\":\\\"53.34\\\"},{\\\"TIME\\\":\\\"90\\\",\\\"DB\\\":\\\"54.53\\\"},{\\\"TIME\\\":\\\"91\\\",\\\"DB\\\":\\\"58.41\\\"},{\\\"TIME\\\":\\\"92\\\",\\\"DB\\\":\\\"59.19\\\"},{\\\"TIME\\\":\\\"93\\\",\\\"DB\\\":\\\"49.08\\\"},{\\\"TIME\\\":\\\"94\\\",\\\"DB\\\":\\\"59.29\\\"},{\\\"TIME\\\":\\\"95\\\",\\\"DB\\\":\\\"55.92\\\"},{\\\"TIME\\\":\\\"96\\\",\\\"DB\\\":\\\"58.98\\\"},{\\\"TIME\\\":\\\"97\\\",\\\"DB\\\":\\\"57.90\\\"},{\\\"TIME\\\":\\\"98\\\",\\\"DB\\\":\\\"57.89\\\"},{\\\"TIME\\\":\\\"99\\\",\\\"DB\\\":\\\"45.47\\\"},{\\\"TIME\\\":\\\"100\\\",\\\"DB\\\":\\\"45.96\\\"},{\\\"TIME\\\":\\\"101\\\",\\\"DB\\\":\\\"47.18\\\"},{\\\"TIME\\\":\\\"102\\\",\\\"DB\\\":\\\"48.72\\\"},{\\\"TIME\\\":\\\"103\\\",\\\"DB\\\":\\\"39.94\\\"},{\\\"TIME\\\":\\\"104\\\",\\\"DB\\\":\\\"29.89\\\"},{\\\"TIME\\\":\\\"105\\\",\\\"DB\\\":\\\"29.52\\\"},{\\\"TIME\\\":\\\"106\\\",\\\"DB\\\":\\\"32.70\\\"},{\\\"TIME\\\":\\\"107\\\",\\\"DB\\\":\\\"44.67\\\"},{\\\"TIME\\\":\\\"108\\\",\\\"DB\\\":\\\"61.49\\\"},{\\\"TIME\\\":\\\"109\\\",\\\"DB\\\":\\\"57.35\\\"},{\\\"TIME\\\":\\\"110\\\",\\\"DB\\\":\\\"56.98\\\"},{\\\"TIME\\\":\\\"111\\\",\\\"DB\\\":\\\"58.03\\\"},{\\\"TIME\\\":\\\"112\\\",\\\"DB\\\":\\\"50.72\\\"},{\\\"TIME\\\":\\\"113\\\",\\\"DB\\\":\\\"52.92\\\"},{\\\"TIME\\\":\\\"114\\\",\\\"DB\\\":\\\"43.83\\\"},{\\\"TIME\\\":\\\"115\\\",\\\"DB\\\":\\\"30.52\\\"},{\\\"TIME\\\":\\\"116\\\",\\\"DB\\\":\\\"31.32\\\"},{\\\"TIME\\\":\\\"117\\\",\\\"DB\\\":\\\"30.06\\\"},{\\\"TIME\\\":\\\"118\\\",\\\"DB\\\":\\\"30.91\\\"},{\\\"TIME\\\":\\\"119\\\",\\\"DB\\\":\\\"30.04\\\"},{\\\"TIME\\\":\\\"120\\\",\\\"DB\\\":\\\"48.79\\\"},{\\\"TIME\\\":\\\"121\\\",\\\"DB\\\":\\\"58.87\\\"},{\\\"TIME\\\":\\\"122\\\",\\\"DB\\\":\\\"53.45\\\"},{\\\"TIME\\\":\\\"123\\\",\\\"DB\\\":\\\"46.73\\\"},{\\\"TIME\\\":\\\"124\\\",\\\"DB\\\":\\\"43.34\\\"},{\\\"TIME\\\":\\\"125\\\",\\\"DB\\\":\\\"36.38\\\"},{\\\"TIME\\\":\\\"126\\\",\\\"DB\\\":\\\"38.14\\\"},{\\\"TIME\\\":\\\"127\\\",\\\"DB\\\":\\\"28.83\\\"},{\\\"TIME\\\":\\\"128\\\",\\\"DB\\\":\\\"33.52\\\"},{\\\"TIME\\\":\\\"129\\\",\\\"DB\\\":\\\"54.44\\\"},{\\\"TIME\\\":\\\"130\\\",\\\"DB\\\":\\\"53.29\\\"},{\\\"TIME\\\":\\\"131\\\",\\\"DB\\\":\\\"63.83\\\"},{\\\"TIME\\\":\\\"132\\\",\\\"DB\\\":\\\"52.21\\\"},{\\\"TIME\\\":\\\"133\\\",\\\"DB\\\":\\\"51.58\\\"},{\\\"TIME\\\":\\\"134\\\",\\\"DB\\\":\\\"53.57\\\"},{\\\"TIME\\\":\\\"135\\\",\\\"DB\\\":\\\"55.97\\\"},{\\\"TIME\\\":\\\"136\\\",\\\"DB\\\":\\\"59.79\\\"},{\\\"TIME\\\":\\\"137\\\",\\\"DB\\\":\\\"57.60\\\"},{\\\"TIME\\\":\\\"138\\\",\\\"DB\\\":\\\"52.68\\\"},{\\\"TIME\\\":\\\"139\\\",\\\"DB\\\":\\\"46.58\\\"},{\\\"TIME\\\":\\\"140\\\",\\\"DB\\\":\\\"47.35\\\"},{\\\"TIME\\\":\\\"141\\\",\\\"DB\\\":\\\"28.58\\\"},{\\\"TIME\\\":\\\"142\\\",\\\"DB\\\":\\\"27.48\\\"},{\\\"TIME\\\":\\\"143\\\",\\\"DB\\\":\\\"25.43\\\"},{\\\"TIME\\\":\\\"144\\\",\\\"DB\\\":\\\"27.00\\\"},{\\\"TIME\\\":\\\"145\\\",\\\"DB\\\":\\\"29.10\\\"},{\\\"TIME\\\":\\\"146\\\",\\\"DB\\\":\\\"33.58\\\"},{\\\"TIME\\\":\\\"147\\\",\\\"DB\\\":\\\"28.57\\\"},{\\\"TIME\\\":\\\"148\\\",\\\"DB\\\":\\\"28.79\\\"},{\\\"TIME\\\":\\\"149\\\",\\\"DB\\\":\\\"28.23\\\"},{\\\"TIME\\\":\\\"150\\\",\\\"DB\\\":\\\"28.08\\\"},{\\\"TIME\\\":\\\"151\\\",\\\"DB\\\":\\\"26.76\\\"},{\\\"TIME\\\":\\\"152\\\",\\\"DB\\\":\\\"25.38\\\"},{\\\"TIME\\\":\\\"153\\\",\\\"DB\\\":\\\"47.08\\\"},{\\\"TIME\\\":\\\"154\\\",\\\"DB\\\":\\\"27.08\\\"},{\\\"TIME\\\":\\\"155\\\",\\\"DB\\\":\\\"26.95\\\"},{\\\"TIME\\\":\\\"156\\\",\\\"DB\\\":\\\"25.04\\\"},{\\\"TIME\\\":\\\"157\\\",\\\"DB\\\":\\\"28.34\\\"},{\\\"TIME\\\":\\\"158\\\",\\\"DB\\\":\\\"26.91\\\"},{\\\"TIME\\\":\\\"159\\\",\\\"DB\\\":\\\"27.66\\\"},{\\\"TIME\\\":\\\"160\\\",\\\"DB\\\":\\\"27.79\\\"},{\\\"TIME\\\":\\\"161\\\",\\\"DB\\\":\\\"28.13\\\"},{\\\"TIME\\\":\\\"162\\\",\\\"DB\\\":\\\"52.68\\\"},{\\\"TIME\\\":\\\"163\\\",\\\"DB\\\":\\\"33.44\\\"},{\\\"TIME\\\":\\\"164\\\",\\\"DB\\\":\\\"28.33\\\"},{\\\"TIME\\\":\\\"165\\\",\\\"DB\\\":\\\"32.30\\\"},{\\\"TIME\\\":\\\"166\\\",\\\"DB\\\":\\\"27.04\\\"},{\\\"TIME\\\":\\\"167\\\",\\\"DB\\\":\\\"28.17\\\"},{\\\"TIME\\\":\\\"168\\\",\\\"DB\\\":\\\"28.15\\\"},{\\\"TIME\\\":\\\"169\\\",\\\"DB\\\":\\\"29.80\\\"},{\\\"TIME\\\":\\\"170\\\",\\\"DB\\\":\\\"29.84\\\"},{\\\"TIME\\\":\\\"171\\\",\\\"DB\\\":\\\"26.75\\\"},{\\\"TIME\\\":\\\"172\\\",\\\"DB\\\":\\\"27.36\\\"},{\\\"TIME\\\":\\\"173\\\",\\\"DB\\\":\\\"26.12\\\"},{\\\"TIME\\\":\\\"174\\\",\\\"DB\\\":\\\"40.11\\\"},{\\\"TIME\\\":\\\"175\\\",\\\"DB\\\":\\\"27.44\\\"},{\\\"TIME\\\":\\\"176\\\",\\\"DB\\\":\\\"27.38\\\"},{\\\"TIME\\\":\\\"177\\\",\\\"DB\\\":\\\"26.97\\\"},{\\\"TIME\\\":\\\"178\\\",\\\"DB\\\":\\\"32.53\\\"},{\\\"TIME\\\":\\\"179\\\",\\\"DB\\\":\\\"55.52\\\"},{\\\"TIME\\\":\\\"180\\\",\\\"DB\\\":\\\"50.40\\\"},{\\\"TIME\\\":\\\"181\\\",\\\"DB\\\":\\\"49.80\\\"},{\\\"TIME\\\":\\\"182\\\",\\\"DB\\\":\\\"44.88\\\"},{\\\"TIME\\\":\\\"183\\\",\\\"DB\\\":\\\"31.09\\\"},{\\\"TIME\\\":\\\"184\\\",\\\"DB\\\":\\\"35.85\\\"},{\\\"TIME\\\":\\\"185\\\",\\\"DB\\\":\\\"41.21\\\"},{\\\"TIME\\\":\\\"186\\\",\\\"DB\\\":\\\"23.26\\\"},{\\\"TIME\\\":\\\"187\\\",\\\"DB\\\":\\\"29.38\\\"},{\\\"TIME\\\":\\\"188\\\",\\\"DB\\\":\\\"23.35\\\"},{\\\"TIME\\\":\\\"189\\\",\\\"DB\\\":\\\"36.26\\\"},{\\\"TIME\\\":\\\"190\\\",\\\"DB\\\":\\\"23.16\\\"},{\\\"TIME\\\":\\\"191\\\",\\\"DB\\\":\\\"23.41\\\"},{\\\"TIME\\\":\\\"192\\\",\\\"DB\\\":\\\"23.21\\\"},{\\\"TIME\\\":\\\"193\\\",\\\"DB\\\":\\\"35.99\\\"},{\\\"TIME\\\":\\\"194\\\",\\\"DB\\\":\\\"42.83\\\"},{\\\"TIME\\\":\\\"195\\\",\\\"DB\\\":\\\"23.14\\\"},{\\\"TIME\\\":\\\"196\\\",\\\"DB\\\":\\\"23.57\\\"},{\\\"TIME\\\":\\\"197\\\",\\\"DB\\\":\\\"23.68\\\"},{\\\"TIME\\\":\\\"198\\\",\\\"DB\\\":\\\"23.17\\\"},{\\\"TIME\\\":\\\"199\\\",\\\"DB\\\":\\\"23.28\\\"},{\\\"TIME\\\":\\\"200\\\",\\\"DB\\\":\\\"23.10\\\"},{\\\"TIME\\\":\\\"201\\\",\\\"DB\\\":\\\"23.46\\\"},{\\\"TIME\\\":\\\"202\\\",\\\"DB\\\":\\\"30.35\\\"},{\\\"TIME\\\":\\\"203\\\",\\\"DB\\\":\\\"23.79\\\"},{\\\"TIME\\\":\\\"204\\\",\\\"DB\\\":\\\"23.32\\\"},{\\\"TIME\\\":\\\"205\\\",\\\"DB\\\":\\\"23.84\\\"},{\\\"TIME\\\":\\\"206\\\",\\\"DB\\\":\\\"23.39\\\"},{\\\"TIME\\\":\\\"207\\\",\\\"DB\\\":\\\"24.21\\\"},{\\\"TIME\\\":\\\"208\\\",\\\"DB\\\":\\\"23.72\\\"},{\\\"TIME\\\":\\\"209\\\",\\\"DB\\\":\\\"25.38\\\"}]\",\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/290\"}}},{\"analysisId\":444,\"analysisStartD\":\"2019-06-14T00:00:00\",\"analysisStartDt\":\"2019-06-14T11:34:19\",\"analysisEndD\":\"2019-06-14T00:00:00\",\"analysisEndDt\":\"2019-06-14T11:35:24\",\"analysisFileNm\":\"snoring-201906_14_1134~14_1135_1560479724337.mp3\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/6\",\"analysisServerUploadYn\":\"N\",\"analysisDetailsList\":[],\"claimYn\":\"N\",\"recordingData\":\"[{\\\"TIME\\\":\\\"210\\\",\\\"DB\\\":\\\"23.77\\\"},{\\\"TIME\\\":\\\"211\\\",\\\"DB\\\":\\\"23.88\\\"},{\\\"TIME\\\":\\\"212\\\",\\\"DB\\\":\\\"28.02\\\"},{\\\"TIME\\\":\\\"213\\\",\\\"DB\\\":\\\"23.35\\\"},{\\\"TIME\\\":\\\"214\\\",\\\"DB\\\":\\\"23.22\\\"},{\\\"TIME\\\":\\\"215\\\",\\\"DB\\\":\\\"23.44\\\"},{\\\"TIME\\\":\\\"216\\\",\\\"DB\\\":\\\"23.18\\\"},{\\\"TIME\\\":\\\"217\\\",\\\"DB\\\":\\\"35.82\\\"},{\\\"TIME\\\":\\\"218\\\",\\\"DB\\\":\\\"28.04\\\"},{\\\"TIME\\\":\\\"219\\\",\\\"DB\\\":\\\"29.10\\\"},{\\\"TIME\\\":\\\"220\\\",\\\"DB\\\":\\\"28.28\\\"},{\\\"TIME\\\":\\\"221\\\",\\\"DB\\\":\\\"28.85\\\"},{\\\"TIME\\\":\\\"222\\\",\\\"DB\\\":\\\"26.46\\\"},{\\\"TIME\\\":\\\"223\\\",\\\"DB\\\":\\\"27.97\\\"},{\\\"TIME\\\":\\\"224\\\",\\\"DB\\\":\\\"25.60\\\"},{\\\"TIME\\\":\\\"225\\\",\\\"DB\\\":\\\"29.79\\\"},{\\\"TIME\\\":\\\"226\\\",\\\"DB\\\":\\\"26.33\\\"},{\\\"TIME\\\":\\\"227\\\",\\\"DB\\\":\\\"27.77\\\"},{\\\"TIME\\\":\\\"228\\\",\\\"DB\\\":\\\"27.38\\\"},{\\\"TIME\\\":\\\"229\\\",\\\"DB\\\":\\\"26.22\\\"},{\\\"TIME\\\":\\\"230\\\",\\\"DB\\\":\\\"27.05\\\"},{\\\"TIME\\\":\\\"231\\\",\\\"DB\\\":\\\"26.58\\\"},{\\\"TIME\\\":\\\"232\\\",\\\"DB\\\":\\\"25.74\\\"},{\\\"TIME\\\":\\\"233\\\",\\\"DB\\\":\\\"25.80\\\"},{\\\"TIME\\\":\\\"234\\\",\\\"DB\\\":\\\"27.03\\\"},{\\\"TIME\\\":\\\"235\\\",\\\"DB\\\":\\\"26.67\\\"},{\\\"TIME\\\":\\\"236\\\",\\\"DB\\\":\\\"27.08\\\"},{\\\"TIME\\\":\\\"237\\\",\\\"DB\\\":\\\"29.23\\\"},{\\\"TIME\\\":\\\"238\\\",\\\"DB\\\":\\\"27.04\\\"},{\\\"TIME\\\":\\\"239\\\",\\\"DB\\\":\\\"56.78\\\"},{\\\"TIME\\\":\\\"240\\\",\\\"DB\\\":\\\"25.86\\\"},{\\\"TIME\\\":\\\"241\\\",\\\"DB\\\":\\\"27.25\\\"}]\",\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/290\"}}}],\"_links\":{\"self\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/290\"},\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/290\"},\"user\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/290/user\"},\"admin\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/290/admin\"},\"sleepStatusCd\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/290/sleepStatusCd\"}}}";
//                //mp3파일이 삭제됬거나  이상하게 저장되어있는 테스트 데이터
////                String testDt = "{\"userAppId\":\"7dc9e960-b0db-4c1c-81b5-2c8f2ce7ca4f\",\"recordId\":86,\"recordStartD\":\"2019-05-29\",\"recordStartDt\":\"2019-05-29T16:10:31\",\"recordEndD\":\"2019-05-29\",\"recordEndDt\":\"2019-05-29T16:10:54\",\"consultingYn\":\"N\",\"consultingReplyYn\":\"N\",\"analysisList\":[{\"analysisId\":63,\"analysisStartD\":\"2019-05-29T16:10:34\",\"analysisStartDt\":\"2019-05-29T16:10:34\",\"analysisEndD\":\"2019-05-29T16:10:54\",\"analysisEndDt\":\"2019-05-29T16:10:54\",\"analysisFileNm\":\"snoring-20190605_1708-05_1709_1559712312312312322170788.mp3\",\"analysisFileAppPath\":\"/data/user/0/kr.co.dwebss.kococo/files/rec_data/8\",\"analysisServerUploadYn\":\"N\",\"claimYn\":\"N\",\"analysisDetailsList\":[{\"analysisDetailsId\":65,\"termTypeCd\":200102,\"termStartDt\":\"2019-05-29T16:10:36\",\"termEndDt\":\"2019-05-29T16:10:40\"}],\"_links\":{\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"}}}],\"_links\":{\"self\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"record\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86\"},\"admin\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/admin\"},\"user\":{\"href\":\"http://52.79.88.47:8080/kococo/api/record/86/user\"}}}";
//                Intent intent = new Intent(getActivity(), ResultActivity.class);
//                intent.putExtra("responseData",testDt); /*송신*/
//                startActivity(intent);

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

                    recodeBtn.setText("녹음 시작");
                    recodeTxt.setVisibility(View.INVISIBLE);
                    recordTimer.setVisibility(View.INVISIBLE);
                    logo.setVisibility(View.VISIBLE);
                    recordTime = 0;
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

        //Audio_Recording();
        recordingThread = new RecordingThread(this);
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
}