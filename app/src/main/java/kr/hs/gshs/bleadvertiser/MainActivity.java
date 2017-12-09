package kr.hs.gshs.bleadvertiser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

import kr.hs.gshs.blebeaconprotocollibrary.PacketTypes;
import kr.hs.gshs.blebeaconprotocollibrary.Struct;
import kr.hs.gshs.blebeaconprotocollibrary.StructTypes;

/**
 * Ensure the device supports Bluetooth.
 * Allows user to start & stop Bluetooth LE Advertising of their device.
 */
public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 1;

    private Spinner spinnerPacketType;
    private Spinner spinnerStructType;
    private EditText editTextStructData;
    private Button buttonAddStruct;
    private ListView listViewStruct;

    private StructAdapter structAdapter;

    private int selectedPacketTypeOrdinal, selectedStructTypeOrdinal;

    /**
     * Lets user toggle BLE Advertising.
     */
    private Switch mSwitch;

    private BluetoothAdapter mBluetoothAdapter;

    /**
     * Listens for notifications that the {@code AdvertiserService} has failed to start advertising.
     * This Receiver deals with Activity UI elements and only needs to be active when the Activity
     * is on-screen, so it's defined and registered in code instead of the Manifest.
     */
    private BroadcastReceiver advertisingFailureReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            checkBluetooth();
        }

        setupSpinnerPacketType();

        setupStructEditor();

        // sets up advertisement switch
        mSwitch = (Switch) findViewById(R.id.switchAdvertisement);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startAdvertising();
                } else {
                    stopAdvertising();
                }
            }
        });

        receiveAdvertisingFailure();
    }

    private void checkBluetooth() {
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        // Is Bluetooth supported on this device?
        if (mBluetoothAdapter != null) {

            // Is Bluetooth turned on?
            if (mBluetoothAdapter.isEnabled()) {

                // Are Bluetooth Advertisements supported on this device?
                if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {

                    // Bluetooth Advertisements are not supported.
                    Toast.makeText(this, "Bluetooth Advertisements are not supported on this device.", Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {

                // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {

            // Bluetooth is not supported.
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // continued from checkBluetooth()
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {

                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on this device?
                    if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {

                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(this, "Bluetooth Advertisements are not supported on this device.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {

                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, "User declined to enable Bluetooth, exiting Bluetooth Advertisements.", Toast.LENGTH_SHORT).show();
                    finish();
                }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setupSpinnerPacketType() {
        // packet type spinner
        spinnerPacketType = findViewById(R.id.spinnerPacketType);

        PacketTypes[] packetTypes = PacketTypes.getValues();
        String[] packetTypeNames = new String[packetTypes.length];
        for (int i=0; i<packetTypes.length; ++i)
            packetTypeNames[i] = packetTypes[i].displayName();

        ArrayAdapter<String> adapterPacketType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, packetTypeNames);
        adapterPacketType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerPacketType.setAdapter(adapterPacketType);

        spinnerPacketType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPacketTypeOrdinal = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupStructEditor() {
        // struct list view
        listViewStruct = findViewById(R.id.listViewStruct);

        structAdapter = new StructAdapter(this, getLayoutInflater());

        listViewStruct.setAdapter(structAdapter);

        listViewStruct.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                structAdapter.removeItem(position);
                structAdapter.notifyDataSetChanged();
            }
        });

        // struct type spinner
        spinnerStructType = findViewById(R.id.spinnerStructType);

        StructTypes[] structTypes = StructTypes.getValues();
        String[] structTypeNames = new String[structTypes.length];
        for (int i=0; i<structTypes.length; ++i)
            structTypeNames[i] = structTypes[i].displayName();

        ArrayAdapter<String> adapterStructType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, structTypeNames);
        adapterStructType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerStructType.setAdapter(adapterStructType);

        spinnerStructType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStructTypeOrdinal = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // struct data edit text and & struct button
        editTextStructData = findViewById(R.id.editTextStructData);
        buttonAddStruct = findViewById(R.id.buttonStructAdd);

        buttonAddStruct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredStructData = editTextStructData.getText().toString();

                structAdapter.addItem(new Struct(StructTypes.fromOrdinal(selectedStructTypeOrdinal), enteredStructData));
                structAdapter.notifyDataSetChanged();

                editTextStructData.selectAll();
            }
        });
    }

    private void receiveAdvertisingFailure() {
        advertisingFailureReceiver = new BroadcastReceiver() {

            /**
             * Receives Advertising error codes from {@code AdvertiserService} and displays error messages
             * to the user. Sets the advertising toggle to 'false.'
             */
            @Override
            public void onReceive(Context context, Intent intent) {

                int errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);

                mSwitch.setChecked(false);

                String errorMessage = "Start advertising failed: ";
                switch (errorCode) {
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage += "already started.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage += "data packet exceeded 31 byte limit.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += "not supported on this device.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage += "internal error.";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage += "too many advertisers.";
                        break;
                    case AdvertiserService.ADVERTISING_TIMED_OUT:
                        errorMessage = "Advertising stopped due to timeout.";
                        break;
                    default:
                        errorMessage += "unknown error";
                }

                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }

    /**
     * When app comes on screen, check if BLE Advertisements are running, set switch accordingly,
     * and register the Receiver to be notified if Advertising fails.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (AdvertiserService.running) {
            mSwitch.setChecked(true);
        } else {
            mSwitch.setChecked(false);
        }

        IntentFilter failureFilter = new IntentFilter(AdvertiserService.ADVERTISING_FAILED);
        registerReceiver(advertisingFailureReceiver, failureFilter);
    }

    /**
     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
     * (and because the app doesn't care if Advertising fails while the UI isn't active)
     */
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(advertisingFailureReceiver);
    }

    /**
     * Starts BLE Advertising by starting {@code AdvertiserService}.
     */
    private void startAdvertising() {
        Intent intent = new Intent(this, AdvertiserService.class);

        intent.putExtra("packetType", selectedPacketTypeOrdinal);

        ArrayList<Struct> structs = new ArrayList<>();
        for (int i=0; i<structAdapter.getCount(); ++i)
            structs.add((Struct) structAdapter.getItem(i));
        intent.putParcelableArrayListExtra("structs", structs);

        startService(intent);
    }

    /**
     * Stops BLE Advertising by stopping {@code AdvertiserService}.
     */
    private void stopAdvertising() {
        Intent intent = new Intent(this, AdvertiserService.class);
        stopService(intent);
        mSwitch.setChecked(false);
    }
}
