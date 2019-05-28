package kr.co.dwebss.kococo.fragment.recorder;

import java.time.LocalDateTime;
import java.util.Date;

public class AnalysisDetails {

	private Integer analysisDetailsId;
	private Analysis analysis;
	private Integer termTypeCd;
	private String termStartDt;
	private String termEndDt;

	public Integer getAnalysisDetailsId() {
		return analysisDetailsId;
	}

	public void setAnalysisDetailsId(Integer analysisDetailsId) {
		this.analysisDetailsId = analysisDetailsId;
	}

	public Analysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(Analysis analysis) {
		this.analysis = analysis;
	}

	public Integer getTermTypeCd() {
		return termTypeCd;
	}

	public void setTermTypeCd(Integer termTypeCd) {
		this.termTypeCd = termTypeCd;
	}

	public String getTermStartDt() {
		return termStartDt;
	}

	public void setTermStartDt(String termStartDt) {
		this.termStartDt = termStartDt;
	}

	public String getTermEndDt() {
		return termEndDt;
	}

	public void setTermEndDt(String termEndDt) {
		this.termEndDt = termEndDt;
	}
}
