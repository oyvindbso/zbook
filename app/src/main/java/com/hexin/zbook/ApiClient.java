package com.hexin.zbook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.hexin.zbook.BuildConfig;

public class ApiClient {

    private static Retrofit retrofit = null;
    private static Gson customGson = null;

    private static void buildGson() {
        if (customGson == null) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Item.class, new ItemDeserializer());
            gsonBuilder.registerTypeAdapter(Collection.class, new CollectionDeserializer());
            customGson = gsonBuilder.create();
        }
    }

    public static Retrofit getClient() {
        // The client is now expected to be initialized by a call to resetClient
        // before the first sync. Returning null is safer than using a hardcoded URL.
        return retrofit;
    }

    public static void resetClient(String userUrl) {
        buildGson();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            httpClient.addInterceptor(logging);
        }

        retrofit = new Retrofit.Builder()
                .baseUrl(userUrl)
                .addConverterFactory(GsonConverterFactory.create(customGson))
                .client(httpClient.build())
                .build();
    }

    public static Retrofit getTestClient(String userUrl) {
        buildGson();
        
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            httpClient.addInterceptor(logging);
        }
        
        return new Retrofit.Builder()
                .baseUrl(userUrl)
                .addConverterFactory(GsonConverterFactory.create(customGson))
                .client(httpClient.build())
                .build();
    }
}
