package com.example.tp2;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.tp2.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private SensorManager mSensorManager;
    TextView mSensorsTot,mSensorAvailables;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get the texts fields of the layout and setup to invisible
        mSensorsTot   = (TextView) findViewById(R.id.sensoritot);
        mSensorAvailables  = (TextView) findViewById(R.id.sensoridisponibili);

        // Get the SensorManager
        mSensorManager= (SensorManager) getSystemService(SENSOR_SERVICE);

        // List of Sensors Available
        List<Sensor> msensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        // Print how may Sensors are there
        mSensorsTot.setText(msensorList.size()+" "+this.getString(R.string.sensors)+"!");

        // Print each Sensor available using sSensList as the String to be printed
        String sSensList = new String("");
        Sensor tmp;
        int x,i;
        for (i=0;i<msensorList.size();i++){
            tmp = msensorList.get(i);
            sSensList = " "+sSensList+tmp.getName(); // Add the sensor name to the string of sensors available
        }
        // if there are sensors available show the list
        if (i>0){
            sSensList = getString(R.string.sensors)+":"+sSensList;
            mSensorAvailables.setText(sSensList);
        }
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