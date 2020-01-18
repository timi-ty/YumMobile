package com.inc.tracks.yummobile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;


public class PaymentFragment extends Fragment implements
        View.OnClickListener{


    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private EditText txtCardNumber;
    private EditText txtCVV;
    private EditText txtExpiryMonth;
    private EditText txtExpiryYear;

    private Button btnPayNow;
    private Button btnSaveCard;

    private ProgressBar pbLoading;

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
        mListener.onFragmentInteraction(R.layout.fragment_payment);

        btnPayNow = fragView.findViewById(R.id.btn_payNow);
        btnSaveCard = fragView.findViewById(R.id.btn_saveCard);

        txtCardNumber = fragView.findViewById(R.id.txt_cardNumber);
        txtCVV = fragView.findViewById(R.id.txt_CVV);
        txtExpiryMonth = fragView.findViewById(R.id.txt_expiryMonth);
        txtExpiryYear = fragView.findViewById(R.id.txt_expiryYear);

        pbLoading = fragView.findViewById(R.id.pb_paymentFragment);

        btnPayNow.setOnClickListener(this);
        btnSaveCard.setOnClickListener(this);

        return fragView;
    }

    private void onButtonPressed(int buttonId) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_payNow:
                break;
            case R.id.btn_saveCard:
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId);
    }


    private void setLoadingUi(boolean loading){
        txtCardNumber.setEnabled(!loading);
        txtCVV.setEnabled(!loading);
        txtExpiryMonth.setEnabled(!loading);
        txtExpiryYear.setEnabled(!loading);
        btnPayNow.setEnabled(!loading);
        btnSaveCard.setEnabled(!loading);

        pbLoading.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
    }
}
