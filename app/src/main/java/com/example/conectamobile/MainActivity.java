package com.example.conectamobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configura el Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Configura el FloatingActionButton
        FloatingActionButton agregarContacto = findViewById(R.id.agregarContacto);
        agregarContacto.setOnClickListener(v -> {
            // Muestra el DialogFragment cuando se presione el FAB
            AddContactoActivity dialog = new AddContactoActivity();
            dialog.show(getSupportFragmentManager(), "AddContactoActivity");
        });    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Infla el menú
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Maneja la acción al seleccionar una opción del menú
        int id = item.getItemId();
        if (id == R.id.action_perfil) {
            openProfile();
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openProfile() {
        ProfileDialogFragment profileDialogFragment = new ProfileDialogFragment();
        profileDialogFragment.show(getSupportFragmentManager(), "ProfileDialog");
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut(); // Cerrar sesión en Firebase
        Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show(); // Corregir el contexto
        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finalizar la actividad actual
    }
}
