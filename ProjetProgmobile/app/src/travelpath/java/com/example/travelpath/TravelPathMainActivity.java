package com.example.travelpath;

import com.example.projetprogmobile.FeatureNavigation;
import com.example.projetprogmobile.ProfileAvatarUtils;
import com.example.projetprogmobile.ProfileActivity;
import com.example.projetprogmobile.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TravelPathMainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ImageButton profileButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travelpath_main);

        setTitle(R.string.travelpath_home_title);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tabLayout = findViewById(R.id.travelpath_tabs);
        viewPager = findViewById(R.id.travelpath_view_pager);
        profileButton = findViewById(R.id.travelpath_profile_button);
        viewPager.setAdapter(new TravelPathPagerAdapter(this));

        profileButton.setOnClickListener(v -> startActivity(
                ProfileActivity.createIntent(this, FeatureNavigation.DESTINATION_TRAVEL_PATH)));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.travelpath_tab_search);
                return;
            }

            tab.setText(R.string.travelpath_tab_saved_routes);
        }).attach();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isAuthenticated = auth.getCurrentUser() != null;
        profileButton.setVisibility(isAuthenticated ? View.VISIBLE : View.GONE);
        if (!isAuthenticated || auth.getUid() == null) {
            ProfileAvatarUtils.applyAvatar(profileButton, null, 8);
            return;
        }

        db.collection("users")
                .document(auth.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> ProfileAvatarUtils.applyAvatar(
                        profileButton,
                        documentSnapshot.getString("avatarBase64"),
                        8))
                .addOnFailureListener(error -> ProfileAvatarUtils.applyAvatar(profileButton, null, 8));
    }

    private static final class TravelPathPagerAdapter extends FragmentStateAdapter {

        private TravelPathPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new TravelPathSearchFragment();
            }

            return new SavedRoutesFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}