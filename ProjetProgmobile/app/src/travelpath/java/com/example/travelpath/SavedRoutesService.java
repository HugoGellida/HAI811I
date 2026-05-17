package com.example.travelpath;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.travelpath.data.LocalSavedRoutesStore;
import com.example.travelpath.model.TravelRoute;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SavedRoutesService {

    public interface SavedRoutesCallback {
        void onRoutesChanged(List<TravelRoute> routes);

        void onError(@NonNull Exception error);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final LocalSavedRoutesStore localSavedRoutesStore = new LocalSavedRoutesStore();

    public void saveRoute(
        @NonNull Context context,
            @NonNull String userId,
            @NonNull TravelRoute route,
            @NonNull OnSuccessListener<Void> onSuccessListener,
            @NonNull OnFailureListener onFailureListener
    ) {
        String documentId = resolveDocumentId(route);
        route.setId(documentId);
        route.setSaved(true);
        route.setSavedAt(System.currentTimeMillis());
        getSavedRoutesCollection(userId)
            .document(documentId)
                .set(route)
                .addOnSuccessListener(unused -> {
                    persistLocally(context, userId, route);
                    onSuccessListener.onSuccess(unused);
                })
                .addOnFailureListener(error -> {
                    if (persistLocally(context, userId, route)) {
                        onSuccessListener.onSuccess(null);
                        return;
                    }

                    onFailureListener.onFailure(error);
                });
    }

        public void deleteRoute(
            @NonNull Context context,
            @NonNull String userId,
            @NonNull TravelRoute route,
            @NonNull OnSuccessListener<Void> onSuccessListener,
            @NonNull OnFailureListener onFailureListener
        ) {
        boolean deletedLocally = deleteLocally(context, userId, route);
        getSavedRoutesCollection(userId)
            .document(resolveDocumentId(route))
            .delete()
            .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(error -> {
                    if (deletedLocally) {
                        onSuccessListener.onSuccess(null);
                        return;
                    }

                    onFailureListener.onFailure(error);
                });
        }

    public ListenerRegistration listenToSavedRoutes(
            @NonNull Context context,
            @NonNull String userId,
            @NonNull SavedRoutesCallback callback
    ) {
        List<TravelRoute> cachedRoutes = localSavedRoutesStore.loadRoutes(context, userId);
        if (!cachedRoutes.isEmpty()) {
            callback.onRoutesChanged(cachedRoutes);
        }

        return getSavedRoutesCollection(userId)
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        List<TravelRoute> fallbackRoutes = localSavedRoutesStore.loadRoutes(context, userId);
                        if (!fallbackRoutes.isEmpty()) {
                            callback.onRoutesChanged(fallbackRoutes);
                            return;
                        }

                        callback.onError(error);
                        return;
                    }

                    List<TravelRoute> remoteRoutes = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            TravelRoute route = document.toObject(TravelRoute.class);
                            if (route == null) {
                                continue;
                            }

                            if (route.getId() == null || route.getId().trim().isEmpty()) {
                                route.setId(document.getId());
                            }

                            route.setSaved(true);
                            remoteRoutes.add(route);
                        }
                    }

                    callback.onRoutesChanged(mergeRoutes(localSavedRoutesStore.loadRoutes(context, userId), remoteRoutes));
                });
    }

    private CollectionReference getSavedRoutesCollection(@NonNull String userId) {
        return db.collection("users")
                .document(userId)
                .collection("saved_routes");
    }

    private String resolveDocumentId(TravelRoute route) {
        if (route.getId() != null && !route.getId().trim().isEmpty()) {
            return route.getId();
        }

        return "route_" + System.currentTimeMillis();
    }

    private boolean persistLocally(@NonNull Context context, @NonNull String userId, @NonNull TravelRoute route) {
        try {
            localSavedRoutesStore.saveRoute(context, userId, route);
            return true;
        } catch (IllegalStateException error) {
            return false;
        }
    }

    private boolean deleteLocally(@NonNull Context context, @NonNull String userId, @NonNull TravelRoute route) {
        try {
            localSavedRoutesStore.deleteRoute(context, userId, route);
            return true;
        } catch (IllegalStateException error) {
            return false;
        }
    }

    private List<TravelRoute> mergeRoutes(List<TravelRoute> localRoutes, List<TravelRoute> remoteRoutes) {
        Map<String, TravelRoute> mergedRoutes = new LinkedHashMap<>();

        for (TravelRoute route : remoteRoutes) {
            if (route == null || route.getId() == null || route.getId().trim().isEmpty()) {
                continue;
            }

            mergedRoutes.put(route.getId(), route);
        }

        for (TravelRoute route : localRoutes) {
            if (route == null || route.getId() == null || route.getId().trim().isEmpty()) {
                continue;
            }

            if (!mergedRoutes.containsKey(route.getId())) {
                mergedRoutes.put(route.getId(), route);
            }
        }

        return new ArrayList<>(mergedRoutes.values());
    }
}