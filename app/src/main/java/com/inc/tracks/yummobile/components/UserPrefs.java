package com.inc.tracks.yummobile.components;

import java.io.Serializable;

public class UserPrefs implements Serializable {

    public UserPrefs(){}

    private String userName;
    private String userPhone;
    private String id;


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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
