package kr.co.dwebss.kococo.fragment.recorder;

import java.time.LocalDateTime;
import java.util.Date;

public class AnalysisDetails {

	private Integer analysisDetailsId;
	private Analysis analysis;
	private Integer termTypeCd;
	private Date termStartDt;
	private Date termEndDt;
	
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
	public Date getTermStartDt() {
		return termStartDt;
	}
	public void setTermStartDt(Date termStartDt) {
		this.termStartDt = termStartDt;
	}
	public Date getTermEndDt() {
		return termEndDt;
	}
	public void setTermEndDt(Date termEndDt) {
		this.termEndDt = termEndDt;
	}

	

}
