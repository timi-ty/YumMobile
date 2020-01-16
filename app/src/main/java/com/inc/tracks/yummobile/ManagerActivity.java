package com.inc.tracks.yummobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ManagerActivity extends AppCompatActivity implements
        ManagerHomeFragment.OnFragmentInteractionListener,
        ManagerRestaurantsFragment.OnFragmentInteractionListener,
        ManagerRestaurantsEditorFragment.OnFragmentInteractionListener,
        ManagerMenuFragment.OnFragmentInteractionListener,
        ManagerOrdersFragment.OnFragmentInteractionListener,
        ManagerOrderDetailsFragment.OnFragmentInteractionListener,
        ManagerOrdersAdminFragment.OnFragmentInteractionListener{

    private final String TAG = "ManagerActivity";

    private String mode;

    FragmentManager fragmentManager;

    ManagerRestaurantsEditorFragment restEditorFragment;

    ManagerMenuFragment menuFragment;

    Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        fragmentManager = getSupportFragmentManager();

        myToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(myToolbar);

        Intent incoming = getIntent();

        if(savedInstanceState == null){
            assert incoming.getExtras() != null;
            mode = incoming.getExtras().getString("mode");
            assert mode != null;

            if(mode.equals("manage")){
                goToHomeFragment();
            }
            else if(mode.equals("transport")){
                goToManageOrdersFragment();
            }
        }
        else{
            mode = savedInstanceState.getString("mode");
            assert mode != null;

            if(mode.equals("manage")){
                goToHomeFragment();
            }
            else if(mode.equals("transport")){
                goToManageOrdersFragment();
            }
        }
    }

    private void goToHomeFragment(){
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container, ManagerHomeFragment.newInstance());
        fragmentTransaction.commit();
    }

    private void goToManageRestaurantsFragment(){
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        ManagerRestaurantsFragment.newInstance())
                .addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToRestaurantEditorFragment(RestaurantItem restaurantItem){
        if(restaurantItem != null){
            restEditorFragment = ManagerRestaurantsEditorFragment.newInstance(restaurantItem);
        }
        else if(restEditorFragment == null){
            restEditorFragment = ManagerRestaurantsEditorFragment.newInstance(new RestaurantItem());
        }
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container, restEditorFragment)
                .addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToMenuEditorFragment(RestaurantItem restaurantItem){
        if(restaurantItem != null){
            menuFragment = ManagerMenuFragment.newInstance(restaurantItem);

            FragmentTransaction fragmentTransaction;
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction
                    .replace(R.id.manager_frag_container, menuFragment)
                    .addToBackStack(null);
            fragmentTransaction.commit();
        }
        else {
            Log.w(TAG, "Could not manage menu because a restaurant could not be found.");
        }
    }

    private void goToManageOrdersFragment(){
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        ManagerOrdersFragment.newInstance());
        fragmentTransaction.commit();
    }

    private void goToManageOrdersAdminFragment(){
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        ManagerOrdersAdminFragment.newInstance())
                .addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToManageOrderDetailsFragment(ActiveOrder activeOrder){
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        ManagerOrderDetailsFragment.newInstance(activeOrder))
                .addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onFragmentInteraction(int interactionId) {
        switch (interactionId){
            case R.id.btn_manageRestaurants:
                goToManageRestaurantsFragment();
                break;
            case R.id.btn_manageOrders:
                goToManageOrdersAdminFragment();
                break;
            case R.layout.fragment_manager_home:
            case R.layout.fragment_manager_restaurant_editor:
            case R.layout.fragment_manager_order_details:
                myToolbar.setVisibility(View.GONE);
                break;
            case R.layout.fragment_manager_restaurants:
            case R.layout.fragment_manager_menu:
            case R.layout.fragment_manager_active_orders:
                myToolbar.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onFragmentInteraction(int interactionId, ActiveOrder activeOrder) {
        goToManageOrderDetailsFragment(activeOrder);
    }

    @Override
    public void onFragmentInteraction(int interactionId, RestaurantItem restaurantItem) {
        switch (interactionId){
            case R.id.fab_addRestaurant:
            case R.id.item_manageRestaurant:
                goToRestaurantEditorFragment(restaurantItem);
                break;
            case R.id.btn_manageMenu:
                goToMenuEditorFragment(restaurantItem);
                break;
        }
    }

    @Override
    public void onFragmentInteraction(int buttonId, MessageDialog messageDialog) {
        messageDialog.show(fragmentManager, "Dialog");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("mode", mode);
        super.onSaveInstanceState(outState);
    }
}
