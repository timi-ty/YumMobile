package com.inc.tracks.yummobile;

import android.location.Location;

import com.google.firebase.firestore.GeoPoint;

import java.util.Comparator;

public class SortActiveOrders implements Comparator<ActiveOrder> {

    private final int EARTH_RADIUS = 6378137; // approximate Earth radius, *in meters*

    private Location currentLoc;

    SortActiveOrders(Location current){
        currentLoc = current;
    }

    @Override
    public int compare(final ActiveOrder activeOrder1, final ActiveOrder activeOrder2) {
        GeoPoint location1 = activeOrder1.getClientLocation();
        GeoPoint location2 = activeOrder2.getClientLocation();

        double distanceToClient1;
        double distanceToClient2;

        if(location1 != null){
            double lat1 = location1.getLatitude();
            double lon1 = location1.getLongitude();
            distanceToClient1 = distance(currentLoc.getLatitude(), currentLoc.getLongitude(), lat1, lon1);
        }
        else {
            // since geo tag is null, assume greatest distance possible.
            distanceToClient1 = EARTH_RADIUS * 2 * Math.PI;
        }

        if(location2 != null){
            double lat2 = location2.getLatitude();
            double lon2 = location2.getLongitude();
            distanceToClient2 = distance(currentLoc.getLatitude(), currentLoc.getLongitude(), lat2, lon2);
        }
        else {
            // since geo tag is null, assume greatest distance possible.
            distanceToClient2 = EARTH_RADIUS * 2 * Math.PI;
        }

        return (int) (distanceToClient1 - distanceToClient2);
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
