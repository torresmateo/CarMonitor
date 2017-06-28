package uk.ac.rhul.cyclingprofessor.carmonitor;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

public class TurnConfigFragment extends Fragment {

    private static final String TAG = "TurnConfigFragment";

    public static final String PREFS_NAME = "MyPrefsFile";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private SeekBar mSpeedStep1;
    private SeekBar mSpeedStep2;
    private SeekBar mAngleStep1;
    private SeekBar mAngleStep2;
    private SeekBar mGlobalSpeed;

    private TextView speedStep1;
    private TextView angleStep1;
    private TextView speedStep2;
    private TextView angleStep2;
    private TextView globalSpeed;

    private int vSpeedStep1;
    private int vAngleStep1;
    private int vSpeedStep2;
    private int vAngleStep2;
    private int vGlobalSpeed;


    private CarCommsFragment senderRef = null;


    int speedStep = 1;
    int speedMax = 50;
    int speedMin = 20;
    int angleStep = 1;
    int angleMax = 105;
    int angleMin = 10;


    private Button mSetConfigButton;
    private Button mSaveConfigButton;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mCommsService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

        // Restore preferences
        SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
        vSpeedStep1 = settings.getInt("speed-step1", speedMin);
        vAngleStep1 = settings.getInt("angle-step1", angleMin);
        vSpeedStep2 = settings.getInt("angle-step2", speedMin);;
        vAngleStep2 = settings.getInt("angle-step2", angleMin);;
        vGlobalSpeed = settings.getInt("speed-global", speedMin);;

    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupComms() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the session
        } else if (mCommsService == null) {
            setupComms();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCommsService != null) {
            mCommsService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mCommsService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mCommsService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mCommsService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_configuration, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mSpeedStep1 = (SeekBar) view.findViewById(R.id.speedStep1);
        mSpeedStep2 = (SeekBar) view.findViewById(R.id.speedStep2);
        mAngleStep1 = (SeekBar) view.findViewById(R.id.angleStep1);
        mAngleStep2 = (SeekBar) view.findViewById(R.id.angleStep2);
        mGlobalSpeed = (SeekBar) view.findViewById(R.id.globalSpeed);

        speedStep1 = (TextView) view.findViewById(R.id.textSpeed1);
        angleStep1 = (TextView) view.findViewById(R.id.textAngle1);
        speedStep2 = (TextView) view.findViewById(R.id.textSpeed2);
        angleStep2 = (TextView) view.findViewById(R.id.textAngle2);
        globalSpeed = (TextView) view.findViewById(R.id.textGlobalSpeed);


        //values for speed
        mSpeedStep1.setMax((speedMax - speedMin)/ speedStep);
        mSpeedStep2.setMax((speedMax - speedMin)/ speedStep);
        mGlobalSpeed.setMax((speedMax - speedMin)/ speedStep);
        //values for angles
        mAngleStep1.setMax((angleMax - angleMin)/angleStep);
        mAngleStep2.setMax((angleMax - angleMin)/angleStep);

        //set progresses with the default values
        mSpeedStep1.setProgress((vSpeedStep1 - speedMin)/speedStep);
        mSpeedStep1.setProgress((vSpeedStep2 - speedMin)/speedStep);
        mAngleStep1.setProgress((vAngleStep1 - angleMin)/angleStep);
        mAngleStep2.setProgress((vAngleStep2 - angleMin)/angleStep);
        mGlobalSpeed.setProgress((vGlobalSpeed - speedMin)/speedStep);

        mSetConfigButton = (Button) view.findViewById(R.id.setConfigButton);
        mSaveConfigButton = (Button) view.findViewById(R.id.saveConfigButton);
    }



    /**
     * Set up the UI and background operations for chat.
     */
    private void setupComms() {
        Log.d(TAG, "setupComms()");

        // Initialize the send button with a listener that for click events

        mSpeedStep1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vSpeedStep1 = speedMin + (i * speedStep);
                speedStep1.setText(String.valueOf(vSpeedStep1));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        mAngleStep1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vAngleStep1 = angleMin + (i * angleStep);
                angleStep1.setText(String.valueOf(vAngleStep1));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mSpeedStep2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vSpeedStep2 = speedMin + (i * speedStep);
                speedStep2.setText(String.valueOf(vSpeedStep2));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        mAngleStep2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vAngleStep2 = angleMin + (i * angleStep);
                angleStep2.setText(String.valueOf(vAngleStep2));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mGlobalSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vGlobalSpeed = speedMin + (i * speedStep);
                globalSpeed.setText(String.valueOf(vGlobalSpeed));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        mSetConfigButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widgets
                String speed1 = String.valueOf(vSpeedStep1);
                String angle1 = String.valueOf(vAngleStep1);
                String speed2 = String.valueOf(vSpeedStep2);
                String angle2 = String.valueOf(vAngleStep2);
                String globalSpeed = String.valueOf(vGlobalSpeed);
                String message = "(" + speed1 + ":" + angle1+":" + speed2+":" + angle2+":" + globalSpeed+ ")";
                Log.i(TAG,"sent message: " + message);
                senderRef.outputMessage(message);
                setStatus(R.string.settingsFeedbackSet);
            }
        });
        mSetConfigButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // We need an Editor object to make preference changes.
                // All objects are from android.context.Context
                SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("speed-step1", vSpeedStep1);
                editor.putInt("angle-step1", vAngleStep1);
                editor.putInt("angle-step2", vSpeedStep2);;
                editor.putInt("angle-step2", vAngleStep2);;
                editor.putInt("speed-global", vGlobalSpeed);;


                // Commit the edits!
                editor.commit();

                setStatus(R.string.settingsFeedbackSave);
            }
        });

    }

    //@TODO: this was copy-pasted from CarCommsFragment, Since I can't test the app because I have an apple phone, this is probably not working
    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    public void setSenderRef(CarCommsFragment senderRef) {
        this.senderRef = senderRef;
    }



}
