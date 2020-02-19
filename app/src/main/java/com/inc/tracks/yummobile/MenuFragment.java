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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
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
import java.util.List;
import java.util.Locale;


public class MenuFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_REST_ITEM = "arg_rest_path";
    private static final String ARG_ORDER_GROUPS = "arg_order_groups";

    private ConstraintLayout myLayout;

    private RestaurantItem activeRestItem;

    private RecyclerView rvFoodMenu;

    private TextView tvRestaurantName;

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

        rvFoodMenu.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
        if(menuItemsAdapter == null){
            menuItemsAdapter = new FoodMenuRVAdapter();
        }
        if(rvFoodMenu.getAdapter() == null){
            rvFoodMenu.setAdapter(menuItemsAdapter);
        }
        menuItemsAdapter.getFilter().filter("");

        tvRestaurantName.setText(activeRestItem.getName());

        fragView.findViewById(R.id.fab_cart).setOnClickListener(this);

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

    void onSearchQuery(String newText){
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

    void updateActiveRestaurantItem(RestaurantItem restaurantItem){
        tvRestaurantName.setText(activeRestItem.getName());

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
