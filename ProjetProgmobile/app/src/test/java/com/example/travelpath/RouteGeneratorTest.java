package com.example.travelpath;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.travelpath.model.OutdoorPreference;
import com.example.travelpath.model.Poi;
import com.example.travelpath.model.RouteSearchCriteria;
import com.example.travelpath.model.TravelRoute;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RouteGeneratorTest {

    private final RouteGenerator generator = new RouteGenerator();

    @Test
    public void generateRoutes_respectsBudgetAndDuration() {
        RouteSearchCriteria criteria = new RouteSearchCriteria();
        criteria.setMaxBudgetEuros(18);
        criteria.setAvailableDurationMinutes(120);

        List<TravelRoute> routes = generator.generateRoutes(buildSamplePois(), criteria);

        assertFalse(routes.isEmpty());
        for (TravelRoute route : routes) {
            assertTrue(route.getTotalBudgetEuros() <= 18);
            assertTrue(route.getTotalDurationMinutes() <= 120);
        }
    }

    @Test
    public void generateRoutes_requiresRequestedPlaceWhenProvided() {
        RouteSearchCriteria criteria = new RouteSearchCriteria();
        criteria.setMaxBudgetEuros(40);
        criteria.setAvailableDurationMinutes(180);
        criteria.setPlaceToInclude("Mirabeau");

        List<TravelRoute> routes = generator.generateRoutes(buildSamplePois(), criteria);

        assertFalse(routes.isEmpty());
        for (TravelRoute route : routes) {
            assertTrue(route.getStops().get(0).matchesPlace("Mirabeau")
                    || route.getStops().get(1).matchesPlace("Mirabeau"));
        }
    }

    @Test
    public void generateRoutes_relaxedEffortKeepsAccessibleStops() {
        RouteSearchCriteria criteria = new RouteSearchCriteria();
        criteria.setMaxBudgetEuros(30);
        criteria.setAvailableDurationMinutes(180);
        criteria.setRelaxedEffort(true);

        List<TravelRoute> routes = generator.generateRoutes(buildSamplePois(), criteria);

        assertFalse(routes.isEmpty());
        for (TravelRoute route : routes) {
            for (Poi stop : route.getStops()) {
                assertTrue(stop.isSeniorFriendly() || stop.isChildFriendly());
            }
        }
    }

    @Test
    public void generateRoutes_outdoorOnlyKeepsOutdoorStops() {
        RouteSearchCriteria criteria = new RouteSearchCriteria();
        criteria.setMaxBudgetEuros(30);
        criteria.setAvailableDurationMinutes(180);
        criteria.setOutdoorPreference(OutdoorPreference.OUTDOOR_ONLY);

        List<TravelRoute> routes = generator.generateRoutes(buildSamplePois(), criteria);

        assertFalse(routes.isEmpty());
        for (TravelRoute route : routes) {
            for (Poi stop : route.getStops()) {
                assertTrue(stop.isOutdoor());
            }
        }
    }

    private List<Poi> buildSamplePois() {
        return Arrays.asList(
                poi("cours_mirabeau", "Cours Mirabeau", "balade", 0, 45, true, true, true, "centre", "promenade"),
                poi("musee_granet", "Musee Granet", "culture", 12, 90, false, true, true, "art", "musee"),
                poi("place_cardeurs", "Place des Cardeurs", "gastronomie", 18, 75, true, true, true, "restaurant", "place"),
                poi("terrain_des_peintres", "Terrain des Peintres", "nature", 0, 40, true, true, true, "panorama"),
                poi("carrieres_bibemus", "Carrieres de Bibemus", "nature", 9, 120, true, false, false, "randonnee"),
                poi("bibliotheque_mejanes", "Bibliotheque Mejanes", "famille", 0, 50, false, true, true, "lecture")
        );
    }

    private Poi poi(
            String id,
            String name,
            String activity,
            int budget,
            int duration,
            boolean outdoor,
            boolean seniorFriendly,
            boolean childFriendly,
            String... keywords
    ) {
        return new Poi(
                id,
                name,
                activity,
                "Aix",
                name,
                43.5297,
                5.4474,
                budget,
                duration,
                outdoor,
                seniorFriendly,
                childFriendly,
                Arrays.asList(keywords));
    }
}