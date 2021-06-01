package net.intensecorp.meeteazy.listener;

import net.intensecorp.meeteazy.models.User;

import java.util.List;

public interface UsersListener {

    void initiatePersonalCall(User callee);

    void initiateGroupCall(List<User> callees);
}
