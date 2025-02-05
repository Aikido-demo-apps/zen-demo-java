package dev.aikido;

import dev.aikido.agent_api.context.Context;
import dev.aikido.agent_api.middleware.AikidoJavalinMiddleware;
import dev.aikido.handlers.SetUserHandler;
import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static dev.aikido.Helpers.*;

public class JavalinPostgres {
    public static class CommandRequest { public String userCommand;}
    public static class RequestRequest { public String url;}
    public static class CreateRequest { public String name;}

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(Integer.valueOf(System.getProperty("portNumber", "8088")));
        // Add our middleware :
        app.before(new SetUserHandler());
        app.before(new AikidoJavalinMiddleware());

        app.get("/", ctx -> {
            ctx.html(loadHtmlFromFile("src/main/resources/index.html"));
        });
        app.get("/pages/execute", ctx -> {
            ctx.html(loadHtmlFromFile("src/main/resources/execute_command.html"));
        });
        app.get("/pages/create", ctx -> {
            ctx.html(loadHtmlFromFile("src/main/resources/create.html"));
        });
        app.get("/pages/request", ctx -> {
            ctx.html(loadHtmlFromFile("src/main/resources/request.html"));
        });
        app.get("/pages/read", ctx -> {
            ctx.html(loadHtmlFromFile("src/main/resources/read_file.html"));
        });

        // Test rate-limiting :
        app.get("/test_ratelimiting_1", ctx -> {
            ctx.result("Request successful (Ratelimiting 1)");
        });
        app.get("/test_ratelimiting_2", ctx -> {
            ctx.result("Request successful (Ratelimiting 2)");
        });

        // Test bot blocking :
        app.get("/test_bot_blocking", ctx -> {
            ctx.result("Hello World! Bot blocking enabled on this route.");
        });

        // Test user blocking :
        app.get("/test_user_blocking", ctx -> {
            String id = Context.get().getUser().id();
            ctx.result("Hello User with id: " + id);
        });

        // Clear database :
        app.get("/clear", ctx -> {
            DatabaseHelper.clearAll();
            ctx.result("Cleared successfully.");
        });


        // Serve API :
        app.get("/api/pets", ctx -> {
            ArrayList<Object> pets = DatabaseHelper.getAllPets();
            ctx.json(pets);
        });

        app.post("/api/create", ctx -> {
            String petName = ctx.bodyAsClass(CreateRequest.class).name;
            ctx.result(petName);
            Integer rowsCreated = DatabaseHelper.createPetByName(petName);
            if (rowsCreated == -1) {
                ctx.result(String.valueOf("Database error occurred"));
            } else {
                ctx.result("Success!");
            }
        });

        app.post("/api/execute", ctx -> {
            String userCommand = ctx.bodyAsClass(CommandRequest.class).userCommand;
            String result = executeShellCommand(userCommand);
            ctx.result(result);
        });
        app.get("/api/execute/<command>", ctx -> {
            String userCommand = ctx.pathParam("command");
            String result = executeShellCommand(userCommand);
            ctx.result(result);
        });

        app.post("/api/request", ctx -> {
            String url = ctx.bodyAsClass(RequestRequest.class).url;
            String response = makeHttpRequest(url);
            ctx.result(response);
        });

        app.post("/api/request2", ctx -> {
            String url = ctx.bodyAsClass(RequestRequest.class).url;
            String response = makeHttpRequestWithOkHttp(url);
            ctx.result(response);
        });

        app.get("/api/read", ctx -> {
            String filePath = ctx.queryParam("path");
            String content = Helpers.readFile(filePath);
            ctx.result(content);
        });
    }

    private static String loadHtmlFromFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            return "Error loading HTML file: " + e.getMessage();
        }
        return content.toString();
    }
}