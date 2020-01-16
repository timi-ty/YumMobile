package com.inc.tracks.yummobile;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;

public class RestaurantItem implements Serializable {

    public RestaurantItem(){}


    private String id;
    private String name;
    private String description;
    private String address;
    private String imgRef;

    private Double latitude;
    private Double longitude;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImgRef() {
        return imgRef;
    }

    public void setImgRef(String imgRef) {
        this.imgRef = imgRef;
    }

    public void setLocation(@Nullable GeoPoint location){
        if(location != null){
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
        }
        else{
            latitude = longitude = null;
        }
    }

    public GeoPoint getLocation(){
        if(latitude != null && longitude != null){
            return new GeoPoint(latitude, longitude);
        }
        else{
            return null;
        }
    }
}
