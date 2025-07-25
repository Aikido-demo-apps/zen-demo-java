package dev.aikido;

import io.sentry.Sentry;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import dev.aikido.agent_api.vulnerabilities.AikidoException;
import java.io.*;
import java.nio.file.Path;

public class Helpers {
    public static class ResponseResult {
        private final int statusCode;
        private final String message;

        public ResponseResult(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getMessage() {
            return message;
        }
    }

    public static ResponseResult executeShellCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            int exitCode = process.waitFor();
            return new ResponseResult(exitCode == 0 ? 200 : 500, output.toString());
        } catch (IOException | InterruptedException e) {
            Sentry.captureException(e);
            return new ResponseResult(500, "Error: " + e.getMessage());
        }
    }

    public static ResponseResult makeHttpRequest(String urlString) {
        StringBuilder response = new StringBuilder();
        try {
            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            in.close();
            return new ResponseResult(responseCode, response.toString());
        } catch (AikidoException e) {
            Sentry.captureException(e);
            return new ResponseResult(500, "Error: " + e.getMessage());
        } catch (IOException e) {
            Sentry.captureException(e);
            return new ResponseResult(400, "Error: " + e.getMessage());
        }
    }

    public static ResponseResult makeHttpRequestWithOkHttp(String urlString) {
        StringBuilder response = new StringBuilder();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(urlString).build();
        try (Response resp = client.newCall(request).execute()) {
            if (resp.body() != null) {
                response.append(resp.body().string());
            }
            return new ResponseResult(resp.code(), response.toString());

        } 
        catch (AikidoException e) {
            Sentry.captureException(e);
            return new ResponseResult(500, "Error: " + e.getMessage());
        }
         catch (IOException e) {
            Sentry.captureException(e);
            // (Is a directory)
            if (e.getMessage().contains("Is a directory")) {
                return new ResponseResult(500, "Error: " + e.getMessage());
            }
            return new ResponseResult(400, "Error: " + e.getMessage());
        }

    }

    public static ResponseResult readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        File file = Path.of("src/main/resources/blogs", filePath).toFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            return new ResponseResult(200, content.toString());
        } catch (IOException e) {
            Sentry.captureException(e);
            return new ResponseResult(500, "Error: " + e.getMessage());
        }
    }
}
