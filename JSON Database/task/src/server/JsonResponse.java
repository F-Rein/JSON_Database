package server;

import com.google.gson.JsonElement;


public class JsonResponse {
    String response;
    String reason;

    JsonElement value;

    public JsonResponse() {
    }

    public void setResponse(String response) {
        this.response = response;
        if (response.equals("ERROR")) {
            setReason("No such key");
        }
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setValue(JsonElement value) {
        this.value = value;
    }
}
