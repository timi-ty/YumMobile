package com.inc.tracks.yummobile;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.HashMap;

public class ActiveOrder implements Serializable {

    public ActiveOrder(){}

    public ActiveOrder(String clientId, String restaurantId,
                       HashMap<String, Integer> orderItems, int cost,
                       String description, Timestamp timestamp) {
        this.clientId = clientId;
        this.restaurantId = restaurantId;
        this.orderItems = orderItems;
        this.cost = cost;
        this.description = description;
        this.timestamp = timestamp;

        accepted = false;
        transporterConfirmed = false;
        clientConfirmed = false;
    }

    private String id;

    private String clientId;

    private String restaurantId;

    private HashMap<String, Integer> orderItems;

    private int cost;

    private String description;

    private boolean accepted;

    private boolean transporterConfirmed;
    private boolean clientConfirmed;

    private String transporterId;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public boolean isAccepted() {
        return accepted;
    }


    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public void setTransporter(String transporterId) {
        this.accepted = true;
        this.transporterId = transporterId;
    }

    public String getTransporterId() {
        return transporterId;
    }

    public boolean isTransporterConfirmed() {
        return transporterConfirmed;
    }

    public void setTransporterConfirmed(boolean transporterConfirmed) {
        this.transporterConfirmed = transporterConfirmed;
    }

    public boolean isClientConfirmed() {
        return clientConfirmed;
    }

    public void setClientConfirmed(boolean clientConfirmed) {
        this.clientConfirmed = clientConfirmed;
    }
}
