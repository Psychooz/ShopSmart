package xyz.ziadboukhalkhal.shopsmart.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import xyz.ziadboukhalkhal.shopsmart.data.local.dao.ShoppingListDao;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;

@Database(entities = {ShoppingListItem.class}, version = 6, exportSchema = false)
public abstract class ShoppingListDatabase extends RoomDatabase {
    private static volatile ShoppingListDatabase instance;

    public abstract ShoppingListDao shoppingListDao();


    public static ShoppingListDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ShoppingListDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    ShoppingListDatabase.class,
                                    "shopping_list_database")
                            .fallbackToDestructiveMigration() // Temporary for development
                            .build();
                }
            }
        }
        return instance;
    }
}