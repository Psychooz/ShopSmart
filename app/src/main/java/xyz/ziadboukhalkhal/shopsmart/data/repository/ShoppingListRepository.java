package xyz.ziadboukhalkhal.shopsmart.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.ziadboukhalkhal.shopsmart.data.local.dao.ShoppingListDao;
import xyz.ziadboukhalkhal.shopsmart.data.local.database.ShoppingListDatabase;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;

public class ShoppingListRepository {
    private ShoppingListDao shoppingListDao;
    private LiveData<List<ShoppingListItem>> allItems;
    private LiveData<List<ShoppingListItem>> unpurchasedItems;
    private LiveData<List<String>> allCategories;
    private final ExecutorService executorService;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final String userId;
    private boolean isSyncing = false;

    public ShoppingListRepository(Application application) {
        ShoppingListDatabase database = ShoppingListDatabase.getInstance(application);
        shoppingListDao = database.shoppingListDao();
        allItems = shoppingListDao.getAllItems();
        unpurchasedItems = shoppingListDao.getUnpurchasedItems();
        allCategories = shoppingListDao.getAllCategories();
        executorService = Executors.newSingleThreadExecutor();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    // Basic CRUD operations
    public void insert(ShoppingListItem item) {
        if (!item.isValid()) {
            return;
        }
        item.setLastUpdated(System.currentTimeMillis());
        executorService.execute(() -> shoppingListDao.insert(item));
    }

    public void update(ShoppingListItem item) {
        item.setLastUpdated(System.currentTimeMillis());
        executorService.execute(() -> shoppingListDao.update(item));
    }

    public void delete(ShoppingListItem item) {
        executorService.execute(() -> shoppingListDao.delete(item));
    }

    public void deleteAllItems() {
        executorService.execute(shoppingListDao::deleteAllItems);
    }

    // LiveData getters
    public LiveData<List<ShoppingListItem>> getAllItems() {
        return allItems;
    }

    public LiveData<List<ShoppingListItem>> getUnpurchasedItems() {
        return unpurchasedItems;
    }

    public LiveData<List<String>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<ShoppingListItem>> getItemsByCategory(String category) {
        return shoppingListDao.getItemsByCategory(category);
    }

    public LiveData<List<ShoppingListItem>> searchItems(String query) {
        String searchQuery = "%" + query + "%";
        return shoppingListDao.searchItems(searchQuery);
    }

    // Sync methods
    public void syncWithCloud() {
        if (isSyncing) return;
        isSyncing = true;

        executorService.execute(() -> {
            try {
                // 1. Push local changes to Firestore
                List<ShoppingListItem> unsyncedItems = shoppingListDao.getUnsyncedItems();
                if (!unsyncedItems.isEmpty()) {
                    pushToCloud(unsyncedItems);
                }

                // 2. Pull from Firestore
                pullFromCloud();
            } catch (Exception e) {
                Log.e("Sync", "Error during sync", e);
            } finally {
                isSyncing = false;
            }
        });
    }

    private void pushToCloud(List<ShoppingListItem> items) {
        long syncTime = System.currentTimeMillis();

        for (ShoppingListItem item : items) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id", item.getId());
            itemData.put("name", item.getName());
            itemData.put("quantity", item.getQuantity());
            itemData.put("category", item.getCategory());
            itemData.put("notes", item.getNotes());
            itemData.put("imagePath", item.getImagePath());
            itemData.put("purchased", item.isPurchased());
            itemData.put("timestamp", item.getTimestamp());
            itemData.put("lastUpdated", item.getLastUpdated());

            firestore.collection("users").document(userId)
                    .collection("items").document(String.valueOf(item.getId()))
                    .set(itemData)
                    .addOnSuccessListener(aVoid -> {
                        executorService.execute(() -> {
                            shoppingListDao.markAsSynced(item.getId(), syncTime);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Sync", "Error syncing item " + item.getId(), e);
                    });
        }
    }

    private void pullFromCloud() {
        firestore.collection("users").document(userId)
                .collection("items").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShoppingListItem> validItems = new ArrayList<>();
                    long syncTime = System.currentTimeMillis();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ShoppingListItem item = doc.toObject(ShoppingListItem.class);
                        if (item != null && item.isValid()) {  // Check validity
                            item.setLastSynced(syncTime);
                            validItems.add(item);
                        }
                    }

                    executorService.execute(() -> {
                        for (ShoppingListItem cloudItem : validItems) {
                            ShoppingListItem localItem = shoppingListDao.getItemById(cloudItem.getId());

                            if (localItem == null) {
                                shoppingListDao.insert(cloudItem);
                            } else if (cloudItem.getLastUpdated() > localItem.getLastUpdated()) {
                                shoppingListDao.update(cloudItem);
                            }
                        }
                    });
                }).addOnFailureListener(e -> {
                    Log.e("Sync", "Error fetching cloud data", e);
                });

    }
}
