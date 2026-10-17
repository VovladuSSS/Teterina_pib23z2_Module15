package academy.tochkavhoda.competition.server;

/**
 * Standard response envelope returned by every Server method.
 * responseCode 200 means success, 400 means client error.
 * responseData is a JSON string (payload or {"error":"..."}).
 */
public class ServerResponse {
    private int responseCode;
    private String responseData;

    public ServerResponse() {
    }

    public ServerResponse(int responseCode, String responseData) {
        this.responseCode = responseCode;
        this.responseData = responseData;
    }

    public static ServerResponse success(String jsonPayload) {
        return new ServerResponse(200, jsonPayload == null ? "{}" : jsonPayload);
    }

    public static ServerResponse success() {
        return new ServerResponse(200, "{}");
    }

    public static ServerResponse error(String humanReadableMessage) {
        return new ServerResponse(400, "{\"error\":\"" + escape(humanReadableMessage) + "\"}");
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }
}
