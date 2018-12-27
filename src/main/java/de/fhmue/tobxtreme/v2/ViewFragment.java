package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


public class ViewFragment extends Fragment {
    /**
     * INTERFACE
     */
    public interface ViewFragmentInterface
    {
        void subscribeToCharacteristic(BluetoothGattCharacteristic characteristic);
        void unsubscribeAllCharacteristics();
        void disconnectFromDevice();
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


    /**
     * CONSTANTS
     */
    private final String TAG = "ViewFragment";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_view, container, false);

        m_listView = v.findViewById(R.id.view_fragment_listview);
        m_textView = v.findViewById(R.id.view_fragment_textView);
        m_disconnectButton = v.findViewById(R.id.view_fragment_disconnect_button);

        m_listView.setOnItemClickListener(m_itemClickListener);
        m_disconnectButton.setOnClickListener(m_clickListener);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ConnectionFragment.ConnectionFragmentInterface) {
            m_Callback = (ViewFragment.ViewFragmentInterface) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ConnectionFragment.ConnectionFragmentInterface");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
    }


    public void addService(BluetoothGatt newService){
        Log.i("ViewFragment", "addService(): Setting BluetoothGatt Element");
        m_btGatt = newService;


        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("ViewFragment", "addService(): Setting List Adapter on UI Thread.");

                m_adapter = new ListAdapter_BTLE_Services(getActivity(), R.layout.btle_service_list_item, m_btGatt);
                m_listView.setAdapter(m_adapter);
            }
        });

    }

    public void displayCharacteristicValue(BluetoothGattCharacteristic characteristic)
    {
        //save characteristic
        m_characteristic = characteristic;

        //Display Value on UI Thread
        getActivity().runOnUiThread(() -> m_textView.setText("" +
                m_characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0)));
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
