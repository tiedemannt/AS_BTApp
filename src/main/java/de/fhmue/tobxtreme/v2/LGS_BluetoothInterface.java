package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Diese Klasse stellt alle relevanten Bluetooth-Funktionen
 * für die LGS-App zur Verfügung.
 */
public class LGS_BluetoothInterface {

    /**
     * Konstanten:
     */
    private static final String TAG = "LGS_BluetoothInterface";

    /**
     * Interface:
     */
    interface Interface{

        void sensorFound(boolean isFound); //Callback nach Suchprozess
        void sensorConnected(boolean isConnected); //Callback nach Verbindungsprozess
        void setDisconnected(); //Verbindung abgebrochen Callback
        void servicesFound(boolean isFound);

        void handleWriteResponse(BluetoothGattCharacteristic characteristic, int status);
        void handleReadResponse(BluetoothGattCharacteristic characteristic, int status);
        void handleNotify(BluetoothGattCharacteristic characteristic);


        void makeToast(String text);
        void checkPermissions();
        void startActivity(Intent intent);
        Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter);
    }
    private Interface m_interface;


    /**
     * Objects
     */
    //Das App Context
    private Context m_appContext;

    //Der Bluetooth-Adapter:
    private BluetoothAdapter m_btAdapter;

    //Das Bluetooth-Device Objekt:
    private BluetoothDevice m_btDevice;

    //Das Bluetooth-Gatt Objekt:
    private BluetoothGatt m_btGatt;

    //Liste von Characteristics, für die Notifications erhalten werden:
    private ArrayList<BluetoothGattCharacteristic> m_subscribedCharacteristics;

    //Liste von Characteristics, die bereits requested wurden:
    private ArrayList<BluetoothGattCharacteristic> m_requestedCharacteristics;

    //Timer zum subscriben auf Characteristics
    private static final long TIMER_SUBSCRIBE_DELAY = 200;
    private static final long TIMER_REQUEST_SETTINGS_DELAY = 400;
    private Timer m_timer;


    /**
     * Konstruktor
     */
    public LGS_BluetoothInterface(LGS_BluetoothFSM fsm, Context appContext)
    {
        m_appContext                    = appContext;
        m_interface                     = (Interface)fsm;
        m_btAdapter                     = BluetoothAdapter.getDefaultAdapter();
        m_subscribedCharacteristics     = new ArrayList<>();
        m_requestedCharacteristics      = new ArrayList<>();
    }


    //Bluetooth auf Device verfügbar?
    public boolean isBluetoothAvailable(){
        if(m_btAdapter == null) {
            return false;
        }
        else {
            return true;
        }
    }
    //Starte den Scanvorgang
    public void startScanning(){

        //Bluetooth aktiviert?
        if(!m_btAdapter.isEnabled())
        {
            Log.d(TAG, "startScan(): Enabling Bluetooth...");

            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            m_interface.startActivity(enableBTintent);

            IntentFilter btActFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            m_interface.registerReceiver(mBluetoothEnabledReceiver, btActFilter);

            return;
        }

        //Check BT Permissions Manifest
        checkBTPermissions();

        //LE Scan Start
        m_btAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
    }
    public void stopScanning(){
        if(m_btAdapter != null) m_btAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
    }
    //Starte den Verbindungsvorgang
    public void connectSensor(){
        m_btGatt = m_btDevice.connectGatt(m_appContext, false, mGATTCallback);
    }
    public void discoverServices(){
        m_btGatt.discoverServices();
    }
    public void requestCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        m_btGatt.readCharacteristic(characteristic);
    }
    public void writeRepRate(int value)
    {
        BluetoothGattCharacteristic characteristic =
                m_btGatt
                .getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_REPRATE);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        m_btGatt.writeCharacteristic(characteristic);
    }
    public void writeCriticTemperature(int value)
    {
        BluetoothGattCharacteristic characteristic =
                m_btGatt
                        .getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITTEMP);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
        m_btGatt.writeCharacteristic(characteristic);
    }
    public void writeCriticPressure(int value)
    {
        BluetoothGattCharacteristic characteristic =
                m_btGatt
                        .getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITPRES);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        m_btGatt.writeCharacteristic(characteristic);
    }
    public void writeCriticHumidity(int value)
    {
        BluetoothGattCharacteristic characteristic =
                m_btGatt
                        .getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITHUM);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        m_btGatt.writeCharacteristic(characteristic);
    }
    public void writeCriticCO2(int value)
    {
        BluetoothGattCharacteristic characteristic =
                m_btGatt
                        .getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITCO2);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        m_btGatt.writeCharacteristic(characteristic);
    }
    public void writeCriticVOC(int value)
    {
        BluetoothGattCharacteristic characteristic =
                m_btGatt
                        .getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITVOC);
        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        m_btGatt.writeCharacteristic(characteristic);
    }
    public void readPaketCharacteristic()
    {
        BluetoothGattCharacteristic characteristic =
                m_btGatt
                        .getService(LGS_Constants.UUID_SERVICE_FSM)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_READPACKAGE);
        m_btGatt.readCharacteristic(characteristic);
    }
    public void sendProcessRequestCharacteristic(int requestType)
    {
        /**
         * Tabelle:
         * - 0: Prozess abgeschlossen
         * - 1: Temperatur lesen
         * - 2: Feuchte lesen
         * - 3: Druck lesen
         * - 4: Co2 lesen
         * - 5: Voc lesen
         */
        BluetoothGattCharacteristic m_fsmRequestCharacteristic =
                m_btGatt
                        .getService(LGS_Constants.UUID_SERVICE_FSM)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_REQUEST);
        m_fsmRequestCharacteristic.setValue(requestType, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        m_btGatt.writeCharacteristic(m_fsmRequestCharacteristic);
    }

    //Subscripe auf alle relevanten Characteristics
    public void subscribeOnCharacteristics(){

        m_subscribedCharacteristics = new ArrayList<>();

        m_timer = new Timer();
        m_timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_FSM)
                        .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_NOTIFYREADY)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_FSM)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_NOTIFYREADY));
                }
                else if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_BRIGHT)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_BRIGHT));
                }
                else if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_TEMPERATURE)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_TEMPERATURE));
                }
                else if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_VOC)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_VOC));
                }
                else if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_CO2)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_CO2));
                }
                else if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_HUMIDITY)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_HUMIDITY));
                }
                else if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_PRESSURE)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_ENVIRONMENT)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_PRESSURE));
                }
                else if(!m_subscribedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_OUTPUTACT)))
                {
                    subscribeToCharacteristic(
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_OUTPUTACT));
                }
                else
                {
                    Log.i(TAG, "subscribeOnCharacteristics(): Success!");
                    m_timer.cancel();
                }
            }
        }, 0, TIMER_SUBSCRIBE_DELAY);
    }
    public void requestAllSettings()
    {
        m_requestedCharacteristics = new ArrayList<>();

        m_timer = new Timer();
        m_timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!m_requestedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_REPRATE)))
                {
                    BluetoothGattCharacteristic characteristic =
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                            .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_REPRATE);
                    m_btGatt.readCharacteristic(characteristic);
                    m_requestedCharacteristics.add(characteristic);
                }
                else if(!m_requestedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITTEMP)))
                {
                    BluetoothGattCharacteristic characteristic =
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITTEMP);
                    m_btGatt.readCharacteristic(characteristic);
                    m_requestedCharacteristics.add(characteristic);
                }
                else if(!m_requestedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITPRES)))
                {
                    BluetoothGattCharacteristic characteristic =
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITPRES);
                    m_btGatt.readCharacteristic(characteristic);
                    m_requestedCharacteristics.add(characteristic);
                }
                else if(!m_requestedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITCO2)))
                {
                    BluetoothGattCharacteristic characteristic =
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITCO2);
                    m_btGatt.readCharacteristic(characteristic);
                    m_requestedCharacteristics.add(characteristic);
                }
                else if(!m_requestedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITHUM)))
                {
                    BluetoothGattCharacteristic characteristic =
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITHUM);
                    m_btGatt.readCharacteristic(characteristic);
                    m_requestedCharacteristics.add(characteristic);
                }
                else if(!m_requestedCharacteristics.contains(
                        m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITVOC)))
                {
                    BluetoothGattCharacteristic characteristic =
                            m_btGatt.getService(LGS_Constants.UUID_SERVICE_CONFIGURATION)
                                    .getCharacteristic(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITVOC);
                    m_btGatt.readCharacteristic(characteristic);
                    m_requestedCharacteristics.add(characteristic);
                }
                else
                {
                    Log.i(TAG, "requestAllSettings(): Success!");
                    m_timer.cancel();
                }
            }
        }, 0, TIMER_REQUEST_SETTINGS_DELAY);
    }
    //Trennt die Verbindung
    public void disconnectDevice(){
        m_btGatt.disconnect();
    }
    //Subscribe einem Characteristic
    public void subscribeToCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        // Enable notifications for this characteristic locally
        m_btGatt.setCharacteristicNotification(characteristic, true);

        // Write on the config descriptor to be notified when the value changes
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(LGS_Constants.UUID_DESCRIPTOR_CONFIG);
        if(descriptor != null)
        {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            m_btGatt.writeDescriptor(descriptor);
            m_subscribedCharacteristics.add(characteristic);

            Log.i(TAG, "Subscribed to Characteristic: " + characteristic.getUuid().toString());
        }
        else
        {
            m_interface.makeToast("Cannot subscribe to characteristic!");
        }
    }



    /**
     * LE SCAN Callback Object
     */
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(callbackType != ScanSettings.CALLBACK_TYPE_MATCH_LOST)
            {
                BluetoothDevice foundDevice = result.getDevice();

                if((foundDevice.getName() != null)
                        && (foundDevice.getName().equals(LGS_Constants.LGS_DEVICENAME)))
                {
                    Log.i(TAG, "onScanResult: Sensor gefunden!");
                    m_btDevice = result.getDevice();
                    m_interface.sensorFound(true);
                }
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "onScanFailed: " + errorCode);
            m_interface.makeToast("ScanCallback: Scan failed!");
            Log.e(TAG, "ScanCallback: onScanFailed!");
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.e(TAG, "ScanCallback: onBatchScanResults!");
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
                Log.i(TAG, "Verbindungsstatus GATT: STATE_CONNECTED");
                m_interface.sensorConnected(true);
            }
            else if(newState == BluetoothProfile.STATE_CONNECTING)
            {
                Log.i(TAG, "Verbindungsstatus GATT: STATE_CONNECTING");
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.i(TAG, "Verbindungsstatus GATT: STATE_DISCONNECTED");
                m_interface.setDisconnected();
            }
            else
            {
                Log.i(TAG, "Verbindungsstatus GATT: Unknown");
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                m_interface.servicesFound(true);
            }
            else
            {
                //Nicht Success
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            m_interface.handleNotify(characteristic);
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            m_interface.handleReadResponse(characteristic, status);
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            m_interface.handleWriteResponse(characteristic, status);
        }
    };
    /**
     * BroadcastReceiver Object for Intent: BluetoothAdapter.ACTION_STATE_CHANGED
     */
    private BroadcastReceiver mBluetoothEnabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive(): ACTION STATE CHANGED.");

            if(action.equals(m_btAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, m_btAdapter.ERROR);
                switch(state)
                {
                    case BluetoothAdapter.STATE_ON:
                        Log.i(TAG, "onReceive(): STATE_ON.");
                        startScanning();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i(TAG, "onReceive(): STATE_TURNING_ON.");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(TAG, "onReceive(): STATE_TURNING_OFF.");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(TAG, "onReceive(): STATE_OFF.");
                        break;
                    default:
                        Log.i(TAG, "onReceive(): default?");
                        break;
                }
            }
        }
    };

    /**
     * Prüft, ob die entsprechenden Berechtigungen vorhanden sind
     */
    private void checkBTPermissions()
    {
        Log.i(TAG, "checkBTPermissions()");
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            m_interface.checkPermissions();
        }
        else
        {
            Log.i(TAG, "checkBTPermissions(): SDK < Lollipop.");
        }
    }
}
