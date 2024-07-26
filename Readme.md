# CrptAPI

## Description

The CrptApi class is a Java implementation designed to interact with the Fair Sign API in a thread-safe manner while enforcing rate limits on the number of API requests. This class ensures that the number of API requests made within a specified time interval does not exceed a given limit, which is crucial for preventing overuse and adhering to API usage policies.

### Features
Thread Safety: The class is designed to handle concurrent access from multiple threads safely, ensuring that rate limits are respected across different threads.

Rate Limiting: The class enforces a limit on the number of API requests that can be made within a specified time interval. This prevents exceeding the allowed request quota and ensures fair usage of the API.

Configurable Time Intervals: The time interval for the rate limit can be specified using standard time units (seconds, minutes, etc.), providing flexibility in defining the request limit period.

### Usage example

    public static void main(String[] args) {

        final int requestLimit = 2;
        final TimeUnit timeUnitInSeconds = TimeUnit.SECONDS;
        final CrptApi crptApi = new CrptApi(timeUnitInSeconds, requestLimit);

        final String accessToken = "PUT YOUR ACCESS TOKEN";
        final Document document = getDocumentObject();
        final HttpResponse<String> httpResponse = crptApi.createDocument(accessToken, document);
        log.info("Http response status code: {}", httpResponse.statusCode());
    }

    private static Document getDocumentObject(){

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
