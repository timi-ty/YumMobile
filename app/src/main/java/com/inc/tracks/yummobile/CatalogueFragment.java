package com.inc.tracks.yummobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;


public class CatalogueFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_REST_ITEM = "arg_rest_path";
    private static final String ARG_ORDER_GROUPS = "arg_order_groups";

    private ConstraintLayout myLayout;

    private RestaurantItem activeRestItem;
    private int activeRestPosition;

    private RecyclerView rvRestaurantList;
    private RecyclerView rvFoodMenu;

    private TextView tvRestaurantName;

    private RestaurantsRVAdapter restaurantsAdapter;
    private FoodMenuRVAdapter menuItemsAdapter;

    private FirebaseFirestore fireDB;

    private HashMap<String, HashMap<String, Integer>> orderGroups = new HashMap<>();
    private HashMap<String, Integer> orderItems = new HashMap<>();

    public CatalogueFragment() {
        // Required empty public constructor
    }


    public static CatalogueFragment newInstance(RestaurantItem restaurantItem) {
        CatalogueFragment fragment = new CatalogueFragment();
        Bundle args = new Bundle();
        if(restaurantItem == null) {
            restaurantItem  = new RestaurantItem();
        }
        args.putSerializable(ARG_REST_ITEM, restaurantItem);
        args.putSerializable(ARG_ORDER_GROUPS, new HashMap<>());
        fragment.setArguments(args);
        return fragment;
    }

    public static CatalogueFragment newInstance(HashMap orderGroups) {
        CatalogueFragment fragment = new CatalogueFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER_GROUPS, orderGroups);
        args.putSerializable(ARG_REST_ITEM, new RestaurantItem());
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
        View fragView = inflater.inflate(R.layout.fragment_catalogue,
                container, false);

        myLayout = fragView.findViewById(R.id.layout_catalogue);

        rvRestaurantList = fragView.findViewById(R.id.rv_restaurantList);
        rvFoodMenu = fragView.findViewById(R.id.rv_foodMenu);

        tvRestaurantName = fragView.findViewById(R.id.tv_restaurantName);

        rvRestaurantList.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
        if(restaurantsAdapter == null){
            restaurantsAdapter = new RestaurantsRVAdapter();
        }
        if(rvRestaurantList.getAdapter() == null){
            rvRestaurantList.setAdapter(restaurantsAdapter);
        }

        rvFoodMenu.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
        if(menuItemsAdapter == null){
            menuItemsAdapter = new FoodMenuRVAdapter();
        }
        if(rvFoodMenu.getAdapter() == null){
            rvFoodMenu.setAdapter(menuItemsAdapter);
        }

        fragView.findViewById(R.id.fab_cart).setOnClickListener(this);

        selectActiveRestaurantFromId();

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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab_cart) {
            goToCart();
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

    private void selectActiveRestaurantFromId(){
        if(activeRestItem.getId() != null){
            Handler scrollHandler = new Handler(Objects.requireNonNull(getActivity()).getMainLooper());
            scrollHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean foundRestaurant = false;
                    for (RestaurantItem item : restaurantsAdapter.restaurantItems){
                        if(item.getId().equals(activeRestItem.getId())){
                            int selectedPosition = restaurantsAdapter.restaurantItems
                                    .indexOf(item);

                            selectActiveRestaurantFromPosition(selectedPosition);

                            foundRestaurant = true;

                            break;
                        }
                    }
                    if(!foundRestaurant){
                        Handler scrollHandler = new Handler(Objects.requireNonNull(getActivity()).getMainLooper());
                        scrollHandler.postDelayed(this, 100);
                        Log.d("ScrollHandler", "Restaurant not found, retrying...");
                    }
                }
            }, 100);
        }
    }

    private void selectActiveRestaurantFromPosition(int position){
        int formerActive = activeRestPosition;

        activeRestPosition = position;

        restaurantsAdapter.notifyItemChanged(formerActive);


        restaurantsAdapter.notifyItemChanged(activeRestPosition);

        tvRestaurantName.setText(activeRestItem.getName());

        rvRestaurantList.scrollToPosition(activeRestPosition);

        Log.d("Selected Rest Item",  "" + activeRestPosition);
    }

    void updateActiveRestaurantItem(RestaurantItem restaurantItem){
        if(!orderItems.isEmpty() && activeRestItem != null){
            orderGroups.put(activeRestItem.getId(), orderItems);
        }

        if (getArguments() != null) {
            getArguments().putSerializable(ARG_REST_ITEM, restaurantItem);
            activeRestItem = (RestaurantItem) getArguments().getSerializable(ARG_REST_ITEM);
        }

        menuItemsAdapter = new FoodMenuRVAdapter();

        rvFoodMenu.setAdapter(menuItemsAdapter);

        rvFoodMenu.invalidate();
    }

    @SuppressWarnings("unchecked")
    void updateOrderGroups(HashMap<String, HashMap<String, Integer>> orderGroups){
        this.orderGroups = orderGroups;
        if (getArguments() != null) {
            getArguments().putSerializable(ARG_ORDER_GROUPS, orderGroups);
            this.orderGroups = (HashMap) getArguments().getSerializable(ARG_ORDER_GROUPS);
        }

        menuItemsAdapter = new FoodMenuRVAdapter();

        rvFoodMenu.setAdapter(menuItemsAdapter);

        rvFoodMenu.invalidate();
    }


    public class RestaurantsRVAdapter extends RecyclerView.Adapter<RestaurantsRVAdapter.RstViewHolder>{

        private final String TAG = "FireStore";
        private ArrayList<RestaurantItem> restaurantItems = new ArrayList<>();

        RestaurantsRVAdapter() {
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
                                RestaurantItem restaurantItem;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        restaurantItem.setId(dc.getDocument().getId());

                                        restaurantItems.add(restaurantItem);

                                        notifyItemInserted(restaurantItems.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        for(RestaurantItem item : restaurantItems){
                                            if(item.getId().equals(restaurantItem.getId())){
                                                position = restaurantItems.indexOf(item);
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        for(RestaurantItem item : restaurantItems){
                                            if(item.getId().equals(restaurantItem.getId())){
                                                position = restaurantItems.indexOf(item);
                                                Log.d(TAG, "Removed Restaurant Notified!: " + item.getId()
                                                        + " => " + restaurantItem.getId() + " => " + restaurantItems.indexOf(item));
                                            }
                                        }

                                        if(position >= 0){
                                            restaurantItems.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("restaurants").addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_catalogue_restaurant, viewGroup, false);
            return new RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int position) {
            RestaurantItem restaurantItem = restaurantItems.get(position);
            viewHolder.bindView(restaurantItem);
        }

        @Override
        public int getItemCount() {
            return restaurantItems.size();
        }

        class RstViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvName;
            ImageView imgLogo;
            ConstraintLayout container;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_restaurantName);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);
                container = itemView.findViewById(R.id.item_catalogueRestaurant);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();

                updateActiveRestaurantItem(restaurantItems.get(position));

                selectActiveRestaurantFromPosition(position);
            }

            void bindView(RestaurantItem restaurantItem){
                tvName.setText(restaurantItem.getName());

                if(getAdapterPosition() == activeRestPosition){
                    activateItem();
                }
                else {
                    deactivateItem();
                }

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

            private void activateItem(){
                container.setBackgroundResource(R.drawable.background_highlight);
            }

            private void deactivateItem(){
                container.setBackground(null);
            }
        }
    }

    public class FoodMenuRVAdapter extends RecyclerView.Adapter<FoodMenuRVAdapter.MenuItemViewHolder>{

        private final String TAG = "FireStore";
        private ArrayList<MenuItem> menuItems = new ArrayList<>();

        private int activeMenuItemPos = -1;


        FoodMenuRVAdapter() {
            fireDB = FirebaseFirestore.getInstance();

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
            if(i >= menuItems.size()){
                viewHolder.bindPlaceholder();
            }
            else {
                viewHolder.bindView(menuItems.get(i));
            }
        }

        @Override
        public int getItemCount() {
            return menuItems.size() + 1;
        }

        class MenuItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            MenuItem menuItem;

            View itemView;

            TextView tvName;
            TextView tvDesc;
            TextView tvPrice;
            TextView tvCount;
            ImageView imgLogo;

            ConstraintLayout vgSelectorView;
            FloatingActionButton fabPlus;
            FloatingActionButton fabMinus;
            ImageButton btnDoneSelector;
            EditText txtCount;

            MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);

                this.itemView = itemView;

                tvName = itemView.findViewById(R.id.tv_menuItemName);
                tvDesc = itemView.findViewById(R.id.tv_menuItemDesc);
                tvPrice = itemView.findViewById(R.id.tv_menuItemPrice);
                tvCount = itemView.findViewById(R.id.tv_count);
                imgLogo = itemView.findViewById(R.id.img_menuItemLogo);

                vgSelectorView = itemView.findViewById(R.id.vg_selectorView);
                fabPlus = itemView.findViewById(R.id.fab_plus);
                fabMinus = itemView.findViewById(R.id.fab_minus);
                btnDoneSelector = itemView.findViewById(R.id.btn_doneSelector);
                txtCount = itemView.findViewById(R.id.txt_count);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.item_catalogueFood:
                        selectActiveMenuItemFromPosition(getAdapterPosition());
                        break;
                    case R.id.btn_doneSelector:
                        addToCart(txtCount.getText().toString());
                        deactivateItem();
                        break;
                    case R.id.fab_plus:
                        addToCart();
                        break;
                    case R.id.fab_minus:
                        removeFromCart();
                }
            }

            void bindView(MenuItem menuItem){
                this.menuItem = menuItem;

                itemView.setOnClickListener(this);
                fabPlus.setOnClickListener(this);
                fabMinus.setOnClickListener(this);
                btnDoneSelector.setOnClickListener(this);

                tvName.setText(menuItem.getName());
                tvDesc.setText(menuItem.getDescription());
                tvPrice.setText(String.valueOf(menuItem.getPrice()));

                updateOrderCount();

                if(getAdapterPosition() == activeMenuItemPos){
                    activateItem();
                }
                else {
                    deactivateItem();
                }

                refreshThumbnail();
            }

            void bindPlaceholder(){
                itemView.setVisibility(View.INVISIBLE);
            }

            private void refreshThumbnail() {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(menuItem.getImgRef());

                    GlideApp.with(imgLogo.getContext())
                            .load(imageRef)
                            .into(imgLogo);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            private void activateItem(){
                vgSelectorView.setVisibility(View.VISIBLE);
                tvCount.setVisibility(View.INVISIBLE);
            }

            private void deactivateItem(){
                vgSelectorView.setVisibility(View.INVISIBLE);

                updateOrderCount();
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
                    String strCount = String.format(Locale.ENGLISH, "%d", mCount);
                    txtCount.setText(strCount);
                    tvCount.setText(strCount);

                    if(vgSelectorView.getVisibility() == View.INVISIBLE){
                        tvCount.setVisibility(View.VISIBLE);
                    }
                }
                else{
                    String strCount = String.format(Locale.ENGLISH, "%d", 0);

                    txtCount.setText(strCount);
                    tvCount.setText(strCount);

                    tvCount.setVisibility(View.INVISIBLE);
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
// TODO: 1/10/2020 save orderItems to orderGroups when leaving this activity and add order groups to the intent
