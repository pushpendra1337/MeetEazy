package net.intensecorp.meeteazy.listener;

import net.intensecorp.meeteazy.models.Contact;

import java.util.List;

public interface ActionListener {

    void onInitiatePersonalCall(Contact contact);

    void onSelection(List<Contact> contacts);
}
