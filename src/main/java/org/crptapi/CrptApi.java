package org.crptapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    public static void main(String[] args) {

        final int requestLimit = 2;
        final TimeUnit timeUnitInSeconds = TimeUnit.SECONDS;
        final CrptApi crptApi = new CrptApi(timeUnitInSeconds, requestLimit);

        final String accessToken = "PUT YOUR ACCESS TOKEN";
        final Document document = getDocumentObject();
        final HttpResponse<String> httpResponse = crptApi.createDocument(accessToken, document);
        LOGGER.info("Http response status code: {}", httpResponse.statusCode());
    }

    private static Document getDocumentObject() {

        final CrptApi.Product product = new CrptApi.Product(
                "certificateDocument",
                "certificateDocumentDate",
                "certificateDocumentNumber",
                "ownerInn",
                "producerInn",
                "productionDate",
                "tnvedCode",
                "uitCode",
                "uituCode"
        );

        return new Document(
                "description",
                "docId",
                "docStatus",
                "docType",
                false,
                "ownerInn",
                "participantInn",
                "producerInn",
                "productionDate",
                "productionType",
                List.of(product),
                "regDate",
                "regNumber"
        );
    }

    private static final Logger LOGGER = LogManager.getLogger(CrptApi.class);
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final int REQUEST_DURATION = 1;
    private final int requestLimit;
    private final Duration interval;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private volatile long lastTimestamp;
    private long requestCount;

    public CrptApi(final TimeUnit timeUnit, final int requestLimit) {
        this.requestLimit = requestLimit;
        this.interval = Duration.ofMillis(timeUnit.toMillis(REQUEST_DURATION));
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.lastTimestamp = System.currentTimeMillis();
        this.requestCount = requestLimit;
    }

    public HttpResponse<String> createDocument(final String accessToken, final Document document) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Requested to create document: {} with access token {}", document, accessToken);
        }

        // Check request limit and wait if necessary
        trySendRequest();

        final JsonNode requestBody = objectMapper.convertValue(document, JsonNode.class);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header(HttpUtils.CONTENT_TYPE, HttpUtils.APPLICATION_JSON)
                .header(HttpUtils.AUTHORIZATION, HttpUtils.BEARER + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        final HttpResponse<String> response;
        try {
            LOGGER.info("Sending request at: {} with request body: {}", LocalTime.now(), requestBody);
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final IOException | InterruptedException e) {
            LOGGER.error("Exception was thrown while calling API, cause: ", e.getCause());
            throw new HttpClientRequestException("Exception occurred during http client request call");
        }
        return response;
    }

    private void trySendRequest() {
        synchronized (this) {
            while (!canSendRequest()) {
                try {
                    wait(interval.toMillis());
                } catch (final InterruptedException e) {
                    LOGGER.warn("Exception occurred during allowable request limit waiting, cause: ", e.getCause());
                }
            }
        }
    }

    private boolean canSendRequest() {

        final long currentTime = System.currentTimeMillis();
        // Calculate the number of allowed requests based on elapsed time since the last refill.
        final long allowedRequests = ((currentTime - lastTimestamp) / interval.toMillis()) * requestLimit;

        if (allowedRequests > 0) {
            requestCount = allowedRequests;
            lastTimestamp = currentTime;
        }

        if (requestCount > 0) {
            requestCount--;
            return true;
        }
        return false;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class Document {
        public String description;
        public String docId;
        public String docStatus;
        public String docType;
        public boolean importRequest;
        public String ownerInn;
        public String participantInn;
        public String producerInn;
        public String productionDate;
        public String productionType;
        public List<Product> products;
        public String regDate;
        public String regNumber;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @ToString
    public static class Product {
        public String certificateDocument;
        public String certificateDocumentDate;
        public String certificateDocumentNumber;
        public String ownerInn;
        public String producerInn;
        public String productionDate;
        public String tnvedCode;
        public String uitCode;
        public String uituCode;
    }

    @NoArgsConstructor
    static class HttpClientRequestException extends RuntimeException {
        public HttpClientRequestException(final String message) {
            super(message);
        }
    }

    static final class HttpUtils {

        public static final String CONTENT_TYPE = "Content-Type";
        public static final String APPLICATION_JSON = "application/json";
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER = "Bearer ";

        private HttpUtils() {
        }
    }
}