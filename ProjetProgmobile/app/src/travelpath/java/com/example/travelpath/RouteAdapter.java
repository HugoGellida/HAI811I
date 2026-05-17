package com.example.travelpath;

import com.example.projetprogmobile.R;
import com.example.travelpath.model.TravelRoute;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {

    public interface OnOpenRouteListener {
        void onOpenRoute(TravelRoute route);
    }

    public interface OnSaveRouteListener {
        void onSaveRoute(TravelRoute route);
    }

    public interface OnDeleteRouteListener {
        void onDeleteRoute(TravelRoute route);
    }

    private final List<TravelRoute> routes;
    private final boolean showSaveAction;
    private final boolean showDeleteAction;
    private final OnOpenRouteListener onOpenRouteListener;
    private final OnSaveRouteListener onSaveRouteListener;
    private final OnDeleteRouteListener onDeleteRouteListener;
    private boolean signedIn;

    public RouteAdapter(
            List<TravelRoute> routes,
            boolean showSaveAction,
            @NonNull OnOpenRouteListener onOpenRouteListener,
            @Nullable OnSaveRouteListener onSaveRouteListener
    ) {
        this(routes, showSaveAction, false, onOpenRouteListener, onSaveRouteListener, null);
    }

    public RouteAdapter(
            List<TravelRoute> routes,
            boolean showSaveAction,
            boolean showDeleteAction,
            @NonNull OnOpenRouteListener onOpenRouteListener,
            @Nullable OnSaveRouteListener onSaveRouteListener,
            @Nullable OnDeleteRouteListener onDeleteRouteListener
    ) {
        this.routes = routes;
        this.showSaveAction = showSaveAction;
        this.showDeleteAction = showDeleteAction;
        this.onOpenRouteListener = onOpenRouteListener;
        this.onSaveRouteListener = onSaveRouteListener;
        this.onDeleteRouteListener = onDeleteRouteListener;
    }

    public void setSignedIn(boolean signedIn) {
        this.signedIn = signedIn;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TravelRoute route = routes.get(position);
        Context context = holder.itemView.getContext();

        holder.titleView.setText(route.getTitle());
        holder.summaryView.setText(route.getSummary());
        holder.metaView.setText(context.getString(
                R.string.travelpath_route_card_meta,
                route.getDisplayBudget(),
                route.getDisplayDuration(),
                route.getEnvironmentLabel()));
        holder.stopsView.setText(route.getStopsSummary());
        holder.mapButton.setOnClickListener(v -> onOpenRouteListener.onOpenRoute(route));

        if (showDeleteAction && onDeleteRouteListener != null) {
            holder.saveButton.setVisibility(View.VISIBLE);
            holder.saveButton.setEnabled(true);
            holder.saveButton.setAlpha(1f);
            holder.saveButton.setText(R.string.travelpath_route_delete);
            holder.saveButton.setOnClickListener(v -> onDeleteRouteListener.onDeleteRoute(route));
            return;
        }

        if (!showSaveAction || onSaveRouteListener == null) {
            holder.saveButton.setVisibility(View.GONE);
            return;
        }

        holder.saveButton.setVisibility(View.VISIBLE);
        holder.saveButton.setOnClickListener(v -> onSaveRouteListener.onSaveRoute(route));

        if (route.isSaved()) {
            holder.saveButton.setText(R.string.travelpath_route_saved);
            holder.saveButton.setEnabled(false);
            holder.saveButton.setAlpha(0.7f);
            return;
        }

        holder.saveButton.setEnabled(true);
        holder.saveButton.setAlpha(1f);
        holder.saveButton.setText(signedIn
                ? R.string.travelpath_route_save
                : R.string.travelpath_route_sign_in_to_save);
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleView;
        private final TextView summaryView;
        private final TextView metaView;
        private final TextView stopsView;
        private final Button mapButton;
        private final Button saveButton;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            titleView = itemView.findViewById(R.id.travelpath_route_title);
            summaryView = itemView.findViewById(R.id.travelpath_route_summary);
            metaView = itemView.findViewById(R.id.travelpath_route_meta);
            stopsView = itemView.findViewById(R.id.travelpath_route_stops);
            mapButton = itemView.findViewById(R.id.travelpath_route_map_button);
            saveButton = itemView.findViewById(R.id.travelpath_route_save_button);
        }
    }
}