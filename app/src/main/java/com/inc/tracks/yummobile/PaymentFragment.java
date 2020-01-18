package com.inc.tracks.yummobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import co.paystack.android.model.Card;


public class PaymentFragment extends Fragment implements
        View.OnClickListener{


    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private EditText txtCardNumber;
    private EditText txtCVV;
    private EditText txtExpiryMonth;
    private EditText txtExpiryYear;

    private FeedProgressDbHelper progressDbHelper;
    private SQLiteDatabase readableProgressDb;
    private SQLiteDatabase writableProgressDb;

    public PaymentFragment() {
        // Required empty public constructor
    }


    public static PaymentFragment newInstance() {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_payment,
                container, false);

        myLayout = fragView.findViewById(R.id.layout_paymentFragment);

        Button btnPayNow = fragView.findViewById(R.id.btn_payNow);

        txtCardNumber = fragView.findViewById(R.id.txt_cardNumber);
        txtCVV = fragView.findViewById(R.id.txt_CVV);
        txtExpiryMonth = fragView.findViewById(R.id.txt_expiryMonth);
        txtExpiryYear = fragView.findViewById(R.id.txt_expiryYear);

        btnPayNow.setOnClickListener(this);

        getSavedCard();

        return fragView;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        progressDbHelper = new FeedProgressDbHelper(context);
        readableProgressDb = progressDbHelper.getReadableDatabase();
        writableProgressDb = progressDbHelper.getWritableDatabase();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        readableProgressDb.close();
        writableProgressDb.close();
        progressDbHelper.close();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_payNow) {
            saveCard(getCardEntry());
            initiatePayment();
        }
    }

    private void initiatePayment(){
        String cardNumber = txtCardNumber.getText().toString();
        String cvv = txtCVV.getText().toString();
        int expMonth = 0;
        int expYear = 0;

        try{
            expMonth = Integer.parseInt(txtExpiryMonth.getText().toString());
            expYear = Integer.parseInt(txtExpiryYear.getText().toString());
        }
        catch (NumberFormatException e){
            e.printStackTrace();
        }

        Card card = new Card(cardNumber, expMonth, expYear, cvv);
        if(card.isValid()){
            mListener.onFragmentInteraction(R.id.btn_payNow, card);
        }
        else{
            Snackbar.make((View) myLayout.getParent(),
                    "Invalid Card Details.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private CardInfo getCardEntry(){
        String cardNumber = txtCardNumber.getText().toString();
        String cvv = txtCVV.getText().toString();
        int expMonth = 0;
        int expYear = 0;

        try{
            expMonth = Integer.parseInt(txtExpiryMonth.getText().toString());
            expYear = Integer.parseInt(txtExpiryYear.getText().toString());
        }
        catch (NumberFormatException e){
            e.printStackTrace();
        }

        Card card = new Card(cardNumber, expMonth, expYear, cvv);
        if(card.isValid()){
            return new CardInfo(cardNumber, cvv, expMonth, expYear);
        }
        else{
            Snackbar.make((View) myLayout.getParent(),
                    "Invalid Card Details.", Snackbar.LENGTH_SHORT).show();
            return  null;
        }
    }




    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId, Card card);
    }

    private void getSavedCard(){
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

        if(cursor.getCount() < 1) return;

        cursor.moveToFirst();

        String cardNum = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN));
        String cvv = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN));
        int expMonth = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN));
        int expYear = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN));

        cursor.close();

        txtCardNumber.setText(cardNum);
        txtCVV.setText(cvv);
        txtExpiryMonth.setText(String.format(Locale.ENGLISH, "%d", expMonth));
        txtExpiryYear.setText(String.format(Locale.ENGLISH, "%d", expYear));
    }

    private void saveCard(@Nullable CardInfo card){
        if(card == null) return;

        String selection = ProgressDbContract.FeedSavedCardEntry._ID + " != ?";
        String[] selectionArgs = {"NULL"};

        String[] projection = {
                BaseColumns._ID,
                ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN,
                ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN,
        };

        ContentValues cardData = new ContentValues();

        cardData.put(ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN, card.getCardNumber());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN, card.getCvv());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN, card.getExpiryMonth());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN, card.getExpiryYear());

        Cursor cursor = readableProgressDb.query(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE,
                projection, null, null, null, null, null);

        if(cursor.getCount() < 1){
            writableProgressDb.insert(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, null, cardData);

            Snackbar.make((View) myLayout.getParent(),
                    "Card Details Saved.", Snackbar.LENGTH_SHORT).show();
        }
        else{
            writableProgressDb.update(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, cardData, selection, selectionArgs);
        }

        cursor.close();
    }
}
