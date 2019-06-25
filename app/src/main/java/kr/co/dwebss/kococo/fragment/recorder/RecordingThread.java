package kr.co.dwebss.kococo.fragment.recorder;

import android.os.Process;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dalvik.system.DexClassLoader;
import kr.co.dwebss.kococo.activity.StaticVariables;
import kr.co.dwebss.kococo.fragment.RecordFragment;
import kr.co.dwebss.kococo.util.AudioCalculator;
import kr.co.dwebss.kococo.util.SimpleLame;
import kr.co.dwebss.kococo.util.WaveFormatConverter;
import kr.co.dwebss.soundanalysis.AnalysisRawData;
import kr.co.dwebss.soundanalysis.StartEnd;

import static kr.co.dwebss.kococo.activity.StaticVariables.isCorrectPatch;

public class RecordingThread extends Thread {
    private static final String LOG_TAG2 = "audio_recording2";
    private static final String LOG_TAG3 = "audio_recording3";
    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    int frameByteSize = 1024;
    static List<StartEnd> snoringTermList;
    public static List<StartEnd> osaTermList;
    static List<StartEnd> grindingTermList;
    List<StartEnd> noiseTermListForOsaList;
    List<kr.co.dwebss.soundanalysis.AnalysisRawData> AllAnalysisRawDataList;

    ByteArrayOutputStream baos;

    short[] audioData = new short[frameByteSize/2];

    int frameByteSizePer = 16;
    int frameByteSizeForSnoring = 1024*frameByteSizePer;

    static boolean isOSATermTimeOccur = false;
    private AudioCalculator audioCalculator;
    byte[] frameBytes = new byte[frameByteSize];
    byte[] frameBytesForSnoring = new byte[frameByteSizeForSnoring];
    FFTDataThread fftDataThread;
    int l = 0;

    RecordFragment recordFragment;
    short[] tmpBytes = null;
    double[] allFHAndDB = null;
    Class<Object> SleepCheck;
    Method setMaxDB;
    Method setMinDB;
    Field tmpMinDb;
    Field tmpMaxDb;
    Method noiseCheckForStart;
    Method noiseCheck;
    Method snoringCheck;
    Method osaCheck;
    Method someNoiseCheck;
    Class<Object> AnalysisRawData;
    Field CHECKED_STATUS;
    Field CHECKED_ERROR;
    Field allFHAndDb_NEED_INITIALIZE;
    Field noiseChkSum;
    Field noiseChkCnt;
    int errLoginCnt = 0;

    public RecordingThread(RecordFragment recordFragment) {
        this.recordFragment = recordFragment;

        try{
            final String libPath = recordFragment.getThisContext().getFilesDir().getAbsolutePath() + "/libs/SoundAnalysis_" + StaticVariables.version + ".jar";
            Log.e("yrseo", "libPath: "+libPath+" isExists: "+new File(libPath).exists());
            final File tmpDir = new File(recordFragment.getThisContext().getFilesDir()+"/libs/dex");
            Log.e("yrseo", "tmpDir: "+tmpDir+" isExists: "+tmpDir.exists());
            if(!tmpDir.exists()){
                tmpDir.mkdir();
            }
            if(isCorrectPatch) {
                final DexClassLoader classloader = new DexClassLoader(libPath, tmpDir.getAbsolutePath(), null, this.getClass().getClassLoader());
                SleepCheck = (Class<Object>) classloader.loadClass("kr.co.dwebss.soundanalysis.SleepCheck");
                AnalysisRawData = (Class<Object>) classloader.loadClass("kr.co.dwebss.soundanalysis.AnalysisRawData");

                Field[] fields = SleepCheck.getFields();
                for (Field field : fields) {
                    Log.e("yrseo", field.getType().getName() + " " + field.getName());
                }
                Log.e("yrseo", "--------------------------------------------");

                Method[] methods = SleepCheck.getMethods();
                StringBuffer sb = new StringBuffer();
                for (Method method : methods) {
                    sb.append(method.getName());

                    Class<?>[] argTypes = method.getParameterTypes();
                    sb.append("(");
                    int size = argTypes.length;
                    for (Class<?> argType : argTypes) {
                        String argName = argType.getName();
                        sb.append(argName + " val");
                        if (--size != 0) {
                            sb.append(", ");
                        }
                    }
                    sb.append(")");

                    Class<?> returnType = method.getReturnType();
                    sb.append(" : " + returnType.getName());

                    Log.e("yrseo", sb.toString());
                    sb.setLength(0);
                }
                Log.e("yrseo", "--------------------------------------------");

                setMaxDB = SleepCheck.getMethod("setMaxDB", double.class);
                setMinDB = SleepCheck.getMethod("setMinDB", double.class);
                tmpMinDb = SleepCheck.getDeclaredField("tmpMinDb");
                tmpMaxDb = SleepCheck.getDeclaredField("tmpMaxDb");
                noiseCheckForStart = SleepCheck.getMethod("noiseCheckForStart", double.class);
                noiseCheck = SleepCheck.getMethod("noiseCheck", double.class);
                snoringCheck = SleepCheck.getMethod("snoringCheck", double[].class, double.class, double.class, java.util.List.class, java.util.List.class, AnalysisRawData.class);
                osaCheck = SleepCheck.getMethod("osaCheck", double.class, double.class, java.util.List.class, java.util.List.class, java.util.List.class);
                someNoiseCheck = SleepCheck.getMethod("someNoiseCheck", double.class, double.class, java.util.List.class);
                CHECKED_STATUS = SleepCheck.getDeclaredField("CHECKED_STATUS");
                CHECKED_ERROR = SleepCheck.getDeclaredField("CHECKED_ERROR");
                allFHAndDb_NEED_INITIALIZE = SleepCheck.getDeclaredField("allFHAndDb_NEED_INITIALIZE");
                noiseChkSum = SleepCheck.getDeclaredField("noiseChkSum");
                noiseChkCnt = SleepCheck.getDeclaredField("noiseChkCnt");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void run(){
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO );
        long shortsRead = 0;

        audioCalculator = new AudioCalculator();

        long recordStartingTIme = 0L;
        snoringTermList = new ArrayList<StartEnd>();
        grindingTermList = new ArrayList<StartEnd>();
        osaTermList = new ArrayList<StartEnd>();
        noiseTermListForOsaList = new ArrayList<StartEnd>();
        AllAnalysisRawDataList = new ArrayList<AnalysisRawData>();
        JsonArray ansList = new JsonArray();

        double times=0.0;
        int i = 0;
        boolean isRecording = false;
        int snoringBufferFilledCnt = 0;


        AnalysisRawData maxARD = null;
        double timesForMaxArd = 0.0;

        int recordingLength = 0;
        double tmpMaxDbVal = 0;
        int noiseChkSumVal = 0;
        int noiseChkCntVal = 0;
try {
    while (recordFragment.getShouldContinue()) {
        times = (((double) (frameBytes.length / (44100d * 16 * 1))) * 8) * i;
        int numberOfShort = recordFragment.getAudioRecord().read(audioData, 0, audioData.length);
        shortsRead += numberOfShort;
        if (numberOfShort < 0) {
            continue;
        }
        frameBytes = shortToByte(audioData, numberOfShort);
        int amplitude = 0;
        double decibel = 0;
        double frequency = 0;
        audioCalculator.setBytes(frameBytes);
        try {
            // 소리가 발생하면 녹음을 시작하고, 1분이상 소리가 발생하지 않으면 녹음을 하지 않는다.
            amplitude = audioCalculator.getAmplitude();
            decibel = audioCalculator.getDecibel();
            frequency = audioCalculator.getFrequency();
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.v(LOG_TAG2, e.getMessage());
            continue;
        }

        //전체 진폭을 가져온다.
        //전체 진폭에 대한 주파수, 주파수의 갭=hzPerDataPoint
        //전체 진폭에 대한 주파수 리스트 길이=fftSize
        if (snoringBufferFilledCnt < frameByteSizePer) {
            System.arraycopy(frameBytes, 0, frameBytesForSnoring, frameBytes.length * snoringBufferFilledCnt, frameBytes.length);
            snoringBufferFilledCnt++;
        }

        if (snoringBufferFilledCnt == frameByteSizePer) {
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
        //SleepCheck.setMaxDB(decibel);
        //SleepCheck.setMinDB(decibel);
        try {
            setMaxDB.invoke(SleepCheck, decibel);
            setMinDB.invoke(SleepCheck, decibel);
        } catch (Exception e) {
            kr.co.dwebss.soundanalysis.SleepCheck.setMaxDB(decibel);
            kr.co.dwebss.soundanalysis.SleepCheck.setMaxDB(decibel);
            if(errLoginCnt<6){
                Log.e(LOG_TAG2, "패치 로드를 실패하여 jar 로직을 수행합니다. " + e.getMessage());
                e.printStackTrace();
                errLoginCnt++;
            }
        }

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
        //if (SleepCheck.noiseCheckForStart(decibel) >= 1 && isRecording == false
        int noiseCheckForStartVal = 0;
        int noiseCheckVal = 0;
        try {
            noiseCheckForStartVal = (Integer) noiseCheckForStart.invoke(SleepCheck, decibel);
            noiseCheckVal = (Integer) noiseCheck.invoke(SleepCheck, decibel);
        }catch(Exception e){
            noiseCheckForStartVal = kr.co.dwebss.soundanalysis.SleepCheck.noiseCheckForStart(decibel);
            noiseCheckVal = kr.co.dwebss.soundanalysis.SleepCheck.noiseCheck(decibel);
            if(errLoginCnt<6){
                Log.e(LOG_TAG2, "패치 로드를 실패하여 jar 로직을 수행합니다. " + e.getMessage());
                e.printStackTrace();
                errLoginCnt++;
            }
        }
        if ( noiseCheckForStartVal >= 1 && isRecording == false
                && Math.floor((double) (audioData.length / (44100d * 16 * 1)) * 8) != Math.floor(times)) {
            Log.v(LOG_TAG2, (calcTime(times) + "(" + String.format("%.2f", times) + "s) 녹음 시작!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
            //recordStartingTIme = times;
            recordStartingTIme = System.currentTimeMillis();
            baos = new ByteArrayOutputStream();
            recordingLength = 0;
            isRecording = true;
            snoringTermList = new ArrayList<StartEnd>();
            grindingTermList = new ArrayList<StartEnd>();
            osaTermList = new ArrayList<StartEnd>();
            noiseTermListForOsaList = new ArrayList<StartEnd>();
            AllAnalysisRawDataList = new ArrayList<AnalysisRawData>();
            isOSATermTimeOccur = false;
//                    } else if (isRecording == true && (SleepCheck.noiseCheck(decibel)==0 || recodeFlag==false) ) {
            //} else if (isRecording == true && SleepCheck.noiseCheck(decibel) <= 100) {
        //} else if (isRecording == true && SleepCheck.noiseCheck(decibel) <= 100) {
        } else if (isRecording == true && noiseCheckVal <= 100) {
            Log.v(LOG_TAG2, (calcTime(times) + "(" + String.format("%.2f", times) + "s) 녹음 종료!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"));
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

            Log.v(LOG_TAG2, ("=====녹음중 분석 종료, 분석정보 시작====="));
            Log.v(LOG_TAG2, ("녹음파일 길이(s): " + ((double) (recordingLength / (44100d * 16 * 1))) * 8));
            Log.v(LOG_TAG2, ("tmpMinDb: " + tmpMinDb));
            Log.v(LOG_TAG2, ("tmpMaxDb: " + tmpMaxDb));

            JsonObject ans = new JsonObject();
            StartEnd tmpSE = new StartEnd(); //전체 분석 데이터를 변환하기 위해 임시로 vo를 생성
            tmpSE.AnalysisRawDataList = AllAnalysisRawDataList;

            try {
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                String andRList = gson.toJson(printAnalysisRawDataList(tmpSE));
                //Log.v(LOG_TAG2,andRList);
                ans.addProperty("recordingData", andRList);

            } catch (NullPointerException e) {
                e.getMessage();
            }
            //ans.setAnalysisStartDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(recordStartingTIme), ZoneId.systemDefault()));
            //ans.setAnalysisEndDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
            ans.addProperty("analysisStartDt", dayTimeDefalt.format(new Date(recordStartingTIme)));
            ans.addProperty("analysisEndDt", dayTimeDefalt.format(new Date(time)));
            ans.addProperty("analysisFileAppPath", fileInfo[0]);
            ans.addProperty("analysisFileNm", fileInfo[1]);
            JsonArray ansDList = new JsonArray();
            JsonObject ansd = new JsonObject();
            for (int s = 0; s < snoringTermList.size(); s++) {
                if (s > 0) {
                    StartEnd se = snoringTermList.get(s);
                    StartEnd bse = snoringTermList.get(s - 1);
                    double curStartTime = se.start;
                    double beforeEndTime = bse.end;
                    if (curStartTime - beforeEndTime <= 1 && se.end != 0) {
                        bse.end = se.end;
                        bse.negitiveCnt += se.negitiveCnt;
                        bse.positiveCnt += se.positiveCnt;
                        bse.first = (bse.first + se.first);
                        bse.second = (bse.second + se.second);
                        bse.chk += se.chk;
                        bse.AnalysisRawDataList.addAll(se.AnalysisRawDataList);
                        snoringTermList.remove(se);
                        s--;
                    }
                }
            }
            for (StartEnd se : snoringTermList) {
                if (se.end != 0 && se.end > se.start) {
                    Log.v(LOG_TAG2, se.getTerm());
                    ansd = new JsonObject();
                    ansd.addProperty("termTypeCd", 200101);
                    ansd.addProperty("termStartDt", dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl() + se.start * 1000))));
                    ansd.addProperty("termEndDt", dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl() + se.end * 1000))));
                    try {
                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        String andRList = gson.toJson(printAnalysisRawDataList(se));
                        Log.v(LOG_TAG2, andRList);
                        ansd.addProperty("analysisData", andRList);

                    } catch (NullPointerException e) {
                        e.getMessage();
                    }
                    ansDList.add(ansd);
                } else {
                    if (snoringTermList != null && snoringTermList.size() > 0) {
                        snoringTermList.remove(snoringTermList.size() - 1);
                    } else {
                        Log.e(LOG_TAG3, "snoringTermList!=null && snoringTermList.size()>0, line 252");
                    }
                }
            }
            for (int s = 0; s < grindingTermList.size(); s++) {
                if (s > 0) {
                    StartEnd se = grindingTermList.get(s);
                    StartEnd bse = grindingTermList.get(s - 1);
                    double curStartTime = se.start;
                    double beforeEndTime = bse.end;
                    if (curStartTime - beforeEndTime <= 1 && se.end != 0) {
                        bse.end = se.end;
                        bse.negitiveCnt += se.negitiveCnt;
                        bse.positiveCnt += se.positiveCnt;
                        bse.first = (bse.first + se.first);
                        bse.second = (bse.second + se.second);
                        bse.chk += se.chk;
                        bse.AnalysisRawDataList.addAll(se.AnalysisRawDataList);
                        grindingTermList.remove(se);
                        s--;
                    }
                }
            }
            for (StartEnd se : grindingTermList) {
                if (se.end != 0 && se.end > se.start) {
                    Log.v(LOG_TAG2, se.getTerm());
                    ansd = new JsonObject();
                    ansd.addProperty("termTypeCd", 200102);
                    ansd.addProperty("termStartDt", dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl() + se.start * 1000))));
                    ansd.addProperty("termEndDt", dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl() + se.end * 1000))));
                    try {
                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        String andRList = gson.toJson(printAnalysisRawDataList(se));
                        Log.v(LOG_TAG2, andRList);
                        ansd.addProperty("analysisData", andRList);

                    } catch (NullPointerException e) {
                        e.getMessage();
                    }
                    ansDList.add(ansd);
                } else {
                    if (grindingTermList != null && grindingTermList.size() > 0) {
                        grindingTermList.remove(grindingTermList.size() - 1);
                    } else {
                        Log.e(LOG_TAG3, "grindingTermList!=null && grindingTermList.size()>0, line 296");
                    }
                }
            }
            for (StartEnd se : osaTermList) {
                if (se.end != 0 && se.end > se.start) {
                    Log.v(LOG_TAG2, se.getTerm());
                    ansd = new JsonObject();
                    ansd.addProperty("termTypeCd", 200103);
                    ansd.addProperty("termStartDt", dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl() + se.start * 1000))));
                    ansd.addProperty("termEndDt", dayTimeDefalt.format(new Date((long) (recordFragment.getRecordStartDtl() + se.end * 1000))));
                    try {
                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        String andRList = gson.toJson(printAnalysisRawDataList(se));
                        Log.v(LOG_TAG2, andRList);
                        ansd.addProperty("analysisData", andRList);

                    } catch (NullPointerException e) {
                        e.getMessage();
                    }
                    ansDList.add(ansd);
                } else {
                    if (osaTermList != null && osaTermList.size() > 0) {
                        osaTermList.remove(osaTermList.size() - 1);
                    } else {
                        Log.e(LOG_TAG3, "osaTermList!=null && osaTermList.size()>0, line 296");
                    }
                }
            }
            ans.add("analysisDetailsList", ansDList);
            ansList.add(ans);
            Log.v(LOG_TAG2, ("=====녹음중 분석 종료, 분석정보 끝====="));
            recordStartingTIme = 0;
            isRecording = false;
        }

        if (isRecording == false) {
            continue;
        }
        //baos.write(frameBytes);
        if (audioData != null) {
            recordingLength += (audioData.length * 2);
        }
        int encResult = SimpleLame.encode(audioData, audioData, numberOfShort, recordFragment.getMp3buffer());
        if (encResult != 0) {
            baos.write(recordFragment.getMp3buffer(), 0, encResult);
        }
        try {
            tmpMaxDbVal = (Double) tmpMaxDb.get(SleepCheck);
            noiseChkSumVal = (Integer) noiseChkSum.get(SleepCheck);
            noiseChkCntVal = (Integer) noiseChkCnt.get(noiseChkCnt);
        }catch(Exception e){
            tmpMaxDbVal = kr.co.dwebss.soundanalysis.SleepCheck.tmpMaxDb;
            noiseChkSumVal = kr.co.dwebss.soundanalysis.SleepCheck.noiseChkSum;
            noiseChkCntVal = kr.co.dwebss.soundanalysis.SleepCheck.noiseChkCnt;
            if(errLoginCnt<6){
                Log.e(LOG_TAG2, "패치 로드를 실패하여 jar 로직을 수행합니다. " + e.getMessage());
                e.printStackTrace();
                errLoginCnt++;
            }
        }

        if (allFHAndDB != null && tmpMaxDbVal > 40) {
            //Log.v(LOG_TAG3, (calcTime(times) + " " + hz + " " + db + " " + amp + " " + decibel + ", 100db: " + tmpMaxDb + "db, max: " + SleepCheck.getMaxDB() + ", min: " + SleepCheck.getMinDB() + " " + SleepCheck.noiseChkSum + " " + SleepCheck.noiseChkCnt));
            Log.v(LOG_TAG3, (calcTime(times) + " " + hz + " " + db + " " + amp + " " + decibel + ", 100db: " + tmpMaxDbVal + "db, max: " + noiseChkSumVal + " " + noiseChkCntVal ));
        }
        try {
            snoringCheck.invoke(SleepCheck, allFHAndDB, decibel, times, snoringTermList, grindingTermList, maxARD);
            if ((Integer) CHECKED_STATUS.get(SleepCheck) == (Integer) CHECKED_ERROR.get(SleepCheck)) { //발생하지 않을 것 같지만 아주 만약을 위해 0 리턴하는 방어코드를 삽입하였다.
                continue;
            } else if ((Integer) CHECKED_STATUS.get(SleepCheck) == (Integer) allFHAndDb_NEED_INITIALIZE.get(SleepCheck)) { //allFHAndDB가 초기화 되어야 한다.
                allFHAndDB = null;
            }
            //SleepCheck.osaCheck(decibel, times, osaTermList, snoringTermList, noiseTermListForOsaList);
            osaCheck.invoke(SleepCheck, decibel, times, osaTermList, snoringTermList, noiseTermListForOsaList);
            if ((Integer) CHECKED_STATUS.get(SleepCheck) == (Integer) CHECKED_ERROR.get(SleepCheck)) { //발생하지 않을 것 같지만 아주 만약을 위해 0 리턴하는 방어코드를 삽입하였다.
                continue;
            }
            //SleepCheck.someNoiseCheck(times, amplitude, noiseTermListForOsaList);
            someNoiseCheck.invoke(SleepCheck, times, amplitude, noiseTermListForOsaList);
            if ((Integer) CHECKED_STATUS.get(SleepCheck) == (Integer) CHECKED_ERROR.get(SleepCheck)) { //발생하지 않을 것 같지만 아주 만약을 위해 0 리턴하는 방어코드를 삽입하였다.
                continue;
            }
        }catch(Exception e){
            kr.co.dwebss.soundanalysis.SleepCheck.snoringCheck(allFHAndDB, decibel, times, snoringTermList, grindingTermList, maxARD);
            if (kr.co.dwebss.soundanalysis.SleepCheck.CHECKED_STATUS == kr.co.dwebss.soundanalysis.SleepCheck.CHECKED_ERROR) { //발생하지 않을 것 같지만 아주 만약을 위해 0 리턴하는 방어코드를 삽입하였다.
                continue;
            } else if (kr.co.dwebss.soundanalysis.SleepCheck.CHECKED_STATUS == kr.co.dwebss.soundanalysis.SleepCheck.allFHAndDb_NEED_INITIALIZE) { //allFHAndDB가 초기화 되어야 한다.
                allFHAndDB = null;
            }
            kr.co.dwebss.soundanalysis.SleepCheck.osaCheck(decibel, times, osaTermList, snoringTermList, noiseTermListForOsaList);
            if (kr.co.dwebss.soundanalysis.SleepCheck.CHECKED_STATUS == kr.co.dwebss.soundanalysis.SleepCheck.CHECKED_ERROR) { //발생하지 않을 것 같지만 아주 만약을 위해 0 리턴하는 방어코드를 삽입하였다.
                continue;
            }
            kr.co.dwebss.soundanalysis.SleepCheck.someNoiseCheck(times, amplitude, noiseTermListForOsaList);
            if (kr.co.dwebss.soundanalysis.SleepCheck.CHECKED_STATUS == kr.co.dwebss.soundanalysis.SleepCheck.CHECKED_ERROR) { //발생하지 않을 것 같지만 아주 만약을 위해 0 리턴하는 방어코드를 삽입하였다.
                continue;
            }
            if(errLoginCnt<6){
                Log.e(LOG_TAG2, "패치 로드를 실패하여 jar 로직을 수행합니다. " + e.getMessage());
                e.printStackTrace();
                errLoginCnt++;
            }
        }

        if (maxARD != null) {
            if (decibel > maxARD.getDecibel()) {
                maxARD = new AnalysisRawData(times, amplitude, tmpMaxDbVal, frequency);
            }
        } else {
            maxARD = new AnalysisRawData(times, amplitude, tmpMaxDbVal, frequency);
            timesForMaxArd = Math.floor(times);
        }
        if (Math.floor(times) > timesForMaxArd) {
            //코골이 기록용 vo 생성
            if (maxARD.getDecibel() == 0) {
                maxARD.setDecibel(tmpMaxDbVal);
            }
            //System.out.println(calcTime(times)+" "+snoringTermList.size()+" "+SleepCheck.isOSATerm+" "+SleepCheck.isBreathTerm+" "+SleepCheck.isOSAAnsStart);
            if (snoringTermList.size() > 0 && isRecording == true) {
                if (snoringTermList.get(snoringTermList.size() - 1).end != 0) {
                    if (snoringTermList.get(snoringTermList.size() - 1).end > times) {
                        snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                    }
                } else {
                    snoringTermList.get(snoringTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                }
            }
            if (osaTermList.size() > 0 && isRecording == true && isOSATermTimeOccur) {
                if (osaTermList.get(osaTermList.size() - 1).end != 0) {
                    if (osaTermList.get(osaTermList.size() - 1).end > times) {
                        osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                    }
                } else {
                    osaTermList.get(osaTermList.size() - 1).AnalysisRawDataList.add(maxARD);
                }
            }
            if (isRecording == true) {
                //AllAnalysisRawDataList.add(maxARD);
                int tmpTime = (int) Math.floor(times);
                //1초 혹은 1분 단위로 기록
                if (tmpTime < 31 || AllAnalysisRawDataList.size() < 31) {
                    Log.v(LOG_TAG2, (calcTime(times)) + " 1번");
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
                if (tmpTime > 30 && AllAnalysisRawDataList.size() > 0) { //녹음이 새로 시작할 때 리스트가 0인 경우라면 오류가 발생한다. 앞에서 리스트가 31 미만이면 리스트를 추가하게 되어있으므로 size가 0인 경우는 없음.

                    double tmpCM = (times + (int) (recordFragment.getRecordStartDtl() / 1000) % 60);
                    double tmpBeforeCM = (AllAnalysisRawDataList.get(AllAnalysisRawDataList.size() - 1).getTimes() + (int) (recordFragment.getRecordStartDtl() / 1000) % 60);
                    int tmpM = calcMinute(tmpCM);
                    int tmpBeforeM = calcMinute(tmpBeforeCM);
                    //Log.v(LOG_TAG2,(calcTime(times)+" "+tmpCM+" "+tmpBeforeCM+" "+tmpM+" "+tmpBeforeM));
                    if (tmpM != tmpBeforeM) {
                        Log.v(LOG_TAG2, (calcTime(times)) + " 3번 " + tmpTime);
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
            maxARD = new AnalysisRawData(times, amplitude, tmpMaxDbVal, frequency);
            timesForMaxArd = Math.floor(times);

            try {
                tmpMaxDb.set(SleepCheck, 0);
                tmpMinDb.set(SleepCheck, 99999);
            }catch(Exception e){
                kr.co.dwebss.soundanalysis.SleepCheck.tmpMaxDb = 0;
                kr.co.dwebss.soundanalysis.SleepCheck.tmpMinDb = 99999;
                if(errLoginCnt<6){
                    Log.e(LOG_TAG2, "패치 로드를 실패하여 jar 로직을 수행합니다. " + e.getMessage());
                    e.printStackTrace();
                    errLoginCnt++;
                }
            }
        }
    }
}catch(Exception e){
e.printStackTrace();
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
                String andRList = gson.toJson(printAnalysisRawDataList(tmpSE));
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
                        String andRList = gson.toJson(printAnalysisRawDataList(se));
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
                        String andRList = gson.toJson(printAnalysisRawDataList(se));
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
                        String andRList = gson.toJson(printAnalysisRawDataList(se));
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
        Log.v(LOG_TAG2,( "무호흡 구간 시작=========="));
        Log.v(LOG_TAG2,( "무호흡" + osaTermList.size()+"회 발생 "));
        for(StartEnd se : osaTermList) {
            Log.v(LOG_TAG2,(se.getTerm()));
        }
        Log.v(LOG_TAG2,( "무호흡 구간 끝=========="));

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

    public JsonArray printAnalysisRawDataList(StartEnd startEnd) {
        JsonArray rtn = new JsonArray();
        if(startEnd.AnalysisRawDataList!=null) {
            for(AnalysisRawData d : startEnd.AnalysisRawDataList) {

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
