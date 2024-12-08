package com.example.conectamobile;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddContactoActivity extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Crea el inflador para el layout del dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_add_conacto, null);

        // Referencia a los campos y el botón
        EditText nombreContacto = view.findViewById(R.id.nombreContacto);
        EditText correoContacto = view.findViewById(R.id.correoContacto);
        Button btnCrearContacto = view.findViewById(R.id.btnCrearContacto);

        // Crear el diálogo con el layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setTitle("Nuevo Contacto");

        // Acción del botón para guardar el contacto
        btnCrearContacto.setOnClickListener(v -> {
            String nombre = nombreContacto.getText().toString().trim();
            String correo = correoContacto.getText().toString().trim();

            if (nombre.isEmpty() || correo.isEmpty()) {
                Toast.makeText(getContext(), "Rellene todos los campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(getContext(), "Usuario no autenticado.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference contactsRef = FirebaseDatabase.getInstance()
                    .getReference("contacts")
                    .child(currentUser.getUid());

            String contactId = contactsRef.push().getKey();
            Contact contact = new Contact(contactId, nombre, correo);

            contactsRef.child(contactId).setValue(contact)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Contacto agregado.", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al agregar contacto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        return builder.create();
    }
}