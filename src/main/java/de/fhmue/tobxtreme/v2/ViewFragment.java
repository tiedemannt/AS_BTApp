package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ViewFragment extends Fragment {
    /**
     * INTERFACE
     */
    public interface ViewFragmentInterface
    {
        void subscribeToCharacteristic(BluetoothGattCharacteristic characteristic);
        void unsubscribeAllCharacteristics();
        void disconnectFromDevice();
        void switchToHomeFragment(List<BluetoothGattCharacteristic> EnvCharlist,
                                  List<BluetoothGattCharacteristic> SetCharlist);
    }
    ViewFragmentInterface m_Callback;


    /**
     * OBJECTS
     */
    private BluetoothGatt                           m_btGatt;               //Service List Data
    private ListAdapter_BTLE_Services               m_adapter;              //Listadapter
    private ListView                                m_listView;             //Listview Services
    private TextView                                m_textView;             //Textview
    private BluetoothGattCharacteristic             m_characteristic;       //Actual Value
    private Button                                  m_disconnectButton;     //Disconnect Button

    private ArrayList<MapItem> m_UUIDMAP;         //Map Data


    /**
     * CONSTANTS
     */
    private final String TAG = "ViewFragment";
    //Service UUID
    public final static String UUID_SERVICE_ENVIRONMENT        = "7d36eed5-ca05-42f3-867e-4d800a459ca1";
    public final static String UUID_SERVICE_CONFIGURATION      = "7d36eed5-ca05-42f3-867e-4d800a459ca2";

    //Data Characteristics UUIDS
    public final static String UUID_CHARACTERISTIC_BRIGHT      = "c50956f6-cb78-487e-9566-b883ff3e5d53";
    public final static String UUID_CHARACTERISTIC_TEMPERATURE = "b33102eb-43a0-4da1-8183-ed169c0f1720";
    public final static String UUID_CHARACTERISTIC_VOC         = "6bb014e9-a0c1-47b7-939d-f97b8e4f7877";
    public final static String UUID_CHARACTERISTIC_CO2         = "4e1fcadd-cdbf-46bc-8faa-4b06320cfa2c";
    public final static String UUID_CHARACTERISTIC_HUMIDITY    = "4e311cb9-a68b-44b7-aa97-a591190aa08e";
    public final static String UUID_CHARACTERISTIC_PRESSURE    = "666b7e99-e973-4860-9006-c78cb95da5aa";

    //Settings Characteristics UUIDS
    public final static String UUID_CHARACTERISTIC_SETTING_REPRATE   = "286cc204-4b3f-4f82-8ebb-667372b15669";
    public final static String UUID_CHARACTERISTIC_SETTING_CRITTEMP  = "286cc204-4b3f-4f82-8ebb-667372b1566a";
    public final static String UUID_CHARACTERISTIC_SETTING_CRITPRES  = "286cc204-4b3f-4f82-8ebb-667372b1566b";
    public final static String UUID_CHARACTERISTIC_SETTING_CRITCO2   = "286cc204-4b3f-4f82-8ebb-667372b1566c";
    public final static String UUID_CHARACTERISTIC_SETTING_CRITHUM   = "286cc204-4b3f-4f82-8ebb-667372b1566d";
    public final static String UUID_CHARACTERISTIC_SETTING_CRITVOC   = "286cc204-4b3f-4f82-8ebb-667372b1566e";
    public final static String UUID_CHARACTERISTIC_SETTING_OUTPUTACT = "286cc204-4b3f-4f82-8ebb-667372b1566f";

    //Text (für Mapping)
    public final static String TEXT_SERVICE_ENVIRONMENT        = "LGS Messdaten";
    public final static String TEXT_SERVICE_CONFIGURATION      = "LGS Konfiguration";
    public final static String TEXT_CHARACTERISTIC_BRIGHT      = "Umgebung hell (ja/nein)";
    public final static String TEXT_CHARACTERISTIC_TEMPERATURE = "Temperatur (°C)";
    public final static String TEXT_CHARACTERISTIC_VOC         = "VOC (ppb)";
    public final static String TEXT_CHARACTERISTIC_CO2         = "CO2 (ppm)";
    public final static String TEXT_CHARACTERISTIC_HUMIDITY    = "Luftfeuchte (%)";
    public final static String TEXT_CHARACTERISTIC_PRESSURE    = "Luftdruck (mBar)";
    public final static String TEXT_CONFIGURATION_REPRATE      = "Repetition Rate (ms)";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_view, container, false);

        m_listView = v.findViewById(R.id.view_fragment_listview);
        m_textView = v.findViewById(R.id.view_fragment_textView);
        m_disconnectButton = v.findViewById(R.id.view_fragment_disconnect_button);

        m_listView.setOnItemClickListener(m_itemClickListener);
        m_disconnectButton.setOnClickListener(m_clickListener);

        //Service Mappings
        m_UUIDMAP = new ArrayList<>();
        m_UUIDMAP.add(new MapItem(UUID_SERVICE_ENVIRONMENT, TEXT_SERVICE_ENVIRONMENT));
        m_UUIDMAP.add(new MapItem(UUID_SERVICE_CONFIGURATION, TEXT_SERVICE_CONFIGURATION));

        //Characteristic Mappings
        m_UUIDMAP.add(new MapItem(UUID_CHARACTERISTIC_BRIGHT     , TEXT_CHARACTERISTIC_BRIGHT));
        m_UUIDMAP.add(new MapItem(UUID_CHARACTERISTIC_TEMPERATURE, TEXT_CHARACTERISTIC_TEMPERATURE));
        m_UUIDMAP.add(new MapItem(UUID_CHARACTERISTIC_VOC        , TEXT_CHARACTERISTIC_VOC));
        m_UUIDMAP.add(new MapItem(UUID_CHARACTERISTIC_CO2        , TEXT_CHARACTERISTIC_CO2));
        m_UUIDMAP.add(new MapItem(UUID_CHARACTERISTIC_HUMIDITY   , TEXT_CHARACTERISTIC_HUMIDITY));
        m_UUIDMAP.add(new MapItem(UUID_CHARACTERISTIC_PRESSURE   , TEXT_CHARACTERISTIC_PRESSURE));

        m_UUIDMAP.add(new MapItem(UUID_CHARACTERISTIC_SETTING_REPRATE, TEXT_CONFIGURATION_REPRATE));

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ConnectionFragment.ConnectionFragmentInterface) {
            m_Callback = (ViewFragment.ViewFragmentInterface) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ViewFragment.ViewFragmentInterface");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
    }


    public void addService(BluetoothGatt newBtGatt)
    {
        boolean envservice = false;
        boolean setservice = false;

        Log.i(TAG, "addService(): Setting BluetoothGatt Element");
        m_btGatt = newBtGatt;

        //Services durchsuchen nach Environment Service
        for(BluetoothGattService service : newBtGatt.getServices())
        {
            if(service.getUuid().toString().equals(UUID_SERVICE_ENVIRONMENT)) envservice = true;
            if(service.getUuid().toString().equals(UUID_SERVICE_CONFIGURATION)) setservice = true;
        }

        if(envservice && setservice)
        {
            Log.i(TAG, "Device bietet den Environment Service an -> LGS Sensor");

            m_Callback.switchToHomeFragment(
                    newBtGatt.getService(UUID.fromString(UUID_SERVICE_ENVIRONMENT)).getCharacteristics(),
                    newBtGatt.getService(UUID.fromString(UUID_SERVICE_CONFIGURATION)).getCharacteristics());
        }
        else
        {
            Log.i(TAG, "Device bietet nicht den Environment Service an -> kein LGS Sensor");

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("ViewFragment", "addService(): Setting List Adapter on UI Thread.");

                    m_adapter = new ListAdapter_BTLE_Services(getActivity(),
                            R.layout.btle_service_list_item, m_btGatt, m_UUIDMAP);
                    m_listView.setAdapter(m_adapter);

                    Toast.makeText(getActivity(),
                            "Kein LGS Sensor!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void displayCharacteristicValue(BluetoothGattCharacteristic characteristic)
    {
        //save characteristic
        m_characteristic = characteristic;

        //Display Value on UI Thread
        getActivity().runOnUiThread(() -> {
            switch (m_characteristic.getUuid().toString())
            {
                case UUID_CHARACTERISTIC_BRIGHT:
                {
                    m_textView.setText("" +
                            m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                    break;
                }
                case UUID_CHARACTERISTIC_TEMPERATURE:
                {
                    m_textView.setText("" +
                            m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
                    break;
                }
                case UUID_CHARACTERISTIC_VOC:
                {
                    m_textView.setText("" +
                            m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                    break;
                }
                case UUID_CHARACTERISTIC_CO2:
                {
                    m_textView.setText("" +
                            m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                    break;
                }
                case UUID_CHARACTERISTIC_HUMIDITY:
                {
                    m_textView.setText("" +
                            m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                    break;
                }
                case UUID_CHARACTERISTIC_PRESSURE:
                {
                    m_textView.setText("" +
                            m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                    break;
                }
                default:
                {
                    //Unknown Characteristic; Show as 16bit Signed Integer
                    m_textView.setText("UNC Val: " +
                            m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0));
                    break;
                }
            }
        });
    }

    /**
     *   UI Adapters
     */
    public AdapterView.OnItemClickListener m_itemClickListener = (adapterView, view, i, l) -> {
        m_textView.setText("Value");
        m_Callback.unsubscribeAllCharacteristics();
        m_Callback.subscribeToCharacteristic(ListAdapter_BTLE_Services.getCharacteristics(m_btGatt).get(i));
    };
    public AdapterView.OnClickListener m_clickListener = (View view) -> {
        switch(view.getId())
        {
            case R.id.view_fragment_disconnect_button:
            {
                Log.i(TAG, "AdapterView.OnClickListener - view_fragment_disconnect_button clicked.");
                m_Callback.disconnectFromDevice();
                break;
            }
            default:
            {
                Log.i(TAG, "AdapterView.OnClickListener - No Reaction.");
                break;
            }
        }
    };
}
