package kr.co.dwebss.kococo.fragment.recorder;

import android.os.Process;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.fragment.RecordFragment;
import kr.co.dwebss.kococo.fragment.recorderUtil.ShortValues;
import kr.co.dwebss.kococo.model.AnalysisRawData;
import kr.co.dwebss.kococo.util.AudioCalculator;
import kr.co.dwebss.kococo.util.SimpleLame;
import kr.co.dwebss.kococo.util.WaveFormatConverter;

public class RecordingThread extends Thread {
    private static final String LOG_TAG2 = "audio_recording2";
    private static final String LOG_TAG3 = "audio_recording3";
    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    int frameByteSize = 1024;
    static List<StartEnd> snoringTermList;
    public static List<StartEnd> osaTermList;
    static List<StartEnd> grindingTermList;
    List<AnalysisRawData> AllAnalysisRawDataList;

    boolean isRecording = false;
    ByteArrayOutputStream baos;
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
    private AudioCalculator audioCalculator;
    byte[] frameBytes = new byte[frameByteSize];
    byte[] frameBytesForSnoring = new byte[frameByteSizeForSnoring];
    FFTDataThread fftDataThread;
    int l = 0;

    RecordFragment recordFragment;
    short[] tmpBytes = null;
    double[] allFHAndDB = null;

    public RecordingThread(RecordFragment recordFragment) {
        this.recordFragment = recordFragment;
    }
    public void run(){
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO );
        long shortsRead = 0;

        audioCalculator = new AudioCalculator();

        long recordStartingTIme = 0L;
        snoringTermList = new ArrayList<StartEnd>();
        grindingTermList = new ArrayList<StartEnd>();
        osaTermList = new ArrayList<StartEnd>();
        AllAnalysisRawDataList = new ArrayList<AnalysisRawData>();
        JsonArray ansList = new JsonArray();

        double times=0.0;
        int i = 0;
        boolean isRecording = false;
        boolean soundStartInRecording = false;
        int snoringBufferFilledCnt = 0;
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

        while (recordFragment.getShouldContinue()) {
            times = (((double) (frameBytes.length / (44100d * 16 * 1))) * 8) * i;
            int numberOfShort = recordFragment.getAudioRecord().read(audioData, 0, audioData.length);
            shortsRead += numberOfShort;
            if(numberOfShort<0){
                continue;
            }
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
                tmpBytes = getAmplitudesFromBytesShort(frameBytesForSnoring);
                fftDataThread = new FFTDataThread(this);
                fftDataThread.setPriority(Thread.MAX_PRIORITY);
                fftDataThread.start();
                /*
                ShortValues shortValues = new ShortValues(tmpBytes);
                fft.run(shortValues, fftData);
                allFHAndDB = fftData.getItemsArray();
                */

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
            if (SleepCheck.noiseCheckForStart(decibel) >= 1 && isRecording == false
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
                String[] fileInfo = wfc.saveLongTermMp3(fileName, recordFragment.getThisContext(), waveData);

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
                        ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.start*1000))));
                        ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.end*1000))));
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
                        ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.start*1000))));
                        ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.end*1000))));
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
                        ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.start*1000))));
                        ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.end*1000))));
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
            int encResult = SimpleLame.encode(audioData, audioData, numberOfShort, recordFragment.getMp3buffer());
            if (encResult != 0) {
                baos.write(recordFragment.getMp3buffer(), 0, encResult);
            }

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
                    if(tmpTime<31){
                        Log.v(LOG_TAG2,(calcTime(times))+" 1번");
                        AllAnalysisRawDataList.add(maxARD);
                    }
                    //if(tmpTime>60 && tmpTime%30 ==2) {
                    /*
                    if(tmpTime%30 ==2) {
                        Log.v(LOG_TAG2,(calcTime(times))+" 2번");
                        //Log.v(LOG_TAG2,(calcTime(times)+" "+calcTime(maxARD.getTimes())+" "+maxARD.getDecibel()));
                        AllAnalysisRawDataList.add(maxARD);
                    }
                    */

                    //Log.v(LOG_TAG2,(calcTime(times))+" "+calcTime((System.currentTimeMillis()/1000)%60));
                    if(tmpTime>30) {

                        double tmpCM = (times+(int) (recordFragment.getRecordStartDtl() / 1000) % 60);
                        double tmpBeforeCM = (AllAnalysisRawDataList.get(AllAnalysisRawDataList.size()-1).getTimes()+(int) (recordFragment.getRecordStartDtl() / 1000) % 60);
                        int tmpM = calcMinute(tmpCM);
                        int tmpBeforeM = calcMinute(tmpBeforeCM);
                        //Log.v(LOG_TAG2,(calcTime(times)+" "+tmpCM+" "+tmpBeforeCM+" "+tmpM+" "+tmpBeforeM));
                        if(tmpM!=tmpBeforeM){
                            Log.v(LOG_TAG2,(calcTime(times))+" 3번 "+tmpTime);
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
        if (isRecording == true && recordFragment.getRecordeFlag() == false) {
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
            String[] fileInfo = wfc.saveLongTermMp3(fileName, recordFragment.getThisContext(), waveData);

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
                    ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.start*1000))));
                    ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.end*1000))));
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
                    ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.start*1000))));
                    ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.end*1000))));
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
                    ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.start*1000))));
                    ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl()+se.end*1000))));
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

        recordFragment.setRecordData(new JsonObject());
//                recordData.addProperty("recordEndDt",recordEndDt);
        recordFragment.getRecordData().addProperty("recordEndDt",dayTimeDefalt.format(new Date(System.currentTimeMillis())));
        recordFragment.getRecordData().add("analysisList", ansList);

        Log.v(LOG_TAG2,(" =============녹음 종료버튼  ===========recordData: "+recordFragment.getRecordData().toString()));

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

    public int getFrameBytesForSnoringLength() {
        return this.frameBytesForSnoring.length;
    }

    public short[] getTmpBytes() {
        return this.tmpBytes;
    }

    public void setAllFHAndDB(double[] allFHAndDB) {
        this.allFHAndDB = allFHAndDB;
    }

    public static class SleepCheck {

        static double OSAcurTermTime = 0.0;

        static boolean isBreathTerm = false;
        static boolean isOSATerm = false;
        static int isBreathTermCnt = 0;
        static int isBreathTermCntOpp = 0;
        static int isOSATermCnt = 0;
        static int isOSATermCntOpp = 0;
        static String beforeTermWord = "";
        static String BREATH = "breath";

        static double decibelSumCnt = 0;

        static int AVR_DB_CHECK_TERM = 2000;
        static double MAX_DB_CRIT_VALUE = -31.5;
        static double MIN_DB_CRIT_VALUE = -(31.5-(31.5*35/120)); //http://www.noiseinfo.or.kr/about/info.jsp?pageNo=942 조용한 공원(수면에 거의 영향 없음) 35, 40부터 낮아진다

        static int noiseChkSum = 0;
        static int noiseNoneChkSum = 0;
        static int noiseChkCnt = 0;
        static int noiseChkForStartSum = 0;
        static int noiseNoneChkForStartSum = 0;
        static int noiseChkForStartCnt = 0;

        static double MAX_DB = -31.5;
        static double MIN_DB = 0;

        static boolean isOSATermTimeOccur = false;
        static boolean isOSAAnsStart = false;

        static double getMinDB() {
            /*
            double avrDB = -AVR_DB_INIT_VALUE;
            if (decibelSum != 0 && decibelSumCnt != 0) {
                avrDB = decibelSum / decibelSumCnt;
            }
            //System.out.print(decibelSum+" "+decibelSumCnt+" "+avrDB+" ");
            */
            return MIN_DB/2 > MIN_DB_CRIT_VALUE ? Math.floor(MIN_DB_CRIT_VALUE) : MIN_DB/2;
        }

        static double setMinDB(double decibel) {
            //10분마다 평균 데시벨을 다시 계산한다.
            if(Math.abs(decibel) != 0 && decibel < MIN_DB) {
                MIN_DB = decibel;
            }
            /*
            if (decibelSumCnt >= AVR_DB_CHECK_TERM) {
                decibelSum = 0;
                decibelSumCnt = 0;
            }
            double avrDB = -AVR_DB_INIT_VALUE;
            decibelSum += decibel;
            decibelSumCnt ++;
            if (decibelSum != 0 && decibelSumCnt != 0) {
                avrDB = decibelSum / decibelSumCnt;
            }
            */
            return MIN_DB/2 > MIN_DB_CRIT_VALUE ? Math.floor(MIN_DB_CRIT_VALUE) : MIN_DB/2;
        }
        static double getMaxDB() {
            /*
            double avrDB = -AVR_DB_INIT_VALUE;
            if (decibelSum != 0 && decibelSumCnt != 0) {
                avrDB = decibelSum / decibelSumCnt;
            }
            //System.out.print(decibelSum+" "+decibelSumCnt+" "+avrDB+" ");
            */
            return MAX_DB*2 < MAX_DB_CRIT_VALUE ? Math.floor(MAX_DB_CRIT_VALUE) : MAX_DB*2;
        }

        static double setMaxDB(double decibel) {
            //10분마다 평균 데시벨을 다시 계산한다.
            if(Math.abs(decibel) != 0 && decibel > MAX_DB) {
                MAX_DB = decibel-1;
            }
            if (decibelSumCnt >= AVR_DB_CHECK_TERM) {
                decibelSumCnt = 0;
                MAX_DB = -31.5;
                MIN_DB = 0;
            }
            decibelSumCnt ++;
            /*
            if (decibelSumCnt >= AVR_DB_CHECK_TERM) {
                decibelSum = 0;
                decibelSumCnt = 0;
            }
            double avrDB = -AVR_DB_INIT_VALUE;
            decibelSum += decibel;
            decibelSumCnt ++;
            if (decibelSum != 0 && decibelSumCnt != 0) {
                avrDB = decibelSum / decibelSumCnt;
            }
            */
            return MAX_DB*2 < MAX_DB_CRIT_VALUE? Math.floor(MAX_DB_CRIT_VALUE) : MAX_DB*2;
        }
        static int noiseCheck(double decibel) {
            //1분동안 소리가 발생하지 않았는지 체크한다.
            //0.01초 단위임으로, 6000번 해야 60초임.
            //1분이 되었으면, 데시벨보다 높은 소리가 발생하지 않은 경우
            if(noiseChkCnt>=6000) {
                int tmpN = noiseChkSum;
                noiseChkCnt = 0;
                noiseChkSum = 0;
                noiseNoneChkSum = 0;
                return tmpN;
            }else {
                //아직 1분이 안되었으면 계속 소리 체크를 한다.
                //소리 체크는 1분동안 평균 데시벨보다 최저 임계 데시벨의 소리가 발생했는지를 체크한다.
                //리턴이 0이면 녹음 종료하게 되어있음.X
                if(decibel >= getMinDB()) {
                    //noiseChkCnt++;
                    noiseChkSum++;
                }else {
                    noiseNoneChkSum++;
                }
                noiseChkCnt++;
                return 6001;
                //return noiseChkCnt;
            }

        }

        static int noiseCheckForStart(double decibel) {
            //1분동안 소리가 발생하지 않았는지 체크한다.
            //0.01초 단위임으로, 6000번 해야 60초임.
            //1분이 되었으면, 데시벨보다 높은 소리가 발생하지 않은 경우
            if(noiseChkForStartCnt>=200) {
                int tmpN = noiseChkForStartSum;
                noiseChkForStartCnt = 0;
                noiseChkForStartSum = 0;
                noiseNoneChkForStartSum = 0;
                return tmpN;
            }else {
                //아직 1분이 안되었으면 계속 소리 체크를 한다.
                //소리 체크는 1분동안 평균 데시벨보다 높은 데시벨의 소리가 발생했는지를 체크한다.
                //리턴이 0이면 녹음 종료하게 되어있음.
                if(decibel >= getMinDB()) {
                    //noiseChkCnt++;
                    noiseChkForStartSum++;
                }else {
                    noiseNoneChkForStartSum++;
                }
                noiseChkForStartCnt++;
                return -1;
                //return noiseChkCnt;
            }

        }

        /*
         * 무호흡증은 항상 코골이를 동반하며, 코골이의 시작하고 종료한 시간은 5~10초이내 숨을 쉬어야 하기 때문에 호흡이 가파른 느낌이 있다,
         * 무호흡 코골이 시간이 종료한 후 숨을 멈추는 시간이 30~50초 이내이다. 1.db세기로 무호흡코골이 및 호흡으로 측정되는 부분을
         * 특정한다. 2. 호흡은 0.7초 간격은 0.2초 3. 2번이 유지되면 무호흡코골이 혹은 호흡하는 구간이다. 4. 3번을 유지하지 않는경우
         * 일정 db이하로 30~50초 동안 유지되는지를 체크함으로써 무호흡 구간인지 특정한다.
         */
        static int OSACheck(double times, double decibel, int amplitude, double frequency) {
            // 2. 기준 데시벨보다 높은 소리라면 호흡(혹은 코골이) 구간인지 체크한다.
            //System.out.println("OSACheckDb:" +decibel +"vs" + getMinDB());
            if (decibel > getMinDB()*0.45) {
                // 2-1. 데시벨을 이용해서 연속된 소리인지 체크한다.
                // 2-1-1. 연속된 소리인지 체크하기 위해서는 비슷한 데시벨인지만 체크한다.
                // (주파수나 진폭은 0.01초 단위로 상이하기 때문에 팩터로 이용할 수 없음.)
                // 2-1-2. 비슷한 데시벨은 기준 정보의 데시벨에서 +-1db로 하며, 체크될 경우 숨쉬는 구간 카운트를 1씩 증가한다.
                // 체크가 안되면 숨쉬기 아님 카운트를 1씩 증가한다. 숨쉬기구간은 true 된다.
                /*
                 * if (Math.abs(Math.abs(curTermDb) - Math.abs(decibel)) < 1 // 데시벨 오차 범위가
                 * 1db인지만 체크 ) { isBreathTermCnt++; isBreathTerm = true; // 2-1-2-1. 숨쉬기 구간이
                 * true가 될 때, 무호흡 구간이 true인 경우, 무호흡 구간을 false로 바꾸며, // 이때 기준 정보의 시간은 무호흡 시작시간,
                 * 종료시간은 무호흡 종료시간이 된다. // 무호흡 구간 카운트 로깅을 하고 무호흡 구간 카운트를 초기화 한다. if(isOSATerm ==
                 * true) { System.out.println("["+String.format("%.2f", curTermTime) +
                 * "~"+String.format("%.2f", times) + "s, isOSATermCnt: " +
                 * isOSATermCnt+", isOSATermCntOpp:"+isOSATermCntOpp+"]"); curTermTime = times;
                 * isOSATerm = false; isOSATermCnt = 0; } } else { isBreathTermCntOpp++; }
                 */

                if (isOSATerm == true) {
                    // 무호흡에서 호흡으로 넘어오는 경우 오차범위가 5초는 넘어야 무호흡구간으로 본다.
                    if (beforeTermWord.equals(BREATH) && isOSATermCnt > 1000) {
                        isOSATermTimeOccur=false;
                        /*
                         * if(beforeTermWord.equals(OSA)) { System.out.println("["+String.format("%.2f",
                         * curTermTime) + "~"+String.format("%.2f", times) + "s, isOSATermCnt: " +
                         * isOSATermCnt+", isOSATermCntOpp:"+isOSATermCntOpp+"]"); }else {
                         * System.out.println("["+String.format("%.2f", curTermTime) +
                         * "~"+String.format("%.2f", times) + "s, isOSATermCnt: " +
                         * isOSATermCnt+", isOSATermCntOpp:"+isOSATermCntOpp+"]"); curTermTime = times;
                         * }
                         */
                        //System.out.println("![" + String.format("%.2f", OSAcurTermTime) + "~" + String.format("%.2f", times)+ "s, isOSATermCnt: " + isOSATermCnt + ", isOSATermCntOpp:" + isOSATermCntOpp + "]");
                        //분석이 종료되는 시점은 앞에서 분석된 시간으로부터 1분이상 초과된 경우에 종료하고, 아닌 경우에는 현재 시간을 end.times에 추가한다.
                        if(osaTermList.size()>1) {
                            int beforeEndTime = (int) osaTermList.get(osaTermList.size()-2).end; //0이거나 값이 있거나
                            int currentTime = (int) times;
                            //System.out.println(beforeEndTime +" "+ currentTime+"="+(currentTime-beforeEndTime));
                            if(currentTime - beforeEndTime > 60) {
                                //System.out.println("기록vo종료");
                                isOSAAnsStart = false;
                                osaTermList.get(osaTermList.size()-1).end=times;
                                osaTermList.get(osaTermList.size()-1).first = firstDecibelAvg;
                                osaTermList.get(osaTermList.size()-1).second = secondDecibelAvg;
                                osaTermList.get(osaTermList.size()-1).chk = snoringDbChkCnt;
                                osaTermList.get(osaTermList.size()-1).positiveCnt = isOSATermCnt;
                                osaTermList.get(osaTermList.size()-1).negitiveCnt = isOSATermCntOpp;
                            }else {
                                //System.out.println("1분이 안 지났으므로, 기록vo종료하지 않고, 이전기록vo에 종료입력, 현재 기록vo 삭제");
                                osaTermList.get(osaTermList.size()-2).AnalysisRawDataList.addAll(
                                        osaTermList.get(osaTermList.size()-1).AnalysisRawDataList);
                                osaTermList.get(osaTermList.size()-1).AnalysisRawDataList=osaTermList.get(osaTermList.size()-2).AnalysisRawDataList;
                                osaTermList.remove(osaTermList.size()-1);
                                osaTermList.get(osaTermList.size()-1).end=times;
                                osaTermList.get(osaTermList.size()-1).first = firstDecibelAvg;
                                osaTermList.get(osaTermList.size()-1).second = secondDecibelAvg;
                                osaTermList.get(osaTermList.size()-1).chk = snoringDbChkCnt;
                                osaTermList.get(osaTermList.size()-1).positiveCnt = isOSATermCnt;
                                osaTermList.get(osaTermList.size()-1).negitiveCnt = isOSATermCntOpp;
                            }
                        }else {
                            isOSAAnsStart = false;
                            if(osaTermList.size()>0) {
                                osaTermList.get(osaTermList.size() - 1).end = times;
                                osaTermList.get(osaTermList.size() - 1).first = firstDecibelAvg;
                                osaTermList.get(osaTermList.size() - 1).second = secondDecibelAvg;
                                osaTermList.get(osaTermList.size() - 1).chk = snoringDbChkCnt;
                                osaTermList.get(osaTermList.size() - 1).positiveCnt = isOSATermCnt;
                                osaTermList.get(osaTermList.size() - 1).negitiveCnt = isOSATermCntOpp;
                            }
                        }

                        double tmpD = OSAcurTermTime;
                        OSAcurTermTime = times;
                        isOSATerm = false;
                        isOSATermCnt = 0;
                        isOSATermCntOpp = 0;
                        // beforeTermWord=OSA;
                        return (int) (times-tmpD);
                    } else {
                        /*
                        System.out.println("[ignore term, "+String.format("%.2f", curTermTime) +
                         "~"+String.format("%.2f", times) + "s, isOSATermCnt: " + isOSATermCnt+", isOSATermCntOpp:"+isOSATermCntOpp+"]");
                        */
                        //curTermTime = times;
                        isOSATerm = false;
                        isOSATermCnt = 0;
                        isOSATermCntOpp = 0;
                        // beforeTermWord=OSA;
                    }
                } else {
                    //현재 데시벨이 기준 데시벨보다 크고, 무호흡 구간이 아닌 경우이다. 여기서 초기화를 해야한다.
                    //문제는 무호흡 구간이 아닌 경우 0.01초 단위로 계속 이곳을 타기 때문에, 한번만 초기화할 수 있어야 한다. 한번 초기화 했으면 다시 안하도록 한다.
                    //위에서 문제는 한번 초기화 한 경우 텀이 너무 길어진다는 문제가 있다. 앞에서부터의 15초 데이터만 저장하도록 한다.
                    //15초인 이유는 무호흡이 발생하는 데이터의 호흡시간이 15초 정도 발생하기 떄문이다.
                    //분석이 종료되는 시점은 앞에서 분석된 시간으로부터 1분이상 초과된 경우에 종료하고, 아닌 경우에는 현재 시간을 end.times에 추가한다.
                    //초기화를 한적이 있거나, 초기화하고 15초가 지났나?
                    if(!isOSATermTimeOccur || (isOSATermTimeOccur && times-OSAcurTermTime>15)) {
                        isOSAAnsStart = true;
                        OSAcurTermTime = times;
                        if(isOSATermTimeOccur && osaTermList.size()>0) {
                            //System.out.println("이전기록vo취소");
                            osaTermList.remove(osaTermList.size()-1);
                        }
                        //System.out.println("기록vo생성");
                        osaTermList.add(new StartEnd());
                        osaTermList.get(osaTermList.size()-1).start=times;
                        osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList = new ArrayList<AnalysisRawData>();
                        //osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(times, amplitude, tmpMaxDb, frequency));
                        isOSATermTimeOccur = true;
                        //System.out.println(OSAcurTermTime);
                    }

                    isBreathTermCnt++;
                    isBreathTerm = true;
                }
                // 2-1-3. 숨쉬기 아님 카운트가 20을 넘으면(0.2초가 초과되면), 숨쉬는 구간 카운트가 70(0.7초) 미만일 시,
                // 숨쉬는 구간 카운트, 숨쉬기 아님 카운트를 0으로 초기화 한다. 숨쉬기구간은 false가 된다.
            } else {
                // 3. 기준 데시벨보다 낮은 소리인 경우는 무호흡 중인지 체크한다.
                // 3-1. 숨쉬기 구간이 false인 경우, 무호흡 구간 카운트를 증가하며, 무호흡 구간은 true가 된다.
                if (isBreathTerm == false) {
                    isOSATermCnt++;
                    isOSATerm = true;
                } else {
                    // 3-1-2. 숨쉬기 구간이 true인 경우에는 숨쉬기 아님 카운트를 증가시킨다.
                    isBreathTermCntOpp++;
                    isOSATermCntOpp++;
                }
            }

            if (isBreathTermCntOpp > 20) {
                if (isBreathTermCnt < 70) {
                    // 일정 데시벨 이상이고, 숨쉬기 카운트의 0.2초 오차가 발생한 데이터로 무시함.
                    // System.out.println("[ignore term, "+String.format("%.2f", curTermTime) +
                    // "~"+String.format("%.2f", times) + "s, isBreathTermCnt: " +
                    // isBreathTermCnt+", isBreathTermCntOpp: "+isBreathTermCntOpp+"]");
                    // curTermTime = time;
                    isBreathTermCnt = 0;
                    isBreathTermCntOpp = 0;
                    isBreathTerm = false;
                    // beforeTermWord=BREATH;
                } else {
                    // 2-1-3-1. 기준 정보의 기준 시간이 숨쉬기 시작시간, 현재 시간은 숨쉬기 종료 시간
                    // (이 시간은 한번 호흡이지 무호흡 사이의 구간을 의미하지는 않는다.)
                    /*
                     * if(beforeTermWord.equals(BREATH)) {
                     * System.out.println("["+String.format("%.2f", curTermTime) +
                     * "~"+String.format("%.2f", times) + "s, isBreathTermCnt: " +
                     * isBreathTermCnt+", isBreathTermCntOpp: "+isBreathTermCntOpp+"]"); }else {
                     * System.out.println("["+String.format("%.2f", curTermTime) +
                     * "~"+String.format("%.2f", times) + "s, isBreathTermCnt: " +
                     * isBreathTermCnt+", isBreathTermCntOpp: "+isBreathTermCntOpp+"]"); curTermTime
                     * = times; }
                     */
                    // System.out.println("["+String.format("%.2f", curTermTime) +
                    // "~"+String.format("%.2f", times) + "s, isBreathTermCnt: " +
                    // isBreathTermCnt+", isBreathTermCntOpp: "+isBreathTermCntOpp+"]");
                    //System.out.println("!!");
                    /*
                    if(times-OSAcurTermTime >15) {
                        OSAcurTermTime = times;
                        System.out.println(OSAcurTermTime);
                        EventFireGui.osaTermList.add(new StartEnd());
                        EventFireGui.osaTermList.get(EventFireGui.osaTermList.size()-1).start=OSAcurTermTime;
                        isOSATermTimeOccur = true;
                    }
                    */
                    isBreathTermCnt = 0;
                    isBreathTermCntOpp = 0;
                    isBreathTerm = false;
                    beforeTermWord = BREATH;
                }
            }
            // 1. 연속된 소리가 되는 기준 정보를 초기화 한다.
            // 1-1. 기준 정보의 기준 시간은 기준 시간이 0이거나, 숨쉬는 구간 카운트가 0이며, 숨쉬기 아님 카운트가 0일 경우 초기화 한다.
            if (OSAcurTermTime == 0 || (isBreathTermCnt == 0 && isOSATermCnt == 0)) {
                //System.out.println("@@");
                //OSAcurTermTime = times;
            }
            return 0;
        }
    }
}
