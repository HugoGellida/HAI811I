package com.example.travelpath;

import com.example.projetprogmobile.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class TravelPathMainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travelpath_main);

        setTitle(R.string.travelpath_home_title);

        tabLayout = findViewById(R.id.travelpath_tabs);
        viewPager = findViewById(R.id.travelpath_view_pager);
        viewPager.setAdapter(new TravelPathPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.travelpath_tab_search);
                return;
            }

            tab.setText(R.string.travelpath_tab_saved_routes);
        }).attach();
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