package com.inc.tracks.yummobile;

import android.location.Location;

import java.util.Comparator;

public class SortRestaurants implements Comparator<RestaurantItem> {
    Location currentLoc;

    public SortRestaurants(Location current){
        currentLoc = current;
    }

    @Override
    public int compare(final RestaurantItem restaurant1, final RestaurantItem restaurant2) {
        double lat1 = restaurant1.getLocation().getLatitude();
        double lon1 = restaurant1.getLocation().getLongitude();
        double lat2 = restaurant2.getLocation().getLatitude();
        double lon2 = restaurant2.getLocation().getLongitude();

        double distanceToPlace1 = distance(currentLoc.getLatitude(), currentLoc.getLongitude(), lat1, lon1);
        double distanceToPlace2 = distance(currentLoc.getLatitude(), currentLoc.getLongitude(), lat2, lon2);
        return (int) (distanceToPlace1 - distanceToPlace2);
    }

    public double distance(double fromLat, double fromLon, double toLat, double toLon) {
        double radius = 6378137;   // approximate Earth radius, *in meters*
        double deltaLat = toLat - fromLat;
        double deltaLon = toLon - fromLon;
        double angle = 2 * Math.asin( Math.sqrt(
                Math.pow(Math.sin(deltaLat/2), 2) +
                        Math.cos(fromLat) * Math.cos(toLat) *
                                Math.pow(Math.sin(deltaLon/2), 2) ) );
        return radius * angle;
    }
}
