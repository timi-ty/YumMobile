package com.inc.tracks.yummobile.user_fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.utils.GlideApp;

import java.util.Locale;


public class CardFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_CARD_INFO = "param1";

    private OnFragmentInteractionListener mListener;
    private CardInfo cardInfo;
    private View fragView;

    public CardFragment() {
        // Required empty public constructor
    }

    public static CardFragment newInstance(CardInfo cardInfo) {
        CardFragment fragment = new CardFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CARD_INFO, cardInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cardInfo = (CardInfo) getArguments().getSerializable(ARG_CARD_INFO);
        }
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragView = inflater.inflate(R.layout.item_card, container);

        updateCardInfo(cardInfo);

        return fragView;
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

    void updateCardInfo(CardInfo cardInfo){
        TextView txtCardNumber = fragView.findViewById(R.id.txt_cardNumber);
        TextView txtHolderName = fragView.findViewById(R.id.txt_cardHolderName);
        TextView txtCardExpiry = fragView.findViewById(R.id.txt_cardExpiry);
        ImageView imgCardVendor = fragView.findViewById(R.id.img_cardVendor);
        ImageView emptyView = fragView.findViewById(R.id.img_emptyView);

        ImageView background = fragView.findViewById(R.id.background);
        GlideApp.with(background.getContext())
                .load(R.drawable.card_bg)
                .transform(new CenterCrop(), new RoundedCorners(48))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(background);


        if(cardInfo == null){
            emptyView.setVisibility(View.VISIBLE);
            txtCardNumber.setVisibility(View.INVISIBLE);
            txtHolderName.setVisibility(View.INVISIBLE);
            txtCardExpiry.setVisibility(View.INVISIBLE);
            imgCardVendor.setVisibility(View.INVISIBLE);
            fragView.findViewById(R.id.txt_valid).setVisibility(View.INVISIBLE);
            fragView.findViewById(R.id.img_cardChip).setVisibility(View.INVISIBLE);
            fragView.findViewById(R.id.txt_cardType).setVisibility(View.INVISIBLE);
            fragView.findViewById(R.id.btn_deleteCard).setVisibility(View.INVISIBLE);

            background.setEnabled(false);
        }
        else{
            emptyView.setVisibility(View.GONE);
            txtCardNumber.setVisibility(View.VISIBLE);
            txtHolderName.setVisibility(View.VISIBLE);
            txtCardExpiry.setVisibility(View.VISIBLE);
            imgCardVendor.setVisibility(View.VISIBLE);
            fragView.findViewById(R.id.txt_valid).setVisibility(View.VISIBLE);
            fragView.findViewById(R.id.img_cardChip).setVisibility(View.VISIBLE);
            fragView.findViewById(R.id.txt_cardType).setVisibility(View.VISIBLE);
            fragView.findViewById(R.id.btn_deleteCard).setVisibility(View.VISIBLE);

            background.setEnabled(true);

            txtCardNumber.setText(formatCardNumber(cardInfo.getCardNumber()));
            txtHolderName.setText(cardInfo.getHolderName());
            txtCardExpiry.setText(String.format(Locale.ENGLISH, "%d/%d", cardInfo.getExpiryMonth()
                    , cardInfo.getExpiryYear()));
            fragView.findViewById(R.id.btn_deleteCard).setOnClickListener(this);
        }

        this.cardInfo = cardInfo;
        Bundle args = new Bundle();
        args.putSerializable(ARG_CARD_INFO, this.cardInfo);
        setArguments(args);
    }

    @Override
    public void onClick(View v) {
        mListener.onFragmentInteraction(v.getId());
    }

    public interface OnFragmentInteractionListener{
        void onFragmentInteraction(int buttonId);
    }
}
