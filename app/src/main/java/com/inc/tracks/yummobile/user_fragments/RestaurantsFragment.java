package com.inc.tracks.yummobile.user_fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
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


public class RestaurantsFragment extends Fragment{
    private OnFragmentInteractionListener mListener;

    private RestaurantsRVAdapter restaurantsRVAdapter;

    public RestaurantsFragment() {
        // Required empty public constructor
    }


    public static RestaurantsFragment newInstance() {
        RestaurantsFragment fragment = new RestaurantsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_restaurants,
                container, false);

        RecyclerView rvRestaurantList = fragView.findViewById(R.id.rv_restaurantList);

        rvRestaurantList.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
        if(restaurantsRVAdapter == null){
            restaurantsRVAdapter = new RestaurantsRVAdapter();
        }
        if(rvRestaurantList.getAdapter() == null){
            rvRestaurantList.setAdapter(restaurantsRVAdapter);
        }
        restaurantsRVAdapter.getFilter().filter("");

        return fragView;
    }

    private void onButtonPressed(int buttonId, RestaurantItem restaurantItem) {
        if (mListener != null) {
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
    }

    public interface OnFragmentInteractionListener {
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
        private List<RestaurantItem> restaurantListFiltered = new ArrayList<>();

        RestaurantsRVAdapter() {
            FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

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
                    .inflate(R.layout.item_catalogue_restaurant, viewGroup, false);
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
            RestaurantItem restaurantItem;
            TextView tvName;
            TextView tvAddress;
            TextView tvRange;
            ImageView imgLogo;
            ConstraintLayout container;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_restaurantName);
                tvAddress = itemView.findViewById(R.id.tv_restaurantAddress);
                tvRange = itemView.findViewById(R.id.tv_priceRange);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                container = itemView.findViewById(R.id.item_catalogueRestaurant);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                onButtonPressed(v.getId(), restaurantItem);
            }

            void bindView(RestaurantItem restaurantItem){
                this.restaurantItem = restaurantItem;

                tvName.setText(restaurantItem.getName());

                tvAddress.setText(restaurantItem.getAddress());
                Integer[] range = restaurantItem.getPriceRange().toArray(new Integer[2]);
                String s_range = "₦" + range[0]
                        + " - ₦" + range[1];
                tvRange.setText(s_range);

                refreshThumbnail(restaurantItem);
            }

            private void refreshThumbnail(RestaurantItem item) {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(item.getImgRef());

                    GlideApp.with(imgLogo.getContext())
                            .load(imageRef)
                            .transform(new CenterCrop(), new RoundedCorners(16))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imgLogo);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
