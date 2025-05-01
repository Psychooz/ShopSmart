package xyz.ziadboukhalkhal.shopsmart.data.local.database;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import xyz.ziadboukhalkhal.shopsmart.data.local.dao.ShoppingListDao;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;


@Database(entities = {ShoppingListItem.class}, version = 4, exportSchema = false)
public abstract class ShoppingListDatabase extends RoomDatabase {

    private static ShoppingListDatabase instance;

    public abstract ShoppingListDao shoppingListDao();

    // Migration from version 3 to 4 (adding lastUpdated/lastSynced)
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE shopping_items ADD COLUMN lastUpdated INTEGER DEFAULT 0");
            database.execSQL("ALTER TABLE shopping_items ADD COLUMN lastSynced INTEGER DEFAULT 0");
        }
    };

    public static synchronized ShoppingListDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ShoppingListDatabase.class,
                            "shopping_list_database")
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build();
        }
        return instance;
    }
}