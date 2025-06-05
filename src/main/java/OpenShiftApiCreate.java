import okhttp3.*;

import javax.net.ssl.X509TrustManager;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenShiftApiCreate {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("BEARER_TOKEN");
        String caCertPath = "./openshift-ca.crt";
        String apiServer = "https://127.0.0.1:6443";
        String namespace = "default";

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(
                        SSLSocketFactoryUtil.fromCAFile(caCertPath),
                        (X509TrustManager) SSLSocketFactoryUtil.trustManagerFromCA(caCertPath).getTrustManagers()[0]
                )
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
