package kr.co.dwebss.kococo.fragment;

import android.util.Log;

import java.text.DecimalFormat;
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

	static int checkTerm = 0; // 1당 0.01초
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

	static double decibelSum = 0;
	static double decibelSumCnt = 0;

	static int EXCEPTION_DB_FOR_AVR_DB = -10;
	static int AVR_DB_CHECK_TERM = 2000;
	static double MAX_DB_CRIT_VALUE = -31.5;
	static double MIN_DB_CRIT_VALUE = -(31.5-(31.5*35/100)); //http://www.noiseinfo.or.kr/about/info.jsp?pageNo=942 조용한 공원(수면에 거의 영향 없음) 35, 40부터 낮아진다
	static int NOISE_DB_INIT_VALUE = -10;
	static int NOISE_DB_CHECK_TERM = 1*100*60;

	static int noiseChkSum = 0;
	static int noiseNoneChkSum = 0;
	static int noiseChkCnt = 0;
	static int noiseChkForStartSum = 0;
	static int noiseNoneChkForStartSum = 0;
	static int noiseChkForStartCnt = 0;

	static double GrindingCheckTermSecond = 0;
	static double GrindingCheckStartTermSecond = 0;
	static double GrindingCheckStartTermDecibel = 0;
	static boolean GrindTermCheckBoolean = false;

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
	static int snoringCheckNew(String times, double decibel, double frequency, double sefrequency, double[] allFHAndDB) {

		//allFhAndDB에 담긴 값과 decibel 값이 서로 다른 라이브러리를 쓰고 있어, 단위가 맞지 않는다.
		//allFhAndDB의 범위는 알지 못하나 최대 81까지 최소는 -67 측정됨(, decibel ~31.5~0값임
		//allFhAndDB의 범위가 더 크니까 allFhAndDB 보정해서 차이를 구한다.
		DecimalFormat df = new DecimalFormat("0.00");
		double d1 = -(31.5-allFHAndDB[0]/255*31.5);
		System.out.print(times+"\t");
		System.out.print(decibel+"\t");
		System.out.print(frequency+"\t");
		//System.out.print(df.format(d1)+"\t");
		//System.out.print(df.format(allFHAndDB[0])+"\t");
		System.out.println(df.format(Math.abs(decibel-d1)));
		if(Math.abs(decibel-d1)>Math.abs(decibel)/2){
			return 1;
		}else {
			return 0;
		}
	}

	static int snoringCheck(double decibel, double frequency, double sefrequency) {
		System.out.println(decibel+" "+frequency);
		return 0;
	}
	static int grindingCheck(double times, double decibel, double frequency, double sefrequency) {
		// 이갈이, 이갈이는 높은 주파수가 굉장히 짧은 간격으로 여러번 나타난다.
		// 아래 1,2,3 무시, 다시-> 이갈이는 0.02~0.07초 사이의 큰 진폭을 갖는다. 즉, 0.01초 단위로 분석해서 연속으로 2~7회의
		// 진폭이 발생하는 것을 잡으면 됨.
		// 이 때, 진폭의 수치는 의미가 없을 수 있으므로 먼저 반복횟수로만 잡아본다.
		// 1. 특정 주파수보다 높은 주파수가 --> 특정주파수 a=?
		// 2. 특정 시간동안 특정 횟수보다 많이 반복되는지 --> 특정시간 b=?, 특정횟수 c=?
		// 3. 체크되면 이갈이다.

		// 특정 주파수대역에서 연속적으로 발생해야 한다.
		// 분석하는 소리의 길이는 0.01초.
		// 아래는 0.01초 데이터를 0.1초 단위로 분석X

		// 소리가 평균 크기보다 클 때 분석한다.
		// -> 이갈이는 소리 폭이 너무 커서 평균 측정 소리 때문에 측정이 안될 수 도 있다.-> 평균 소리보다 2배 이상 진폭이 크면 평균치에
		// 합산 안함.
		// if(amplitude > (maxAmp/sumCnt)) {
		// if(amplitude > 1000) {
		// 계속 발생되는 주파수의 0.1초간격으로 비교하여 비슷한 주파수 일때(+-50)가 연속되는 경우에 카운팅 한다.X
		// 연속으로 높게 발생되는 진폭이 0.01초 단위로 2~7회 연속되는지 체크

		// System.out.println(times+" - "+termTime+" =
		// "+String.valueOf(times-termTime));
		//System.out.println(String.format("%.3f", times)+" "+checkTermSecond+" "+curTermSecond);
		// 데시벨이 더 높고 주파수대역이 100의 자리에서 내림했을 때 동일하며, 0.02초 동안만 반복되어야 한다.(1번 반복)X
		//System.out.println("grindingChkDb:" +decibel +"vs" + getMinDB()*1.1+" ");
		if (decibel > getMinDB()*0.55
			// curTermDb >= decibel && // 비교기준이 되는 데시벨은 고점이어야 한다.X -> 고점에서 점차 데시벨이 내려오는것은 다른
			// 사운드와 동일한 특징이다. 비슷한 대역의 소리가 계속 발생하는것을 찾아야한다.
				/*&& (
		Math.abs(Math.abs(curTermDb) - Math.abs(decibel)) < 1 // 데시벨 오차 범위가 1db 이면 같은 이어지는 소리로 판단한다.
		//주파수가 같아야함.
		&& (
				// 100보다 큰 경우 십의 자리 까지 버리고 비교한다.
				(frequency > 100 && curTermHz > 100 && (int)curTermHz / 100 != (int)frequency / 100 )
				||
				// 100보다 작은 경우 십의 자리 까지 버리고 비교한다.
				(frequency < 10 && curTermHz < 10 && (int)curTermHz / 10 != (int)frequency / 10 )
				||
				// 100보다 큰 경우 십의 자리 까지 버리고 비교한다.
				(sefrequency > 100 && curTermSecondHz > 100 && (int)curTermSecondHz / 100 != (int)sefrequency / 100 )
				||
				// 100보다 작은 경우 십의 자리 까지 버리고 비교한다.
				(sefrequency < 10 && curTermSecondHz < 10 && (int)curTermSecondHz / 10 != (int)sefrequency / 10 )

				)
				)
				*/
		){
			//0.01초 단위로 연속으로 반복하는 여부 카운트 증가
			//매우 짧은 간격 으로 높은 데시벨이 연속 된다고 하고, 이 간격은 0.02초이며, 0.02초가 지나면, 체크중인 이갈이 평균 데시벨로 부터 2데시벨이 차이나면, 반대 카운트를 증가한다.
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
			//System.out.println(String.format("%.2f", times) + "s " + frequency + " " + decibel + " " + amplitude + " " + sefrequency + " " +grindingContinueAmpCnt);

			/*
			System.out.println("==========GRIND CHECK STA==============");
			System.out.println(String.format("%.2f", times) + "s " + frequency + " " + decibel + " " + amplitude + " " + sefrequency + " " );
			System.out.println( curTermDb + " " + Math.abs(decibel));

			System.out.println( (int)curTermHz / 100+ " " + (int)frequency / 100);
			System.out.println( (int)curTermHz%100 / 10+ " " + (int)frequency%100 / 10);
			System.out.println((frequency > 100 && curTermHz > 100 && (int)curTermHz / 100 != (int)frequency / 100 )
					||
					// 100보다 작은 경우 십의 자리 까지 버리고 비교한다.
					(frequency < 10 && curTermHz < 10 && (int)curTermHz / 10 != (int)frequency / 10 ));
			System.out.println( (int)curTermSecondHz / 100+ " " + (int)sefrequency / 100);
			System.out.println( (int)curTermSecondHz%100 / 10+ " " + (int)sefrequency%100 / 10);
			System.out.println((sefrequency > 100 && curTermSecondHz > 100 && (int)curTermSecondHz / 100 != (int)sefrequency / 100 )
					||
					// 100보다 작은 경우 십의 자리 까지 버리고 비교한다.
					(sefrequency < 10 && curTermSecondHz < 10 && (int)curTermSecondHz / 10 != (int)sefrequency / 10 ));
			System.out.println( (int)curTermAmp / 100+ " " + (int)amplitude / 100);
			System.out.println( (int)curTermAmp%100 / 10+ " " + (int)amplitude%100 / 10);
			System.out.println("==========GRIND CHECK END==============");
			*/
		} else {
			//신호가 바뀔 때 신호가 반복된 카운트가 1인 경우에만 유효카운트를 증가한다.
			//System.out.println(String.format("%.2f", times) + "s " + grindingRepeatOnceAmpCnt);
			//System.out.println(String.format("%.2f", times) + "s " + frequency + " " + decibel + " " + amplitude + " " + sefrequency + " " +grindingContinueAmpCnt);
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

		// 1. 진폭, 데시벨, 주파수를 이용해서 0.01초 단위로 1번만 발생하거나 2번 연속발생한 포먼트를 측정하고,
		// 2. 위 포먼트가 1초동안 얼만큼 발생했는지를 카운트를 체크, 기준치는 3으로 잡았다.
		// 3. 3번 이상 연속발생한 포먼트의 카운트를 체크, 기준치는 69로 잡았다.
		// -> 즉 1번의 포먼트가 1초동안 3번 발생했는지 체크, 동시에 3회이상연속한 포먼트 카운트가 69를 넘어야함. 이 현상이 수초(2초)
		// 동안 발생해야 함
		// 연속 카운트
		//System.out.println(String.format("%.3f", times)+" "+checkTermSecond+" "+curTermSecond+" "+grindingContinueAmpCnt+" "+grindingContinueAmpOppCnt);

		//if (checkTerm % 100 == 0) { //분석단위가 0.11, 0.12초라 100이라는 수치는 오차가 발생한다.
		//초를 계산해서 처리하도록 함.
		//System.out.println(curTermSecond + " "+checkTermSecond+" "+grindingContinueAmpCnt+" "+grindingContinueAmpOppCnt);
		if (Math.floor((GrindingCheckTermSecond - GrindingCheckStartTermSecond)*100) == 101) {
			//System.out.println(curTermSecond + "~"+checkTermSecond+"s, grindingContinueAmpCnt:"+grindingContinueAmpCnt+", grindingContinueAmpOppCnt:"+grindingContinueAmpOppCnt+", grindingRepeatAmpCnt:"+grindingRepeatAmpCnt);
			if(grindingContinueAmpCnt >= 3
					&& grindingContinueAmpCnt <=15
					&& grindingContinueAmpOppCnt >= 50
			) {
				grindingRepeatAmpCnt++;
				//System.out.println(curTermSecond + " "+checkTermSecond+" "+grindingContinueAmpCnt+" "+grindingContinueAmpOppCnt+" "+grindingRepeatAmpCnt);
			}else {
				grindingRepeatAmpCnt = 0;
				//System.out.println("여기8");
			}
			grindingContinueAmpCnt = 0;
			grindingContinueAmpOppCnt = 0;
		}

		//무조건 반환하고, 2~3초 이상 지속되는지는 여기서 판단하지 않는다.
		/*
		if(grindingRepeatAmpCnt>=2) {
			System.out.println(curTermSecond + " "+checkTermSecond+" "+grindingContinueAmpCnt+" "+grindingContinueAmpOppCnt+" "+grindingRepeatAmpCnt);
			grindingRepeatAmpCnt = 0;
			//grindingContinueAmpCnt=0;
			//grindingContinueAmpOppCnt = 0;
			return 1;
		}
		*/

		//1초 단위로 유효카운트가 몇회인지 체크한다. 유효카운트가 범위에 해당하면, 몇초동안 지속되는지 체크한다.
		/*
		if (checkTerm % 100 == 0) {
			if (//checkTerm % 100 == 0 &&
					grindingRepeatAmpCnt >= 2
					//&& grindingContinueAmpCnt >= 1 && grindingContinueAmpCnt <=5 && grindingContinueAmpOppCnt >= 60
					) {
				int tmpI = grindingRepeatAmpCnt;
				grindingRepeatAmpCnt=0;
				grindingRepeatOnceAmpCnt = 0;
				grindingContinueAmpCnt = 0;
				grindingContinueAmpOppCnt = 0;
				return tmpI;
			}
			grindingContinueAmpCnt = 0;
			grindingContinueAmpOppCnt = 0;
		}else {
			//System.out.println(String.format("%.2f", times) + "s "+checkTerm % 100+" "+grindingRepeatAmpCnt+" "+grindingContinueAmpCnt+" "+grindingContinueAmpOppCnt);
			if(grindingContinueAmpCnt >= 1
					&& grindingContinueAmpCnt <=6
					&& grindingContinueAmpOppCnt >= 70) {

				grindingRepeatAmpCnt++;
				grindingContinueAmpCnt = 0;
				grindingContinueAmpOppCnt = 0;
			}
		}
		*/
		return 0;
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
					System.out.println("![" + String.format("%.2f", OSAcurTermTime) + "~" + String.format("%.2f", times)
							+ "s, isOSATermCnt: " + isOSATermCnt + ", isOSATermCntOpp:" + isOSATermCntOpp + "]");
					//분석이 종료되는 시점은 앞에서 분석된 시간으로부터 1분이상 초과된 경우에 종료하고, 아닌 경우에는 현재 시간을 end.times에 추가한다.
					if(RecordFragment.osaTermList.size()>1) {
						int beforeEndTime = (int) RecordFragment.osaTermList.get(RecordFragment.osaTermList.size()-2).end; //0이거나 값이 있거나
						int currentTime = (int) times;
						System.out.println(beforeEndTime +" "+ currentTime+"="+(currentTime-beforeEndTime));
						if(currentTime - beforeEndTime > 60) {
							//System.out.println("기록vo종료");
							isOSAAnsStart = false;
							RecordFragment.osaTermList.get(RecordFragment.osaTermList.size()-1).end=times;
						}else {
							//System.out.println("1분이 안 지났으므로, 기록vo종료하지 않고, 이전기록vo에 종료입력, 현재 기록vo 삭제");
							RecordFragment.osaTermList.get(RecordFragment.osaTermList.size()-2).AnalysisRawDataList.addAll(
									RecordFragment.osaTermList.get(RecordFragment.osaTermList.size()-1).AnalysisRawDataList);
							RecordFragment.osaTermList.remove(RecordFragment.osaTermList.size()-1);
							RecordFragment.osaTermList.get(RecordFragment.osaTermList.size()-1).end=times;
						}
					}else {
						isOSAAnsStart = false;
						RecordFragment.osaTermList.get(RecordFragment.osaTermList.size()-1).end=times;
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
					if(isOSATermTimeOccur && RecordFragment.osaTermList.size()>0) {
						//System.out.println("이전기록vo취소");
						RecordFragment.osaTermList.remove(RecordFragment.osaTermList.size()-1);
					}
					//System.out.println("기록vo생성");
					RecordFragment.osaTermList.add(new StartEnd());
					RecordFragment.osaTermList.get(RecordFragment.osaTermList.size()-1).start=times;
					RecordFragment.osaTermList.get(RecordFragment.osaTermList.size() - 1).AnalysisRawDataList = new ArrayList<AnalysisRawData>();
					RecordFragment.osaTermList.get(RecordFragment.osaTermList.size() - 1).AnalysisRawDataList.add(new AnalysisRawData(times, amplitude, decibel, frequency));
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