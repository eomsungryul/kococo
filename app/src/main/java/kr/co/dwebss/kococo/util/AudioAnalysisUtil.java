package kr.co.dwebss.kococo.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.dwebss.kococo.fragment.RecodeFragment;
import kr.co.dwebss.kococo.http.ApiService;
import kr.co.dwebss.kococo.model.StartEnd;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/*
 * 오디오 분석 모듈 코골이
 *
 * */
public class AudioAnalysisUtil {

    private String LOG_TAG = "AudioAnalysisUtil";

    //오디오 품질 고정값
    private int sampleRate = 44100;
    private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    //녹음 관련
    private String LOG_TAG2 = "AudioAnalysisUtil2";
    int state = 0;
    private boolean mShouldContinue = true;
    private AudioCalculator audioCalculator;
    int frameByteSize = 1024;
    static List<StartEnd> snoringTermList;
    public static List<StartEnd> osaTermList;
    static List<StartEnd> grindingTermList;
    byte[] audioData = new byte[frameByteSize];

    boolean isRecording = false;
    ByteArrayOutputStream baos;
    private static AudioRecord record;

    Retrofit retrofit;
    ApiService apiService;

    JsonObject recordData;
    String userAppId;
    String recordStartDt;
    String recordEndDt;
    SimpleDateFormat dayTimeDefalt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    Boolean recodeFlag = false;
    AudioAnalysisThread  aat;

    public void start(View v) {
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
        int result  = v.getContext().checkCallingOrSelfPermission(permission);
        record.startRecording();
        int recordingState = record.getRecordingState();
        Log.e(RecodeFragment.class.getSimpleName(), "RecordingState() after startRecording() = " + String.valueOf(recordingState));
        if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(RecodeFragment.class.getSimpleName(), "AudioRecord error has occured. Reopen app.");
            //System.exit(0);
        }
        Log.v(LOG_TAG, "Recording has started");
        mShouldContinue = true;
        recodeFlag = true;
        recordStartDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
//        Audio_Recording(v);
        state = 1;
//        Toast.makeText(getActivity(), "Started Recording", Toast.LENGTH_SHORT).show();

        aat= new AudioAnalysisThread();
        aat.start();
    }

    public JsonObject stop(View v) {
        state = 0;
        mShouldContinue = false;
        recodeFlag = false;
        recordEndDt= dayTimeDefalt.format(new Date(System.currentTimeMillis()));
        //녹음을 정지한다.
        record.stop();
        record.release();
        record = null;

        System.out.println(" ================JsonObject stop(View v)=======response: "+recordData);
        return recordData;
    }

    public void Audio_Recording(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long shortsRead = 0;

                byte[] frameBytes = new byte[frameByteSize];
                audioCalculator = new AudioCalculator();

                SleepCheckUtil.checkTerm = 0;
                SleepCheckUtil.checkTermSecond = 0;
                int osaCnt = 0;
                boolean grindingStart = false;
                boolean grindingContinue = false;
                int grindingRecordingContinueCnt = 0;
                boolean osaStart = false;
                boolean osaContinue = false;
                int osaRecordingExit = 0;
                int osaRecordingContinueCnt = 0;
                double osaStartTimes = 0.0;
                SleepCheckUtil.grindingContinueAmpCnt = 0;
                SleepCheckUtil.grindingContinueAmpOppCnt = 0;
                SleepCheckUtil.grindingRepeatAmpCnt = 0;
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
                        SleepCheckUtil.curTermSecond = (int) Math.floor(times);

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
                        if (decibel > SleepCheckUtil.NOISE_DB_INIT_VALUE && isRecording == false
                                && Math.floor((double) (audioData.length / (44100d * 16 * 1)) * 8) != Math.floor(times) //사운드 파일 테스트용
                        ) {
                            Log.v(LOG_TAG2,("녹음 시작! "));
                            Log.v(LOG_TAG2,(String.format("%.2f", times)+"s~"));
                            recordStartingTIme = System.currentTimeMillis();
                            baos = new ByteArrayOutputStream();
                            isRecording = true;
                        } else if (isRecording == true && (SleepCheckUtil.noiseCheck(decibel)==0 || recodeFlag==false) ) {
                            Log.v(LOG_TAG2,("녹음 종료! "));
                            Log.v(LOG_TAG2,(String.format("%.2f", times)+"s "));

                            SimpleDateFormat dayTime = new SimpleDateFormat("yyyymmdd_hhmm");
                            String fileName = dayTime.format(new Date(recordStartingTIme));
                            dayTime = new SimpleDateFormat("dd_hhmm");
                            long time = System.currentTimeMillis();
                            fileName += "-" + dayTime.format(new Date(time));
                            byte[] waveData = baos.toByteArray();

                            //녹음된 파일이 저장되는 시점
                            WaveFormatConverter wfc = new WaveFormatConverter(44100, (short)1, waveData, 0, waveData.length-1);
                            String[] filePath = wfc.saveLongTermWave(fileName, v.getContext());

                            Log.v(LOG_TAG2,("=====녹음중 분석 종료, 분석정보 시작====="));
                            Log.v(LOG_TAG2,("녹음파일 길이(s): " + ((double) (waveData.length / (44100d * 16 * 1))) * 8));

                            JsonObject ans = new JsonObject();
                            //ans.setAnalysisStartDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(recordStartingTIme), ZoneId.systemDefault()));
                            //ans.setAnalysisEndDt(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
                            ans.addProperty("analysisStartDt",dayTimeDefalt.format(new Date(recordStartingTIme)));
                            ans.addProperty("analysisEndDt",dayTimeDefalt.format(new Date(time)));
                            ans.addProperty("analysisFileAppPath",filePath[0]);
                            ans.addProperty("analysisFileNm",filePath[1]);
                            JsonArray ansDList = new JsonArray();
                            JsonObject ansd = new JsonObject();
                            for(StartEnd se : snoringTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200101);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.getStart()*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.getEnd()*1000))));
                                ansDList.add(ansd);
                            }
                            for(StartEnd se : grindingTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200102);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.getStart()*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.getEnd()*1000))));
                                ansDList.add(ansd);
                            }
                            for(StartEnd se : osaTermList) {
                                ansd = new JsonObject();
                                ansd.addProperty("termTypeCd",200103);
                                ansd.addProperty("termStartDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.getStart()*1000))));
                                ansd.addProperty("termEndDt",dayTimeDefalt.format(new Date((long) (recordStartingTIme+se.getEnd()*1000))));
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

//                            System.out.println(" ========================recordData: "+recordData.toString());
//                            RequestBody requestData = RequestBody.create(MediaType.parse("application/json"), new Gson().toJson(recordData));
//                            //POST /api/record를 호출한다.
//                            apiService.addRecord(requestData).enqueue(new Callback<RequestBody>() {
//                                @Override
//                                public void onResponse(Call<RequestBody> call, Response<RequestBody> response) {
//                                    System.out.println(" ========================response: "+response.body().toString());
//                                }
//
//                                @Override
//                                public void onFailure(Call<RequestBody> call, Throwable t) {
//
//                                }
//                            });

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
                        int snoreChecked = SleepCheckUtil.snoringCheck(decibel, frequency, sefrequency);
                        if(snoreChecked==1) {
                            if(snoringTermList.size()>0) {
                                double beforeTime = snoringTermList.get(snoringTermList.size()-1).getStart();
                                if(Math.floor(beforeTime)+100<Math.floor(times)) {
                                    snoringTermList.add(new StartEnd());
                                    snoringTermList.get(snoringTermList.size()-1).setStart(times);
                                    snoringTermList.get(snoringTermList.size()-1).setEnd(times);
                                }
                            }else {
                                snoringTermList.add(new StartEnd());
                                snoringTermList.get(0).setStart(times);
                                snoringTermList.get(0).setEnd(times);
                            }
                        }
                        // 이갈이는 기존 로직대로 체크해서, 어디 구간에서 발생했는지 체크한다.
                        SleepCheckUtil.grindingCheck(times, decibel, sefamplitude, frequency, sefrequency);
                        // 이갈이 신호가 발생하고, 이갈이 체크 상태가 아니면 이갈이 체크를 시작한다.
                        if (SleepCheckUtil.grindingRepeatAmpCnt == 1 && grindingStart == false) {
							/*
							System.out.print("이갈이 체크를 시작한다.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
									+ "s " + SleepCheckUtil.grindingContinueAmpCnt + " "
									+ SleepCheckUtil.grindingContinueAmpOppCnt + " " + SleepCheckUtil.grindingRepeatAmpCnt);
							*/
                            grindingTermList.add(new StartEnd());
                            grindingTermList.get(grindingTermList.size()-1).setStart(times);
                            grindingStart = true;
                            grindingContinue = false;
                            // 이갈이 체크 중에 1초간격으로 유효 카운트가 연속적으로 발생했으면 계속 체크한다.
                        } else if (SleepCheckUtil.curTermSecond - SleepCheckUtil.checkTermSecond == 1
                                && SleepCheckUtil.grindingRepeatAmpCnt >= 3 && grindingStart == true) {
                            if (((double) (audioData.length / (44100d * 16 * 1))) * 8 < times + 1) {
								/*
								System.out.print("이갈이 종료.");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
										+ "s " + SleepCheckUtil.grindingContinueAmpCnt + " "
										+ SleepCheckUtil.grindingContinueAmpOppCnt + " " + SleepCheckUtil.grindingRepeatAmpCnt);
								*/
                                grindingTermList.get(grindingTermList.size()-1).setEnd(times);
                                grindingStart = false;
                                grindingContinue = false;
                                grindingRecordingContinueCnt = 0;
                            }
							/*
							System.out.print("이갈이 중.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
									+ "s " + SleepCheckUtil.grindingContinueAmpCnt + " "
									+ SleepCheckUtil.grindingContinueAmpOppCnt + " " + SleepCheckUtil.grindingRepeatAmpCnt);
							*/
                            grindingRecordingContinueCnt = 0;
                            grindingContinue = true;
                            // 이갈이 체크 중에 1초간격으로 유효 카운트가 연속적으로 발생하지 않으면 체크를 취소한다.
                        } else if (SleepCheckUtil.curTermSecond - SleepCheckUtil.checkTermSecond == 1
                                && SleepCheckUtil.grindingRepeatAmpCnt == 0 && grindingStart == true
                                && grindingContinue == false) {
                            // 1초 단위 발생하는 이갈이도 잡기위해 유예 카운트를 넣는다. 1초만 한번더 체크함.
                            if (grindingRecordingContinueCnt >= SleepCheckUtil.GRINDING_RECORDING_CONTINUE_CNT) {
								/*
								System.out.print("이갈이 아님, 체크 취소.");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
										+ "s " + SleepCheckUtil.grindingContinueAmpCnt + " "
										+ SleepCheckUtil.grindingContinueAmpOppCnt + " " + SleepCheckUtil.grindingRepeatAmpCnt);
								*/
                                grindingTermList.remove(grindingTermList.size()-1);
                                grindingStart = false;
                                grindingRecordingContinueCnt = 0;
                            } else {
								/*
								System.out.print("이갈이 체크를 취소하지 않고 진행한다.(1초 유예)");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
										+ "s " + SleepCheckUtil.grindingContinueAmpCnt + " "
										+ SleepCheckUtil.grindingContinueAmpOppCnt + " " + SleepCheckUtil.grindingRepeatAmpCnt);
								*/
                                grindingRecordingContinueCnt++;
                            }
                            // 이갈이 체크 중에 1초간격으로 유효카운트가 더이상 발생하지 않으나 이전에 발생했더라면 현재 체크하는 이갈이는 유효함.
                        } else if (SleepCheckUtil.curTermSecond - SleepCheckUtil.checkTermSecond == 1
                                && SleepCheckUtil.grindingRepeatAmpCnt == 0 && grindingContinue == true) {
							/*
							System.out.print("이갈이 종료.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "~" + String.format("%.2f", times + 1)
									+ "s " + SleepCheckUtil.grindingContinueAmpCnt + " "
									+ SleepCheckUtil.grindingContinueAmpOppCnt + " " + SleepCheckUtil.grindingRepeatAmpCnt);
							*/
                            grindingTermList.get(grindingTermList.size()-1).setEnd(times);
                            grindingStart = false;
                            grindingContinue = false;
                            grindingRecordingContinueCnt = 0;
                        } else if (SleepCheckUtil.curTermSecond - SleepCheckUtil.checkTermSecond == 1) {
                            if (grindingStart) {
								/*
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "s 이갈이 중 " + grindingStart + " "
										+ grindingContinue + " " + grindingRecordingContinueCnt);
								*/
                            }
                        }
                        // 무호흡도 기존 로직대로 체크해서, 어디 구간에서 발생했는지 체크한다.
                        osaCnt = SleepCheckUtil.OSACheck(times, decibel, sefamplitude, frequency, sefrequency);
                        osaRecordingContinueCnt += osaCnt;
                        // 무호흡 카운트가 발생하고, 체크 상태가 아니면 체크를 시작한다.
                        if (osaRecordingExit > 0) {
                            osaRecordingExit--;
                        }
                        if (osaCnt > 0 && osaStart == false) {
							/*
							System.out.print("무호흡 체크를 시작한다.");
							Log.v(LOG_TAG2,(String.format("%.2f", times) + "s~" + SleepCheckUtil.isOSATerm + " "
									+ SleepCheckUtil.isBreathTerm + " " + SleepCheckUtil.isOSATermCnt);
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
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheckUtil.isOSATerm + " "
										+ SleepCheckUtil.isBreathTerm + " " + SleepCheckUtil.isOSATermCnt);
								*/
                                osaStart = false;
                                osaRecordingContinueCnt = 0;
                            } else {
                                if (((double) (audioData.length / (44100d * 16 * 1))) * 8 < times + 1) {
									/*
									System.out.print("무호흡 끝.");
									Log.v(LOG_TAG2,(
											String.format("%.2f", times) + "~" + String.format("%.2f", times + 1) + "s "
													+ SleepCheckUtil.grindingContinueAmpCnt + " "
													+ SleepCheckUtil.grindingContinueAmpOppCnt + " "
													+ SleepCheckUtil.grindingRepeatAmpCnt);
									*/
                                    osaStart = false;
                                    osaRecordingContinueCnt = 0;
                                }
                                osaContinue = true;
								/*
								System.out.print("무호흡 중.");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheckUtil.isOSATerm + " "
										+ SleepCheckUtil.isBreathTerm + " " + SleepCheckUtil.isOSATermCnt);
								*/
                            }
                            // 무호흡 녹음 중 5초 이 후에 소리가 발생하면, 다음 소리가 발생한 구간까지 체크한다.
                        } else if (times - osaStartTimes > 5 && osaStart == true) {
                            if (SleepCheckUtil.isBreathTerm == true) { // 숨쉬는 구간이 되었으면, 체크 계속 플래그를 업데이트
                                if (((double) (audioData.length / (44100d * 16 * 1))) * 8 < times + 1) {
									/*
									System.out.print("무호흡 끝.");
									Log.v(LOG_TAG2,(
											String.format("%.2f", times) + "~" + String.format("%.2f", times + 1) + "s "
													+ SleepCheckUtil.grindingContinueAmpCnt + " "
													+ SleepCheckUtil.grindingContinueAmpOppCnt + " "
													+ SleepCheckUtil.grindingRepeatAmpCnt);
									*/
                                    osaStart = false;
                                    osaRecordingContinueCnt = 0;
                                }
                                osaContinue = true;
								/*
								System.out.print("무호흡 중.2 ");
								Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheckUtil.isOSATerm + " "
										+ SleepCheckUtil.isBreathTerm + " " + SleepCheckUtil.isOSATermCnt);
								*/
                            } else {
                                if (osaContinue == true && osaRecordingExit == 1) {
									/*
									System.out.print("무호흡 끝.");
									Log.v(LOG_TAG2,(String.format("%.2f", times) + "~"
											+ String.format("%.2f", times + 0.01) + "s " + SleepCheckUtil.isOSATerm + " "
											+ SleepCheckUtil.isBreathTerm + " " + SleepCheckUtil.isOSATermCnt);
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
										+ String.format("%.2f", times + 0.01) + "s " + SleepCheckUtil.isOSATerm + " "
										+ SleepCheckUtil.isBreathTerm + " " + SleepCheckUtil.isOSATermCnt);
								*/
                            }
                        }
                        SleepCheckUtil.curTermTime = times;
                        SleepCheckUtil.curTermDb = decibel;
                        SleepCheckUtil.curTermAmp = amplitude;
                        SleepCheckUtil.curTermHz = frequency;
                        SleepCheckUtil.curTermSecondHz = sefrequency;

                        SleepCheckUtil.checkTerm++;
                        SleepCheckUtil.checkTermSecond = (int) Math.floor(times);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }
                //Log.v(LOG_TAG2,("audio length(s): " + ((double) (audioData.length / (44100d * 16 * 1))) * 8));
                Log.v(LOG_TAG2,("audio length(s): " + String.format("%.2f", times)));

                Log.v(LOG_TAG2,( "코골이 여부 " + SleepCheckUtil.snoringContinue));
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
