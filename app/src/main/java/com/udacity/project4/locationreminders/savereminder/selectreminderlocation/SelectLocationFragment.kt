package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import timber.log.Timber

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var reminderCircle: Circle
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            _viewModel.resetSelection()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.rangeSlider.setLabelFormatter { getString(R.string.range_hint_format, it) }

        // map setup
        EspressoIdlingResource.increment()
        (childFragmentManager.findFragmentById(binding.reminderMap.id) as SupportMapFragment).getMapAsync(this)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        loadMapStyle()
        _viewModel.selectedLocation.observe(viewLifecycleOwner) {
            updateCircle(latLng = it)
            updateMarker(it ?: return@observe)
        }

        _viewModel.selectedRadius.observe(viewLifecycleOwner) {
            updateCircle(radius = it)
        }

        map.setOnMapClickListener {
            _viewModel.setLatLonSelection(it)
        }

        map.setOnPoiClickListener {
            _viewModel.selectPOI(it)
        }
        lifecycleScope.launch {
            showDefaultLocation()
        }
        EspressoIdlingResource.decrement()
    }

    private fun loadMapStyle() {
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        } catch (e: Resources.NotFoundException) {
            Timber.e(e)
        }
    }

    private fun updateCircle(latLng: LatLng? = _viewModel.selectedLocation.value, radius: Float? = _viewModel.selectedRadius.value) {
        latLng ?: return
        radius ?: return
        if (::reminderCircle.isInitialized) {
            reminderCircle.center = latLng
            reminderCircle.radius = radius.toDouble()
        } else {
            reminderCircle = createCircle(latLng, radius)
        }
    }

    private fun updateMarker(latLng: LatLng) {
        marker?.run {
            position = latLng
            return
        }
        marker = map.addMarker(MarkerOptions().position(latLng))
    }

    private fun createCircle(latLng: LatLng, radius: Float) = map.addCircle(
        CircleOptions()
            .center(latLng)
            .radius(radius.toDouble())
            .strokeColor(ContextCompat.getColor(requireContext(), R.color.colorCircleStroke))
            .strokeWidth(2f)
            .fillColor(ContextCompat.getColor(requireContext(), R.color.colorCircleFill))
    )

    private fun showPermissionsError() {
        Snackbar.make(requireView(), R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    private fun zoomMapTo(coord: LatLng) = map.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 15f))

    private fun isFineLocationGranted() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun isBackgroundLocationGranted() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationPermissionsGranted() = isFineLocationGranted() && isBackgroundLocationGranted()

    // Asking permissions one by one because of this:
    // ...
    // If your app targets Android 11 (API level 30) or higher, the system enforces this best practice.
    // If you request a foreground location permission and the background location permission at the same time,
    // the system ignores the request and doesn't grant your app either permission.

    private val requestForegroundPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if (result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                lifecycleScope.launch { showDefaultLocation() }
            }
        } else {
            showPermissionsError()
        }
    }

    private val requestBackgroundPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if (result) {
            lifecycleScope.launch { showDefaultLocation() }
        } else {
            showPermissionsError()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun showDefaultLocation() {
        if (isLocationPermissionsGranted()) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            try {
                val selectedLocation = _viewModel.selectedLocation.value
                if (selectedLocation != null) {
                    zoomMapTo(selectedLocation)
                } else {
                    try {
                        LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.await().let {
                            it ?: return
                            val lat = it.latitude
                            val lon = it.longitude
                            Timber.d("Last known location: $lat, $lon")
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15f))
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            } catch (e: SecurityException) {
                Timber.e(e)
            }
        } else {
            requestForegroundPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

}
