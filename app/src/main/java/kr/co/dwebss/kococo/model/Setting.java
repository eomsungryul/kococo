package kr.co.dwebss.kococo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Setting implements Serializable {

    private String menuName;
    private Integer seq;

    public Setting() {
    }

    public Setting(String menuName, int seq) {
        this.menuName = menuName;
        this.seq = seq;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }
}
