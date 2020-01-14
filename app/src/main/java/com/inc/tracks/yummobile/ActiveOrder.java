package com.inc.tracks.yummobile;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.HashMap;

public class ActiveOrder implements Serializable {

    public ActiveOrder(){}

    public ActiveOrder(String clientId, String restaurantId,
                       HashMap<String, Integer> orderItems, Timestamp timestamp) {
        this.clientId = clientId;
        this.restaurantId = restaurantId;
        this.orderItems = orderItems;
        this.timestamp = timestamp;
    }

    private String id;

    private String clientId;

    private String restaurantId;

    private HashMap<String, Integer> orderItems;

    private Timestamp timestamp;


    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public HashMap<String, Integer> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(HashMap<String, Integer> orderItems) {
        this.orderItems = orderItems;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
