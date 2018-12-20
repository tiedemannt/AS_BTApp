package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;


public class ViewFragment extends Fragment {

    /**
     * OBJECTS
     */
    private List<BluetoothGattService>              m_serviceList;          //Service List Data
    private ListAdapter_BTLE_Services               m_adapter;              //Listadapter
    private ListView                                m_listView;             //Listview Services



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_connection, container, false);

        m_listView = v.findViewById(R.id.view_fragment_listview);

        m_serviceList.clear(); //Clear Service List
        m_adapter = new ListAdapter_BTLE_Services(getActivity(), R.layout.btle_service_list_item, m_serviceList);
        m_listView.setAdapter(m_adapter);

        return v;
    }

    public void addService(BluetoothGatt newService){
        m_serviceList = newService.getServices();
        m_adapter = new ListAdapter_BTLE_Services(getActivity(), R.layout.btle_service_list_item, m_serviceList);
        m_listView.setAdapter(m_adapter);
    }
}
