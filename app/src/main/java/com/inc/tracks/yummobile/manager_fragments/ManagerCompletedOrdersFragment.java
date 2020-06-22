package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
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


public class ManagerCompletedOrdersFragment extends Fragment {

    private final static String ARG_TRANSPORTER = "transporter_user_prefs";

    private UserPrefs transporter;

    private OnFragmentInteractionListener mListener;

    private RecyclerView rvOrders;

    private ConstraintLayout myLayout;

    private CompletedOrdersOrdersRVAdapter completedOrdersRVAdapter;

    FirebaseFirestore fireDB;

    public ManagerCompletedOrdersFragment() {
        // Required empty public constructor
    }


    public static ManagerCompletedOrdersFragment newInstance(UserPrefs transporter) {
        ManagerCompletedOrdersFragment fragment = new ManagerCompletedOrdersFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRANSPORTER, transporter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            transporter = (UserPrefs) getArguments().getSerializable(ARG_TRANSPORTER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_completed_orders, container, false);

        myLayout = fragView.findViewById(R.id.layout_orderManager);

        rvOrders = fragView.findViewById(R.id.rv_completedOrders);

        if (savedInstanceState == null) {
            completedOrdersRVAdapter = new CompletedOrdersOrdersRVAdapter();

            rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
            rvOrders.setAdapter(completedOrdersRVAdapter);

            completedOrdersRVAdapter.getFilter().filter("");
        }


        mListener.onFragmentInteraction(R.layout.fragment_manager_completed_orders);

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

    public void onSearchQuery(String newText) {
        if (isVisible()) {
            completedOrdersRVAdapter.getFilter().filter(newText);
        }
    }

    public class CompletedOrdersOrdersRVAdapter extends
            RecyclerView.Adapter<CompletedOrdersOrdersRVAdapter.OrderViewHolder> implements Filterable {

        private final String TAG = "FireStore";

        private ArrayList<OrderItem> completedOrders = new ArrayList<>();
        private List<OrderItem> completedOrdersFiltered = new ArrayList<>();
        private HashMap<String, RestaurantItem> restaurantItems = new HashMap<>();
        private HashMap<String, UserPrefs> buyers = new HashMap<>();

        CompletedOrdersOrdersRVAdapter() {
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

                                        completedOrders.add(activeOrder);

                                        notifyItemInserted(completedOrders.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified OrderItem: " + dc.getDocument().getData());

                                        for (OrderItem item : completedOrders) {
                                            if (item.getId().equals(dc.getDocument().getId())) {
                                                position = completedOrders.indexOf(item);
                                            }
                                        }
                                        if (position >= 0) {
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed OrderItem: " + dc.getDocument().getData());

                                        for (OrderItem item : completedOrders) {
                                            if (item.getId().equals(dc.getDocument().getId())) {
                                                position = completedOrders.indexOf(item);
                                            }
                                        }

                                        if (position >= 0) {
                                            completedOrders.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("transporters")
                    .document(transporter.getId())
                    .collection("completedDeliveries")
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_order, viewGroup, false);
            return new OrderViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder viewHolder, int position) {
            OrderItem activeOrder = completedOrdersFiltered.get(position);
            viewHolder.bindView(activeOrder);
        }

        @Override
        public int getItemCount() {
            return completedOrdersFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();

                    if (charString.isEmpty()) {
                        completedOrdersFiltered = completedOrders;
                    } else {
                        List<OrderItem> filteredList = new ArrayList<>();
                        for (OrderItem order : completedOrders) {
                            RestaurantItem restaurantItem = restaurantItems.get(order.getRestaurantId());
                            String restaurantName = "";
                            if(restaurantItem != null){
                                restaurantName = restaurantItem.getName();
                            }
                            if (order.getDescription().toLowerCase().contains(charString.toLowerCase())
                                    || restaurantName.toLowerCase().contains(charString.toLowerCase())
                                    || order.getTimestamp().toDate().toString().toLowerCase()
                                    .contains(charString.toLowerCase())) {
                                filteredList.add(order);
                            }
                        }

                        completedOrdersFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = completedOrdersFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    completedOrdersFiltered = (ArrayList<OrderItem>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }

        class OrderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView tvDesc;
            TextView tvTimestamp;
            TextView tvPrice;
            Button btnAcceptOrder;
            ProgressBar pbLoading;
            ImageView imgLogo;

            OrderItem completedOrder;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDesc = itemView.findViewById(R.id.tv_orderDesc);
                tvTimestamp = itemView.findViewById(R.id.tv_timeStamp);
                tvPrice = itemView.findViewById(R.id.tv_orderPrice);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                btnAcceptOrder = itemView.findViewById(R.id.btn_orderInteraction);
                pbLoading = itemView.findViewById(R.id.pb_activeOrder);

                itemView.setOnClickListener(this);
            }

            void bindView(final OrderItem activeOrder) {
                this.completedOrder = activeOrder;

                RestaurantItem restaurantItem = restaurantItems.get(activeOrder.getRestaurantId());
                UserPrefs buyer = buyers.get(activeOrder.getClientId());

                if (restaurantItem != null && buyer != null) {
                    String message = "Order for " + buyer.getUserName() +
                            " (" + buyer.getUserPhone() + ") from "
                            + restaurantItem.getName() + " has been completed.";

                    tvDesc.setText(message);

                    tvPrice.setText(String.format(Locale.ENGLISH,
                            "%d", activeOrder.getCost()));

                    tvTimestamp.setText(activeOrder.getTimestamp().toDate().toString());

                    btnAcceptOrder.setText(R.string.close_order);

                    btnAcceptOrder.setOnClickListener(this);

                    refreshThumbnail(restaurantItem);
                }
                if (restaurantItem == null) {
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
                if (buyer == null) {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.item_activeOrder:
                        onButtonPressed(v.getId(), completedOrder);
                        break;
                    case R.id.btn_orderInteraction:
                        closeOrder();
                        break;
                }
            }

            private void closeOrder(){
                btnAcceptOrder.setVisibility(View.INVISIBLE);
                pbLoading.setVisibility(View.VISIBLE);

                fireDB.collection("transporters")
                        .document(completedOrder.getTransporterId())
                        .collection("completedDeliveries")
                        .document(completedOrder.getId())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Snackbar.make(myLayout, "You closed the order!",
                                        Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(myLayout, "Failed to close order.",
                                        Snackbar.LENGTH_SHORT).show();

                                btnAcceptOrder.setVisibility(View.VISIBLE);
                                pbLoading.setVisibility(View.INVISIBLE);
                            }
                        });
            }
        }
    }
}
