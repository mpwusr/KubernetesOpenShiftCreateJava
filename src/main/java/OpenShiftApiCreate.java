import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.Map;

public class OpenShiftApiCreate {

    /** Fail fast if an env var / .env entry is missing or empty. */
    private static String requireEnv(Dotenv dotenv, String key) {
        String val = dotenv.get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException(
                    "Required environment variable " + key + " is not set");
        }
        return val;
    }

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure()
                .filename(".env")   // still lets you override with real env vars
                .load();

        // ── REQUIRED settings ───────────────────────────────────────
        String token           = requireEnv(dotenv, "BEARER_TOKEN");
        String caCertPath      = requireEnv(dotenv, "CA_CERT_PATH");
        String apiServer       = requireEnv(dotenv, "API_SERVER");
        String namespace       = requireEnv(dotenv, "NAMESPACE");
        String deploymentUriStr= requireEnv(dotenv, "DEPLOYMENT_URI");
        // ────────────────────────────────────────────────────────────

        URI deploymentUri = URI.create(deploymentUriStr);
        Map<String, Object> obj;

        // Fetch or load the YAML manifest
        if (deploymentUri.getScheme().startsWith("http")) {
            OkHttpClient fetchClient = new OkHttpClient();
            Request fetchRequest = new Request.Builder().url(deploymentUriStr).build();
            try (Response response = fetchClient.newCall(fetchRequest).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new RuntimeException("Failed to fetch deployment file: " + response);
                }
                try (InputStream in = response.body().byteStream()) {
                    obj = new ObjectMapper(new YAMLFactory()).readValue(in, Map.class);
                }
            }
        } else if ("file".equals(deploymentUri.getScheme())) {
            try (InputStream in = Files.newInputStream(Paths.get(deploymentUri))) {
                obj = new ObjectMapper(new YAMLFactory()).readValue(in, Map.class);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported URI scheme for DEPLOYMENT_URI: " + deploymentUriStr);
        }

        // YAML → JSON
        String jsonBody = new ObjectMapper().writeValueAsString(obj);

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(
                        SSLSocketFactoryUtil.fromCAFile(caCertPath),
                        (X509TrustManager) SSLSocketFactoryUtil
                                .trustManagerFromCA(caCertPath)
                                .getTrustManagers()[0])
                .build();

        RequestBody body = RequestBody.create(jsonBody,
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(apiServer + "/apis/apps/v1/namespaces/" + namespace + "/deployments")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Body: " +
                    (response.body() != null ? response.body().string() : "No response body"));
        }
    }
}
