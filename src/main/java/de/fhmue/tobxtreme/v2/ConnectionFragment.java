package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothDevice;
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
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

public class ConnectionFragment extends Fragment {
    /**
     * CONSTANTS
     */
    private static final String TAG = "ConnectionFragment";

    private ListView                    m_listView;             //Listview Devices
    private ListAdapter_BTLE_Devices    m_adapter;              //Listadapter
    private ArrayList<BluetoothDevice>  m_BTDevicesArrayList;   //Device Arraylist Data
    private Switch                      m_switch;               //Switch Button

    public interface ConnectionFragmentInterface
    {
        void startScan();
        void stopScan();
    }
    ConnectionFragmentInterface m_Callback;

    public void clearDeviceList()
    {
        m_BTDevicesArrayList.clear();
        m_adapter = new ListAdapter_BTLE_Devices(getActivity(), R.layout.btle_device_list_item, m_BTDevicesArrayList);
        m_listView.setAdapter(m_adapter);
    }
    public void addDevice(BluetoothDevice device)
    {
        Log.d(TAG, "addDevice: " + device.getName() + ": " + device.getAddress());
        Toast.makeText(getActivity(), "Device found: " + device.getAddress(), Toast.LENGTH_SHORT).show();
        m_BTDevicesArrayList.add(device);
        m_adapter = new ListAdapter_BTLE_Devices(getActivity(), R.layout.btle_device_list_item, m_BTDevicesArrayList);
        m_listView.setAdapter(m_adapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ConnectionFragmentInterface) {
            m_Callback = (ConnectionFragmentInterface) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ConnectionFragment.ConnectionFragmentInterface");
        }
    }

    @Override
    public void onDetach()
    {
        m_Callback.stopScan();
        super.onDetach();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_connection, container, false);

        m_BTDevicesArrayList = new ArrayList<>();
        m_adapter = new ListAdapter_BTLE_Devices(getActivity(), R.layout.btle_device_list_item, m_BTDevicesArrayList);

        /**
         * Get UI-Items from Layout
         */
        m_switch = v.findViewById(R.id.connection_fragment_switch);
        m_listView = v.findViewById(R.id.connection_fragment_listView);

        /**
         * Init UI-Items
         */
        m_switch.setOnClickListener(m_switchListener);
        m_listView.setAdapter(m_adapter);
        m_listView.setOnItemClickListener(m_itemClickListener);

        return v;
    }

    /**
     *   UI Adapters
     */
    private AdapterView.OnItemClickListener m_itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //TODO
        }
    };
    private Switch.OnClickListener m_switchListener = new Switch.OnClickListener(){
        @Override
        public void onClick(View view) {
            if(m_switch.isChecked())
            {
                m_Callback.startScan();
            }
            else
            {
                m_Callback.stopScan();
            }
        }
    };
}
