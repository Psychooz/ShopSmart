package xyz.ziadboukhalkhal.shopsmart.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "shopping_items",
        indices = {
                @Index(value = {"name"}),
                @Index(value = {"notes"})
        })
public class ShoppingListItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int quantity;
    private String category;
    private String notes;
    private String imagePath;
    private boolean purchased;
    private long timestamp;

    private long lastUpdated = System.currentTimeMillis();
    private long lastSynced = 0;

    public ShoppingListItem(String name, int quantity, String imagePath,String category, String notes) {
        this.name = name;
        this.quantity = quantity;
        this.imagePath = imagePath;
        this.purchased = false;
        this.timestamp = System.currentTimeMillis();
        this.category = category;
        this.notes = notes;
    }
    @Ignore
    public ShoppingListItem() {
        this.name = "";
        this.quantity = 1;
        this.purchased = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public long getLastSynced() { return lastSynced; }
    public void setLastSynced(long lastSynced) { this.lastSynced = lastSynced; }
}