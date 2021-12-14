package com.dantaeusb.zettergallery.network.http.stub;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

public class PaintingsResponse {
    public String seed;

    public SellResponse sell;
    // Can't have enums, look for https://github.com/google/gson/issues/501
    public Map<String, Vector<PaintingItem>> feeds;

    public static class SellResponse {
        public boolean allowed;
        public String message;

        public SellResponse(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
    }

    public static class PaintingItem {
        public UUID uuid;
        public String name;
        public String author;
        public int resolution;
        public int sizeH;
        public int sizeW;
        @JsonAdapter(ByteArrayToBase64TypeAdapter.class)
        public byte[] color;
        public int price;

        public PaintingItem(UUID uuid, String name, String author, byte[] color, int resolution, int sizeH, int sizeW, int price) {
            this.uuid = uuid;
            this.name = name;
            this.author = author;
            this.color = color;
            this.resolution = resolution;
            this.sizeH = sizeH;
            this.sizeW = sizeW;
            this.price = price;
        }
    }

    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.getDecoder().decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }
    }
}
