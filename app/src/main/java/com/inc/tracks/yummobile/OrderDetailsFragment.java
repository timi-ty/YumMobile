package com.inc.tracks.yummobile;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class OrderDetailsFragment extends Fragment {

    private static final String ARG_ORDER_GROUPS = "order_groups";

    private HashMap<String, HashMap<String, Integer>> orderGroups;

    private OnFragmentInteractionListener mListener;

    private FirebaseFirestore fireDB;

    public OrderDetailsFragment() {
        // Required empty public constructor
    }

    public static OrderDetailsFragment newInstance(HashMap hashParam) {
        OrderDetailsFragment fragment = new OrderDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER_GROUPS, hashParam);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderGroups = (HashMap) getArguments().getSerializable(ARG_ORDER_GROUPS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragView = inflater.inflate(R.layout.fragment_order_details,
                container, false);

        RecyclerView orderDetails = fragView.findViewById(R.id.rv_orderDetails);

        orderDetails.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
        orderDetails.setAdapter(new OrderDetailsRVAdapter());

        Button checkoutButton = fragView.findViewById(R.id.btn_checkout);
        checkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(Uri.fromParts("Fragment",
                        "onClick", "OrderDetails"));
            }
        });

        return fragView;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
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
        void onFragmentInteraction(Uri uri);
    }

    public class OrderDetailsRVAdapter extends RecyclerView.Adapter<OrderDetailsRVAdapter.RstViewHolder>{

        private List<RestaurantItem> restaurantItems = new ArrayList<>();

        public OrderDetailsRVAdapter() {
            ArrayList<String> groups = new ArrayList<>(orderGroups.keySet());

            fireDB = FirebaseFirestore.getInstance();

            fireDB.collection("restaurants")
                    .whereIn("id", groups)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            restaurantItems = queryDocumentSnapshots.toObjects(RestaurantItem.class);
                            notifyDataSetChanged();
                        }
                    });
        }

        @NonNull
        @Override
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_order_detail_group, viewGroup, false);
            return new RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int i) {
            viewHolder.bindView(restaurantItems.get(i));
        }

        @Override
        public int getItemCount() {
            return restaurantItems.size();
        }

        class RstViewHolder extends RecyclerView.ViewHolder{

            TextView tvName;
            ImageView imgLogo;

            RecyclerView orderItemsRV;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                orderItemsRV = itemView.findViewById(R.id.rv_orderItems);
                tvName = itemView.findViewById(R.id.tv_restaurantName);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);

                orderItemsRV.setLayoutManager(new LinearLayoutManager(getContext(),
                        LinearLayoutManager.HORIZONTAL, false));
            }

            void bindView(RestaurantItem restaurantItem){
                orderItemsRV.setAdapter(new OrderItemsRVAdapter(restaurantItem.getId()));
                tvName.setText(restaurantItem.getName());

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
        }
    }

    public class OrderItemsRVAdapter extends RecyclerView.Adapter<OrderItemsRVAdapter.MenuItemViewHolder>{

        private List<MenuItem> menuItems = new ArrayList<>();
        private HashMap<String, Integer> itemCount;

        public OrderItemsRVAdapter(String groupId) {
            itemCount = orderGroups.get(groupId);
            assert itemCount != null;
            ArrayList<String> items = new ArrayList<>(itemCount.keySet());

            fireDB = FirebaseFirestore.getInstance();

            fireDB.collection("restaurants")
                    .document(groupId)
                    .collection("menuItems")
                    .whereIn("id", items)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            menuItems = queryDocumentSnapshots.toObjects(MenuItem.class);
                            notifyDataSetChanged();
                        }
                    });
        }

        @NonNull
        @Override
        public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View orderItemView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_order_detail_food, viewGroup, false);
            return new MenuItemViewHolder(orderItemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MenuItemViewHolder viewHolder, int i) {
            viewHolder.bindView(menuItems.get(i));
        }

        @Override

        public int getItemCount() {
            return menuItems.size();
        }
        class MenuItemViewHolder extends RecyclerView.ViewHolder{
            View orderItem;

            TextView tvName;
            ImageView imgLogo;

            MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);
                orderItem = itemView;

                tvName = itemView.findViewById(R.id.tv_menuItemName);
                imgLogo = itemView.findViewById(R.id.img_menuItemLogo);
            }

            void bindView(MenuItem menuItem){
                String countName = itemCount.get(menuItem.getId()) + " X "+ menuItem.getName();
                tvName.setText(countName);

                refreshThumbnail(menuItem);
            }

            private void refreshThumbnail(MenuItem item) {
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
        }
    }
}
