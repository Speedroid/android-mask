package com.survivalcoding.maskinfo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.survivalcoding.maskinfo.MaskInfoApplication
import com.survivalcoding.maskinfo.R
import com.survivalcoding.maskinfo.databinding.ActivityMainBinding
import com.survivalcoding.maskinfo.ui.adapter.MaskStockAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((this.application as MaskInfoApplication).storeRepository)
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            this
        )
    }

    override fun onStart() {
        super.onStart()
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loadMyLocation()
        viewModel.load()

        val maskStockRecyclerView = binding.maskStockRecyclerView
        val maskStockAdapter = MaskStockAdapter()
        maskStockRecyclerView.adapter = maskStockAdapter


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainUiState.collectLatest {
                    maskStockAdapter.submitList(it.maskStocks.toMutableList()) {
                        binding.toolbar.title =
                            getString(R.string.title, maskStockAdapter.itemCount)
                    }
                }
            }
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_search -> {
                    loadMyLocation()
                    viewModel.load()
                }
            }
            true
        }
    }

    fun loadMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                viewModel.myLat = it.latitude.toFloat()
                viewModel.myLng = it.longitude.toFloat()
            }
            Log.d("tag", "${viewModel.myLat}, ${viewModel.myLng}")
        }
    }

    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        false
                    ) -> {
                loadMyLocation()
                viewModel.load()
            }
            else -> {
                // No location access granted.
            }
        }

    }
}