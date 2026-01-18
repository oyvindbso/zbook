package com.hexin.zbook;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class CollectionDeserializer implements JsonDeserializer<Collection> {
    @Override
    public Collection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Collection collection = new Collection();

        collection.key = jsonObject.get("key").getAsString();
        collection.version = jsonObject.get("version").getAsInt();

        collection.deleted = false; // Default to not deleted

        if (jsonObject.has("data")) {
            JsonObject data = jsonObject.getAsJsonObject("data");

            // The "deleted" flag is inside the "data" object.
            if (data.has("deleted") && data.get("deleted").getAsBoolean()) {
                collection.deleted = true;
            }
            
            collection.name = data.has("name") ? data.get("name").getAsString() : "";

            collection.parentCollection = null; // Default to null
            if (data.has("parentCollection") && data.get("parentCollection").isJsonPrimitive()) {
                 JsonElement parentElement = data.get("parentCollection");
                 if(parentElement.getAsJsonPrimitive().isString()){
                     String parentKey = parentElement.getAsString();
                     if(!parentKey.isEmpty()){
                        collection.parentCollection = parentKey;
                     }
                 }
            }
        } else {
            // This case should not happen based on new info, but we handle it defensively.
            collection.name = "[No Data Object]";
            collection.parentCollection = null;
        }

        return collection;
    }
}
