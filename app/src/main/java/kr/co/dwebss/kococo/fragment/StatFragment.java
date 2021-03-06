package kr.co.dwebss.kococo.fragment;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import kr.co.dwebss.kococo.R;
import kr.co.dwebss.kococo.activity.StaticVariables;
import kr.co.dwebss.kococo.adapter.StatAdapter;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.RowData;
import kr.co.dwebss.kococo.model.Section;
import kr.co.dwebss.kococo.model.StatData;
import kr.co.dwebss.kococo.util.DateFormatter;
import kr.co.dwebss.kococo.util.FindAppIdUtil;
import kr.co.dwebss.kococo.util.StatFormatter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static kr.co.dwebss.kococo.activity.StaticVariables.isCorrectPatch;
import static kr.co.dwebss.kococo.activity.StaticVariables.patchCnt;

public class StatFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
    private String LOG_TAG = "StatFragment";

    private Section mAccountsSection;
    private RecyclerView mAccountsRV;
    private PieChart chart;
    List<RowData> stats;
    Resources res;

    Retrofit retrofit;
    ApiService apiService;

    FindAppIdUtil fau = new FindAppIdUtil();
    Spinner statSp;

    Map<String,Integer> statTermMap;

    public StatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stat, container, false);

        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);

        //res/string.xml을 사용하기 위한
        res = getResources();

        ArrayList<String> statTermList = new ArrayList();
        statTermMap = new LinkedHashMap<>();
        statTermMap.put("7일간",100301);
        statTermMap.put("30일간",100302);
        statTermMap.put("전체기간",100303);

        Set key = statTermMap.keySet();
        for (Iterator iterator = key.iterator(); iterator.hasNext();) {
            String keyName = (String) iterator.next();
//            int valueName = (Integer) statTermMap.get(keyName);
            statTermList.add(keyName);
        }
        statSp = (Spinner)v.findViewById(R.id.stat_term);
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_spinner_dropdown_item,statTermList);
        statSp.setAdapter(ageAdapter);
        statSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                int statTermCode =statTermMap.get(statSp.getSelectedItem());
                getUserStat(v, statTermCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return v;
    }

    private void getUserStat(View v,Integer statTermCode){

        System.out.println(" =============statTermCode===========: "+statTermCode);
        apiService.getStats(fau.getAppid(getContext()),statTermCode.toString()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                System.out.println(" =============getStats===========response: "+response.body());
                if(response.body()!=null){

                    JsonObject result = response.body();
                    stats = new ArrayList<RowData>();

                    long recordedTimes=result.get("recordedTimes").getAsLong();
                    long snoringTimes=result.get("snoringTimes").getAsLong();
                    long osaTimes=result.get("osaTimes").getAsLong();
                    long grindingTimes=result.get("grindingTimes").getAsLong();

                    String osaCnt=result.get("osaCnt").getAsString();
                    String grindingCnt=result.get("grindingCnt").getAsString();


                    int sleepScore=result.get("sleepScore").getAsInt();
                    DateFormatter df = new DateFormatter();

                    long totalTimes=recordedTimes+snoringTimes+osaTimes+grindingTimes;

                    stats.add(new StatData(res.getString(R.string.goodSleepRow),df.longToStringFormat(recordedTimes), recordedTimes, "1","", 0xFF1EB980));
                    stats.add(new StatData(res.getString(R.string.snoreRow), df.longToStringFormat(snoringTimes),snoringTimes, "1","", 0xFFFF6859));
                    stats.add(new StatData(res.getString(R.string.grindRow),df.longToStringFormat(grindingTimes),grindingTimes, "1",grindingCnt, 0xFFFFCF44));
                    stats.add(new StatData(res.getString(R.string.apneaRow),df.longToStringFormat(osaTimes),osaTimes, "1",osaCnt, 0xFFB15DFF));
                    mAccountsSection = new Section(stats, "수면 점수", false);

                    mAccountsRV = v.findViewById(R.id.statView);
                    //context 를 사용하려면 getActivity를 하면됨
                    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
                    mAccountsRV.setLayoutManager(layoutManager);
//        RecyclerView.Adapter accountsAdapter = new SectionAdapter(mAccountsSection);
                    RecyclerView.Adapter accountsAdapter = new StatAdapter(mAccountsSection);
                    mAccountsRV.setAdapter(accountsAdapter);

                    //파이 차트 시작!
                    chart = v.findViewById(R.id.chart1);
                    //값을 넣어버리면 퍼센트로바꿈
                    chart.setUsePercentValues(true);
                    //설명충 등판 (하단에 Description Label이라는게 생김 ㅡㅡ)
                    chart.getDescription().setEnabled(false);
                    chart.setExtraOffsets(5, 10, 5, 5);

                    chart.setDragDecelerationFrictionCoef(0.95f);

                    //폰트체 설정하는부분
//        chart.setCenterTextTypeface(tfLight);
                    chart.setCenterText(generateCenterSpannableText(sleepScore));
                    chart.setCenterTextColor(Color.WHITE);

                    //안에 구멍을 넣을지 말지.. 없으면 피자조각처럼 됨
                    chart.setDrawHoleEnabled(true);
                    //파이 차트 안의 색깔
                    chart.setHoleColor(Color.TRANSPARENT);
                    //파이 안쪽 투명 테두리 설정 (총수면시간)
                    chart.setTransparentCircleColor(Color.WHITE);
                    chart.setTransparentCircleAlpha(110);


                    chart.setHoleRadius(58f);
                    chart.setTransparentCircleRadius(61f);
                    //있어야 중간에 텍스트 삽입됨
                    chart.setDrawCenterText(true);

                    chart.setRotationAngle(0);
                    // enable rotation of the chart by touch
                    chart.setRotationEnabled(false);
                    chart.setHighlightPerTapEnabled(true);

                    // chart.setUnit(" €");
                    // chart.setDrawUnitsInChart(true);

                    // add a selection listener
//        chart.setOnChartValueSelectedListener(this);

                    chart.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {}
                    });

                    chart.animateY(1400, Easing.EaseInOutQuad);
                    // chart.spin(2000, 0, 360);

//        Legend l = chart.getLegend();
//        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
//        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
//        l.setOrientation(Legend.LegendOrientation.VERTICAL);
//        l.setDrawInside(false);
//        l.setXEntrySpace(7f);
//        l.setYEntrySpace(0f);
//        l.setYOffset(0f);
                    chart.getLegend().setEnabled(false);
                    // entry label styling
                    chart.setEntryLabelColor(Color.WHITE);
//        chart.setEntryLabelTypeface(tfRegular);
                    chart.setEntryLabelTextSize(12f);

                    //그래프가 작아짐
//                chart.setMinOffset(100f);

                    //값 넣기
                    ArrayList<PieEntry> entries = new ArrayList<>();

                    for (int i = 0; i < stats.size() ; i++) {
                        float val = stats.get(i).getRowAmount();
                        float valval = (val/totalTimes)*100;
//                    System.out.println("=======valval======="+valval);
                        if(valval>1.8f){
                            entries.add(new PieEntry(
                                    val,
                                    stats.get(i).getRowName()));
//                        entries.get(i).setLabel("");
//                        entries.get(i).setData("");
                        }
                    }

                    //라벨이 있을시 목차(legend)의 라벨이 입력됨
                    PieDataSet dataSet = new PieDataSet(entries, "");
                    dataSet.setDrawIcons(false);
                    //파이 사이의 공간 설정
                    dataSet.setSliceSpace(0f);

                    dataSet.setIconsOffset(new MPPointF(0, 40));
                    dataSet.setSelectionShift(5f);


                    //컬러 설정
                    //총 수면시간 (초록) #1EB980 RGB 30, 185, 128
                    //코골이 (오렌지) #FF6859 RGB 255, 104, 89
                    //이갈이 (옐로우) #FFCF44 RGB 255, 207, 68
                    //무호흡 (퍼플) #B15DFF RGB	177, 93, 255
                    // add a lot of colors
                    ArrayList<Integer> colors = new ArrayList<>();
                    colors.add(Color.rgb(30, 185, 128));
                    colors.add(Color.rgb(255, 104, 89));
                    colors.add(Color.rgb(255, 207, 68));
                    colors.add(Color.rgb(177, 93, 255));
//        for (int c : ColorTemplate.PASTEL_COLORS)
//            colors.add(c);

                    colors.add(ColorTemplate.getHoloBlue());

                    dataSet.setColors(colors);

                    PieData data = new PieData(dataSet);
//        data.setValueFormatter(new PercentFormatter(chart));
                    data.setValueTextSize(11f);
                    data.setValueTextColor(Color.WHITE);
                    dataSet.setValueFormatter(new StatFormatter());


                    dataSet.setValueLinePart1OffsetPercentage(1.f);
                    //Y축이 길어진다.
//                dataSet.setValueLinePart1Length(0.4f);
                    dataSet.setValueLinePart1Length(0.4f);
                    //X축이 길어진다.
                    dataSet.setValueLinePart2Length(0.4f);
                    dataSet.setValueLineColor(Color.TRANSPARENT);

                    //데이터 이름이 파이차트에서 빠지고 밖으로 값을 나타내게 변경됨
                    dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                    //값들이 파이차트에서 빠지고 밖으로 값을 나타내게 변경됨
                    dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

//        data.setValueTypeface(tfLight);
                    chart.setData(data);
                    // undo all highlights
                    chart.highlightValues(null);
                    chart.invalidate();
                    //파이 차트 끝!
                }else{
                    Toast.makeText(getContext(),"통계 데이터 오류",Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                System.out.println(" ========================Throwable: "+t.getMessage());
            }
        });

    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    //안드로이드에서 TextView에 setText시 text에 부분적으로 밑줄을 긋거나 색상을 바꾸거나 이미지를 중간에 삽입하거나 등이 필요 시
    private SpannableString generateCenterSpannableText(int sleepScore) {
//        SpannableString s = new SpannableString("평균\n80점");
//        //사이즈 크기조절 RelativeSizeSpan
//        s.setSpan(new RelativeSizeSpan(1.7f), 0, s.length()-3, 0);
//        s.setSpan(new RelativeSizeSpan(3.7f), s.length()-3, s.length(), 0);
        SpannableString s = new SpannableString(sleepScore+"점");
        s.setSpan(new RelativeSizeSpan(3.7f), 0, s.length(), 0);
//        s.setSpan(new ForegroundColorSpan(Color.GRAY), 14, s.length() - 15, 0);
//        s.setSpan(new RelativeSizeSpan(.8f), 14, s.length() - 15, 0);
        return s;
    }

    //컬러 설정
    //총 수면시간 (초록) #1EB980 RGB 30, 185, 128
    //코골이 (오렌지) #FF6859 RGB 255, 104, 89
    //이갈이 (옐로우) #FFCF44 RGB 255, 207, 68
    //무호흡 (퍼플) #B15DFF RGB	177, 93, 255
    private void initializeData() {

//        stats = new ArrayList<RowData>();
//        stats.add(new StatData(res.getString(R.string.goodSleepRow),"8시간", 480f, "1", 0xFF1EB980));
//        stats.add(new StatData(res.getString(R.string.snoreRow), "1시간 20분",80f, "1", 0xFFFF6859));
//        stats.add(new StatData(res.getString(R.string.grindRow),"20분", 20f, "1", 0xFFFFCF44));
//        stats.add(new StatData(res.getString(R.string.apneaRow),"20분", 20f, "1", 0xFFB15DFF));
//        mAccountsSection = new Section(stats, "수면 점수", false);

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}


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
//
//    public void initStatCode(){
//
//        apiService.getApiCode().enqueue(new Callback<JsonObject>() {
//            @Override
//            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
//                JsonArray codeList = response.body().getAsJsonObject("_embedded").getAsJsonArray("code");
//                Gson gson = new Gson();
//                Map<String, Object> map = new HashMap<String, Object>();
//
//                for (JsonElement je : codeList) {
//                    map = (Map<String, Object>) gson.fromJson(je.getAsJsonObject(), map.getClass());
//                    if (((Double) map.get("code")).intValue() == 999999) {
//                        String value = String.valueOf(map.get("codeValue"));
//                        if(!StaticVariables.version.equals(value)){
//                            isChangedVersion = true;
//                        }
//                        StaticVariables.version = value;
//                        Log.e(TAG_VERSION, "Version: " + StaticVariables.version);
//                    }else
//                    if (((Double) map.get("code")).intValue() == 999998) {
//                        try {
//                            int value = Integer.parseInt(((String) map.get("codeValue")));
//                            if(StaticVariables.size!=value){
//                                isChangedVersion = true;
//                            }
//                            StaticVariables.size = value;
//                        }catch(ClassCastException e){
//                            e.printStackTrace();
//                            StaticVariables.size = 0;
//                        }
//                        Log.e(TAG_VERSION, "Size: " + StaticVariables.size);
//                    }else
//                    if (((Double) map.get("code")).intValue() == 900001) {
//                        String value = String.valueOf(map.get("codeValue"));
//                        int val = Integer.parseInt(value);
//                        StaticVariables.forNoiseCheckForStartVal  = val;
//                        Log.e(TAG_VERSION, "forNoiseCheckForStartVal: " + StaticVariables.forNoiseCheckForStartVal );
//                    }else
//                    if (((Double) map.get("code")).intValue() == 900002) {
//                        String value = String.valueOf(map.get("codeValue"));
//                        int val = Integer.parseInt(value);
//                        StaticVariables.forNoiseCheckVal = val;
//                        Log.e(TAG_VERSION, "forNoiseCheckVal: " + StaticVariables.forNoiseCheckVal);
//                    }
//                }
//                if(isChangedVersion){
//                    patchCnt = 0;
//                }
//                System.out.println(" ============getApiCode2============result: " + codeList);
//
//                FirebaseStorage storage = FirebaseStorage.getInstance("gs://kococo-2996f.appspot.com/");
//                storage.setMaxDownloadRetryTimeMillis(60000);  // 1분 지나면 실패
//                StorageReference storageRef = storage.getReference();
//                StorageReference pathReference = storageRef.child("libs/SoundAnalysis_" + StaticVariables.version + ".jar");
//                File path = new File(activity.getFilesDir()+"/libs");
//                Log.e(TAG_VERSION,"jar가 다운받아질 경로: "+path.getAbsolutePath());
//                File[] files = path.listFiles();
//                String filename = "";
//                int tmpSizeForChk = 0;
//
//                final File tmpDir = activity.getDir("dex", 0);
//                File[] files2 = tmpDir.listFiles();
//                for (int i = 0; i < files2.length; i++) {
//                    Log.e(TAG_VERSION,files2[i].getAbsolutePath()+" asdasd");
//                }
//
//                if (StaticVariables.version == null || StaticVariables.version.equals("")) {
//
//                } else {
//                    if (files != null) {
//                        for (int i = 0; i < files.length; i++) {
//                            filename = files[i].getName();
//                            Log.e(TAG_VERSION, new File(path + "/" + filename).getAbsolutePath());
//                            if (filename.indexOf("jar") > -1) {
//                                Log.e(TAG_VERSION, "jar checking: " + filename);
//                                if (filename.indexOf(StaticVariables.version) > -1) {
//                                    Log.e(TAG_VERSION, "name checking: " + filename);
//                                    tmpSizeForChk = (int) new File(path + "/" + filename).length();
//                                    Log.e(TAG_VERSION, "size checking: " + tmpSizeForChk + "vs" + StaticVariables.size);
//                                    if (tmpSizeForChk == StaticVariables.size) {
//                                        isCorrectPatch = true;
//                                        break;
//                                    }
//                                }
//                            }
//
//                            if (!isCorrectPatch) {
//                                if (!filename.equals("")) {
//                                    File tmpFile = new File(path + "/" + filename);
//                                    if(tmpFile.isDirectory()){
//                                        File[] deleteFolderList = tmpFile.listFiles();
//                                        Log.e(TAG_VERSION, "deleting directory: " + path + "/" + filename);
//                                        for (int j = 0; j < deleteFolderList.length; j++  ) {
//                                            System.out.println();
//                                            Log.e(TAG_VERSION, "deleting file: " + deleteFolderList[j].getAbsolutePath());
//                                            deleteFolderList[j].delete();
//                                        }
//                                    }else{
//                                        Log.e(TAG_VERSION, "deleting file: " + path + "/" + filename);
//                                        tmpFile.delete();
//                                    }
//
//                                }
//                                filename = "";
//                            }
//                        }
//                    }
//                }
//                progressBarTxt.setVisibility(View.VISIBLE);
//                if(isCorrectPatch==false && patchCnt>3){
//                    progressBarTxt.setText("패치 서버에 오류가 있어 구 버전의 앱을 실행합니다. 녹음결과 분석 중 수정안된 오차가 있을 수 있습니다.");
//                    confirmBtn.setVisibility(View.VISIBLE);
//                }else {
//                    if (isCorrectPatch) {
//                        Log.e(TAG_VERSION, "정상 버전임(version: " + StaticVariables.version + "): " + filename);
////                        progressBarTxt.setText("최신 버전입니다");
////                        confirmBtn.setVisibility(View.VISIBLE);
//                        progressOFF();
//                    } else {
//                        Log.e(TAG_VERSION, "최신 버전이 아닙니다.(version: " + StaticVariables.version + "): " + (filename.equals("") ? "jar가 없음":filename));
//                        progressDialog.show();
//                        progressBarTxt.setText("최신 버전이 아님으로 업데이트를 진행합니다.");
//                        patchCnt++;
//                        //데이터 와이파이인지의 여부를 물어봐야하는 곳
//                        wifiCheck(path, pathReference, StaticVariables.version,activity);
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<JsonObject> call, Throwable t) {
//
//            }
//        });
//
//
//    }


}
