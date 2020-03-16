package com.inc.tracks.yummobile.user_fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;


import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.utils.GlideApp;

import java.util.Locale;


public class CardFragment extends Fragment {
    private static final String ARG_CARD_INFO = "param1";


    private CardInfo cardInfo;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.item_card, container);

        TextView txtCardNumber = fragView.findViewById(R.id.txt_cardNumber);
        TextView txtCardCVV = fragView.findViewById(R.id.txt_cvv);
        TextView txtCardExpiry = fragView.findViewById(R.id.txt_cardExpiry);

        ImageView background = fragView.findViewById(R.id.background);

        GlideApp.with(background.getContext())
                .load(R.drawable.card_bg)
                .transform(new CenterCrop(), new RoundedCorners(48))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(background);


        if(cardInfo == null){

        }
        else{
            txtCardNumber.setText(cardInfo.getCardNumber());
            txtCardCVV.setText(cardInfo.getCvv());
            txtCardExpiry.setText(String.format(Locale.ENGLISH, "%d/%d", cardInfo.getExpiryMonth()
                    , cardInfo.getExpiryYear()));
        }


        return fragView;
    }
}
