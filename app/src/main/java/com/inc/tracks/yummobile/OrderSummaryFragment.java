package com.inc.tracks.yummobile;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class OrderSummaryFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_ORDER_GROUPS = "order_groups";

    private HashMap<String, HashMap<String, Integer>> orderGroups;

    private HashMap<String, Integer> groupsPrices = new HashMap<>();

    private HashMap<String, String> groupsDescs = new HashMap<>();

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout btnCardPay;

    private ConstraintLayout btnDeliveryPay;

    private ProgressBar pbLoading;

    private TextView tvTotalPrice;

    public OrderSummaryFragment() {
        // Required empty public constructor
    }

    public static OrderSummaryFragment newInstance(HashMap param1) {
        OrderSummaryFragment fragment = new OrderSummaryFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER_GROUPS, param1);
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
        View fragView =  inflater.inflate(R.layout.fragment_order_summary, container, false);

        RecyclerView orderSummary = fragView.findViewById(R.id.rv_orderSummary);
        tvTotalPrice = fragView.findViewById(R.id.tv_totalPrice);

        orderSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        orderSummary.setAdapter(new OrderSummaryRVAdapter());

        btnCardPay =  fragView.findViewById(R.id.btn_cardPay);
        btnDeliveryPay = fragView.findViewById(R.id.btn_deliveryPay);

        pbLoading  = fragView.findViewById(R.id.pb_orderSummary);

        return fragView;
    }

    private void onButtonPressed(int buttonId) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId, orderGroups, groupsPrices, groupsDescs);
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
        onButtonPressed(v.getId());
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId, HashMap orderGroups,
                                   HashMap groupPrices, HashMap groupDescriptions);
    }

    private void setPaymentOptions(){
        if(groupsPrices.size() == orderGroups.size()){
            btnDeliveryPay.setVisibility(View.VISIBLE);
            btnCardPay.setVisibility(View.VISIBLE);

            pbLoading.setVisibility(View.INVISIBLE);

            btnCardPay.setOnClickListener(this);
            btnCardPay.setOnClickListener(this);
        }
        else{
            btnDeliveryPay.setVisibility(View.INVISIBLE);
            btnCardPay.setVisibility(View.INVISIBLE);

            pbLoading.setVisibility(View.VISIBLE);
        }
    }

    public class OrderSummaryRVAdapter extends RecyclerView.Adapter<OrderSummaryRVAdapter.RstViewHolder>{

        private List<RestaurantItem> restaurantItems = new ArrayList<>();

        OrderSummaryRVAdapter(){
            ArrayList<String> groups = new ArrayList<>(orderGroups.keySet());

            FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

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

            for(final String group : groups){
                final HashMap<String, Integer> itemCount = orderGroups.get(group);
                assert itemCount != null;
                ArrayList<String> items = new ArrayList<>(itemCount.keySet());

                fireDB.collection("restaurants")
                        .document(group)
                        .collection("menuItems")
                        .whereIn("id", items)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                List<MenuItem> menuItems =
                                        queryDocumentSnapshots.toObjects(MenuItem.class);

                                int price = 0;
                                StringBuilder strDesc = new StringBuilder();

                                for(MenuItem menuItem : menuItems){
                                    Integer mCount = itemCount.get(menuItem.getId());
                                    assert mCount != null;

                                    price += (menuItem.getPrice() * mCount);

                                    strDesc.append(mCount.toString()).append(" ")
                                            .append(menuItem.getName()).append(", ");
                                }

                                strDesc.delete(strDesc.length() - 2, strDesc.length());

                                groupsPrices.put(group, price);

                                groupsDescs.put(group, strDesc.toString());

                                setPaymentOptions();

                                notifyDataSetChanged();
                            }
                        });
            }
        }

        @NonNull
        @Override
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_order_summary, viewGroup, false);
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
            TextView tvDesc;
            TextView tvPrice;
            ImageView imgLogo;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_restaurantName);
                tvDesc = itemView.findViewById(R.id.tv_orderGroupDesc);
                tvPrice = itemView.findViewById(R.id.tv_orderGroupPrice);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
            }

            void bindView(RestaurantItem restaurantItem){
                tvName.setText(restaurantItem.getName());
                tvDesc.setText(groupsDescs.get(restaurantItem.getId()));

                Integer price = groupsPrices.get(restaurantItem.getId());
                assert price != null;
                tvPrice.setText(String.format(Locale.ENGLISH, "%d", price));

                int totalPrice = 0;
                for(Integer groupPrice : groupsPrices.values()){
                    totalPrice  += groupPrice;
                }
                tvTotalPrice.setText(String.format(Locale.ENGLISH, "%d", totalPrice));

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
}
