package com.inc.tracks.yummobile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ManagerActivity extends AppCompatActivity implements
        ManagerHomeFragment.OnFragmentInteractionListener,
        ManagerRestaurantsFragment.OnFragmentInteractionListener,
        ManagerRestaurantsEditorFragment.OnFragmentInteractionListener,
        ManagerMenuFragment.OnFragmentInteractionListener,
        ManagerOrdersFragment.OnFragmentInteractionListener,
        ManagerOrderDetailsFragment.OnFragmentInteractionListener{

    private final String TAG = "ManagerActivity";

    FragmentManager fragmentManager;

    ManagerRestaurantsEditorFragment restEditorFragment;

    ManagerMenuFragment menuFragment;

    Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        fragmentManager = getSupportFragmentManager();

        myToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(myToolbar);

        goToHomeFragment();
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
                        ManagerOrdersFragment.newInstance())
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
                goToManageOrdersFragment();
                break;
            case R.layout.fragment_manager_home:
            case R.layout.fragment_manager_restaurant_editor:
                myToolbar.setVisibility(View.GONE);
                break;
            case R.layout.fragment_manager_restaurants:
            case R.layout.fragment_manager_menu:
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
}
