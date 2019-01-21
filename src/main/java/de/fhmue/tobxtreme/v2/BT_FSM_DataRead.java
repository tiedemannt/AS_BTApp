package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
        void makeToast(String text);
    }

    BT_FSM_DataRead_Interface m_interface;

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

    //Flag: Ist LGS Connected:
    private boolean m_isConnected = false;

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

    //Timeout-Timer
    private static final long TIMER_TIMOUT_DELAY = 2000;
    Timer m_timeoutTimer;
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
                }
                if (item.getUuid().toString().equals(UUID_CHARACTERISTIC_NOTIFYREADY))
                {
                    characteristicCount++;
                }
                if (item.getUuid().toString().equals(UUID_CHARACTERISTIC_READPACKAGE))
                {
                    characteristicCount++;
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
            if((status == BluetoothGatt.GATT_SUCCESS))
            {
                Log.d(TAG, "handleWriteResponse: GATT_SUCCESS; Value written: "
                    + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
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
     * Hilfscallback: Auf Notify Subscriben
     */
    private void subscribeToNotifyCharacteristic()
    {
        BluetoothGattCharacteristic chara =
                m_interface
                        .getGattObject()
                        .getService(UUID.fromString(UUID_SERVICE_FSM))
                        .getCharacteristic(UUID.fromString(UUID_CHARACTERISTIC_NOTIFYREADY));
        m_interface.subscribeToCharacteristic(chara);
    }
    /**
     * Hilfscallback: Gibt den Request-Characteristic zurück
     */
    private BluetoothGattCharacteristic getRequestCharacteristic()
    {
        BluetoothGattCharacteristic chara =
                m_interface
                        .getGattObject()
                        .getService(UUID.fromString(UUID_SERVICE_FSM))
                        .getCharacteristic(UUID.fromString(UUID_CHARACTERISTIC_REQUEST));
        return chara;
    }
    /**
     * Hilfscallback: Gibt den Paket-Characteristic zurück
     */
    private BluetoothGattCharacteristic getPaketCharacteristic()
    {
        BluetoothGattCharacteristic chara =
                m_interface
                        .getGattObject()
                        .getService(UUID.fromString(UUID_SERVICE_FSM))
                        .getCharacteristic(UUID.fromString(UUID_CHARACTERISTIC_READPACKAGE));
        return chara;
    }
    private void sendProcessRequestCharacteristic(int requestType)
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
        BluetoothGattCharacteristic m_fsmRequestCharacteristic = getRequestCharacteristic();
        m_fsmRequestCharacteristic.setValue(requestType, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        m_interface.getGattObject().writeCharacteristic(m_fsmRequestCharacteristic);
    }
    private void startTimeoutSupervision(long delay)
    {
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
        boolean keepFlags = false;

        //Initialer State oder Device nicht verbunden
        if (state == FSM_STATE.STATE_NOT_CONNECTED) {
            Log.d(TAG, "STATE_NOT_CONNECTED");
            if (m_isConnected) //Warten bis verbunden...
            {
                subscribeToNotifyCharacteristic();
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

            if(m_readProperty == FSM_SENSORPROPERTY.SENSORPROPERTY_NONE)
            {
                newState = FSM_STATE.STATE_UNKNOWN;
            }
            else
            {
                switch (m_readProperty) {
                    case SENSORPROPERTY_TEMPERATURE:
                    {
                        sendProcessRequestCharacteristic(1);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_HUMIDITY:
                    {
                        sendProcessRequestCharacteristic(2);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_PRESSURE:
                    {
                        sendProcessRequestCharacteristic(3);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_CO2:
                    {
                        sendProcessRequestCharacteristic(4);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                    case SENSORPROPERTY_VOC:
                    {
                        sendProcessRequestCharacteristic(5);
                        newState = FSM_STATE.STATE_WAITING_FOR_READDATA_READY;
                        break;
                    }
                }

                startTimeoutSupervision(TIMER_TIMOUT_DELAY);
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
                    m_timeoutTimer.cancel();
                    m_remainingPaketsToRead =
                            m_receivedPaket.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    m_dataCountPerPaket =
                            m_receivedPaket.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);

                    Log.d(TAG, "processState: Remaining Pakets to read: " + m_remainingPaketsToRead
                        + ", Data Count per Paket: " + m_dataCountPerPaket);

                    m_readProcessData = new ReadProcessData(m_readProperty);
                    newState = FSM_STATE.STATE_REQUEST_DATAPACKAGE;
                }

                //Hat der User einen anderen Prozess gestartet und es wurde noch kein
                //Notifyready empfangen? (-> ggf. Fehler im Modul)
                if(m_startReadProcess)
                {
                    keepFlags = true;
                    newState = FSM_STATE.STATE_CONNECTED;
                }

                //Timeout?
                if(m_isTimeout)
                {
                    newState = FSM_STATE.CANCEL_PROCESS;
                    m_interface.makeToast("Timeout!");
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
                m_interface.getGattObject().readCharacteristic(getPaketCharacteristic());

                m_remainingPaketsToRead--;
                startTimeoutSupervision(TIMER_TIMOUT_DELAY);
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
                                Log.d(TAG, "processState: Paket " + i + ": " + newValue);

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
                                Log.d(TAG, "processState: Paket " + i + ": " + newValue);

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

                //Timeout?
                if(m_isTimeout)
                {
                    newState = FSM_STATE.CANCEL_PROCESS;
                    m_interface.makeToast("Timeout!");
                }
            }
        }
        else if (state == FSM_STATE.STATE_READDATA_COMPLETED)
        {
            Log.d(TAG, "STATE_READDATA_COMPLETED");

            sendProcessRequestCharacteristic(0); //Indicate Finished
            m_interface.makeToast("Complete!");
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
        if(!keepFlags) {
            m_startReadProcess = false;
            m_isDataPaketReceived = false;
            m_isWriteResponseReceived = false;
            m_isReadResponseReceived = false;
            m_isNotifyReceived = false;
        }

        return newState;
    }
}
