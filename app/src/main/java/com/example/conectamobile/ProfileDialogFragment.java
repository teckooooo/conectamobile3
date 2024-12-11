package com.example.conectamobile;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ProfileDialogFragment extends DialogFragment {

    private EditText editTextName;
    private Button btnSaveProfile;
    private ImageView profileImage;
    private FirebaseUser currentUser;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.perfil, container, false);

        // Inicializar los componentes
        editTextName = view.findViewById(R.id.editTextName);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        profileImage = view.findViewById(R.id.profileImage);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String nameFromDb = task.getResult().child("name").getValue(String.class);
                    String imageFromDb = task.getResult().child("profileImage").getValue(String.class);

                    if (nameFromDb != null) {
                        editTextName.setText(nameFromDb);
                    }
                    if (imageFromDb != null) {
                        byte[] bytes = Base64.decode(imageFromDb, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        profileImage.setImageBitmap(bitmap);
                    }
                } else {
                    Toast.makeText(getContext(), "Error al obtener los datos del perfil", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Cambiar el listener de btnChangePhoto al profileImage
        profileImage.setOnClickListener(v -> openGallery());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            profileImage.setImageBitmap(bitmap);

                            // Convertir la imagen a Base64
                            String encodedImage = encodeImage(bitmap);
                            saveProfileImage(encodedImage);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        return view;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void saveProfileImage(String encodedImage) {
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.child("profileImage").setValue(encodedImage).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Imagen de perfil actualizada con éxito", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error al actualizar la imagen", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
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

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.child("name").setValue(updatedName).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Nombre actualizado con éxito", Toast.LENGTH_SHORT).show();
                    dismiss(); //
                } else {
                    Toast.makeText(getContext(), "Error al actualizar el nombre", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }
}

