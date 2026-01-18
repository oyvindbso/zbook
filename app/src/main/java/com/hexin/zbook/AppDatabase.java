package com.hexin.zbook;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(exportSchema = false, entities = {Collection.class, Item.class}, version = 8) // Increment version number
public abstract class AppDatabase extends RoomDatabase {

    public abstract CollectionDao collectionDao();

    public abstract ItemDao itemDao();

    private static volatile AppDatabase INSTANCE;

    static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "zbook_database")
                            .fallbackToDestructiveMigration() // Add destructive migration strategy
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
