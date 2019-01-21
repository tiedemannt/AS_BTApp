package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import java.util.List;
import java.util.UUID;

public class BT_FSM_DataRead {

    /**
     * Constants
     * --------------------------------------------------------------------------
     */
    public final static String UUID_SERVICE_FSM = "ad42a590-b7af-4082-b8f4-b4b48798e696";
    public final static String UUID_CHARACTERISTIC_REQUEST = "ad42a590-b7af-4082-b8f4-b4b48798e697";
    public final static String UUID_CHARACTERISTIC_NOTIFYREADY = "ad42a590-b7af-4082-b8f4-b4b48798e698";
    public final static String UUID_CHARACTERISTIC_READPACKAGE = "ad42a590-b7af-4082-b8f4-b4b48798e699";
    private static final String TAG = "BT_FSM_DataRead";
    //---------------------------------------------------------------------------


    /**
     * Enums
     * --------------------------------------------------------------------------
     */
    private enum FSM_STATE {
        STATE_UNKNOWN,                      //Unbekannter State
        STATE_NOT_CONNECTED,                //LGS nicht verbunden
        STATE_CONNECTED,                    //LGS Device verbunden
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
    public interface BT_FSM_DataRead_Interface {
        void subscribeToCharacteristic(BluetoothGattCharacteristic characteristic);
        void readProcessFinished(ReadProcessData readData);
        BluetoothGatt getGattObject();
    }

    BT_FSM_DataRead_Interface m_interface;

    public class ReadProcessData {
        public FSM_SENSORPROPERTY c_dataType;
        public List<DataPoint> c_dataPoints;

        public ReadProcessData(FSM_SENSORPROPERTY propertyType) {
            c_dataType = propertyType;;
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

    //Characteristic des FSM Service: Notifyready Characteristic
    private BluetoothGattCharacteristic m_fsmNotifyCharacteristic = null;
    private BluetoothGattCharacteristic m_fsmRequestCharacteristic = null;
    private BluetoothGattCharacteristic m_fsmPaketCharacteristic = null;

    //Das zu lesende Property:
    private FSM_SENSORPROPERTY m_readProperty = FSM_SENSORPROPERTY.SENSORPROPERTY_NONE;

    //Das empfangene Paket:
    private BluetoothGattCharacteristic m_receivedPaket = null;

    //Flag: Ist LGS Connected:
    private boolean m_isConnected = false;

    //Die noch zu lesenden Pakete:
    private int m_remainingPaketsToRead = 0;
    private ReadProcessData m_readProcessData;

    //Control Flags:
    private boolean m_isNotifyReceived = false;         //Flag: Ist Notify empfangen worden?
    private boolean m_isReadResponseReceived = false;   //Flag: Readresponse emfpangen?
    private boolean m_isWriteResponseReceived = false;  //Flag: Writeresponse empfangen?
    private boolean m_isDataPaketReceived = false;      //Flag: Datenpaket empfangen?
    private boolean m_startReadProcess = false;         //Flag: Leseprozess starten
    //---------------------------------------------------------------------------


    /**
     * Konstruktor
     * --------------------------------------------------------------------------
     */
    public BT_FSM_DataRead(Context appContext) {
        if (appContext instanceof BT_FSM_DataRead_Interface) {

            m_interface = (BT_FSM_DataRead_Interface) appContext;

            m_fsmState = FSM_STATE.STATE_UNKNOWN;         //Aktueller State
            m_newState = FSM_STATE.STATE_NOT_CONNECTED;   //Der nächste State

            triggerFSM();

        } else {
            throw new ClassCastException(appContext.toString()
                    + " must implement BT_FSM_DataRead_Interface");
        }
    }

    /**
     * Callback:
     * -> Receive Characteristics
     * --------------------------------------------------------------------------
     */
    public void setLGSConnected(List<BluetoothGattCharacteristic> fsmCharlist) {
        Log.d(TAG, "setLGSConnected()!");

        if (fsmCharlist.size() != 0) {
            int characteristicCount = 0;

            //Check, ob alle Characteristics enthalten:
            for (BluetoothGattCharacteristic item : fsmCharlist) {
                if (item.getUuid().toString().equals(UUID_CHARACTERISTIC_REQUEST))
                {
                    characteristicCount++;
                    m_fsmRequestCharacteristic = item;
                }
                if (item.getUuid().toString().equals(UUID_CHARACTERISTIC_NOTIFYREADY))
                {
                    characteristicCount++;
                    m_fsmNotifyCharacteristic = item;
                }
                if (item.getUuid().toString().equals(UUID_CHARACTERISTIC_READPACKAGE))
                {
                    characteristicCount++;
                    m_fsmPaketCharacteristic = item;
                }
            }

            if (characteristicCount == 3) {
                m_isConnected = true;

                Log.d(TAG, "setLGSConnected: Alle Characteristics gefunden, triggere FSM!");
                triggerFSM();
            } else {
                Log.d(TAG, "setLGSConnected: Nicht alle Characteristics gefunden!");
            }
        }
    }

    /**
     * Callback:
     * -> LGS Disconnected!
     * --------------------------------------------------------------------------
     */
    public void setLGSDisconnected() {
        Log.d(TAG, "setLGSDisconnected()!");

        m_fsmNotifyCharacteristic = null;
        m_fsmPaketCharacteristic = null;
        m_fsmRequestCharacteristic = null;
        m_readProperty = FSM_SENSORPROPERTY.SENSORPROPERTY_NONE;
        m_isConnected = false;
        triggerFSM();
    }

    /**
     * Callback:
     * -> Receive Notify Package
     * --------------------------------------------------------------------------
     */
    public void handleNotify(BluetoothGattCharacteristic characteristic) 
    {
        if (characteristic.getUuid().toString().equals(UUID_CHARACTERISTIC_NOTIFYREADY)) 
        {
            Log.d(TAG, "handleNotify: NotifyReady empfangen! Paket Count: " +
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));

            m_receivedPaket = characteristic;
            m_isNotifyReceived = true;
            triggerFSM();
        } 
        else 
        {
            //Paket interessiert nicht, ignorieren
            //Log.d(TAG, "handleNotify: UUID != UUID_CHARACTERISTIC_NOTIFYREADY: " + characteristic.getUuid().toString());
        }
    }

    /**
     * Callback:
     * -> Receive Readresponse
     * --------------------------------------------------------------------------
     */
    public void handleReadResponse(BluetoothGattCharacteristic characteristic, int status)
    {
        if (characteristic.getUuid().toString().equals(UUID_CHARACTERISTIC_READPACKAGE))
        {
            Log.d(TAG, "handleReadResponse: Datenpaket empfangen!");
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                m_receivedPaket = characteristic;
                m_isDataPaketReceived = true;
                triggerFSM();
            }
            else
            {
                Log.d(TAG, "handleReadResponse: Fehler mit Datenpaket!");
            }
        }
        else
        {
            //Paket interessiert nicht, ignorieren
        }
    }

    /**
     * Callback:
     * -> Receive Writeresponse
     * --------------------------------------------------------------------------
     */
    public void handleWriteResponse(BluetoothGattCharacteristic characteristic, int status)
    {
        if (characteristic.getUuid().toString().equals(UUID_CHARACTERISTIC_REQUEST))
        {
            if((status == BluetoothGatt.GATT_SUCCESS)) {
                Log.d(TAG, "handleWriteResponse: GATT_SUCCESS");
                m_receivedPaket = characteristic;
                m_isWriteResponseReceived = true;
                triggerFSM();
            }
            else
            {
                Log.d(TAG, "handleWriteResponse: Status != GATT_SUCCESS, " + status);
            }
        }

    }

    /**
     * Callback:
     * -> Startet den Leseprozess
     * --------------------------------------------------------------------------
     */
    public void startReadProcess(FSM_SENSORPROPERTY readproperty)
    {
        if (readproperty != FSM_SENSORPROPERTY.SENSORPROPERTY_NONE)
        {
            m_startReadProcess = true;
            m_readProperty = readproperty;
            triggerFSM();
        }
        else {
            //Es wurde bereits ein Leseprozess gestartet, ignoriere Anfrage
            Log.d(TAG, "startReadProcess: Bereits ein Prozess aktiv!");
        }
    }

    /**
     * Hilfscallback: Wurde ein Paket verworfen/nicht verarbeitet?
     *
     * @return
     */
    private boolean moreThanOnePaketReceived() {
        int paketCount = 0;

        if (m_isNotifyReceived)         paketCount++;
        if (m_isReadResponseReceived)   paketCount++;
        if (m_isWriteResponseReceived)  paketCount++;
        if (m_isDataPaketReceived)      paketCount++;

        return (paketCount > 1);
    }

    /**
     * Hilfscallback: Leseprozess fortsetzen?
     *
     * @return
     */
    private boolean continueReadProcess() {
        boolean continueProcess = true;

        if ((moreThanOnePaketReceived())
                || (m_readProperty == FSM_SENSORPROPERTY.SENSORPROPERTY_NONE)
                || (!m_isConnected)) {
            continueProcess = false;
        }
        return continueProcess;
    }

    /**
     * Hilfscallback: Reset Process Data
     *
     * @return
     */
    private void resetProcessData() {
        m_readProperty = FSM_SENSORPROPERTY.SENSORPROPERTY_NONE;
        m_remainingPaketsToRead = 0;
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
                                                    //Beim ersten Aufruf: m_newState - > NOT_CONNECTED
                                                    //                    m_fsmState - > UNKNOWN
            m_fsmState = m_newState;                //m_fsmState: UNKNOWN -> NOT_CONNECTED
            m_newState = processState(m_fsmState);  //Trigger mit NOT_CONNECTED

            preventEndlessLoop--;
        } while ((m_newState != m_fsmState)
                && (preventEndlessLoop > 0));
    }

    private FSM_STATE processState(FSM_STATE state) {
        FSM_STATE newState = state;

        //Initialer State oder Device nicht verbunden
        if (state == FSM_STATE.STATE_NOT_CONNECTED) {
            Log.d(TAG, "STATE_NOT_CONNECTED");
            if (m_isConnected) //Warten bis verbunden...
            {
                m_interface.subscribeToCharacteristic(m_fsmNotifyCharacteristic);

                newState = FSM_STATE.STATE_CONNECTED;
            }
        }
        else if (state == FSM_STATE.STATE_CONNECTED)
        {
            Log.d(TAG, "STATE_CONNECTED");
            if (!m_isConnected) {
                newState = FSM_STATE.STATE_NOT_CONNECTED;
            } else {
                if (m_startReadProcess) {
                    newState = FSM_STATE.STATE_REQUEST_READ_DATA;
                } else {
                    //Es soll kein Prozess gestartet werden, tue nichts.
                }
            }
        }
        else if (state == FSM_STATE.STATE_REQUEST_READ_DATA)
        {
            Log.d(TAG, "STATE_REQUEST_READ_DATA");

            switch (m_readProperty) {
                case SENSORPROPERTY_TEMPERATURE:
                {
                    m_fsmRequestCharacteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    m_interface.getGattObject().writeCharacteristic(m_fsmRequestCharacteristic);
                    newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                    break;
                }
                case SENSORPROPERTY_HUMIDITY:
                {
                    m_fsmRequestCharacteristic.setValue(2, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    m_interface.getGattObject().writeCharacteristic(m_fsmRequestCharacteristic);
                    newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                    break;
                }
                case SENSORPROPERTY_PRESSURE:
                {
                    m_fsmRequestCharacteristic.setValue(3, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    m_interface.getGattObject().writeCharacteristic(m_fsmRequestCharacteristic);
                    newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                    break;
                }
                case SENSORPROPERTY_CO2:
                {
                    m_fsmRequestCharacteristic.setValue(4, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    m_interface.getGattObject().writeCharacteristic(m_fsmRequestCharacteristic);
                    newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                    break;
                }
                case SENSORPROPERTY_VOC:
                {
                    m_fsmRequestCharacteristic.setValue(5, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    m_interface.getGattObject().writeCharacteristic(m_fsmRequestCharacteristic);
                    newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                    break;
                }
                case SENSORPROPERTY_NONE:
                default: {
                    newState = FSM_STATE.STATE_UNKNOWN;
                    break;
                }
            }
        }
        else if (state == FSM_STATE.STATE_WAITING_FOR_READDATA_READY)
        {
            Log.d(TAG, "STATE_WAITING_FOR_READDATA_READY");
            if (!continueReadProcess()) {
                newState = FSM_STATE.CANCEL_PROCESS;
            }
            else
            {
                //Notify Readdata Ready empfangen?
                if (m_isNotifyReceived)
                {
                    m_remainingPaketsToRead =
                            m_receivedPaket.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);

                    Log.d(TAG, "processState: Remaining Pakets to read: " + m_remainingPaketsToRead);

                    m_readProcessData = new ReadProcessData(m_readProperty);
                    newState = FSM_STATE.STATE_REQUEST_DATAPACKAGE;
                }
            }
        }
        else if (state == FSM_STATE.STATE_REQUEST_DATAPACKAGE)
        {
            Log.d(TAG, "STATE_REQUEST_DATAPACKAGE");
            if (!continueReadProcess()) {
                newState = FSM_STATE.CANCEL_PROCESS;
            }
            else
            {
                m_interface.getGattObject().readCharacteristic(m_fsmPaketCharacteristic);

                m_remainingPaketsToRead--;
                newState = FSM_STATE.STATE_WAITING_FOR_DATAPACKAGE;
            }
        }
        else if (state == FSM_STATE.STATE_WAITING_FOR_DATAPACKAGE)
        {
            Log.d(TAG, "STATE_WAITING_FOR_DATAPACKAGE");
            if (!continueReadProcess())
            {
                newState = FSM_STATE.CANCEL_PROCESS;
            }
            else
            {
                if (m_isDataPaketReceived)
                {
                    switch (m_readProperty) {
                        case SENSORPROPERTY_CO2:
                        case SENSORPROPERTY_VOC:
                        case SENSORPROPERTY_PRESSURE:
                        {
                            for(int i = 0; i < 10; i++)
                            {
                                int newValue = m_receivedPaket.getIntValue(
                                        BluetoothGattCharacteristic.FORMAT_UINT16, 2*i);
                                Log.d(TAG, "processState: Paket " + i + ": " + newValue);

                                m_readProcessData.addDataPoint(new DataPoint(
                                        m_readProcessData.c_dataPoints.size() + 1, (float)newValue));
                            }
                            break;
                        }
                        case SENSORPROPERTY_HUMIDITY:
                        {
                            for(int i = 0; i < 20; i++)
                            {
                                int newValue = m_receivedPaket.getIntValue(
                                        BluetoothGattCharacteristic.FORMAT_UINT8, i);
                                Log.d(TAG, "processState: Paket " + i + ": " + newValue);

                                m_readProcessData.addDataPoint(new DataPoint(
                                        m_readProcessData.c_dataPoints.size() + 1, (float)newValue));
                            }
                            break;
                        }
                        case SENSORPROPERTY_TEMPERATURE:
                        {
                            for(int i = 0; i < 20; i++)
                            {
                                int newValue = m_receivedPaket.getIntValue(
                                        BluetoothGattCharacteristic.FORMAT_SINT8, i);
                                Log.d(TAG, "processState: Paket " + i + ": " + newValue);

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
            }
        }
        else if (state == FSM_STATE.STATE_READDATA_COMPLETED)
        {
            Log.d(TAG, "STATE_READDATA_COMPLETED");
            m_interface.readProcessFinished(m_readProcessData);

            resetProcessData();
            if (m_isConnected)
                newState = FSM_STATE.STATE_CONNECTED;
            else
                newState = FSM_STATE.STATE_NOT_CONNECTED;
        }
        else if (state == FSM_STATE.CANCEL_PROCESS)
        {
            Log.d(TAG, "CANCEL_PROCESS");

            resetProcessData();

            if (m_isConnected)
                newState = FSM_STATE.STATE_CONNECTED;
            else
                newState = FSM_STATE.STATE_NOT_CONNECTED;
        }
        else
        {
            //Hier ist etwas schief gelaufen
            Log.d(TAG, "STATE_UNKNOWN");
        }

        //Reset Control-Flags:
        m_startReadProcess = false;
        m_isDataPaketReceived = false;
        m_isWriteResponseReceived = false;
        m_isReadResponseReceived = false;
        m_isNotifyReceived = false;

        return newState;
    }
}
