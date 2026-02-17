package com.example.tp1;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.text.InputType;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.tp1.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ***************************************

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        setSupportActionBar(binding.toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });

        // ***************************************
        /*
        ConstraintLayout layout = new ConstraintLayout(this);
        layout.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        ));

        EditText name = new EditText(this);
        name.setId(View.generateViewId());
        name.setHint("Enter your name");

        EditText first_name = new EditText(this);
        first_name.setId(View.generateViewId());
        first_name.setHint("Enter your first name");

        EditText age = new EditText(this);
        age.setId(View.generateViewId());
        age.setHint("XX");
        age.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText domain = new EditText(this);
        domain.setId(View.generateViewId());
        domain.setHint("Enter your assets");



        Button button = new Button(this);
        button.setId(View.generateViewId());
        button.setText("Submit");

        layout.addView(name);
        layout.addView(first_name);
        layout.addView(age);
        layout.addView(domain);
        layout.addView(button);

        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        setContentView(layout);

        set.connect(name.getId(), ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP, 100);
        set.connect(name.getId(), ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START, 32);
        set.connect(name.getId(), ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END, 32);
        set.constrainWidth(name.getId(), ConstraintSet.MATCH_CONSTRAINT);

        set.connect(first_name.getId(), ConstraintSet.TOP,
                name.getId(), ConstraintSet.BOTTOM, 32);
        set.connect(first_name.getId(), ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START);
        set.connect(first_name.getId(), ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END);

        set.connect(age.getId(), ConstraintSet.TOP,
                first_name.getId(), ConstraintSet.BOTTOM, 32);
        set.connect(age.getId(), ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START);
        set.connect(age.getId(), ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END);

        set.connect(domain.getId(), ConstraintSet.TOP,
                age.getId(), ConstraintSet.BOTTOM, 32);
        set.connect(domain.getId(), ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START);
        set.connect(domain.getId(), ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END);

        // Button constraints
        set.connect(button.getId(), ConstraintSet.TOP,
                domain.getId(), ConstraintSet.BOTTOM, 32);
        set.connect(button.getId(), ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START);
        set.connect(button.getId(), ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END);

        set.applyTo(layout);

         */
        // ***************************************

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}