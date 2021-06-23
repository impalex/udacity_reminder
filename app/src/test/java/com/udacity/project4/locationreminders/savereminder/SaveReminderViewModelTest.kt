package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.DATA_NOT_FOUND_ERROR
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class SaveReminderViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var saveViewModel: SaveReminderViewModel

    @Before
    fun setupViewModel() {
        fakeDataSource = FakeDataSource().apply {
            addData(
                ReminderDTO("Title 1", "Description 1", "Location 1", 1.0, 1.0, 1f, "aaa"),
                ReminderDTO("Title 2", "Description 2", "Location 2", 2.0, 2.0, 2f, "bbb"),
                ReminderDTO("Title 3", "Description 3", "Location 3", 3.0, 3.0, 3f, "ccc")
            )
        }
        saveViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun loadReminder_success() {
        val item = fakeDataSource.remindersData.values.first()
        mainCoroutineRule.dispatcher.pauseDispatcher()
        saveViewModel.loadReminder(item.id)
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(true))
        assertThat(saveViewModel.isExisting.getOrAwaitValue(), `is`(false))

        mainCoroutineRule.dispatcher.resumeDispatcher()
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(false))

        assertThat(saveViewModel.id, `is`(item.id))
        assertThat(saveViewModel.reminderTitle.getOrAwaitValue(), `is`(item.title))
        assertThat(saveViewModel.reminderDescription.getOrAwaitValue(), `is`(item.description))
        assertThat(saveViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(item.location))
        assertThat(saveViewModel.reminderRadius.getOrAwaitValue(), `is`(item.radius))
        assertThat(saveViewModel.latitude.getOrAwaitValue(), `is`(item.latitude))
        assertThat(saveViewModel.longitude.getOrAwaitValue(), `is`(item.longitude))
        assertThat(saveViewModel.isExisting.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminder_reminder_notFound() {
        saveViewModel.loadReminder("unknown_reminder")
        assertThat(saveViewModel.showSnackBar.getOrAwaitValue(), `is`(DATA_NOT_FOUND_ERROR))
        assertThat(saveViewModel.isExisting.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun removeReminder_test() {
        val item = fakeDataSource.remindersData.values.first()
        saveViewModel.loadReminder(item.id)
        mainCoroutineRule.dispatcher.pauseDispatcher()
        saveViewModel.removeReminder()
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.dispatcher.resumeDispatcher()
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(saveViewModel.showToast.getOrAwaitValue(), `is`(saveViewModel.app.getString(R.string.reminder_deleted)))
        assertThat(fakeDataSource.remindersData.values.contains(item), `is`(false))
        assertThat(saveViewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back))
    }

    @Test
    fun resetSelection_test() {
        val item = fakeDataSource.remindersData.values.first()
        saveViewModel.loadReminder(item.id)
        saveViewModel.resetSelection()
        assertThat(saveViewModel.selectedDescription.getOrAwaitValue(), `is`(saveViewModel.reminderSelectedLocationStr.getOrAwaitValue()))
        assertThat(saveViewModel.selectedRadius.getOrAwaitValue(), `is`(saveViewModel.reminderRadius.getOrAwaitValue()))
        val latlon = saveViewModel.selectedLocation.getOrAwaitValue()
        assertThat(latlon?.latitude, `is`(saveViewModel.latitude.getOrAwaitValue()))
        assertThat(latlon?.longitude, `is`(saveViewModel.longitude.getOrAwaitValue()))
    }

    @Test
    fun selectPOI_test() {
        val poi = PointOfInterest(LatLng(100.0, 100.0), "placeid", "placename")
        saveViewModel.selectPOI(poi)
        assertThat(saveViewModel.selectedLocation.getOrAwaitValue(), `is`(poi.latLng))
        assertThat(saveViewModel.selectedDescription.getOrAwaitValue(), `is`(poi.name))
    }

    @Test
    fun setLatLonSelection_dontResetDescription() {
        val originalDescription = "description"
        val latlon = LatLng(100.0, 100.0)
        saveViewModel.selectedDescription.value = originalDescription
        saveViewModel.setLatLonSelection(latlon)
        assertThat(saveViewModel.selectedDescription.getOrAwaitValue(), `is`(originalDescription))
        assertThat(saveViewModel.selectedLocation.getOrAwaitValue(), `is`(latlon))
    }

    @Test
    fun setLatLonSelection_resetDescription() {
        val poi = PointOfInterest(LatLng(100.0, 100.0), "placeid", "placename")
        saveViewModel.selectPOI(poi)
        val latlon = LatLng(50.0, 50.0)
        saveViewModel.setLatLonSelection(latlon)
        assertThat(saveViewModel.selectedDescription.getOrAwaitValue(), `is`(""))
        assertThat(saveViewModel.selectedLocation.getOrAwaitValue(), `is`(latlon))
    }

    @Test
    fun clear_test() {
        val item = fakeDataSource.remindersData.values.first()
        saveViewModel.loadReminder(item.id)
        assertThat(saveViewModel.id, `is`(item.id))
        saveViewModel.clear()
        assertThat(saveViewModel.id, not(item.id))
        assertThat(saveViewModel.reminderTitle.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.reminderDescription.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.reminderSelectedLocationStr.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.latitude.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.longitude.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.reminderRadius.getOrAwaitValue(), `is`(DEFAULT_RADIUS))
        assertThat(saveViewModel.selectedLocation.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.selectedDescription.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.selectedRadius.getOrAwaitValue(), nullValue())
        assertThat(saveViewModel.isExisting.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun confirmSelection_noLocationError() {
        saveViewModel.confirmSelection()
        assertThat(saveViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun confirmSelection_noDescriptionError() {
        saveViewModel.setLatLonSelection(LatLng(100.0, 100.0))
        saveViewModel.confirmSelection()
        assertThat(saveViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_location_description))
    }

    @Test
    fun confirmSelection_success() {
        val testDescription = "test description"
        val testLocation = LatLng(100.0, 100.0)
        val testRadius = 69f
        saveViewModel.setLatLonSelection(testLocation)
        saveViewModel.selectedDescription.value = testDescription
        saveViewModel.selectedRadius.value = testRadius
        saveViewModel.confirmSelection()
        assertThat(saveViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(testDescription))
        assertThat(saveViewModel.latitude.getOrAwaitValue(), `is`(testLocation.latitude))
        assertThat(saveViewModel.longitude.getOrAwaitValue(), `is`(testLocation.longitude))
        assertThat(saveViewModel.reminderRadius.getOrAwaitValue(), `is`(testRadius))
        assertThat(saveViewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back))
    }

    @Test
    fun saveReminder_test() {
        val data = ReminderDataItem("test title", "test description", "test location", 100.0, 200.0, 300f, "test_id")
        mainCoroutineRule.dispatcher.pauseDispatcher()
        saveViewModel.saveReminder(data)
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.dispatcher.resumeDispatcher()
        assertThat(saveViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(saveViewModel.showToast.getOrAwaitValue(), `is`(saveViewModel.app.getString(R.string.reminder_saved)))
        assertThat(saveViewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back))
        val savedValue = fakeDataSource.remindersData[data.id]!!
        assertThat(savedValue.title, `is`(data.title))
        assertThat(savedValue.description, `is`(data.description))
        assertThat(savedValue.location, `is`(data.location))
        assertThat(savedValue.latitude, `is`(data.latitude))
        assertThat(savedValue.longitude, `is`(data.longitude))
        assertThat(savedValue.radius, `is`(data.radius))
    }

    @Test
    fun validateEnteredData_noTitleError() {
        val data = ReminderDataItem("", "test description", "test location", 100.0, 200.0, 300f, "test_id")
        assertThat(saveViewModel.validateEnteredData(data), `is`(false))
        assertThat(saveViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_noLocationError() {
        val data = ReminderDataItem("test title", "test description", "", 100.0, 200.0, 300f, "test_id")
        assertThat(saveViewModel.validateEnteredData(data), `is`(false))
        assertThat(saveViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun validateEnteredData_success() {
        val data = ReminderDataItem("test title", "test description", "test location", 100.0, 200.0, 300f, "test_id")
        assertThat(saveViewModel.validateEnteredData(data), `is`(true))
    }


}