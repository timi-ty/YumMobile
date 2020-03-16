package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.RestaurantItem;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.utils.GlideApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ManagerRestaurantsFragment extends Fragment implements View.OnClickListener{

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private RestaurantsRVAdapter restaurantsRVAdapter;

    public ManagerRestaurantsFragment() {
        // Required empty public constructor
    }


    public static ManagerRestaurantsFragment newInstance() {
        ManagerRestaurantsFragment fragment = new ManagerRestaurantsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_restaurants, container, false);
        myLayout = fragView.findViewById(R.id.layout_restManager);
        RecyclerView rvRestaurants = fragView.findViewById(R.id.rv_allRestaurants);
        FloatingActionButton fab_addRestaurant = fragView.findViewById(R.id.fab_addRestaurant);

        rvRestaurants.setLayoutManager(new LinearLayoutManager(getContext()));
        if(restaurantsRVAdapter == null){
            restaurantsRVAdapter = new RestaurantsRVAdapter();
        }
        restaurantsRVAdapter.getFilter().filter("");
        rvRestaurants.setAdapter(restaurantsRVAdapter);

        fab_addRestaurant.setOnClickListener(this);

        mListener.onFragmentInteraction(R.layout.fragment_manager_restaurants);

        return fragView;
    }

    private void onButtonPressed(int buttonId, RestaurantItem restaurantItem) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId);
            mListener.onFragmentInteraction(buttonId, restaurantItem);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab_addRestaurant){
            onButtonPressed(v.getId(), new RestaurantItem());
        }
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int interactionId);
        void onFragmentInteraction(int interactionId, RestaurantItem restaurantItem);
    }

    public void onSearchQuery(String newText){
        if(isVisible()){
            restaurantsRVAdapter.getFilter().filter(newText);
        }
    }

    public class RestaurantsRVAdapter extends
            RecyclerView.Adapter<RestaurantsRVAdapter.RstViewHolder> implements Filterable {

        private final String TAG = "FireStore";
        private ArrayList<RestaurantItem> restaurantList = new ArrayList<>();
        List<RestaurantItem> restaurantListFiltered = new ArrayList<>();
        FirebaseFirestore fireDB;

        RestaurantsRVAdapter() {
            fireDB = FirebaseFirestore.getInstance();

            EventListener<QuerySnapshot> dataChangedListener =
                    new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot snapshots,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w(TAG, "listen:error", e);
                                return;
                            }

                            if (snapshots == null) {
                                Log.w(TAG, "snapshot not found:error");
                                return;
                            }

                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                RestaurantItem restaurantItem;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        restaurantItem.setId(dc.getDocument().getId());

                                        restaurantList.add(restaurantItem);

                                        notifyItemInserted(restaurantList.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        for(RestaurantItem item : restaurantList){
                                            if(item.getId().equals(restaurantItem.getId())){
                                                position = restaurantList.indexOf(item);
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        for(RestaurantItem item : restaurantList){
                                            if(item.getId().equals(restaurantItem.getId())){
                                                position = restaurantList.indexOf(item);
                                                Log.d(TAG, "Removed Restaurant Notified!: " + item.getId()
                                                        + " => " + restaurantItem.getId() + " => " + restaurantList.indexOf(item));
                                            }
                                        }

                                        if(position >= 0){
                                            restaurantList.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("restaurants").addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_restaurant, viewGroup, false);
            return new RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int position) {
            RestaurantItem restaurantItem = restaurantListFiltered.get(position);
            viewHolder.bindView(restaurantItem);
        }

        @Override
        public int getItemCount() {
            return restaurantListFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();

                    if (charString.isEmpty()) {
                        restaurantListFiltered = restaurantList;
                    } else {
                        List<RestaurantItem> filteredList = new ArrayList<>();
                        for (RestaurantItem item : restaurantList) {
                            if (item.getName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(item);
                            }
                        }

                        restaurantListFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = restaurantListFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    restaurantListFiltered = (ArrayList<RestaurantItem>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }

        class RstViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvName;
            TextView tvAddress;
            TextView tvRange;
            ImageView imgLogo;

            RestaurantItem restaurantItem;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_restaurantName);
                tvAddress = itemView.findViewById(R.id.tv_restaurantAddress);
                tvRange = itemView.findViewById(R.id.tv_priceRange);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);

                ImageButton btnDelete = itemView.findViewById(R.id.btn_deleteRestaurant);

                itemView.setOnClickListener(this);
                btnDelete.setOnClickListener(this);
            }

            void bindView(RestaurantItem restaurantItem){
                this.restaurantItem = restaurantItem;

                tvName.setText(restaurantItem.getName());
                String range = "₦" + restaurantItem.getPriceRange().get(0)
                        + " - " + restaurantItem.getPriceRange().get(1);
                tvAddress.setText(range);
                tvRange.setText(restaurantItem.getAddress());

                if(restaurantItem.getLocation() != null){
                    tvRange.setOnClickListener(this);

                    tvRange.setCompoundDrawablesRelativeWithIntrinsicBounds
                            (0, 0, R.drawable.ic_pin, 0);
                }
                else{
                    tvRange.setCompoundDrawablesRelativeWithIntrinsicBounds
                            (0, 0, 0, 0);
                }

                refreshThumbnail(restaurantItem);
            }

            private void refreshThumbnail(RestaurantItem restaurantItem) {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(restaurantItem.getImgRef());

                    GlideApp.with(imgLogo.getContext())
                            .load(imageRef)
                            .into(imgLogo);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_deleteRestaurant:
                        deleteRestaurant();
                        break;
                    case R.id.item_manageRestaurant:
                        onButtonPressed(v.getId(), restaurantItem);
                        break;
                    case R.id.tv_priceRange:
                        findInGMaps(restaurantItem);
                        break;
                }
            }

            private void findInGMaps(RestaurantItem restaurantItem){
                String latitude = "" + restaurantItem.getLocation().getLatitude();
                String longitude = "" + restaurantItem.getLocation().getLongitude();

                Uri gmmIntentUri = Uri.parse("geo:"+latitude+","+longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(Objects.requireNonNull(getActivity())
                        .getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
                else{
                    Snackbar.make(myLayout, "Install Google Maps to find this restaurant on the map.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            private void deleteRestaurant(){
                Snackbar.make(myLayout,
                        "Delete " + tvName.getText() + " Permanently?", Snackbar.LENGTH_LONG)
                        .setAction("Delete", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                fireDB.collection("restaurants")
                                        .document(restaurantItem.getId())
                                        .delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Snackbar.make(myLayout,
                                                        "Restaurant Deleted Successfully.", Snackbar.LENGTH_LONG).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(myLayout,
                                                        "Failed. Could Not Delete.", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        })
                        .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
                        .show();
            }
        }
    }
}
