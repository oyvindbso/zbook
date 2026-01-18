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

public class ItemListDeserializer implements JsonDeserializer<List<Item>> {
    @Override
    public List<Item> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<Item> items = new ArrayList<>();
        JsonArray jsonArray = json.getAsJsonArray();

        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            Item item = new Item();

            // Key and Version are in the top-level object
            item.key = jsonObject.get("key").getAsString();
            item.version = jsonObject.get("version").getAsInt();

            // Other details are in the nested "data" object
            if (jsonObject.has("data")) {
                JsonObject dataObject = jsonObject.getAsJsonObject("data");
                if (dataObject.has("itemType")) {
                    item.itemType = dataObject.get("itemType").getAsString();
                }
                if (dataObject.has("title")) {
                    item.title = dataObject.get("title").getAsString();
                }
                if (dataObject.has("parentItem") && !dataObject.get("parentItem").isJsonNull()) {
                    item.parentItem = dataObject.get("parentItem").getAsString();
                }
                if (dataObject.has("filename") && !dataObject.get("filename").isJsonNull()) {
                    item.filename = dataObject.get("filename").getAsString();
                }
                if (dataObject.has("url") && !dataObject.get("url").isJsonNull()) {
                    item.url = dataObject.get("url").getAsString();
                }
                 if (dataObject.has("dateAdded") && !dataObject.get("dateAdded").isJsonNull()) {
                    item.dateAdded = dataObject.get("dateAdded").getAsString();
                }
                if (dataObject.has("dateModified") && !dataObject.get("dateModified").isJsonNull()) {
                    item.dateModified = dataObject.get("dateModified").getAsString();
                }
                // In Zotero API, items in a collection have the collection key in an array.
                if (dataObject.has("collections") && dataObject.get("collections").isJsonArray()) {
                    JsonArray collectionsArray = dataObject.getAsJsonArray("collections");
                    if (collectionsArray.size() > 0) {
                        item.collectionKey = collectionsArray.get(0).getAsString();
                    }
                }
            }
            items.add(item);
        }
        return items;
    }
}
