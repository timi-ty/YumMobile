package com.inc.tracks.yummobile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class ManagerOrdersFragment extends Fragment{

    private OnFragmentInteractionListener mListener;

    private TextView tvTitle;

    private RecyclerView rvOrders;

    private Menu bottomMenu;

    private ConstraintLayout myLayout;

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
            rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
            rvOrders.setAdapter(new ActiveOrdersRVAdapter());
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
                    rvOrders.setAdapter(new ActiveOrdersRVAdapter());
                    tvTitle.setText(R.string.title_pending_orders);
                    bottomMenu.getItem(0).setChecked(true);
                    return true;
                case R.id.navigation_accepted_orders:
                    rvOrders.setAdapter(new AcceptedOrdersRVAdapter());
                    tvTitle.setText(R.string.title_your_deliveries);
                    bottomMenu.getItem(1).setChecked(true);
                    return true;
            }
            return false;
        }
    };

    private void onButtonPressed(int buttonId, ActiveOrder activeOrder) {
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
        void onFragmentInteraction(int interactionId, ActiveOrder activeOrder);
    }

    public class ActiveOrdersRVAdapter extends RecyclerView.Adapter<ActiveOrdersRVAdapter.RstViewHolder>{

        private final String TAG = "FireStore";
        FirebaseFirestore fireDB;

        private ArrayList<ActiveOrder> activeOrders = new ArrayList<>();
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
                                ActiveOrder activeOrder;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New ActiveOrder: " + dc.getDocument().getData());

                                        activeOrder = dc.getDocument().toObject(ActiveOrder.class);

                                        activeOrder.setId(dc.getDocument().getId());

                                        activeOrders.add(activeOrder);

                                        notifyItemInserted(activeOrders.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified ActiveOrder: " + dc.getDocument().getData());

                                        for(ActiveOrder item : activeOrders){
                                            if(item.getId().equals(dc.getDocument().getId())){
                                                position = activeOrders.indexOf(item);
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed ActiveOrder: " + dc.getDocument().getData());

                                        for(ActiveOrder item : activeOrders){
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
                    .inflate(R.layout.item_manage_active_order, viewGroup, false);
            return new RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int position) {
            ActiveOrder activeOrder = activeOrders.get(position);
            viewHolder.bindView(activeOrder);
        }

        @Override
        public int getItemCount() {
            return activeOrders.size();
        }

        class RstViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvDesc;
            TextView tvTimestamp;
            TextView tvPrice;
            Button btnAcceptOrder;
            ProgressBar pbLoading;
            ImageView imgLogo;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDesc = itemView.findViewById(R.id.tv_orderDesc);
                tvTimestamp = itemView.findViewById(R.id.tv_timeStamp);
                tvPrice = itemView.findViewById(R.id.tv_orderPrice);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                btnAcceptOrder = itemView.findViewById(R.id.btn_orderInteraction);
                pbLoading = itemView.findViewById(R.id.pb_activeOrder);

                itemView.setOnClickListener(this);
                btnAcceptOrder.setOnClickListener(this);
            }

            void bindView(final ActiveOrder activeOrder){
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
                        onButtonPressed(v.getId(), activeOrders.get(getAdapterPosition()));
                        break;
                    case R.id.btn_orderInteraction:
                        acceptOrder();
                        break;
                }
            }

            private void acceptOrder(){
                btnAcceptOrder.setVisibility(View.INVISIBLE);
                pbLoading.setVisibility(View.VISIBLE);

                ActiveOrder activeOrder = activeOrders.get(getAdapterPosition());

                activeOrder.setTransporter(UserAuth.currentUser.getUid());

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
        }
    }

    public class AcceptedOrdersRVAdapter extends RecyclerView.Adapter<AcceptedOrdersRVAdapter.RstViewHolder>{

        private final String TAG = "FireStore";
        FirebaseFirestore fireDB;

        private ArrayList<ActiveOrder> acceptedOrders = new ArrayList<>();
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
                                ActiveOrder activeOrder;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New AcceptedOrder: " + dc.getDocument().getData());

                                        activeOrder = dc.getDocument().toObject(ActiveOrder.class);

                                        activeOrder.setId(dc.getDocument().getId());

                                        acceptedOrders.add(activeOrder);

                                        notifyItemInserted(acceptedOrders.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified AcceptedOrder: " + dc.getDocument().getData());

                                        for(ActiveOrder item : acceptedOrders){
                                            if(item.getId().equals(dc.getDocument().getId())){
                                                position = acceptedOrders.indexOf(item);
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed AcceptedOrder: " + dc.getDocument().getData());

                                        for(ActiveOrder item : acceptedOrders){
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
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_active_order, viewGroup, false);
            return new RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int position) {
            ActiveOrder activeOrder = acceptedOrders.get(position);
            viewHolder.bindView(activeOrder);
        }

        @Override
        public int getItemCount() {
            return acceptedOrders.size();
        }

        class RstViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvDesc;
            TextView tvTimestamp;
            TextView tvPrice;
            Button btnOrderFulfilled;
            ImageView imgLogo;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDesc = itemView.findViewById(R.id.tv_orderDesc);
                tvTimestamp = itemView.findViewById(R.id.tv_timeStamp);
                tvPrice = itemView.findViewById(R.id.tv_orderPrice);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                btnOrderFulfilled = itemView.findViewById(R.id.btn_orderInteraction);

                itemView.setOnClickListener(this);
            }

            void bindView(final ActiveOrder activeOrder){
                RestaurantItem restaurantItem = restaurantItems.get(activeOrder.getRestaurantId());
                UserPrefs buyer = buyers.get(activeOrder.getClientId());

                if(restaurantItem != null && buyer != null){
                    String message = buyer.getUserName() +
                            " (" + buyer.getUserPhone() + ") is waiting for you " +
                            "to deliver his order from " + restaurantItem.getName() + ".";

                    tvDesc.setText(message);

                    tvPrice.setText(String.format(Locale.ENGLISH,
                            "%d", activeOrder.getCost()));

                    tvTimestamp.setText(activeOrder.getTimestamp().toDate().toString());

                    btnOrderFulfilled.setText(R.string.order_fulfilled);

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
                onButtonPressed(v.getId(), acceptedOrders.get(getAdapterPosition()));
            }
        }
    }
}
