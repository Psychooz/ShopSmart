package xyz.ziadboukhalkhal.shopsmart.data.repository;


import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.ziadboukhalkhal.shopsmart.data.local.dao.ShoppingListDao;
import xyz.ziadboukhalkhal.shopsmart.data.local.database.ShoppingListDatabase;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;

public class ShoppingListRepository {
    private ShoppingListDao shoppingListDao;
    private LiveData<List<ShoppingListItem>> allItems;
    private LiveData<List<ShoppingListItem>> unpurchasedItems;
    private ExecutorService executorService;

    public ShoppingListRepository(Application application) {
        ShoppingListDatabase database = ShoppingListDatabase.getInstance(application);
        shoppingListDao = database.shoppingListDao();
        allItems = shoppingListDao.getAllItems();
        unpurchasedItems = shoppingListDao.getUnpurchasedItems();
        executorService = Executors.newSingleThreadExecutor();
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
}