package com.example.conectamobile;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

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
            // Aquí puedes manejar la lógica para guardar el nuevo contacto
            String nombre = nombreContacto.getText().toString();
            String correo = correoContacto.getText().toString();
            // Agregar lógica para guardar los datos
            dismiss(); // Cierra el diálogo después de agregar el contacto
        });

        return builder.create();
    }
}
