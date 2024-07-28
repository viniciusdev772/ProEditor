package com.viniciusdev.proeditor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<FileItem> fileList;
    private final Set<FileItem> selectedItems = new HashSet<>();
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private SelectionListener selectionListener;
    private boolean multiSelectMode = false;

    private static final int TYPE_FILE = 0;
    private static final int TYPE_EMPTY = 1;

    public interface OnItemClickListener {
        void onItemClick(FileItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(FileItem item);
    }

    public interface SelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public FileAdapter(List<FileItem> fileList) {
        this.fileList = fileList;
    }

    @Override
    public int getItemViewType(int position) {
        if (fileList.get(position).getName().equals("EMPTY")) {
            return TYPE_EMPTY;
        } else {
            return TYPE_FILE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FILE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty, parent, false);
            return new EmptyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_FILE) {
            FileViewHolder fileViewHolder = (FileViewHolder) holder;
            FileItem fileItem = fileList.get(position);
            fileViewHolder.textView.setText(fileItem.getName());
            File file = new File(fileItem.getPath());

            if (fileItem.isDirectory()) {
                fileViewHolder.imageView.setImageResource(R.drawable.ic_folder);
            } else if (isImageFile(file.getPath())) {
                Glide.with(fileViewHolder.imageView.getContext())
                        .load(file)
                        .into(fileViewHolder.imageView);
            } else {
                fileViewHolder.imageView.setImageResource(R.drawable.ic_file);
            }

            fileViewHolder.itemView.setOnClickListener(v -> {
                if (multiSelectMode) {
                    toggleSelection(fileItem, fileViewHolder);
                } else if (clickListener != null) {
                    clickListener.onItemClick(fileItem);
                }
            });

            fileViewHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(fileItem);
                }
                return true;
            });

            fileViewHolder.itemView.setBackgroundColor(selectedItems.contains(fileItem) ? 0x9934B5E4 : 0);
        } else {
            EmptyViewHolder emptyViewHolder = (EmptyViewHolder) holder;
            emptyViewHolder.textView.setText("Pasta vazia ou sem permissões de leitura");
        }
    }

    private void toggleSelection(FileItem fileItem, FileViewHolder fileViewHolder) {
        if (selectedItems.contains(fileItem)) {
            selectedItems.remove(fileItem);
            fileViewHolder.itemView.setBackgroundColor(0);
        } else {
            selectedItems.add(fileItem);
            fileViewHolder.itemView.setBackgroundColor(0x9934B5E4);
        }
        notifySelectionChanged();
    }

    private boolean isImageFile(String path) {
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        for (String extension : imageExtensions) {
            if (path.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedItems.size());
        }
    }

    public Set<FileItem> getSelectedItems() {
        return selectedItems;
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public void setMultiSelectMode(boolean enabled) {
        multiSelectMode = enabled;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView textView;

        FileViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView = itemView.findViewById(R.id.textView);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        EmptyViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}
