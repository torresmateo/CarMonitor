package uk.ac.rhul.cyclingprofessor.carmonitor;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ViewAnimator;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "CarMonitorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }

            CarCommsFragment firstFragment = new CarCommsFragment();

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();




        }
    }

    @Override
    protected  void onStart() {
        super.onStart();
        Log.i(TAG, "Ready");
    }
}
