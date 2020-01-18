package com.inc.tracks.yummobile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;

import co.paystack.android.PaystackSdk;


public class OrderActivity extends AppCompatActivity implements
        OrderDetailsFragment.OnFragmentInteractionListener,
        OrderSummaryFragment.OnFragmentInteractionListener,
        PaymentFragment.OnFragmentInteractionListener{

    FragmentManager fragmentManager;

    ConstraintLayout myLayout;

    ProgressBar pbLoading;

    HashMap<String, HashMap> orderGroups;
    HashMap<String, Integer> groupPrices;
    HashMap<String, String> groupDescs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        myLayout = findViewById(R.id.layout_orderActivity);

        pbLoading = findViewById(R.id.pb_orderActivity);

        Toolbar myToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(myToolbar);

        fragmentManager = getSupportFragmentManager();

        Bundle incomingExtras = getIntent().getExtras();

        if (incomingExtras != null) {
            HashMap orderGroups = (HashMap) incomingExtras.get("cart");

            goToOrderDetailsFragment(orderGroups);
        }

        myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        PaystackSdk.initialize(getApplicationContext());
    }

    private void goToOrderDetailsFragment(HashMap orderGroups){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment_container, OrderDetailsFragment.newInstance(orderGroups));
        fragmentTransaction.commit();
    }

    private void goToOrderSummaryFragment(HashMap orderSummary){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment_container, OrderSummaryFragment.newInstance(orderSummary));
        fragmentTransaction.commit();
    }

    private void goToOrderCompleteFragment(HashMap orderSummary){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment_container, OrderCompleteFragment.newInstance(orderSummary));
        fragmentTransaction.commit();
    }

    private void addPaymentFragment(){
        findViewById(R.id.order_fragment_container).setAlpha(0.4f);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.payment_fragment_container, PaymentFragment.newInstance());
        fragmentTransaction.commit();
    }

    private void takePayments(){
        addPaymentFragment();
    }


    @Override
    public void onFragmentInteraction(int buttonId) {
        if(buttonId == R.id.btn_payNow){
            confirmOrder(orderGroups, groupPrices, groupDescs);
        }
    }

    @Override
    public void onFragmentInteraction(int buttonId, HashMap orderGroups) {
        if (buttonId == R.id.btn_checkout) {
            goToOrderSummaryFragment(orderGroups);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onFragmentInteraction(int buttonId, HashMap orderGroups,
                                      HashMap groupPrices, HashMap groupDescs) {
        this.orderGroups = orderGroups;
        this.groupPrices = groupPrices;
        this.groupDescs = groupDescs;

        switch (buttonId){
            case R.id.btn_cardPay:
                takePayments();
                break;
            case R.id.btn_deliveryPay:
                confirmOrder(orderGroups, groupPrices, groupDescs);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void confirmOrder(final HashMap<String, HashMap> orderGroups,
                              final HashMap<String, Integer> groupPrices,
                              final HashMap<String, String> groupDescs){
        setLoadingUi(true);

        FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

        ArrayList<String> restaurantIds = new ArrayList<>(orderGroups.keySet());

        for(String restaurantId : restaurantIds){

            String clientId = UserAuth.currentUser.getUid();
            HashMap orderItems = orderGroups.get(restaurantId);
            Integer cost = groupPrices.get(restaurantId);
            String description = groupDescs.get(restaurantId);

            assert cost != null;

            ActiveOrder activeOrder = new ActiveOrder(clientId, restaurantId, orderItems,
                    cost, description, Timestamp.now());

            if(UserAuth.mCurrentLocation != null){
                activeOrder.setClientLocation(new GeoPoint
                        (UserAuth.mCurrentLocation.getLatitude(),
                                UserAuth.mCurrentLocation.getLongitude()));
            }

            String uniqueOrderId = activeOrder.getClientId() + activeOrder.getRestaurantId()
                    + activeOrder.getTimestamp().hashCode();

                    fireDB.collection("activeOrders")
                            .document(uniqueOrderId)
                            .set(activeOrder)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    setLoadingUi(false);
                                    goToOrderCompleteFragment(orderGroups);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar snackbar = Snackbar.make(myLayout,
                                            "Order could not be made", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(getResources()
                                                    .getColor(R.color.colorPrimaryDark));

                                    snackbar.setAction("Try Again", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            confirmOrder(orderGroups, groupPrices, groupDescs);
                                        }
                                    });

                                    setLoadingUi(false);
                                }
                            });
        }
    }

    private void setLoadingUi(boolean loading){
        myLayout.setEnabled(!loading);

        pbLoading.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);

        myLayout.setAlpha(loading ? 0.4f : 1.0f);
    }
}
