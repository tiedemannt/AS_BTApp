package de.fhmue.tobxtreme.v2;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity

    /**
     *  IMPLEMENTS
     */
    implements
        ConnectionFragment.Interface, //startScan()
        HomeFragment.Interface,
        SettingsFragment.Interface,
        LGS_BluetoothFSM.Interface
{

    /**
     * CONSTANTS
     */
    private static final String TAG = "MainActivity";


    /**
     *   OBJECTS
     */
    //Fragment Object:
    private Fragment    m_activeFragment;

    //FSM: Data Read from LGS:
    LGS_BluetoothFSM     m_fsm;


    /**
     *   OnCreate Function
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: Begin!");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_activeFragment = new ConnectionFragment();
        getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container, m_activeFragment).commit();

        //Titel der Activity setzen
        setTitle("LUFTGÜTESENSORIK");

        //Init FSM
        m_fsm = new LGS_BluetoothFSM(this);

        Handler mHandler = new Handler();
        mHandler.postDelayed(() -> {
            m_fsm.startSearchForSensor();
            if(m_activeFragment instanceof ConnectionFragment){
                ((ConnectionFragment)m_activeFragment).setScanActive(true);
            }
        }, 200);


        Log.i(TAG, "onCreate: Success!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        m_fsm.disconnect();
    }


    /**
     *  -Methods implemented from Interface ConnectionFragmentInterface-
     *  ################################################################
     */
    public void startScan()
    {
        runOnUiThread(() -> {
            m_fsm.startSearchForSensor();
            if(m_activeFragment instanceof ConnectionFragment){
                ((ConnectionFragment)m_activeFragment).setScanActive(true);
            }
        });
    }


    /**
     * -Methods implemented from Interface HomeFragmentInterface-
     * ################################################################
     */
    public void switchToSettingsFragment()
    {
        runOnUiThread(() -> {
            if(!(m_activeFragment instanceof SettingsFragment))
            {
                Fragment nextFragment = new SettingsFragment();
                m_activeFragment = nextFragment;
                getSupportFragmentManager().beginTransaction().replace(
                        R.id.fragment_container, nextFragment).commit();
            }
        });
    }


    /**
     * -Methods implemented from Interface SettingsFragmentInterface-
     * ################################################################
     */
    public void reSwitchToHomeFragment()
    {
        runOnUiThread(() -> {
            if(!(m_activeFragment instanceof HomeFragment))
            {
                Log.i(TAG, "Switching to Home Fragment");

                Fragment nextFragment = new HomeFragment();
                m_activeFragment = nextFragment;
                getSupportFragmentManager().beginTransaction().replace(
                        R.id.fragment_container, nextFragment).commit();
            }
        });
    }
    public LGS_BluetoothFSM getFSM(){return m_fsm;}


    /**
     * -Methods implemented from Interface LGS_BluetoothFSM-
     * ################################################################
     */
    public void readProcessFinished(LGS_BluetoothFSM.ReadProcessData readData)
    {
        Log.d(TAG, "readProcessFinished() called!");
        if(m_activeFragment instanceof HomeFragment)
        {
            ((HomeFragment)m_activeFragment).fsmReadProcessFinished(readData);
        }
    }
    public void makeToast(String text)
    {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        });
    }
    public void scanProcessFinished(boolean isSensorFound){
        runOnUiThread(() -> {
            if(!isSensorFound){
                if(m_activeFragment instanceof ConnectionFragment){
                    ((ConnectionFragment)m_activeFragment).setScanActive(false);
                }
            }
            else{
                //Sensor wurde gefunden; Auto Connect
                if(m_activeFragment instanceof ConnectionFragment){
                    ((ConnectionFragment)m_activeFragment).setConnecting(true);
                }
            }
        });
    }
    public void connectProcessFinished(boolean isSensorConnected){
        runOnUiThread(() -> {
            if(!isSensorConnected)
            {
                if(m_activeFragment instanceof ConnectionFragment){
                    ((ConnectionFragment)m_activeFragment).setConnecting(false);
                }

                //Konnte nicht mit Sensor verbinden oder Verbindung abgebrochen
                if(!(m_activeFragment instanceof ConnectionFragment)){
                    Fragment nextFragment = new ConnectionFragment();
                    m_activeFragment = nextFragment;
                    getSupportFragmentManager().beginTransaction().replace(
                            R.id.fragment_container, nextFragment).commit();
                }
            }
            else
            {
                //Mit Sensor verbunden, wechsle zu Home Fragment
                if(!(m_activeFragment instanceof HomeFragment)){
                    Fragment nextFragment = new HomeFragment();
                    m_activeFragment = nextFragment;
                    getSupportFragmentManager().beginTransaction().replace(
                            R.id.fragment_container, nextFragment).commit();
                }
            }
        });
    }
    public void servicesFound(boolean isFound)
    {
        runOnUiThread(() -> {
            if(!isFound)
            {
                //Konnte nicht mit Sensor verbinden;
                if(!(m_activeFragment instanceof ConnectionFragment)){
                    Fragment nextFragment = new ConnectionFragment();
                    m_activeFragment = nextFragment;
                    getSupportFragmentManager().beginTransaction().replace(
                            R.id.fragment_container, nextFragment).commit();
                }
            }
        });
    }
    public void forwardNotification(BluetoothGattCharacteristic chara)
    {
        if(m_activeFragment instanceof HomeFragment)
        {
            ((HomeFragment)m_activeFragment).displayCharacteristicValue(chara);
        }
        else if(m_activeFragment instanceof SettingsFragment)
        {
            ((SettingsFragment)m_activeFragment).handleCharacteristicUpdate(chara);
        }
        else
        {
            Log.e(TAG, "forwardNotification: Code prüfen!");
        }
    }
    public void forwardReadResponse(BluetoothGattCharacteristic chara, int status)
    {
        if(m_activeFragment instanceof HomeFragment)
        {
            //Homefragment interessiert das nicht
        }
        else if(m_activeFragment instanceof SettingsFragment)
        {
            ((SettingsFragment)m_activeFragment).handleReadRequestAnswer(chara);
        }
        else
        {
            Log.e(TAG, "forwardReadResponse: Code prüfen!");
        }
    }
    public void forwardWriteResponse(BluetoothGattCharacteristic chara, int status)
    {
        if(m_activeFragment instanceof HomeFragment)
        {
            //Homefragment interessiert das nicht
        }
        else if(m_activeFragment instanceof SettingsFragment)
        {
            ((SettingsFragment)m_activeFragment).handleWriteRequestAnswer(chara, status);
        }
        else
        {
            Log.e(TAG, "forwardWriteResponse: Code prüfen!");
        }
    }


    public void checkPermissions()
    {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if(permissionCheck != 0)
        {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
    }
}
