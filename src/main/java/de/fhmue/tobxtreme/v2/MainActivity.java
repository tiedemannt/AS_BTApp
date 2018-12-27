package de.fhmue.tobxtreme.v2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.BluetoothGattCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
//import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.bluetooth.le.ScanCallback;
import android.view.View;
import android.widget.Toast;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity

    /**
     *  IMPLEMENTS
     */
    implements ConnectionFragment.ConnectionFragmentInterface
{

    /**
     * CONSTANTS
     */
    private static final String TAG = "MainActivity";
    private static final long CONST_SCAN_PERIOD = 5000;  //Dauer für den Scanvorgang
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    /**
     *   OBJECTS
     */
    private Fragment            m_activeFragment;         //Fragment Object
    private BluetoothAdapter    m_btadapter;              //Bluetooth Adapter Object
    private Handler             m_handler;                //Handler Object
    private BluetoothGatt       m_bluetoothGATTObject;    //GATT Object
    private int m_connectionState = STATE_DISCONNECTED;   //Connection State GATT


    /**
     *   OnCreate Function
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bottomnavigationview einrichten:
        //BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        //bottomNav.setOnNavigationItemSelectedListener(navListener);

        //Connection Fragment bei Start öffnen:
        m_activeFragment = new ConnectionFragment();
        getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container, m_activeFragment).commit();

        //Titel der Activity setzen
        setTitle("LUFTGÜTESENSORIK");

        m_btadapter = BluetoothAdapter.getDefaultAdapter();
        if(m_btadapter != null) //Gibt es einen Adapter?
        {
            m_handler = new Handler();
            m_handler.postDelayed(() -> startScan(), 1000);
        }
        else
        {
            Toast.makeText(this, "Bluetoothadapter nicht gefunden!", Toast.LENGTH_LONG).show();
        }


        Log.d(TAG, "MainActivity::onCreate(): finished.");
    }

//    /**
//     * Handler für BottomnavigationView
//     */
//    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
//            new BottomNavigationView.OnNavigationItemSelectedListener()
//            {
//                @Override
//                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
//                    Fragment selectedFragment = null;
//
//                    switch (menuItem.getItemId()) {
//                        case R.id.nav_home:
//                            selectedFragment = new HomeFragment();
//                            Log.d(TAG, "onNavigationItemSelected(): HomeFragment selected.");
//                            break;
//                        case R.id.nav_connection:
//                            selectedFragment = new ConnectionFragment();
//                            Log.d(TAG, "onNavigationItemSelected(): ConnectionFragment selected.");
//                            break;
//                        case R.id.nav_view:
//                            selectedFragment = new ViewFragment();
//                            Log.d(TAG, "onNavigationItemSelected(): ViewFragment selected.");
//                            break;
//                    }
//
//                    m_activeFragment = selectedFragment;
//                    getSupportFragmentManager().beginTransaction().replace(
//                            R.id.fragment_container, selectedFragment).commit();
//
//                    return true; //Item soll selected werden
//                }
//            };


    /**
     *  -Method implemented from Interface ConnectionFragmentInterface-
     *  Start BTLE Scan
     */
    public void startScan()
    {
        Log.d(TAG, "startScan(): called.");

        if(!m_btadapter.isEnabled())
        {
            Log.d(TAG, "startScan(): Enabling Bluetooth...");
            //Toast.makeText(this, "Enabling Bluetooth...", Toast.LENGTH_SHORT).show();

            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTintent);

            IntentFilter btActFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBluetoothEnabledReceiver, btActFilter);

            return;
        }

        if(m_activeFragment instanceof ConnectionFragment)
        {
            ((ConnectionFragment)m_activeFragment).clearDeviceList();

            //Check BT Permissions Manifest
            checkBTPermissions();

            Log.d(TAG, "startScan(): Starting Discovery.");

            //LE Scan Start
            m_btadapter.getBluetoothLeScanner().startScan(mLeScanCallback);
            ((ConnectionFragment)m_activeFragment).setScanActive(true);

            m_handler = new Handler();
            m_handler.postDelayed(()-> stopScan(), CONST_SCAN_PERIOD);

            Toast.makeText(this, "Starting Discovery...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            throw new RuntimeException("Connection Fragment must be active.");
        }
    }

    /**
     * BroadcastReceiver Object for Intent: BluetoothAdapter.ACTION_STATE_CHANGED
     */
    private BroadcastReceiver mBluetoothEnabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive(): ACTION STATE CHANGED.");

            if(action.equals(m_btadapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, m_btadapter.ERROR);

                switch(state)
                {
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive(): STATE_ON.");
                        startScan();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive(): STATE_TURNING_ON.");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive(): STATE_TURNING_OFF.");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive(): STATE_OFF.");
                        break;
                    default:
                        Log.d(TAG, "onReceive(): default?");
                        break;
                }
            }
        }
    };

    /**
     * -Method implemented from Interface ConnectionFragmentInterface-
     * Stop BTLE Scan
     */
    public void stopScan()
    {
        Log.d(TAG, "stopScan(): called.");

        m_btadapter.getBluetoothLeScanner().stopScan(mLeScanCallback);

        if(m_activeFragment instanceof ConnectionFragment) {
            ((ConnectionFragment) m_activeFragment).setScanActive(false);
        }
    }

    /**
     * LE SCAN Callback Object
     */
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST)
            {
                //Match lost
                Log.d(TAG, "onScanResult: lost Device " + result.toString());
                Toast.makeText(MainActivity.this, "Lost: " + result.getDevice().getAddress(), Toast.LENGTH_SHORT).show();
                ((ConnectionFragment)m_activeFragment).removeDevice(new BT_Device(result.getDevice(), result.getRssi()));
            }
            else
            {
                //New Device found?
                if(!((ConnectionFragment)m_activeFragment).getDeviceList().contains(new BT_Device(result.getDevice(), result.getRssi())))
                {
                    Log.d(TAG, "onScanResult: found Device " + result.toString());

                    if((m_activeFragment instanceof ConnectionFragment)){
                        BT_Device newDevice = new BT_Device(result.getDevice(), result.getRssi());
                        ((ConnectionFragment)m_activeFragment).addDevice(newDevice);
                    }
                    else {
                        Log.d(TAG, "onReceive(): ConnectionFragment not active.");
                        m_btadapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    }
                }
                else
                {
                    //Device bereits in Liste
                    Log.d(TAG, "onScanResult: Device bereits in Liste.");
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "onScanFailed: " + errorCode);
            Toast.makeText(getApplicationContext(), "BTLE Scan Failed: " + errorCode, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: ...");
            Toast.makeText(getApplicationContext(), "onBatchScanResults: ..." , Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * GATT Callback Object
     */
    private BluetoothGattCallback mGATTCallback = new BluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED)
            {
                m_connectionState = STATE_CONNECTED;
                //Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "New State: STATE_CONNECTED");

                m_bluetoothGATTObject.discoverServices();

                //Fragment wechseln: Connection -> View
                if(m_activeFragment instanceof ConnectionFragment)
                {
                    Log.i(TAG, "Connected, setting View Fragment");
                    Fragment nextFragment = new ViewFragment();
                    m_activeFragment = nextFragment;
                    getSupportFragmentManager().beginTransaction().replace(
                            R.id.fragment_container, nextFragment).commit();
                }
            }
            else if(newState == BluetoothProfile.STATE_CONNECTING)
            {
                m_connectionState = STATE_CONNECTING;
                Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "New State: STATE_CONNECTING");
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                m_connectionState = STATE_DISCONNECTED;
                Toast.makeText(MainActivity.this, "Disconnected!", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "New State: STATE_DISCONNECTED");

                //Fragment wechseln: Connection
                if(! (m_activeFragment instanceof ConnectionFragment))
                {
                    Log.i(TAG, "Disconnected, setting Connection Fragment");
                    Fragment nextFragment = new ConnectionFragment();
                    m_activeFragment = nextFragment;
                    getSupportFragmentManager().beginTransaction().replace(
                            R.id.fragment_container, nextFragment).commit();

                    m_handler.postDelayed(() -> startScan(), 1000);
                }
            }
            else
            {
                Log.i(TAG, "New State: Unknown");
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "New Service discovered!");
            if(m_activeFragment instanceof ViewFragment)
            {
                if(status == BluetoothGatt.GATT_SUCCESS)
                {
                    ((ViewFragment)m_activeFragment).addService(gatt);
                }
                else
                {
                    Log.i(TAG, "Status != GATT_SUCCESS");
                }
            }
            else
            {
                Log.i(TAG, "ViewFragment is not active!");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged");
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicRead");
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite");
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorRead");
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite");
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.i(TAG, "onMtuChanged");
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.i(TAG, "onPhyRead");
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.i(TAG, "onPhyUpdate");
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(TAG, "onReadRemoteRssi");
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onReliableWriteCompleted");
            super.onReliableWriteCompleted(gatt, status);
        }
    };

    /**
     *  -Method implemented from Interface ConnectionFragmentInterface-
     *  Device selected, connect to Device:
     */
    public void connectToDevice(BT_Device device) {
        Log.i(TAG, "Connecting to Device: " + device.m_device.getAddress());
        m_bluetoothGATTObject = device.m_device.connectGatt(this, false, mGATTCallback);
    }


    /**
     *   Utils
     */
    private void checkBTPermissions()
    {
        Log.d(TAG, "checkBTPermissions(): Called.");
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0)
            {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        }
        else
        {
            Log.d(TAG, "checkBTPermissions(): SDK < Lollipop.");
        }
    }
}
