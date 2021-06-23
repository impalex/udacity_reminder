package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.launch
import java.util.*

const val DEFAULT_RADIUS = 100.0f

class SaveReminderViewModel(val app: Application, private val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    private var _id = UUID.randomUUID().toString()
    val id: String
        get() = _id
    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val reminderSelectedLocationStr = MutableLiveData<String>()
    val reminderRadius = MutableLiveData<Float>().apply { value = DEFAULT_RADIUS }
    private var selectedPOI: PointOfInterest? = null
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()
    private val _selectedLocation = MutableLiveData<LatLng?>()
    val selectedLocation: LiveData<LatLng?>
        get() = _selectedLocation
    val selectedDescription = MutableLiveData<String>()
    val selectedRadius = MutableLiveData<Float>()
    private val _isExisting = MutableLiveData<Boolean>()
    val isExisting: LiveData<Boolean>
        get() = _isExisting

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    @SuppressLint("NullSafeMutableLiveData")
    fun clear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI = null
        latitude.value = null
        longitude.value = null
        reminderRadius.value = DEFAULT_RADIUS
        _selectedLocation.value = null
        selectedDescription.value = null
        selectedRadius.value = null
        _isExisting.value = false
        _id = UUID.randomUUID().toString()
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun resetSelection() {
        selectedDescription.value = reminderSelectedLocationStr.value
        val lat = latitude.value
        val lon = longitude.value
        _selectedLocation.value = if (lat != null && lon != null) LatLng(lat, lon) else null
        selectedRadius.value = reminderRadius.value
    }

    fun setLatLonSelection(latLng: LatLng) {
        _selectedLocation.value = latLng
        if (selectedPOI?.name == selectedDescription.value) {
            selectedDescription.value = ""
        }
    }

    fun selectPOI(pointOfInterest: PointOfInterest) = pointOfInterest.let {
        setLatLonSelection(it.latLng)
        selectedPOI = pointOfInterest
        selectedDescription.value = it.name
        EspressoIdlingResource.decrement()
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun loadReminder(id: String) {
        showLoading.value = true
        _isExisting.postValue(false)
        viewModelScope.launch {
            dataSource.getReminder(id).let { reminder ->
                when (reminder) {
                    is Result.Success<ReminderDTO> -> reminder.data.let {
                        this@SaveReminderViewModel._id = it.id
                        reminderTitle.postValue(it.title)
                        reminderDescription.postValue(it.description)
                        reminderSelectedLocationStr.postValue(it.location)
                        reminderRadius.postValue(it.radius)
                        latitude.postValue(it.latitude)
                        longitude.postValue(it.longitude)
                        _isExisting.postValue(true)
                    }
                    is Result.Error -> showSnackBar.postValue(reminder.message)
                }
            }
            showLoading.postValue(false)
        }
    }

    fun confirmSelection() {
        if (selectedLocation.value == null) {
            showSnackBarInt.value = R.string.err_select_location
            return
        }
        if (selectedDescription.value.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_location_description
            return
        }
        reminderSelectedLocationStr.value = selectedDescription.value
        selectedLocation.value?.let {
            latitude.value = it.latitude
            longitude.value = it.longitude
        }
        reminderRadius.value = selectedRadius.value
        navigationCommand.value = NavigationCommand.Back
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.radius,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    fun removeReminder() {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.deleteReminder(_id)
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_deleted)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }
}