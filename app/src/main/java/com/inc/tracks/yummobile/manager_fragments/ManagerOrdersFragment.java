package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.StorageReference;
import com.inc.tracks.yummobile.components.OrderItem;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.RestaurantItem;
import com.inc.tracks.yummobile.utils.GlideApp;
import com.inc.tracks.yummobile.utils.SortActiveOrders;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.components.UserPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class ManagerOrdersFragment extends Fragment{

    private OnFragmentInteractionListener mListener;

    private TextView tvTitle;

    private RecyclerView rvOrders;

    private Menu bottomMenu;

    private ConstraintLayout myLayout;

    private AcceptedOrdersRVAdapter acceptedOrdersRVAdapter;

    private ActiveOrdersRVAdapter activeOrdersRVAdapter;

    FirebaseFirestore fireDB;

    public ManagerOrdersFragment() {
        // Required empty public constructor
    }


    public static ManagerOrdersFragment newInstance() {
        ManagerOrdersFragment fragment = new ManagerOrdersFragment();
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
        View fragView = inflater.inflate(R.layout.fragment_manager_active_orders, container, false);

        myLayout = fragView.findViewById(R.id.layout_orderManager);

        tvTitle = fragView.findViewById(R.id.tv_orderRVTitle);

        rvOrders = fragView.findViewById(R.id.rv_activeOrders);

        if(savedInstanceState == null){
            activeOrdersRVAdapter = new ActiveOrdersRVAdapter();
            acceptedOrdersRVAdapter = new AcceptedOrdersRVAdapter();

            rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
            rvOrders.setAdapter(activeOrdersRVAdapter);

            activeOrdersRVAdapter.getFilter().filter("");
            acceptedOrdersRVAdapter.getFilter().filter("");
        }

        BottomNavigationView bottomNavView = fragView.findViewById(R.id.bottom_nav_view);

        bottomNavView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        bottomMenu = bottomNavView.getMenu();


        mListener.onFragmentInteraction(R.layout.fragment_manager_active_orders);

        return fragView;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {
                case R.id.navigation_pending_orders:
                    if(activeOrdersRVAdapter == null){
                        activeOrdersRVAdapter = new ActiveOrdersRVAdapter();
                    }
                    rvOrders.setAdapter(activeOrdersRVAdapter);
                    tvTitle.setText(R.string.title_pending_orders);
                    bottomMenu.getItem(0).setChecked(true);
                    return true;
                case R.id.navigation_accepted_orders:
                    if(acceptedOrdersRVAdapter == null){
                        acceptedOrdersRVAdapter = new AcceptedOrdersRVAdapter();
                    }
                    rvOrders.setAdapter(acceptedOrdersRVAdapter);
                    tvTitle.setText(R.string.title_your_deliveries);
                    bottomMenu.getItem(1).setChecked(true);
                    return true;
            }
            return false;
        }
    };

    private void onButtonPressed(int buttonId, OrderItem activeOrder) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId);
            mListener.onFragmentInteraction(buttonId, activeOrder);
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



    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int interactionId);
        void onFragmentInteraction(int interactionId, OrderItem activeOrder);
    }

    public void onLocationUpdate(){
        if(activeOrdersRVAdapter.activeOrders != null){
            Collections.sort(activeOrdersRVAdapter.activeOrders, new SortActiveOrders(UserAuth.mCurrentLocation));
            activeOrdersRVAdapter.notifyDataSetChanged();
        }

        if(acceptedOrdersRVAdapter.acceptedOrders != null){
            WriteBatch batch = fireDB.batch();

            for(OrderItem acceptedOrder : acceptedOrdersRVAdapter.acceptedOrders){
                acceptedOrder.setTransLocation(new GeoPoint
                        (UserAuth.mCurrentLocation.getLatitude(),
                                UserAuth.mCurrentLocation.getLongitude()));

                DocumentReference acceptedOrderRef = fireDB.collection("activeOrders")
                        .document(acceptedOrder.getId());

                batch.set(acceptedOrderRef, acceptedOrder);
            }


            batch.commit();
        }
    }

    public void onSearchQuery(String newText){
        if(isVisible()){
            activeOrdersRVAdapter.getFilter().filter(newText);
            acceptedOrdersRVAdapter.getFilter().filter(newText);
        }
    }

    public class ActiveOrdersRVAdapter extends
            RecyclerView.Adapter<ActiveOrdersRVAdapter.RstViewHolder> implements Filterable {

        private final String TAG = "FireStore";

        private ArrayList<OrderItem> activeOrders = new ArrayList<>();
        List<OrderItem> activeOrdersFiltered = new ArrayList<>();
        private HashMap<String, RestaurantItem> restaurantItems = new HashMap<>();
        private HashMap<String, UserPrefs> buyers = new HashMap<>();

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
                    .whereEqualTo("transporterId", null)
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_order, viewGroup, false);
            return new RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int position) {
            OrderItem activeOrder = activeOrdersFiltered.get(position);
            viewHolder.bindView(activeOrder);
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

        class RstViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvDesc;
            TextView tvTimestamp;
            TextView tvPrice;
            Button btnAcceptOrder;
            ProgressBar pbLoading;
            ImageView imgLogo;

            OrderItem activeOrder;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDesc = itemView.findViewById(R.id.tv_orderDesc);
                tvTimestamp = itemView.findViewById(R.id.tv_timeStamp);
                tvPrice = itemView.findViewById(R.id.tv_orderPrice);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                btnAcceptOrder = itemView.findViewById(R.id.btn_orderInteraction);
                pbLoading = itemView.findViewById(R.id.pb_activeOrder);

                itemView.setOnClickListener(this);
            }

            void bindView(final OrderItem activeOrder){
                this.activeOrder = activeOrder;

                RestaurantItem restaurantItem = restaurantItems.get(activeOrder.getRestaurantId());
                UserPrefs buyer = buyers.get(activeOrder.getClientId());

                if(restaurantItem != null && buyer != null){
                    String message = "Order for " + buyer.getUserName() +
                            " (" + buyer.getUserPhone() +") from "
                            + restaurantItem.getName() + " is pending.";

                    tvDesc.setText(message);

                    tvPrice.setText(String.format(Locale.ENGLISH,
                            "%d", activeOrder.getCost()));

                    tvTimestamp.setText(activeOrder.getTimestamp().toDate().toString());

                    btnAcceptOrder.setText(R.string.accept_order);

                    btnAcceptOrder.setOnClickListener(this);

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
                if(buyer == null) {
                    fireDB.collection("users")
                            .document(activeOrder.getClientId())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    UserPrefs buyer = documentSnapshot
                                            .toObject(UserPrefs.class);

                                    buyers.put(activeOrder.getClientId(),
                                            buyer);

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
                    case R.id.item_activeOrder:
                        onButtonPressed(v.getId(), activeOrder);
                        break;
                    case R.id.btn_orderInteraction:
                        acceptOrder();
                        break;
                }
            }

            private void acceptOrder(){
                btnAcceptOrder.setVisibility(View.INVISIBLE);
                pbLoading.setVisibility(View.VISIBLE);

                activeOrder.setTransporter(UserAuth.currentUser.getUid());

                if(UserAuth.mCurrentLocation != null){
                    activeOrder.setTransLocation(new GeoPoint
                            (UserAuth.mCurrentLocation.getLatitude(),
                                    UserAuth.mCurrentLocation.getLongitude()));
                }

                activeOrder.setInitialDistance(computeInitialDistance(
                        activeOrder.getTransLocation(), activeOrder.getClientLocation()));

                fireDB.collection("activeOrders")
                        .document(activeOrder.getId())
                        .set(activeOrder, SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Snackbar.make(myLayout, "You accepted the order!",
                                        Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(myLayout, "Failed to accept order.",
                                        Snackbar.LENGTH_SHORT).show();

                                btnAcceptOrder.setVisibility(View.VISIBLE);
                                pbLoading.setVisibility(View.INVISIBLE);
                            }
                        });
            }

            private int computeInitialDistance(GeoPoint transLocation, GeoPoint clientLocation) {
                final int EARTH_RADIUS = 6378137;

                if(transLocation == null || clientLocation == null){
                    return EARTH_RADIUS;
                }

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
        }
    }

    public class AcceptedOrdersRVAdapter extends
            RecyclerView.Adapter<AcceptedOrdersRVAdapter.ActiveOrderViewHolder> implements Filterable{

        private final String TAG = "FireStore";
        FirebaseFirestore fireDB;

        private ArrayList<OrderItem> acceptedOrders = new ArrayList<>();
        List<OrderItem> acceptedOrdersFiltered = new ArrayList<>();
        private HashMap<String, RestaurantItem> restaurantItems = new HashMap<>();
        private HashMap<String, UserPrefs> buyers = new HashMap<>();

        AcceptedOrdersRVAdapter() {
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
                                        Log.d(TAG, "New AcceptedOrder: " + dc.getDocument().getData());

                                        activeOrder = dc.getDocument().toObject(OrderItem.class);

                                        activeOrder.setId(dc.getDocument().getId());

                                        acceptedOrders.add(activeOrder);

                                        notifyItemInserted(acceptedOrders.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified AcceptedOrder: " + dc.getDocument().getData());

                                        for(OrderItem item : acceptedOrders){
                                            if(item.getId().equals(dc.getDocument().getId())){
                                                position = acceptedOrders.indexOf(item);
                                                acceptedOrders.set(position, dc.getDocument()
                                                        .toObject(OrderItem.class));
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed AcceptedOrder: " + dc.getDocument().getData());

                                        for(OrderItem item : acceptedOrders){
                                            if(item.getId().equals(dc.getDocument().getId())){
                                                position = acceptedOrders.indexOf(item);
                                            }
                                        }

                                        if(position >= 0){
                                            acceptedOrders.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("activeOrders")
                    .whereEqualTo("transporterId", UserAuth.currentUser.getUid())
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public ActiveOrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_order, viewGroup, false);
            return new ActiveOrderViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull ActiveOrderViewHolder viewHolder, int position) {
            OrderItem activeOrder = acceptedOrdersFiltered.get(position);
            viewHolder.bindView(activeOrder);
        }

        @Override
        public int getItemCount() {
            return acceptedOrdersFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();

                    if (charString.isEmpty()) {
                        acceptedOrdersFiltered = acceptedOrders;
                    } else {
                        List<OrderItem> filteredList = new ArrayList<>();
                        for (OrderItem order : acceptedOrders) {
                            RestaurantItem restaurantItem = restaurantItems.get(order.getRestaurantId());
                            assert  restaurantItem != null;
                            String restaurantName = restaurantItem.getName();
                            if (order.getDescription().toLowerCase().contains(charString.toLowerCase())
                                    || restaurantName.toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(order);
                            }
                        }

                        acceptedOrdersFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = acceptedOrdersFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    acceptedOrdersFiltered = (ArrayList<OrderItem>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }

        class ActiveOrderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvDesc;
            TextView tvTimestamp;
            TextView tvPrice;
            Button btnOrderDelivered;
            ProgressBar pbLoading;
            ImageView imgLogo;

            OrderItem acceptedOrder;

            ActiveOrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDesc = itemView.findViewById(R.id.tv_orderDesc);
                tvTimestamp = itemView.findViewById(R.id.tv_timeStamp);
                tvPrice = itemView.findViewById(R.id.tv_orderPrice);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                btnOrderDelivered = itemView.findViewById(R.id.btn_orderInteraction);
                pbLoading = itemView.findViewById(R.id.pb_activeOrder);

                itemView.setOnClickListener(this);
            }

            void bindView(final OrderItem acceptedOrder){
                this.acceptedOrder = acceptedOrder;

                RestaurantItem restaurantItem = restaurantItems.get(acceptedOrder.getRestaurantId());
                UserPrefs buyer = buyers.get(acceptedOrder.getClientId());

                if(restaurantItem != null && buyer != null){
                    String message = buyer.getUserName() +
                            " (" + buyer.getUserPhone() + ") is waiting for you " +
                            "to deliver his order from " + restaurantItem.getName() + ".";

                    tvDesc.setText(message);

                    tvPrice.setText(String.format(Locale.ENGLISH,
                            "%d", acceptedOrder.getCost()));

                    tvTimestamp.setText(acceptedOrder.getTimestamp().toDate().toString());

                    boolean fulfilled = acceptedOrder.isTransporterConfirmed();
                    btnOrderDelivered.setText(fulfilled ?
                            R.string.wait_on_customer : R.string.confirm_delivered);

                    btnOrderDelivered.setOnClickListener(this);

                    refreshThumbnail(restaurantItem);
                }
                if(restaurantItem == null) {
                    fireDB.collection("restaurants")
                            .document(acceptedOrder.getRestaurantId())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    RestaurantItem restaurantItem = documentSnapshot
                                            .toObject(RestaurantItem.class);

                                    restaurantItems.put(acceptedOrder.getRestaurantId(),
                                            restaurantItem);

                                    notifyItemChanged(getAdapterPosition());
                                }
                            });
                }
                if(buyer == null) {
                    fireDB.collection("users")
                            .document(acceptedOrder.getClientId())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    UserPrefs buyer = documentSnapshot
                                            .toObject(UserPrefs.class);

                                    buyers.put(acceptedOrder.getClientId(),
                                            buyer);

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
                if(v.getId() == R.id.btn_orderInteraction){
                    fulfillOrder();
                }
                else{
                    onButtonPressed(v.getId(), acceptedOrder);
                }
            }

            private void fulfillOrder(){
                btnOrderDelivered.setVisibility(View.INVISIBLE);
                pbLoading.setVisibility(View.VISIBLE);

                acceptedOrder.setTransporterConfirmed(true);

                fireDB.collection("activeOrders")
                        .document(acceptedOrder.getId())
                        .set(acceptedOrder, SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Snackbar.make(myLayout, "You delivered the order!",
                                        Snackbar.LENGTH_SHORT).show();
                                btnOrderDelivered.setVisibility(View.VISIBLE);
                                pbLoading.setVisibility(View.INVISIBLE);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(myLayout, "Failed to confirm delivery.",
                                        Snackbar.LENGTH_SHORT).show();

                                btnOrderDelivered.setVisibility(View.VISIBLE);
                                pbLoading.setVisibility(View.INVISIBLE);
                            }
                        });
            }
        }
    }
}
