package net.intensecorp.meeteazy.listener;

import net.intensecorp.meeteazy.models.Contact;

import java.util.List;

public interface ActionListener {

    void initiatePersonalCall(Contact contact);

    void handleSelection(List<Contact> contacts);
}
