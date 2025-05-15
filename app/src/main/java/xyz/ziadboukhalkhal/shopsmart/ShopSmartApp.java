package xyz.ziadboukhalkhal.shopsmart;

import android.app.Application;

import xyz.ziadboukhalkhal.shopsmart.data.local.database.ShoppingListDatabase;
import xyz.ziadboukhalkhal.shopsmart.data.repository.ShoppingListRepository;

public class ShopSmartApp extends Application {
    private ShoppingListRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();

        ShoppingListDatabase database = ShoppingListDatabase.getInstance(this);
        repository = new ShoppingListRepository(database.shoppingListDao());
    }

    public ShoppingListRepository getRepository() {
        return repository;
    }
}