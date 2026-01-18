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

public class ItemDeserializer implements JsonDeserializer<Item> {
    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Item item = new Item();

        item.key = jsonObject.get("key").getAsString();
        item.version = jsonObject.get("version").getAsInt();

        // Check for the "deleted" flag in the top-level object
        if (jsonObject.has("deleted") && jsonObject.get("deleted").getAsBoolean()) {
            item.deleted = true;
        } else {
            item.deleted = false;
        }

        if (jsonObject.has("data")) {
            JsonObject data = jsonObject.getAsJsonObject("data");
            item.itemType = data.has("itemType") ? data.get("itemType").getAsString() : null;
            item.title = data.has("title") ? data.get("title").getAsString() : "";
            item.parentItem = data.has("parentItem") ? data.get("parentItem").getAsString() : null;
            item.dateAdded = data.has("dateAdded") ? data.get("dateAdded").getAsString() : null;
            item.dateModified = data.has("dateModified") ? data.get("dateModified").getAsString() : null;

            // Handle collection keys which can be an array
            if (data.has("collections") && data.get("collections").isJsonArray()) {
                JsonArray collectionsArray = data.get("collections").getAsJsonArray();
                if (collectionsArray.size() > 0) {
                    List<String> keys = new ArrayList<>();
                    for(JsonElement keyElement : collectionsArray){
                        keys.add(keyElement.getAsString());
                    }
                    item.collectionKey = String.join(",", keys);
                } else {
                    item.collectionKey = null;
                }
            } else {
                item.collectionKey = null;
            }

            // Attachment-specific fields
            if ("attachment".equals(item.itemType)) {
                item.filename = data.has("filename") ? data.get("filename").getAsString() : "";
                item.url = data.has("url") ? data.get("url").getAsString() : "";
                item.filesize = data.has("filesize") ? data.get("filesize").getAsLong() : 0;
            }


            // Handle creators
            if (data.has("creators") && data.get("creators").isJsonArray()) {
                JsonArray creatorsArray = data.get("creators").getAsJsonArray();
                StringBuilder creatorsBuilder = new StringBuilder();
                for (JsonElement creatorElement : creatorsArray) {
                    JsonObject creatorObject = creatorElement.getAsJsonObject();
                    if (creatorObject.has("firstName") && creatorObject.has("lastName")) {
                        String firstName = creatorObject.get("firstName").getAsString();
                        String lastName = creatorObject.get("lastName").getAsString();
                        if (creatorsBuilder.length() > 0) {
                            creatorsBuilder.append(", ");
                        }
                        creatorsBuilder.append(firstName).append(" ").append(lastName);
                    } else if (creatorObject.has("name")) { // For institutional authors
                        if (creatorsBuilder.length() > 0) {
                            creatorsBuilder.append(", ");
                        }
                        creatorsBuilder.append(creatorObject.get("name").getAsString());
                    }
                }
                item.creators = creatorsBuilder.toString();
            } else {
                item.creators = "";
            }

            // ... 解析 attachment-specific fields

            // ... 在解析 dateModified 之后

            item.publicationDate = data.has("date") ? data.get("date").getAsString() : null;

            // ... 在解析 creators 之前


        }

        return item;
    }
}
