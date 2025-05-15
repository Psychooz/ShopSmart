package xyz.ziadboukhalkhal.shopsmart.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;
import xyz.ziadboukhalkhal.shopsmart.data.repository.ShoppingListRepository;

public class ShoppingListViewModel extends ViewModel {
    private final ShoppingListRepository repository;

    public ShoppingListViewModel(ShoppingListRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<ShoppingListItem>> getAllItems() {
        return repository.getAllItems();
    }

    public void addItem(ShoppingListItem item) {
        repository.addItem(item);
    }

    public void updateItem(ShoppingListItem item) {
        repository.updateItem(item);
    }

    public void deleteItem(ShoppingListItem item) {
        repository.deleteItem(item);
    }
}