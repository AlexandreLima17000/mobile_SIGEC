package com.example.mobilesigec;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;

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

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        if (binding.appBarMain.fab != null) {
            binding.appBarMain.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).setAnchorView(R.id.fab).show());
        }
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        NavigationView navigationView = binding.navView;
        if (navigationView != null) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_calendario, R.id.nav_settings, R.id.nav_profile)
                    .setOpenableLayout(binding.drawerLayout)
                    .build();
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            // Trata o clique direto no item de Sair no Drawer
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_sair) {
                    efetuarLogout();
                    return true;
                }
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (handled && binding.drawerLayout != null) {
                    binding.drawerLayout.closeDrawers();
                }
                return handled;
            });
        }

        BottomNavigationView bottomNavigationView = binding.appBarMain.contentMain.bottomNavView;
        if (bottomNavigationView != null) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_calendario, R.id.nav_receita)
                    .build();
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (navController.getCurrentDestination() != null && itemId == navController.getCurrentDestination().getId()) {
                    return true;
                }
                if (itemId == R.id.nav_home) {
                    boolean popped = navController.popBackStack(R.id.nav_home, false);
                    if (!popped) {
                        navController.navigate(R.id.nav_home);
                    }
                    return true;
                } else if (itemId == R.id.nav_calendario) {
                    navController.navigate(R.id.nav_calendario);
                    return true;
                } else if (itemId == R.id.nav_receita) {
                    navController.navigate(R.id.nav_receita);
                    return true;
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                if (destId == R.id.nav_checklist_insumos
                        || destId == R.id.nav_checklist_utensilios
                        || destId == R.id.nav_detalhes_receita
                        || destId == R.id.nav_settings) {
                    bottomNavigationView.setVisibility(android.view.View.GONE);
                } else {
                    bottomNavigationView.setVisibility(android.view.View.VISIBLE);
                    Menu menu = bottomNavigationView.getMenu();
                    MenuItem menuItem = menu.findItem(destId);
                    if (menuItem != null) {
                        menuItem.setChecked(true);
                    }
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        NavigationView navView = findViewById(R.id.nav_view);
        if (navView == null) {
            getMenuInflater().inflate(R.menu.overflow, menu);
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        if (id == R.id.nav_profile || id == R.id.nav_settings) {
            navController.navigate(R.id.nav_settings);
            return true;
        } else if (id == R.id.nav_sair) {
            efetuarLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void efetuarLogout() {
        SharedPreferences prefs = getSharedPreferences("SessaoApp", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(this, "Sessão encerrada", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
