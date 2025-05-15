package xyz.ziadboukhalkhal.shopsmart.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import xyz.ziadboukhalkhal.shopsmart.R;
import xyz.ziadboukhalkhal.shopsmart.data.local.dao.ShoppingListDao;
import xyz.ziadboukhalkhal.shopsmart.data.local.database.ShoppingListDatabase;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;
import xyz.ziadboukhalkhal.shopsmart.notifications.ShoppingReminderWorker;
import xyz.ziadboukhalkhal.shopsmart.ui.adapters.ShoppingListAdapter;
import xyz.ziadboukhalkhal.shopsmart.ui.viewmodels.ShoppingListViewModel;

public class MainActivity extends AppCompatActivity {
    public static final int ADD_ITEM_REQUEST = 1;
    public static final int EDIT_ITEM_REQUEST = 2;

    private ShoppingListViewModel viewModel;
    private ShoppingListAdapter adapter;
    private ExecutorService executorService;


    private ChipGroup chipGroup;
    private boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.e("FirebaseInit", "Firebase initialization failed", e);
        }

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
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

        // Check authentication
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // Initial sync
            viewModel.syncWithCloud();
            // Schedule periodic sync every 5 minutes
            schedulePeriodicSync();
        }

        // Observe data changes
        viewModel.getAllItems().observe(this, items -> {
            adapter.submitList(items);
            if (shouldSync()) {
                viewModel.syncWithCloud();
            }
        });

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
            intent.putExtra(AddEditItemActivity.EXTRA_CATEGORY, item.getCategory());
            intent.putExtra(AddEditItemActivity.EXTRA_NOTES, item.getNotes());
            startActivityForResult(intent, EDIT_ITEM_REQUEST);
        });

        // Checkbox change
        adapter.setOnItemCheckedChangeListener((item, isChecked) -> {
            item.setPurchased(isChecked);
            item.setLastUpdated(System.currentTimeMillis());
            viewModel.update(item);
        });

        cleanupInvalidItems();

        // Schedule notifications
        scheduleNotification();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor when activity is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }


    private void schedulePeriodicSync() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isSyncing) {
                isSyncing = true;
                viewModel.syncWithCloud();
                // After sync completes, schedule next one
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    isSyncing = false;
                    schedulePeriodicSync();
                }, 30000); // Wait 30 seconds between syncs
            } else {
                schedulePeriodicSync();
            }
        }, 300000); // 5 minutes
    }

    private boolean shouldSync() {
        SharedPreferences prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE);
        long lastSync = prefs.getLong("last_sync", 0);
        boolean shouldSync = System.currentTimeMillis() - lastSync > TimeUnit.HOURS.toMillis(1);
        if (shouldSync) {
            prefs.edit().putLong("last_sync", System.currentTimeMillis()).apply();
        }
        return shouldSync;
    }

    private void scheduleNotification() {
        PeriodicWorkRequest reminderWorkRequest =
                new PeriodicWorkRequest.Builder(ShoppingReminderWorker.class, 1, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SHOPPING_REMINDER_WORK",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderWorkRequest
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra(AddEditItemActivity.EXTRA_NAME);
            int quantity = data.getIntExtra(AddEditItemActivity.EXTRA_QUANTITY, 1);
            String imagePath = data.getStringExtra(AddEditItemActivity.EXTRA_IMAGE_PATH);
            String category = data.getStringExtra(AddEditItemActivity.EXTRA_CATEGORY);
            String notes = data.getStringExtra(AddEditItemActivity.EXTRA_NOTES);

            ShoppingListItem item = new ShoppingListItem(name, quantity, imagePath, category, notes);

            if (requestCode == ADD_ITEM_REQUEST) {
                viewModel.insert(item);
                Toast.makeText(this, "Item saved", Toast.LENGTH_SHORT).show();
            } else if (requestCode == EDIT_ITEM_REQUEST) {
                int id = data.getIntExtra(AddEditItemActivity.EXTRA_ID, -1);
                if (id == -1) {
                    Toast.makeText(this, "Item can't be updated", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.setId(id);
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
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.action_delete_all) {
            viewModel.deleteAllItems();
            Toast.makeText(this, "All items deleted", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_sync) {
            viewModel.syncWithCloud();
            Toast.makeText(this, "Syncing with cloud...", Toast.LENGTH_SHORT).show();
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
                    viewModel.getAllItems().observe(MainActivity.this, items ->
                            adapter.submitList(items));
                } else {
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
                if (category != null && !category.isEmpty()) {
                    Chip chip = new Chip(this);
                    chip.setText(category);
                    chip.setCheckable(true);
                    chipGroup.addView(chip);
                }
            }

            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    viewModel.getAllItems().observe(MainActivity.this, items ->
                            adapter.submitList(items));
                } else {
                    Chip selectedChip = group.findViewById(checkedIds.get(0));
                    String selectedCategory = selectedChip.getId() == R.id.chip_all ? null : selectedChip.getText().toString();

                    if (selectedCategory == null) {
                        viewModel.getAllItems().observe(MainActivity.this, items ->
                                adapter.submitList(items));
                    } else {
                        viewModel.getItemsByCategory(selectedCategory).observe(MainActivity.this, items ->
                                adapter.submitList(items));
                    }
                }
            });
        });
    }

    private void cleanupInvalidItems() {
        executorService.execute(() -> {
            try {
                ShoppingListDatabase database = ShoppingListDatabase.getInstance(this);
                ShoppingListDao dao = database.shoppingListDao();
                List<ShoppingListItem> allItems = dao.getAllItemsSync();

                int deletedCount = 0;
                for (ShoppingListItem item : allItems) {
                    if (item.getName() == null || item.getName().trim().isEmpty()) {
                        dao.delete(item);
                        deletedCount++;
                    }
                }

                if (deletedCount > 0) {
                    Log.d("Cleanup", "Deleted " + deletedCount + " invalid items");
                }
            } catch (Exception e) {
                Log.e("Cleanup", "Error cleaning up items", e);
            }
        });
    }


}