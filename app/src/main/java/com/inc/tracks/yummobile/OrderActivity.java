package com.inc.tracks.yummobile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import co.paystack.android.Paystack;
import co.paystack.android.PaystackSdk;
import co.paystack.android.Transaction;
import co.paystack.android.model.Card;
import co.paystack.android.model.Charge;


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

    private void getCardInfo(){
        addPaymentFragment();
    }


    @Override
    public void onFragmentInteraction(int buttonId, Card card) {
        chargeCard(card);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment paymentFragment = fragmentManager.findFragmentById(R.id.payment_fragment_container);
        assert paymentFragment != null;
        fragmentTransaction.detach(paymentFragment);
        fragmentTransaction.commit();
        findViewById(R.id.order_fragment_container).setAlpha(1.0f);
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
                getCardInfo();
                break;
            case R.id.btn_deliveryPay:
                confirmOrder(orderGroups, groupPrices, groupDescs, false);
                setLoadingUi(false);
                break;
        }
    }

    private void chargeCard(Card card){
        setLoadingUi(true);
        int totalPrice = 0;
        for(Integer groupPrice : groupPrices.values()){
            totalPrice  += groupPrice;
        }

        Charge charge = new Charge();
        charge.setCard(card);
        charge.setAmount(totalPrice * 100);
        charge.setEmail(UserAuth.currentUser.getEmail());

        PaystackSdk.chargeCard(OrderActivity.this, charge, new Paystack.TransactionCallback() {
            @Override
            public void onSuccess(Transaction transaction) {

                RequestQueue queue = Volley.newRequestQueue(OrderActivity.this);
                String url = "https://api.paystack.co/transaction/verify/" + transaction.getReference();

                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer sk_test_9fe2ac2ae3c74affdf4b9d70efbb0184a1b55c04");/**/

                YumJsonObjectRequest jsonRequest = new YumJsonObjectRequest(Request.Method.GET,
                        url, headers,null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                String status = "";
                                try {
                                    status = response.getJSONObject("data").getString("status");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if(status.equals("success")){
                                    confirmOrder(orderGroups, groupPrices, groupDescs, true);
                                }
                                else{
                                    Snackbar.make(myLayout,
                                            "Failed. Could not pay, try again.",
                                            Snackbar.LENGTH_SHORT).show();

                                    goToOrderSummaryFragment(orderGroups);
                                }
                                setLoadingUi(false);

                                Log.d("Paystack Response", response.toString());
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Snackbar.make(myLayout,
                                "Failed. Could not pay, try again.",
                                Snackbar.LENGTH_SHORT).show();

                        setLoadingUi(false);

                        goToOrderSummaryFragment(orderGroups);
                    }
                });

                queue.add(jsonRequest);
            }

            @Override
            public void beforeValidate(Transaction transaction) {
                // This is called only before requesting OTP.
                // Save reference so you may send to server. If
                // error occurs with OTP, you should still verify on server.
            }

            @Override
            public void onError(Throwable error, Transaction transaction) {
                error.printStackTrace();
                setLoadingUi(false);
            }

        });
    }

    @SuppressWarnings("unchecked")
    private void confirmOrder(final HashMap<String, HashMap> orderGroups,
                              final HashMap<String, Integer> groupPrices,
                              final HashMap<String, String> groupDescs,
                              final boolean paidFor){
        setLoadingUi(true);

        FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

        ArrayList<String> restaurantIds = new ArrayList<>(orderGroups.keySet());

        for(String restaurantId : restaurantIds){

            String clientId = UserAuth.currentUser.getUid();
            HashMap orderItems = orderGroups.get(restaurantId);
            Integer cost = groupPrices.get(restaurantId);
            String description = groupDescs.get(restaurantId);

            assert cost != null;

            OrderItem activeOrder = new OrderItem(clientId, restaurantId, orderItems,
                    cost, description, paidFor, Timestamp.now());

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
                                            confirmOrder(orderGroups, groupPrices, groupDescs, paidFor);
                                        }
                                    });

                                    setLoadingUi(false);

                                    goToOrderSummaryFragment(orderGroups);
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
