package net.intensecorp.meeteazy.listener;

import net.intensecorp.meeteazy.models.User;

public interface UsersListener {

    void initiateVideoCall(User user);

    void initiateVoiceCall(User user);
}
