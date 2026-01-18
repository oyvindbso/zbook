package com.hexin.zbook;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface ZoteroApi {

    @Headers("Cache-Control: no-store")
    @GET("collections")
    Call<List<Collection>> getCollections(
            @Header("Zotero-API-Version") int apiVersion,
            @Header("Zotero-API-Key") String apiKey,
            @Query("since") int since,
            @Query("start") int start,
            @Query("limit") int limit,
            @Query("includeTrashed") int includeTrashed
    );

    @Headers("Cache-Control: no-store")
    @HEAD("collections")
    Call<Void> getCollectionsCount(
            @Header("Zotero-API-Version") int apiVersion,
            @Header("Zotero-API-Key") String apiKey,
            @Query("since") Integer since,
            @Query("includeTrashed") int includeTrashed
    );

    @Headers("Cache-Control: no-store")
    @GET("items")
    Call<List<Item>> getItems(
            @Header("Zotero-API-Version") int apiVersion,
            @Header("Zotero-API-Key") String apiKey,
            @Query("since") int since,
            @Query("start") int start,
            @Query("limit") int limit,
            @Query("includeTrashed") int includeTrashed,
            @Query("itemType") String itemType
    );

    @Headers("Cache-Control: no-store")
    @HEAD("items")
    Call<Void> getItemsCount(
            @Header("Zotero-API-Version") int apiVersion,
            @Header("Zotero-API-Key") String apiKey,
            @Query("since") Integer since,
            @Query("includeTrashed") int includeTrashed
    );

    @Headers("Cache-Control: no-store")
    @GET("deleted")
    Call<Deleted> getDeleted(
            @Header("Zotero-API-Version") int apiVersion,
            @Header("Zotero-API-Key") String apiKey,
            @Query("since") int since
    );
    
    @Headers("Cache-control: no-store")
    @GET("collections")
    Call<Void> checkCollections(@Header("Zotero-API-Key") String apiKey, @Query("limit") int limit);
}
