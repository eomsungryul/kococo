
package kr.co.dwebss.kococo.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Code {

    @SerializedName("code")
    @Expose
    private Integer code;
    @SerializedName("codeCateogry")
    @Expose
    private String codeCateogry;
    @SerializedName("codeValue")
    @Expose
    private String codeValue;
    @SerializedName("_links")
    @Expose
    private Links links;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getCodeCateogry() {
        return codeCateogry;
    }

    public void setCodeCateogry(String codeCateogry) {
        this.codeCateogry = codeCateogry;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

}
