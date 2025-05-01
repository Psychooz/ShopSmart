package xyz.ziadboukhalkhal.shopsmart.data.repository;

import androidx.lifecycle.LiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import xyz.ziadboukhalkhal.shopsmart.data.local.dao.ShoppingListDao;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;

public class ShoppingListRepository {
    private final ShoppingListDao dao;
    private final FirebaseFirestore firestore;
    private final Executor executor;
    private final String userId;

    public ShoppingListRepository(ShoppingListDao dao) {
        this.dao = dao;
        this.firestore = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Enable Firestore offline persistence
        firestore.enableNetwork().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                syncWithFirestore();
            }
        });
    }

    public LiveData<List<ShoppingListItem>> getAllItems() {
        return dao.getAllItems(userId);
    }

    public void addItem(ShoppingListItem item) {
        executor.execute(() -> {
            item.setUserId(userId);
            item.setLocalTimestamp(new Date());
            item.setSynced(false);
            dao.insert(item);
            syncItemWithFirestore(item);
        });
    }

    public void updateItem(ShoppingListItem item) {
        executor.execute(() -> {
            item.setLocalTimestamp(new Date());
            item.setSynced(false);
            dao.update(item);
            syncItemWithFirestore(item);
        });
    }

    public void deleteItem(ShoppingListItem item) {
        executor.execute(() -> {
            dao.delete(item.getId());
            deleteFromFirestore(item.getId());
        });
    }

    private void syncWithFirestore() {
        executor.execute(() -> {
            // Push local changes to Firestore
            List<ShoppingListItem> unsyncedItems = dao.getUnsyncedItems(userId);
            for (ShoppingListItem item : unsyncedItems) {
                syncItemWithFirestore(item);
            }

            // Pull changes from Firestore
            firestore.collection("shoppingItems")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null || snapshots == null) return;

                        executor.execute(() -> {
                            List<ShoppingListItem> localItems = dao.getAllItemsSync(userId);
                            for (QueryDocumentSnapshot doc : snapshots) {
                                ShoppingListItem cloudItem = doc.toObject(ShoppingListItem.class);
                                cloudItem.setId(doc.getId());
                                cloudItem.setSynced(true);

                                // Merge with local version if exists
                                localItems.stream()
                                        .filter(local -> local.getId().equals(cloudItem.getId()))
                                        .findFirst()
                                        .ifPresentOrElse(
                                                local -> {
                                                    local.mergeWithCloudVersion(cloudItem);
                                                    dao.update(local);
                                                },
                                                () -> dao.insert(cloudItem)
                                        );
                            }
                        });
                    });
        });
    }

    private void syncItemWithFirestore(ShoppingListItem item) {
        if (item.getId() == null) {
            // New item
            firestore.collection("shoppingItems")
                    .add(item.toMap())
                    .addOnSuccessListener(ref -> {
                        item.setId(ref.getId());
                        item.setSynced(true);
                        dao.update(item);
                    });
        } else {
            // Existing item
            firestore.collection("shoppingItems")
                    .document(item.getId())
                    .set(item.toMap())
                    .addOnSuccessListener(aVoid -> {
                        item.setSynced(true);
                        dao.update(item);
                    });
        }
    }

    private void deleteFromFirestore(String id) {
        firestore.collection("shoppingItems")
                .document(id)
                .delete();
    }
}