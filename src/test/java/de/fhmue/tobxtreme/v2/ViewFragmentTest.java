package de.fhmue.tobxtreme.v2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;


@RunWith(PowerMockRunner.class)

@PrepareForTest({Fragment.class})

public class ViewFragmentTest {

    ViewFragment mFragment;
    @Mock   MainActivity mActivity;
    @Mock   View mView;
    @Mock   LayoutInflater mInflater;
    @Mock   ViewGroup mViewGroup;
    @Mock   Bundle mBundle;

    //TODO
    //...

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        mFragment = PowerMockito.spy(new ViewFragment());

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mInflater.inflate(anyInt(), isA(ViewGroup.class), anyBoolean())).thenReturn(mView);

        //TODO
        //mFragment.onCreateView(mInflater, mViewGroup, mBundle);
    }


    @Test
    public void testOnAttach() throws Exception{
        mFragment.onAttach(mActivity);
    }

    @Test
    public void testOnDetach() throws Exception{
        mFragment.onDetach();
    }

}