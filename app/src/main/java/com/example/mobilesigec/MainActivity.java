package com.example.mobilesigec;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilesigec.databinding.ActivityMainBinding;

import java.sql.Connection;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding =
                ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        if (binding.appBarMain.fab != null) {
            binding.appBarMain.fab.setOnClickListener(view ->
                    Snackbar.make(
                                    view,
                                    "Replace with your own action",
                                    Snackbar.LENGTH_LONG
                            )
                            .setAction("Action", null)
                            .setAnchorView(R.id.fab)
                            .show()
            );
        }

        // Recupera o NavHost
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment_content_main);

        assert navHostFragment != null;

        NavController navController =
                navHostFragment.getNavController();

        // MENU LATERAL
        NavigationView navigationView = binding.navView;

        if (navigationView != null) {

            mAppBarConfiguration =
                    new AppBarConfiguration.Builder(
                            R.id.nav_home,
                            R.id.nav_calendario,
                            R.id.nav_receita,
                            R.id.nav_settings
                    )
                            .setOpenableLayout(binding.drawerLayout)
                            .build();

            NavigationUI.setupActionBarWithNavController(
                    this,
                    navController,
                    mAppBarConfiguration
            );

            NavigationUI.setupWithNavController(
                    navigationView,
                    navController
            );
        }

        // BARRA INFERIOR
        BottomNavigationView bottomNavigationView =
                binding.appBarMain.contentMain.bottomNavView;

        if (bottomNavigationView != null) {

            mAppBarConfiguration =
                    new AppBarConfiguration.Builder(
                            R.id.nav_home,
                            R.id.nav_calendario,
                            R.id.nav_receita
                    )
                            .build();

            NavigationUI.setupActionBarWithNavController(
                    this,
                    navController,
                    mAppBarConfiguration
            );

            NavigationUI.setupWithNavController(
                    bottomNavigationView,
                    navController
            );
        }

        // NÃO COLOCAR NAVEGAÇÃO MANUAL PARA ListaReceitasFragment AQUI
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        boolean result = super.onCreateOptionsMenu(menu);

        NavigationView navView = findViewById(R.id.nav_view);

        if (navView == null) {

            getMenuInflater().inflate(R.menu.overflow, menu);

            Connection conexao = ConexaoMySQL.conectar();

            if (conexao != null) {

                Log.d(
                        "Conexão",
                        "Conexão estabelecida com sucesso!"
                );

                ConexaoMySQL.fecharConexao(conexao);

            } else {

                Log.d(
                        "Conexão",
                        "Erro ao conectar ao banco de dados!"
                );
            }
        }

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.nav_settings) {

            NavController navController =
                    Navigation.findNavController(
                            this,
                            R.id.nav_host_fragment_content_main
                    );

            navController.navigate(R.id.nav_settings);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {

        NavController navController =
                Navigation.findNavController(
                        this,
                        R.id.nav_host_fragment_content_main
                );

        return NavigationUI.navigateUp(
                navController,
                mAppBarConfiguration
        ) || super.onSupportNavigateUp();
    }
}