package com.example.mobilesigec.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilesigec.R;
import com.example.mobilesigec.model.InsumoModelo;

import java.util.List;

public class InsumoAdapter extends RecyclerView.Adapter<InsumoAdapter.InsumoViewHolder> {

    private List<InsumoModelo> listaInsumos;
    private OnItemCheckListener listener;


    public interface OnItemCheckListener {
        void onItemCheckChanged();
    }

    // Construtor do Adapter
    public InsumoAdapter(List<InsumoModelo> listaInsumos, OnItemCheckListener listener) {
        this.listaInsumos = listaInsumos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InsumoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Sopra" (inflate) o layout XML da linha que criamos anteriormente
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checklist_row, parent, false);
        return new InsumoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsumoViewHolder holder, int position) {
        InsumoModelo insumo = listaInsumos.get(position);

        // Preenche os dados de texto
        holder.tvItemNome.setText(insumo.getNomeInsumo());
        holder.tvItemQuantidade.setText(insumo.getQuantidade() + " " + insumo.getUnidadeMedida());


        holder.cbItem.setOnCheckedChangeListener(null);

        // Define se o checkbox deve aparecer marcado ou vazio baseado no nosso InsumoModelo
        holder.cbItem.setChecked(insumo.isMarcado());

        // Agora sim, colocamos o "ouvidor" para capturar o clique real do usuário
        holder.cbItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            insumo.setMarcado(isChecked); // Atualiza o modelo (true/false)

            if (listener != null) {
                listener.onItemCheckChanged(); // Avisa o Fragmento para atualizar a barra de progresso
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaInsumos.size();
    }

    // O ViewHolder é o "guardião" das views do layout item_checklist_row.xml
    static class InsumoViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbItem;
        TextView tvItemNome;
        TextView tvItemQuantidade;

        public InsumoViewHolder(@NonNull View itemView) {
            super(itemView);
            cbItem = itemView.findViewById(R.id.cb_item);
            tvItemNome = itemView.findViewById(R.id.tv_item_nome);
            tvItemQuantidade = itemView.findViewById(R.id.tv_item_quantidade);
        }
    }
}