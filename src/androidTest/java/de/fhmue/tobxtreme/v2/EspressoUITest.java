package de.fhmue.tobxtreme.v2;


import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import static  org.hamcrest.Matchers.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EspressoUITest {

    @Rule
    public ActivityTestRule<MainActivity> mTestRule =
            new ActivityTestRule<MainActivity>(MainActivity.class);

    @Test
    public void testStartup() throws Exception {
        onView(withId(R.id.connection_fragment_scanButton)).check(matches(isDisplayed()));
        onView(withId(R.id.connection_fragment_scanButton)).check(matches(not(isEnabled())));
        onView(withId(R.id.connection_fragment_listView)).check(matches(isDisplayed()));
        onView(withId(R.id.connection_fragment_textView)).check(matches(isDisplayed()));
        onView(withId(R.id.connection_fragment_viewLine)).check(matches(isDisplayed()));
        onView(withId(R.id.connection_fragment_viewLine2)).check(matches(isDisplayed()));
        onView(withId(R.id.connection_fragment_activityIndicator)).check(matches(not(isDisplayed())));
    }
}
