package kr.co.dwebss.kococo.fragment.recorder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Record {

	private String userAppId;
	private Integer recordId;
	private LocalDateTime recordStartD;
	private LocalDateTime recordStartDt;
	private LocalDateTime recordEndD;
	private LocalDateTime recordEndDt;
	private Character consultingYn='N';
	private Character consultingReplyYn='N';
	private String consultingTitle;
	private String consultingContents;
	private LocalDateTime consultingRegistDt;
	private String consultingReplyContents;
	private LocalDateTime consultingReplyRegistDt;
	private List<Analysis> analysisList = new ArrayList<Analysis>(0);
	public Record() {
		super();
	}
	public Record(String userAppId, Integer recordId, LocalDateTime recordStartD, LocalDateTime recordStartDt,
			LocalDateTime recordEndD, LocalDateTime recordEndDt, Character consultingYn, Character consultingReplyYn,
			String consultingTitle, String consultingContents, LocalDateTime consultingRegistDt,
			String consultingReplyContents, LocalDateTime consultingReplyRegistDt, List<Analysis> analysisList) {
		super();
		this.userAppId = userAppId;
		this.recordId = recordId;
		this.recordStartD = recordStartD;
		this.recordStartDt = recordStartDt;
		this.recordEndD = recordEndD;
		this.recordEndDt = recordEndDt;
		this.consultingYn = consultingYn;
		this.consultingReplyYn = consultingReplyYn;
		this.consultingTitle = consultingTitle;
		this.consultingContents = consultingContents;
		this.consultingRegistDt = consultingRegistDt;
		this.consultingReplyContents = consultingReplyContents;
		this.consultingReplyRegistDt = consultingReplyRegistDt;
		this.analysisList = analysisList;
	}
	public String getUserAppId() {
		return userAppId;
	}
	public void setUserAppId(String userAppId) {
		this.userAppId = userAppId;
	}
	public Integer getRecordId() {
		return recordId;
	}
	public void setRecordId(Integer recordId) {
		this.recordId = recordId;
	}
	public LocalDateTime getRecordStartD() {
		return recordStartD;
	}
	public void setRecordStartD(LocalDateTime recordStartD) {
		this.recordStartD = recordStartD;
	}
	public LocalDateTime getRecordStartDt() {
		return recordStartDt;
	}
	public void setRecordStartDt(LocalDateTime recordStartDt) {
		this.recordStartDt = recordStartDt;
	}
	public LocalDateTime getRecordEndD() {
		return recordEndD;
	}
	public void setRecordEndD(LocalDateTime recordEndD) {
		this.recordEndD = recordEndD;
	}
	public LocalDateTime getRecordEndDt() {
		return recordEndDt;
	}
	public void setRecordEndDt(LocalDateTime recordEndDt) {
		this.recordEndDt = recordEndDt;
	}
	public Character getConsultingYn() {
		return consultingYn;
	}
	public void setConsultingYn(Character consultingYn) {
		this.consultingYn = consultingYn;
	}
	public Character getConsultingReplyYn() {
		return consultingReplyYn;
	}
	public void setConsultingReplyYn(Character consultingReplyYn) {
		this.consultingReplyYn = consultingReplyYn;
	}
	public String getConsultingTitle() {
		return consultingTitle;
	}
	public void setConsultingTitle(String consultingTitle) {
		this.consultingTitle = consultingTitle;
	}
	public String getConsultingContents() {
		return consultingContents;
	}
	public void setConsultingContents(String consultingContents) {
		this.consultingContents = consultingContents;
	}
	public LocalDateTime getConsultingRegistDt() {
		return consultingRegistDt;
	}
	public void setConsultingRegistDt(LocalDateTime consultingRegistDt) {
		this.consultingRegistDt = consultingRegistDt;
	}
	public String getConsultingReplyContents() {
		return consultingReplyContents;
	}
	public void setConsultingReplyContents(String consultingReplyContents) {
		this.consultingReplyContents = consultingReplyContents;
	}
	public LocalDateTime getConsultingReplyRegistDt() {
		return consultingReplyRegistDt;
	}
	public void setConsultingReplyRegistDt(LocalDateTime consultingReplyRegistDt) {
		this.consultingReplyRegistDt = consultingReplyRegistDt;
	}
	public List<Analysis> getAnalysisList() {
		return analysisList;
	}
	public void setAnalysisList(List<Analysis> analysisList) {
		this.analysisList = analysisList;
	}

	
}
