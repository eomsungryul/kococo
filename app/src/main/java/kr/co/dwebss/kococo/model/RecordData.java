/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.dwebss.kococo.model;

import java.io.Serializable;
import java.text.DecimalFormat;
@SuppressWarnings("serial")
public class RecordData implements Serializable {
    private int analysisId;
    private String analysisFileAppPath;
    private String analysisFileNm;
    private int analysisDetailsId;
    private String title;
    private int termTypeCd;
    private String termStartDt;
    private String termEndDt;
    private String analysisStartDt;
    private String analysisEndDt;

    private String responseObj;

    public String getResponseObj() {
        return responseObj;
    }

    public void setResponseObj(String responseObj) {
        this.responseObj = responseObj;
    }

    public RecordData(){

    }

    public RecordData(int analysisId, String analysisFileAppPath, String analysisFileNm, int analysisDetailsId, String title, int termTypeCd, String termStartDt, String termEndDt, String analysisStartDt, String analysisEndDt) {
        this.analysisId = analysisId;
        this.analysisFileAppPath = analysisFileAppPath;
        this.analysisFileNm = analysisFileNm;
        this.analysisDetailsId = analysisDetailsId;
        this.title = title;
        this.termTypeCd = termTypeCd;
        this.termStartDt = termStartDt;
        this.termEndDt = termEndDt;
        this.analysisStartDt = analysisStartDt;
        this.analysisEndDt = analysisEndDt;
    }

    public RecordData(int analysisId, String analysisFileAppPath, String analysisFileNm, int analysisDetailsId, String title, int termTypeCd, String termStartDt, String termEndDt) {
        this.analysisId = analysisId;
        this.analysisFileAppPath = analysisFileAppPath;
        this.analysisFileNm = analysisFileNm;
        this.analysisDetailsId = analysisDetailsId;
        this.title = title;
        this.termTypeCd = termTypeCd;
        this.termStartDt = termStartDt;
        this.termEndDt = termEndDt;
    }

    public String getAnalysisStartDt() {
        return analysisStartDt;
    }

    public void setAnalysisStartDt(String analysisStartDt) {
        this.analysisStartDt = analysisStartDt;
    }

    public String getAnalysisEndDt() {
        return analysisEndDt;
    }

    public void setAnalysisEndDt(String analysisEndDt) {
        this.analysisEndDt = analysisEndDt;
    }

    public int getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(int analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisFileAppPath() {
        return analysisFileAppPath;
    }

    public void setAnalysisFileAppPath(String analysisFileAppPath) {
        this.analysisFileAppPath = analysisFileAppPath;
    }

    public String getAnalysisFileNm() {
        return analysisFileNm;
    }

    public void setAnalysisFileNm(String analysisFileNm) {
        this.analysisFileNm = analysisFileNm;
    }

    public int getAnalysisDetailsId() {
        return analysisDetailsId;
    }

    public void setAnalysisDetailsId(int analysisDetailsId) {
        this.analysisDetailsId = analysisDetailsId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTermTypeCd() {
        return termTypeCd;
    }

    public void setTermTypeCd(int termTypeCd) {
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
