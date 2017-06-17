package uk.ac.rhul.cyclingprofessor.carmonitor;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;

public class CarCommsFragment extends Fragment {

    private static final String TAG = "CarCommsFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private EditText mDesiredSpeed;
    private EditText mDesiredStopCode;

    private String unParsedInput;  // Data read from car that is incomplete
    private static final String END_MARKER = ":END:";
    private static final int MAX_MSG = 40;
    private Timer statusTimer;

    private TextView mTacho;
    private TextView mRFID;
    private TextView mMagnet;
    private TextView mSpeed;
    private TextView mParams;
    private TextView mEcho;
    private Button mSendButton;
    private ToggleButton mStartStopButton;

    private TurnConfigFragment secondFragment;

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
        unParsedInput = new String("");

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
        setHasOptionsMenu(true);
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
        return inflater.inflate(R.layout.fragment_car_comms, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        //mSendButton = (Button) view.findViewById(R.id.button_send);
        //mDesiredSpeed = (EditText) view.findViewById(R.id.speed);
        //mDesiredStopCode = (EditText) view.findViewById(R.id.stopcode);
        mMagnet = (TextView) view.findViewById(R.id.magnetValue);
        mSpeed = (TextView) view.findViewById(R.id.speedValue);
        //mEcho = (TextView) view.findViewById(R.id.echoValue);
        mRFID = (TextView) view.findViewById(R.id.RFIDtext);
        mTacho = (TextView) view.findViewById(R.id.tachoValue);
        mParams = (TextView) view.findViewById(R.id.params);
        secondFragment = new TurnConfigFragment();
        secondFragment.setSenderRef(this);
        mStartStopButton = (ToggleButton) view.findViewById(R.id.startStopButton);
    }

    private TimerTask getStatus = new TimerTask() {
        public void run() {
            outputMessage("?");
        }
    };

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupComms() {
        Log.d(TAG, "setupComms()");

        // Initialize the send button with a listener that for click events
        /*mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widgets
                String speed = mDesiredSpeed.getText().toString();
                String stopCode = mDesiredStopCode.getText().toString();
                String message = "(" + speed + ":" + stopCode + ")";
                if (outputMessage(message)) {
                    mDesiredSpeed.setBackgroundColor(Color.WHITE);
                };
            }
        });*/
        mStartStopButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    outputMessage(isChecked ? "F" : "S");
            }
        });
        /*mDesiredSpeed.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mDesiredSpeed.setBackgroundColor(Color.rgb(150,235,255));
            }
        });*/

    // Initialize the BluetoothChatService to perform bluetooth connections
        mCommsService = new BluetoothService(getActivity(), mHandler);
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    public boolean outputMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mCommsService.getState() != BluetoothService.STATE_CONNECTED) {
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, getString(R.string.not_connected));
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            return false;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mCommsService.write(send);
        }
        return true;
    }


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

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //@TODO: Fix the crash after a reconnect
                            if (statusTimer == null) {
                                statusTimer = new Timer();
                                statusTimer.schedule(getStatus, 50, 100);
                            }
                            // create configuration panel and attach it to the sliding menu
                            //add the turn configuration panel to the fragment
                            getFragmentManager().beginTransaction()
                                    .add(R.id.settingsConfigurator, secondFragment).commit();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            Log.d(TAG, "Changed state to not connected");
                            if (statusTimer != null) {
                                boolean status = getStatus.cancel();
                                statusTimer.cancel();
                                Log.d(TAG, "Return from StatusTask Cancel:" + status);
                                statusTimer = null;
                            }
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = unParsedInput.concat(new String(readBuf, 0, msg.arg1));
                    parseData(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private void parseData(String fragment) {
        int start = 0;
        int end = fragment.indexOf(END_MARKER);
        while (end >= 0) {
            TextView text = null;
            switch (fragment.charAt(start)) {
                case 'R': //RFID
                    text = mRFID;
                    break;
                case 'M': //Magnetometer
                    text = mMagnet;
                    break;
                case 'T': //Tacho
                    text = mTacho;
                    break;
                case 'S': //Speed
                    text = mSpeed;
                    break;
                case 'P': //Speed
                    Log.i(TAG, "we received parameters" + fragment.substring(start + 1, Math.min(end, start + MAX_MSG)));
                    text = mParams;
                    break;
                //case 'D': //Distance
                //    text = mEcho;
                //    break;
                case '?':
                    break;
            }
            if (text != null) {
                text.setText(fragment.substring(start + 1, Math.min(end, start + MAX_MSG)));
            }
            start = end + END_MARKER.length();
            end = fragment.indexOf(END_MARKER, start);
        }
        unParsedInput = fragment.substring(start);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a comms session
                    setupComms();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mCommsService.connect(device);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }
}
