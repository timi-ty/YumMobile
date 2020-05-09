package com.inc.tracks.yummobile.user_fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.utils.CardManager;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.List;

import co.paystack.android.model.Card;


public class ManageCardsFragment extends Fragment implements
        View.OnClickListener{

    private ViewPager2 mPager;
    private CardPagerAdapter pagerAdapter;

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private EditText txtCardNumber;
    private EditText txtCVV;
    private EditText txtExpiryDate;
    private EditText txtHolderName;

    private CardManager cardManager;

    private TextWatcher dateWatcher = new TextWatcher() {
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
                setEditTexts(cardManager.getSavedCard(position));
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

        cardManager = new CardManager(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        cardManager.finishManagingCards();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_addCard) {
            saveCard(getCardEntry());
        }
    }

    private void setEditTexts(@Nullable CardInfo cardInfo){
        if(cardInfo == null) {
            releaseEditTexts();
            return;
        }
        txtCardNumber.setText(cardManager.formatCardNumber(cardInfo.getCardNumber()));
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

    private void saveCard(@Nullable CardInfo card){
        if(card == null) return;

        cardManager.saveCard(card);

        Snackbar.make((View) myLayout.getParent(),
                "Card Details Saved.", Snackbar.LENGTH_SHORT).show();

        pagerAdapter.addCardFragment(card);
    }

    private void deleteCard(int position){
        cardManager.deleteCard(position);

        Snackbar.make((View) myLayout.getParent(),
                "Card Deleted.", Snackbar.LENGTH_SHORT).show();

        pagerAdapter.deleteCardFragment(position);
    }

    public boolean cardInteraction(int interactionId){
        switch (interactionId){
            case R.id.btn_deleteCard:
                deleteCard(mPager.getCurrentItem());
                return true;
            default: return false;
        }
    }

    private class CardPagerAdapter extends FragmentStateAdapter{
        private List<CardFragment> cardFragments = new ArrayList<>();
        CardPagerAdapter(FragmentActivity fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            CardFragment cardFragment;
            if(position >= cardFragments.size()) {
                cardFragment = CardFragment.newInstance(cardManager.getSavedCard(position));
                cardFragments.add(cardFragment);
                Log.d("fragments", "count = " + cardFragments.size() + " pos = " + position);
            }
            else {
                cardFragments.set(position, CardFragment.newInstance(cardManager.getSavedCard(position)));
                cardFragment = cardFragments.get(position);
                Log.d("fragments", "countme = " + cardFragments.size() + " pos = " + position);
            }
            return cardFragment;
        }

        void addCardFragment(CardInfo card){
            cardFragments.get(mPager.getCurrentItem()).updateCardInfo(card);
            setEditTexts(card);
            notifyDataSetChanged();
        }

        void deleteCardFragment(int position){
            cardFragments.remove(position);
            setEditTexts(cardManager.getSavedCard(position));
            notifyItemRemoved(position - 1);
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);
        }

        @Override
        public int getItemCount() {
            return cardManager.getCardCount() + 1;
        }
    }
}
