package com.lumina;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    Library library;
    ArtCache art;

    @Override
    public void onCreate() {
        super.onCreate();
        Ui.init(this);
        library = new Library(this);
        art = new ArtCache(this);
    }

    static App get(Context c) { return (App) c.getApplicationContext(); }
}
