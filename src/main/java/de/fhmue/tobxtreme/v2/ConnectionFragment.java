package de.fhmue.tobxtreme.v2;

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
import android.widget.ProgressBar;
import android.widget.TextView;



public class ConnectionFragment extends Fragment {
    /**
     * INTERFACE
     */
    public interface Interface {
        void startScan();
    }
    public Interface m_interface;


    /**
     * CONSTANTS
     */
    private static final String TAG             = "ConnectionFragment";


    /**
     * OBJECTS
     */
    private Button                      m_scanButton;           //Button Retry Scan
    private ProgressBar                 m_progressBar;          //ProgressBar ActivityIndicator
    private TextView                    m_textView;             //Textview "Suche Sensor"


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_connection, container, false);

        /**
         * Get UI-Items from Layout
         */
        m_scanButton = v.findViewById(R.id.connection_fragment_scanButton);
        m_textView = v.findViewById(R.id.connection_fragment_textView);
        m_progressBar = v.findViewById(R.id.connection_fragment_activityIndicator);

        /**
         * Init UI-Items
         */
        m_progressBar.setIndeterminate(false);
        m_progressBar.setVisibility(View.INVISIBLE);
        m_scanButton.setEnabled(false);
        m_scanButton.setOnClickListener(m_buttonClickListener);
        m_textView.setVisibility(View.INVISIBLE);

        //Scan direkt aktiv bei Start:
        setScanActive(false);

        return v;
    }


    public void setScanActive(boolean isActive)
    {
        Log.i(TAG, "setScanActive: " + String.valueOf(isActive));
        m_progressBar.setIndeterminate(isActive);
        if (isActive)
        {
            m_progressBar.setVisibility(View.VISIBLE);
            m_scanButton.setEnabled(false);

            m_textView.setVisibility(View.VISIBLE);
            m_textView.setText("Suche Sensor");
        }
        else
        {
            m_progressBar.setVisibility(View.INVISIBLE);
            m_scanButton.setEnabled(true);

            m_textView.setVisibility(View.VISIBLE);
            m_textView.setText("Sensor nicht gefunden");
        }
    }

    public void setConnecting(boolean isConnecting)
    {
        Log.i(TAG, "setScanActive: " + String.valueOf(isConnecting));
        if(isConnecting)
        {
            m_progressBar.setIndeterminate(true);
            m_progressBar.setVisibility(View.VISIBLE);
            m_scanButton.setEnabled(false);

            m_textView.setVisibility(View.VISIBLE);
            m_textView.setText("Verbinde");
        }
        else
        {
            m_progressBar.setVisibility(View.INVISIBLE);
            m_scanButton.setEnabled(true);

            m_textView.setVisibility(View.VISIBLE);
            m_textView.setText("Konnte nicht verbinden");
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Interface) {
            m_interface = (Interface) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ConnectionFragment.ConnectionFragmentInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    /**
     * UI Adapters
     */
    private AdapterView.OnClickListener m_buttonClickListener = (View v) -> {
        if (v.getId() == R.id.connection_fragment_scanButton) {
            Log.i(TAG, ": Starte Scan!");
            if(m_interface != null) m_interface.startScan();
        }
    };
}
