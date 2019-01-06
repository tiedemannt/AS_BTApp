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
    LineGraphSeries<DataPoint>  m_dataTemperatureChar;
    LineGraphSeries<DataPoint>  m_dataVOCChar;
    LineGraphSeries<DataPoint>  m_dataCO2Char;
    LineGraphSeries<DataPoint>  m_dataHumidityChar;
    LineGraphSeries<DataPoint>  m_dataPressureChar;
    private long                m_startmillies;

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
        m_dataTemperatureChar  = new LineGraphSeries<>();   // min: 0           max: 35
        m_dataVOCChar          = new LineGraphSeries<>();   // min: 0           max: ca. 4000
        m_dataCO2Char          = new LineGraphSeries<>();   // min: 0           max: ca. 2000
        m_dataHumidityChar     = new LineGraphSeries<>();   // min: 0           max: 100
        m_dataPressureChar     = new LineGraphSeries<>();   // min ca. 800:     max: ca. 1200

        m_dataCO2Char.setColor          (Color.BLUE);
        m_dataPressureChar.setColor     (Color.BLUE);
        m_dataVOCChar.setColor          (Color.BLUE);
        m_dataTemperatureChar.setColor  (Color.BLUE);
        m_dataHumidityChar.setColor     (Color.BLUE);
        m_dataCO2Char.setTitle          ("CO2 [ppm]");
        m_dataPressureChar.setTitle     ("Druck [mBar]");
        m_dataVOCChar.setTitle          ("VOC [ppb]");
        m_dataTemperatureChar.setTitle  ("Temperatur [°C]");
        m_dataHumidityChar.setTitle     ("Feuchte [%]");
        m_dataCO2Char.setDrawDataPoints(true);
        m_dataCO2Char.setDataPointsRadius(10);
        m_dataCO2Char.setThickness(8);
        m_dataCO2Char.setDrawBackground(true);
        m_dataCO2Char.setBackgroundColor(android.R.color.holo_blue_bright);
        m_dataPressureChar.setDrawDataPoints(true);
        m_dataPressureChar.setDataPointsRadius(10);
        m_dataPressureChar.setThickness(8);
        m_dataPressureChar.setDrawBackground(true);
        m_dataPressureChar.setBackgroundColor(android.R.color.holo_blue_bright);
        m_dataVOCChar.setDrawDataPoints(true);
        m_dataVOCChar.setDataPointsRadius(10);
        m_dataVOCChar.setThickness(8);
        m_dataVOCChar.setDrawBackground(true);
        m_dataVOCChar.setBackgroundColor(android.R.color.holo_blue_bright);
        m_dataTemperatureChar.setDrawDataPoints(true);
        m_dataTemperatureChar.setDataPointsRadius(10);
        m_dataTemperatureChar.setThickness(8);
        m_dataTemperatureChar.setDrawBackground(true);
        m_dataTemperatureChar.setBackgroundColor(android.R.color.holo_blue_bright);
        m_dataHumidityChar.setDrawDataPoints(true);
        m_dataHumidityChar.setDataPointsRadius(10);
        m_dataHumidityChar.setThickness(8);
        m_dataHumidityChar.setDrawBackground(true);
        m_dataHumidityChar.setBackgroundColor(android.R.color.holo_blue_bright);


        //Assign to Data Objects to GraphView
        m_graphView.getViewport().setScalable(true);        //Manuelles scrollen
        m_graphView.getViewport().setScalableY(true);
        m_graphView.getViewport().setScrollable(true);
        m_graphView.getViewport().setScrollableY(true);

        m_graphView.setTitle("LGS Daten");
        m_graphView.getLegendRenderer().setVisible(true);
        m_graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        m_graphView.getLegendRenderer().setBackgroundColor(Color.LTGRAY);

        m_startmillies = Calendar.getInstance().getTimeInMillis();
        m_graphView.getViewport().setMinX(
                (float)(((Calendar.getInstance().getTimeInMillis() - m_startmillies) / 1000)));
        m_graphView.getViewport().setMaxX(
                (float)(((Calendar.getInstance().getTimeInMillis() - m_startmillies) / 1000) + 1.0));

        m_graphView.addSeries(m_dataTemperatureChar); //Initial: Temperatur plotten
        m_graphView.getViewport().setMinY(0.0);
        m_graphView.getViewport().setMaxY(30.0);
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
                    //Log.i(TAG, "Update received: UUID_CHARACTERISTIC_BRIGHT");
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
                    //Log.i(TAG, "Update received: UUID_CHARACTERISTIC_TEMPERATURE");
                    DataPoint newPoint = new DataPoint(
                            (float)((Calendar.getInstance().getTimeInMillis() - m_startmillies) / 1000),
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0));
                    m_dataTemperatureChar.appendData(newPoint, false, 2048);
                    m_temperature.setText("" + newPoint.getY() + " °C");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_VOC:
                {
                    //Log.i(TAG, "Update received: UUID_CHARACTERISTIC_VOC");
                    DataPoint newPoint = new DataPoint(
                            (float)((Calendar.getInstance().getTimeInMillis() - m_startmillies) / 1000),
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                    m_dataVOCChar.appendData(newPoint, false, 2048);
                    m_voc.setText("" + newPoint.getY() + " ppb");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_CO2:
                {
                    //Log.i(TAG, "Update received: UUID_CHARACTERISTIC_CO2");
                    DataPoint newPoint = new DataPoint(
                            (float)((Calendar.getInstance().getTimeInMillis() - m_startmillies) / 1000),
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                    m_dataCO2Char.appendData(newPoint, false, 2048);
                    m_co2.setText("" + newPoint.getY() + " ppm");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_HUMIDITY:
                {
                    //Log.i(TAG, "Update received: UUID_CHARACTERISTIC_HUMIDITY");
                    DataPoint newPoint = new DataPoint(
                            (float)((Calendar.getInstance().getTimeInMillis() - m_startmillies) / 1000),
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                    m_dataHumidityChar.appendData(newPoint, false, 2048);
                    m_Feuchte.setText("" + newPoint.getY() + " %");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_PRESSURE:
                {
                    //Log.i(TAG, "Update received: UUID_CHARACTERISTIC_PRESSURE");
                    DataPoint newPoint = new DataPoint(
                            (float)((Calendar.getInstance().getTimeInMillis() - m_startmillies) / 1000),
                            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                    m_dataPressureChar.appendData(newPoint, false, 2048);
                    m_Pressure.setText("" + newPoint.getY() + " mBar");
                    break;
                }
                case ViewFragment.UUID_CHARACTERISTIC_SETTING_OUTPUTACT:
                {
                    //Ignore
                    break;
                }
                default:
                {
                    Log.i(TAG, "Ungültige Characteristic empfangen. + " + characteristic.getUuid().toString());
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
                m_graphView.removeAllSeries();
                m_graphView.addSeries(m_dataTemperatureChar); //Initial: Temperatur plotten
                m_graphView.getViewport().setMinY(-5.0);
                m_graphView.getViewport().setMaxY(30.0);
                break;
            }
            case R.id.homeview_buttonFeuchte:
            {
                m_graphView.removeAllSeries();
                m_graphView.addSeries(m_dataHumidityChar); //Initial: Temperatur plotten
                m_graphView.getViewport().setMinY(-5.0);
                m_graphView.getViewport().setMaxY(105.0);
                break;
            }
            case R.id.homeview_buttonDruck:
            {
                m_graphView.removeAllSeries();
                m_graphView.addSeries(m_dataPressureChar); //Initial: Temperatur plotten
                m_graphView.getViewport().setMinY(850);
                m_graphView.getViewport().setMaxY(1150);
                break;
            }
            case R.id.homeview_buttonCO2:
            {
                m_graphView.removeAllSeries();
                m_graphView.addSeries(m_dataCO2Char); //Initial: Temperatur plotten
                m_graphView.getViewport().setMinY(-5.0);
                m_graphView.getViewport().setMaxY(2000.0);
                break;
            }
            case R.id.homeview_buttonVOC:
            {
                m_graphView.removeAllSeries();
                m_graphView.addSeries(m_dataVOCChar); //Initial: Temperatur plotten
                m_graphView.getViewport().setMinY(-5.0);
                m_graphView.getViewport().setMaxY(3500.0);
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

