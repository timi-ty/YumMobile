package com.inc.tracks.yummobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;


public class OrderCompleteFragment extends Fragment implements View.OnClickListener{

    private static final String ARG_SUMMARY = "orderSummary";

    private EditText txtNameOrder;

    private Button btnSaveOrder;

    private Button btnGoHome;

    private ProgressBar pbSaveOrder;

    private HashMap orderSummary;

    public OrderCompleteFragment() {
        // Required empty public constructor
    }


    public static OrderCompleteFragment newInstance(HashMap orderSummary) {
        OrderCompleteFragment fragment = new OrderCompleteFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SUMMARY, orderSummary);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderSummary = (HashMap) getArguments().getSerializable(ARG_SUMMARY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater
                .inflate(R.layout.fragment_order_complete, container, false);

        txtNameOrder = fragView.findViewById(R.id.txt_nameOrder);

        btnSaveOrder = fragView.findViewById(R.id.btn_saveOrder);

        btnGoHome = fragView.findViewById(R.id.btn_goHome);

        pbSaveOrder = fragView.findViewById(R.id.pb_saveOrder);

        btnSaveOrder.setOnClickListener(this);
        btnGoHome.setOnClickListener(this);

        setLoadingUi(false);

        return fragView;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_saveOrder:
                if(verifyUserInput()){
                    saveOrder();
                }
                break;
            case R.id.btn_goHome:
                goHome();
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void saveOrder(){
        setLoadingUi(true);
        FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

        RecentOrder recentOrder = new RecentOrder(txtNameOrder.getText().toString(), orderSummary);

        DocumentReference docRef = fireDB.collection("users")
                .document(UserAuth.currentUser.getUid())
                .collection("recentOrders")
                .document();

        recentOrder.setId(docRef.getId());

        docRef.set(recentOrder)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        setLoadingUi(false);
                        setInactive();
                        final Snackbar snackbar = Snackbar.make((View) btnSaveOrder.getParent(),
                                "Order saved.", Snackbar.LENGTH_SHORT);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackbar.dismiss();
                            }
                        });
                        snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                goHome();
                                super.onDismissed(transientBottomBar, event);
                            }
                        });
                        snackbar.setActionTextColor(getResources()
                                .getColor(R.color.colorPrimaryDark));
                        snackbar.show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoadingUi(false);
                        final Snackbar snackbar = Snackbar.make((View) btnSaveOrder.getParent(),
                                "Failed to save order.", Snackbar.LENGTH_SHORT);
                        snackbar.setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                saveOrder();
                                snackbar.dismiss();
                            }
                        });
                        snackbar.setActionTextColor(getResources()
                                .getColor(R.color.colorPrimaryDark));
                        snackbar.show();
                    }
                });
    }

    private void goHome(){
        Intent homeIntent = new Intent(getActivity(), MainActivity.class);
        startActivity(homeIntent);
        Objects.requireNonNull(getActivity()).finish();
    }

    private boolean verifyUserInput(){
        boolean isVerified = true;

        String name = txtNameOrder.getText().toString();

        if(name.equals("")){
            txtNameOrder.setHint("You must name the order to save it.");
            txtNameOrder.setHintTextColor(getResources().getColor(R.color.colorError));
            isVerified = false;
        }

        return isVerified;
    }


    private void setLoadingUi(boolean loading){
        btnSaveOrder.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        btnGoHome.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);

        pbSaveOrder.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
    }

    private void setInactive(){
        btnSaveOrder.setVisibility(View.INVISIBLE);
        btnGoHome.setVisibility(View.INVISIBLE);
    }
}
