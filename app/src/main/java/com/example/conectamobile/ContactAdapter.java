package com.example.conectamobile;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> implements Filterable {

    private List<Contact> contactList; // Lista completa
    private List<Contact> contactListFiltered; // Lista filtrada

    public ContactAdapter(List<Contact> contactList) {
        this.contactList = contactList;
        this.contactListFiltered = new ArrayList<>(contactList); // Inicialmente, la lista filtrada es igual a la lista completa
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactListFiltered.get(position); // Usar la lista filtrada
        holder.nameTextView.setText(contact.getName());
        holder.emailTextView.setText(contact.getEmail());

        holder.itemView.setOnClickListener(v -> {
            // Creación del Intent para pasar los datos al Chat
            Intent intent = new Intent(holder.itemView.getContext(), Chat.class);
            intent.putExtra("contactId", contact.getId());       // Pasa el id del contacto
            intent.putExtra("contactName", contact.getName());   // Pasa el nombre del contacto
            intent.putExtra("contactEmail", contact.getEmail()); // Pasa el email del contacto
            holder.itemView.getContext().startActivity(intent);   // Inicia la actividad de chat
        });
    }
    public void updateContactList(List<Contact> newContactList) {
        contactList.clear();
        contactList.addAll(newContactList);
        contactListFiltered.clear();
        contactListFiltered.addAll(newContactList); // Asegúrate de sincronizar ambas listas
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return contactListFiltered.size(); // Cambiar para devolver el tamaño de la lista filtrada
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Contact> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    // Si no hay texto para filtrar, mostrar todos los contactos
                    filteredList.addAll(contactList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Contact contact : contactList) {
                        if (contact.getName().toLowerCase().contains(filterPattern)) {
                            filteredList.add(contact); // Agregar contactos que coincidan con el filtro
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                contactListFiltered.clear();
                contactListFiltered.addAll((List<Contact>) results.values);
                notifyDataSetChanged(); // Actualizar la lista
            }
        };
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, emailTextView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewName);
            emailTextView = itemView.findViewById(R.id.textViewEmail);
        }
    }
}
