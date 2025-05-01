package xyz.ziadboukhalkhal.shopsmart.ui.viewmodels;


import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;
import xyz.ziadboukhalkhal.shopsmart.data.repository.ShoppingListRepository;

public class ShoppingListViewModel extends AndroidViewModel {
    private ShoppingListRepository repository;
    private LiveData<List<ShoppingListItem>> allItems;
    private LiveData<List<ShoppingListItem>> unpurchasedItems;

    private LiveData<List<String>> allCategories;
    private LiveData<List<ShoppingListItem>> itemsByCategory;

    private LiveData<List<ShoppingListItem>> searchResults;


    public ShoppingListViewModel(@NonNull Application application) {
        super(application);
        repository = new ShoppingListRepository(application);
        allItems = repository.getAllItems();
        unpurchasedItems = repository.getUnpurchasedItems();
        allCategories = repository.getAllCategories();
    }

    public void insert(ShoppingListItem item) {
        repository.insert(item);
    }

    public void update(ShoppingListItem item) {
        repository.update(item);
    }

    public void delete(ShoppingListItem item) {
        repository.delete(item);
    }

    public void deleteAllItems() {
        repository.deleteAllItems();
    }

    public LiveData<List<ShoppingListItem>> getAllItems() {
        return allItems;
    }

    public LiveData<List<ShoppingListItem>> getUnpurchasedItems() {
        return unpurchasedItems;
    }

    public LiveData<List<String>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<ShoppingListItem>> getItemsByCategory(String category) {
        itemsByCategory = repository.getItemsByCategory(category);
        return itemsByCategory;
    }

    public LiveData<List<ShoppingListItem>> searchItems(String query) {
        searchResults = repository.searchItems(query);
        return searchResults;
    }

    public void syncWithCloud(){
        repository.syncWithCloud();
    }
}