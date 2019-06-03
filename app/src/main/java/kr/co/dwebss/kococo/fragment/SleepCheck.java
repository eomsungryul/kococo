package kr.co.dwebss.kococo.fragment;

import android.util.Log;

import java.util.ArrayList;

import kr.co.dwebss.kococo.model.AnalysisRawData;

public class SleepCheck {

	static double curTermHz = 0.0;
	static double curTermSecondHz = 0.0;
	static double curTermTime = 0.0;
	static double OSAcurTermTime = 0.0;
	static double curTermDb = 0.0;
	static int curTermAmp = 0;
	static double grindChkDb = -10;

	static double chkOSADb = -9;
	static boolean isBreathTerm = false;
	static boolean isOSATerm = false;
	static int isBreathTermCnt = 0;
	static int isBreathTermCntOpp = 0;
	static int isOSATermCnt = 0;
	static int isOSATermCntOpp = 0;
	static String beforeTermWord = "";
	static String BREATH = "breath";
	static String OSA = "osa";

	static int checkTerm = 0; // 1�� 0.01��
	static int grindingRepeatOnceAmpCnt = 0;
	static int grindingRepeatAmpCnt = 0;
	static int grindingContinueAmpCnt = 0;
	static int grindingContinueAmpOppCnt = 0;

	static int snoringCheckCnt = 0;
	static int snoringContinue = 0;
	static int snoringContinueOpp = 0;

	static int checkTermSecond = 0;
	static int curTermSecond = 0;

	static int GRINDING_RECORDING_CONTINUE_CNT = 1;

	static int decibelSum = 0;
	static int decibelSumCnt = 0;

	static int EXCEPTION_DB_FOR_AVR_DB = -10;
    static int AVR_DB_CHECK_TERM = 6000;
    static double MAX_DB_CRIT_VALUE = -31.5;
	static double MIN_DB_CRIT_VALUE = -20;
	static int NOISE_DB_INIT_VALUE = -10;
	static int NOISE_DB_CHECK_TERM = 1*100*60;

	static int noiseChkSum = 0;
	static int noiseNoneChkSum = 0;
	static int noiseChkCnt = 0;

	static double GrindingCheckTermSecond = 0;
	static double GrindingCheckStartTermSecond = 0;
	static double GrindingCheckStartTermDecibel = 0;

    static double MAX_DB = -31.5;
    static double MIN_DB = 0;
	static boolean isSnoringStart = false;

    static boolean isOSATermTimeOccur = false;
    static boolean isOSAAnsStart = false;

	/*
	static double getAvrDB(double decibel) {
		double avrDB = -AVR_DB_INIT_VALUE;
		if (decibelSumCnt >= AVR_DB_CHECK_TERM || decibelSumCnt == 0) {
			decibelSum = 0;
			decibelSumCnt = 0;
		}
		if (decibel < EXCEPTION_DB_FOR_AVR_DB) {
			decibelSum += decibel;
			decibelSumCnt++;
		}
		if (decibelSum != 0 && decibelSumCnt != 0) {
			avrDB = decibelSum / decibelSumCnt;
		}
		return avrDB;
	}

	static double getAvrDB() {
		double avrDB = -AVR_DB_INIT_VALUE;
		if (decibelSumCnt >= AVR_DB_CHECK_TERM || decibelSumCnt == 0) {
			decibelSum = 0;
			decibelSumCnt = 0;
		}
		if (decibelSum != 0 && decibelSumCnt != 0) {
			avrDB = decibelSum / decibelSumCnt;
		}
		return avrDB;
	}
*/

    static double getMinDB() {
		/*
		double avrDB = -AVR_DB_INIT_VALUE;
		if (decibelSum != 0 && decibelSumCnt != 0) {
			avrDB = decibelSum / decibelSumCnt;
		}
		//System.out.print(decibelSum+" "+decibelSumCnt+" "+avrDB+" ");
		*/
        return MIN_DB/2 < MIN_DB_CRIT_VALUE ? Math.floor(MIN_DB_CRIT_VALUE) : MIN_DB/2;
    }

    static double setMinDB(double decibel) {
        //10분마다 평균 데시벨을 다시 계산한다.
        if(Math.abs(decibel) != 0 && Math.abs(decibel) != 31.5 && decibel < MIN_DB) {
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
        return MIN_DB/2 < MIN_DB_CRIT_VALUE ? Math.floor(MIN_DB_CRIT_VALUE) : MIN_DB/2;
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
		//0.01초 단위임으로, 600번 해야 60초임.
		//1분이 되었으면, 데시벨보다 높은 소리가 발생하지 않은 경우
		if (noiseChkCnt >= 600) {
			int tmpN = noiseChkSum;
			noiseChkCnt = 0;
			noiseChkSum = 0;
			noiseNoneChkSum = 0;
			return tmpN;
		} else {
			//아직 1분이 안되었으면 계속 소리 체크를 한다.
			//소리 체크는 1분동안 평균 데시벨보다 높은 데시벨의 소리가 발생했는지를 체크한다.
			//리턴이 0이면 녹음 종료하게 되어있음.
			if (decibel >= getMaxDB()) {
				//noiseChkCnt++;
				noiseChkSum++;
			} else {
				noiseNoneChkSum++;
			}
			noiseChkCnt++;
			//return 101;
			return noiseChkCnt;
		}
	}

	static int snoringCheck(double decibel, double frequency, double sefrequency) {
		if (
				//decibel > getAvrDB() &&
				frequency >= 150 && frequency <= 250 && sefrequency >= 950 && sefrequency < 1050
		) {
			snoringContinue++;
		} else {
			snoringContinueOpp++;
		}
		if(snoringContinue+snoringContinueOpp>6000) {
			//1분동안 주파수 탐지 횟수가 1~2번 혹은 10~15(앞은 무호흡, 뒤는 일본 코골이)인 경우 코골이를 했다고 판단.
			if((snoringContinue <= 2 && snoringContinue >=1 )|| (snoringContinue >= 10 && snoringContinue <= 15)) {
				return 2;
			}else {
				snoringContinue = 0;
				snoringContinueOpp = 0;
				return 3;
			}
		}
		return 0;
	}

	static int grindingCheck(double times, double decibel, int amplitude, double frequency, double sefrequency) {
        if (decibel > getMinDB()*0.55
        ){
			if(grindingRepeatOnceAmpCnt==0) {
				GrindingCheckStartTermDecibel = decibel;
			}else {
				GrindingCheckStartTermDecibel = (GrindingCheckStartTermDecibel+decibel) / grindingRepeatOnceAmpCnt;
			}
			if(grindingRepeatOnceAmpCnt>=2) {
				if(decibel > grindingRepeatOnceAmpCnt) {
					grindingContinueAmpOppCnt++;
				}else {
					grindingRepeatOnceAmpCnt++;
				}
			}else {
				grindingRepeatOnceAmpCnt++;
			}
		} else {
			if (grindingRepeatOnceAmpCnt <= 4 && grindingRepeatOnceAmpCnt>=2) {
				if(grindingContinueAmpCnt == 0) {
					GrindingCheckStartTermSecond = times;
				}
				grindingContinueAmpCnt++;
				//System.out.println(String.format("%.2f", times) + "s " + frequency + " " + decibel + " " + amplitude + " " + sefrequency + " " +grindingContinueAmpCnt);
			}
			grindingContinueAmpOppCnt++;
			grindingRepeatOnceAmpCnt = 0;
		}

		if (Math.floor((GrindingCheckTermSecond - GrindingCheckStartTermSecond)*100) == 101) {
			//System.out.println(curTermSecond + "~"+checkTermSecond+"s, grindingContinueAmpCnt:"+grindingContinueAmpCnt+", grindingContinueAmpOppCnt:"+grindingContinueAmpOppCnt+", grindingRepeatAmpCnt:"+grindingRepeatAmpCnt);
			if(grindingContinueAmpCnt >= 3
					&& grindingContinueAmpCnt <=15
					&& grindingContinueAmpOppCnt >= 50
			) {
				grindingRepeatAmpCnt++;
				System.out.println(curTermSecond + " "+checkTermSecond+" "+grindingContinueAmpCnt+" "+grindingContinueAmpOppCnt+" "+grindingRepeatAmpCnt);
			}else {
				grindingRepeatAmpCnt = 0;
				//System.out.println("여기8");
			}
			grindingContinueAmpCnt = 0;
			grindingContinueAmpOppCnt = 0;
		}

		return 0;
	}

	static int OSACheck(double times, double decibel, int amplitude, double frequency, double sefrequency) {
        //System.out.println("OSACheckDb:" +decibel +"vs" + getMinDB());
        if (decibel > getMinDB()*0.45) {

            if (isOSATerm == true) {
                if (beforeTermWord.equals(BREATH) && isOSATermCnt > 1000) {
                    isOSATermTimeOccur=false;
                    Log.v("YRSEO",("[" + String.format("%.2f", OSAcurTermTime) + "~" + String.format("%.2f", times)
							+ "s, isOSATermCnt: " + isOSATermCnt + ", isOSATermCntOpp:" + isOSATermCntOpp + "]"));
//분석이 종료되는 시점은 앞에서 분석된 시간으로부터 1분이상 초과된 경우에 종료하고, 아닌 경우에는 현재 시간을 end.times에 추가한다.
                    if(RecodeFragment.osaTermList.size()>1) {
                        int beforeEndTime = (int) RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-2).end; //0이거나 값이 있거나
                        int currentTime = (int) times;
                        //System.out.println(beforeEndTime +" "+ currentTime+"="+(currentTime-beforeEndTime));
                        if(currentTime - beforeEndTime > 60) {
                            System.out.println("기록vo종료");
                            isOSAAnsStart = false;
                            RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-1).end=times;
                        }else {
                            //System.out.println("1분이 안 지났으므로, 기록vo종료하지 않고, 이전기록vo에 종료입력, 현재 기록vo 삭제");
                            RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-2).AnalysisRawDataList.addAll(
                                    RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-1).AnalysisRawDataList);
                            RecodeFragment.osaTermList.remove(RecodeFragment.osaTermList.size()-1);
                            RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-1).end=times;
                        }
                    }else {
                        isOSAAnsStart = false;
                        RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-1).end=times;
                    }

                    double tmpD = OSAcurTermTime;
                    OSAcurTermTime = times;
                    isOSATerm = false;
                    isOSATermCnt = 0;
                    isOSATermCntOpp = 0;
                    // beforeTermWord=OSA;
                    return (int) (times-tmpD);

				} else {
					isOSATerm = false;
					isOSATermCnt = 0;
					isOSATermCntOpp = 0;
				}
			} else {
                if(!isOSATermTimeOccur || (isOSATermTimeOccur && times-OSAcurTermTime>15)) {
                isOSAAnsStart = true;
                OSAcurTermTime = times;
                if(isOSATermTimeOccur && RecodeFragment.osaTermList.size()>0) {
                    //System.out.println("이전기록vo취소");
                    RecodeFragment.osaTermList.remove(RecodeFragment.osaTermList.size()-1);
                }
                //System.out.println("기록vo생성");
                RecodeFragment.osaTermList.add(new StartEnd());
                RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-1).start=times;
                RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size() - 1).AnalysisRawDataList = new ArrayList<AnalysisRawData>();
                RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(times, amplitude, decibel, frequency, sefrequency, 0));
                isOSATermTimeOccur = true;
                System.out.println(OSAcurTermTime);
            }

                isBreathTermCnt++;
                isBreathTerm = true;
			}
		} else {
			if (isBreathTerm == false) {
				isOSATermCnt++;
				isOSATerm = true;
			} else {
				isBreathTermCntOpp++;
				isOSATermCntOpp++;
			}
		}

		if (isBreathTermCntOpp > 20) {
			if (isBreathTermCnt < 70) {
				isBreathTermCnt = 0;
				isBreathTermCntOpp = 0;
				isBreathTerm = false;
			} else {
				//OSAcurTermTime = times;
				isBreathTermCnt = 0;
				isBreathTermCntOpp = 0;
				isBreathTerm = false;
				beforeTermWord = BREATH;
			}
		}
		if (OSAcurTermTime == 0 || (isBreathTermCnt == 0 && isOSATermCnt == 0)) {
			//OSAcurTermTime = times;
		}
		return 0;
	}
}
