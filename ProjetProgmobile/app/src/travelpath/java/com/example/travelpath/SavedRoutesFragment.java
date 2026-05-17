package com.example.travelpath;

import com.example.projetprogmobile.FeatureNavigation;
import com.example.projetprogmobile.R;
import com.example.travelpath.model.TravelRoute;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SavedRoutesFragment extends Fragment {

    private final List<TravelRoute> savedRoutes = new ArrayList<>();
    private final SavedRoutesService savedRoutesService = new SavedRoutesService();

    private RecyclerView recyclerView;
    private TextView statusView;
    private Button signInButton;
    private FirebaseAuth auth;
    private RouteAdapter routeAdapter;
    private ListenerRegistration routesRegistration;

    public SavedRoutesFragment() {
        super(R.layout.fragment_saved_routes);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_routes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.travelpath_saved_routes_recycler_view);
        statusView = view.findViewById(R.id.travelpath_saved_routes_status_text);
        signInButton = view.findViewById(R.id.travelpath_saved_routes_sign_in_button);

        routeAdapter = new RouteAdapter(savedRoutes, false, true, this::openRouteOnMap, null, this::deleteRoute);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(routeAdapter);

        signInButton.setOnClickListener(v -> startActivity(
                FeatureNavigation.createLoginIntent(requireContext(), FeatureNavigation.DESTINATION_TRAVEL_PATH)));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRoutes();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachListener();
    }

    private void refreshRoutes() {
        if (auth.getCurrentUser() == null || auth.getUid() == null) {
            detachListener();
            savedRoutes.clear();
            routeAdapter.notifyDataSetChanged();
            recyclerView.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
            updateStatus(getString(R.string.travelpath_saved_routes_sign_in_required));
            return;
        }

        signInButton.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        updateStatus(getString(R.string.travelpath_saved_routes_loading));
        attachListener(auth.getUid());
    }

    private void attachListener(@NonNull String userId) {
        detachListener();
        routesRegistration = savedRoutesService.listenToSavedRoutes(requireContext(), userId, new SavedRoutesService.SavedRoutesCallback() {
            @Override
            public void onRoutesChanged(List<TravelRoute> routes) {
                savedRoutes.clear();
                savedRoutes.addAll(routes);
                routeAdapter.notifyDataSetChanged();

                if (savedRoutes.isEmpty()) {
                    updateStatus(getString(R.string.travelpath_saved_routes_empty));
                    return;
                }

                updateStatus(null);
            }

            @Override
            public void onError(@NonNull Exception error) {
                updateStatus(getString(R.string.travelpath_saved_routes_error));
            }
        });
    }

    private void detachListener() {
        if (routesRegistration == null) {
            return;
        }

        routesRegistration.remove();
        routesRegistration = null;
    }

    private void openRouteOnMap(TravelRoute route) {
        if (route == null) {
            return;
        }

        startActivity(MapActivity.createGeneratedRouteIntent(requireContext(), route));
    }

    private void deleteRoute(TravelRoute route) {
        if (route == null || auth.getUid() == null) {
            Toast.makeText(requireContext(), R.string.travelpath_route_delete_failure, Toast.LENGTH_SHORT).show();
            return;
        }

        savedRoutesService.deleteRoute(
            requireContext(),
                auth.getUid(),
                route,
                unused -> Toast.makeText(requireContext(), R.string.travelpath_route_delete_success, Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(requireContext(), R.string.travelpath_route_delete_failure, Toast.LENGTH_SHORT).show());
    }

    private void updateStatus(@Nullable String message) {
        boolean hasMessage = message != null && !message.trim().isEmpty();
        statusView.setVisibility(hasMessage ? View.VISIBLE : View.GONE);
        statusView.setText(hasMessage ? message : "");
    }
}