package com.inc.tracks.yummobile.user_fragments;

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
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.utils.FeedProgressDbHelper;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.utils.ProgressDbContract;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.Locale;

import co.paystack.android.model.Card;


public class PaymentFragment extends Fragment implements
        View.OnClickListener{

    ViewPager2 mPager;
    FragmentStateAdapter pagerAdapter;

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private EditText txtCardNumber;
    private EditText txtCVV;
    private EditText txtExpiryDate;

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

        mPager = fragView.findViewById(R.id.pager_cards);
        pagerAdapter = new CardPagerAdapter(getActivity());
        mPager.setAdapter(pagerAdapter);

        DotsIndicator dotsIndicator = fragView.findViewById(R.id.dots_indicator);

        dotsIndicator.setViewPager2(mPager);

        Button btnPayNow = fragView.findViewById(R.id.btn_payNow);

        txtCardNumber = fragView.findViewById(R.id.txt_cardNumber);
        txtCVV = fragView.findViewById(R.id.txt_CVV);
        txtExpiryDate = fragView.findViewById(R.id.txt_expirationDate);

        btnPayNow.setOnClickListener(this);

        pagerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                populateTextViews(getSavedCard(mPager.getCurrentItem()));
                super.onChanged();
            }
        });

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

        openLocalDb(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        closeLocalDb();
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
//            expMonth = Integer.parseInt(txtExpiryMonth.getText().toString());
            /*parse date to month and year here*/
            expYear = Integer.parseInt(txtExpiryDate.getText().toString());
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

    private void populateTextViews(@Nullable CardInfo cardInfo){
        if(cardInfo == null) return;
        txtCardNumber.setText(cardInfo.getCardNumber());
        txtCVV.setText(cardInfo.getCvv());
        txtExpiryDate.setText(String.format(Locale.ENGLISH, "%d/%d", cardInfo.getExpiryMonth(),
                cardInfo.getExpiryYear()));
    }

    private CardInfo getCardEntry(){
        String cardNumber = txtCardNumber.getText().toString();
        String cvv = txtCVV.getText().toString();
        int expMonth = 0;
        int expYear = 0;

        try{
            /*parse date to month and year here*/
            expYear = Integer.parseInt(txtExpiryDate.getText().toString());
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

    private int getCardCount(){
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

    private CardInfo getSavedCard(int position){
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

        if(cursor.getCount() < 1) return null;

        cursor.moveToPosition(position);

        String cardNum = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN));
        String cvv = cursor.getString(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN));
        int expMonth = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN));
        int expYear = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN));

        cursor.close();

        return new CardInfo(cardNum, cvv, expMonth, expYear);
    }

    private void saveCard(@Nullable CardInfo card){
        if(card == null) return;

        String selection = ProgressDbContract.FeedSavedCardEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(mPager.getCurrentItem())};

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

        if(mPager.getCurrentItem() >= cursor.getCount()){
            writableProgressDb.insert(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, null, cardData);

            Snackbar.make((View) myLayout.getParent(),
                    "Card Details Saved.", Snackbar.LENGTH_SHORT).show();
        }
        else{
            writableProgressDb.update(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, cardData, selection, selectionArgs);
        }

        cursor.close();
    }

    private void openLocalDb(Context context){
        progressDbHelper = new FeedProgressDbHelper(context);
        readableProgressDb = progressDbHelper.getReadableDatabase();
        writableProgressDb = progressDbHelper.getWritableDatabase();
    }

    private void closeLocalDb(){
        readableProgressDb.close();
        writableProgressDb.close();
        progressDbHelper.close();
    }

    private class CardPagerAdapter extends FragmentStateAdapter {
        public CardPagerAdapter(FragmentActivity fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return CardFragment.newInstance(getSavedCard(position));
        }

        @Override
        public int getItemCount() {
            return getCardCount() + 1;
        }
    }
}
