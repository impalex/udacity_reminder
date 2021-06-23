package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showPermissionsError
import com.udacity.project4.utils.wrapEspressoIdlingResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import timber.log.Timber

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private val args by navArgs<SaveReminderFragmentArgs>()
    private val geofenceClient by lazy { LocationServices.getGeofencingClient(requireContext()) }
    private val geofencePendingIntent by lazy {
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            Intent(requireContext(), GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            _viewModel.clear()
            args.reminderId?.let { _viewModel.loadReminder(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener { createGeofenceAndSaveReminder() }
        binding.deleteButton.setOnClickListener { lifecycleScope.launch { removeGeofenceAndReminder() } }
    }

    override fun onDestroy() {

        super.onDestroy()
        arguments?.clear()
    }

    private fun isBackgroundLocationPermissionGranted() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isFineLocationGranted() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val requestLocationPermissionLauncherForCreateGeofence = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if (result) {
            lifecycleScope.launch { createGeofenceAndSaveReminder() }
        } else {
            showPermissionsError()
        }
    }

    private fun createGeofenceAndSaveReminder() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val radius = _viewModel.reminderRadius.value
        val id = _viewModel.id
        val reminder = ReminderDataItem(title, description, location, latitude, longitude, radius, id)
        if (_viewModel.validateEnteredData(reminder)) {
            lifecycleScope.launch {
                addGeofenceAndSaveReminder(reminder)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun addGeofenceAndSaveReminder(reminder: ReminderDataItem) {
        if (isFineLocationGranted()) {
            if (isBackgroundLocationPermissionGranted()) {
                val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    val geofence = Geofence.Builder()
                        .setRequestId(reminder.id)
                        .setCircularRegion(reminder.latitude ?: return, reminder.longitude ?: return, reminder.radius ?: return)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build()
                    val request = GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofence(geofence)
                        .build()
                    wrapEspressoIdlingResource {
                        try {
                            geofenceClient.addGeofences(request, geofencePendingIntent).await()
                        } catch (e: Exception) {
                            Timber.e(e)
                            _viewModel.showSnackBarInt.postValue(R.string.error_adding_geofence)
                            return
                        }

                        _viewModel.saveReminder(reminder)
                    }
                } else {
                    Snackbar.make(requireView(), R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                }
            } else {
                requestLocationPermissionLauncherForCreateGeofence.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            requestLocationPermissionLauncherForCreateGeofence.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestLocationPermissionLauncherForRemoveGeofence = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if (result) {
            lifecycleScope.launch { removeGeofenceAndReminder() }
        } else {
            showPermissionsError()
        }
    }

    private suspend fun removeGeofenceAndReminder() {
        if (isFineLocationGranted()) {
            if (isBackgroundLocationPermissionGranted()) {
                wrapEspressoIdlingResource {
                    try {
                        geofenceClient.removeGeofences(mutableListOf(_viewModel.id)).await()
                        Timber.d("Geofence removed")
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    _viewModel.removeReminder()
                }
            } else {
                requestLocationPermissionLauncherForRemoveGeofence.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            requestLocationPermissionLauncherForRemoveGeofence.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

}
