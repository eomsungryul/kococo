package kr.co.dwebss.kococo.fragment.recorder;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Analysis{

	private Integer analysisId;
	private Record record;
	private String analysisStartD;
	private String analysisStartDt;
	private String analysisEndD;
	private String analysisEndDt;
	private String analysisFileNm;
	private String analysisFileAppPath;
	private Character analysisServerUploadYn='N';
	private String analysisServerUploadPath;
	private String analysisServerUploadDt;
	private Character claimYn='N';
	private Integer claimReasonCd;
	private String claimContents;
	private String claimRegistDt;
	private List<AnalysisDetails> analysisDetailsList = new ArrayList<AnalysisDetails>(0);

	public Integer getAnalysisId() {
		return analysisId;
	}

	public void setAnalysisId(Integer analysisId) {
		this.analysisId = analysisId;
	}

	public Record getRecord() {
		return record;
	}

	public void setRecord(Record record) {
		this.record = record;
	}

	public String getAnalysisStartD() {
		return analysisStartD;
	}

	public void setAnalysisStartD(String analysisStartD) {
		this.analysisStartD = analysisStartD;
	}

	public String getAnalysisStartDt() {
		return analysisStartDt;
	}

	public void setAnalysisStartDt(String analysisStartDt) {
		this.analysisStartDt = analysisStartDt;
	}

	public String getAnalysisEndD() {
		return analysisEndD;
	}

	public void setAnalysisEndD(String analysisEndD) {
		this.analysisEndD = analysisEndD;
	}

	public String getAnalysisEndDt() {
		return analysisEndDt;
	}

	public void setAnalysisEndDt(String analysisEndDt) {
		this.analysisEndDt = analysisEndDt;
	}

	public String getAnalysisFileNm() {
		return analysisFileNm;
	}

	public void setAnalysisFileNm(String analysisFileNm) {
		this.analysisFileNm = analysisFileNm;
	}

	public String getAnalysisFileAppPath() {
		return analysisFileAppPath;
	}

	public void setAnalysisFileAppPath(String analysisFileAppPath) {
		this.analysisFileAppPath = analysisFileAppPath;
	}

	public Character getAnalysisServerUploadYn() {
		return analysisServerUploadYn;
	}

	public void setAnalysisServerUploadYn(Character analysisServerUploadYn) {
		this.analysisServerUploadYn = analysisServerUploadYn;
	}

	public String getAnalysisServerUploadPath() {
		return analysisServerUploadPath;
	}

	public void setAnalysisServerUploadPath(String analysisServerUploadPath) {
		this.analysisServerUploadPath = analysisServerUploadPath;
	}

	public String getAnalysisServerUploadDt() {
		return analysisServerUploadDt;
	}

	public void setAnalysisServerUploadDt(String analysisServerUploadDt) {
		this.analysisServerUploadDt = analysisServerUploadDt;
	}

	public Character getClaimYn() {
		return claimYn;
	}

	public void setClaimYn(Character claimYn) {
		this.claimYn = claimYn;
	}

	public Integer getClaimReasonCd() {
		return claimReasonCd;
	}

	public void setClaimReasonCd(Integer claimReasonCd) {
		this.claimReasonCd = claimReasonCd;
	}

	public String getClaimContents() {
		return claimContents;
	}

	public void setClaimContents(String claimContents) {
		this.claimContents = claimContents;
	}

	public String getClaimRegistDt() {
		return claimRegistDt;
	}

	public void setClaimRegistDt(String claimRegistDt) {
		this.claimRegistDt = claimRegistDt;
	}

	public List<AnalysisDetails> getAnalysisDetailsList() {
		return analysisDetailsList;
	}

	public void setAnalysisDetailsList(List<AnalysisDetails> analysisDetailsList) {
		this.analysisDetailsList = analysisDetailsList;
	}
}
