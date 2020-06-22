package com.inc.tracks.yummobile.user_activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.utils.YumJsonObjectRequest;
import com.inc.tracks.yummobile.components.OrderItem;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.user_fragments.OrderCompleteFragment;
import com.inc.tracks.yummobile.user_fragments.OrderSummaryFragment;
import com.inc.tracks.yummobile.user_fragments.ManageCardsFragment;

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
        OrderSummaryFragment.OnFragmentInteractionListener,
        ManageCardsFragment.OnFragmentInteractionListener{

    private final String ORDER_SUMMARY_FRAG = "order_summary";

    FragmentManager fragmentManager;

    ManageCardsFragment manageCardsFragment;

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

        fragmentManager = getSupportFragmentManager();

        Bundle incomingExtras = getIntent().getExtras();

        if (incomingExtras != null) {
            HashMap orderGroups = (HashMap) incomingExtras.get("cart");

            goToOrderSummaryFragment(orderGroups);
        }

        PaystackSdk.initialize(getApplicationContext());
    }

    private void goToOrderSummaryFragment(HashMap orderSummary){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment_container,
                OrderSummaryFragment.newInstance(orderSummary), ORDER_SUMMARY_FRAG);
        fragmentTransaction.commit();
    }

    private void goToOrderCompleteFragment(HashMap orderSummary){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment_container, OrderCompleteFragment.newInstance(orderSummary));
        fragmentTransaction.commit();
    }

    private void goToManageCardsFragment(){
        if(manageCardsFragment == null){
            manageCardsFragment = ManageCardsFragment.newInstance();
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.order_fragment_container, manageCardsFragment);
        fragmentTransaction.addToBackStack("ManageCards");
        fragmentTransaction.commit();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onFragmentInteraction(int buttonId, HashMap orderGroups, HashMap groupPrices,
                                      HashMap groupDescs, CardInfo paymentMethod) {
        this.orderGroups = orderGroups;
        this.groupPrices = groupPrices;
        this.groupDescs = groupDescs;

        if(paymentMethod == null){
            confirmOrder(orderGroups, groupPrices, groupDescs, false);
            setLoadingUi(false);
        }
        else{
            chargeCard(paymentMethod);
        }
    }

    @Override
    public void onFragmentInteraction(int interactionId) {
        switch (interactionId) {
            case R.id.btn_back:
                onBackPressed();
                checkVisibleFragment();
                break;
            case R.id.btn_addCard:
                goToManageCardsFragment();
                break;
        }
        if(manageCardsFragment != null) manageCardsFragment.cardInteraction(interactionId);
    }

    private void checkVisibleFragment(){
        OrderSummaryFragment orderSummaryFragment = (OrderSummaryFragment)
                fragmentManager.findFragmentByTag(ORDER_SUMMARY_FRAG);
        if (orderSummaryFragment != null) {
            orderSummaryFragment.refreshPaymentOptions();
            Log.d("Adapter", "Refreshed");
        }
    }

    private void chargeCard(CardInfo cardInfo){
        String cardNumber = cardInfo.getCardNumber();
        String cvv = cardInfo.getCvv();
        int expMonth = cardInfo.getExpiryMonth();
        int expYear = cardInfo.getExpiryYear();

        Card card = new Card(cardNumber, expMonth, expYear, cvv);
        if(!card.isValid()){
            Snackbar.make(myLayout,
                    "Invalid Card Details.", Snackbar.LENGTH_SHORT).show();
            return;
        }

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
