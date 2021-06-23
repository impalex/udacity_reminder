package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.FakeAndroidTestRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        repository = FakeAndroidTestRepository()
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
                repository as ReminderDataSource
            }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
    }

    @After
    fun stop() {
        stopKoin()
    }

    @Test
    fun emptyList_test() {
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun notEmptyList_test() = runBlockingTest {
        val data = ReminderDTO("title", "description", "location", 100.0, 100.0, 100f, "test_id")
        repository.saveReminder(data)
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText(data.title))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText(data.description))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasDescendant(withText(data.location))))
    }

    @Test
    fun addReminder_navigateToEdit() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment { Navigation.setViewNavController(it.view!!, navController) }

        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder(null))
    }

    @Test
    fun editReminder_navigateToEdit() = runBlockingTest {
        val data = ReminderDTO("title", "description", "location", 100.0, 100.0, 100f, "test_id")
        repository.saveReminder(data)
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment { Navigation.setViewNavController(it.view!!, navController) }

        onView(withId(R.id.reminderssRecyclerView)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(data.title)),
                click()
            )
        )

        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder(data.id))

    }
}