
package kr.co.dwebss.kococo.model;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Embedded {

    @SerializedName("code")
    @Expose
    private List<Code> code = null;

    public List<Code> getCode() {
        return code;
    }

    public void setCode(List<Code> code) {
        this.code = code;
    }

}
