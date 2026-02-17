package com.example.tp2;

import static java.lang.Math.abs;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.tp2.databinding.ActivityMainBinding;

public class FourthActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private SensorManager mSensorManager;
    private Sensor mot;
    private TriggerEventListener triggerEventListener;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourth);

        TextView d = (TextView) findViewById(R.id.direction);

        mSensorManager= (SensorManager) getSystemService(SENSOR_SERVICE);
        mot = mSensorManager.getDefaultSensor(Sensor.TYPE_MOTION_DETECT);
        if (mot == null){
            Toast.makeText(getApplicationContext(), "Sensor unavailable", Toast.LENGTH_LONG).show();
        } else {
            mSensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }

                @Override
                public void onSensorChanged(SensorEvent event) {
                    float x = event.values[0];
                    float y = event.values[1] - 9.81f;

                    String lr = x < -0.01f? "Gauche": x > 0.01f? "Droit": "";
                    String ud = y < -0.01f? "Bas": y > 0.01f? "Haut": "";

                    d.setText(lr + ud);
                }
            }, mot, 2);
        }

        Button butt = (Button) findViewById(R.id.button4);
        butt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FourthActivity.this, FifthActivity.class));
            }
        });
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