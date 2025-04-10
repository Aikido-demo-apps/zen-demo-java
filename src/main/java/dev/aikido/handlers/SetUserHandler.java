package dev.aikido.handlers;

import dev.aikido.agent_api.SetUser;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.List;
import java.util.Arrays;

import static dev.aikido.agent_api.SetUser.setUser;

public class SetUserHandler implements Handler {

    private static final List<String> NAMES = Arrays.asList(
            "Hans", "Pablo", "Samuel", "Timo", "Tudor", "Willem", "Wout", "Yannis"
    );

    private static String getNameById(int id) {
        int index = Math.abs(id) % NAMES.size();
        return NAMES.get(index);
    }

    @Override
    public void handle(Context ctx) throws Exception {
        String userId = ctx.header("user");
        if (userId != null) {
            try {
                int id = Integer.parseInt(userId);
                setUser(new SetUser.UserObject(userId, getNameById(id)));
            } catch (NumberFormatException e) {
                System.err.println("Warning: 'user' header is not a valid integer: " + userId);
            }
        } else {
            String userIdHeader = ctx.header("X-User-ID");
            String userNameHeader = ctx.header("X-User-Name");
            if (userIdHeader != null && userNameHeader != null) {
                 try {
                    Integer.parseInt(userIdHeader);
                    setUser(new SetUser.UserObject(userIdHeader, userNameHeader));
                 } catch (NumberFormatException e) {
                    System.err.println("Warning: 'X-User-ID' header is not a valid integer: " + userIdHeader);
                 }
            }
        }
    }
}
