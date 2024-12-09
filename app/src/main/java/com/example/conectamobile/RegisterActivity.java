package com.example.conectamobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText registerEmailEditText;
    private EditText registerPasswordEditText;
    private EditText registerNameEditText;
    private Button registerUserButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;  // Instancia de Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerEmailEditText = findViewById(R.id.txtEmail);
        registerPasswordEditText = findViewById(R.id.txtPassword);
        registerNameEditText = findViewById(R.id.txtNombre);
        registerUserButton = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();  // Inicializamos Firestore

        registerUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = registerEmailEditText.getText().toString().trim();
                String password = registerPasswordEditText.getText().toString().trim();
                String name = registerNameEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Completar todos los campos", Toast.LENGTH_SHORT).show();
                } else if (!isValidEmail(email)) {
                    Toast.makeText(RegisterActivity.this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                } else {
                    registerUser(email, password, name);
                }
            }
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void registerUser(String email, String password, final String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Actualizamos el perfil del usuario
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // Almacenar los datos del usuario en Firestore
                                            saveUserData(user.getUid(), name, email);

                                            Toast.makeText(RegisterActivity.this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(RegisterActivity.this, Login.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                        }
                    } else {
                        Log.w("RegisterActivity", "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String userId, String name, String email) {
        // Crear un mapa con los datos que queremos almacenar
        User user = new User(name, email);

        // Guardar los datos en Firestore
        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Datos de usuario guardados correctamente");
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error al guardar los datos", e);
                });
    }
}
