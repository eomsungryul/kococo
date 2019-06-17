package kr.co.dwebss.kococo.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
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
import java.util.Timer;
import java.util.TimerTask;

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
    List<AnalysisRawData> AllAnalysisRawDataList;

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

    static double tmpMinDb = 99999;
    static double tmpMaxDb = 0;
    static double firstDecibelAvg = 0.0;
    static double secondDecibelAvg = 0.0;
    static double snoringDbChkCnt = 0;
    static int soundStartAndSnroingCnt = 0;
    static int soundStartAndSnroingOppCnt = 0;

    static boolean isBreathTerm = false;
    static boolean isOSATermTimeOccur = false;
    static int isBreathTermCnt = 0;
    static double OSAcurTermTime = 0.0;
    static int isOSATermCnt = 0;
    static int osaContinueCnt = 0;

    int l = 0;


    Retrofit retrofit;
    ApiService apiService;


    //request 데이터 모음
    JsonObject recordData;
    String userAppId;
    String recordStartDt;
    Long recordStartDtL;
    String recordEndDt;
    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    byte[] mp3buffer;

    //사용자의 밝기 저장
    int now_bright_status;

    Button recodeBtn;
    TextView recodeTxt;
    TextView recordTimer;
    ImageView logo;
    int recordTime=0;

    Handler timerMessegeHandler;

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
                            now_bright_status = android.provider.Settings.System.getInt(getContext().getContentResolver(),
                                    android.provider.Settings.System.SCREEN_BRIGHTNESS);
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
                        Timer mTimer =  new Timer();
                        mTimer.schedule(new CustomTimer(), 2000, 1000);

                        recodeFlag = true;
                        recordStartDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
                        recordStartDtL= System.currentTimeMillis();
                        start();

                    }
                }else{
                    Toast.makeText(getActivity(), "분석중입니다 잠시만 기다려주세요...", Toast.LENGTH_LONG).show();
                    recodeFlag = false;
                    stop(v);

                    //기존 밝기로 복귀
                    params.screenBrightness = (float) now_bright_status/100;
                    getActivity().getWindow().setAttributes(params);

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
        //다시 돌아왔을 경우에 텍스트를 종료에서 시작으로 바꿈
//        recodeBtn.setText("녹음 시작");
//        recodeTxt.setVisibility(View.INVISIBLE);
//        recordTimer.setVisibility(View.INVISIBLE);
//        logo.setVisibility(View.VISIBLE);
//        recordTime = 0;
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

                long recordStartingTIme = 0L;
                snoringTermList = new ArrayList<StartEnd>();
                grindingTermList = new ArrayList<StartEnd>();
                osaTermList = new ArrayList<StartEnd>();
                AllAnalysisRawDataList = new ArrayList<AnalysisRawData>();
                JsonArray ansList = new JsonArray();

                double times=0.0;
                int i = 0;
                int[] tmpArray = null;
                boolean isRecording = false;
                boolean soundStartInRecording = false;
                int snoringBufferFilledCnt = 0;
                double[] allFHAndDB = null;
                int grindingRepeatOnceAmpCnt = 0;

                double chkDBAgainInRecording = 0.0;
                int continueCntInChkTermForGrinding = 0;
                int continueCntInChkTermForGrindingChange = 0;

                int osaCnt = 0;
                int osaRecordingContinueCnt = 0;
                int osaRecordingExit = 0;
                boolean osaStart = false;
                boolean osaContinue = false;
                double osaStartTimes = 0.0;

                AnalysisRawData maxARD = null;
                double timesForMaxArd = 0.0;

                int recordingLength = 0;
                while (mShouldContinue) {
                    times = (((double) (frameBytes.length / (44100d * 16 * 1))) * 8) * i;
                    int numberOfShort = record.read(audioData, 0, audioData.length);
                    shortsRead += numberOfShort;
                    frameBytes = shortToByte(audioData,numberOfShort);
                    int amplitude = 0;
                    double decibel = 0;
                    double frequency = 0;
                    audioCalculator.setBytes(frameBytes);
                    try{
                        // 소리가 발생하면 녹음을 시작하고, 1분이상 소리가 발생하지 않으면 녹음을 하지 않는다.
                        amplitude = audioCalculator.getAmplitude();
                        decibel = audioCalculator.getDecibel();
                        frequency = audioCalculator.getFrequency();
                    }catch(ArrayIndexOutOfBoundsException e){
                        Log.v(LOG_TAG2, e.getMessage());
                        continue;
                    }

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
                    //Log.v(LOG_TAG3,(calcTime(times)+" "+hz +" "+db+" "+amp+" "+decibel+"vs"+SleepCheck.getMaxDB())+","+SleepCheck.getMinDB()+" "+SleepCheck.noiseChkSum+" "+SleepCheck.noiseChkCnt);

                    //실제로는 1초 이후 분석한다.
                    if (i < 100) {
                        continue;
                    }

                    // 소리가 발생하면 녹음을 시작하고, 1분이상 소리가 발생하지 않으면 녹음을 하지 않는다.
                    //if (SleepCheck.noiseCheckForStart(decibel) >= 30 && isRecording == false
                    if (isRecording == false
                            && Math.floor((double) (audioData.length / (44100d * 16 * 1)) * 8) != Math.floor(times) ) {
                        Log.v(LOG_TAG2,(calcTime(times)+"("+String.format("%.2f", times) + "s) 녹음 시작!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
                        //recordStartingTIme = times;
                        recordStartingTIme = System.currentTimeMillis();
                        baos = new ByteArrayOutputStream();
                        recordingLength = 0;
                        isRecording = true;
                        snoringTermList = new ArrayList<StartEnd>();
                        grindingTermList = new ArrayList<StartEnd>();
                        osaTermList = new ArrayList<StartEnd>();
                        AllAnalysisRawDataList = new ArrayList<AnalysisRawData>();
                        isBreathTerm = false;
                        isOSATermTimeOccur = false;
//                    } else if (isRecording == true && (SleepCheck.noiseCheck(decibel)==0 || recodeFlag==false) ) {
                    //} else if (isRecording == true && SleepCheck.noiseCheck(decibel) <= 100) {
                    } else if (isRecording == true && SleepCheck.noiseCheck(decibel) == 0) {
                        Log.v(LOG_TAG2,(calcTime(times)+"("+String.format("%.2f", times) + "s) 녹음 종료!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
                        AllAnalysisRawDataList.add(maxARD);
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
                        Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (recordingLength / (44100d * 16 * 1))) * 8));
                        Log.v(LOG_TAG2,("tmpMinDb: "+tmpMinDb));
                        Log.v(LOG_TAG2,("tmpMaxDb: "+tmpMaxDb));

                        JsonObject ans = new JsonObject();
                        StartEnd tmpSE = new StartEnd(); //전체 분석 데이터를 변환하기 위해 임시로 vo를 생성
                        tmpSE.AnalysisRawDataList = AllAnalysisRawDataList;

                        try {
                            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                            String andRList = gson.toJson(tmpSE.printAnalysisRawDataList());
                            //Log.v(LOG_TAG2,andRList);
                            ans.addProperty("recordingData", andRList);

                        }catch(NullPointerException e){
                            e.getMessage();
                        }
                        //ans.setAnalysisStartDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(recordStartingTIme), ZoneId.systemDefault()));
                        //ans.setAnalysisEndDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
                        ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                        ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                        ans.addProperty("analysisFileAppPath",fileInfo[0]);
                        ans.addProperty("analysisFileNm",fileInfo[1]);
                        JsonArray ansDList = new JsonArray();
                        JsonObject ansd = new JsonObject();
                        for ( int s = 0 ; s < snoringTermList.size() ; s ++) {
                            if(s>0) {
                                StartEnd se = snoringTermList.get(s);
                                StartEnd bse = snoringTermList.get(s-1);
                                double curStartTime = se.start;
                                double beforeEndTime = bse.end;
                                if(curStartTime - beforeEndTime <= 1 && se.end!=0) {
                                    bse.end = se.end;
                                    bse.negitiveCnt += se.negitiveCnt;
                                    bse.positiveCnt += se.positiveCnt;
                                    bse.first = (bse.first+se.first);
                                    bse.second = (bse.second+se.second);
                                    bse.chk += se.chk;
                                    bse.AnalysisRawDataList.addAll(se.AnalysisRawDataList);
                                    snoringTermList.remove(se);
                                    s--;
                                }
                            }
                        }
                        for(StartEnd se : snoringTermList) {
                            if(se.end!=0 && se.end>se.start){
                                Log.v(LOG_TAG2,se.getTerm());
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200101);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.start*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.end*1000))));
                                try {
                                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                    String andRList = gson.toJson(se.printAnalysisRawDataList());
                                    Log.v(LOG_TAG2,andRList);
                                    ansd.addProperty("analysisData", andRList);

                                }catch(NullPointerException e){
                                    e.getMessage();
                                }
                                ansDList.add(ansd);
                            }else{
                                snoringTermList.remove(snoringTermList.size()-1);
                            }
                        }
                        for ( int s = 0 ; s < grindingTermList.size() ; s ++) {
                            if(s>0) {
                                StartEnd se = grindingTermList.get(s);
                                StartEnd bse = grindingTermList.get(s-1);
                                double curStartTime = se.start;
                                double beforeEndTime = bse.end;
                                if(curStartTime - beforeEndTime <= 1 && se.end!=0) {
                                    bse.end = se.end;
                                    bse.negitiveCnt += se.negitiveCnt;
                                    bse.positiveCnt += se.positiveCnt;
                                    bse.first = (bse.first+se.first);
                                    bse.second = (bse.second+se.second);
                                    bse.chk += se.chk;
                                    bse.AnalysisRawDataList.addAll(se.AnalysisRawDataList);
                                    grindingTermList.remove(se);
                                    s--;
                                }
                            }
                        }
                        for(StartEnd se : grindingTermList) {
                            if(se.end!=0 && se.end>se.start){
                                Log.v(LOG_TAG2,se.getTerm());
                                    ansd = new JsonObject();
                                    ansd.addProperty("termTypeCd",200102);
                                    ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.start*1000))));
                                    ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.end*1000))));
                                    try {
                                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                        String andRList = gson.toJson(se.printAnalysisRawDataList());
                                        Log.v(LOG_TAG2,andRList);
                                        ansd.addProperty("analysisData", andRList);

                                    }catch(NullPointerException e){
                                        e.getMessage();
                                    }
                                    ansDList.add(ansd);
                            }else{
                                grindingTermList.remove(grindingTermList.size()-1);
                            }
                        }
                        for(StartEnd se : osaTermList) {
                            if(se.end!=0 && se.end>se.start){
                                Log.v(LOG_TAG2,se.getTerm());
                                    ansd = new JsonObject();
                                    ansd.addProperty("termTypeCd",200103);
                                    ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.start*1000))));
                                    ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.end*1000))));
                                    try {
                                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                        String andRList = gson.toJson(se.printAnalysisRawDataList());
                                        Log.v(LOG_TAG2,andRList);
                                        ansd.addProperty("analysisData", andRList);

                                    }catch(NullPointerException e){
                                        e.getMessage();
                                    }
                                    ansDList.add(ansd);
                            }else{
                                osaTermList.remove(osaTermList.size()-1);
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

                    if(isRecording==false) {
                        continue;
                    }
                    //baos.write(frameBytes);
                    if(audioData != null ) {
                        recordingLength += (audioData.length*2);
                    }
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
                    if(allFHAndDB!=null) {
                        //코골이는 임계치를 보정해서 코골이의 음파 여부를 판단한다.
                        int maxDBL = allFHAndDB.length;
                        maxDBL = maxDBL > 41 ? 41 : maxDBL;
                        for(int m = 0 ; m < maxDBL ; m++){
                            if(allFHAndDB[m] > tmpMaxDb){
                                tmpMaxDb = allFHAndDB[m];
                                if(tmpMaxDb<0){
                                    tmpMaxDb = Math.abs(tmpMaxDb);
                                }
                            }
                            if(allFHAndDB[m] < tmpMinDb){
                                tmpMinDb = allFHAndDB[m];
                            }
                        }
                        if(tmpMaxDb>40) {
                            Log.v(LOG_TAG3, (calcTime(times) + " " + hz + " " + db + " " + amp + " " + decibel + ", 100db: " + tmpMaxDb + "db, max: " + SleepCheck.getMaxDB()) + ", min: " + SleepCheck.getMinDB() + " " + SleepCheck.noiseChkSum + " " + SleepCheck.noiseChkCnt);
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
                        if(decibel > chkSnoringDb && tmpMaxDb>40) {
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
                                //음파시작시간을 보관하기 위해 기록vo를 생성
                                StartEnd st = new StartEnd();
                                st.start = times;
                                st.AnalysisRawDataList = new ArrayList<AnalysisRawData>();
                                //st.AnalysisRawDataList.add(maxARD);
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
                                if(snoringTermList == null || snoringTermList.size()==0){
                                    soundStartInRecording = false;
                                    continue;
                                }
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
                                        if(snoringTermList.get(snoringTermList.size()-1).AnalysisRawDataList!=null &&
                                                snoringTermList.get(snoringTermList.size()-1).AnalysisRawDataList.size() >0){
                                            double tmpTimes1 = snoringTermList.get(snoringTermList.size()-1).AnalysisRawDataList.get(
                                                    snoringTermList.get(snoringTermList.size()-1).AnalysisRawDataList.size()-1
                                            ).getTimes();
                                            tmpTimes1 = Math.floor(tmpTimes1);
                                            double currentTimes1 = Math.floor(times);
                                            if(currentTimes1-1 == tmpTimes1){
                                                snoringTermList.get(snoringTermList.size()-1).AnalysisRawDataList.add(maxARD);
                                            }else if(currentTimes1-2 == tmpTimes1){
                                                AnalysisRawData tmpD = new AnalysisRawData(currentTimes1-1, maxARD.getAmplitude(), tmpMaxDb, maxARD.getFrequency());
                                                snoringTermList.get(snoringTermList.size()-1).AnalysisRawDataList.add(tmpD);
                                            }
                                        }
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
                                            if(st.AnalysisRawDataList!=null &&
                                                    st.AnalysisRawDataList.size() >0){
                                                double tmpTimes1 = st.AnalysisRawDataList.get(st.AnalysisRawDataList.size()-1).getTimes();
                                                tmpTimes1 = Math.floor(tmpTimes1);
                                                double currentTimes1 = Math.floor(times);
                                                if(currentTimes1-1 == tmpTimes1){
                                                    st.AnalysisRawDataList.add(maxARD);
                                                }else if(currentTimes1-2 == tmpTimes1){
                                                    AnalysisRawData tmpD = new AnalysisRawData(currentTimes1-1, maxARD.getAmplitude(), tmpMaxDb, maxARD.getFrequency());
                                                    st.AnalysisRawDataList.add(tmpD);
                                                }
                                            }
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

                    //if (decibel > SleepCheck.getMinDB()*0.45) {
                    if(decibel > chkGrindingDb) {
                        //소리가 발생했고, 분석 시작 변수 값이 true 인 경우 종료한다.
                        if(isOSATermTimeOccur) {
                            //0.1초 동안 소리가 70% 이상 발생한 경우 소리가 발생한 것으로 본다.

                            if(isOSATermCnt+isBreathTermCnt>90 && isOSATermCnt > 20 && isBreathTermCnt > 70) {
                                //오차범위를 둔다. 0.5초 동안 연속으로 소리가 발생해야 한다.
                                if(osaContinueCnt > 4) {
                                    isOSATermTimeOccur = false;
                                    isBreathTermCnt = 0;
                                    isBreathTerm = true;
                                    osaTermList.get(osaTermList.size()-1).end=times;
                                    osaTermList.get(osaTermList.size()-1).chk=0;
                                    osaContinueCnt = 0;
                                }else {
                                    if(osaContinueCnt!=0) {
                                        osaContinueCnt ++;
                                    }else {
                                        osaContinueCnt = 1;
                                    }
                                }
                            }
                        }else {

                        }
                        isBreathTermCnt++;
                    }else {
                        //무호흡을 측정하기 위한 분석 시작 변수 초기화
                        //코골이가 발생하고 5초가 안지났어야 함.
                        if(snoringTermList.size() > 0
                                && snoringTermList.get(snoringTermList.size()-1).end != 0
                                && times - snoringTermList.get(snoringTermList.size()-1).end > 0
                                && times - snoringTermList.get(snoringTermList.size()-1).end < 5
                                && !isOSATermTimeOccur) {
                            //0.1초 동안 묵음이 70% 이상 발생한 경우 소리가 발생한 것으로 본다.
                            if(isOSATermCnt+isBreathTermCnt>90 && isBreathTermCnt > 70 && isBreathTermCnt > 20) {
                                osaContinueCnt = 0;
                                OSAcurTermTime = times;
                                isOSATermTimeOccur = true;
                                isBreathTerm = false;
                                osaTermList.add(new StartEnd());
                                osaTermList.get(osaTermList.size()-1).start=times;
                                osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList = new ArrayList<AnalysisRawData>();
                            }
                        }
                        isOSATermCnt++;
                    }
                    //무호흡 발생후 3분동안 종료되지 않는다면 취소
                    if(osaTermList.size()>0 && osaTermList.get(osaTermList.size()-1).end==0 && times-osaTermList.get(osaTermList.size()-1).start > 180) {
                        isOSATermTimeOccur = false;
                        isOSATermCnt = 0;
                        isBreathTerm = false;
                        isBreathTermCnt = 0;
                        OSAcurTermTime = 0.0;
                        osaTermList.remove(osaTermList.size()-1);
                    }

                    //무호흡 종료 후 녹음된 시간이 너무 짧으면 삭제한다.
                    if(osaTermList.size()>0 && osaTermList.get(osaTermList.size()-1).end!=0 && times - osaTermList.get(osaTermList.size()-1).end < 5) {
                        if(osaTermList.get(osaTermList.size()-1).end - osaTermList.get(osaTermList.size()-1).start < 5 ){
                            osaTermList.remove(osaTermList.size()-1);
                        }
                    }

                    //무호흡 종료 후 5초 이내에 코골이가 발생하지 않으면 취소
                    //무호흡 종료 후 5초 동안 코골이 발생여부를 체크한다.
                    if(osaTermList.size()>0 && osaTermList.get(osaTermList.size()-1).end!=0 && times - osaTermList.get(osaTermList.size()-1).end < 5) {
                        if(snoringTermList.size()>0 && snoringTermList.get(snoringTermList.size()-1).start - osaTermList.get(osaTermList.size()-1).end > 0 && snoringTermList.get(snoringTermList.size()-1).start - osaTermList.get(osaTermList.size()-1).end < 5){
                            //코골이가 녹음 중이게 되었을 때, 체크 플래그를 업데이트
                            if(snoringTermList.get(snoringTermList.size() - 1).end==0){
                                osaTermList.get(osaTermList.size()-1).chk = 1;
                            }
                        }
                    }
                    //무호흡 종료 후 5초가 넘은 경우 플래그를 체크해서 코골이를 삭제한다.
                    if(osaTermList.size()>0 && osaTermList.get(osaTermList.size()-1).end!=0 && times - osaTermList.get(osaTermList.size()-1).end > 5) {
                        if(osaTermList.get(osaTermList.size()-1).chk==0) {
                            osaTermList.remove(osaTermList.size()-1);
                        }
                    }


                    if(maxARD!=null){
                        if(decibel > maxARD.getDecibel()){
                            maxARD = new AnalysisRawData(times, amplitude, tmpMaxDb, frequency);
                        }
                    }else{
                        maxARD = new AnalysisRawData(times, amplitude, tmpMaxDb, frequency);
                        timesForMaxArd = Math.floor(times);
                    }
                    if(Math.floor(times) > timesForMaxArd){
                        //코골이 기록용 vo 생성
                        if(maxARD.getDecibel()==0){
                            maxARD.setDecibel(tmpMaxDb);
                        }
                        //System.out.println(calcTime(times)+" "+snoringTermList.size()+" "+SleepCheck.isOSATerm+" "+SleepCheck.isBreathTerm+" "+SleepCheck.isOSAAnsStart);
                        if(snoringTermList.size()>0 && isRecording == true){
                            if(snoringTermList.get(snoringTermList.size() - 1).end!=0){
                                if(snoringTermList.get(snoringTermList.size() - 1).end > times){
                                    snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                                }
                            }else {
                                snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                            }
                        }
                        if(osaTermList.size()>0 && isRecording == true && isOSATermTimeOccur){
                            if(osaTermList.get(osaTermList.size() - 1).end!=0){
                                if(osaTermList.get(osaTermList.size() - 1).end > times){
                                    osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                                }
                            }else {
                                osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                            }
                        }
                        if(isRecording == true){
                            //AllAnalysisRawDataList.add(maxARD);
                            int tmpTime = (int) Math.floor(times);
                            //1초 혹은 1분 단위로 기록
                            if(tmpTime<=61){
                                AllAnalysisRawDataList.add(maxARD);
                            }
                            if(tmpTime>60 && tmpTime%60 ==2) {
                                Log.v(LOG_TAG2,(calcTime(times)+" "+calcTime(maxARD.getTimes())+" "+maxARD.getDecibel()));
                                AllAnalysisRawDataList.add(maxARD);
                            }
                            if(tmpTime>60 && tmpTime%60 > 2) {

                                double tmpCM = (times+(int) (recordStartDtL / 1000) % 60);
                                double tmpBeforeCM = (AllAnalysisRawDataList.get(AllAnalysisRawDataList.size()-1).getTimes()+(int) (recordStartDtL / 1000) % 60);
                                int tmpM = calcMinute(tmpCM);
                                int tmpBeforeM = calcMinute(tmpBeforeCM);
                                //Log.v(LOG_TAG2,(calcTime(times)+" "+tmpCM+" "+tmpBeforeCM+" "+tmpM+" "+tmpBeforeM));
                                if(tmpM%60==2){
                                    AllAnalysisRawDataList.add(maxARD);
                                }
                            }
                            //1분 당시의 데이터가 없는 경우
                            /*
                            if(AllAnalysisRawDataList.size()>0 && tmpTime%60==1 && AllAnalysisRawDataList.size() > 0){
                                AnalysisRawData tmpAwd = AllAnalysisRawDataList.get(AllAnalysisRawDataList.size()-1);
                                int tmpTimeBefore = (int) Math.floor(tmpAwd.getTimes());
                                if(tmpTime - tmpTimeBefore < 70){
                                    AllAnalysisRawDataList.add(maxARD);
                                }
                            }
                            */
                        }
                        maxARD = new AnalysisRawData(times, amplitude, tmpMaxDb, frequency);
                        timesForMaxArd = Math.floor(times);

                        tmpMaxDb = 0;
                        tmpMinDb = 99999;
                    }
                }
                if (isRecording == true && recodeFlag==false) {
                    Log.v(LOG_TAG2,(calcTime(times)+"("+String.format("%.2f", times) + "s) 녹음 종료 버튼을 눌러서 현재 진행되던 녹음을 종료!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
                    AllAnalysisRawDataList.add(maxARD);
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
                    Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (recordingLength/ (44100d * 16 * 1))) * 8));
                    Log.v(LOG_TAG2,("tmpMinDb: "+tmpMinDb));
                    Log.v(LOG_TAG2,("tmpMaxDb: "+tmpMaxDb));

                    JsonObject ans = new JsonObject();
                    StartEnd tmpSE = new StartEnd(); //전체 분석 데이터를 변환하기 위해 임시로 vo를 생성
                    tmpSE.AnalysisRawDataList = AllAnalysisRawDataList;

                    try {
                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        String andRList = gson.toJson(tmpSE.printAnalysisRawDataList());
                        //Log.v(LOG_TAG2,andRList);
                        ans.addProperty("recordingData", andRList);

                    }catch(NullPointerException e){
                        e.getMessage();
                    }
                    ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                    ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                    ans.addProperty("analysisFileAppPath",fileInfo[0]);
                    ans.addProperty("analysisFileNm",fileInfo[1]);
                    JsonArray ansDList = new JsonArray();
                    JsonObject ansd = new JsonObject();
                    for ( int s = 0 ; s < snoringTermList.size() ; s ++) {
                        if(s>0) {
                            StartEnd se = snoringTermList.get(s);
                            StartEnd bse = snoringTermList.get(s-1);
                            double curStartTime = se.start;
                            double beforeEndTime = bse.end;
                            if(curStartTime - beforeEndTime <= 1 && se.end!=0) {
                                bse.end = se.end;
                                bse.negitiveCnt += se.negitiveCnt;
                                bse.positiveCnt += se.positiveCnt;
                                bse.first = (bse.first+se.first);
                                bse.second = (bse.second+se.second);
                                bse.chk += se.chk;
                                bse.AnalysisRawDataList.addAll(se.AnalysisRawDataList);
                                snoringTermList.remove(se);
                                s--;
                            }
                        }
                    }
                    for(StartEnd se : snoringTermList) {
                        if(se.end!=0 && se.end>se.start){
                            Log.v(LOG_TAG2,se.getTerm());
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200101);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.end*1000))));
                            try {
                                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                String andRList = gson.toJson(se.printAnalysisRawDataList());
                                Log.v(LOG_TAG2,andRList);
                                ansd.addProperty("analysisData", andRList);

                            }catch(NullPointerException e){
                                e.getMessage();
                            }
                            ansDList.add(ansd);
                        }else{
                            snoringTermList.remove(se);
                        }
                    }
                    for ( int s = 0 ; s < grindingTermList.size() ; s ++) {
                        if(s>0) {
                            StartEnd se = grindingTermList.get(s);
                            StartEnd bse = grindingTermList.get(s-1);
                            double curStartTime = se.start;
                            double beforeEndTime = bse.end;
                            if(curStartTime - beforeEndTime <= 1 && se.end!=0) {
                                bse.end = se.end;
                                bse.negitiveCnt += se.negitiveCnt;
                                bse.positiveCnt += se.positiveCnt;
                                bse.first = (bse.first+se.first);
                                bse.second = (bse.second+se.second);
                                bse.chk += se.chk;
                                bse.AnalysisRawDataList.addAll(se.AnalysisRawDataList);
                                grindingTermList.remove(se);
                                s--;
                            }
                        }
                    }
                    for(StartEnd se : grindingTermList) {
                        if(se.end!=0 && se.end>se.start){
                            Log.v(LOG_TAG2,se.getTerm());
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200102);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.end*1000))));
                            try {
                                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                String andRList = gson.toJson(se.printAnalysisRawDataList());
                                Log.v(LOG_TAG2,andRList);
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
                        if(se.end!=0 && se.end>se.start){
                            Log.v(LOG_TAG2,se.getTerm());
                            ansd = new JsonObject();
                            ansd.addProperty("termTypeCd",200103);
                            ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.start*1000))));
                            ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartDtL+se.end*1000))));
                            try {
                                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                String andRList = gson.toJson(se.printAnalysisRawDataList());
                                Log.v(LOG_TAG2,andRList);
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
//                recordData.addProperty("recordEndDt",recordEndDt);
                recordData.addProperty("recordEndDt",dayTimeDefalt.format(new Date(System.currentTimeMillis())));
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
    private int calcMinute(double times) {
        int seconds;
        int minutes ;
        int hours;
        seconds =  (int)times;
        hours = seconds / 3600;
        minutes = (seconds%3600)/60;
        double seconds_output = (times% 3600)%60;
        seconds_output = Math.floor(seconds_output*1000)/1000;
        return minutes;
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

    public String getTermForRequest(int termCd, long recordStartDtL) {
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return "termTypeCd: " + termCd + ", termStartDt: "
                + dayTime.format(new Date((long) (recordStartDtL + this.start * 1000))) + ",termEndDt: "
                + dayTime.format(new Date((long) (recordStartDtL + this.end * 1000)));
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

                JsonObject data = new JsonObject();
                data.addProperty("TIME", String.format("%.0f", d.getTimes()));
                data.addProperty("DB", String.format("%.2f", d.getDecibel()));
                //data.addProperty("HZ", d.getFrequency());
                //data.addProperty("AMP", d.getAmplitude());

//                rtn+=d.toString()+"\r\n";
                rtn.add(data);
            }
        }
        return rtn;
    }
}


