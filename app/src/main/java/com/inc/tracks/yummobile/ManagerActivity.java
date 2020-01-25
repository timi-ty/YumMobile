package com.inc.tracks.yummobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
        ManagerOrdersAdminFragment.OnFragmentInteractionListener,
        ManagerTransportersFragment.OnFragmentInteractionListener,
        ManagerCompletedOrdersFragment.OnFragmentInteractionListener{

    private static int RC_PERMISSION_LOCATION = 4505;

    private static int REQUEST_CHECK_SETTINGS = 8714;


    private String mode;

    private int search_prompt_res;

    ConstraintLayout myLayout;

    FragmentManager fragmentManager;

    ManagerRestaurantsEditorFragment restEditorFragment;

    ManagerRestaurantsFragment managerRestaurantsFragment;

    ManagerMenuFragment managerMenuFragment;

    ManagerTransportersFragment managerTransportersFragment;

    ManagerOrdersFragment managerOrdersFragment;

    ManagerOrdersAdminFragment managerOrdersAdminFragment;

    ManagerCompletedOrdersFragment completedOrdersFragment;

    Toolbar myToolbar;

    private FusedLocationProviderClient fusedLocationClient;

    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

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

        search_prompt_res = R.string.search;

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

        if(hasLocationPermission()){
            createLocationRequest();
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                UserAuth.mCurrentLocation = locationResult.getLastLocation();

                if(managerOrdersFragment != null)
                    managerOrdersFragment.onLocationUpdate();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem mSearch = menu.findItem(R.id.appSearchBar);
        SearchView mSearchView = (SearchView) mSearch.getActionView();
        mSearchView.setQueryHint(getResources().getString(search_prompt_res));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if(managerRestaurantsFragment != null){
                    managerRestaurantsFragment.onSearchQuery(newText);
                }
                if(managerMenuFragment != null){
                    managerMenuFragment.onSearchQuery(newText);
                }
                if(managerOrdersFragment != null){
                    managerOrdersFragment.onSearchQuery(newText);
                }
                if(managerOrdersAdminFragment != null){
                    managerOrdersAdminFragment.onSearchQuery(newText);
                }
                if(managerTransportersFragment != null){
                    managerTransportersFragment.onSearchQuery(newText);
                }
                if(completedOrdersFragment != null){
                    completedOrdersFragment.onSearchQuery(newText);
                }
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void goToHomeFragment(){
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container, ManagerHomeFragment.newInstance());
        fragmentTransaction.commit();
    }

    private void goToManageRestaurantsFragment(){
        if(managerRestaurantsFragment == null){
            managerRestaurantsFragment = ManagerRestaurantsFragment.newInstance();
        }
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        managerRestaurantsFragment)
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
            managerMenuFragment = ManagerMenuFragment.newInstance(restaurantItem);

            FragmentTransaction fragmentTransaction;
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction
                    .replace(R.id.manager_frag_container, managerMenuFragment)
                    .addToBackStack(null);
            fragmentTransaction.commit();
        }
        else {
            String TAG = "ManagerActivity";
            Log.w(TAG, "Could not manage menu because a restaurant could not be found.");
        }
    }

    private void goToManageTransportersFragment(){
        if(managerTransportersFragment == null){
            managerTransportersFragment = ManagerTransportersFragment.newInstance();
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        managerTransportersFragment)
                .addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToManageOrdersFragment(){
        if(managerOrdersFragment == null){
            managerOrdersFragment = ManagerOrdersFragment.newInstance();
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        managerOrdersFragment);
        fragmentTransaction.commit();
    }

    private void goToManageOrdersAdminFragment(){
        if(managerOrdersAdminFragment == null){
            managerOrdersAdminFragment = ManagerOrdersAdminFragment.newInstance();
        }
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        managerOrdersAdminFragment)
                .addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToManageOrderDetailsFragment(OrderItem activeOrder){
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        ManagerOrderDetailsFragment.newInstance(activeOrder))
                .addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void goToCompletedOrdersFragment(UserPrefs transporter){
        if(completedOrdersFragment == null){
            completedOrdersFragment = ManagerCompletedOrdersFragment.newInstance(transporter);
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.manager_frag_container,
                        completedOrdersFragment)
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
            case R.id.btn_manageTransporters:
                goToManageTransportersFragment();
                break;
            case R.layout.fragment_manager_home:
            case R.layout.fragment_manager_restaurant_editor:
            case R.layout.fragment_manager_order_details:
                myToolbar.setVisibility(View.GONE);
                break;
            case R.layout.fragment_manager_restaurants:
                search_prompt_res = R.string.prompt_restaurant_search;
                myToolbar.setTitle(R.string.prompt_restaurant_search);
                myToolbar.setVisibility(View.VISIBLE);
                break;
            case R.layout.fragment_manager_menu:
                search_prompt_res = R.string.prompt_menu_search;
                myToolbar.setTitle(R.string.prompt_menu_search);
                myToolbar.setVisibility(View.VISIBLE);
                break;
            case R.layout.fragment_manager_active_orders:
            case R.layout.fragment_manager_active_orders_admin:
                search_prompt_res = R.string.prompt_order_search;
                myToolbar.setTitle(R.string.prompt_order_search);
                myToolbar.setVisibility(View.VISIBLE);
                break;
            case R.layout.fragment_manager_transporters:
                search_prompt_res = R.string.prompt_search_transporters;
                myToolbar.setTitle(R.string.prompt_search_transporters);
                myToolbar.setVisibility(View.VISIBLE);
                break;
            case R.layout.fragment_manager_completed_orders:
                search_prompt_res = R.string.prompt_search_completed_orders;
                myToolbar.setTitle(R.string.prompt_search_completed_orders);
                myToolbar.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onFragmentInteraction(int interactionId, UserPrefs transporter) {
        goToCompletedOrdersFragment(transporter);
    }

    @Override
    public void onFragmentInteraction(int interactionId, OrderItem activeOrder) {
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
                Snackbar.make(myLayout, "Location Permission Granted.",
                        Snackbar.LENGTH_SHORT).show();

                createLocationRequest();

                startLocationUpdates();
            }
            else{
                Snackbar.make(myLayout, "You Must Grant Location Permission " +
                        "For Effective Management And Delivery.", Snackbar.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10 * 1000);
        locationRequest.setFastestInterval(10 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                Snackbar.make(myLayout, "Waiting For Location Update...",
                        Snackbar.LENGTH_SHORT).show();

                new Handler(getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(ManagerActivity.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        if(location != null && location.getTime() >
                                                Calendar.getInstance().getTimeInMillis() - 2 * 60 * 1000) {

                                            UserAuth.mCurrentLocation = location;
                                        }
                                        else {
                                            Snackbar snack = Snackbar.make(myLayout,
                                                    "Location Update Timed Out.",
                                                    Snackbar.LENGTH_SHORT);
                                            snack.setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
                                            snack.setAction("Try Again", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    createLocationRequest();
                                                }
                                            });
                                            snack.show();
                                        }
                                    }
                                });
                    }
                }, 3000);
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        Snackbar.make(myLayout, "Failed. Turn on your location services and try again.",
                                Snackbar.LENGTH_SHORT).show();

                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(ManagerActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        sendEx.printStackTrace();
                    }
                }
            }
        });
    }

    private boolean hasLocationPermission(){
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
                return false;
            }
            else{
                return true;
            }
        }
        return true;
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
