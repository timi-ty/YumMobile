package com.inc.tracks.yummobile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class ManagerOrderDetailsFragment extends Fragment{

    private static final String ARG_ORDER = "currentOrder";

    private ActiveOrder activeOrder;

    private FirebaseFirestore fireDB;

    private OnFragmentInteractionListener mListener;

    public ManagerOrderDetailsFragment() {
        // Required empty public constructor
    }


    public static ManagerOrderDetailsFragment newInstance(ActiveOrder activeOrder) {
        ManagerOrderDetailsFragment fragment = new ManagerOrderDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, activeOrder);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            activeOrder = (ActiveOrder) getArguments().getSerializable(ARG_ORDER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_order_details, container, false);

        fireDB = FirebaseFirestore.getInstance();

        RecyclerView rvItems = fragView.findViewById(R.id.rv_orderItems);

        final TextView tvName = fragView.findViewById(R.id.tv_clientName);

        final TextView tvPhone = fragView.findViewById(R.id.tv_clientPhone);

        final TextView tvLocation = fragView.findViewById(R.id.tv_clientLocation);

        final TextView tvTotalCost = fragView.findViewById(R.id.tv_totalPrice);

        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvItems.setAdapter(new OrderRVAdapter());

        mListener.onFragmentInteraction(R.layout.fragment_manager_order_details);

        fireDB.collection("users")
                .document(activeOrder.getClientId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        UserPrefs userPrefs = documentSnapshot.toObject(UserPrefs.class);
                        assert userPrefs != null;

                        tvName.setText(userPrefs.getUserName());
                        tvPhone.setText(userPrefs.getUserPhone());
                        tvTotalCost.setText(String.format(Locale.ENGLISH, "%d", activeOrder.getCost()));
                    }
                });

        return fragView;
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
    }

    public class OrderRVAdapter extends RecyclerView.Adapter<OrderRVAdapter.RstViewHolder>{

        private List<MenuItem> orderItems = new ArrayList<>();
        private HashMap<String, Integer> itemQuantities;

        OrderRVAdapter() {
            List<String> itemIds = new ArrayList<>(activeOrder.getOrderItems().keySet());
            itemQuantities = activeOrder.getOrderItems();

            fireDB.collection("restaurants")
                    .document(activeOrder.getRestaurantId())
                    .collection("menuItems")
                    .whereIn("id", itemIds)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            orderItems = queryDocumentSnapshots.toObjects(MenuItem.class);
                            notifyDataSetChanged();
                        }
                    });
        }

        @NonNull
        @Override
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_order_detail, viewGroup, false);
            return new RstViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int position) {
            MenuItem orderItem = orderItems.get(position);
            viewHolder.bindView(orderItem);
        }

        @Override
        public int getItemCount() {
            return orderItems.size();
        }

        class RstViewHolder extends RecyclerView.ViewHolder{
            TextView tvName;
            TextView tvPriceAndCount;
            TextView tvSubCost;
            ImageView imgLogo;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_menuItemName);
                tvPriceAndCount = itemView.findViewById(R.id.tv_priceAndCount);
                tvSubCost = itemView.findViewById(R.id.tv_subCost);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
            }

            void bindView(final MenuItem orderItem){
                Integer quantity = itemQuantities.get(orderItem.getId());
                assert quantity != null;

                String strPriceCount = orderItem.getPrice() + " X "
                        + itemQuantities.get(orderItem.getId());
                String strSubCost = String.format(Locale.ENGLISH, "%d",
                        orderItem.getPrice() * quantity);

                tvName.setText(orderItem.getName());
                tvPriceAndCount.setText(strPriceCount);
                tvSubCost.setText(strSubCost);

                refreshThumbnail(orderItem);
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
