package kr.co.dwebss.kococo.model;

import java.io.Serializable;

public class AnalysisDetails implements Serializable {

    private Integer analysisId;
    private Integer analysisDetailsId;
    private Analysis analysis;
    private Integer termTypeCd;
    private String termStartDt;
    private String termEndDt;
    private Character claimYn='N';
    private Integer claimReasonCd;
    private String claimContents;
    private String claimRegistDt;

    public AnalysisDetails() {
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

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
}
