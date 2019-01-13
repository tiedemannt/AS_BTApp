package de.fhmue.tobxtreme.v2;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;

import java.util.ArrayList;


@RunWith(PowerMockRunner.class)

@PrepareForTest({Fragment.class})

public class ConnectionFragmentTest {

    ConnectionFragment mFragment;
    @Mock   MainActivity mActivity;
    @Mock   View mView;
    @Mock   LayoutInflater mInflater;
    @Mock   ViewGroup mViewGroup;
    @Mock   Bundle mBundle;

    @Mock   ListView mListView;
    @Mock   Button mButton;
    @Mock   ProgressBar mProgressBar;


    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        mFragment = PowerMockito.spy(new ConnectionFragment());

        when(mFragment.getActivity()).thenReturn(mActivity);

        when(mInflater.inflate(anyInt(), isA(ViewGroup.class), anyBoolean())).thenReturn(mView);
        when(mView.findViewById(R.id.connection_fragment_scanButton)).thenReturn(mButton);
        when(mView.findViewById(R.id.connection_fragment_listView)).thenReturn(mListView);
        when(mView.findViewById(R.id.connection_fragment_activityIndicator)).thenReturn(mProgressBar);
        doNothing().when(mListView).setAdapter(isA(ListAdapter_BTLE_Devices.class));

        mFragment.onCreateView(mInflater, mViewGroup, mBundle);
    }


    @Test
    public void testOnAttach() throws Exception{
        mFragment.onAttach(mActivity);
    }

    @Test
    public void testOnDetach() throws Exception{
        mFragment.m_Callback = mActivity;

        mFragment.onDetach();

        verify(mActivity, times(1))
                .stopScan();
    }

    @Test
    public void testOnCreateView() throws Exception{
        //Verify
        verify(mView, times(3))
                .findViewById(anyInt());
        verify(mInflater, times(1))
                .inflate(anyInt(), isA(ViewGroup.class), anyBoolean());
        verify(mListView, times(1))
                .setAdapter(anyObject());
        verify(mListView, times(1))
                .setOnItemClickListener(any());
        verify(mProgressBar, times(1))
                .setIndeterminate(false);
        verify(mProgressBar, times(1))
                .setVisibility(View.INVISIBLE);
        verify(mButton, times(1))
                .setEnabled(false);
        verify(mButton, times(1))
                .setOnClickListener(anyObject());

        verifyNoMoreInteractions(mButton);
        verifyNoMoreInteractions(mProgressBar);
        verifyNoMoreInteractions(mListView);
        verifyNoMoreInteractions(mView);
        verifyNoMoreInteractions(mInflater);
    }

    @Test
    public void testSetScanActive(){
        mFragment.setScanActive(true);
        verify(mProgressBar, times(1)).setIndeterminate(true);
        verify(mProgressBar, times(1)).setVisibility(View.VISIBLE);
        verify(mButton, times(2)).setEnabled(false);

        mFragment.setScanActive(false);
        verify(mProgressBar, times(2)).setIndeterminate(false);
        verify(mProgressBar, times(2)).setVisibility(View.INVISIBLE);
        verify(mButton, times(1)).setEnabled(true);
    }

    @Test
    public void testDeviceListHandling(){
        BluetoothDevice mbtDevice = mock(BluetoothDevice.class);
        when(mbtDevice.getAddress()).thenReturn("Adress1");
        when(mbtDevice.getName()).thenReturn("Name1");
        BT_Device mDevice = spy(new BT_Device(mbtDevice, 50));

        mFragment.addDevice(mDevice);
        assertTrue(!mFragment.getDeviceList().isEmpty());

        BluetoothDevice mbtDevice2 = mock(BluetoothDevice.class);
        when(mbtDevice.getAddress()).thenReturn("Adress2");
        when(mbtDevice.getName()).thenReturn("Name1");
        BT_Device mDevice2 = spy(new BT_Device(mbtDevice2, 50));
        mFragment.removeDevice(mDevice2);
        assertTrue(!mFragment.getDeviceList().isEmpty());

        mFragment.clearDeviceList();
        assertTrue(mFragment.getDeviceList().isEmpty());
    }
}