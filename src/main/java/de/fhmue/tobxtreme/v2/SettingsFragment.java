package de.fhmue.tobxtreme.v2;

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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsFragment extends Fragment {

    public interface Interface
    {
        void reSwitchToHomeFragment();
        LGS_BluetoothFSM getFSM();
    }
    Interface m_interface;

    /**
     * Constants
     */
    private static final String TAG = "SettingsFragment";

    /**
     * Objects
     */
    private ImageButton     m_returnButton;     //Button Back To HomeFragment
    private ImageButton     m_syncButton;       //Button Sync
    private EditText        m_editRepRate;      //Edit Field Rep Rate
    //Aktuelle Werte
    private TextView        m_aktTemperatur;    //Aktuelle Temperatur (rot)
    private TextView        m_aktPressure;      //Aktueller Luftdruck (rot)
    private TextView        m_aktCo2;           //Aktuelle CO2 Konzentration (rot)
    private TextView        m_aktVoc;           //Aktuelle VOC Konzentration (rot)
    private TextView        m_aktHumidity;      //Aktuelle Luftfeuchte (rot)
    //Kritische Werte Edits
    private EditText        m_editCriticTemperature;
    private EditText        m_editCriticPressure;
    private EditText        m_editCriticCo2;
    private EditText        m_editCriticHumidity;
    private EditText        m_editCriticVoc;
    //Indicators Ausgang
    private ImageView       m_outputactiveImage;
    private ImageView       m_outputinactiveImage;

    //Value Objects
    private int             m_aktTempValue, m_aktPressureValue, m_aktCo2Value, m_aktVocValue,
                            m_aktHumidityValue;
    //Aktuelle Konfigurationswerte Periphal
    private boolean         m_isOutputActive;
    private int             m_aktreprate;
    private int             m_aktcriticTemp, m_aktcriticPressure, m_aktcriticCo2,
                            m_aktcriticHumidity, m_aktcriticVoc;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        m_returnButton = v.findViewById(R.id.settingsfragment_backButton);
        m_syncButton = v.findViewById(R.id.settingsfragment_syncButton);
        m_editRepRate = v.findViewById(R.id.settingsfragment_reprate_edit);
        m_aktCo2 = v.findViewById(R.id.settingsfragment_aktco2);
        m_aktHumidity = v.findViewById(R.id.settingsfragment_akthumidity);
        m_aktPressure = v.findViewById(R.id.settingsfragment_aktpressure);
        m_aktTemperatur = v.findViewById(R.id.settingsfragment_akttemp);
        m_aktVoc = v.findViewById(R.id.settingsfragment_aktvoc);
        m_editCriticTemperature = v.findViewById(R.id.settingsfragment_criticTemp_edit);
        m_editCriticPressure = v.findViewById(R.id.settingsfragment_criticPressure_edit);
        m_editCriticCo2 = v.findViewById(R.id.settingsfragment_criticco2_edit);
        m_editCriticHumidity = v.findViewById(R.id.settingsfragment_criticHumidity_edit);
        m_editCriticVoc = v.findViewById(R.id.settingsfragment_criticVOC_edit);
        m_outputactiveImage = v.findViewById(R.id.settingsfragment_outputactive);
        m_outputinactiveImage = v.findViewById(R.id.settingsfragment_outputinactive);

        m_returnButton.setOnClickListener(m_clickListener);
        m_syncButton.setOnClickListener(m_clickListener);

        m_outputactiveImage.setVisibility(View.INVISIBLE);
        m_outputinactiveImage.setVisibility(View.INVISIBLE);


        //Daten anfragen
        m_isOutputActive = false;
        m_interface.getFSM().switchedToSettingsFragment();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SettingsFragment.Interface) {
            m_interface = (Interface) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement SettingsFragment.SettingsFragmentInterface");
        }
    }
    @Override
    public void onDetach()
    {
        super.onDetach();
    }

    /**
     * Handelt Read-Request Antworten vom Remote Device
     * @param characteristic
     */
    public void handleReadRequestAnswer(BluetoothGattCharacteristic characteristic)
    {
        //Display Value on UI Thread
        getActivity().runOnUiThread(() -> {
            if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_SETTING_REPRATE))
            {
                Log.i(TAG, "Update received: UUID_CHARACTERISTIC_SETTING_REPRATE");
                m_aktreprate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                m_editRepRate.setText("" + m_aktreprate);
                m_editRepRate.setEnabled(true);
            }
            else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITTEMP))
            {
                Log.i(TAG, "Update received: UUID_CHARACTERISTIC_SETTING_CRITTEMP");
                m_aktcriticTemp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                m_editCriticTemperature.setText("" + m_aktcriticTemp);
                m_editCriticTemperature.setEnabled(true);
            }
            else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITPRES))
            {
                Log.i(TAG, "Update received: UUID_CHARACTERISTIC_SETTING_CRITPRES");
                m_aktcriticPressure = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                m_editCriticPressure.setText("" + m_aktcriticPressure);
                m_editCriticPressure.setEnabled(true);
            }
            else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITCO2))
            {
                Log.i(TAG, "Update received: UUID_CHARACTERISTIC_SETTING_CRITCO2");
                m_aktcriticCo2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                m_editCriticCo2.setText("" + m_aktcriticCo2);
                m_editCriticCo2.setEnabled(true);
            }
            else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITHUM))
            {
                Log.i(TAG, "Update received: UUID_CHARACTERISTIC_SETTING_CRITHUM");
                m_aktcriticHumidity = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                m_editCriticHumidity.setText("" + m_aktcriticHumidity);
                m_editCriticHumidity.setEnabled(true);
            }
            else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_SETTING_CRITVOC))
            {
                Log.i(TAG, "Update received: UUID_CHARACTERISTIC_SETTING_CRITVOC");
                m_aktcriticVoc = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                m_editCriticVoc.setText("" + m_aktcriticVoc);
                m_editCriticVoc.setEnabled(true);
            }
            else
            {
                    Log.i(TAG, "handleReadRequestAnswer() - Characteristic ignoriert.");
            }
        });
    }

    /**
     * Handelt Write-Request Antworten vom Remote Device
     * @param characteristic
     * @param status
     */
    public void handleWriteRequestAnswer(BluetoothGattCharacteristic characteristic, int status)
    {
        switch(status)
        {
               case 0x0: //OK, alternativ Konstante BluetoothGatt.GATT_SUCCESS
               {
                   Log.i(TAG, "handleWriteRequestAnswer() - Value accepted!");
                   m_interface.getFSM().requestReadCharacteristic(characteristic); //Settings Daten neu requesten
                   break;
               }
               case 0x10: //Unknown Attribute
               {
                   Log.e(TAG, "handleWriteRequestAnswer() - Unknown Attribute! Status: " + status);
                   getActivity().runOnUiThread(() -> {
                           Toast.makeText(getActivity(),
                                   "Fehler 0x10!", Toast.LENGTH_LONG).show();
                       });
                   break;
               }
               case 0x11: //Length NOK
               {
                   Log.e(TAG, "handleWriteRequestAnswer() - Length not OK (must be 4)! Status: " + status);
                   getActivity().runOnUiThread(() -> {
                       Toast.makeText(getActivity(),
                               "Fehler 0x11!", Toast.LENGTH_LONG).show();
                   });
                   break;
               }
                case 0x12: //Value NOK
               {
                   Log.e(TAG, "handleWriteRequestAnswer() - requested Value to write not OK" +
                           "(must in value range)! Status: " + status);
                   getActivity().runOnUiThread(() -> {
                       Toast.makeText(getActivity(),
                               "Fehler 0x12!", Toast.LENGTH_LONG).show();
                   });
                   break;
               }
               default:
               {
                   Log.e(TAG, "handleWriteRequestAnswer() - Status unbekannt! Status: " + status);
                   getActivity().runOnUiThread(() -> {
                       Toast.makeText(getActivity(),
                               "Unbekannter Fehler!", Toast.LENGTH_LONG).show();
                   });
                   break;
               }
           }
    }

    public void handleCharacteristicUpdate(BluetoothGattCharacteristic characteristic)
    {
        if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_TEMPERATURE))
        {
            //Display Temperatur
            getActivity().runOnUiThread(() -> {
                m_aktTempValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                String temptext = ("(" + m_aktTempValue + " °C)");
                m_aktTemperatur.setText(temptext);
            });
        }
        else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_PRESSURE))
        {
            //Display Pressure
            getActivity().runOnUiThread(() -> {
                m_aktPressureValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                String temptext = ("(" + m_aktPressureValue + " mBar)");
                m_aktPressure.setText(temptext);
            });
        }
        else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_CO2))
        {
            //Display CO2
            getActivity().runOnUiThread(() -> {
                m_aktCo2Value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                String temptext = ("(" + m_aktCo2Value + " ppm)");
                m_aktCo2.setText(temptext);
            });
        }
        else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_HUMIDITY))
        {
            //Display Humidity
            getActivity().runOnUiThread(() -> {
                m_aktHumidityValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                String temptext = ("(" + m_aktHumidityValue + " %)");
                m_aktHumidity.setText(temptext);
            });
        }
        else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_VOC))
        {
            //Display VOC
            getActivity().runOnUiThread(() -> {
                m_aktVocValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                String temptext = ("(" + m_aktVocValue + " ppb)");
                m_aktVoc.setText(temptext);
            });
        }
        else if(characteristic.getUuid().equals(LGS_Constants.UUID_CHARACTERISTIC_SETTING_OUTPUTACT))
        {
            getActivity().runOnUiThread(() -> {
                if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) == 0)
                {
                    m_isOutputActive = false;
                    m_outputinactiveImage.setVisibility(View.VISIBLE);
                    m_outputactiveImage.setVisibility(View.INVISIBLE);
                }
                else {
                    m_isOutputActive = true;
                    m_outputinactiveImage.setVisibility(View.INVISIBLE);
                    m_outputactiveImage.setVisibility(View.VISIBLE);
                }
            });
        }
        else {
            //Bright oder Default: Ignorieren
        }
    }


    public AdapterView.OnClickListener m_clickListener = (View view) -> {
        switch(view.getId())
        {
            case R.id.settingsfragment_backButton:
            {
                Log.i(TAG, "AdapterView.OnClickListener - settingsfragment_backButton clicked.");
                m_interface.reSwitchToHomeFragment();
                break;
            }
            case R.id.settingsfragment_syncButton:
            {
                Log.i(TAG, "AdapterView.OnClickListener - settingsfragment_syncButton clicked.");

                LGS_BluetoothInterface btinterface = m_interface.getFSM().getBtInterface();
                if(btinterface != null)
                {
                    //Reprate changed?
                    if(m_aktreprate != Integer.parseInt(m_editRepRate.getText().toString())) {
                        btinterface
                                .writeRepRate(Integer.parseInt(m_editRepRate.getText().toString()));
                        m_editRepRate.setEnabled(false);
                    }

                    //Critical Temperature changed?
                    if(m_aktcriticTemp != Integer.parseInt(m_editCriticTemperature.getText().toString())) {
                        btinterface
                                .writeCriticTemperature(Integer.parseInt(m_editCriticTemperature.getText().toString()));
                        m_editCriticTemperature.setEnabled(false);
                    }

                    //Critical Pressure changed?
                    if(m_aktcriticPressure != Integer.parseInt(m_editCriticPressure.getText().toString())) {
                        btinterface
                                .writeCriticPressure(Integer.parseInt(m_editCriticPressure.getText().toString()));
                        m_editCriticPressure.setEnabled(false);
                    }

                    //Critical CO2 changed?
                    if(m_aktcriticCo2 != Integer.parseInt(m_editCriticCo2.getText().toString())) {
                        btinterface
                                .writeCriticCO2(Integer.parseInt(m_editCriticCo2.getText().toString()));
                        m_editCriticCo2.setEnabled(false);
                    }

                    //Critical Humidity changed?
                    if(m_aktcriticHumidity != Integer.parseInt(m_editCriticHumidity.getText().toString())) {
                        btinterface
                                .writeCriticHumidity(Integer.parseInt(m_editCriticHumidity.getText().toString()));
                        m_editCriticHumidity.setEnabled(false);
                    }

                    //Critical VOC changed?
                    if(m_aktcriticVoc != Integer.parseInt(m_editCriticVoc.getText().toString())) {
                        btinterface
                                .writeCriticVOC(Integer.parseInt(m_editCriticVoc.getText().toString()));
                        m_editCriticVoc.setEnabled(false);
                    }
                }
                else
                {
                    Log.e(TAG, "LGS_BluetoothInterface: Nullobjekt! Luschige Programmierung.");
                }
            }
            default:
            {
                Log.i(TAG, "AdapterView.OnClickListener - No Reaction.");
                break;
            }
        }
    };
}

