package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;



public class LGS_BluetoothFSM

    implements LGS_BluetoothInterface.Interface

{
    //Routing
    public void makeToast(String text){m_interface.makeToast(text);}
    public void checkPermissions(){m_interface.checkPermissions();}
    public void startActivity(Intent intent){m_interface.startActivity(intent);};
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter)
        {return m_interface.registerReceiver(receiver, filter);};


    /**
     * Constants
     * --------------------------------------------------------------------------
     */
    private static final String TAG = "BT_FSM_DataRead";
    //---------------------------------------------------------------------------

    /**
     * Enums
     * --------------------------------------------------------------------------
     */
    private enum FSM_STATE {
        STATE_UNKNOWN,                      //Unbekannter State
        STATE_NOT_CONNECTED,                //LGS nicht verbunden
        STATE_SEARCHING,                    //Suche nach LGS Device
        STATE_CONNECTING,                   //Verbinde mit LGS Sensor
        STATE_CONNECTED,                    //LGS Device verbunden
        STATE_DISCOVER_SERVICES,            //Scanne Device nach Services
        STATE_CONNECTED_READY,              //Verbunden und bereit
        //---------------------
        STATE_REQUEST_READ_DATA,            //Start-State: Sende Read Request
        STATE_WAITING_FOR_READDATA_READY,   //Warte auf Antwort von Device
        STATE_REQUEST_DATAPACKAGE,          //Sende Readrequest für Datenpaket
        STATE_WAITING_FOR_DATAPACKAGE,      //Warte auf Antwort/Datenpaket
        STATE_READDATA_COMPLETED,           //Vorgang abgeschlossen
        //---------------------
        CANCEL_PROCESS                      //Vorgang aufgrund von z.B. Timeout abgebrochen
    }

    public enum FSM_SENSORPROPERTY {
        SENSORPROPERTY_NONE,
        SENSORPROPERTY_TEMPERATURE,
        SENSORPROPERTY_PRESSURE,
        SENSORPROPERTY_CO2,
        SENSORPROPERTY_VOC,
        SENSORPROPERTY_HUMIDITY
    }
    //---------------------------------------------------------------------------

    /**
     * Interfaces
     * --------------------------------------------------------------------------
     */
    public interface Interface {
        void readProcessFinished(ReadProcessData readData);
        void makeToast(String text);
        void scanProcessFinished(boolean isSensorFound);
        void connectProcessFinished(boolean isSensorConnected);
        void startActivity(Intent intent);
        Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter);
        void servicesFound(boolean isFound);

        void forwardNotification(BluetoothGattCharacteristic chara);
        void forwardReadResponse(BluetoothGattCharacteristic chara, int status);
        void forwardWriteResponse(BluetoothGattCharacteristic chara, int status);

        void checkPermissions();
    }
    Interface m_interface;

    public class ReadProcessData {
        public FSM_SENSORPROPERTY c_dataType;
        public ArrayList<DataPoint> c_dataPoints;

        public ReadProcessData(FSM_SENSORPROPERTY propertyType) {
            c_dataType = propertyType;
            c_dataPoints = new ArrayList<>();
        }

        public void addDataPoint(DataPoint newPoint) {
            c_dataPoints.add(newPoint);
        }
    }
    //---------------------------------------------------------------------------


    /**
     * Daten:
     * --------------------------------------------------------------------------
     */
    private FSM_STATE m_fsmState;   //Aktueller State
    private FSM_STATE m_newState;   //Der nächste State

    //Das zu lesende Property:
    private FSM_SENSORPROPERTY m_readProperty = FSM_SENSORPROPERTY.SENSORPROPERTY_NONE;

    //Das empfangene Paket:
    private BluetoothGattCharacteristic m_receivedPaket = null;

    //Die noch zu lesenden Pakete:
    private int m_remainingPaketsToRead = 0;
    private int m_dataCountPerPaket = 0;
    private ReadProcessData m_readProcessData;

    //Control Flags:
    private boolean m_isNotifyReceived = false;         //Flag: Ist Notify empfangen worden?
    private boolean m_isReadResponseReceived = false;   //Flag: Readresponse emfpangen?
    private boolean m_isWriteResponseReceived = false;  //Flag: Writeresponse empfangen?
    private boolean m_isDataPaketReceived = false;      //Flag: Datenpaket empfangen?
    private boolean m_startReadProcess = false;         //Flag: Leseprozess starten
    private boolean m_isTimeout = false;                //Flag: Timeout
    private boolean m_startSearch = false;              //Flag: Starte Suche
    private boolean m_sensorFound = false;              //Flag: Sensor gefunden
    private boolean m_isConnected = false;              //Flag: Sensor verbunden
    private boolean m_servicesFound = false;            //Flag: Services wurden gefunden
    private boolean m_onDisconnect = false;             //Flag: Verbindung soll getrennt werden
    private boolean m_disconnected = false;             //Flag: Verbindung abgebrochen
    private boolean m_switchedToSettings = false;       //Flag: Zu Settings Fragment geswitched

    //Timeout-Timer
    private static final long TIMER_TIMOUT_DATAREADPROCESS_DELAY = 5000;
    private static final long TIMER_TIMEOUT_SCANPROCESS_DELAY    = 10000;
    private static final long TIMER_TIMEOUT_CONNECTPROCESS_DELAY = 5000;
    private static final long TIMER_TIMEOUT_SERVICESCAN_DELAY    = 5000;
    Timer m_timeoutTimer;

    //Bluetooth-Schnittstelle
    LGS_BluetoothInterface m_btInterface;
    //---------------------------------------------------------------------------


    /**
     * Konstruktor
     * --------------------------------------------------------------------------
     */
    public LGS_BluetoothFSM(Context appContext) {
        if (appContext instanceof Interface) {
            m_interface = (Interface) appContext;
            m_btInterface = new LGS_BluetoothInterface(this, appContext);

            m_fsmState = FSM_STATE.STATE_UNKNOWN;         //Aktueller State
            m_newState = FSM_STATE.STATE_NOT_CONNECTED;   //Der nächste State

            triggerFSM();

        } else {
            throw new ClassCastException(appContext.toString()
                    + " must implement BT_FSM_DataRead_Interface");
        }
    }


    /**
     * Interface Funktionen für Bluetooth-Interfaceklasse
     * --------------------------------------------------------------------------
     */
    public void servicesFound(boolean isFound){
        if(isFound)
        {
            m_servicesFound = true;
            triggerFSM();
        }
    }
    public void sensorFound(boolean isFound){
        Log.i(TAG, "sensorFound: " + String.valueOf(isFound));
        if(isFound)
        {
            m_sensorFound = true;
            triggerFSM();
        }
    }
    public void sensorConnected(boolean isConnected) {
        Log.i(TAG, "sensorConnected: " + String.valueOf(isConnected));
        if(isConnected)
        {
            m_isConnected = true;
            triggerFSM();
        }
    }
    public void setDisconnected(){
        Log.i(TAG, "setDisconnected()");
        if(m_fsmState != FSM_STATE.STATE_CONNECTING)
        {
            m_disconnected = true;
            triggerFSM();
        }
    }
    public void handleNotify(BluetoothGattCharacteristic characteristic)
    {
        if (characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_NOTIFYREADY))
        {
            Log.i(TAG, "handleNotify: NotifyReady empfangen! Paket Count: " +
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));

            m_receivedPaket = characteristic;
            m_isNotifyReceived = true;
            triggerFSM();
        }
        else
        {
            //Interessiert keinen Prozess, an Fragment weiterleiten?
            //Log.i(TAG, "handleNotify: forwarding"); //Auskommentiert, da zu viele Nachrichten
            m_interface.forwardNotification(characteristic);
        }
    }
    public void handleReadResponse(BluetoothGattCharacteristic characteristic, int status)
    {
        if (characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_READPACKAGE))
        {
            Log.i(TAG, "handleReadResponse: Datenpaket empfangen!");
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                m_receivedPaket = characteristic;
                m_isDataPaketReceived = true;
                triggerFSM();
            }
            else
            {
                Log.e(TAG, "handleReadResponse: Fehler mit Datenpaket!");
            }
        }
        else
        {
            //Interessiert keinen Prozess, an Fragment weiterleiten?
            Log.i(TAG, "handleReadResponse: forwarding");
            m_interface.forwardReadResponse(characteristic, status);
        }
    }
    public void handleWriteResponse(BluetoothGattCharacteristic characteristic, int status)
    {
        if (characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_REQUEST))
        {
            if((status == BluetoothGatt.GATT_SUCCESS))
            {
                Log.i(TAG, "handleWriteResponse: GATT_SUCCESS; Value written: "
                        + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                m_receivedPaket = characteristic;
                m_isWriteResponseReceived = true;
                triggerFSM();
            }
            else
            {
                Log.e(TAG, "handleWriteResponse: Status != GATT_SUCCESS, " + status);
            }
        }
        else
        {
            //Interessiert keinen Prozess, an Fragment weiterleiten?
            Log.i(TAG, "handleWriteResponse: forwarding");
            m_interface.forwardWriteResponse(characteristic, status);
        }
    }
    public void requestReadCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        Log.i(TAG, "requestReadCharacteristic()");
        if(m_fsmState == FSM_STATE.STATE_CONNECTED_READY){
            m_btInterface.requestCharacteristic(characteristic);
        }
    }
    public LGS_BluetoothInterface getBtInterface()
    {
        Log.i(TAG, "getBtInterface()");
        if(m_fsmState == FSM_STATE.STATE_CONNECTED_READY){
             return m_btInterface;
        }
        else
        {
            return null;
        }
    }



    //--------------------------------------------------------------------------
    // CALLBACKS - PUBLIC
    //--------------------------------------------------------------------------
    /**
     * Callback:
     * -> Starte Suche nach LGS Sensor
     * --------------------------------------------------------------------------
     */
    public void switchedToSettingsFragment(){
        Log.i(TAG, "switchedToSettingsFragment()");
        m_switchedToSettings = true;
        triggerFSM();
    }
    public void startSearchForSensor(){
        Log.i(TAG, "startSearchForSensor: Beginne Suche!");
        if(m_btInterface.isBluetoothAvailable()) {
            m_startSearch = true;
            triggerFSM();
        }
        else{
            Log.e(TAG, "startSearchForSensor: Bluetoothadapter nicht verfügbar!");
            m_interface.makeToast("Bluetoothadapter nicht verfügbar!");
        }
    }
    /**
     * Callback:
     * -> Startet den Leseprozess
     * --------------------------------------------------------------------------
     */
    public void startReadProcess(FSM_SENSORPROPERTY readproperty)
    {
        Log.i(TAG, "startReadProcess()");
        if (readproperty != FSM_SENSORPROPERTY.SENSORPROPERTY_NONE)
        {
            m_startReadProcess = true;
            m_readProperty = readproperty;
            triggerFSM();
        }
        else {
            //Es wurde bereits ein Leseprozess gestartet, ignoriere Anfrage
            Log.e(TAG, "startReadProcess: Bereits ein Prozess aktiv!");
        }
    }
    /**
     * Callback: Trennt die Verbindung
     * --------------------------------------------------------------------------
     */
    public void disconnect()
    {
        Log.i(TAG, "disconnect()");
        m_onDisconnect = true;
        triggerFSM();
    }




    //--------------------------------------------------------------------------
    // Hilfscallbacks - PRIVATE
    //--------------------------------------------------------------------------
    private boolean cancelConnection() {
        boolean cancelConnection = false;

        if (m_onDisconnect || m_disconnected){
            cancelConnection = true;
            Log.i(TAG, "cancelConnection!");
        }
        return cancelConnection;
    }
    private void resetProcessData() {
        Log.i(TAG, "resetProcessData()");
        m_readProperty = FSM_SENSORPROPERTY.SENSORPROPERTY_NONE;
        m_remainingPaketsToRead = 0;
    }
    private void startTimeoutSupervision(long delay)
    {
        Log.i(TAG, "startTimeoutSupervision()");
        m_timeoutTimer = new Timer();
        m_timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                m_isTimeout = true;
                triggerFSM();
            }
        }, delay);
    }


    /**
     * --------------------------------------------------------------------------
     * --------------------------------------------------------------------------
     * Callback:
     * -> FSM HANDLING
     * --------------------------------------------------------------------------
     * --------------------------------------------------------------------------
     */
    private void triggerFSM() {
        int preventEndlessLoop = 5;

        do {
            m_fsmState = m_newState;
            m_newState = processState(m_fsmState);
            preventEndlessLoop--;
        }
        while ((m_newState != m_fsmState)
                && (preventEndlessLoop > 0));
    }

    private FSM_STATE processState(FSM_STATE state) {
        FSM_STATE newState = state;
        boolean keepFlags = false;

        //######################################################################
        if (state == FSM_STATE.STATE_NOT_CONNECTED) {
            Log.i(TAG, "STATE_NOT_CONNECTED");

            if (m_startSearch)
            {
                newState = FSM_STATE.STATE_SEARCHING;
                m_btInterface.startScanning();
                startTimeoutSupervision(TIMER_TIMEOUT_SCANPROCESS_DELAY);
            }
        }
        //######################################################################
        else if(state == FSM_STATE.STATE_SEARCHING)
        {
            Log.i(TAG, "STATE_SEARCHING");

            if (m_sensorFound) //Warten bis verbunden...
            {
                m_timeoutTimer.cancel();
                newState = FSM_STATE.STATE_CONNECTING;
                m_btInterface.connectSensor();
                startTimeoutSupervision(TIMER_TIMEOUT_CONNECTPROCESS_DELAY);
            }

            if(m_isTimeout)
            {
                //Sensor nicht gefunden!
                m_interface.scanProcessFinished(false);
                m_btInterface.stopScanning();
                newState = FSM_STATE.STATE_NOT_CONNECTED;
            }

            if(cancelConnection()){
                if(!m_disconnected)
                    m_btInterface.disconnectDevice();
                m_disconnected = false;
                m_interface.connectProcessFinished(false);
                newState = FSM_STATE.STATE_NOT_CONNECTED;
            }
        }
        //######################################################################
        else if(state == FSM_STATE.STATE_CONNECTING)
        {
            Log.i(TAG, "STATE_CONNECTING");

            if(m_isConnected)
            {
                m_timeoutTimer.cancel();
                newState = FSM_STATE.STATE_CONNECTED;
                m_interface.connectProcessFinished(true);
            }

            if(m_isTimeout)
            {
                //Konnte nicht mit Sensor verbinden
                m_interface.connectProcessFinished(false);
            }

            if(cancelConnection()){
                if(!m_disconnected)
                    m_btInterface.disconnectDevice();
                m_disconnected = false;
                m_interface.connectProcessFinished(false);
                newState = FSM_STATE.STATE_NOT_CONNECTED;
            }
        }
        //######################################################################
        else if (state == FSM_STATE.STATE_CONNECTED)
        {
            Log.i(TAG, "STATE_CONNECTED");
            m_btInterface.discoverServices();
            startTimeoutSupervision(TIMER_TIMEOUT_SERVICESCAN_DELAY);
            newState = FSM_STATE.STATE_DISCOVER_SERVICES;
        }
        //######################################################################
        else if(state == FSM_STATE.STATE_DISCOVER_SERVICES)
        {
            Log.i(TAG, "STATE_DISCOVER_SERVICES");

            if(m_servicesFound)
            {
                m_timeoutTimer.cancel();
                newState = FSM_STATE.STATE_CONNECTED_READY;
                m_btInterface.subscribeOnCharacteristics();
            }

            if(m_isTimeout)
            {
                //Konnte Services nicht finden, Verbindungsabbruch
                newState = FSM_STATE.STATE_NOT_CONNECTED;
                m_btInterface.disconnectDevice();
                m_interface.servicesFound(false);
            }

            if(cancelConnection()){
                if(!m_disconnected)
                    m_btInterface.disconnectDevice();
                m_disconnected = false;
                m_interface.connectProcessFinished(false);
                newState = FSM_STATE.STATE_NOT_CONNECTED;
            }
        }
        /**
         * ################################################
         * ################################################
         */
        else if(state == FSM_STATE.STATE_CONNECTED_READY)
        {
            Log.i(TAG, "STATE_CONNECTED_READY");

            if (m_startReadProcess) {
                newState = FSM_STATE.STATE_REQUEST_READ_DATA;
            }

            if(m_switchedToSettings){
                m_btInterface.requestAllSettings();
                //Newstate ist wieder STATE_CONNECTED_READY
            }

            if(cancelConnection()){
                if(!m_disconnected)
                    m_btInterface.disconnectDevice();
                m_disconnected = false;
                m_interface.connectProcessFinished(false);
                newState = FSM_STATE.STATE_NOT_CONNECTED;
            }
        }
        /**
         * ################################################
         * ################################################
         */
        else if (state == FSM_STATE.STATE_REQUEST_READ_DATA)
        {
            Log.i(TAG, "STATE_REQUEST_READ_DATA");

            if(m_readProperty == FSM_SENSORPROPERTY.SENSORPROPERTY_NONE)
            {
                newState = FSM_STATE.STATE_UNKNOWN;
            }
            else
            {
                switch (m_readProperty) {
                    case SENSORPROPERTY_TEMPERATURE:
                    {
                        m_btInterface.sendProcessRequestCharacteristic(1);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_HUMIDITY:
                    {
                        m_btInterface.sendProcessRequestCharacteristic(2);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_PRESSURE:
                    {
                        m_btInterface.sendProcessRequestCharacteristic(3);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_CO2:
                    {
                        m_btInterface.sendProcessRequestCharacteristic(4);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_VOC:
                    {
                        m_btInterface.sendProcessRequestCharacteristic(5);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                }

                startTimeoutSupervision(TIMER_TIMOUT_DATAREADPROCESS_DELAY);
            }

        }
        //######################################################################
        else if (state == FSM_STATE.STATE_WAITING_FOR_READDATA_READY)
        {
            Log.i(TAG, "STATE_WAITING_FOR_READDATA_READY");

            //Notify Readdata Ready empfangen?
            if (m_isNotifyReceived)
            {
                m_timeoutTimer.cancel();
                m_remainingPaketsToRead =
                            m_receivedPaket.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                m_dataCountPerPaket =
                            m_receivedPaket.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);

                Log.i(TAG, "processState: Remaining Pakets to read: " + m_remainingPaketsToRead
                        + ", Data Count per Paket: " + m_dataCountPerPaket);

                m_readProcessData = new ReadProcessData(m_readProperty);
                newState = FSM_STATE.STATE_REQUEST_DATAPACKAGE;
            }

            //Hat der User einen anderen Prozess gestartet und es wurde noch kein
            //Notifyready empfangen? (-> ggf. Fehler im Modul)
            if(m_startReadProcess) {
                keepFlags = true;
                newState = FSM_STATE.STATE_CONNECTED;
            }

                //Timeout?
            if(m_isTimeout) {
                newState = FSM_STATE.CANCEL_PROCESS;
                m_interface.makeToast("Timeout!");
            }

            if(cancelConnection()){
                if(!m_disconnected)
                    m_btInterface.disconnectDevice();
                m_disconnected = false;
                m_interface.connectProcessFinished(false);
                newState = FSM_STATE.STATE_NOT_CONNECTED;
            }
        }
        //######################################################################
        else if (state == FSM_STATE.STATE_REQUEST_DATAPACKAGE)
        {
            Log.i(TAG, "STATE_REQUEST_DATAPACKAGE");

            m_btInterface.readPaketCharacteristic();

            m_remainingPaketsToRead--;
            startTimeoutSupervision(TIMER_TIMOUT_DATAREADPROCESS_DELAY);
            newState = FSM_STATE.STATE_WAITING_FOR_DATAPACKAGE;

        }
        //######################################################################
        else if (state == FSM_STATE.STATE_WAITING_FOR_DATAPACKAGE)
        {
            Log.i(TAG, "STATE_WAITING_FOR_DATAPACKAGE");

            if (m_isDataPaketReceived)
            {
                m_timeoutTimer.cancel();

                switch (m_readProperty) {
                    case SENSORPROPERTY_CO2:
                    case SENSORPROPERTY_VOC:
                    case SENSORPROPERTY_PRESSURE:
                    {
                        for(int i = 0; i < m_dataCountPerPaket; i++)
                        {
                            int newValue = m_receivedPaket.getIntValue(
                                    BluetoothGattCharacteristic.FORMAT_UINT16, 2*i);
                            Log.i(TAG, "processState: Paket " + i + ": " + newValue);
                            m_readProcessData.addDataPoint(new DataPoint(
                                   m_readProcessData.c_dataPoints.size() + 1, (float)newValue));
                        }
                        break;
                    }
                    case SENSORPROPERTY_HUMIDITY:
                    {
                        for(int i = 0; i < m_dataCountPerPaket; i++)
                        {
                            int newValue = m_receivedPaket.getIntValue(
                                    BluetoothGattCharacteristic.FORMAT_UINT8, i);
                            Log.i(TAG, "processState: Paket " + i + ": " + newValue);
                            m_readProcessData.addDataPoint(new DataPoint(
                                    m_readProcessData.c_dataPoints.size() + 1, (float)newValue));
                        }
                        break;
                    }
                    case SENSORPROPERTY_TEMPERATURE:
                    {
                        for(int i = 0; i < m_dataCountPerPaket; i++)
                        {
                            int newValue = m_receivedPaket.getIntValue(
                                    BluetoothGattCharacteristic.FORMAT_SINT8, i);
                            Log.i(TAG, "processState: Paket " + i + ": " + newValue);

                            m_readProcessData.addDataPoint(new DataPoint(
                                    m_readProcessData.c_dataPoints.size() + 1, (float)newValue));
                        }
                        break;
                    }
                }

                if (m_remainingPaketsToRead < 1)
                    newState = FSM_STATE.STATE_READDATA_COMPLETED;
                else
                    newState = FSM_STATE.STATE_REQUEST_DATAPACKAGE;
                }

                //Timeout?
                if(m_isTimeout)
                {
                    newState = FSM_STATE.CANCEL_PROCESS;
                    m_interface.makeToast("Timeout!");
                }

                if(cancelConnection()){
                    if(!m_disconnected)
                        m_btInterface.disconnectDevice();
                    m_disconnected = false;
                    m_interface.connectProcessFinished(false);
                    newState = FSM_STATE.STATE_NOT_CONNECTED;
                }
        }
        //######################################################################
        else if (state == FSM_STATE.STATE_READDATA_COMPLETED)
        {
            Log.i(TAG, "STATE_READDATA_COMPLETED");

            m_btInterface.sendProcessRequestCharacteristic(0);
            m_interface.makeToast("Complete!");
            m_interface.readProcessFinished(m_readProcessData);

            resetProcessData();
            newState = FSM_STATE.STATE_CONNECTED_READY;
        }
        //######################################################################
        else if (state == FSM_STATE.CANCEL_PROCESS)
        {
            Log.i(TAG, "CANCEL_PROCESS");

            //An Sensor schicken: Abbruch
            m_btInterface.sendProcessRequestCharacteristic(0);
            resetProcessData();
            newState = FSM_STATE.STATE_CONNECTED_READY;
        }
        //######################################################################
        else
        {
            //Hier ist etwas schief gelaufen
            Log.e(TAG, "STATE_UNKNOWN");
        }

        //Reset Control-Flags:
        if(!keepFlags) {
            m_startReadProcess = false;
            m_isDataPaketReceived = false;
            m_isWriteResponseReceived = false;
            m_isReadResponseReceived = false;
            m_isNotifyReceived = false;
            m_isTimeout = false;
            m_startSearch = false;
            m_onDisconnect = false;
            m_isConnected = false;
            m_switchedToSettings = false;
        }

        return newState;
    }
}
