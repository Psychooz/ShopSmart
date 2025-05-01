package xyz.ziadboukhalkhal.shopsmart.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

import xyz.ziadboukhalkhal.shopsmart.R;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;
import xyz.ziadboukhalkhal.shopsmart.notifications.ShoppingReminderWorker;
import xyz.ziadboukhalkhal.shopsmart.ui.adapters.ShoppingListAdapter;
import xyz.ziadboukhalkhal.shopsmart.ui.viewmodels.ShoppingListViewModel;

public class MainActivity extends AppCompatActivity {
    public static final int ADD_ITEM_REQUEST = 1;
    public static final int EDIT_ITEM_REQUEST = 2;

    private ShoppingListViewModel viewModel;
    private ShoppingListAdapter adapter;
    private ChipGroup chipGroup; //categorie filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.e("FirebaseInit", "Firebase initialization failed", e);
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditItemActivity.class);
            startActivityForResult(intent, ADD_ITEM_REQUEST);
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new ShoppingListAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ShoppingListViewModel.class);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            viewModel.syncWithCloud();
        }
        viewModel.getAllItems().observe(this, items -> adapter.submitList(items));

        chipGroup = findViewById(R.id.chipGroup);
        setupCategoryFilter();

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final ShoppingListItem deletedItem = adapter.getItemAt(viewHolder.getAdapterPosition());
                viewModel.delete(deletedItem);

                Snackbar.make(findViewById(android.R.id.content), "Item deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> viewModel.insert(deletedItem))
                        .show();
            }
        }).attachToRecyclerView(recyclerView);

        // Click on item to edit
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(MainActivity.this, AddEditItemActivity.class);
            intent.putExtra(AddEditItemActivity.EXTRA_ID, item.getId());
            intent.putExtra(AddEditItemActivity.EXTRA_NAME, item.getName());
            intent.putExtra(AddEditItemActivity.EXTRA_QUANTITY, item.getQuantity());
            intent.putExtra(AddEditItemActivity.EXTRA_IMAGE_PATH, item.getImagePath());
            startActivityForResult(intent, EDIT_ITEM_REQUEST);
        });

        // Checkbox change
        adapter.setOnItemCheckedChangeListener((item, isChecked) -> {
            item.setPurchased(isChecked);
            viewModel.update(item);
        });
        scheduleNotification();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (shouldSync()) {
            viewModel.syncWithCloud();
        }
    }

    private boolean shouldSync() {
        SharedPreferences prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE);
        long lastSync = prefs.getLong("last_sync", 0);
        return System.currentTimeMillis() - lastSync > TimeUnit.HOURS.toMillis(1);
    }
    private void scheduleNotification() {
        PeriodicWorkRequest reminderWorkRequest =
                new PeriodicWorkRequest.Builder(ShoppingReminderWorker.class, 10, TimeUnit.SECONDS)
                        .build();

        // Planifier la tâche périodique
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SHOPPING_REMINDER_WORK",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderWorkRequest
        );

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && resultCode == RESULT_OK) {
            String name = data.getStringExtra(AddEditItemActivity.EXTRA_NAME);
            int quantity = data.getIntExtra(AddEditItemActivity.EXTRA_QUANTITY, 1);
            String imagePath = data.getStringExtra(AddEditItemActivity.EXTRA_IMAGE_PATH);

            String category = data.getStringExtra(AddEditItemActivity.EXTRA_CATEGORY);
            String notes = data.getStringExtra(AddEditItemActivity.EXTRA_NOTES);


            if (requestCode == ADD_ITEM_REQUEST) {
                ShoppingListItem item = new ShoppingListItem(name, quantity, imagePath, category, notes);
                viewModel.insert(item);
                Toast.makeText(this, "Item saved", Toast.LENGTH_SHORT).show();
            } else if (requestCode == EDIT_ITEM_REQUEST) {
                int id = data.getIntExtra(AddEditItemActivity.EXTRA_ID, -1);
                if (id == -1) {
                    Toast.makeText(this, "Item can't be updated", Toast.LENGTH_SHORT).show();
                    return;
                }

                ShoppingListItem item = new ShoppingListItem(name, quantity, imagePath, category, notes);
                item.setId(id);
                item.setPurchased(false);
                viewModel.update(item);
                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Item not saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        setupSearchView(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            viewModel.deleteAllItems();
            Toast.makeText(this, "All items deleted", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setupSearchView(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search items or notes...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    // Show all items when search is empty
                    viewModel.getAllItems().observe(MainActivity.this, items ->
                            adapter.submitList(items));
                } else {
                    // Perform search
                    viewModel.searchItems(newText).observe(MainActivity.this, items ->
                            adapter.submitList(items));
                }
                return true;
            }
        });
    }

    private void setupCategoryFilter() {
        viewModel.getAllCategories().observe(this, categories -> {
            chipGroup.removeViews(1, chipGroup.getChildCount() - 1); // Keep "All" chip

            // Add a chip for each category
            for (String category : categories) {
                Chip chip = new Chip(this);
                chip.setText(category);
                chip.setCheckable(true);
                chipGroup.addView(chip);
            }

            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    // Show all items if nothing is selected
                    viewModel.getAllItems().observe(this, items ->
                            adapter.submitList(items));
                } else {
                    Chip selectedChip = group.findViewById(checkedIds.get(0));
                    String selectedCategory = selectedChip.getId() == R.id.chip_all ? null : selectedChip.getText().toString();

                    if (selectedCategory == null) {
                        viewModel.getAllItems().observe(this, items ->
                                adapter.submitList(items));
                    } else {
                        viewModel.getItemsByCategory(selectedCategory).observe(this, items ->
                                adapter.submitList(items));
                    }
                }
            });
        });
    }



}
