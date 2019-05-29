package kr.co.dwebss.kococo.fragment;

import android.util.Log;

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
	static int AVR_DB_CHECK_TERM = 300;
	static int AVR_DB_INIT_VALUE = -100;
	static int NOISE_DB_INIT_VALUE = -10;
	static int NOISE_DB_CHECK_TERM = 1*100*60;

	static int noiseChkSum = 0;
	static int noiseNoneChkSum = 0;
	static int noiseChkCnt = 0;

	static double GrindingCheckTermSecond = 0;
	static double GrindingCheckStartTermSecond = 0;
	static double GrindingCheckStartTermDecibel = 0;

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

	static int noiseCheck(double decibel) {
		if(noiseChkCnt>=100) {
			int tmpN = noiseChkCnt;
			noiseChkSum = 0;
			noiseNoneChkSum = 0;
			return tmpN;
		}else {
			if(decibel > NOISE_DB_INIT_VALUE) {
				noiseChkSum++;
			}else {
				noiseNoneChkSum++;
			}
			noiseChkCnt++;
			return 101;
		}

	}

	static int snoringCheck(double decibel, double frequency, double sefrequency) {
		if (
				decibel > getAvrDB() &&
				frequency >= 150 && frequency <= 250 && sefrequency >= 950 && sefrequency < 1050
		) {
			snoringContinue++;
			return 1;
		} else {
			snoringContinueOpp++;
		}
		return 0;
	}

	static int grindingCheck(double times, double decibel, int amplitude, double frequency, double sefrequency) {
		if (decibel > grindChkDb
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
		if (decibel > chkOSADb) {

			if (isOSATerm == true) {
				if (beforeTermWord.equals(BREATH) && isOSATermCnt > 3000) { //5초에서 30초로 변경
					Log.v("YRSEO",("[" + String.format("%.2f", OSAcurTermTime) + "~" + String.format("%.2f", times)
							+ "s, isOSATermCnt: " + isOSATermCnt + ", isOSATermCntOpp:" + isOSATermCntOpp + "]"));

					RecodeFragment.osaTermList.add(new StartEnd());
					RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-1).start=OSAcurTermTime;
					RecodeFragment.osaTermList.get(RecodeFragment.osaTermList.size()-1).end=times;
					double tmpD = OSAcurTermTime;
					OSAcurTermTime = times;
					isOSATerm = false;
					isOSATermCnt = 0;
					isOSATermCntOpp = 0;
					return (int) (times-tmpD);
				} else {
					isOSATerm = false;
					isOSATermCnt = 0;
					isOSATermCntOpp = 0;
				}
			} else {
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
				OSAcurTermTime = times;
				isBreathTermCnt = 0;
				isBreathTermCntOpp = 0;
				isBreathTerm = false;
				beforeTermWord = BREATH;
			}
		}
		if (OSAcurTermTime == 0 || (isBreathTermCnt == 0 && isOSATermCnt == 0)) {
			OSAcurTermTime = times;
		}
		return 0;
	}
}
