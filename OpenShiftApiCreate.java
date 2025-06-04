import okhttp3.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenShiftApiCreate {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("BEARER_TOKEN");
        String caCertPath = "/path/to/ca.crt";
        String apiServer = "https://api.openshift.example.com:6443";
        String namespace = "default";

        OkHttpClient client = new OkHttpClient.Builder()
            .sslSocketFactory(SSLSocketFactoryUtil.fromCAFile(caCertPath), SSLSocketFactoryUtil.trustManagerFromCA(caCertPath))
            .build();

        String jsonBody = new String(Files.readAllBytes(Paths.get("deployment.json")));
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(apiServer + "/apis/apps/v1/namespaces/" + namespace + "/deployments")
            .addHeader("Authorization", "Bearer " + token)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Body: " + response.body().string());
        }
    }
}
