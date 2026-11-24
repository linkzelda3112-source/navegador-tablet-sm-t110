package com.sobreviveunano.game;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.blockstore.Blockstore;
import com.google.android.gms.auth.blockstore.BlockstoreClient;
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse;
import com.google.android.gms.auth.blockstore.StoreBytesData;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/** Guarda el progreso en Block Store de Google: sobrevive a desinstalar y reinstalar la app. */
@CapacitorPlugin(name = "PersistentStore")
public class PersistentStorePlugin extends Plugin {

    @PluginMethod
    public void set(final PluginCall call) {
        String key = call.getString("key");
        String value = call.getString("value");
        if (key == null || value == null) {
            call.reject("faltan datos");
            return;
        }
        try {
            BlockstoreClient client = Blockstore.getClient(getContext());
            StoreBytesData data = new StoreBytesData.Builder()
                    .setBytes(value.getBytes(StandardCharsets.UTF_8))
                    .setKey(key)
                    .setShouldBackupToCloud(true)
                    .build();
            client.storeBytes(data)
                    .addOnSuccessListener(r -> call.resolve())
                    .addOnFailureListener(e -> call.reject(String.valueOf(e.getMessage())));
        } catch (Throwable t) {
            call.reject(String.valueOf(t));
        }
    }

    @PluginMethod
    public void get(final PluginCall call) {
        final String key = call.getString("key");
        if (key == null) {
            call.reject("falta la clave");
            return;
        }
        try {
            BlockstoreClient client = Blockstore.getClient(getContext());
            RetrieveBytesRequest req = new RetrieveBytesRequest.Builder()
                    .setKeys(Collections.singletonList(key))
                    .build();
            client.retrieveBytes(req)
                    .addOnSuccessListener(resp -> {
                        JSObject out = new JSObject();
                        Map<String, RetrieveBytesResponse.BlockstoreData> m = resp.getBlockstoreDataMap();
                        RetrieveBytesResponse.BlockstoreData d = m.get(key);
                        if (d != null) {
                            out.put("value", new String(d.getBytes(), StandardCharsets.UTF_8));
                        }
                        call.resolve(out);
                    })
                    .addOnFailureListener(e -> call.reject(String.valueOf(e.getMessage())));
        } catch (Throwable t) {
            call.reject(String.valueOf(t));
        }
    }
}
