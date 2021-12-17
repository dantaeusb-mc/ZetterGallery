package me.dantaeusb.zettergallery.network.http.stub;

import me.dantaeusb.zetter.storage.PaintingData;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Base64;

public class SaleRequest {
    public String name;
    public int resolution;
    public int sizeH;
    public int sizeW;
    @JsonAdapter(SaleRequest.ByteArrayToBase64TypeAdapter.class)
    public byte[] color;

    public SaleRequest(PaintingData paintingData) {
        final int resolution = paintingData.getResolution().getNumeric();

        this.name = paintingData.getPaintingName();
        this.resolution = resolution;
        this.sizeH = paintingData.getHeight() / resolution;
        this.sizeW = paintingData.getWidth() / resolution;
        this.color = this.extractColorData(paintingData);
    }

    /**
     * Converts ARGB to RGBA
     * @param paintingData
     * @return
     */
    private byte[] extractColorData(PaintingData paintingData) {
        final int paintingSize = paintingData.getHeight() * paintingData.getWidth();
        ByteBuffer inColor = paintingData.getColorDataBuffer();
        ByteBuffer outColor = ByteBuffer.allocate(inColor.rewind().remaining());

        for (int i = 0; i < paintingSize; i++) {
            outColor.putInt(Integer.rotateLeft(inColor.getInt(i * 4), 8));
        }

        return outColor.array();
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
