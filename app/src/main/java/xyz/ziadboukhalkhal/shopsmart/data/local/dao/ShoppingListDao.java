package xyz.ziadboukhalkhal.shopsmart.data.local.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
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
}