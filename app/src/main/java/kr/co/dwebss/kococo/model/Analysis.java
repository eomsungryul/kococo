package kr.co.dwebss.kococo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Analysis implements Serializable {

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
    private List<AnalysisDetails> analysisDetailsList = new ArrayList<AnalysisDetails>();
    private String recordingData;

    public Analysis() {
    }

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

    public List<AnalysisDetails> getAnalysisDetailsList() {
        return analysisDetailsList;
    }

    public void setAnalysisDetailsList(List<AnalysisDetails> analysisDetailsList) {
        this.analysisDetailsList = analysisDetailsList;
    }

    public String getRecordingData() {
        return recordingData;
    }

    public void setRecordingData(String recordingData) {
        this.recordingData = recordingData;
    }
}
