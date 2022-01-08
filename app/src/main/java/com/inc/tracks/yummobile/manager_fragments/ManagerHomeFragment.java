package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.AppInfo;


public class ManagerHomeFragment extends Fragment implements View.OnClickListener{

    private OnFragmentInteractionListener mListener;
    private ConstraintLayout mLayout;
    private EditText txtCustomerSupportPhone;
    private ImageButton btnSaveSupportPhone;
    private ProgressBar pbSaveSupportPhone;

    TextWatcher customerSupportPhoneWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateUi();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    public ManagerHomeFragment() {
        // Required empty public constructor
    }


    public static ManagerHomeFragment newInstance() {
        ManagerHomeFragment fragment = new ManagerHomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_home,
                container, false);

        mLayout = fragView.findViewById(R.id.layout_managerHome);

        fragView.findViewById(R.id.btn_manageRestaurants).setOnClickListener(this);

        fragView.findViewById(R.id.btn_manageOrders).setOnClickListener(this);

        fragView.findViewById(R.id.btn_manageTransporters).setOnClickListener(this);

        txtCustomerSupportPhone = fragView.findViewById(R.id.edit_supportPhone);
        txtCustomerSupportPhone.setText(AppInfo.getCachedInstance().getCustomerSupportPhone());
        txtCustomerSupportPhone.addTextChangedListener(customerSupportPhoneWatcher);

        pbSaveSupportPhone = fragView.findViewById(R.id.pb_saveSupportPhone);

        btnSaveSupportPhone = fragView.findViewById(R.id.btn_saveSupportHotline);
        btnSaveSupportPhone.setOnClickListener(this);

        mListener.onFragmentInteraction(R.layout.fragment_manager_home);

        updateUi();

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


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_saveSupportHotline){
            saveCustomerSupportPhoneToFirestore(txtCustomerSupportPhone.getText().toString());
        }
        else {
            onButtonPressed(v.getId());
        }
    }

    private void saveCustomerSupportPhoneToFirestore(String phone){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef = db.collection("appInfo")
                .document("main");

        AppInfo.getCachedInstance().setCustomerSupportPhone(phone);

        pbSaveSupportPhone.setVisibility(View.VISIBLE);
        updateUi();

        docRef.set(AppInfo.getCachedInstance(), SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pbSaveSupportPhone.setVisibility(View.INVISIBLE);

                        Snackbar.make(mLayout,
                                "Customer Support Number Saved Successfully.", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pbSaveSupportPhone.setVisibility(View.INVISIBLE);
                        AppInfo.getCachedInstance().revertToOldCustomerSupportPhone();
                        updateUi();

                        Snackbar.make(mLayout,
                                "An error occurred. Customer Support Number Not Saved.",
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUi(){
        boolean _check = txtCustomerSupportPhone.getText().toString()
                .equals(AppInfo.getCachedInstance().getCustomerSupportPhone());

        btnSaveSupportPhone.setVisibility(_check ? View.INVISIBLE : View.VISIBLE);
    }
}
