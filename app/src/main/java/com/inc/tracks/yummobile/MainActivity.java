package com.inc.tracks.yummobile;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Toast;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements
        HomeFragment.OnFragmentInteractionListener,
        RestaurantsFragment.OnFragmentInteractionListener{

    private static int REQUEST_CHECK_SETTINGS = 8714;

    private static int RC_PERMISSION_LOCATION = 4505;

    View myLayout;

    FragmentManager fragmentManager;

    HomeFragment homeFragment;
    MenuFragment menuFragment;
    RestaurantsFragment restaurantsFragment;
    ActiveOrdersFragment activeOrdersFragment;

    Menu bottomMenu;

    private FusedLocationProviderClient fusedLocationClient;

    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    goToHomeFragment();
                    return true;
                case R.id.navigation_catalogue:
                    goToRestaurantsFragment();
                    return true;
                case R.id.navigation_orders:
                    goToOrdersFragment();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragmentManager = getSupportFragmentManager();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        myLayout = findViewById(R.id.layout_mainActivity);

        BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
        Toolbar myToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(myToolbar);

        bottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        bottomMenu = bottomNavView.getMenu();

        if(savedInstanceState == null){
            goToHomeFragment();
        }

        initializeSideNav(myToolbar);

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

                if(homeFragment != null)
                    homeFragment.onLocationUpdate();

                if(activeOrdersFragment != null)
                    activeOrdersFragment.onLocationUpdate();
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
        mSearchView.setQueryHint(getResources().getString(R.string.prompt_restaurant_search));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if(homeFragment != null){
                    homeFragment.onSearchQuery(newText);
                }
                if(menuFragment != null){
                    menuFragment.onSearchQuery(newText);
                }
                if(activeOrdersFragment != null){
                    activeOrdersFragment.onSearchQuery(newText);
                }
                if(restaurantsFragment != null){
                    restaurantsFragment.onSearchQuery(newText);
                }
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
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
                        "For Convenient Shopping And Delivery.", Snackbar.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onFragmentInteraction(int interactionId, RestaurantItem restaurantItem) {
        switch (interactionId){
            case R.id.item_nearRestaurant:
            case R.id.item_catalogueRestaurant:
                goToMenuFragment(restaurantItem);
                break;
            case R.id.tv_viewMore:
                goToRestaurantsFragment();
                break;
        }
    }

    @Override
    public void onFragmentInteraction(int interactionId, HashMap orderSummary) {
        goToCart(orderSummary);
    }

    @Override
    public void onFragmentInteraction(int interactionId, String searchText) {
        goToRestaurantsFragment();
        restaurantsFragment.onSearchQuery(searchText);
    }

    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void goToHomeFragment(){
        if(homeFragment == null){
            homeFragment = HomeFragment.newInstance();
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, homeFragment);
        fragmentTransaction.commit();

        bottomMenu.getItem(1).setChecked(true);

        bottomMenu.getItem(0).setTitle(R.string.title_restaurants);
    }

    private void goToMenuFragment(RestaurantItem restaurantItem){
        if(menuFragment == null){
            menuFragment = MenuFragment.newInstance(restaurantItem);
        }
        else if(restaurantItem != null){
            menuFragment.updateActiveRestaurantItem(restaurantItem);
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, menuFragment);
        fragmentTransaction.commit();

        bottomMenu.getItem(0).setChecked(true).setTitle(R.string.title_menu);
    }

    private void goToRestaurantsFragment(){
        if(restaurantsFragment == null){
            restaurantsFragment = RestaurantsFragment.newInstance();
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, restaurantsFragment);
        fragmentTransaction.commit();

        bottomMenu.getItem(0).setChecked(true).setTitle(R.string.title_restaurants);
    }

    private void goToOrdersFragment(){
        if(activeOrdersFragment == null){
            activeOrdersFragment = ActiveOrdersFragment.newInstance();
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, activeOrdersFragment);
        fragmentTransaction.commit();

        bottomMenu.getItem(2).setChecked(true);

        bottomMenu.getItem(0).setTitle(R.string.title_restaurants);
    }

    private void goToCart(HashMap orderSummary){
        Intent orderIntent = new Intent(this, OrderActivity.class);
        orderIntent.putExtra("cart", orderSummary);
        startActivity(orderIntent);
    }

    private void startManagingServices(){
        Intent managerIntent = new Intent(MainActivity.this, ManagerActivity.class);
        managerIntent.putExtra("mode", "manage");
        startActivity(managerIntent);
    }

    private void startTransportingOrders(){
        Intent managerIntent = new Intent(MainActivity.this, ManagerActivity.class);
        managerIntent.putExtra("mode", "transport");
        startActivity(managerIntent);
    }

    private void initializeSideNav(Toolbar mToolbar){
        ActionBarDrawerToggle drawerToggle;
        NavigationView sideNavView;
        final DrawerLayout drawerLayout = findViewById(R.id.layout_mainActivity);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, mToolbar, R.string.open, R.string.close);


        drawerLayout.addDrawerListener(drawerToggle);

        drawerToggle.syncState();

        sideNavView = findViewById(R.id.side_nav_view);

        MenuItem manageRestaurants = sideNavView.getMenu().findItem(R.id.side_nav_manage_services);
        manageRestaurants.setVisible(UserAuth.isAdmin);

        MenuItem transportOrders = sideNavView.getMenu().findItem(R.id.side_nav_transport_orders);
        transportOrders.setVisible(UserAuth.isTransporter);

        sideNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch(id)
                {
                    case R.id.side_nav_home:
                        goToHomeFragment();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.side_nav_orders:
                        goToOrdersFragment();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.side_nav_support:
                        Toast.makeText(MainActivity.this, "Support",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.side_nav_about:
                        Toast.makeText(MainActivity.this, "About",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.side_nav_payment_methods:
                        Toast.makeText(MainActivity.this, "Payment Methods",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.side_nav_sign_out:
                        signOutToLauncher();
                        break;
                    case R.id.side_nav_manage_services:
                        startManagingServices();
                        break;
                    case R.id.side_nav_transport_orders:
                        startTransportingOrders();
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });
    }

    private void signOutToLauncher(){
         FirebaseAuth.getInstance().signOut();

         Intent mainActivityIntent = new Intent(MainActivity.this,
                 Launcher.class);
         startActivity(mainActivityIntent);
         MainActivity.this.finish();
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
                                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        if(location != null && location.getTime() >
                                                Calendar.getInstance().getTimeInMillis() - 2 * 60 * 1000) {

                                            UserAuth.mCurrentLocation = location;

                                            if(homeFragment != null)
                                                homeFragment.onLocationUpdate();

                                            if(activeOrdersFragment != null)
                                                activeOrdersFragment.onLocationUpdate();
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
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        sendEx.printStackTrace();
                    }
                }
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
                        R.string.whine_about_location_permission, Snackbar.LENGTH_SHORT).show();
            }
        }, getResources().getString(R.string.location_permission_encouragement), this);
        reasonDialog.show(fragmentManager, "Dialog");
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
}
