package xyz.ziadboukhalkhal.shopsmart.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Objects;

import xyz.ziadboukhalkhal.shopsmart.R;
import xyz.ziadboukhalkhal.shopsmart.data.local.entity.ShoppingListItem;

public class ShoppingListAdapter extends ListAdapter<ShoppingListItem, ShoppingListAdapter.ShoppingItemViewHolder> {

    private OnItemClickListener listener;
    private OnItemCheckedChangeListener checkedChangeListener;

    public ShoppingListAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<ShoppingListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ShoppingListItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ShoppingListItem oldItem, @NonNull ShoppingListItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ShoppingListItem oldItem, @NonNull ShoppingListItem newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getQuantity() == newItem.getQuantity() &&
                    oldItem.isPurchased() == newItem.isPurchased() &&
                    Objects.equals(oldItem.getImagePath(), newItem.getImagePath());
        }
    };

    @NonNull
    @Override
    public ShoppingItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping, parent, false);
        return new ShoppingItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingItemViewHolder holder, int position) {
        ShoppingListItem currentItem = getItem(position);
        holder.textViewName.setText(currentItem.getName());
        holder.textViewQuantity.setText("Quantity: " + currentItem.getQuantity());
//        holder.checkBoxPurchased.setChecked(currentItem.isPurchased());
        holder.checkBoxPurchased.setOnCheckedChangeListener(null);
        holder.checkBoxPurchased.setChecked(currentItem.isPurchased());
        holder.textViewCategory.setText(currentItem.getCategory());


        // Only show notes if they exist
        if (currentItem.getNotes() != null && !currentItem.getNotes().isEmpty()) {
            holder.textViewNotes.setText(currentItem.getNotes());
            holder.textViewNotes.setVisibility(View.VISIBLE);
        } else {
            holder.textViewNotes.setVisibility(View.GONE);
        }

        // Load image with Glide
        if (currentItem.getImagePath() != null && !currentItem.getImagePath().isEmpty()) {
            File imageFile = new File(currentItem.getImagePath());
            if (imageFile.exists()) {
                Glide.with(holder.itemView)
                        .load(imageFile)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(holder.imageViewItem);
            } else {
                holder.imageViewItem.setImageResource(R.drawable.ic_image_placeholder);
            }
        } else {
            holder.imageViewItem.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.checkBoxPurchased.setOnClickListener(v -> {
            if (checkedChangeListener != null && position != RecyclerView.NO_POSITION) {
                currentItem.setPurchased(holder.checkBoxPurchased.isChecked());
                checkedChangeListener.onItemCheckedChange(currentItem, holder.checkBoxPurchased.isChecked());
            }
        });
    }

    public ShoppingListItem getItemAt(int position) {
        return getItem(position);
    }

    public class ShoppingItemViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private TextView textViewQuantity;
        private CheckBox checkBoxPurchased;
        private ImageView imageViewItem;

        private TextView textViewCategory;
        private TextView textViewNotes;

        public ShoppingItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_name);
            textViewQuantity = itemView.findViewById(R.id.text_quantity);
            checkBoxPurchased = itemView.findViewById(R.id.checkbox_purchased);
            imageViewItem = itemView.findViewById(R.id.image_item);
            textViewCategory = itemView.findViewById(R.id.text_category);
            textViewNotes = itemView.findViewById(R.id.text_notes);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position));
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ShoppingListItem item);
    }

    public interface OnItemCheckedChangeListener {
        void onItemCheckedChange(ShoppingListItem item, boolean isChecked);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemCheckedChangeListener(OnItemCheckedChangeListener listener) {
        this.checkedChangeListener = listener;
    }
}