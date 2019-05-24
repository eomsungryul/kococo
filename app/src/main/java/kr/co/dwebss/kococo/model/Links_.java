
package kr.co.dwebss.kococo.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Links_ {

    @SerializedName("self")
    @Expose
    private Self_ self;
    @SerializedName("profile")
    @Expose
    private Profile profile;

    public Self_ getSelf() {
        return self;
    }

    public void setSelf(Self_ self) {
        this.self = self;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

}
