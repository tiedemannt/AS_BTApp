package de.fhmue.tobxtreme.v2;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.util.Calendar;

public class HomeFragment extends Fragment {

    public interface HomeFragmentInterface
    {
        void switchToSettingsFragment();
        BT_FSM_DataRead getBTFsm();
    }
    HomeFragmentInterface m_CallBack;

    /**
     * Interface von ViewFragment nutzen
     */
    ViewFragment.ViewFragmentInterface m_viewFragmentCallback;

    /**
     * OBJECTS
     */
    private ImageButton     m_settingsButton;   //Settings-Button
    private ImageButton     m_disconnectButton; //Disconnect-Button
    private ImageView       m_sun;              //Zustand-Indicator Sonne
    private GraphView       m_graphView;        //Graphview für Live-Datenanzeige
    private TextView        m_temperature;      //Textanzeige Temperatur
    private TextView        m_Pressure;         //Textanzeige Druck
    private TextView        m_Feuchte;          //Textanzeige Feuchte
    private TextView        m_voc;              //Textanzeige VOC
    private TextView        m_co2;              //Textanzeige CO2
    private ImageButton     m_plotTemperature;  //Button Plot Temperature
    private ImageButton     m_plotPressure;     //Button Plot Pressure
    private ImageButton     m_plotFeuchte;      //Button Plot Feuchte
    private ImageButton     m_plotVOC;          //Button Plot VOC
    private ImageButton     m_plotCo2;          //Button Plot CO2

    //Data:
    boolean                     m_isBright;
    LineGraphSeries<DataPoint>  m_lineGraphSeries;

    /**
     * Constants
     */
    private static final String TAG = "HomeFragment";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        /**
         * Assign Objects
         */
        m_settingsButton = v.findViewById(R.id.homefragment_adjustbutton);
        m_disconnectButton = v.findViewById(R.id.homefragment_disconnectButton);
        m_graphView = v.findViewById(R.id.homefragment_graphview);
        m_sun = v.findViewById(R.id.homefragment_sun);
        m_temperature = v.findViewById(R.id.homeview_temperatur);
        m_Pressure = v.findViewById(R.id.homeview_druck);
        m_Feuchte = v.findViewById(R.id.homeview_feuchte);
        m_voc = v.findViewById(R.id.homeview_voc);
        m_co2 = v.findViewById(R.id.homeview_co2);
        m_plotTemperature = v.findViewById(R.id.homeview_buttonTemperatur);
        m_plotPressure = v.findViewById(R.id.homeview_buttonDruck);
        m_plotFeuchte = v.findViewById(R.id.homeview_buttonFeuchte);
        m_plotVOC = v.findViewById(R.id.homeview_buttonVOC);
        m_plotCo2 = v.findViewById(R.id.homeview_buttonCO2);

        m_settingsButton.setOnClickListener(m_clickListener);
        m_disconnectButton.setOnClickListener(m_clickListener);
        m_plotTemperature.setOnClickListener(m_clickListener);
        m_plotPressure.setOnClickListener(m_clickListener);
        m_plotFeuchte.setOnClickListener(m_clickListener);
        m_plotVOC.setOnClickListener(m_clickListener);
        m_plotCo2.setOnClickListener(m_clickListener);


        //Init Data Objects:
        m_isBright = false;
        m_lineGraphSeries  = new LineGraphSeries<>();

        //Init GraphView Object
        m_graphView.getViewport().setScalable(true);        //Manuelles scrollen
        m_graphView.getViewport().setScalableY(true);
        m_graphView.getViewport().setScrollable(true);
        m_graphView.getViewport().setScrollableY(true);

        m_graphView.getLegendRenderer().setVisible(false);
        m_graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        m_graphView.getLegendRenderer().setBackgroundColor(Color.LTGRAY);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if ((context instanceof ConnectionFragment.ConnectionFragmentInterface)
                && (context instanceof HomeFragment.HomeFragmentInterface))
        {
            m_viewFragmentCallback = (ViewFragment.ViewFragmentInterface) context;
            m_CallBack = (HomeFragmentInterface) context;
        }
        else {
            throw new ClassCastException(context.toString()
                    + " must implement ViewFragment.ViewFragmentInterface and HomeFragment.HomeFragmentInterface");
        }
    }
    @Override
    public void onDetach()
    {
        super.onDetach();
    }

    /**
     * Update zu einem Characteristic Value empfangen
     * @param characteristic
     */
    public void displayCharacteristicValue(BluetoothGattCharacteristic characteristic)
    {
        //Display Value on UI Thread
        getActivity().runOnUiThread(() -> {
            switch (characteristic.getUuid().toString())
            {
                case ViewFragment.UUID_CHARACTERISTIC_BRIGHT:
                {
                    int isbright = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    if(isbright == 1 && !m_isBright) {
                        m_isBright = true;
                        m_sun.setVisibility(View.VISIBLE);
                    }
                    else if(isbright == 0 && m_isBright) {
                        m_isBright = false;
                        m_sun.setVisibility(View.INVISIBLE);
                    }
                    else {
                        //Tue nichts
                    }
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_TEMPERATURE:
                {
                    m_temperature.setText(
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0) + " °C");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_VOC:
                {
                    m_voc.setText(
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0) + " ppb");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_CO2:
                {
                    m_co2.setText(
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0) + " ppm");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_HUMIDITY:
                {
                    m_Feuchte.setText(
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) + " %");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_PRESSURE:
                {
                    m_Pressure.setText(
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0) + " mBar");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_SETTING_OUTPUTACT:
                {
                    //Ignore
                    break;
                }
                default:
                {
                    //Log.i(TAG, "Ungültige Characteristic empfangen. + " + characteristic.getUuid().toString());
                    break;
                }
            }

        });
    }

    public AdapterView.OnClickListener m_clickListener = (View view) -> {
        switch(view.getId())
        {
            case R.id.homefragment_disconnectButton: {
                Log.i(TAG, "AdapterView.OnClickListener - homefragment_disconnectButton clicked.");
                m_viewFragmentCallback.disconnectFromDevice();
                break;
            }
            case R.id.homefragment_adjustbutton: {
                Log.i(TAG, "AdapterView.OnClickListener - homefragment_adjustbutton clicked.");
                m_CallBack.switchToSettingsFragment();
                break;
            }
            case R.id.homeview_buttonTemperatur:
            {
                m_CallBack.getBTFsm().startReadProcess(BT_FSM_DataRead.FSM_SENSORPROPERTY.SENSORPROPERTY_TEMPERATURE);
                break;
            }
            case R.id.homeview_buttonFeuchte:
            {
                m_CallBack.getBTFsm().startReadProcess(BT_FSM_DataRead.FSM_SENSORPROPERTY.SENSORPROPERTY_HUMIDITY);
                break;
            }
            case R.id.homeview_buttonDruck:
            {
                m_CallBack.getBTFsm().startReadProcess(BT_FSM_DataRead.FSM_SENSORPROPERTY.SENSORPROPERTY_PRESSURE);
                break;
            }
            case R.id.homeview_buttonCO2:
            {
                m_CallBack.getBTFsm().startReadProcess(BT_FSM_DataRead.FSM_SENSORPROPERTY.SENSORPROPERTY_CO2);
                break;
            }
            case R.id.homeview_buttonVOC:
            {
                m_CallBack.getBTFsm().startReadProcess(BT_FSM_DataRead.FSM_SENSORPROPERTY.SENSORPROPERTY_VOC);
                break;
            }
            default:
            {
                Log.i(TAG, "AdapterView.OnClickListener - No Reaction.");
                break;
            }
        }
    };
    public void fsmReadProcessFinished(BT_FSM_DataRead.ReadProcessData readData)
    {
        Log.d(TAG, "fsmReadProcessFinished() called!");

        boolean acceptData = true;
        
        switch (readData.c_dataType)
        {
            case SENSORPROPERTY_TEMPERATURE:
            {
                m_lineGraphSeries = new LineGraphSeries<>();
                m_lineGraphSeries.setTitle  ("Temperatur [°C]");
                break;
            }
            case SENSORPROPERTY_HUMIDITY:
            {
                m_lineGraphSeries = new LineGraphSeries<>();
                m_lineGraphSeries.setTitle     ("Feuchte [%]");
                break;
            }
            case SENSORPROPERTY_PRESSURE:
            {
                m_lineGraphSeries = new LineGraphSeries<>();
                m_lineGraphSeries.setTitle     ("Druck [mBar]");
                break;
            }
            case SENSORPROPERTY_CO2:
            {
                m_lineGraphSeries = new LineGraphSeries<>();
                m_lineGraphSeries.setTitle          ("CO2 [ppm]");
                break;
            }
            case SENSORPROPERTY_VOC:
            {
                m_lineGraphSeries = new LineGraphSeries<>();
                m_lineGraphSeries.setTitle          ("VOC [ppb]");
                break;
            }
            default:
            {
                Log.d(TAG, "fsmReadProcessFinished: Unbekannter Datentyp!");
                acceptData = false;
                break;
            }
        }

        if(acceptData)
        {
            m_lineGraphSeries.setColor          (Color.BLUE);
            m_lineGraphSeries.setDrawDataPoints(true);
            m_lineGraphSeries.setDataPointsRadius(5);
            m_lineGraphSeries.setThickness(4);
            m_lineGraphSeries.setDrawBackground(false);
            m_lineGraphSeries.setDrawAsPath(false);
            m_lineGraphSeries.setBackgroundColor(android.R.color.holo_blue_bright);

            //Daten überführen:
            for(DataPoint point : readData.c_dataPoints)
            {
                m_lineGraphSeries.appendData(point, false, readData.c_dataPoints.size());
            }
            m_graphView.removeAllSeries();
            m_graphView.addSeries(m_lineGraphSeries);

            switch (readData.c_dataType)
            {
                case SENSORPROPERTY_TEMPERATURE:
                {
                    m_graphView.setTitle("Temperaturen [°C]");
                    break;
                }
                case SENSORPROPERTY_HUMIDITY:
                {
                    m_graphView.setTitle("Feuchtigkeiten [%]");
                    break;
                }
                case SENSORPROPERTY_PRESSURE:
                {
                    m_graphView.setTitle("Luftdrücke [mBar]");
                    break;
                }
                case SENSORPROPERTY_CO2:
                {
                    m_graphView.setTitle("CO2 Konzentrationen [ppm]");
                }
                case SENSORPROPERTY_VOC:
                {
                    m_graphView.setTitle("VOC Konzentrationen [ppb]");
                }
            }
            //GGF: Hier noch aufsplitten nach Properties
            //m_graphView.getViewport().setMinY(0.0);
            //m_graphView.getViewport().setMaxY(30.0);
        }
    }
}

