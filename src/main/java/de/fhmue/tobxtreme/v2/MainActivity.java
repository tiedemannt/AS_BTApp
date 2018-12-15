package de.fhmue.tobxtreme.v2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
//import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.bluetooth.le.ScanCallback;
import android.widget.Toast;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity

    /**
     *  IMPLEMENTS
     */
    implements ConnectionFragment.ConnectionFragmentInterface
{

    /**
     * CONSTANTS
     */
    private static final String TAG = "MainActivity";
    private static final long CONST_SCAN_PERIOD = 10000;  //Dauer für den Scanvorgang


    /**
     *   OBJECTS
     */
    private Fragment            m_activeFragment;
    private BluetoothAdapter    m_btadapter;
    private Handler             m_handler;


    /**
     *   OnCreate Function
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bottomnavigationview einrichten:
        //BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        //bottomNav.setOnNavigationItemSelectedListener(navListener);

        //Connection Fragment bei Start öffnen:
        getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container, new ConnectionFragment()).commit();

        //Titel der Activity setzen
        setTitle("LUFTGÜTESENSORIK");

        m_btadapter = BluetoothAdapter.getDefaultAdapter();

        //startScan(); //-> Funktioniert auf Emulator nicht


        Log.d(TAG, "MainActivity::onCreate(): finished.");
    }

//    /**
//     * Handler für BottomnavigationView
//     */
//    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
//            new BottomNavigationView.OnNavigationItemSelectedListener()
//            {
//                @Override
//                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
//                    Fragment selectedFragment = null;
//
//                    switch (menuItem.getItemId()) {
//                        case R.id.nav_home:
//                            selectedFragment = new HomeFragment();
//                            Log.d(TAG, "onNavigationItemSelected(): HomeFragment selected.");
//                            break;
//                        case R.id.nav_connection:
//                            selectedFragment = new ConnectionFragment();
//                            Log.d(TAG, "onNavigationItemSelected(): ConnectionFragment selected.");
//                            break;
//                        case R.id.nav_view:
//                            selectedFragment = new ViewFragment();
//                            Log.d(TAG, "onNavigationItemSelected(): ViewFragment selected.");
//                            break;
//                    }
//
//                    m_activeFragment = selectedFragment;
//                    getSupportFragmentManager().beginTransaction().replace(
//                            R.id.fragment_container, selectedFragment).commit();
//
//                    return true; //Item soll selected werden
//                }
//            };


    /**
     *   Interface Funktionen mit Connection Fragment
     */
    public void startScan()
    {
        Log.d(TAG, "startScan(): called.");

        if(m_btadapter == null)
        {
            //Emuliert oder BT-Adapter nicht gefunden?
            throw new RuntimeException("BluetoothAdapter : NullObject.");
        }

        if(!m_btadapter.isEnabled())
        {
            Log.d(TAG, "startScan(): Enabling Bluetooth...");
            //Toast.makeText(this, "Enabling Bluetooth...", Toast.LENGTH_SHORT).show();

            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTintent);

            IntentFilter btActFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBluetoothEnabledReceiver, btActFilter);

            return;
        }

        if(m_activeFragment instanceof ConnectionFragment)
        {
            ((ConnectionFragment)m_activeFragment).clearDeviceList();

            //Check BT Permissions Manifest
            checkBTPermissions();

            Log.d(TAG, "startScan(): Starting Discovery.");

            //LE Scan Start
            m_btadapter.getBluetoothLeScanner().startScan(mLeScanCallback);
            ((ConnectionFragment)m_activeFragment).setScanActive(true);

            m_handler.postDelayed(()-> stopScan(), CONST_SCAN_PERIOD);

            Toast.makeText(this, "Starting Discovery...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            throw new RuntimeException("Connection Fragment must be active.");
        }
    }
    private BroadcastReceiver mBluetoothEnabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive(): ACTION STATE CHANGED.");

            if(action.equals(m_btadapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, m_btadapter.ERROR);

                switch(state)
                {
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive(): STATE_ON.");
                        startScan();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive(): STATE_TURNING_ON.");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive(): STATE_TURNING_OFF.");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive(): STATE_OFF.");
                        break;
                    default:
                        Log.d(TAG, "onReceive(): default?");
                        break;
                }
            }
        }
    };
    public void stopScan()
    {
        Log.d(TAG, "stopScan(): called.");

        m_btadapter.getBluetoothLeScanner().stopScan(mLeScanCallback);

        if(m_activeFragment instanceof ConnectionFragment) {
            ((ConnectionFragment) m_activeFragment).setScanActive(false);
        }
    }
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST)
            {
                //Match lost
                Log.d(TAG, "onScanResult: lost Device " + result.toString());
                Toast.makeText(getApplicationContext(), "Lost: " + result.getDevice().getAddress(), Toast.LENGTH_SHORT).show();
                ((ConnectionFragment)m_activeFragment).removeDevice(new BT_Device(result.getDevice(), result.getRssi()));
            }
            else
            {
                //New Device found?
                if(!((ConnectionFragment)m_activeFragment).getDeviceList().contains(new BT_Device(result.getDevice(), result.getRssi())))
                {
                    Log.d(TAG, "onScanResult: found Device " + result.toString());

                    if((m_activeFragment instanceof ConnectionFragment)){
                        BT_Device newDevice = new BT_Device(result.getDevice(), result.getRssi());
                        ((ConnectionFragment)m_activeFragment).addDevice(newDevice);
                    }
                    else {
                        Log.d(TAG, "onReceive(): ConnectionFragment not active.");
                        m_btadapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                    }
                }
                else
                {
                    //Device bereits in Liste
                    Log.d(TAG, "onScanResult: Device bereits in Liste.");
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "onScanFailed: " + errorCode);
            Toast.makeText(getApplicationContext(), "BTLE Scan Failed: " + errorCode, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: ...");
            Toast.makeText(getApplicationContext(), "onBatchScanResults: ..." , Toast.LENGTH_SHORT).show();
        }
    };


    /**
     *   Utils
     */
    private void checkBTPermissions()
    {
        Log.d(TAG, "checkBTPermissions(): Called.");
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0)
            {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        }
        else
        {
            Log.d(TAG, "checkBTPermissions(): SDK < Lollipop.");
        }
    }
}
