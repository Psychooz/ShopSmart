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
    private final ExecutorService executorService;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final String userId;


    private boolean isSyncing = false;
    private long lastSyncTime = 0;
    private static final long SYNC_COOLDOWN_MS = 30000; // 30 seconds between syncs

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

    public void insert(ShoppingListItem item) {
        executorService.execute(() -> shoppingListDao.insert(item));
    }

    public void update(ShoppingListItem item) {
        executorService.execute(() -> shoppingListDao.update(item));
    }

    public void delete(ShoppingListItem item) {
        executorService.execute(() -> shoppingListDao.delete(item));
    }

    public void deleteAllItems() {
        executorService.execute(shoppingListDao::deleteAllItems);
    }

    public LiveData<List<ShoppingListItem>> getAllItems() {
        return allItems;
    }

    public LiveData<List<ShoppingListItem>> getUnpurchasedItems() {
        return unpurchasedItems;
    }


    private LiveData<List<String>> allCategories;

    // Add new method
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


    public void syncWithCloud() {
        if (isSyncing || System.currentTimeMillis() - lastSyncTime < SYNC_COOLDOWN_MS) {
            return;
        }

        isSyncing = true;
        lastSyncTime = System.currentTimeMillis();

        // 1. First pull from cloud
        firestore.collection("users/"+userId+"/items").get()
                .addOnSuccessListener(cloudSnapshot -> {
                    List<ShoppingListItem> cloudItems = new ArrayList<>();

                    for (DocumentSnapshot doc : cloudSnapshot.getDocuments()) {
                        ShoppingListItem item = doc.toObject(ShoppingListItem.class);
                        if (item != null) {
                            item.setLastSynced(System.currentTimeMillis());
                            cloudItems.add(item);
                        }
                    }
                    final long currentTime = System.currentTimeMillis() ;
                    // 2. Merge with local data
                    executorService.execute(() -> {
                        List<ShoppingListItem> localItems = shoppingListDao.getItemsToSync(currentTime - SYNC_COOLDOWN_MS);
                        Map<String, ShoppingListItem> mergedItems = new HashMap<>();

                        // Add all cloud items
                        for (ShoppingListItem cloudItem : cloudItems) {
                            mergedItems.put(cloudItem.getId()+"", cloudItem);
                        }

                        // Add local items if newer or not in cloud
                        for (ShoppingListItem localItem : localItems) {
                            ShoppingListItem cloudVersion = mergedItems.get(localItem.getId()+"");
                            if (cloudVersion == null || localItem.getLastUpdated() > cloudVersion.getLastUpdated()) {
                                mergedItems.put(localItem.getId()+"", localItem);
                            }
                        }

                        // 3. Save merged data
                        shoppingListDao.deleteAllItems();
                        shoppingListDao.insertAll(new ArrayList<>(mergedItems.values()));

                        // 4. Push final merged data to cloud
                        pushToCloud(new ArrayList<>(mergedItems.values()));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Sync", "Pull failed", e);
                    isSyncing = false;
                });
    }

    private void pushToCloud(List<ShoppingListItem> items) {
        Map<String, Object> updates = new HashMap<>();
        List<Map<String, Object>> itemMaps = new ArrayList<>();

        for (ShoppingListItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("name", item.getName());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("imagePath", item.getImagePath());
            itemMap.put("purchased", item.isPurchased());
            itemMap.put("timestamp", item.getTimestamp());
            itemMap.put("category", item.getCategory());
            itemMap.put("notes", item.getNotes());
            itemMap.put("lastUpdated", item.getLastUpdated());
            itemMap.put("lastSynced", System.currentTimeMillis());
            itemMaps.add(itemMap);
        }

        updates.put("items", itemMaps);

        firestore.collection("users/"+userId+"/items")
                .document("bulk")
                .set(updates)
                .addOnSuccessListener(aVoid -> isSyncing = false)
                .addOnFailureListener(e -> {
                    Log.e("Sync", "Push failed", e);
                    isSyncing = false;
                });
    }
}