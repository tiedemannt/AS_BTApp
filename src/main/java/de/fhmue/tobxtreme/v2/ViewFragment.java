package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;


public class ViewFragment extends Fragment {

    /**
     * OBJECTS
     */
    private BluetoothGatt                           m_btGatt;          //Service List Data
    private ListAdapter_BTLE_Services               m_adapter;              //Listadapter
    private ListView                                m_listView;             //Listview Services



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_view, container, false);

        m_listView = v.findViewById(R.id.view_fragment_listview);


        return v;
    }

    public void addService(BluetoothGatt newService){
        Log.i("ViewFragment", "addService()");

        for(int i = 0; i < newService.getServices().size(); i++)
            Log.i("ViewFragment", "Service " + i + "(Hashcode):" + newService.getServices().get(i).getUuid().toString());

        m_btGatt = newService;
        m_adapter = new ListAdapter_BTLE_Services(getActivity(), R.layout.btle_service_list_item, m_btGatt);
        m_listView.setAdapter(m_adapter);
    }
}
