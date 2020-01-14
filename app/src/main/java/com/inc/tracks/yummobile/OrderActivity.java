package com.inc.tracks.yummobile;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.View;

import java.util.HashMap;


public class OrderActivity extends AppCompatActivity implements
        OrderDetailsFragment.OnFragmentInteractionListener,
        OrderSummaryFragment.OnFragmentInteractionListener{

    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

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
    }

    private void goToOrderDetailsFragment(HashMap orderGroups){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment, OrderDetailsFragment.newInstance(orderGroups));
        fragmentTransaction.commit();
    }

    private void goToOrderSummaryFragment(HashMap orderSummary){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment, OrderSummaryFragment.newInstance(orderSummary));
        fragmentTransaction.commit();
    }

    private void goToOrderCompleteFragment(HashMap orderSummary){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.order_fragment, OrderCompleteFragment.newInstance(orderSummary));
        fragmentTransaction.commit();
    }

    private boolean takePayments(){
        //paystack payment
        return true;
    }

    @Override
    public void onFragmentInteraction(int buttonId, HashMap orderGroups) {
        switch (buttonId){
            case R.id.btn_checkout:
                goToOrderSummaryFragment(orderGroups);
                break;
            case R.id.btn_cardPay:
            case R.id.btn_deliveryPay:
                if(takePayments()){
                    goToOrderCompleteFragment(orderGroups);
                }
                break;
        }
    }
}
