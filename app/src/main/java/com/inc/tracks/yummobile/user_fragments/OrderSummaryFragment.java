package com.inc.tracks.yummobile.user_fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import com.inc.tracks.yummobile.components.CardInfo;
import com.inc.tracks.yummobile.components.MenuItem;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.utils.CardManager;
import com.inc.tracks.yummobile.utils.GlideApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class OrderSummaryFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_ORDER_GROUPS = "order_groups";

    private HashMap<String, HashMap<String, Integer>> orderGroups;

    private HashMap<String, Integer> groupsPrices = new HashMap<>();

    private HashMap<String, String> groupsDescs = new HashMap<>();

    private CardInfo paymentMethod;

    private CardManager cardManager;

    private OnFragmentInteractionListener mListener;

    private RecyclerView rvPaymentOptions;

    private TextView tvDefaultPayment;

    private ImageView imvDefaultPayment;

    private ImageButton btnMorePaymentOptions;

    private Button btnCheckout;

    private ProgressBar pbLoading;

    private TextView tvSubTotal;

    private TextView tvDeliveryFee;

    private TextView tvVAT;

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
        tvSubTotal = fragView.findViewById(R.id.tv_subTotal);
        tvDeliveryFee = fragView.findViewById(R.id.tv_deliveryFee);
        tvVAT = fragView.findViewById(R.id.tv_vat);
        tvTotalPrice = fragView.findViewById(R.id.tv_totalPrice);

        orderSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        orderSummary.setAdapter(new OrderSummaryRVAdapter());

        tvDefaultPayment =  fragView.findViewById(R.id.tv_defaultPayment);
        imvDefaultPayment = fragView.findViewById(R.id.imv_defaultPayment);
        btnMorePaymentOptions = fragView.findViewById(R.id.img_arrowDown);

        rvPaymentOptions = fragView.findViewById(R.id.rv_paymentOptions);
        rvPaymentOptions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPaymentOptions.setAdapter(new PaymentOptionsRVAdapter());

        btnCheckout = fragView.findViewById(R.id.btn_checkout);

        pbLoading  = fragView.findViewById(R.id.pb_orderSummary);

        fragView.findViewById(R.id.btn_back).setOnClickListener(this);

        setCheckoutOption();
        setPaymentMethod(cardManager.getDefaultPaymentMethod(fragView.getContext()));

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

        cardManager = new CardManager(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        cardManager.finishManagingCards();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_checkout:
                showCheckoutButton(false);
            case R.id.btn_back:
                onButtonPressed(v.getId());
                break;
            case R.id.tv_defaultPayment:
            case R.id.imv_defaultPayment:
            case R.id.img_arrowDown:
                showPaymentOptions(true);
                break;
        }
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId, HashMap orderGroups, HashMap groupPrices,
                                   HashMap groupDescriptions, CardInfo paymentMethod);
        void onFragmentInteraction(int buttonId);
    }

    private void setCheckoutOption(){
        if(groupsPrices.size() == orderGroups.size()){
            showCheckoutButton(true);

            tvDefaultPayment.setOnClickListener(this);
            imvDefaultPayment.setOnClickListener(this);
            btnMorePaymentOptions.setOnClickListener(this);
            btnCheckout.setOnClickListener(this);
        }
        else{
            showCheckoutButton(false);
        }
    }

    private void computePrice(){
        if(groupsPrices.size() == orderGroups.size()) {
            int totalPrice = 0;
            int delivery = 500;
            int vat = 0;
            for (Integer groupPrice : groupsPrices.values()) {
                totalPrice += groupPrice;
            }
            String subTotal = "₦" + totalPrice;
            String deliveryFee = "₦" + delivery;
            String vatFee = "₦" + vat;
            String total = "₦" + (totalPrice + delivery + vat);
            tvSubTotal.setText(subTotal);
            tvDeliveryFee.setText(deliveryFee);
            tvVAT.setText(vatFee);
            tvTotalPrice.setText(total);
        }
    }

    private void setPaymentMethod(CardInfo paymentMethod){
        this.paymentMethod = paymentMethod;
        cardManager.saveDefaultPaymentMethod(paymentMethod, tvDefaultPayment.getContext());
        tvDefaultPayment.setText(cardManager.getDefaultPaymentMethodName(tvDefaultPayment.getContext()));
        imvDefaultPayment.setImageResource(cardManager.getCardHolderIcon(paymentMethod));
        showPaymentOptions(false);
    }

    private void showPaymentOptions(boolean show){
        rvPaymentOptions.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    private void showCheckoutButton(boolean show){
        btnCheckout.setVisibility(show ? View.VISIBLE : View.INVISIBLE);

        pbLoading.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    public void refreshPaymentOptions(){
        rvPaymentOptions.setAdapter(new PaymentOptionsRVAdapter());
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

    public class PaymentOptionsRVAdapter extends RecyclerView.Adapter<PaymentOptionsRVAdapter.OptionViewHolder>{

        private List<CardInfo> cardInfoList = new ArrayList<>();

        PaymentOptionsRVAdapter(){
            for(int i = 0; i < cardManager.getCardCount(); i++){
                cardInfoList.add(cardManager.getSavedCard(i));
            }
        }

        @NonNull
        @Override
        public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View optionView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_payment_method, viewGroup, false);
            return new OptionViewHolder(optionView);
        }

        @Override
        public void onBindViewHolder(@NonNull OptionViewHolder viewHolder, int i) {
            if(i > 0 && i <= cardInfoList.size()) {
                viewHolder.bindView(cardInfoList.get(i - 1));
            }
            else{
                viewHolder.bindView(i);
            }
        }

        @Override
        public int getItemCount() {
            return cardInfoList.size() + 2;
        }

        class OptionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            View itemView;
            CardInfo mCardInfo;

            TextView tvCardNumber;
            ImageView imgHolderLogo;
            ImageView imgSelected;

            OptionViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                tvCardNumber = itemView.findViewById(R.id.tv_cardNumber);
                imgHolderLogo = itemView.findViewById(R.id.img_holderIcon);
                imgSelected = itemView.findViewById(R.id.check_mark);
            }

            void bindView(CardInfo cardInfo){
                mCardInfo = cardInfo;

                tvCardNumber.setText(cardManager.formatCardNumber(cardInfo.getCardNumber()));

                itemView.setOnClickListener(this);

                imgSelected.setVisibility(cardInfo.getId() ==
                        cardManager.getDefaultPaymentMethodId(itemView.getContext()) ?
                        View.VISIBLE : View.INVISIBLE);

                setHolderThumbnail(cardInfo);
            }

            void bindView(int pos){
                if(pos == 0){
                    mCardInfo = null;

                    tvCardNumber.setText(R.string.prompt_delivery_payment);

                    imgSelected.setVisibility(cardManager.getDefaultPaymentMethodId(itemView.getContext())
                            == -1 ? View.VISIBLE : View.INVISIBLE);

                    imgHolderLogo.setImageResource(R.drawable.ic_cash);

                    itemView.setOnClickListener(this);
                }
                else{
                    tvCardNumber.setText(R.string.add_card);

                    imgSelected.setVisibility(View.INVISIBLE);

                    Drawable drawable = itemView.getContext().getDrawable(R.drawable.ic_add);
                    assert drawable != null;
                    drawable.setTint(itemView.getContext().getResources().getColor(R.color.colorDark));
                    imgHolderLogo.setImageDrawable(drawable);

                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onFragmentInteraction(R.id.btn_addCard);
                        }
                    });
                }
            }

            private void setHolderThumbnail(CardInfo cardInfo) {
                imgHolderLogo.setImageResource(cardManager.getCardHolderIcon(cardInfo));
            }

            @Override
            public void onClick(View v) {
                setPaymentMethod(mCardInfo);
                notifyDataSetChanged();
            }
        }
    }
}
