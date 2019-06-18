package kr.co.dwebss.kococo.fragment.recorder;

import kr.co.dwebss.kococo.fragment.recorderUtil.DoubleValues;
import kr.co.dwebss.kococo.fragment.recorderUtil.Radix2FFT;
import kr.co.dwebss.kococo.fragment.recorderUtil.ShortValues;

public class FFTDataThread extends Thread {

    int[] tmpArray = null;
    int bufferSize = 0;
    Radix2FFT fft = null;
    double hzPerDataPoint = 0;
    int fftSize = 0;
    DoubleValues fftData = new DoubleValues();

    RecordingThread recordingThread;

    public FFTDataThread(RecordingThread recordingThread){
        this.recordingThread = recordingThread;
        bufferSize = this.recordingThread.getFrameBytesForSnoringLength()/2;
        fft = new Radix2FFT(bufferSize);
        hzPerDataPoint = 44100d / bufferSize;
        fftSize = (int) ((44100d / 2) / (44100d / bufferSize))	;
        tmpArray = new int[fftSize];
        for (int k = 0; k < fftSize; k ++) {
            tmpArray[k] = (int) (k * hzPerDataPoint);
        }
    }
    public void run(){
        ShortValues shortValues = new ShortValues(recordingThread.getTmpBytes());
        fft.run(shortValues, fftData);
        recordingThread.setAllFHAndDB(fftData.getItemsArray());
    }
}
