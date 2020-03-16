package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.inc.tracks.yummobile.components.OrderItem;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.RestaurantItem;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.components.UserPrefs;
import com.inc.tracks.yummobile.utils.GlideApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class ManagerOrdersAdminFragment extends Fragment{

    private OnFragmentInteractionListener mListener;

    private ActiveOrdersRVAdapter activeOrdersRVAdapter;

    public ManagerOrdersAdminFragment() {
        // Required empty public constructor
    }


    public static ManagerOrdersAdminFragment newInstance() {
        ManagerOrdersAdminFragment fragment = new ManagerOrdersAdminFragment();
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
        View fragView = inflater.inflate(R.layout.fragment_manager_active_orders_admin,
                container, false);

        RecyclerView rvActiveOrders = fragView.findViewById(R.id.rv_activeOrders);

        rvActiveOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        if(activeOrdersRVAdapter == null){
            activeOrdersRVAdapter = new ActiveOrdersRVAdapter();
            activeOrdersRVAdapter.getFilter().filter("");
        }
        rvActiveOrders.setAdapter(activeOrdersRVAdapter);

        mListener.onFragmentInteraction(R.layout.fragment_manager_active_orders_admin);

        return fragView;
    }

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

    public void onSearchQuery(String newText){
        if(isVisible()){
            activeOrdersRVAdapter.getFilter().filter(newText);
        }
    }

    public class ActiveOrdersRVAdapter extends
            RecyclerView.Adapter<ActiveOrdersRVAdapter.ActiveOrderViewHolder> implements Filterable {

        private final String TAG = "FireStore";
        FirebaseFirestore fireDB;

        private ArrayList<OrderItem> activeOrders = new ArrayList<>();
        List<OrderItem> activeOrdersFiltered = new ArrayList<>();
        private HashMap<String, RestaurantItem> restaurantItems = new HashMap<>();
        private HashMap<String, UserPrefs> buyers = new HashMap<>();
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
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public ActiveOrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View orderView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_order, viewGroup, false);
            return new ActiveOrderViewHolder(orderView);
        }

        @Override
        public void onBindViewHolder(@NonNull ActiveOrderViewHolder viewHolder, int position) {
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

        class ActiveOrderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvDesc;
            TextView tvTimestamp;
            TextView tvPrice;
            Button btnCallTransporter;
            ImageView imgLogo;

            OrderItem activeOrder;
            UserPrefs transporter;

            ActiveOrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDesc = itemView.findViewById(R.id.tv_orderDesc);
                tvTimestamp = itemView.findViewById(R.id.tv_timeStamp);
                tvPrice = itemView.findViewById(R.id.tv_orderPrice);
                btnCallTransporter = itemView.findViewById(R.id.btn_orderInteraction);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);

                itemView.setOnClickListener(this);
            }

            void bindView(final OrderItem activeOrder){
                this.activeOrder = activeOrder;

                RestaurantItem restaurantItem = restaurantItems.get(activeOrder.getRestaurantId());
                UserPrefs buyer = buyers.get(activeOrder.getClientId());
                transporter = transporters.get(activeOrder.getTransporterId());
                if(restaurantItem != null && buyer != null &&
                        (transporter != null || !activeOrder.isAccepted())){

                    String message;

                    if(activeOrder.isAccepted() && transporter != null){
                        message = "Order for " + buyer.getUserName() +
                                " (" + buyer.getUserPhone() +") from "
                                + restaurantItem.getName() + " is being delivered by " +
                                transporter.getUserName() + " (" + transporter.getUserPhone() +
                                ").";

                        btnCallTransporter.setText(getResources().
                                getString(R.string.call_transporter));

                        btnCallTransporter.setOnClickListener(this);
                    }
                    else{
                        message = "Order for " + buyer.getUserName() +
                                " (" + buyer.getUserPhone() +") from "
                                + restaurantItem.getName() + " is pending.";
                    }

                    tvDesc.setText(message);

                    tvPrice.setText(String.format(Locale.ENGLISH,
                            "%d", activeOrder.getCost()));

                    tvTimestamp.setText(activeOrder.getTimestamp().toDate().toString());

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
                if(v.getId() == R.id.btn_orderInteraction){
                    callTransporter();
                }
                else{
                    onButtonPressed(v.getId(), activeOrder);
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
        }
    }
}
