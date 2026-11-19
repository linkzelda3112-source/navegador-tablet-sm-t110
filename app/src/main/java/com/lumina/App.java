package com.lumina;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    Library library;
    ArtCache art;
    CoverStore covers;
    Personal personal;
    Charts charts;

    @Override
    public void onCreate() {
        super.onCreate();
        Ui.init(this);
        library = new Library(this);
        covers = new CoverStore(this);
        art = new ArtCache(this, covers);
        personal = new Personal(this);
        charts = new Charts(this);
    }

    static App get(Context c) { return (App) c.getApplicationContext(); }
}
