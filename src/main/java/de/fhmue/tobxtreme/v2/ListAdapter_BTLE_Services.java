package de.fhmue.tobxtreme.v2;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.bluetooth.BluetoothGattService;

import java.util.List;


public class ListAdapter_BTLE_Services extends ArrayAdapter<BluetoothGattService> {
    Activity activity;
    int layoutResourceID;
    BluetoothGatt services;

    public ListAdapter_BTLE_Services(Activity activity, int resource, BluetoothGatt gattObj) {
        super(activity.getApplicationContext(), resource);

        this.activity = activity;
        layoutResourceID = resource;
        services = gattObj;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
        {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        BluetoothGattService service = services.getServices().get(position);

        TextView tv = null;

        tv = convertView.findViewById(R.id.service_tv_name);
        if (!service.toString().isEmpty()) {
            tv.setText(service.toString());
        }
        else {
            tv.setText("No Name");
        }


        return convertView;
    }
}
