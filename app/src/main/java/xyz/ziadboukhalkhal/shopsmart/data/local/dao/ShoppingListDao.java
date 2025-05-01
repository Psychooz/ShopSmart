package xyz.ziadboukhalkhal.shopsmart.data.local.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;


import java.util.List;

import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;

@Dao
public interface ShoppingListDao {
    @Insert
    void insert(ShoppingListItem item);

    @Update
    void update(ShoppingListItem item);

    @Delete
    void delete(ShoppingListItem item);

    @Query("DELETE FROM shopping_items")
    void deleteAllItems();

    @Query("SELECT * FROM shopping_items ORDER BY timestamp DESC")
    LiveData<List<ShoppingListItem>> getAllItems();

    @Query("SELECT * FROM shopping_items WHERE purchased = 0 ORDER BY timestamp DESC")
    LiveData<List<ShoppingListItem>> getUnpurchasedItems();

    @Query("SELECT DISTINCT category FROM shopping_items WHERE category IS NOT NULL AND category != ''")
    LiveData<List<String>> getAllCategories();

    @Query("SELECT * FROM shopping_items WHERE category = :category ORDER BY timestamp DESC")
    LiveData<List<ShoppingListItem>> getItemsByCategory(String category);

    @Query("SELECT * FROM shopping_items WHERE name LIKE :query OR notes LIKE :query ORDER BY timestamp DESC")
    LiveData<List<ShoppingListItem>> searchItems(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ShoppingListItem> items); //for cloud sync


    @Query("SELECT * FROM shopping_items")
    List<ShoppingListItem> getAllItemsSync();

    @Update
    void updateAll(List<ShoppingListItem> items);

    @Query("SELECT * FROM shopping_items WHERE lastUpdated > lastSynced OR lastSynced = 0")
    List<ShoppingListItem> getItemsToSync(long minLastUpdated);

}