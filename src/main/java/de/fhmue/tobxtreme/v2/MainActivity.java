package de.fhmue.tobxtreme.v2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
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
import android.widget.Toast;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity

    /**
     *  IMPLEMENTS
     */
    implements
        ConnectionFragment.ConnectionFragmentInterface,
        ViewFragment.ViewFragmentInterface,
        HomeFragment.HomeFragmentInterface,
        SettingsFragment.SettingsFragmentInterface,
        BT_FSM_DataRead.BT_FSM_DataRead_Interface
{

    /**
     * CONSTANTS
     */
    private static final String TAG = "MainActivity";
    private static final long CONST_SCAN_PERIOD = 7500;  //Dauer für den Scanvorgang
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final UUID DESCRIPTOR_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    /**
     *   OBJECTS
     */
    private Fragment            m_activeFragment;         //Fragment Object
    private BluetoothAdapter    m_btadapter;              //Bluetooth Adapter Object
    private Handler             m_handler;                //Handler Object
    private BluetoothGatt       m_bluetoothGATTObject;    //GATT Object
    private int m_connectionState = STATE_DISCONNECTED;   //Connection State GATT
    private ArrayList<BluetoothGattCharacteristic> m_subscribedCharacteristics; //Subscribed Characteristics

    //Specific für Home Fragment
    private List<BluetoothGattCharacteristic> m_environmentServiceCharacteristics; //Characteristics des Environment Service
    private List<BluetoothGattCharacteristic> m_settingsServiceCharacteristics;    //Characteristics des Settings Service

    //FSM: Data Read from LGS:
    BT_FSM_DataRead m_fsm;


    /**
     *   OnCreate Function
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        m_subscribedCharacteristics = new ArrayList();

        //Init FSM
        m_fsm = new BT_FSM_DataRead(this);

        Log.d(TAG, "MainActivity::onCreate(): finished.");
    }

    @Override
    protected void onDestroy() {
        if(m_connectionState == STATE_CONNECTED)
        {
            m_bluetoothGATTObject.close();
            m_bluetoothGATTObject = null;
        }

        super.onDestroy();
    }


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

        if(m_btadapter != null) m_btadapter.getBluetoothLeScanner().stopScan(mLeScanCallback);

        if(m_activeFragment instanceof ConnectionFragment) {
            ((ConnectionFragment) m_activeFragment).setScanActive(false);
        }
    }
    /**
     * -Method implemented from Interface ViewFragmentInterface-
     * Subscribe/Unsubscribe To Characteristic
     */
    public void subscribeToCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        // Enable notifications for this characteristic locally
        m_bluetoothGATTObject.setCharacteristicNotification(characteristic, true);

        // Write on the config descriptor to be notified when the value changes
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_CONFIG_UUID);
        if(descriptor != null)
        {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            m_bluetoothGATTObject.writeDescriptor(descriptor);
            m_subscribedCharacteristics.add(characteristic);

            Log.i(TAG, "Subscribed to Characteristic: " + characteristic.getUuid().toString());
        }
        else
        {
            Toast.makeText(MainActivity.this,
                    "Cannot subscribe to selected characteristic",
                    Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * -Methods implemented from Interface ViewFragmentInterface-
     * Subscribe/Unsubscribe To Characteristic
     */
    public void unsubscribeAllCharacteristics()
    {
        for(BluetoothGattCharacteristic characteristic : m_subscribedCharacteristics)
        {
            //Disable Local notifications
            m_bluetoothGATTObject.setCharacteristicNotification(characteristic, false);
        }
        m_subscribedCharacteristics.clear();
    }
    /**
     * -Methods implemented from Interface ViewFragmentInterface-
     * disconnects from device
     */
    public void disconnectFromDevice()
    {
        if (m_connectionState == STATE_CONNECTED)
        {
            m_bluetoothGATTObject.close();

            m_connectionState = STATE_DISCONNECTED;
            Log.i(TAG, "New State: STATE_DISCONNECTED");
            Toast.makeText(MainActivity.this, "Disconnected!", Toast.LENGTH_SHORT).show();

            //Fragment wechseln: Connection
            if(! (m_activeFragment instanceof ConnectionFragment))
            {
                Log.i(TAG, "Disconnected, setting Connection Fragment");
                Fragment nextFragment = new ConnectionFragment();
                m_activeFragment = nextFragment;
                getSupportFragmentManager().beginTransaction().replace(
                        R.id.fragment_container, nextFragment).commit();

                m_handler = new Handler();
                m_handler.postDelayed(() -> startScan(), 1000);
            }
        }
    }
    /**
     * -Methods implemented from Interface ViewFragmentInterface-
     * switches to home fragment
     */
    public void switchToHomeFragment(List<BluetoothGattCharacteristic> EnvCharlist,
                                     List<BluetoothGattCharacteristic> SetCharlist,
                                     List<BluetoothGattCharacteristic> fsmCharlist)
    {
        m_environmentServiceCharacteristics = EnvCharlist;
        m_settingsServiceCharacteristics = SetCharlist;

        if(!(m_activeFragment instanceof HomeFragment))
        {
            Log.i(TAG, "LGS Sensor detected, switching to Home Fragment");
            Fragment nextFragment = new HomeFragment();
            m_activeFragment = nextFragment;
            getSupportFragmentManager().beginTransaction().replace(
                    R.id.fragment_container, nextFragment).commit();
        }

        //Subscribe to all characteristics from environment service:
        unsubscribeAllCharacteristics(); //Wegen die Sicherheit von allen anderen Char's unsubscriben

        runOnUiThread(() -> {
            int delay = 200;
            for (BluetoothGattCharacteristic item : m_environmentServiceCharacteristics) {
                m_handler = new Handler();
                m_handler.postDelayed(() -> subscribeToCharacteristic(item), delay);
                delay += 200;
            }
        });

        m_fsm.setLGSConnected(fsmCharlist);
    }

    /**
     * -Methods implemented from Interface HomeFragmentInterface-
     * switches to Settings fragment
     */
    public void switchToSettingsFragment()
    {
        if(!(m_activeFragment instanceof SettingsFragment))
        {
            Log.i(TAG, "Switching to Settings Fragment");
            Fragment nextFragment = new SettingsFragment();
            m_activeFragment = nextFragment;
            getSupportFragmentManager().beginTransaction().replace(
                    R.id.fragment_container, nextFragment).commit();
        }
    }
    public BT_FSM_DataRead getBTFsm()
    {
        return m_fsm;
    }

    /**
     * -Methods implemented from Interface SettingsFragmentInterface-
     */
    //reswitches to Home fragment
    public void reSwitchToHomeFragment()
    {
        if(!(m_activeFragment instanceof HomeFragment))
        {
            Log.i(TAG, "Switching to Home Fragment again");
            Fragment nextFragment = new HomeFragment();
            m_activeFragment = nextFragment;
            getSupportFragmentManager().beginTransaction().replace(
                    R.id.fragment_container, nextFragment).commit();
        }
    }
    //request Daten für alle Settings Characteristics
    public void requestSettingsData()
    {
        runOnUiThread(() -> {
            int delay = 400;
            for(BluetoothGattCharacteristic chara : m_settingsServiceCharacteristics) {
                m_handler = new Handler();
                m_handler.postDelayed(() -> {
                    //Output Active/Inactive nicht requesten
                    if (!(chara.getUuid().toString().equals(ViewFragment.UUID_CHARACTERISTIC_SETTING_OUTPUTACT))) {
                        Log.i(TAG, "ReadRequest for UUID: " + chara.getUuid().toString() + "-----------------------");
                        m_bluetoothGATTObject.readCharacteristic(chara);
                    }
                }, delay);
                delay += 400;
            }
        });
    }
    //request Daten für einen bestimmten Settings Characteristic
    public void requestSettingsDataForCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        Log.i(TAG, "ReadRequest for UUID: " + characteristic.getUuid().toString() + "-----------------------");
        m_bluetoothGATTObject.readCharacteristic(characteristic);
    }
    //Gebe das GATT Object zurück
    public BluetoothGatt getGattObject()
    {
        return m_bluetoothGATTObject;
    }
    //Subscribe auf Notifications zum Outputactive Characteristic
    public void registerOnOutputActiveCharacteristic()
    {
        for(BluetoothGattCharacteristic characteristic : m_settingsServiceCharacteristics) {
            if (characteristic.getUuid().toString().equals(ViewFragment.UUID_CHARACTERISTIC_SETTING_OUTPUTACT)) {
                subscribeToCharacteristic(characteristic);
            }
        }
    }
    /**
     * -Methods implemented from Interface BT_FSM_DataRead_Interface-
     */
    public void readProcessFinished(BT_FSM_DataRead.ReadProcessData readData)
    {
        Log.d(TAG, "readProcessFinished() called!");
        if(m_activeFragment instanceof HomeFragment)
        {
            ((HomeFragment)m_activeFragment).fsmReadProcessFinished(readData);
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
                if(!((ConnectionFragment)m_activeFragment).getDeviceList().contains(
                        new BT_Device(result.getDevice(), result.getRssi())))
                {
                    Log.i(TAG, "onScanResult: found Device " + result.toString());

                    if((m_activeFragment instanceof ConnectionFragment)){
                        BT_Device newDevice = new BT_Device(result.getDevice(), result.getRssi());
                        ((ConnectionFragment)m_activeFragment).addDevice(newDevice);
                    }
                    else {
                        Log.i(TAG, "onReceive(): ConnectionFragment not active.");
                        m_btadapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    }
                }
                else
                {
                    //Device bereits in Liste
                    Log.i(TAG, "onScanResult: Device bereits in Liste.");
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "onScanFailed: " + errorCode);
            Toast.makeText(getApplicationContext(), "BTLE Scan Failed: " + errorCode, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(TAG, "onBatchScanResults: ...");
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
                m_fsm.setLGSDisconnected();

                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                });
                Log.i(TAG, "New State: STATE_CONNECTING");


            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                m_connectionState = STATE_DISCONNECTED;
                m_fsm.setLGSDisconnected();
                Log.i(TAG, "New State: STATE_DISCONNECTED");

                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_SHORT).show();
                });

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
            Log.i(TAG, "Services discovered!");
            if(m_activeFragment instanceof ViewFragment)
            {
                if(status == BluetoothGatt.GATT_SUCCESS)
                {
                    Log.i(TAG, "----------------- Looking for Characteristics in found Services: ------------------");
                    for(int i = 0; i < gatt.getServices().size(); i++) {
                        for (BluetoothGattCharacteristic characteristic : gatt.getServices().get(i).getCharacteristics()) {
                            Log.i(TAG, "Characteristic in Service " + i + " found: " + characteristic.toString());
                        }
                    }
                    Log.i(TAG, "----------------- ALL CHARACTERISTICS SHOWN ABOVE ------------------");

                    if(m_activeFragment instanceof ViewFragment)
                        ((ViewFragment)m_activeFragment).addService(gatt);

                    //m_bluetoothGATTObject.setCharacteristicNotification(characteristic, true);
                    //Log.i(TAG, "Subscribed to Characteristic: " + characteristic.toString());

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

        /**
         * Once notifications are enabled for a characteristic, an onCharacteristicChanged() callback
         * is triggered if the characteristic changes on the remote device:
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //Log.i(TAG, "onCharacteristicChanged: " + characteristic.toString());

            m_fsm.handleNotify(characteristic);
            if(m_activeFragment instanceof ViewFragment)
            {
                ((ViewFragment)m_activeFragment).displayCharacteristicValue(characteristic);
            }
            else if(m_activeFragment instanceof HomeFragment)
            {
                ((HomeFragment)m_activeFragment).displayCharacteristicValue(characteristic);
            }
            else if(m_activeFragment instanceof SettingsFragment)
            {
                ((SettingsFragment)m_activeFragment).handleCharacteristicUpdate(characteristic);
            }
            else
            {
                Log.e(TAG, "onCharacteristicChanged() - Es ist ein Fragment aktiv, das nicht aktiv sein sollte!");
            }
        }

        // Callback; Wird aufgerufen wenn eine Antwort auf einen ReadRequest empfangen wurde
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            m_fsm.handleReadResponse(characteristic, status);
            if(m_activeFragment instanceof SettingsFragment)
            {
                ((SettingsFragment)m_activeFragment).handleReadRequestAnswer(characteristic);
            }
        }

        // Callback; Wird aufgerufen wenn eine Antwort auf einen Writerequest empfangen wurde
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            m_fsm.handleWriteResponse(characteristic, status);
            if(m_activeFragment instanceof SettingsFragment)
            {
                ((SettingsFragment)m_activeFragment).handleWriteRequestAnswer(characteristic, status);
            }
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
