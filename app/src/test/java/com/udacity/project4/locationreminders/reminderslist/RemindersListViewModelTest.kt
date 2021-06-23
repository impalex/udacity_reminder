package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.TEST_ERROR
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@SmallTest
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersViewModel: RemindersListViewModel

    @Before
    fun setupViewModel() {
        fakeDataSource = FakeDataSource().apply {
            addData(
                ReminderDTO("Title 1", "Description 1", "Location 1", 1.0, 1.0, 1f, "aaa"),
                ReminderDTO("Title 2", "Description 2", "Location 2", 2.0, 2.0, 2f, "bbb"),
                ReminderDTO("Title 3", "Description 3", "Location 3", 3.0, 3.0, 3f, "ccc")
            )
        }
        remindersViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun loadReminders_loading() {
        mainCoroutineRule.pauseDispatcher()
        remindersViewModel.loadReminders()
        assertThat(remindersViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_notEmpty() {
        remindersViewModel.loadReminders()
        assertThat(remindersViewModel.remindersList.getOrAwaitValue().size, `is`(fakeDataSource.remindersData.size))
        assertThat(remindersViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_empty() {
        fakeDataSource.remindersData.clear()
        remindersViewModel.loadReminders()
        assertThat(remindersViewModel.remindersList.getOrAwaitValue().size, `is`(0))
        assertThat(remindersViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_error() {
        fakeDataSource.shouldReturnError = true
        remindersViewModel.loadReminders()
        assertThat(remindersViewModel.showSnackBar.getOrAwaitValue(), `is`(TEST_ERROR))

    }

}