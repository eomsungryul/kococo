
package kr.co.dwebss.kococo.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Links {

    @SerializedName("self")
    @Expose
    private Self self;
    @SerializedName("code")
    @Expose
    private Code_ code;

    public Self getSelf() {
        return self;
    }

    public void setSelf(Self self) {
        this.self = self;
    }

    public Code_ getCode() {
        return code;
    }

    public void setCode(Code_ code) {
        this.code = code;
    }

}
