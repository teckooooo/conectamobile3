package com.example.conectamobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileDialogFragment extends DialogFragment {

    private EditText editTextName;
    private Button btnSaveProfile, btnChangePhoto;
    private ImageView profileImage;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el diseño del perfil
        View view = inflater.inflate(R.layout.perfil, container, false);

        // Referencias a los elementos de la vista
        editTextName = view.findViewById(R.id.editTextName);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        profileImage = view.findViewById(R.id.profileImage);

        // Obtener el usuario actual
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Leer el nombre desde la base de datos de Firebase
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    // Obtener el nombre guardado en la base de datos
                    String nameFromDb = task.getResult().child("name").getValue(String.class);

                    // Si el nombre existe, actualizarlo en la interfaz
                    if (nameFromDb != null) {
                        editTextName.setText(nameFromDb);
                    }
                } else {
                    Toast.makeText(getContext(), "Error al obtener los datos del perfil", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Configurar el botón para guardar cambios
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Ajustar el tamaño del cuadro de diálogo
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Método para guardar los cambios
    private void saveProfileChanges() {
        if (currentUser != null) {
            String updatedName = editTextName.getText().toString().trim();

            if (updatedName.isEmpty()) {
                Toast.makeText(getContext(), "Por favor, ingrese un nombre válido", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar el nombre en Firebase Realtime Database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.child("name").setValue(updatedName).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Nombre actualizado con éxito", Toast.LENGTH_SHORT).show();
                    dismiss(); // Cerrar el diálogo
                } else {
                    Toast.makeText(getContext(), "Error al actualizar el nombre", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }
}
