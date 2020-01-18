package com.inc.tracks.yummobile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class HomeFragment extends Fragment implements View.OnClickListener{

    private OnFragmentInteractionListener mListener;

    private FirebaseFirestore fireDB;

    private RestaurantsRVAdapter restaurantsRVAdapter;

    private TextView txtUserGreeting;

    private View myLayout;

    public HomeFragment() {
      // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
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
        View fragView = inflater.inflate(R.layout.fragment_home,
                container, false);

        myLayout = fragView;

        txtUserGreeting = fragView.findViewById(R.id.txt_userGreeting);

        RecyclerView rvRestaurantList = fragView.findViewById(R.id.rv_nearRestaurants);
        RecyclerView rvRecentOrders = fragView.findViewById(R.id.rv_recentOrders);

        rvRestaurantList.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
        if(restaurantsRVAdapter == null){
            restaurantsRVAdapter = new RestaurantsRVAdapter();
        }
        if(rvRestaurantList.getAdapter() == null){
            rvRestaurantList.setAdapter(restaurantsRVAdapter);
        }
        restaurantsRVAdapter.getFilter().filter("");

        if(fragView.getId() == R.id.layout_landHome){
            rvRecentOrders.setLayoutManager(new LinearLayoutManager(fragView.getContext(),
                    LinearLayoutManager.VERTICAL, false));
            rvRecentOrders.setAdapter(new RecentOrdersRVAdapter());
        }
        else{
            rvRecentOrders.setLayoutManager(new LinearLayoutManager(fragView.getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
            rvRecentOrders.setAdapter(new RecentOrdersRVAdapter());

            fragView.findViewById(R.id.tv_viewMore).setOnClickListener(this);
        }

        greetUser();

        return fragView;
    }

    private void onButtonPressed(int buttonId, RestaurantItem restaurantItem) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId, restaurantItem);
        }
    }

    private void onButtonPressed(int buttonId, HashMap orderSummary) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId, orderSummary);
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
        if (v.getId() == R.id.tv_viewMore) {
            onButtonPressed(v.getId(), (RestaurantItem) null);
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int interactionId, RestaurantItem restaurantItem);
        void onFragmentInteraction(int interactionId, HashMap orderSummary);
    }

    private void greetUser(){
        String userGreeting = "Hello, " + UserAuth.userPrefs.getUserName();
        if (txtUserGreeting != null) {
            txtUserGreeting.setText(userGreeting);
        }
    }

    void onLocationUpdate(){
        Collections.sort(restaurantsRVAdapter.restaurantList, new SortRestaurants(UserAuth.mCurrentLocation));
        restaurantsRVAdapter.notifyDataSetChanged();
    }

    void onSearchQuery(String newText){
        if(isVisible()){
            restaurantsRVAdapter.getFilter().filter(newText);
        }
    }

    public class RestaurantsRVAdapter extends
            RecyclerView.Adapter<RestaurantsRVAdapter.RstViewHolder> implements Filterable {

        private final String TAG = "FireStore";
        private ArrayList<RestaurantItem> restaurantList = new ArrayList<>();
        List<RestaurantItem> restaurantListFiltered = new ArrayList<>();

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
                    .inflate(R.layout.item_near_restaurant, viewGroup, false);
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
            TextView tvDesc;
            TextView tvAddress;
            ImageView imgLogo;

            RestaurantItem restaurantItem;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_restaurantName);
                tvDesc = itemView.findViewById(R.id.tv_restaurantDesc);
                tvAddress = itemView.findViewById(R.id.tv_restaurantAddress);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);

                itemView.setOnClickListener(this);
            }

            void bindView(RestaurantItem restaurantItem){
                this.restaurantItem = restaurantItem;

                tvName.setText(restaurantItem.getName());
                tvDesc.setText(restaurantItem.getDescription());
                tvAddress.setText(restaurantItem.getAddress());

                if(restaurantItem.getLocation() != null){
                    tvAddress.setOnClickListener(this);

                    tvAddress.setCompoundDrawablesRelativeWithIntrinsicBounds
                            (0, 0, R.drawable.ic_pin, 0);
                }
                else{
                    tvAddress.setCompoundDrawablesRelativeWithIntrinsicBounds
                            (0, 0, 0, 0);
                }

                refreshThumbnail(restaurantItem);
            }

            private void refreshThumbnail(RestaurantItem item) {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(item.getImgRef());

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
                switch (v.getId()) {
                    case R.id.item_nearRestaurant:
                        onButtonPressed(v.getId(), restaurantItem);
                        break;
                    case R.id.tv_restaurantAddress:
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
        }
    }

    public class RecentOrdersRVAdapter extends RecyclerView.Adapter<RecentOrdersRVAdapter.RecentOrderViewHolder>{

        private final String TAG = "FireStore";
        private ArrayList<RecentOrder> recentOrders = new ArrayList<>();

        RecentOrdersRVAdapter() {
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
                                RecentOrder recentOrder;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New RecentOrder: " + dc.getDocument().getData());

                                        recentOrder = dc.getDocument().toObject(RecentOrder.class);

                                        recentOrder.setId(dc.getDocument().getId());

                                        recentOrders.add(recentOrder);

                                        notifyItemInserted(recentOrders.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified RecentOrder: " + dc.getDocument().getData());

                                        recentOrder = dc.getDocument().toObject(RecentOrder.class);

                                        for(RecentOrder order : recentOrders){
                                            if(order.getId().equals(recentOrder.getId())){
                                                position = recentOrders.indexOf(order);
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed RecentOrder: " + dc.getDocument().getData());

                                        recentOrder = dc.getDocument().toObject(RecentOrder.class);

                                        for(RecentOrder order : recentOrders){
                                            if(order.getId().equals(recentOrder.getId())){
                                                position = recentOrders.indexOf(order);
                                                Log.d(TAG, "Removed RecentOrder Notified!: " + order.getId()
                                                        + " => " + recentOrder.getId() + " => " + recentOrders.indexOf(order));
                                            }
                                        }

                                        if(position >= 0){
                                            recentOrders.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("users")
                    .document(UserAuth.currentUser.getUid())
                    .collection("recentOrders")
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public RecentOrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView;
            restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_food_recent, viewGroup, false);
            return new RecentOrderViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecentOrderViewHolder viewHolder, int i) {
            viewHolder.bindView(recentOrders.get(i));
        }

        @Override
        public int getItemCount() {
            return recentOrders.size();
        }

        class RecentOrderViewHolder extends RecyclerView.ViewHolder{

            View recentOrderItem;
            TextView tvOrderName;
            ImageView imgOrderImage;

            RecentOrderViewHolder(@NonNull View itemView) {
                super(itemView);
                recentOrderItem = itemView;
                tvOrderName = itemView.findViewById(R.id.tv_orderName);
                imgOrderImage = itemView.findViewById(R.id.img_orderLogo);
            }

            void bindView(final RecentOrder recentOrder){
                tvOrderName.setText(recentOrder.getOrderName());

                recentOrderItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onButtonPressed(v.getId(), recentOrder.getOrderSummary());
                    }
                });

                refreshThumbnail(recentOrder);
            }

            private void refreshThumbnail(RecentOrder recentOrder) {
                if(recentOrder.getImgRef() != null){
                    try {
                        StorageReference imageRef = UserAuth.firebaseStorage
                                .getReferenceFromUrl(recentOrder.getImgRef());

                        GlideApp.with(imgOrderImage.getContext())
                                .load(imageRef)
                                .into(imgOrderImage);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
                else{
                    selectOrderImage(recentOrder);
                }
            }

            private void selectOrderImage(final RecentOrder recentOrder){
                String restId = recentOrder.getOrderSummary().keySet().toArray(new String[1])[0];
                String menuItemId = Objects.requireNonNull(recentOrder
                        .getOrderSummary().get(restId)).keySet().toArray(new String[1])[0];

                final DocumentReference orderRef = fireDB.collection("users")
                        .document(UserAuth.currentUser.getUid())
                        .collection("recentOrders")
                        .document(recentOrder.getId());

                fireDB.collection("restaurants")
                        .document(restId)
                        .collection("menuItems")
                        .document(menuItemId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                          @Override
                          public void onSuccess(DocumentSnapshot documentSnapshot) {
                            MenuItem menuItem = documentSnapshot.toObject(MenuItem.class);
                            assert menuItem != null;
                            recentOrder.setImgRef(menuItem.getImgRef());
                            orderRef.set(recentOrder)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                      @Override
                                      public void onSuccess(Void aVoid) {
                                        notifyItemChanged(getAdapterPosition());
                                      }
                                    });
                          }
                        });
              }
          }
    }
}