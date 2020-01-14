package com.inc.tracks.yummobile;

import android.content.Context;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class HomeFragment extends Fragment implements View.OnClickListener{

  private OnFragmentInteractionListener mListener;

  private FirebaseFirestore fireDB;

  private RestaurantsRVAdapter restaurantsAdapter;

  private TextView txtUserGreeting;

  public HomeFragment() {
    // Required empty public constructor
  }

  public static HomeFragment newInstance() {
    HomeFragment fragment = new HomeFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View fragView = inflater.inflate(R.layout.fragment_home,
            container, false);

    txtUserGreeting = fragView.findViewById(R.id.txt_userGreeting);

    RecyclerView rvRestaurantList = fragView.findViewById(R.id.rv_nearRestaurants);
    RecyclerView rvRecentOrders = fragView.findViewById(R.id.rv_recentOrders);

    fragView.findViewById(R.id.tv_viewMore).setOnClickListener(this);

    rvRestaurantList.setLayoutManager(new LinearLayoutManager(fragView.getContext()));
    if(restaurantsAdapter == null){
      restaurantsAdapter = new RestaurantsRVAdapter();
    }
    if(rvRestaurantList.getAdapter() == null){
      rvRestaurantList.setAdapter(restaurantsAdapter);
    }

    if(fragView.getId() == R.id.layout_landHome){
      rvRecentOrders.setLayoutManager(new LinearLayoutManager(fragView.getContext(),
              LinearLayoutManager.VERTICAL, false));
      rvRecentOrders.setAdapter(new RecentOrdersRVAdapter());
    }
    else{
      rvRecentOrders.setLayoutManager(new LinearLayoutManager(fragView.getContext(),
              LinearLayoutManager.HORIZONTAL, false));
      rvRecentOrders.setAdapter(new RecentOrdersRVAdapter());
    }

    greetUser();

    return fragView;
  }

  private void onButtonPressed(int buttonId, RestaurantItem restaurantItem) {
    if (mListener != null) {
      mListener.onFragmentInteraction(buttonId, restaurantItem);
    }
  }

  private void onButtonPressed(int buttonId, HashMap orderSummary) {
    if (mListener != null) {
      mListener.onFragmentInteraction(buttonId, orderSummary);
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
    if (v.getId() == R.id.tv_viewMore) {
      onButtonPressed(v.getId(), (RestaurantItem) null);
    }
  }

  public interface OnFragmentInteractionListener {
    void onFragmentInteraction(int interactionId, RestaurantItem restaurantItem);
    void onFragmentInteraction(int interactionId, HashMap orderSummary);
  }

  private void greetUser(){
    String userGreeting = "Hello, " + UserAuth.userPrefs.getUserName();
    if (txtUserGreeting != null) {
      txtUserGreeting.setText(userGreeting);
    }
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
              .inflate(R.layout.item_near_restaurant, viewGroup, false);
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
      TextView tvDesc;
      TextView tvAddress;
      ImageView imgLogo;

      RstViewHolder(@NonNull View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tv_restaurantName);
        tvDesc = itemView.findViewById(R.id.tv_restaurantDesc);
        tvAddress = itemView.findViewById(R.id.tv_restaurantAddress);
        imgLogo = itemView.findViewById(R.id.img_restaurantLogo);

        itemView.setOnClickListener(this);
      }

      void bindView(RestaurantItem restaurantItem){
        tvName.setText(restaurantItem.getName());
        tvDesc.setText(restaurantItem.getDescription());
        tvAddress.setText(restaurantItem.getAddress());

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

      @Override
      public void onClick(View v) {
        if (v.getId() == R.id.item_nearRestaurant) {
          int position = getAdapterPosition();
          RestaurantItem restaurantItem = restaurantItems.get(position);

          onButtonPressed(v.getId(), restaurantItem);
        }
      }
    }
  }

  public class RecentOrdersRVAdapter extends RecyclerView.Adapter<RecentOrdersRVAdapter.RecentOrderViewHolder>{

    private final String TAG = "FireStore";
    private ArrayList<RecentOrder> recentOrders = new ArrayList<>();

    RecentOrdersRVAdapter() {
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
                    RecentOrder recentOrder;
                    int position = -1;
                    switch (dc.getType()) {
                      case ADDED:
                        Log.d(TAG, "New RecentOrder: " + dc.getDocument().getData());

                        recentOrder = dc.getDocument().toObject(RecentOrder.class);

                        recentOrder.setId(dc.getDocument().getId());

                        recentOrders.add(recentOrder);

                        notifyItemInserted(recentOrders.size() - 1);
                        break;
                      case MODIFIED:
                        Log.d(TAG, "Modified RecentOrder: " + dc.getDocument().getData());

                        recentOrder = dc.getDocument().toObject(RecentOrder.class);

                        for(RecentOrder order : recentOrders){
                          if(order.getId().equals(recentOrder.getId())){
                            position = recentOrders.indexOf(order);
                          }
                        }
                        if(position >= 0){
                          notifyItemChanged(position);
                        }
                        break;
                      case REMOVED:
                        Log.d(TAG, "Removed RecentOrder: " + dc.getDocument().getData());

                        recentOrder = dc.getDocument().toObject(RecentOrder.class);

                        for(RecentOrder order : recentOrders){
                          if(order.getId().equals(recentOrder.getId())){
                            position = recentOrders.indexOf(order);
                            Log.d(TAG, "Removed RecentOrder Notified!: " + order.getId()
                                    + " => " + recentOrder.getId() + " => " + recentOrders.indexOf(order));
                          }
                        }

                        if(position >= 0){
                          recentOrders.remove(position);
                          notifyItemRemoved(position);
                        }
                        break;
                    }
                  }

                }
              };

      fireDB.collection("users")
              .document(UserAuth.currentUser.getUid())
              .collection("recentOrders")
              .addSnapshotListener(dataChangedListener);
    }

    @NonNull
    @Override
    public RecentOrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

      View restaurantView;
      restaurantView = LayoutInflater.from(viewGroup.getContext())
              .inflate(R.layout.item_food_recent, viewGroup, false);
      return new RecentOrderViewHolder(restaurantView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentOrderViewHolder viewHolder, int i) {
      viewHolder.bindView(recentOrders.get(i));
    }

    @Override
    public int getItemCount() {
      return recentOrders.size();
    }

    class RecentOrderViewHolder extends RecyclerView.ViewHolder{

      View recentOrderItem;
      TextView tvOrderName;
      ImageView imgOrderImage;

      RecentOrderViewHolder(@NonNull View itemView) {
        super(itemView);
        recentOrderItem = itemView;
        tvOrderName = itemView.findViewById(R.id.tv_orderName);
        imgOrderImage = itemView.findViewById(R.id.img_orderLogo);
      }

      void bindView(final RecentOrder recentOrder){
        tvOrderName.setText(recentOrder.getOrderName());

        recentOrderItem.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onButtonPressed(v.getId(), recentOrder.getOrderSummary());
          }
        });

        refreshThumbnail(recentOrder);
      }

      private void refreshThumbnail(RecentOrder recentOrder) {
        if(recentOrder.getImgRef() != null){
          try {
            StorageReference imageRef = UserAuth.firebaseStorage
                    .getReferenceFromUrl(recentOrder.getImgRef());

            GlideApp.with(imgOrderImage.getContext())
                    .load(imageRef)
                    .into(imgOrderImage);
          }
          catch(Exception e){
            e.printStackTrace();
          }
        }
        else{
          selectOrderImage(recentOrder);
        }
      }

      private void selectOrderImage(final RecentOrder recentOrder){
        String restId = recentOrder.getOrderSummary().keySet().toArray(new String[1])[0];
        String menuItemId = Objects.requireNonNull(recentOrder
                .getOrderSummary().get(restId)).keySet().toArray(new String[1])[0];

        final DocumentReference orderRef = fireDB.collection("users")
                .document(UserAuth.currentUser.getUid())
                .collection("recentOrders")
                .document(recentOrder.getId());

        fireDB.collection("restaurants")
                .document(restId)
                .collection("menuItems")
                .document(menuItemId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                  @Override
                  public void onSuccess(DocumentSnapshot documentSnapshot) {
                    MenuItem menuItem = documentSnapshot.toObject(MenuItem.class);
                    assert menuItem != null;
                    recentOrder.setImgRef(menuItem.getImgRef());
                    orderRef.set(recentOrder)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                              @Override
                              public void onSuccess(Void aVoid) {
                                notifyItemChanged(getAdapterPosition());
                              }
                            });
                  }
                });
      }
    }
  }
}