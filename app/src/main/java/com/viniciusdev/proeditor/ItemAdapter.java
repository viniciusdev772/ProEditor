package com.viniciusdev.proeditor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> itemList;
    private OnItemClickListener onItemClickListener;
    private ListarActivity activity;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public ItemAdapter(List<Item> itemList, OnItemClickListener onItemClickListener, ListarActivity activity) {
        this.itemList = itemList;
        this.onItemClickListener = onItemClickListener;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.text_apk.setText(item.getName());
        holder.text_projeto.setText(item.getId());
        holder.textview3.setText(item.getProjectPackage());
        holder.textview4.setText(item.getBuildInfo());

        // Carregar imagem usando Glide
        Glide.with(holder.itemView.getContext())
                .load(item.getIconPath()) // Supondo que o caminho do ícone está armazenado em getIconPath()
                //.placeholder(R.drawable.op_apk_2) // Imagem de placeholder enquanto carrega
                .into(holder.imageView1);

        holder.itemView.setOnClickListener(v -> {
            onItemClickListener.onItemClick(item);
            showDecryptedProjectDialog(item.getId(), item.getName());
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateList(List<Item> newList) {
        itemList = newList;
        notifyDataSetChanged();
    }

    private void showDecryptedProjectDialog(String projectId, String projectName) {
        String projectDataPath = activity.getSketchwareMyscListPath() + "/" + projectId + "/project";
        String decryptedJson = FileUtil.decryptProjectFile(projectDataPath);

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Decrypted Project - " + projectName)
                .setMessage(decryptedJson != null ? decryptedJson : "Failed to decrypt project")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text_apk;
        TextView text_projeto;
        TextView textview3;
        TextView textview4;
        ImageView imageView1;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text_apk = itemView.findViewById(R.id.text_apk);
            text_projeto = itemView.findViewById(R.id.text_projeto);
            textview3 = itemView.findViewById(R.id.textview3);
            textview4 = itemView.findViewById(R.id.textview4);
            imageView1 = itemView.findViewById(R.id.imageview1);
        }
    }
}
