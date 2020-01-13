package com.inc.tracks.yummobile;

import java.io.Serializable;

public class UserPrefs implements Serializable {

    public UserPrefs(){}

    private String userName;
    private String userPhone;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }
}
