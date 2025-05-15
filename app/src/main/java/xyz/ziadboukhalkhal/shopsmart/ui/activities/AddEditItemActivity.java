package xyz.ziadboukhalkhal.shopsmart.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import xyz.ziadboukhalkhal.shopsmart.R;

public class AddEditItemActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "xyz.ziadboukhalkhal.shopsmart.EXTRA_ID";
    public static final String EXTRA_NAME = "xyz.ziadboukhalkhal.shopsmart.EXTRA_NAME";
    public static final String EXTRA_QUANTITY = "xyz.ziadboukhalkhal.shopsmart.EXTRA_QUANTITY";
    public static final String EXTRA_IMAGE_PATH = "xyz.ziadboukhalkhal.shopsmart.EXTRA_IMAGE_PATH";

    public static final String EXTRA_CATEGORY = "xyz.ziadboukhalkhal.shopsmart.EXTRA_CATEGORY";
    public static final String EXTRA_NOTES = "xyz.ziadboukhalkhal.shopsmart.EXTRA_NOTES";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 102;

    private TextInputEditText editTextName;
    private TextInputEditText editTextQuantity;
    private TextInputEditText editTextCategory;
    private TextInputEditText editTextNotes;
    private ImageView imagePreview;
    private Button buttonTakePhoto;
    private Button buttonSelectImage;
    private Button buttonSave;

    private String currentPhotoPath;
    private String selectedImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_edit_item_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find views
        editTextName = findViewById(R.id.edit_name);
        editTextQuantity = findViewById(R.id.edit_quantity);
        imagePreview = findViewById(R.id.image_preview);
        buttonTakePhoto = findViewById(R.id.button_take_photo);
        buttonSelectImage = findViewById(R.id.button_select_image);
        buttonSave = findViewById(R.id.button_save);
        editTextCategory = findViewById(R.id.edit_category);
        editTextNotes = findViewById(R.id.edit_notes);

        Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(R.drawable.ic_close);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_ID)) {
            setTitle("Edit Item");
            editTextName.setText(intent.getStringExtra(EXTRA_NAME));
            editTextQuantity.setText(String.valueOf(intent.getIntExtra(EXTRA_QUANTITY, 1)));
            editTextCategory.setText(intent.getStringExtra(EXTRA_CATEGORY));
            editTextNotes.setText(intent.getStringExtra(EXTRA_NOTES));
            selectedImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH);

            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                Glide.with(this)
                        .load(new File(selectedImagePath))
                        .centerCrop()
                        .into(imagePreview);
            }
        } else {
            setTitle("Add Item");
        }

        // Set up camera button
        buttonTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermission();
            }
        });

        // Set up gallery button
        buttonSelectImage.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        // Set up save button
        buttonSave.setOnClickListener(v -> saveItem());
    }

    private void saveItem() {
        String name = editTextName.getText().toString().trim();
        String quantityStr = editTextQuantity.getText().toString().trim();
        String category = editTextCategory.getText().toString().trim();
        String notes = editTextNotes.getText().toString().trim();

        if (name.isEmpty()) {
            editTextName.setError("Name cannot be empty");
            return;
        }

        if (quantityStr.isEmpty()) {
            editTextQuantity.setError("Quantity cannot be empty");
            return;
        }

        int quantity = Integer.parseInt(quantityStr);

        Intent data = new Intent();
        data.putExtra(EXTRA_NAME, name);
        data.putExtra(EXTRA_QUANTITY, quantity);
        data.putExtra(EXTRA_CATEGORY, category);
        data.putExtra(EXTRA_NOTES, notes);

        // Use the most recent image path
        String imagePath = currentPhotoPath != null ? currentPhotoPath : selectedImagePath;
        data.putExtra(EXTRA_IMAGE_PATH, imagePath);

        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        if (id != -1) {
            data.putExtra(EXTRA_ID, id);
        }

        setResult(RESULT_OK, data);
        finish();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_STORAGE_PERMISSION);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "xyz.ziadboukhalkhal.shopsmart.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Camera photo saved to currentPhotoPath
                Glide.with(this)
                        .load(new File(currentPhotoPath))
                        .centerCrop()
                        .into(imagePreview);

                selectedImagePath = currentPhotoPath;
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                // Gallery image selected
                Uri selectedImageUri = data.getData();

                // Copy the file to our app's private storage
                try {
                    File destinationFile = createImageFile();
                    try (java.io.InputStream in = getContentResolver().openInputStream(selectedImageUri);
                         java.io.OutputStream out = new java.io.FileOutputStream(destinationFile)) {

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                    }
                    Glide.with(this)
                            .load(selectedImageUri)
                            .centerCrop()
                            .into(imagePreview);

                    // Set the selected path
                    selectedImagePath = destinationFile.getAbsolutePath();
                } catch (IOException e) {
                    Toast.makeText(this, "Error copying image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is needed to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission is needed to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }
}