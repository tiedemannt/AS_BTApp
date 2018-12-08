package de.fhmue.tobxtreme.v2;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import java.util.ArrayList;


public class ListAdapter_BTLE_Devices extends ArrayAdapter<BT_Device> {

    Activity activity;
    int layoutResourceID;
    ArrayList<BT_Device> devices;

    public ListAdapter_BTLE_Devices(Activity activity, int resource, ArrayList<BT_Device> objects) {
        super(activity.getApplicationContext(), resource, objects);

        this.activity = activity;
        layoutResourceID = resource;
        devices = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
        {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        BluetoothDevice device = devices.get(position).m_device;
        String name = device.getName();
        String address = device.getAddress();
        int rssi = devices.get(position).m_rssid;

        TextView tv = null;

        tv = convertView.findViewById(R.id.tv_name);
        if (name != null && name.length() > 0) {
            tv.setText(device.getName());
        }
        else {
            tv.setText("No Name");
        }

        tv = convertView.findViewById(R.id.tv_macaddr);
        if (address != null && address.length() > 0) {
            tv.setText(device.getAddress());
        }
        else {
            tv.setText("No Address");
        }

        tv = convertView.findViewById(R.id.tv_rssid);
        if(rssi != 0)
        {
            tv.setText("" + rssi);
        }
        else
        {
            tv.setText("?");
        }


        return convertView;
    }
}