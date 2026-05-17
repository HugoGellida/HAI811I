package com.example.travelpath;

import com.example.projetprogmobile.FeatureNavigation;
import com.example.projetprogmobile.R;
import com.example.travelpath.data.AixPoiRepository;
import com.example.travelpath.model.OutdoorPreference;
import com.example.travelpath.model.Poi;
import com.example.travelpath.model.RouteSearchCriteria;
import com.example.travelpath.model.TravelRoute;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TravelPathSearchFragment extends Fragment {

    private final List<TravelRoute> generatedRoutes = new ArrayList<>();
    private final RouteGenerator routeGenerator = new RouteGenerator();
    private final AixPoiRepository poiRepository = new AixPoiRepository();
    private final SavedRoutesService savedRoutesService = new SavedRoutesService();

    private RecyclerView routesRecyclerView;
    private RouteAdapter routeAdapter;
    private EditText budgetInput;
    private MultiAutoCompleteTextView placeInput;
    private Spinner activitySpinner;
    private Spinner durationSpinner;
    private Spinner effortSpinner;
    private Spinner outdoorSpinner;
    private TextView authStatusView;
    private TextView searchStatusView;
    private FirebaseAuth auth;
    private List<Poi> availablePois = new ArrayList<>();
    private final Set<String> savedRouteIds = new HashSet<>();
    private ListenerRegistration savedRoutesRegistration;

    public TravelPathSearchFragment() {
        super(R.layout.fragment_travelpath_search);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_travelpath_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        authStatusView = view.findViewById(R.id.travelpath_auth_status_text);
        searchStatusView = view.findViewById(R.id.travelpath_search_status_text);
        budgetInput = view.findViewById(R.id.travelpath_budget_input);
        placeInput = view.findViewById(R.id.travelpath_place_input);
        activitySpinner = view.findViewById(R.id.travelpath_activity_spinner);
        durationSpinner = view.findViewById(R.id.travelpath_duration_spinner);
        effortSpinner = view.findViewById(R.id.travelpath_effort_spinner);
        outdoorSpinner = view.findViewById(R.id.travelpath_outdoor_spinner);
        routesRecyclerView = view.findViewById(R.id.travelpath_routes_recycler_view);
        Button generateButton = view.findViewById(R.id.travelpath_generate_button);

        routeAdapter = new RouteAdapter(generatedRoutes, true, this::openRouteOnMap, this::saveRoute);
        routesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        routesRecyclerView.setNestedScrollingEnabled(false);
        routesRecyclerView.setAdapter(routeAdapter);

        loadPoiData();
        setupInputs();
        generateButton.setOnClickListener(v -> generateRoutes());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAuthState();
        startSavedRoutesSync();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachSavedRoutesSync();
    }

    private void loadPoiData() {
        try {
            availablePois = poiRepository.loadPois(requireContext());
            bindPlaceSuggestions();
            bindActivityOptions();
            updateSearchStatus(null);
        } catch (IllegalStateException error) {
            availablePois = new ArrayList<>();
            updateSearchStatus(getString(R.string.travelpath_search_dataset_error));
        }
    }

    private void setupInputs() {
        bindDurationOptions();
        bindEffortOptions();
        bindOutdoorOptions();
    }

    private void bindActivityOptions() {
        List<String> options = poiRepository.loadActivityCategories(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);
    }

    private void bindDurationOptions() {
        List<String> options = Arrays.asList(
                getString(R.string.travelpath_duration_1h),
                getString(R.string.travelpath_duration_2h),
                getString(R.string.travelpath_duration_3h),
                getString(R.string.travelpath_duration_4h),
                getString(R.string.travelpath_duration_5h));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(adapter);
        durationSpinner.setSelection(1);
    }

    private void bindEffortOptions() {
        List<String> options = Arrays.asList(
                getString(R.string.travelpath_effort_relaxed),
                getString(R.string.travelpath_effort_standard));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        effortSpinner.setAdapter(adapter);
    }

    private void bindOutdoorOptions() {
        List<String> options = Arrays.asList(
                getString(R.string.travelpath_outdoor_any),
                getString(R.string.travelpath_outdoor_yes),
                getString(R.string.travelpath_outdoor_no));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outdoorSpinner.setAdapter(adapter);
    }

    private void bindPlaceSuggestions() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                poiRepository.loadPlaceSuggestions(requireContext()));
        placeInput.setAdapter(adapter);
        placeInput.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    }

    private void generateRoutes() {
        if (availablePois.isEmpty()) {
            updateSearchStatus(getString(R.string.travelpath_search_dataset_error));
            return;
        }

        RouteSearchCriteria criteria = buildCriteria();
        if (criteria == null) {
            return;
        }

        List<TravelRoute> routes = routeGenerator.generateRoutes(availablePois, criteria);
        generatedRoutes.clear();
        generatedRoutes.addAll(routes);
        syncGeneratedRouteSavedState();

        if (routes.isEmpty()) {
            String focus = criteria.hasPlaceToInclude()
                    ? criteria.getPlaceToInclude().trim()
                    : activitySpinner.getSelectedItem().toString();
            updateSearchStatus(getString(R.string.travelpath_search_no_results, focus));
            return;
        }

        updateSearchStatus(getString(R.string.travelpath_search_results_summary, routes.size()));
    }

    @Nullable
    private RouteSearchCriteria buildCriteria() {
        String budgetText = budgetInput.getText() == null ? "" : budgetInput.getText().toString().trim();
        if (budgetText.isEmpty()) {
            budgetInput.setError(getString(R.string.travelpath_search_budget_required));
            return null;
        }

        int budget;
        try {
            budget = Integer.parseInt(budgetText);
        } catch (NumberFormatException error) {
            budgetInput.setError(getString(R.string.travelpath_search_budget_invalid));
            return null;
        }

        if (budget <= 0) {
            budgetInput.setError(getString(R.string.travelpath_search_budget_invalid));
            return null;
        }

        budgetInput.setError(null);

        RouteSearchCriteria criteria = new RouteSearchCriteria();
        criteria.setMaxBudgetEuros(budget);
        criteria.setActivityCategory(String.valueOf(activitySpinner.getSelectedItem()));
        criteria.setPlaceToInclude(placeInput.getText() == null ? "" : placeInput.getText().toString().trim());
        criteria.setAvailableDurationMinutes(parseDurationMinutes(String.valueOf(durationSpinner.getSelectedItem())));
        criteria.setRelaxedEffort(getString(R.string.travelpath_effort_relaxed)
                .equals(String.valueOf(effortSpinner.getSelectedItem())));
        criteria.setOutdoorPreference(parseOutdoorPreference(String.valueOf(outdoorSpinner.getSelectedItem())));
        return criteria;
    }

    private int parseDurationMinutes(String rawDuration) {
        if (rawDuration == null) {
            return 120;
        }

        String digits = rawDuration.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 120;
        }

        return Integer.parseInt(digits) * 60;
    }

    private OutdoorPreference parseOutdoorPreference(String rawValue) {
        if (getString(R.string.travelpath_outdoor_yes).equals(rawValue)) {
            return OutdoorPreference.OUTDOOR_ONLY;
        }

        if (getString(R.string.travelpath_outdoor_no).equals(rawValue)) {
            return OutdoorPreference.INDOOR_ONLY;
        }

        return OutdoorPreference.ANY;
    }

    private void openRouteOnMap(TravelRoute route) {
        if (route == null) {
            return;
        }

        startActivity(MapActivity.createGeneratedRouteIntent(requireContext(), route));
    }

    private void saveRoute(TravelRoute route) {
        if (route == null) {
            return;
        }

        if (auth.getCurrentUser() == null) {
            startActivity(FeatureNavigation.createLoginIntent(requireContext(), FeatureNavigation.DESTINATION_TRAVEL_PATH));
            return;
        }

        String userId = auth.getUid();
        if (userId == null) {
            Toast.makeText(requireContext(), R.string.travelpath_route_save_failure, Toast.LENGTH_SHORT).show();
            return;
        }

        savedRoutesService.saveRoute(
            requireContext(),
                userId,
                route,
                unused -> {
                    if (route.getId() != null && !route.getId().trim().isEmpty()) {
                        savedRouteIds.add(route.getId());
                    }
                    syncGeneratedRouteSavedState();
                    Toast.makeText(requireContext(), R.string.travelpath_route_save_success, Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(requireContext(), R.string.travelpath_route_save_failure, Toast.LENGTH_SHORT).show());
    }

    private void updateAuthState() {
        boolean signedIn = auth.getCurrentUser() != null;
        routeAdapter.setSignedIn(signedIn);

        if (!signedIn) {
            savedRouteIds.clear();
            syncGeneratedRouteSavedState();
        }

        authStatusView.setText(signedIn
                ? R.string.travelpath_search_signed_in
                : R.string.travelpath_search_signed_out);
    }

    private void startSavedRoutesSync() {
        if (auth.getCurrentUser() == null || auth.getUid() == null) {
            savedRouteIds.clear();
            syncGeneratedRouteSavedState();
            return;
        }

        detachSavedRoutesSync();
        savedRoutesRegistration = savedRoutesService.listenToSavedRoutes(requireContext(), auth.getUid(), new SavedRoutesService.SavedRoutesCallback() {
            @Override
            public void onRoutesChanged(List<TravelRoute> routes) {
                savedRouteIds.clear();
                for (TravelRoute route : routes) {
                    if (route == null || route.getId() == null || route.getId().trim().isEmpty()) {
                        continue;
                    }

                    savedRouteIds.add(route.getId());
                }
                syncGeneratedRouteSavedState();
            }

            @Override
            public void onError(@NonNull Exception error) {
                savedRouteIds.clear();
                syncGeneratedRouteSavedState();
            }
        });
    }

    private void detachSavedRoutesSync() {
        if (savedRoutesRegistration == null) {
            return;
        }

        savedRoutesRegistration.remove();
        savedRoutesRegistration = null;
    }

    private void syncGeneratedRouteSavedState() {
        for (TravelRoute route : generatedRoutes) {
            boolean isSaved = route != null
                    && route.getId() != null
                    && savedRouteIds.contains(route.getId());
            if (route != null) {
                route.setSaved(isSaved);
            }
        }

        routeAdapter.notifyDataSetChanged();
    }

    private void updateSearchStatus(@Nullable String message) {
        boolean hasMessage = message != null && !message.trim().isEmpty();
        searchStatusView.setVisibility(hasMessage ? View.VISIBLE : View.GONE);
        searchStatusView.setText(hasMessage ? message : "");
    }
}