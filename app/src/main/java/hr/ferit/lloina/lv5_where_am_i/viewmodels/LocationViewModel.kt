package hr.ferit.lloina.lv5_where_am_i.viewmodels

import android.Manifest
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.Marker
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted

class LocationViewModel() : ViewModel() {
    private var location = MutableLiveData<Location>();
    lateinit var userMarker: Marker

    fun getLocation() : LiveData<Location> {
        return location as LiveData<Location>;
    }

    fun setLocation(d : Location){
        location.value = d;
    }
}

