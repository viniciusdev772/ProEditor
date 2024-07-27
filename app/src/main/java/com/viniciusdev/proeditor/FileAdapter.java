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
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<FileItem> fileList;
    private OnItemClickListener listener;

    private static final int TYPE_FILE = 0;
    private static final int TYPE_EMPTY = 1;

    public interface OnItemClickListener {
        void onItemClick(FileItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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
                if (listener != null) {
                    listener.onItemClick(fileItem);
                }
            });
        } else {
            EmptyViewHolder emptyViewHolder = (EmptyViewHolder) holder;
            emptyViewHolder.textView.setText("Pasta vazia ou sem permissões de leitura");
        }
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
