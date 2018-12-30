package de.fhmue.tobxtreme.v2;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;



public class ListAdapter_BTLE_Services extends ArrayAdapter<BluetoothGattCharacteristic> {
    public Activity activity;
    public int layoutResourceID;
    public BluetoothGatt services;
    public ArrayList<BluetoothGattCharacteristic>  m_characteristics;
    public ArrayList<MapItem>                      m_itemsToMap;

    public ListAdapter_BTLE_Services(Activity activity,
                                     int resource,
                                     BluetoothGatt gattObj,
                                     ArrayList<MapItem> mapData) {
        super(activity.getApplicationContext(), resource, getCharacteristics(gattObj));

        this.activity = activity;
        layoutResourceID = resource;
        services = gattObj;
        m_characteristics = getCharacteristics(gattObj);
        m_itemsToMap = mapData;
    }


    public static ArrayList<BluetoothGattCharacteristic> getCharacteristics(BluetoothGatt gattObj)
    {
        ArrayList<BluetoothGattCharacteristic> list = new ArrayList();

        for(BluetoothGattService service : gattObj.getServices())
        {
            for(BluetoothGattCharacteristic characteristic : service.getCharacteristics())
            {
                list.add(characteristic);
            }
        }

        return list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
        {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        String cuuid = m_characteristics.get(position).getUuid().toString();
        String suuid = m_characteristics.get(position).getService().getUuid().toString();

        TextView tv = null;

        tv = convertView.findViewById(R.id.service_tv_name);
        if (!cuuid.isEmpty()) {
            String text = cuuid;
            for(MapItem item : m_itemsToMap)
            {
                if(item.uuid.equals(cuuid)) text = item.text;
            }
            tv.setText(text);
        }
        else {
            tv.setText("Characteristic " + position);
        }

        tv = convertView.findViewById(R.id.service_tv_info);
        if (!suuid.isEmpty()) {
            String text = suuid;
            for(MapItem item : m_itemsToMap)
            {
                if(item.uuid.equals(suuid)) text = item.text;
            }
            tv.setText(text);
        }
        else {
            tv.setText("No UUID found");
        }

        return convertView;
    }
}
