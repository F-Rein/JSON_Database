package server;

import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("DuplicatedCode")
public class ClientHandler extends Thread
{
    Path path = Paths.get("./src/server/data/db.json");
    //Path path = Paths.get("JSON Database/task/src/server/data/db.json");
    final DataInputStream input;
    final DataOutputStream output;
    final Socket socket;


    // Constructor
    public ClientHandler(Socket socket, DataInputStream input, DataOutputStream output)
    {
        this.socket = socket;
        this.input = input;
        this.output = output;
    }

    @Override
    public void run()
    {
        JsonResponse jsonResponse = new JsonResponse();

        JsonObject json;
        try {
            json = new Gson().fromJson(input.readUTF(), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String type = json.get("type").getAsString();
        System.out.println("type:" + type);

        Boolean exit = false;
        switch (type) {
            case "get" -> jsonResponse = get(json);//db.get(jsonInput.getKey());
            case "set" -> jsonResponse = set(json);//outerObject.add(key, setJson(json));
            case "delete" -> jsonResponse = delete(json);
            case "exit" -> {
                jsonResponse.setResponse("OK");
                exit = true;
            }
        }

        try {
            output.writeUTF(new Gson().toJson(jsonResponse)); // resend it to the client
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try
        {
            // closing resources
            this.input.close();
            this.output.close();
            this.socket.close();

        }catch(IOException e){
            e.printStackTrace();
        }
        if (exit) {
            Main.shutdown = true;
            System.out.println(Main.shutdown);
        }
    }

    private synchronized JsonElement setJsonRecursion(JsonObject jsonObject) {
        JsonObject innerObject = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : jsonObject.getAsJsonObject().entrySet()) {
            String valueKey = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                innerObject.add(valueKey, setJsonRecursion(value.getAsJsonObject()));
            } else {
                innerObject.add(valueKey, value);
            }
        }
        return innerObject;
    }
    private synchronized JsonElement setJson(JsonObject json) {
        JsonObject valueObject = json.get("value").getAsJsonObject();
        JsonObject innerObject = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : valueObject.entrySet()) {
            String valueKey = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                innerObject.add(valueKey, setJsonRecursion(value.getAsJsonObject()));
            } else {
                innerObject.add(valueKey, value);
            }
        }
        return innerObject;
    }
    private synchronized JsonObject updateJson(JsonObject json, ArrayList<String> keys) {
        JsonObject jsonObj = getJsonObjectFromFile(path);
        JsonObject nestedJsonObj = jsonObj.getAsJsonObject(keys.get(0));
        for (int i = 1; i <= keys.size() - 2; i++) {
            nestedJsonObj = nestedJsonObj.getAsJsonObject(keys.get(i));
        }
        nestedJsonObj.addProperty(keys.get(keys.size() - 1), json.get("value").getAsString());
        return jsonObj;
    }
    private synchronized JsonObject deleteJson(JsonObject json, ArrayList<String> keys) {
        JsonObject jsonObj = getJsonObjectFromFile(path);
        try {
            JsonObject nestedJsonObj = jsonObj.getAsJsonObject(keys.get(0));
            for (int i = 1; i <= keys.size() - 2; i++) {
                nestedJsonObj = nestedJsonObj.getAsJsonObject(keys.get(i));
            }
            nestedJsonObj.remove(keys.get(keys.size() - 1));
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return jsonObj;
    }
    private synchronized JsonResponse delete(JsonObject json) {
        JsonResponse jsonResponse = new JsonResponse();
        jsonResponse.setResponse("OK");
        JsonObject outerObject = new JsonObject();
        JsonElement jsonKeys = json.get("key");
        ArrayList<String> keys = new ArrayList<>();
        if (jsonKeys.isJsonArray()) {
            for (JsonElement entry : jsonKeys.getAsJsonArray()) {
                keys.add(entry.getAsString());
            }
        } else {
            keys.add(jsonKeys.getAsString());
        }
        outerObject = deleteJson(json, keys);
        if (outerObject == null) {
            jsonResponse.setResponse("ERROR");
        } else {
            writeJsonObjectToFile(outerObject, path);
        }
        return jsonResponse;
    }

    private synchronized JsonResponse set(JsonObject json) {
        JsonResponse jsonResponse = new JsonResponse();
        jsonResponse.setResponse("OK");
        JsonObject outerObject = new JsonObject();
        JsonElement jsonKeys = json.get("key");
        ArrayList<String> keys = new ArrayList<>();
        if (jsonKeys.isJsonArray()) {
            for (JsonElement entry : jsonKeys.getAsJsonArray()) {
                keys.add(entry.getAsString());
            }
        } else {
            keys.add(jsonKeys.getAsString());
        }
        if (keys.size() > 1) {
            outerObject = updateJson(json, keys);
        } else {
            String key = keys.get(0);
            if (json.get("value").isJsonObject()) {
                outerObject.add(key, setJson(json));
            } else {
                outerObject.add(key, json.get("value"));
            }
        }

        writeJsonObjectToFile(outerObject, path);

        return jsonResponse;
    }

    private synchronized JsonResponse get(JsonObject jsonObject) {
        JsonObject json = getJsonObjectFromFile(path);
        JsonResponse jsonResponse = new JsonResponse();
        JsonElement jsonKeys = jsonObject.get("key");
        ArrayList<String> keys = new ArrayList<>();
        try {
            for (JsonElement entry : jsonKeys.getAsJsonArray()) {
                keys.add(entry.getAsString());
            }
        } catch (Exception e) {
            System.out.println("Exception: " +e);
        }
        JsonElement value = null;
        try {
            value = getRecursion(json, keys);
        } catch (Exception e) {
            System.out.println("Exception getRecursion to value: " + e);
        }
        System.out.println(value);
        if (value != null) {
            System.out.println(value);
            jsonResponse.setResponse("OK");
            jsonResponse.setValue(value);
        } else {
            jsonResponse.setResponse("ERROR");
            jsonResponse.setReason("No such key");
        }
        return jsonResponse;
    }

    private synchronized JsonElement getRecursion(JsonObject jsonObject, ArrayList<String> keys) {
        JsonElement returnObject = null;
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String valueKey = entry.getKey();
            JsonElement value = entry.getValue();
            if (valueKey.equals(keys.get(0))) {
                if (keys.size() == 1) {
                    try {
                        return value;
                    } catch (Exception e) {
                        System.out.println("Exception value: " + e);
                    }
                } else {
                    keys.remove(0);
                    returnObject = getRecursion(value.getAsJsonObject(), keys);
                }
            }
        }
        return returnObject;
    }

    private synchronized void writeJsonObjectToFile(JsonObject outerObject, Path path) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try (
                Writer writer = Files.newBufferedWriter(path)
        ) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder
                    .setPrettyPrinting()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            gson.toJson(outerObject, writer);
        } catch (Exception e) {
            System.out.println("Writer Exception: " + e);
        } finally {
            writeLock.unlock();
        }
    }

    private synchronized JsonObject getJsonObjectFromFile(Path path) {
        JsonObject json = null;
        ReadWriteLock lock = new ReentrantReadWriteLock();
        Lock readLock = lock.readLock();
        readLock.lock();
        try (Reader reader = Files.newBufferedReader(path)) {
            Gson gson = new Gson();
            json = gson.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            System.out.println("Reader Exception " + e);
        } finally {
            readLock.unlock();
        }
        return json;
    }
}
