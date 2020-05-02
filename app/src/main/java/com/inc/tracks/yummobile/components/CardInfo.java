package com.inc.tracks.yummobile.components;

import java.io.Serializable;

public class CardInfo implements Serializable {

    public CardInfo(String cardNumber, String cvv, int expiryMonth, int expiryYear, String holderName) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.holderName = holderName;
    }

    private String cardNumber;
    private String cvv;
    private int expiryMonth;
    private int expiryYear;
    private String holderName;

    private int id;


    public String getCardNumber() {
        return cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public String getHolderName() {
        return holderName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
