package com.inc.tracks.yummobile.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.CardInfo;

import java.util.Objects;

public class CardManager {

    private FeedProgressDbHelper progressDbHelper;
    private SQLiteDatabase readableProgressDb;
    private SQLiteDatabase writableProgressDb;

    public CardManager(Context context){
        progressDbHelper = new FeedProgressDbHelper(context);
        readableProgressDb = progressDbHelper.getReadableDatabase();
        writableProgressDb = progressDbHelper.getWritableDatabase();

        progressDbHelper.onCreate(writableProgressDb);
    }

    public int getCardCount(){
        Cursor cursor;
        String[] projection = {
                BaseColumns._ID,
                ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN,
        };
        cursor = readableProgressDb.query(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE,
                projection, null, null, null, null, null);

        int count = cursor.getCount();

        cursor.close();

        return count;
    }

    public CardInfo getSavedCard(int position){
        Cursor cursor;
        String[] projection = {
                BaseColumns._ID,
                ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.NAME_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN,
        };
        cursor = readableProgressDb.query(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE,
                projection, null, null, null, null, null);

        if(cursor.getCount() < 1 || position >= cursor.getCount()) return null;

        cursor.moveToPosition(position);

        String cardNum = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN));
        String cvv = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN));
        String cardName = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.NAME_COLUMN));
        int expMonth = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN));
        int expYear = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN));
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry._ID));

        cursor.close();

        CardInfo card = new CardInfo(cardNum, cvv, expMonth, expYear, cardName);
        card.setId(id);

        return card;
    }

    public void saveCard(@Nullable CardInfo card){
        if(card == null) return;

        ContentValues cardData = new ContentValues();

        cardData.put(ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN, card.getCardNumber());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN, card.getCvv());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.NAME_COLUMN, card.getHolderName());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN, card.getExpiryMonth());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN, card.getExpiryYear());

        writableProgressDb.insert(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, null, cardData);
    }

    public void saveDefaultPaymentMethod(@Nullable CardInfo card, Context context){
        long id = card != null ? card.getId() : -1;
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(context.getString(R.string.file_default_payment), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(context.getString(R.string.key_default_payment), id);
        editor.apply();
    }

    public long getDefaultPaymentMethodId(Context context){
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(context.getString(R.string.file_default_payment), Context.MODE_PRIVATE);
        return sharedPreferences.getLong(context.getString(R.string.key_default_payment), -1);
    }

    public String getDefaultPaymentMethodName(Context context){
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(context.getString(R.string.file_default_payment), Context.MODE_PRIVATE);
        long optionId = sharedPreferences.getLong(context.getString(R.string.key_default_payment), -1);

        if(optionId == -1) return "Pay on Delivery";
        Cursor cursor;
        String[] projection = {
                BaseColumns._ID,
                ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN
        };
        String selection = ProgressDbContract.FeedSavedCardEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(optionId)};
        cursor = readableProgressDb.query(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE,
                projection, selection, selectionArgs, null, null, null);

        if(cursor.getCount() < 1) return "null";

        cursor.moveToFirst();

        String cardNum = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract
                .FeedSavedCardEntry.CARD_NUM_COLUMN));

        cursor.close();

        return formatCardNumber(cardNum);
    }

    public CardInfo getDefaultPaymentMethod(Context context){
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(context.getString(R.string.file_default_payment), Context.MODE_PRIVATE);
        long optionId = sharedPreferences.getLong(context.getString(R.string.key_default_payment), -1);

        Cursor cursor;
        String[] projection = {
                BaseColumns._ID,
                ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.NAME_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN,
        };
        String selection = ProgressDbContract.FeedSavedCardEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(optionId)};
        cursor = readableProgressDb.query(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE,
                projection, selection, selectionArgs, null, null, null);

        if(cursor.getCount() < 1) return null;

        cursor.moveToFirst();

        String cardNum = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN));
        String cvv = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN));
        String cardName = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.NAME_COLUMN));
        int expMonth = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN));
        int expYear = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN));
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry._ID));

        cursor.close();

        CardInfo card = new CardInfo(cardNum, cvv, expMonth, expYear, cardName);
        card.setId(id);

        return card;
    }

    public void deleteCard(int position){
        String selection = ProgressDbContract.FeedSavedCardEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(Objects.requireNonNull(getSavedCard(position)).getId())};

        writableProgressDb.delete(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, selection, selectionArgs);
    }

    public String formatCardNumber(String cardNumber){
        StringBuilder formattedNumber = new StringBuilder();
        for (int i = 0; i < cardNumber.length() - 4; i++) {
            formattedNumber.append("X");
            if((i + 1) % 4 == 0) formattedNumber.append(" ");
        }

        formattedNumber.append(cardNumber.substring(cardNumber.length() - 4));
        return formattedNumber.toString();
    }

    public int getCardHolderIcon(CardInfo cardInfo){
        if(cardInfo == null) return R.drawable.ic_cash;
        if(cardInfo.getCardNumber().charAt(0) == '5') return R.drawable.ic_mastercard_symbol;
        else return R.drawable.ic_visa_card_logo;
    }

    public void finishManagingCards(){
        readableProgressDb.close();
        writableProgressDb.close();
//        progressDbHelper.close();
    }
}
