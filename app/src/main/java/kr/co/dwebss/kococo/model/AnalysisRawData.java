package kr.co.dwebss.kococo.model;

public class AnalysisRawData {

	int sefamplitude;
	double times;
	int amplitude;
	double decibel;
	double frequency;
	double sefrequency;
	
	public AnalysisRawData() {
		super();
	}

	public AnalysisRawData(double times, int amplitude, double decibel, double frequency) {
		super();
		this.times = times;
		this.amplitude = amplitude;
		this.decibel = decibel;
		this.frequency = frequency;
	}
	public AnalysisRawData(double times, int amplitude, double decibel, double frequency, double sefrequency,
			int sefamplitude) {
		super();
		this.times = times;
		this.amplitude = amplitude;
		this.decibel = decibel;
		this.frequency = frequency;
		this.sefrequency = sefrequency;
		this.sefamplitude = sefamplitude;
	}

	public double getTimes() {
		return times;
	}
	public void setTimes(double times) {
		this.times = times;
	}
	public int getAmplitude() {
		return amplitude;
	}
	public void setAmplitude(int amplitude) {
		this.amplitude = amplitude;
	}
	public double getDecibel() {
		return decibel;
	}
	public void setDecibel(double decibel) {
		this.decibel = decibel;
	}
	public double getFrequency() {
		return frequency;
	}
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	public double getSefrequency() {
		return sefrequency;
	}
	public void setSefrequency(double sefrequency) {
		this.sefrequency = sefrequency;
	}
	public int getSefamplitude() {
		return sefamplitude;
	}
	public void setSefamplitude(int sefamplitude) {
		this.sefamplitude = sefamplitude;
	}

	@Override
	public String toString() {
		return "AnalysisRawData [times=" + String.format("%.0f", times) + ", Amp=" + amplitude + ", db=" + decibel + ", Hz="
				+ frequency + ", Hz(2th)=" + sefrequency + ", Amp(2th)=" + sefamplitude + "]";
	}
	
	
}
