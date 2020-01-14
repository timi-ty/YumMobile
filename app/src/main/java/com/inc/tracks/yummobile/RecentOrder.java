package com.inc.tracks.yummobile;

import java.io.Serializable;
import java.util.HashMap;

public class RecentOrder implements Serializable {

    public RecentOrder(){}

    public RecentOrder(String orderName, HashMap<String, HashMap<String, Integer>> orderSummary) {
        this.orderName = orderName;
        this.orderSummary = orderSummary;
    }

    private String id;

    private String orderName;

    private HashMap<String, HashMap<String, Integer>> orderSummary;

    private String imgRef;


    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public HashMap<String, HashMap<String, Integer>> getOrderSummary() {
        return orderSummary;
    }

    public void setOrderSummary(HashMap<String, HashMap<String, Integer>> orderSummary) {
        this.orderSummary = orderSummary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImgRef() {
        return imgRef;
    }

    public void setImgRef(String imgRef) {
        this.imgRef = imgRef;
    }
}
