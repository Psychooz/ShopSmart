package xyz.ziadboukhalkhal.shopsmart.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        viewModel.getAllItems().observe(this, items -> adapter.submitList(items));

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && resultCode == RESULT_OK) {
            String name = data.getStringExtra(AddEditItemActivity.EXTRA_NAME);
            int quantity = data.getIntExtra(AddEditItemActivity.EXTRA_QUANTITY, 1);
            String imagePath = data.getStringExtra(AddEditItemActivity.EXTRA_IMAGE_PATH);

            if (requestCode == ADD_ITEM_REQUEST) {
                ShoppingListItem item = new ShoppingListItem(name, quantity, imagePath);
                viewModel.insert(item);
                Toast.makeText(this, "Item saved", Toast.LENGTH_SHORT).show();
            } else if (requestCode == EDIT_ITEM_REQUEST) {
                int id = data.getIntExtra(AddEditItemActivity.EXTRA_ID, -1);
                if (id == -1) {
                    Toast.makeText(this, "Item can't be updated", Toast.LENGTH_SHORT).show();
                    return;
                }

                ShoppingListItem item = new ShoppingListItem(name, quantity, imagePath);
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


    private void scheduleNotification() {
        // Créer une tâche périodique qui se répète toutes les 24 heures
        PeriodicWorkRequest reminderWorkRequest =
                new PeriodicWorkRequest.Builder(ShoppingReminderWorker.class, 1, TimeUnit.HOURS)
                        .build();

        // Planifier la tâche périodique
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SHOPPING_REMINDER_WORK",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderWorkRequest
        );

    }

}
