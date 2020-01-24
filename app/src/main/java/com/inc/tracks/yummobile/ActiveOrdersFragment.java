package com.inc.tracks.yummobile;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
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
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class ActiveOrdersFragment extends Fragment {

    private View myLayout;

    private ActiveOrdersRVAdapter activeOrdersRVAdapter;

    FirebaseFirestore fireDB;

    public ActiveOrdersFragment() {
        // Required empty public constructor
    }


    public static ActiveOrdersFragment newInstance() {
        ActiveOrdersFragment fragment = new ActiveOrdersFragment();
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
        View fragView = inflater.inflate(R.layout.fragment_active_orders, container, false);

        RecyclerView activeOrders = fragView.findViewById(R.id.rv_activeOrders);

        activeOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        if(activeOrdersRVAdapter == null){
            activeOrdersRVAdapter = new ActiveOrdersRVAdapter();
        }
        activeOrdersRVAdapter.getFilter().filter("");

        activeOrders.setAdapter(activeOrdersRVAdapter);

        myLayout = (View) activeOrders.getParent();

        return fragView;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    void onLocationUpdate(){
        WriteBatch batch = fireDB.batch();

        for(OrderItem activeOrder : activeOrdersRVAdapter.activeOrders){
            activeOrder.setClientLocation(new GeoPoint
                    (UserAuth.mCurrentLocation.getLatitude(),
                            UserAuth.mCurrentLocation.getLongitude()));

            DocumentReference activeOrderRef = fireDB.collection("activeOrders")
                    .document(activeOrder.getId());

            batch.set(activeOrderRef, activeOrder);
        }

        batch.commit();
    }

    void onSearchQuery(String newText){
        if(isVisible()){
            activeOrdersRVAdapter.getFilter().filter(newText);
        }
    }



    public class ActiveOrdersRVAdapter extends
            RecyclerView.Adapter<ActiveOrdersRVAdapter.ActiveOrderViewHolder> implements Filterable {

        private final String TAG = "FireStore";

        private ArrayList<OrderItem> activeOrders = new ArrayList<>();
        List<OrderItem> activeOrdersFiltered = new ArrayList<>();
        private HashMap<String, RestaurantItem> restaurantItems = new HashMap<>();
        private HashMap<String, UserPrefs> transporters = new HashMap<>();

        ActiveOrdersRVAdapter() {
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
                                OrderItem activeOrder;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New OrderItem: " + dc.getDocument().getData());

                                        activeOrder = dc.getDocument().toObject(OrderItem.class);

                                        activeOrder.setId(dc.getDocument().getId());

                                        activeOrders.add(activeOrder);

                                        notifyItemInserted(activeOrders.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified OrderItem: " + dc.getDocument().getData());

                                        for(OrderItem item : activeOrders){
                                            if(item.getId().equals(dc.getDocument().getId())){
                                                position = activeOrders.indexOf(item);
                                                activeOrders.set(position,
                                                        dc.getDocument().toObject(OrderItem.class));
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed OrderItem: " + dc.getDocument().getData());

                                        for(OrderItem item : activeOrders){
                                            if(item.getId().equals(dc.getDocument().getId())){
                                                position = activeOrders.indexOf(item);
                                            }
                                        }

                                        if(position >= 0){
                                            activeOrders.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("activeOrders")
                    .whereEqualTo("clientId", UserAuth.currentUser.getUid())
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public ActiveOrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_active_order, viewGroup, false);
            return new ActiveOrderViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull ActiveOrderViewHolder viewHolder, int i) {
            viewHolder.bindView(activeOrdersFiltered.get(i));
        }

        @Override
        public int getItemCount() {
            return activeOrdersFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();

                    if (charString.isEmpty()) {
                        activeOrdersFiltered = activeOrders;
                    } else {
                        List<OrderItem> filteredList = new ArrayList<>();
                        for (OrderItem order : activeOrders) {
                            RestaurantItem restaurantItem = restaurantItems.get(order.getRestaurantId());
                            assert  restaurantItem != null;
                            String restaurantName = restaurantItem.getName();
                            if (order.getDescription().toLowerCase().contains(charString.toLowerCase())
                                || restaurantName.toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(order);
                            }
                        }

                        activeOrdersFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = activeOrdersFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    activeOrdersFiltered = (ArrayList<OrderItem>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }

        class ActiveOrderViewHolder extends RecyclerView.ViewHolder implements  View.OnClickListener{

            TextView tvDescription;
            TextView tvOrderPrice;
            ProgressBar pbOrderProgress;
            ProgressBar pbLoading;
            Button btnConfirmReceived;
            ImageView imgLogo;

            OrderItem activeOrder;
            UserPrefs transporter;

            ActiveOrderViewHolder(@NonNull View itemView) {
                super(itemView);

                tvDescription = itemView.findViewById(R.id.tv_orderDesc);
                tvOrderPrice = itemView.findViewById(R.id.tv_orderPrice);
                pbOrderProgress = itemView.findViewById(R.id.pb_orderProgress);
                pbLoading = itemView.findViewById(R.id.pb_activeOrder);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                btnConfirmReceived = itemView.findViewById(R.id.btn_confirmReceived);
            }

            void bindView(final OrderItem activeOrder){
                this.activeOrder = activeOrder;

                RestaurantItem restaurantItem = restaurantItems.get(activeOrder.getRestaurantId());
                transporter = transporters.get(activeOrder.getTransporterId());

                if(restaurantItem != null && (transporter != null || !activeOrder.isAccepted())){

                    String message;

                    if(activeOrder.isAccepted() && transporter != null){
                        message = "Order Confirmed! Click here to call "
                                + transporter.getUserName() + " (" + transporter.getUserPhone() +
                                ") to track your delivery of " + activeOrder.getDescription() +
                                " from " + restaurantItem.getName();

                        int orderProgress = activeOrder.getInitialDistance() - computeOrderDistance
                                (activeOrder.getTransLocation(), UserAuth.mCurrentLocation);

                        Log.d("OrderDistance", ": " + computeOrderDistance
                                (activeOrder.getTransLocation(), UserAuth.mCurrentLocation));

                        pbOrderProgress.setMax(activeOrder.getInitialDistance());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            pbOrderProgress.setProgress(orderProgress, true);
                        }
                        else{
                            pbOrderProgress.setProgress(orderProgress);
                        }

                        if(activeOrder.isTransporterConfirmed()){
                            pbOrderProgress.setVisibility(View.INVISIBLE);
                            btnConfirmReceived.setVisibility(View.VISIBLE);

                            btnConfirmReceived.setOnClickListener(this);
                        }
                        else {
                            pbOrderProgress.setVisibility(View.VISIBLE);
                            btnConfirmReceived.setVisibility(View.INVISIBLE);

                            btnConfirmReceived.setOnClickListener(null);
                        }

                        tvDescription.setOnClickListener(this);
                    }
                    else{
                        message = activeOrder.getDescription() + " from "
                                + restaurantItem.getName() + " is coming up.";
                    }

                    tvDescription.setText(message);

                    tvOrderPrice.setText(String.format(Locale.ENGLISH,
                            "%d", activeOrder.getCost()));

                    refreshThumbnail(restaurantItem);
                }
                if(restaurantItem == null) {
                    fireDB.collection("restaurants")
                            .document(activeOrder.getRestaurantId())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    RestaurantItem restaurantItem = documentSnapshot
                                            .toObject(RestaurantItem.class);

                                    restaurantItems.put(activeOrder.getRestaurantId(),
                                            restaurantItem);

                                    notifyItemChanged(getAdapterPosition());
                                }
                            });
                }

                if(activeOrder.isAccepted() && transporter == null) {
                    fireDB.collection("users")
                            .document(activeOrder.getTransporterId())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    UserPrefs transporter = documentSnapshot
                                            .toObject(UserPrefs.class);

                                    transporters.put(activeOrder.getTransporterId(),
                                            transporter);

                                    notifyItemChanged(getAdapterPosition());
                                }
                            });
                }
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
                switch (v.getId()){
                    case R.id.tv_orderDesc:
                        if(activeOrder.isAccepted()){
                            callTransporter();
                        }
                        break;
                    case R.id.btn_confirmReceived:
                        confirmOrderReceived(activeOrder);
                        break;
                }
            }

            private void callTransporter(){
                if(transporter != null){
                    Uri phone = Uri.parse("tel:" + transporter.getUserPhone());

                    Intent dialerIntent = new Intent(Intent.ACTION_DIAL, phone);

                    if (dialerIntent.resolveActivity(Objects.requireNonNull(getActivity())
                            .getPackageManager()) != null) {
                        startActivity(dialerIntent);
                    }
                }
            }

            private int computeOrderDistance(GeoPoint transLocation, Location clientLocation) {
                if (clientLocation == null || transLocation == null){
                    return pbOrderProgress.getMax()/2;
                }

                final int EARTH_RADIUS = 6378137;

                double fromLat = transLocation.getLatitude();
                double fromLon = transLocation.getLongitude();
                double toLat = clientLocation.getLatitude();
                double toLon = clientLocation.getLongitude();

                double deltaLat = toLat - fromLat;
                double deltaLon = toLon - fromLon;
                double angle = 2 * Math.asin( Math.sqrt(
                        Math.pow(Math.sin(deltaLat/2), 2) +
                                Math.cos(fromLat) * Math.cos(toLat) *
                                        Math.pow(Math.sin(deltaLon/2), 2) ) );

                return (int) (EARTH_RADIUS * angle);
            }

            private void confirmOrderReceived(final OrderItem activeOrder){
                if(activeOrder.isTransporterConfirmed()) {
                    setLoading(true);
                    activeOrder.setClientConfirmed(true);

                    fireDB.collection("transporters")
                            .document(activeOrder.getTransporterId())
                            .collection("completedDeliveries")
                            .document(activeOrder.getId())
                            .set(activeOrder)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    fireDB.collection("activeOrders")
                                            .document(activeOrder.getId())
                                            .delete()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    Snackbar.make(myLayout, "Thank you for shopping with us!",
                                                            Snackbar.LENGTH_SHORT).show();
                                                    setLoading(false);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Snackbar.make(myLayout, "Failed. Please try again.",
                                                            Snackbar.LENGTH_SHORT).show();
                                                    setLoading(false);
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar.make(myLayout, "Failed. Please try again.",
                                            Snackbar.LENGTH_SHORT).show();
                                    setLoading(false);
                                }
                            });
                }
                else{
                    Snackbar.make(myLayout, "Failed. Transporter is yet to confirm.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            private void setLoading(boolean loading){
                btnConfirmReceived.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
                pbLoading.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }
}
