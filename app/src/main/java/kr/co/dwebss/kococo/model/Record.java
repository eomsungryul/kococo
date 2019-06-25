package kr.co.dwebss.kococo.model;

import com.google.gson.JsonElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Record implements Serializable {

    private String userAppId;
    private Integer recordId;
    private String recordStartD;
    private String recordStartDt;
    private String recordEndD;
    private String recordEndDt;
    private Character consultingYn='N';
    private Character consultingReplyYn='N';
    private String consultingTitle;
    private String consultingContents;
    private String consultingRegistDt;
    private String consultingReplyContents;
    private String consultingReplyRegistDt;
    private List<Analysis> analysisList = new ArrayList<>(0);
    private Integer sleepStatusCd;
    private Integer sleepStatusCdId;


    public Record() {
    }

    public Record(int recordId, String recordStartDt, String recordEndDt) {
        this.recordId = recordId;
        this.recordStartDt = recordStartDt;
        this.recordEndDt = recordEndDt;
    }

    public Record(int recordId, String recordStartD, String recordEndD, String recordStartDt, String recordEndDt) {
        this.recordId = recordId;
        this.recordStartD = recordStartD;
        this.recordEndD = recordEndD;
        this.recordStartDt = recordStartDt;
        this.recordEndDt = recordEndDt;
    }

    public Record(int recordId, String recordStartD, String recordEndD, String recordStartDt, String recordEndDt,Character consultingReplyYn) {
        this.recordId = recordId;
        this.recordStartD = recordStartD;
        this.recordEndD = recordEndD;
        this.recordStartDt = recordStartDt;
        this.recordEndDt = recordEndDt;
        this.consultingReplyYn = consultingReplyYn;
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

    public String getRecordStartD() {
        return recordStartD;
    }

    public void setRecordStartD(String recordStartD) {
        this.recordStartD = recordStartD;
    }

    public String getRecordStartDt() {
        return recordStartDt;
    }

    public void setRecordStartDt(String recordStartDt) {
        this.recordStartDt = recordStartDt;
    }

    public String getRecordEndD() {
        return recordEndD;
    }

    public void setRecordEndD(String recordEndD) {
        this.recordEndD = recordEndD;
    }

    public String getRecordEndDt() {
        return recordEndDt;
    }

    public void setRecordEndDt(String recordEndDt) {
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

    public String getConsultingRegistDt() {
        return consultingRegistDt;
    }

    public void setConsultingRegistDt(String consultingRegistDt) {
        this.consultingRegistDt = consultingRegistDt;
    }

    public String getConsultingReplyContents() {
        return consultingReplyContents;
    }

    public void setConsultingReplyContents(String consultingReplyContents) {
        this.consultingReplyContents = consultingReplyContents;
    }

    public String getConsultingReplyRegistDt() {
        return consultingReplyRegistDt;
    }

    public void setConsultingReplyRegistDt(String consultingReplyRegistDt) {
        this.consultingReplyRegistDt = consultingReplyRegistDt;
    }

    public List<Analysis> getAnalysisList() {
        return analysisList;
    }

    public void setAnalysisList(List<Analysis> analysisList) {
        this.analysisList = analysisList;
    }

    public Integer getSleepStatusCd() {
        return sleepStatusCd;
    }

    public void setSleepStatusCd(Integer sleepStatusCd) {
        this.sleepStatusCd = sleepStatusCd;
    }

    public Integer getSleepStatusCdId() {
        return sleepStatusCdId;
    }

    public void setSleepStatusCdId(Integer sleepStatusCdId) {
        this.sleepStatusCdId = sleepStatusCdId;
    }
}
