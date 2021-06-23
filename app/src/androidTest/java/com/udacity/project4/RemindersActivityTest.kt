package com.udacity.project4

import android.app.Application
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager.widget.ViewPager
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource, dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource, dataBindingIdlingResource)
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                @Suppress("USELESS_CAST")
                RemindersLocalRepository(get()) as ReminderDataSource
            }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun addAndRemoveReminder() {

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // create new reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        // delete button shouldn't be visible
        onView(withId(R.id.delete_button)).check(matches(not(isDisplayed())))

        // try to save
        onView(withId(R.id.saveReminder)).perform(click())
        // check snackbar
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        // enter title, description
        onView(withId(R.id.reminderTitle)).perform(typeText("Reminder Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Reminder Description"))
        Espresso.closeSoftKeyboard()

        // let's select location
        onView(withId(R.id.selectLocation)).perform(click())

        // select some point on map
        onView(withId(R.id.reminder_map)).perform(GeneralClickAction(Tap.SINGLE, { view ->
            IntArray(2).let {
                view.getLocationOnScreen(it)
                floatArrayOf((it[0] + view.width * 0.3).toFloat(), (it[1] + view.height * 0.3).toFloat())
            }
        }, Press.FINGER, InputDevice.SOURCE_MOUSE, MotionEvent.BUTTON_PRIMARY))

        // enter description, confirm
        onView(withId(R.id.description_text_edit)).perform(click(), clearText())
        onView(withId(R.id.description_text_edit)).perform(typeText("Place description"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.select_reminder_button)).perform(click())

        // save
        onView(withId(R.id.saveReminder)).perform(click())
        // toast
        onView(withText(R.string.reminder_saved)).inRoot(ToastMatcher().apply { matches(isDisplayed()) })

        // check recyclerview
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText("Reminder Title"))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText("Reminder Description"))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText("Place description"))))

        // click on reminder
        onView(withId(R.id.reminderssRecyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // click on delete
        onView(withId(R.id.delete_button)).perform(click())
        // toast again
        onView(withText(R.string.reminder_deleted)).inRoot(ToastMatcher().apply { matches(isDisplayed()) })

        // check if list is empty
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        // omg i'm so tired already...
        activityScenario.close()
    }

}
