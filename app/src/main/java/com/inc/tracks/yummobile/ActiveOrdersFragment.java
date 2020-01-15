package com.inc.tracks.yummobile;

import android.content.Context;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class ActiveOrdersFragment extends Fragment {

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
        activeOrders.setAdapter(new ActiveOrdersRVAdapter());

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


    public class ActiveOrdersRVAdapter extends RecyclerView.Adapter<ActiveOrdersRVAdapter.MenuItemViewHolder>{

        private final String TAG = "FireStore";
        FirebaseFirestore fireDB;

        private ArrayList<ActiveOrder> activeOrders = new ArrayList<>();
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
                                                activeOrders.set(position,
                                                        dc.getDocument().toObject(ActiveOrder.class));
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
                    .whereEqualTo("clientId", UserAuth.currentUser.getUid())
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_active_order, viewGroup, false);
            return new MenuItemViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull MenuItemViewHolder viewHolder, int i) {
            viewHolder.bindView(activeOrders.get(i));
        }

        @Override
        public int getItemCount() {
            return activeOrders.size();
        }

        class MenuItemViewHolder extends RecyclerView.ViewHolder implements  View.OnClickListener{

            TextView tvDescription;
            TextView tvOrderPrice;
            ProgressBar pbOrderProgress;
            Button btnConfirmReceived;
            ImageView imgLogo;

            MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);

                tvDescription = itemView.findViewById(R.id.tv_orderDesc);
                tvOrderPrice = itemView.findViewById(R.id.tv_orderPrice);
                pbOrderProgress = itemView.findViewById(R.id.pb_orderProgress);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                btnConfirmReceived = itemView.findViewById(R.id.btn_confirmReceived);
            }

            void bindView(final ActiveOrder activeOrder){
                RestaurantItem restaurantItem = restaurantItems.get(activeOrder.getRestaurantId());
                UserPrefs transporter = transporters.get(activeOrder.getTransporterId());
                if(restaurantItem != null && (transporter != null || !activeOrder.isAccepted())){

                    String message;

                    if(activeOrder.isAccepted() && transporter != null){
                        message = "Order Confirmed! Click here to call "
                                + transporter.getUserName() + " (" + transporter.getUserPhone() +
                                ") to track your delivery of " + activeOrder.getDescription() +
                                " from " + restaurantItem.getName();

                        pbOrderProgress.setProgressDrawable(getResources()
                                .getDrawable(R.drawable.rounded_background_primary_dark_8));

                        if(activeOrder.isTransporterConfirmed()){
                            pbOrderProgress.setVisibility(View.INVISIBLE);
                            btnConfirmReceived.setVisibility(View.VISIBLE);
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

            }
        }
    }
}
