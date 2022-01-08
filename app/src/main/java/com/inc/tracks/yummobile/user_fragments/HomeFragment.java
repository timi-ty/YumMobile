package com.inc.tracks.yummobile.user_fragments;

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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.inc.tracks.yummobile.components.MenuItem;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.RecentOrder;
import com.inc.tracks.yummobile.components.RestaurantItem;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.utils.GlideApp;
import com.inc.tracks.yummobile.utils.SortRestaurants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        void onFragmentInteraction(int interactionId, String searchText);
    }

    private void greetUser(){
        String userGreeting = "Hello, " + UserAuth.userPrefs.getUserName();
        if (txtUserGreeting != null) {
            txtUserGreeting.setText(userGreeting);
        }
    }

    public void onLocationUpdate(){
        Collections.sort(restaurantsRVAdapter.restaurantList, new SortRestaurants(UserAuth.mCurrentLocation));
        restaurantsRVAdapter.notifyDataSetChanged();
    }

    public void onSearchQuery(String newText){
        if(isVisible()){
            mListener.onFragmentInteraction(R.string.search, newText);
        }
    }

    public class RestaurantsRVAdapter extends
            RecyclerView.Adapter<RestaurantsRVAdapter.RstViewHolder>{

        private final String TAG = "FireStore";
        private ArrayList<RestaurantItem> restaurantList = new ArrayList<>();

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
            RestaurantItem restaurantItem = restaurantList.get(position);
            viewHolder.bindView(restaurantItem);
        }

        @Override
        public int getItemCount() {
            return Math.min(restaurantList.size(), 6);
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

                itemView.setOnClickListener(this);
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

            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.item_nearRestaurant) {
                    onButtonPressed(v.getId(), restaurantItem);
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

            View recentOrderItemView;
            TextView tvOrderName;
            ImageView imgOrderImage;
            FloatingActionButton fabRemove;

            RecentOrderViewHolder(@NonNull View itemView) {
                super(itemView);
                recentOrderItemView = itemView;
                tvOrderName = itemView.findViewById(R.id.tv_orderName);
                imgOrderImage = itemView.findViewById(R.id.img_orderLogo);
                fabRemove = itemView.findViewById(R.id.fab_removeRecent);
            }

            void bindView(final RecentOrder recentOrder){
                tvOrderName.setText(recentOrder.getOrderName());

                recentOrderItemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onButtonPressed(v.getId(), recentOrder.getOrderSummary());
                    }
                });

                fabRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteRecent(recentOrder);
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
                                .transform(new CenterCrop(), new RoundedCorners(20))
                                .transition(DrawableTransitionOptions.withCrossFade())
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

            private void deleteRecent(RecentOrder recentOrderItem){
                String docId = recentOrderItem.getId();
                if(docId == null){
                    recentOrders.remove(recentOrderItem);

                    notifyItemRemoved(getAdapterPosition());
                }
                else{
                    fireDB.collection("users")
                            .document(UserAuth.currentUser.getUid())
                            .collection("recentOrders")
                            .document(docId)
                            .delete()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Snackbar.make(myLayout,
                                            "Saved Order Deleted Successfully.", Snackbar.LENGTH_LONG).show();
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