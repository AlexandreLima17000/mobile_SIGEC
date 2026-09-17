package com.example.mobilesigec.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilesigec.R;
import com.example.mobilesigec.model.UtensilioModelo;

import java.util.List;

public class UtensilioAdapter extends RecyclerView.Adapter<UtensilioAdapter.UtensilioViewHolder> {

    private List<UtensilioModelo> listaUtensilios;
    private OnItemCheckListener listener;

    public interface OnItemCheckListener {
        void onItemCheckChanged();
    }

    public UtensilioAdapter(List<UtensilioModelo> listaUtensilios, OnItemCheckListener listener) {
        this.listaUtensilios = listaUtensilios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UtensilioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checklist_row, parent, false);
        return new UtensilioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UtensilioViewHolder holder, int position) {
        UtensilioModelo utensilio = listaUtensilios.get(position);

        holder.tvItemNome.setText(utensilio.getNomeUtensilio());
        holder.tvItemQuantidade.setText(utensilio.getQuantidade() + " un");

        holder.cbItem.setOnCheckedChangeListener(null);
        holder.cbItem.setChecked(utensilio.isMarcado());

        holder.cbItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            utensilio.setMarcado(isChecked);
            if (listener != null) {
                listener.onItemCheckChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaUtensilios.size();
    }

    static class UtensilioViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbItem;
        TextView tvItemNome;
        TextView tvItemQuantidade;

        public UtensilioViewHolder(@NonNull View itemView) {
            super(itemView);
            cbItem = itemView.findViewById(R.id.cb_item);
            tvItemNome = itemView.findViewById(R.id.tv_item_nome);
            tvItemQuantidade = itemView.findViewById(R.id.tv_item_quantidade);
        }
    }
}