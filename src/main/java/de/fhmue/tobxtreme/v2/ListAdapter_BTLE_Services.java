package de.fhmue.tobxtreme.v2;

import android.app.Activity;
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
    List<BluetoothGattService> services;

    public ListAdapter_BTLE_Services(Activity activity, int resource, List<BluetoothGattService> objects) {
        super(activity.getApplicationContext(), resource, objects);

        this.activity = activity;
        layoutResourceID = resource;
        services = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
        {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        BluetoothGattService service = services.get(position);

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
