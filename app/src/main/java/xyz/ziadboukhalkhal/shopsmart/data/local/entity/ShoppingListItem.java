package xyz.ziadboukhalkhal.shopsmart.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "shopping_items")
public class ShoppingListItem {
    @PrimaryKey
    @NonNull
    private String id; // Firestore document ID
    private String name;
    private int quantity;
    private String category;
    private String notes;
    private boolean purchased;

    @ServerTimestamp
    private Date serverTimestamp;
    private Date localTimestamp;
    private boolean isSynced;
    private String userId;

    // Room constructor
    public ShoppingListItem(@NonNull String id, String name, int quantity,
                            String category, String notes, boolean purchased,
                            Date serverTimestamp, Date localTimestamp,
                            boolean isSynced, String userId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.category = category;
        this.notes = notes;
        this.purchased = purchased;
        this.serverTimestamp = serverTimestamp;
        this.localTimestamp = localTimestamp;
        this.isSynced = isSynced;
        this.userId = userId;
    }

    // Firestore constructor
    public ShoppingListItem() {}

    // Getters and setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }

    public Date getServerTimestamp() { return serverTimestamp; }
    public void setServerTimestamp(Date serverTimestamp) { this.serverTimestamp = serverTimestamp; }

    public Date getLocalTimestamp() { return localTimestamp; }
    public void setLocalTimestamp(Date localTimestamp) { this.localTimestamp = localTimestamp; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("category", category);
        map.put("notes", notes);
        map.put("purchased", purchased);
        map.put("serverTimestamp", serverTimestamp);
        map.put("userId", userId);
        return map;
    }

    // Conflict resolution - server wins
    public void mergeWithCloudVersion(ShoppingListItem cloudItem) {
        if (cloudItem.getServerTimestamp() != null &&
                (this.getServerTimestamp() == null ||
                        cloudItem.getServerTimestamp().after(this.getServerTimestamp()))) {
            this.name = cloudItem.getName();
            this.quantity = cloudItem.getQuantity();
            this.category = cloudItem.getCategory();
            this.notes = cloudItem.getNotes();
            this.purchased = cloudItem.isPurchased();
            this.serverTimestamp = cloudItem.getServerTimestamp();
        }
    }
}