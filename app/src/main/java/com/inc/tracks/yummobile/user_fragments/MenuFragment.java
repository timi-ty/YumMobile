package com.inc.tracks.yummobile.user_fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.inc.tracks.yummobile.components.MenuItem;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.user_activities.OrderActivity;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.RestaurantItem;
import com.inc.tracks.yummobile.utils.GlideApp;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.inc.tracks.yummobile.utils.UIHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MenuFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_REST_ITEM = "arg_rest_path";
    private static final String ARG_ORDER_GROUPS = "arg_order_groups";

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private RestaurantItem activeRestItem;

    private RecyclerView rvFoodMenu;

    private TextView tvRestaurantName;

    private TextView tvRestaurantAddress;

    private Button btnCart;

    private FoodMenuRVAdapter menuItemsAdapter;

    private HashMap<String, HashMap<String, Integer>> orderGroups = new HashMap<>();
    private HashMap<String, Integer> orderItems = new HashMap<>();

    public MenuFragment() {
        // Required empty public constructor
    }


    public static MenuFragment newInstance(RestaurantItem restaurantItem) {
        MenuFragment fragment = new MenuFragment();
        Bundle args = new Bundle();
        if(restaurantItem == null) {
            restaurantItem  = new RestaurantItem();
        }
        args.putSerializable(ARG_REST_ITEM, restaurantItem);
        args.putSerializable(ARG_ORDER_GROUPS, new HashMap<>());
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            activeRestItem = (RestaurantItem) getArguments().getSerializable(ARG_REST_ITEM);
            orderGroups = (HashMap) getArguments().getSerializable(ARG_ORDER_GROUPS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_menu,
                container, false);

        myLayout = fragView.findViewById(R.id.layout_catalogue);

        rvFoodMenu = fragView.findViewById(R.id.rv_foodMenu);

        tvRestaurantName = fragView.findViewById(R.id.tv_restaurantName);

        tvRestaurantAddress = fragView.findViewById(R.id.tv_restaurantAddress);

        btnCart = fragView.findViewById(R.id.btn_cart);

        rvFoodMenu.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
        if(menuItemsAdapter == null){
            menuItemsAdapter = new FoodMenuRVAdapter();
        }
        if(rvFoodMenu.getAdapter() == null){
            rvFoodMenu.setAdapter(menuItemsAdapter);
        }
        menuItemsAdapter.getFilter().filter("");

        tvRestaurantName.setText(activeRestItem.getName());

        tvRestaurantAddress.setText(activeRestItem.getAddress());

        fragView.findViewById(R.id.btn_cart).setOnClickListener(this);
        fragView.findViewById(R.id.btn_emptyCart).setOnClickListener(this);
        fragView.findViewById(R.id.btn_back).setOnClickListener(this);

        return fragView;
    }

    private void onButtonPressed(int buttonId) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId);
        }
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeFragment.OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cart:
                goToCart();
                break;
            case R.id.btn_emptyCart:
                emptyCart();
                break;
            case R.id.btn_back:
                onButtonPressed(v.getId());
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int interactionId);
    }

    public void onSearchQuery(String newText){
        if(isVisible()){
            menuItemsAdapter.getFilter().filter(newText);
        }
    }

    private void goToCart(){
        Intent orderIntent = new Intent(getActivity(), OrderActivity.class);
        if(!orderItems.isEmpty() && activeRestItem != null){
            orderGroups.put(activeRestItem.getId(), orderItems);
        }
        if(!orderGroups.isEmpty()){
            orderIntent.putExtra("cart", orderGroups);
            startActivity(orderIntent);
        }
        else{
            final Snackbar emptyAlert =
                    Snackbar.make(myLayout, "Your cart is empty.", Snackbar.LENGTH_SHORT);
            emptyAlert.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    emptyAlert.dismiss();
                }
            });
            emptyAlert.setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
            emptyAlert.show();
        }
    }

    @SuppressWarnings("unchecked")
    private void emptyCart(){
        orderItems = new HashMap<>();
        if (getArguments() != null) {
            getArguments().putSerializable(ARG_ORDER_GROUPS, new HashMap<>());
            orderGroups = (HashMap) getArguments().getSerializable(ARG_ORDER_GROUPS);
        }
        updateActiveRestaurantItem(activeRestItem);
        final Snackbar emptyAlert =
                Snackbar.make(myLayout, "Your emptied your cart.", Snackbar.LENGTH_SHORT);
        emptyAlert.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emptyAlert.dismiss();
            }
        });
        emptyAlert.setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
        emptyAlert.show();
    }

    public void updateActiveRestaurantItem(RestaurantItem restaurantItem){
        tvRestaurantName.setText(activeRestItem.getName());

        tvRestaurantAddress.setText(activeRestItem.getAddress());

        if(!orderItems.isEmpty() && activeRestItem != null){
            orderGroups.put(activeRestItem.getId(), orderItems);
        }

        if (getArguments() != null) {
            getArguments().putSerializable(ARG_REST_ITEM, restaurantItem);
            activeRestItem = (RestaurantItem) getArguments().getSerializable(ARG_REST_ITEM);
        }

        menuItemsAdapter = new FoodMenuRVAdapter();

        rvFoodMenu.setAdapter(menuItemsAdapter);

        menuItemsAdapter.getFilter().filter("");

        rvFoodMenu.invalidate();
    }


    public class FoodMenuRVAdapter extends RecyclerView.Adapter<FoodMenuRVAdapter.MenuItemViewHolder>
            implements Filterable {

        private final String TAG = "FireStore";
        private ArrayList<MenuItem> menuItems = new ArrayList<>();
        private List<MenuItem> menuItemsFiltered = new ArrayList<>();

        private int activeMenuItemPos = -1;


        FoodMenuRVAdapter() {
            FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

            orderItems = orderGroups.get(activeRestItem.getId());

            if(orderItems == null){
                orderItems = new HashMap<>();
            }


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
                                MenuItem menuItem;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New menuItem: " + dc.getDocument().getData());

                                        menuItem = dc.getDocument().toObject(MenuItem.class);

                                        menuItems.add(menuItem);

                                        notifyItemInserted(menuItems.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified menuItem: " + dc.getDocument().getData());

                                        menuItem = dc.getDocument().toObject(MenuItem.class);

                                        for(MenuItem item : menuItems){
                                            if(item.getId().equals(menuItem.getId())){
                                                position = menuItems.indexOf(item);
                                                break;
                                            }
                                        }

                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed menuItem: " + dc.getDocument().getData());

                                        menuItem = dc.getDocument().toObject(MenuItem.class);

                                        for(MenuItem item : menuItems){
                                            if(item.getId().equals(menuItem.getId())){
                                                position = menuItems.indexOf(item);
                                                break;
                                            }
                                        }

                                        if(position >= 0){
                                            menuItems.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }
                        }
                    };

            if(activeRestItem.getId() != null){
                fireDB.collection("restaurants")
                        .document(activeRestItem.getId())
                        .collection("menuItems")
                        .addSnapshotListener(dataChangedListener);
            }
        }

        @NonNull
        @Override
        public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_catalogue_food, viewGroup, false);
            return new MenuItemViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull MenuItemViewHolder viewHolder, int i) {
            viewHolder.bindView(menuItemsFiltered.get(i));
        }

        @Override
        public int getItemCount() {
            return menuItemsFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();

                    if (charString.isEmpty()) {
                        menuItemsFiltered = menuItems;
                    } else {
                        List<MenuItem> filteredList = new ArrayList<>();
                        for (MenuItem item : menuItems) {
                            if (item.getName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(item);
                            }
                        }

                        menuItemsFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = menuItemsFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    menuItemsFiltered = (ArrayList<MenuItem>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }

        class MenuItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            MenuItem menuItem;

            View itemView;

            TextView tvName;
            TextView tvDesc;
            TextView tvPrice;
            TextView tvCount;
            TextView tvPad;
            ImageView imgLogo;

            View selectionIndicator;
            ImageButton fabPlus;
            ImageButton fabMinus;

            MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);

                this.itemView = itemView;

                tvName = itemView.findViewById(R.id.tv_menuItemName);
                tvDesc = itemView.findViewById(R.id.tv_menuItemDesc);
                tvPrice = itemView.findViewById(R.id.tv_menuItemPrice);
                tvCount = itemView.findViewById(R.id.tv_count);
                tvPad = itemView.findViewById(R.id.marginView);
                imgLogo = itemView.findViewById(R.id.img_menuItemLogo);

                selectionIndicator = itemView.findViewById(R.id.indicator);
                fabPlus = itemView.findViewById(R.id.btn_add);
                fabMinus = itemView.findViewById(R.id.btn_remove);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_add:
                        addToCart();
                        break;
                    case R.id.btn_remove:
                        removeFromCart();
                        break;
                }
            }

            void bindView(MenuItem menuItem){
                this.menuItem = menuItem;

                itemView.setOnClickListener(this);
                fabPlus.setOnClickListener(this);
                fabMinus.setOnClickListener(this);

                tvName.setText(menuItem.getName());
                tvDesc.setText(menuItem.getDescription());
                String price = "₦" + menuItem.getPrice();
                tvPrice.setText(price);

                updateOrderCount();


                refreshThumbnail();
            }


            private void refreshThumbnail() {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(menuItem.getImgRef());

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

            private void activateItem(){
                selectionIndicator.setVisibility(View.VISIBLE);
                tvCount.setVisibility(View.VISIBLE);
                tvPad.setPaddingRelative(0, 0, UIHelper.dpToPx(8), 0);
            }

            private void deactivateItem(){
                selectionIndicator.setVisibility(View.INVISIBLE);
                tvCount.setVisibility(View.GONE);
                tvPad.setPaddingRelative(0, 0, UIHelper.dpToPx(16), 0);
            }

            private void addToCart(){
                String mKey = menuItem.getId();
                if(orderItems.containsKey(mKey)){
                    Integer mCount = orderItems.get(mKey);

                    assert mCount != null;
                    orderItems.put(mKey, ++mCount);
                }
                else {
                    orderItems.put(mKey, 1);
                }
                updateOrderCount();
            }

            private void addToCart(String strCount){
                int count = 0;
                try{
                    count = Integer.parseInt(strCount);
                }
                catch (NumberFormatException e){
                    e.printStackTrace();
                }
                String mKey = menuItem.getId();
                if(count >= 1){
                    orderItems.put(mKey, count);
                }
                else {
                    orderItems.remove(mKey);
                }
                if(orderItems.isEmpty()){
                    orderGroups.remove(activeRestItem.getId());
                }
                updateOrderCount();
            }

            private void removeFromCart(){
                String mKey = menuItem.getId();
                if(orderItems.containsKey(mKey)){
                    Integer mCount = orderItems.get(mKey);

                    assert mCount != null;
                    if(mCount >= 2){
                        orderItems.put(mKey, --mCount);
                    }
                    else{
                        orderItems.remove(mKey);
                    }
                    if(orderItems.isEmpty()){
                        orderGroups.remove(activeRestItem.getId());
                    }
                }
                updateOrderCount();
            }

            private void updateOrderCount(){
                if(orderItems.containsKey(menuItem.getId())){
                    Integer mCount = orderItems.get(menuItem.getId());
                    assert mCount != null;
                    String strCount = String.format(Locale.ENGLISH, "%d", mCount) + "X";
                    tvCount.setText(strCount);

                    if(mCount > 0){
                        activateItem();
                    }
                    else {
                        deactivateItem();
                    }
                }
                else{
                    String strCount = String.format(Locale.ENGLISH, "%d", 0);

                    tvCount.setText(strCount);

                    deactivateItem();
                }

                if(orderItems.isEmpty() && orderGroups.isEmpty()){
                    btnCart.setVisibility(View.GONE);
                }
                else {
                    btnCart.setVisibility(View.VISIBLE);
                }
            }

            private void selectActiveMenuItemFromPosition(int position){
                int formerActive = activeMenuItemPos;

                activeMenuItemPos = position;

                menuItemsAdapter.notifyItemChanged(formerActive);


                menuItemsAdapter.notifyItemChanged(activeMenuItemPos);

                rvFoodMenu.scrollToPosition(activeMenuItemPos);

                Log.d("Selected Menu Item",  "" + activeMenuItemPos);
            }
        }
    }
}
