package de.fhmue.tobxtreme.v2;

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
import android.widget.ProgressBar;
import android.widget.Toast;


import java.util.ArrayList;

public class ConnectionFragment extends Fragment {
    /**
     *  INTERFACE
     */
    public interface ConnectionFragmentInterface
    {
        void startScan();
        void stopScan();
        void connectToDevice(BT_Device device);
    }
    ConnectionFragmentInterface m_Callback;


    /**
     * CONSTANTS
     */
    private static final String TAG = "ConnectionFragment";


    /**
     * OBJECTS
     */
    private ListView                    m_listView;             //Listview Devices
    private ListAdapter_BTLE_Devices    m_adapter;              //Listadapter
    private ArrayList<BT_Device>        m_BTDevicesArrayList;   //Device Arraylist Data
    private boolean                     m_scanActive;           //Flag, ob gerade Scan aktiv
    private Button                      m_scanButton;           //Button Retry Scan
    private ProgressBar                 m_progressBar;          //ProgressBar ActivityIndicator


    /**
     *  GETTER/SETTER
     */
    public ArrayList<BT_Device> getDeviceList() {return m_BTDevicesArrayList;}
    public void setScanActive(boolean isActive){
        m_scanActive = isActive;
        m_progressBar.setIndeterminate(isActive);
        if(isActive)
        {
            m_progressBar.setVisibility(View.VISIBLE);
        }
        else
        {
            m_progressBar.setVisibility(View.INVISIBLE);
        }

        m_scanButton.setEnabled(!isActive);
    }

    public void clearDeviceList()
    {
        m_BTDevicesArrayList.clear();
        m_adapter = new ListAdapter_BTLE_Devices(getActivity(), R.layout.btle_device_list_item, m_BTDevicesArrayList);
        m_listView.setAdapter(m_adapter);
    }
    public void addDevice(final BT_Device device)
    {
        Log.d(TAG, "addDevice: " + device.m_device.getName() + ": " + device.m_device.getAddress());

        if(!m_BTDevicesArrayList.removeIf(n -> (n.m_device.getAddress().equals(device.m_device.getAddress()))))
        {
            Toast.makeText(getActivity(), "Device found: " + device.m_device.getAddress(), Toast.LENGTH_SHORT).show();
        }

        m_BTDevicesArrayList.add(device);
        m_adapter = new ListAdapter_BTLE_Devices(getActivity(), R.layout.btle_device_list_item, m_BTDevicesArrayList);
        m_listView.setAdapter(m_adapter);
    }
    public void removeDevice(final BT_Device device)
    {
        Log.d(TAG, "removeDevice: " + device.m_device.getName() + ": " + device.m_device.getAddress());
        if(m_BTDevicesArrayList.removeIf(n -> (n.m_device.getAddress().equals(device.m_device.getAddress()))))
        {
            m_adapter = new ListAdapter_BTLE_Devices(getActivity(), R.layout.btle_device_list_item, m_BTDevicesArrayList);
            m_listView.setAdapter(m_adapter);
        }
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
        m_scanButton = v.findViewById(R.id.connection_fragment_scanButton);
        m_listView = v.findViewById(R.id.connection_fragment_listView);
        m_progressBar = v.findViewById(R.id.connection_fragment_activityIndicator);

        /**
         * Init UI-Items
         */
        m_listView.setAdapter(m_adapter);
        m_listView.setOnItemClickListener(m_itemClickListener);
        m_progressBar.setIndeterminate(false);
        m_progressBar.setVisibility(View.INVISIBLE);
        m_scanButton.setEnabled(false);
        m_scanButton.setOnClickListener(m_buttonClickListener);

        return v;
    }

    /**
     *   UI Adapters
     */
    private AdapterView.OnItemClickListener m_itemClickListener = (adapterView, view, i, l) -> {
        if(m_scanActive) m_Callback.stopScan(); //Aktuellen Scan stoppen

        /**
         *   Item, auf das geklickt wurde, verarbeiten: (Position in Liste: i)
         *   -> Mit Device verbinden
         */
        BT_Device device = m_BTDevicesArrayList.get(i);
        if(device.m_device.getAddress().isEmpty()) {
            Log.i(TAG, "Adress of Device is empty!");
            Toast.makeText(getActivity(), "Failed to Connect, Deviceadress is empty!", Toast.LENGTH_SHORT).show();
        }
        else {
            m_Callback.connectToDevice(device);
        }
    };
    private AdapterView.OnClickListener m_buttonClickListener = (View v) -> {
        if ( v.getId() == R.id.connection_fragment_scanButton) {
            m_Callback.startScan();
        }
    };
}
