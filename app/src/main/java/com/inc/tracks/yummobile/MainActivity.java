package com.inc.tracks.yummobile;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements
        HomeFragment.OnFragmentInteractionListener{

    FragmentManager fragmentManager;

    HomeFragment homeFragment;
    CatalogueFragment catalogueFragment;
    ActiveOrdersFragment activeOrdersFragment;

    Menu bottomMenu;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    goToHomeFragment();
                    return true;
                case R.id.navigation_catalogue:
                    goToCatalogueFragment((RestaurantItem) null);
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

        BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
        Toolbar myToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(myToolbar);

        bottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        bottomMenu = bottomNavView.getMenu();

        if(savedInstanceState == null){
            goToHomeFragment();
        }

        initializeSideNav(myToolbar);
    }


    @Override
    public void onFragmentInteraction(int interactionId, RestaurantItem restaurantItem) {
        switch (interactionId){
            case R.id.item_nearRestaurant:
            case R.id.tv_viewMore:
                goToCatalogueFragment(restaurantItem);
                break;
        }
    }

    @Override
    public void onFragmentInteraction(int interactionId, HashMap orderSummary) {
        goToCatalogueFragment(orderSummary);
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
    }

    private void goToCatalogueFragment(RestaurantItem restaurantItem){
        if(catalogueFragment == null){
            catalogueFragment = CatalogueFragment.newInstance(restaurantItem);
        }
        else if(restaurantItem != null){
            catalogueFragment.updateActiveRestaurantItem(restaurantItem);
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, catalogueFragment);
        fragmentTransaction.commit();

        bottomMenu.getItem(0).setChecked(true);
    }


    @SuppressWarnings("unchecked")
    private void goToCatalogueFragment(HashMap orderGroups){
        if(catalogueFragment == null){
            catalogueFragment = CatalogueFragment.newInstance(orderGroups);
        }
        else if(orderGroups != null){
            catalogueFragment.updateOrderGroups(orderGroups);
        }

        FragmentTransaction fragmentTransaction;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, catalogueFragment);
        fragmentTransaction.commit();

        bottomMenu.getItem(0).setChecked(true);
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
    }

    private void initializeSideNav(Toolbar mToolbar){
        ActionBarDrawerToggle drawerToggle;
        NavigationView sideNavView;
        final DrawerLayout drawerLayout = findViewById(R.id.side_drawer);
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
                    case R.id.side_nav_address:
                        Toast.makeText(MainActivity.this, "My Address",
                                Toast.LENGTH_SHORT).show();
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
}
