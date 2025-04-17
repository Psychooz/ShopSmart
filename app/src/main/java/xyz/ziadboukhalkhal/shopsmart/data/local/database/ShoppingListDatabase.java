package xyz.ziadboukhalkhal.shopsmart.data.local.database;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import xyz.ziadboukhalkhal.shopsmart.data.local.dao.ShoppingListDao;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;


@Database(entities = {ShoppingListItem.class}, version = 1, exportSchema = false)
public abstract class ShoppingListDatabase extends RoomDatabase {

    private static ShoppingListDatabase instance;

    public abstract ShoppingListDao shoppingListDao();

    public static synchronized ShoppingListDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ShoppingListDatabase.class,
                            "shopping_list_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}