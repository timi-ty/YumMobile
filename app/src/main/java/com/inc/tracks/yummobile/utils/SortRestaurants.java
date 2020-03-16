package com.inc.tracks.yummobile.utils;

import android.location.Location;

import com.google.firebase.firestore.GeoPoint;
import com.inc.tracks.yummobile.components.RestaurantItem;

import java.util.Comparator;

public class SortRestaurants implements Comparator<RestaurantItem> {

    private final int EARTH_RADIUS = 6378137; // approximate Earth radius, *in meters*

    private Location currentLoc;

    public SortRestaurants(Location current){
        currentLoc = current;
    }

    @Override
    public int compare(final RestaurantItem restaurant1, final RestaurantItem restaurant2) {
        GeoPoint location1 = restaurant1.getLocation();
        GeoPoint location2 = restaurant2.getLocation();

        double distanceToRestaurant1;
        double distanceToRestaurant2;

        if(location1 != null){
            double lat1 = location1.getLatitude();
            double lon1 = location1.getLongitude();
            distanceToRestaurant1 = distance(currentLoc.getLatitude(), currentLoc.getLongitude(), lat1, lon1);
        }
        else {
            // since geo tag is null, assume greatest distance possible.
            distanceToRestaurant1 = EARTH_RADIUS * 2 * Math.PI;
        }

        if(location2 != null){
            double lat2 = location2.getLatitude();
            double lon2 = location2.getLongitude();
            distanceToRestaurant2 = distance(currentLoc.getLatitude(), currentLoc.getLongitude(), lat2, lon2);
        }
        else {
            // since geo tag is null, assume greatest distance possible.
            distanceToRestaurant2 = EARTH_RADIUS * 2 * Math.PI;
        }

        return (int) (distanceToRestaurant1 - distanceToRestaurant2);
    }

    private double distance(double fromLat, double fromLon, double toLat, double toLon) {
        double deltaLat = toLat - fromLat;
        double deltaLon = toLon - fromLon;
        double angle = 2 * Math.asin( Math.sqrt(
                Math.pow(Math.sin(deltaLat/2), 2) +
                        Math.cos(fromLat) * Math.cos(toLat) *
                                Math.pow(Math.sin(deltaLon/2), 2) ) );
        return EARTH_RADIUS * angle;
    }
}
