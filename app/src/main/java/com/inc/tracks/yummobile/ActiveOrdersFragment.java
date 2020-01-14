package com.inc.tracks.yummobile;

import android.content.Context;
import android.graphics.drawable.shapes.PathShape;
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

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;


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
        private ArrayList<ActiveOrder> activeOrders = new ArrayList<>();
        private HashMap<String, String> restNames = new HashMap<>();

        ActiveOrdersRVAdapter() {
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
                                ActiveOrder activeOrder;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New Restaurant: " + dc.getDocument().getData());

                                        activeOrder = dc.getDocument().toObject(ActiveOrder.class);

                                        activeOrder.setId(dc.getDocument().getId());

                                        activeOrders.add(activeOrder);

                                        notifyItemInserted(activeOrders.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified Restaurant: " + dc.getDocument().getData());

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
                                        Log.d(TAG, "Removed Restaurant: " + dc.getDocument().getData());

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

        class MenuItemViewHolder extends RecyclerView.ViewHolder{

            MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);
            }

            void bindView(ActiveOrder activeOrder){
                String restName = restNames.get(activeOrder.getRestaurantId());
                if(restName != null){

                }
                else {

                }
            }
        }
    }
}
