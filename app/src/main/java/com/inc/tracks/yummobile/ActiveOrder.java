package com.inc.tracks.yummobile;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.HashMap;

public class ActiveOrder implements Serializable {

    public ActiveOrder(){}

    public ActiveOrder(String clientId, String restaurantId,
                       HashMap<String, Integer> orderItems, int cost,
                       String description, boolean paidFor, Timestamp timestamp) {
        this.clientId = clientId;
        this.restaurantId = restaurantId;
        this.orderItems = orderItems;
        this.cost = cost;
        this.description = description;
        this.paidFor = paidFor;
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

    private boolean paidFor;

    private String transporterId;

    private transient Timestamp timestamp;

    private Double clientLat;
    private Double clientLong;

    private Double transLat;
    private Double transLong;

    private int initialDistance;


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

    public void setClientLocation(@Nullable GeoPoint location){
        if(location != null){
            this.clientLat = location.getLatitude();
            this.clientLong = location.getLongitude();
        }
        else{
            clientLat = clientLong = null;
        }
    }

    public GeoPoint getClientLocation(){
        if(clientLat != null && clientLong != null){
            return new GeoPoint(clientLat, clientLong);
        }
        else{
            return null;
        }
    }

    public void setTransLocation(@Nullable GeoPoint location){
        if(location != null){
            this.transLat = location.getLatitude();
            this.transLong = location.getLongitude();
        }
        else{
            transLat = transLong = null;
        }
    }

    public GeoPoint getTransLocation(){
        if(transLat != null && transLong != null){
            return new GeoPoint(transLat, transLong);
        }
        else{
            return null;
        }
    }

    public int getInitialDistance() {
        return initialDistance;
    }

    public void setInitialDistance(int initialDistance) {
        this.initialDistance = initialDistance;
    }

    public boolean isPaidFor() {
        return paidFor;
    }
}
