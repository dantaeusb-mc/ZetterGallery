package me.dantaeusb.zettergallery.network.http.dto;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.*;

public class PaintingsResponse {
    // Can't have enums, look for https://github.com/google/gson/issues/501
    public Map<String, Vector<PaintingItem>> feeds;
    public CycleInfo cycleInfo;

    public static class PaintingItem {
        public UUID uuid;
        public String name;
        public Author author;
        public int resolution;
        public int sizeH;
        public int sizeW;
        @JsonAdapter(ByteArrayToBase64TypeAdapter.class)
        public byte[] color;
        public int price;

        public PaintingItem(UUID uuid, String name, Author author, byte[] color, int resolution, int sizeH, int sizeW, int price) {
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

    public static class Author {
        public UUID uuid;
        public String nickname;

        public Author(UUID uuid, String nickname) {
            this.uuid = uuid;
            this.nickname = nickname;
        }
    }

    public static class CycleInfo {
        public int incrementId;
        public String seed;
        public Date startsAt;
        public Date endsAt;

        public CycleInfo(int incrementId, String seed, Date startsAt, Date endsAt) {
            this.incrementId = incrementId;
            this.seed = seed;
            this.startsAt = startsAt;
            this.endsAt = endsAt;
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
