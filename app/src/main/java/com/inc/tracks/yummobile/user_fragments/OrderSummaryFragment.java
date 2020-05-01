package com.inc.tracks.yummobile.user_fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.inc.tracks.yummobile.components.MenuItem;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.utils.GlideApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class OrderSummaryFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_ORDER_GROUPS = "order_groups";
    public static final String OPTION_CARD = "card";
    public static final String OPTION_CASH = "cash";

    private HashMap<String, HashMap<String, Integer>> orderGroups;

    private HashMap<String, Integer> groupsPrices = new HashMap<>();

    private HashMap<String, String> groupsDescs = new HashMap<>();

    private String paymentMethod;

    private OnFragmentInteractionListener mListener;

    private RecyclerView rvPaymentOptions;

    private TextView tvDefaultPayment;

    private ImageView imvDefaultPayment;

    private Button btnCheckout;

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
        tvTotalPrice = fragView.findViewById(R.id.tv_subTotal);

        orderSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        orderSummary.setAdapter(new OrderSummaryRVAdapter());

        tvDefaultPayment =  fragView.findViewById(R.id.tv_defaultPayment);
        imvDefaultPayment = fragView.findViewById(R.id.imv_defaultPayment);

        rvPaymentOptions = fragView.findViewById(R.id.rv_paymentOptions);

        rvPaymentOptions.setLayoutManager(new LinearLayoutManager(getContext()));

        btnCheckout = fragView.findViewById(R.id.btn_checkout);

        pbLoading  = fragView.findViewById(R.id.pb_orderSummary);

        fragView.findViewById(R.id.btn_back).setOnClickListener(this);

        setCheckoutOption();

        return fragView;
    }

    private void onButtonPressed(int buttonId) {
        if (mListener != null) {
            switch (buttonId){
                case R.id.btn_checkout:
                    mListener.onFragmentInteraction(buttonId, orderGroups, groupsPrices,
                            groupsDescs, paymentMethod);
                    break;
                case R.id.btn_back:
                    mListener.onFragmentInteraction(buttonId);
                    break;
            }
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
        switch (v.getId()){
            case R.id.btn_checkout:
                showCheckoutButton(false);
            case R.id.btn_back:
                onButtonPressed(v.getId());
//                break;
//            case R.id.img_card:
//            case R.id.img_cash:
//                pickPaymentOption(v.getId());
                break;
        }
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId, HashMap orderGroups, HashMap groupPrices,
                                   HashMap groupDescriptions, String paymentMethod);
        void onFragmentInteraction(int buttonId);
    }

    private void setCheckoutOption(){
        if(groupsPrices.size() == orderGroups.size()){
            showCheckoutButton(true);
            btnCheckout.setEnabled(false);


            tvDefaultPayment.setOnClickListener(this);
            imvDefaultPayment.setOnClickListener(this);
            btnCheckout.setOnClickListener(this);
        }
        else{
            showCheckoutButton(false);
        }
    }

    private void computePrice(){
        if(groupsPrices.size() == orderGroups.size()) {
            int totalPrice = 0;
            for (Integer groupPrice : groupsPrices.values()) {
                totalPrice += groupPrice;
            }
            String price = "₦" + totalPrice;
            tvTotalPrice.setText(price);
        }
    }

//    private void pickPaymentOption(int option){
//        if(option == R.id.img_card){
//            tvDefaultPayment.setSelected(true);
//            imvDefaultPayment.setSelected(false);
//
//            tvCardPay.setSelected(true);
//            tvCashPay.setSelected(false);
//
//            btnCheckout.setEnabled(true);
//
//            paymentMethod = OPTION_CARD;
//        }
//        else if(option == R.id.img_cash){
//            tvDefaultPayment.setSelected(false);
//            imvDefaultPayment.setSelected(true);
//
//            tvCardPay.setSelected(false);
//            tvCashPay.setSelected(true);
//
//            btnCheckout.setEnabled(true);
//
//            paymentMethod = OPTION_CASH;
//        }
//    }

    private void showPaymentOptions(boolean show){
        rvPaymentOptions.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    private void showCheckoutButton(boolean show){
        btnCheckout.setVisibility(show ? View.VISIBLE : View.INVISIBLE);

        pbLoading.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    public class OrderSummaryRVAdapter extends RecyclerView.Adapter<OrderSummaryRVAdapter.RstViewHolder>{

        private List<MenuItem> menuItems = new ArrayList<>();
        private HashMap<String, Integer> itemCount = new HashMap<>();
        private HashMap<String, String> itemGroups = new HashMap<>();

        OrderSummaryRVAdapter(){
            ArrayList<String> groups = new ArrayList<>(orderGroups.keySet());

            FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

            for(final String group : groups){
                itemCount.putAll(Objects.requireNonNull(orderGroups.get(group)));
                assert itemCount != null;
                ArrayList<String> items = new ArrayList<>(Objects.requireNonNull(orderGroups
                        .get(group)).keySet());

                fireDB.collection("restaurants")
                        .document(group)
                        .collection("menuItems")
                        .whereIn("id", items)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                List<MenuItem> subMenuItems =
                                        queryDocumentSnapshots.toObjects(MenuItem.class);

                                menuItems.addAll(subMenuItems);

                                int price = 0;
                                StringBuilder strDesc = new StringBuilder();

                                for(MenuItem menuItem : subMenuItems){
                                    itemGroups.put(menuItem.getId(), group);
                                    Integer mCount = itemCount.get(menuItem.getId());
                                    assert mCount != null;

                                    price += (menuItem.getPrice() * mCount);

                                    strDesc.append(mCount.toString()).append(" ")
                                            .append(menuItem.getName()).append(", ");
                                }

                                strDesc.delete(strDesc.length() - 2, strDesc.length());

                                groupsPrices.put(group, price);

                                groupsDescs.put(group, strDesc.toString());

                                setCheckoutOption();

                                computePrice();

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
            viewHolder.bindView(menuItems.get(i));
        }

        @Override
        public int getItemCount() {
            return menuItems.size();
        }

        class RstViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvName;
            TextView tvCount;
            TextView tvDesc;
            TextView tvPrice;
            ImageView imgLogo;
            ImageButton btnDelete;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_menuItemName);
                tvCount = itemView.findViewById(R.id.tv_count);
                tvDesc = itemView.findViewById(R.id.tv_menuItemDesc);
                tvPrice = itemView.findViewById(R.id.tv_menuItemPrice);
                imgLogo = itemView.findViewById(R.id.img_menuItemLogo);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }

            void bindView(MenuItem menuItem){
                tvName.setText(menuItem.getName());
                tvDesc.setText(menuItem.getDescription());

                String price = "₦" + (menuItem.getPrice() * itemCount.get(menuItem.getId()));
                tvPrice.setText(price);

                String count = itemCount.get(menuItem.getId()) + "X";

                tvCount.setText(count);

                btnDelete.setOnClickListener(this);

                refreshThumbnail(menuItem);
            }

            private void refreshThumbnail(MenuItem item) {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(item.getImgRef());

                    GlideApp.with(imgLogo.getContext())
                            .load(imageRef)
                            .transform(new CenterCrop(), new RoundedCorners(16))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imgLogo);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onClick(View v) {
                if(v.getId() == R.id.btn_delete){
                    deleteMenuItem();
                }
            }

            private void deleteMenuItem(){
                int current = getAdapterPosition();
                MenuItem item = menuItems.get(current);

                String groupId = itemGroups.get(item.getId());

                HashMap<String, Integer> orderGroup = orderGroups.get(groupId);

                assert orderGroup != null;
                orderGroup.remove(item.getId());

                orderGroups.put(groupId, orderGroup);

                menuItems.remove(current);
                notifyItemRemoved(current);

                int price = 0;
                StringBuilder strDesc = new StringBuilder();

                for(String itemId : orderGroup.keySet()){
                    for(MenuItem menuItem : menuItems){
                        if(!menuItem.getId().equals(itemId)) continue;
                        Integer mCount = orderGroup.get(menuItem.getId());
                        assert mCount != null;

                        price += (menuItem.getPrice() * mCount);

                        strDesc.append(mCount.toString()).append(" ")
                                .append(menuItem.getName()).append(", ");
                        break;
                    }
                }

                strDesc.delete(strDesc.length() - 2, strDesc.length());

                groupsPrices.put(groupId, price);

                groupsDescs.put(groupId, strDesc.toString());

                computePrice();
            }
        }
    }
}
