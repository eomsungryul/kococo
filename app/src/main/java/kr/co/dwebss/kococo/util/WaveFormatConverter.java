package kr.co.dwebss.kococo.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class WaveFormatConverter {

	private final int LONGINT = 4;
	private final int SMALLINT = 2;
	private final int INTEGER = 4;
	private final int ID_STRING_SIZE = 4;
	private final int WAV_RIFF_SIZE = LONGINT+ID_STRING_SIZE;
	private final int WAV_FMT_SIZE = (4*SMALLINT)+(INTEGER*2)+LONGINT+ID_STRING_SIZE;
	private final int WAV_DATA_SIZE = ID_STRING_SIZE+LONGINT;
	private final int WAV_HDR_SIZE = WAV_RIFF_SIZE+ID_STRING_SIZE+WAV_FMT_SIZE+WAV_DATA_SIZE;
	private final short PCM = 1;
	private final int SAMPLE_SIZE = 2;
	int cursor, nSamples;
	byte[] output;


	int folderCnt;
	public WaveFormatConverter(){

	}
	public WaveFormatConverter(int sampleRate, short nChannels, byte[] data, int start, int end)
	{
		nSamples=end-start+1;
		cursor=0;
		output=new byte[nSamples*SMALLINT+WAV_HDR_SIZE];
		buildHeader(sampleRate,nChannels);
		writeData(data,start,end);
	}
	// ------------------------------------------------------------
	private void buildHeader(int sampleRate, short nChannels)
	{
		write("RIFF");
		write(output.length/2);
		write("WAVE");
		writeFormat(sampleRate, nChannels);
	}
	// ------------------------------------------------------------
	public void writeFormat(int sampleRate, short nChannels)
	{
		write("fmt ");
		write(WAV_FMT_SIZE-WAV_DATA_SIZE);
		write(PCM);
		write(nChannels);
		write(sampleRate);
		write(nChannels * sampleRate * SAMPLE_SIZE);
		write((short)(nChannels * SAMPLE_SIZE));
		write((short)16);
	}
	// ------------------------------------------------------------
	public void writeData(byte[] data, int start, int end)
	{
		write("data");
		write(nSamples*SMALLINT/2);
		for(int i=start; i<=end; write(data[i++]));
	}
	// ------------------------------------------------------------
	private void write(byte b)
	{
		output[cursor++]=b;
	}
	// ------------------------------------------------------------
	private void write(String id)
	{
		if(id.length()!=ID_STRING_SIZE) System.out.println("String "+id+" must have four characters.");
		else {
			for(int i=0; i<ID_STRING_SIZE; ++i) write((byte)id.charAt(i));
		}
	}
	// ------------------------------------------------------------
	private void write(int i)
	{
		write((byte) (i&0xFF)); i>>=8;
		write((byte) (i&0xFF)); i>>=8;
		write((byte) (i&0xFF)); i>>=8;
		write((byte) (i&0xFF));
	}
	// ------------------------------------------------------------
	private void write(short i)
	{
		write((byte) (i&0xFF)); i>>=8;
		write((byte) (i&0xFF));
	}
	// ------------------------------------------------------------


	public String[] saveLongTermMp3(String fileName, Context x, byte[] waveData) {
//		File myDir = new File(Environment.getExternalStorageDirectory(), "rec_data/");
		//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		String savePath = x.getFilesDir().getAbsolutePath(); // 이경로는 adb pull 이 안됨.
//		savePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(); //테스트하려고 임시로 쓴 경로
		System.out.println(savePath+"/rec_data/"+ "----------------------save rec_data/rec_data/rec_data/rec_data/rec_data/rec_data/");
		subDirList(savePath+"/rec_data/");
		folderCnt++;
		File myDir = new File(savePath, "rec_data/"+folderCnt+"/");
		if(!myDir.exists()){
			myDir.mkdirs();
		}

		System.out.println(fileName+ "----------------------save start");
		System.out.println(myDir.toString()+ "----------------------save wdawdawdawdawdawdawdadw");
		String filename = "snoring-"+fileName+"_"+System.currentTimeMillis()+".mp3";
		try {

			File path=new File(myDir,filename);
			FileOutputStream outFile = new FileOutputStream(path);
			outFile.write(waveData,0,waveData.length);
			outFile.close();
//			filename = path.getAbsolutePath();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			filename = "e1";
		} catch (IOException e) {
			filename = "e2";
			e.printStackTrace();
		}
		System.out.println(fileName+ "----------------------save end");
		String[] fileInfo = new String[2];
		fileInfo[0] = myDir.toString();
		fileInfo[1] = filename;
		return fileInfo;
	}


	public String[] saveLongTermWave(String fileName, Context x) {
//		File myDir = new File(Environment.getExternalStorageDirectory(), "rec_data/");
		System.out.println(x.getFilesDir().getAbsolutePath()+"/rec_data/"+ "=======save rec_data");
		subDirList(x.getFilesDir().getAbsolutePath()+"/rec_data/");
		folderCnt++;
		File myDir = new File(x.getFilesDir().getAbsolutePath(), "rec_data/"+folderCnt+"/");
		if(!myDir.exists()){
			myDir.mkdirs();
		}

		System.out.println("============save===fileName=========="+fileName);
		System.out.println("============myDir.toString()======"+myDir.toString());
		String filename = "snoring-"+fileName+"_"+System.currentTimeMillis()+".wav";
		try {
			File path=new File(myDir,filename);
			FileOutputStream outFile = new FileOutputStream(path);
			outFile.write(output);
			outFile.close();
//			filename = path.getAbsolutePath();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			filename = "e1";
		} catch (IOException e) {
			filename = "e2";
			e.printStackTrace();
		}
		System.out.println(fileName+ "----------------------save end");
		String[] fileInfo = new String[2];
		fileInfo[0] = myDir.toString();
		fileInfo[1] = filename;
		return fileInfo;
	}

	public void subDirList(String source){
		folderCnt = 0;
		File dir = new File(source);
		if(!dir.exists()){
			dir.mkdirs();
		}
//		System.out.println("\t 파일 이름 = " + dir.getName());

		File[] fileList = dir.listFiles();
		for(int i = 0 ; i < fileList.length ; i++){
			File file = fileList[i];
			if(file.isFile()){
				// 파일이 있다면 파일 이름 출력
//				System.out.println("\t 파일 이름 = " + file.getName());
			}else if(file.isDirectory()){
//				System.out.println("디렉토리 이름 = " + file.getName());
				folderCnt++;
				// 서브디렉토리가 존재하면 재귀적 방법으로 다시 탐색
//					subDirList(file.getCanonicalPath().toString());
			}

		}

	}


	public String[] saveLongTermMp3Test(String fileName, Context x, byte[] waveData) {
//		File myDir = new File(Environment.getExternalStorageDirectory(), "rec_data/");
		//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		String savePath = x.getFilesDir().getAbsolutePath(); // 이경로는 adb pull 이 안됨.
		savePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(); //테스트하려고 임시로 쓴 경로
		System.out.println("============savePath======="+savePath);
		File myDir = new File(savePath);
		if(!myDir.exists()){
			myDir.mkdirs();
		}
		String filename = "test.mp3";
		try {
			File path=new File(myDir,filename);
			FileOutputStream outFile = new FileOutputStream(path);
			outFile.write(waveData,0,waveData.length);
			outFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			filename = "e1";
		} catch (IOException e) {
			filename = "e2";
			e.printStackTrace();
		}
		String[] fileInfo = new String[2];
		fileInfo[0] = myDir.toString();
		fileInfo[1] = filename;
		return fileInfo;
	}

}
