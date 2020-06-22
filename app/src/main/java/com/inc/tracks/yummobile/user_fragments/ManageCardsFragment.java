package com.inc.tracks.yummobile.user_fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.snackbar.Snackbar;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.utils.CardManager;
import com.inc.tracks.yummobile.utils.GlideApp;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        pagerAdapter = new CardPagerAdapter();
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
        fragView.findViewById(R.id.btn_back).setOnClickListener(this);

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
        switch (v.getId()) {
            case R.id.btn_addCard:
                addCard();
                break;
            case R.id.btn_back:
                mListener.onFragmentInteraction(v.getId());
                break;
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
        void onFragmentInteraction(int buttonId);
    }

    private void addCard(){
        if(mPager.getCurrentItem() == pagerAdapter.getItemCount() - 1){
            saveCard(getCardEntry());
        }
        else{
            mPager.setCurrentItem(pagerAdapter.getItemCount() - 1,
                    true);
        }
    }

    private void saveCard(@Nullable CardInfo card){
        if(card == null) return;

        cardManager.saveCard(card);

        Snackbar.make((View) myLayout.getParent(),
                "Card Details Saved.", Snackbar.LENGTH_SHORT).show();

        pagerAdapter.addCardInfo(card);
    }

    public boolean cardInteraction(int interactionId){
        if (interactionId == R.id.btn_addCard){
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPager.setCurrentItem(pagerAdapter.getItemCount() - 1,
                            true);
                }
            }, 500);
            return true;
        }
        return false;
    }

    private class CardPagerAdapter extends RecyclerView.Adapter<CardPagerAdapter.CardViewHolder> {

        private List<CardInfo> cardInfoList = new ArrayList<>();

        CardPagerAdapter() {
            for(int i = 0; i < cardManager.getCardCount(); i++){
                CardInfo cardInfo;
                cardInfo = cardManager.getSavedCard(i);
                cardInfoList.add(cardInfo);
            }
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View cardView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_card, parent, false);
            return new CardViewHolder(cardView);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder cardViewHolder, int position) {
            if(position < cardInfoList.size()){
                cardViewHolder.bindView(cardInfoList.get(position));
            }
            else {
                cardViewHolder.bindView(null);
            }
        }

        @Override
        public int getItemCount() {
            return cardManager.getCardCount() + 1;
        }

        class CardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

            TextView txtCardNumber;
            TextView txtHolderName;
            TextView txtCardExpiry;
            ImageView imgCardVendor;
            ImageView emptyView;
            ImageView background;
            View cardView;

            CardViewHolder(@NonNull View itemView) {
                super(itemView);

                cardView = itemView;
                txtCardNumber = itemView.findViewById(R.id.txt_cardNumber);
                txtHolderName = itemView.findViewById(R.id.txt_cardHolderName);
                txtCardExpiry = itemView.findViewById(R.id.txt_cardExpiry);
                imgCardVendor = itemView.findViewById(R.id.img_cardVendor);
                emptyView = itemView.findViewById(R.id.img_emptyView);
                background = itemView.findViewById(R.id.background);
            }

            void bindView(CardInfo cardInfo){
                if(cardInfo == null){
                    emptyView.setVisibility(View.VISIBLE);
                    txtCardNumber.setVisibility(View.INVISIBLE);
                    txtHolderName.setVisibility(View.INVISIBLE);
                    txtCardExpiry.setVisibility(View.INVISIBLE);
                    imgCardVendor.setVisibility(View.INVISIBLE);
                    cardView.findViewById(R.id.txt_valid).setVisibility(View.INVISIBLE);
                    cardView.findViewById(R.id.img_cardChip).setVisibility(View.INVISIBLE);
                    cardView.findViewById(R.id.txt_cardType).setVisibility(View.INVISIBLE);
                    cardView.findViewById(R.id.btn_deleteCard).setVisibility(View.INVISIBLE);

                    background.setEnabled(false);
                }
                else{
                    emptyView.setVisibility(View.GONE);
                    txtCardNumber.setVisibility(View.VISIBLE);
                    txtHolderName.setVisibility(View.VISIBLE);
                    txtCardExpiry.setVisibility(View.VISIBLE);
                    imgCardVendor.setVisibility(View.VISIBLE);
                    cardView.findViewById(R.id.txt_valid).setVisibility(View.VISIBLE);
                    cardView.findViewById(R.id.img_cardChip).setVisibility(View.VISIBLE);
                    cardView.findViewById(R.id.txt_cardType).setVisibility(View.VISIBLE);
                    cardView.findViewById(R.id.btn_deleteCard).setVisibility(View.VISIBLE);

                    background.setEnabled(true);

                    txtCardNumber.setText(cardManager.formatCardNumber(cardInfo.getCardNumber()));
                    txtHolderName.setText(cardInfo.getHolderName());
                    txtCardExpiry.setText(String.format(Locale.ENGLISH, "%d/%d", cardInfo.getExpiryMonth()
                            , cardInfo.getExpiryYear()));
                    cardView.findViewById(R.id.btn_deleteCard).setOnClickListener(this);
                }

                loadBackground();
            }

            private void loadBackground(){
                GlideApp.with(background.getContext())
                        .load(R.drawable.card_bg)
                        .transform(new CenterCrop(), new RoundedCorners(48))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(background);
            }

            @Override
            public void onClick(View v) {
                if(v.getId() == R.id.btn_deleteCard){
                    deleteCardInfo(getAdapterPosition());
                }
            }
        }

        void addCardInfo(CardInfo card){
            cardInfoList.add(card);
            notifyItemInserted(cardInfoList.size() - 1);
            setEditTexts(card);
        }

        void deleteCardInfo(int position){
            cardManager.deleteCard(position);

            Snackbar.make((View) myLayout.getParent(),
                    "Card Deleted.", Snackbar.LENGTH_SHORT).show();

            cardInfoList.remove(position);
            setEditTexts(cardManager.getSavedCard(position));
            notifyItemRemoved(position - 1);
        }
    }
}
