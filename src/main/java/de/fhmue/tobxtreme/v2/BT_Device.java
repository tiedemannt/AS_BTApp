package de.fhmue.tobxtreme.v2;


import android.bluetooth.BluetoothDevice;



public class BT_Device{
    public int             m_rssid;
    public BluetoothDevice m_device;


    public BT_Device(BluetoothDevice device, int rssid) {
        m_device = device;
        m_rssid = rssid;
    }
}
