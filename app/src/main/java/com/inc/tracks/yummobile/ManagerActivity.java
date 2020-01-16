package com.inc.tracks.yummobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class ManagerActivity extends AppCompatActivity implements
        ManagerHomeFragment.OnFragmentInteractionListener,
        ManagerRestaurantsFragment.OnFragmentInteractionListener,
        ManagerRestaurantsEditorFragment.OnFragmentInteractionListener,
        ManagerMenuFragment.OnFragmentInteractionListener,
        ManagerOrdersFragment.OnFragmentInteractionListener,
        ManagerOrderDetailsFragment.OnFragmentInteractionListener,
        ManagerOrdersAdminFragment.OnFragmentInteractionListener{

    private final String TAG = "ManagerActivity";

    private static int RC_PERMISSION_LOCATION = 4505;

    private static int REQUEST_CHECK_SETTINGS = 8714;

    private static int REASON_GEOTAG_RESTAURANT = 2732;

    private static int REASON_SORT_RESTAURANTS = 3273;


    private String mode;

    ConstraintLayout myLayout;

    FragmentManager fragmentManager;

    ManagerRestaurantsEditorFragment restEditorFragment;

    ManagerMenuFragment menuFragment;

    Toolbar myToolbar;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fragmentManager = getSupportFragmentManager();

        myLayout = findViewById(R.id.layout_managerActivity);

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
            case R.id.btn_geoTagRestaurant:
                geoTagCurrentRestaurant();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == RC_PERMISSION_LOCATION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                geoTagCurrentRestaurant();
            }
            else{
                Snackbar.make(myLayout, "You can't auto address " +
                        "without granting this permission", Snackbar.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void geoTagCurrentRestaurant(){
        restEditorFragment.onStartGeoTagAttempt();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)){
                    showPermissionReason();
                }
                else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            RC_PERMISSION_LOCATION);
                }
                return;
            }
        }

        createLocationRequest(REASON_GEOTAG_RESTAURANT);
    }

    protected void createLocationRequest(final int requestReason) {
        final LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                Snackbar.make(myLayout, "Waiting For Location Update...",
                        Snackbar.LENGTH_SHORT).show();

                if(requestReason == REASON_GEOTAG_RESTAURANT){
                    new Handler(getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            fusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(ManagerActivity.this, new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {
                                            if(location != null && location.getTime() >
                                                    Calendar.getInstance().getTimeInMillis() - 2 * 60 * 1000) {

                                                restEditorFragment.geoTagRestaurant(location);
                                            }
                                            else {
                                                Snackbar snack = Snackbar.make(myLayout,
                                                        "Location Update Timed Out.",
                                                        Snackbar.LENGTH_SHORT);
                                                snack.setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
                                                snack.setAction("Try Again", new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        geoTagCurrentRestaurant();
                                                    }
                                                });
                                                snack.show();
                                                restEditorFragment.onEndGeoTagAttempt();
                                            }
                                        }
                                    });
                        }
                    }, 3000);
                }
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        Snackbar.make(myLayout, "Failed. Turn on your location services and try again.",
                                Snackbar.LENGTH_SHORT).show();

                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(ManagerActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }

                restEditorFragment.onEndGeoTagAttempt();
            }
        });
    }

    private void showPermissionReason(){
        MessageDialog reasonDialog = new MessageDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            RC_PERMISSION_LOCATION);
                }
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Snackbar.make(myLayout,
                        R.string.location_permission_consequence, Snackbar.LENGTH_SHORT).show();
            }
        }, getResources().getString(R.string.location_permission_rationale), this);
        reasonDialog.show(fragmentManager, "Dialog");
    }

}
