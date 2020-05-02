package com.inc.tracks.yummobile.user_fragments;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.utils.FeedProgressDbHelper;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.utils.ProgressDbContract;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.List;
import java.util.Objects;

import co.paystack.android.model.Card;


public class ManageCardsFragment extends Fragment implements
        View.OnClickListener{

    ViewPager2 mPager;
    FragmentStateAdapter pagerAdapter;

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private EditText txtCardNumber;
    private EditText txtCVV;
    private EditText txtExpiryDate;
    private EditText txtHolderName;

    private FeedProgressDbHelper progressDbHelper;
    private SQLiteDatabase readableProgressDb;
    private SQLiteDatabase writableProgressDb;

    TextWatcher dateWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() == 3 && s.charAt(2) != '/'){
                String date = s.subSequence(0, 2) + "/" + s.charAt(2);
                txtExpiryDate.setText(date);
                txtExpiryDate.setSelection(4);
            }
        }
    };

    public ManageCardsFragment() {
        // Required empty public constructor
    }


    public static ManageCardsFragment newInstance() {
        ManageCardsFragment fragment = new ManageCardsFragment();
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

        Button btnAddCard = fragView.findViewById(R.id.btn_addCard);

        txtCardNumber = fragView.findViewById(R.id.txt_cardNumber);
        txtCVV = fragView.findViewById(R.id.txt_CVV);
        txtHolderName = fragView.findViewById(R.id.txt_cardHolderName);
        txtExpiryDate = fragView.findViewById(R.id.txt_expirationDate);
        txtExpiryDate.addTextChangedListener(dateWatcher);

        btnAddCard.setOnClickListener(this);

        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if(position >= getCardCount()) releaseEditTexts();
                else populateEditTexts(getSavedCard(position));
                super.onPageSelected(position);
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
        if (v.getId() == R.id.btn_addCard) {
            saveCard(getCardEntry());
        }
    }

    private void populateEditTexts(@Nullable CardInfo cardInfo){
        if(cardInfo == null) return;
        txtCardNumber.setText(formatCardNumber(cardInfo.getCardNumber()));
        txtCVV.setText(R.string.secret_cvv);
        txtExpiryDate.setText(R.string.secret_date);
        txtHolderName.setText(cardInfo.getHolderName());

        txtCardNumber.setEnabled(false);
        txtCVV.setEnabled(false);
        txtExpiryDate.setEnabled(false);
        txtHolderName.setEnabled(false);
    }

    private void releaseEditTexts(){
        txtCardNumber.setEnabled(true);
        txtCVV.setEnabled(true);
        txtExpiryDate.setEnabled(true);
        txtHolderName.setEnabled(true);

        txtCardNumber.setText("");
        txtCVV.setText("");
        txtExpiryDate.setText("");
        txtHolderName.setText("");
    }

    private CardInfo getCardEntry(){
        String cardNumber = txtCardNumber.getText().toString();
        String cvv = txtCVV.getText().toString();
        String cardName = txtHolderName.getText().toString();
        String[] date = txtExpiryDate.getText().toString().split("/");
        int expMonth = 0;
        int expYear = 0;

        try{
            expMonth = Integer.parseInt(date[0]);
            expYear = Integer.parseInt(date[1]);
        }
        catch (NumberFormatException e){
            e.printStackTrace();
        }

        Card card = new Card(cardNumber, expMonth, expYear, cvv);
        if(card.isValid()){
            return new CardInfo(cardNumber, cvv, expMonth, expYear, cardName);
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

    private void saveCard(@Nullable CardInfo card){
        if(card == null) return;

        ContentValues cardData = new ContentValues();

        cardData.put(ProgressDbContract.FeedSavedCardEntry.CARD_NUM_COLUMN, card.getCardNumber());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.CVV_COLUMN, card.getCvv());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.NAME_COLUMN, card.getHolderName());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.EXP_MONTH_COLUMN, card.getExpiryMonth());
        cardData.put(ProgressDbContract.FeedSavedCardEntry.EXPIRY_YEAR_COLUMN, card.getExpiryYear());

        writableProgressDb.insert(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, null, cardData);
        Snackbar.make((View) myLayout.getParent(),
                "Card Details Saved.", Snackbar.LENGTH_SHORT).show();

        pagerAdapter.notifyDataSetChanged();
        pagerAdapter.createFragment(mPager.getCurrentItem());/**/
        mPager.invalidate();/**/
        populateEditTexts(card);
    }

    private void deleteCard(@Nullable int position){
        String selection = ProgressDbContract.FeedSavedCardEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(Objects.requireNonNull(getSavedCard(position)).getId())};

        writableProgressDb.delete(ProgressDbContract.FeedSavedCardEntry.CARD_TABLE, selection, selectionArgs);
        Snackbar.make((View) myLayout.getParent(),
                "Card Deleted.", Snackbar.LENGTH_SHORT).show();

        pagerAdapter.notifyItemRemoved(position);
        pagerAdapter.createFragment(mPager.getCurrentItem());/**/
        mPager.invalidate();/**/
    }

    public boolean cardInteraction(int interactionId){
        switch (interactionId){
            case R.id.btn_deleteCard:
                deleteCard(mPager.getCurrentItem());
                return true;
            default: return false;
        }
    }

    private String formatCardNumber(String cardNumber){
        StringBuilder formattedNumber = new StringBuilder();
        for (int i = 0; i < cardNumber.length() - 4; i++) {
            formattedNumber.append("X");
            if((i + 1) % 4 == 0) formattedNumber.append(" ");
        }

        formattedNumber.append(cardNumber.substring(cardNumber.length() - 4));
        return formattedNumber.toString();
    }

    private void openLocalDb(Context context){
        progressDbHelper = new FeedProgressDbHelper(context);
        readableProgressDb = progressDbHelper.getReadableDatabase();
        writableProgressDb = progressDbHelper.getWritableDatabase();

        progressDbHelper.onCreate(writableProgressDb);
    }

    private void closeLocalDb(){
        readableProgressDb.close();
        writableProgressDb.close();
        progressDbHelper.close();
    }

    private class CardPagerAdapter extends FragmentStateAdapter{
        public CardPagerAdapter(FragmentActivity fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return CardFragment.newInstance(getSavedCard(position));
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);
        }

        @Override
        public int getItemCount() {
            return getCardCount() + 1;
        }
    }
}
