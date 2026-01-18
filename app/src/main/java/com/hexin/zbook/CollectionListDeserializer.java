package com.hexin.zbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CollectionListDeserializer implements JsonDeserializer<List<Collection>> {
    @Override
    public List<Collection> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<Collection> collections = new ArrayList<>();
        JsonArray jsonArray = json.getAsJsonArray();

        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            Collection collection = new Collection();

            collection.key = jsonObject.get("key").getAsString();
            collection.version = jsonObject.get("version").getAsInt();

            if (jsonObject.has("data")) {
                JsonObject dataObject = jsonObject.getAsJsonObject("data");
                if (dataObject.has("name")) {
                    collection.name = dataObject.get("name").getAsString();
                }

                // Default to null
                collection.parentCollection = null; 
                // Only set the parent if it is explicitly a string
                if (dataObject.has("parentCollection")) {
                    JsonElement parentElement = dataObject.get("parentCollection");
                    if (parentElement.isJsonPrimitive() && parentElement.getAsJsonPrimitive().isString()) {
                        collection.parentCollection = parentElement.getAsString();
                    }
                }
            }
            collections.add(collection);
        }
        return collections;
    }
}
