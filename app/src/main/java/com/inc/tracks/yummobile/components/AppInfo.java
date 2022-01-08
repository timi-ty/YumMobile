package com.inc.tracks.yummobile.components;

import java.io.Serializable;

public class AppInfo implements Serializable {

    public AppInfo(){}

    private static AppInfo cachedInstance;

    private String customerSupportPhone;
    private String aboutYumMobile;

    private String _oldCustomerSupportPhone;

    public static AppInfo getCachedInstance() {
        return cachedInstance = cachedInstance == null ? new AppInfo() : cachedInstance;
    }

    public static void setCachedInstance(AppInfo cachedInstance) {
        AppInfo.cachedInstance = cachedInstance;
    }


    public String getCustomerSupportPhone() {
        return customerSupportPhone;
    }

    public void setCustomerSupportPhone(String customerSupportPhone) {
        _oldCustomerSupportPhone = this.customerSupportPhone;
        this.customerSupportPhone = customerSupportPhone;
    }

    public String getAboutYumMobile() {
        return aboutYumMobile;
    }

    public void setAboutYumMobile(String aboutYumMobile) {
        this.aboutYumMobile = aboutYumMobile;
    }

    public void revertToOldCustomerSupportPhone(){
        this.customerSupportPhone = _oldCustomerSupportPhone;
    }
}
