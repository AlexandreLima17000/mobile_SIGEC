package com.example.mobilesigec;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class ListaReceitasFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_receitas, container, false);

        // Atualiza o título da barra superior para "Filtro de Receitas"
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Filtro de Receitas");
            }
        }

        CardView cardTacos = view.findViewById(R.id.card_tacos);
        if (cardTacos != null) {
            cardTacos.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_content_main, new DetalhesReceitaFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        return view;
    }
}