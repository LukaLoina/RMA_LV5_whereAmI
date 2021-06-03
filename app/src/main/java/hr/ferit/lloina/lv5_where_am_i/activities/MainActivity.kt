package hr.ferit.lloina.lv5_where_am_i.activities

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import hr.ferit.lloina.lv5_where_am_i.R
import hr.ferit.lloina.lv5_where_am_i.databinding.ActivityMainBinding
import hr.ferit.lloina.lv5_where_am_i.viewmodels.ImageViewModel
import hr.ferit.lloina.lv5_where_am_i.viewmodels.LocationViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding : ActivityMainBinding;
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var geocoder: Geocoder
    private lateinit var notificationManager: NotificationManager
    private var pinSound : Int = -1;

    private val locationViewModel: LocationViewModel by viewModels()
    private val imageViewModel: ImageViewModel by viewModels()

    private lateinit var map: GoogleMap

    val soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
        val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()
        SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(5)
                .build()
    } else {
        SoundPool(5, AudioManager.STREAM_MUSIC, 0)
    }



    val getImage = registerForActivityResult(ActivityResultContracts.TakePicture()){ success ->
        Log.i("app", "img success: ${success}")
        Log.i("app", imageViewModel.lastImage)
        if(success) {
            val file = File(imageViewModel.lastImage)
            val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            //umjesto getBroadcast iz primjera, potrebno je koristiti getActivity za Intent.ACTION_VIEW.
            val pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);


            var notification = NotificationCompat.Builder(applicationContext, "where-am-I")
                    .setSmallIcon(R.drawable.pin)
                    .setContentTitle("Nova slika")
                    .setContentText("slika")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)


            notificationManager.notify(1, notification.build())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater);

        geocoder = Geocoder(this);
        pinSound = soundPool.load(this, R.raw.pinsound, 1)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel("where-am-I", "where-am-I", NotificationManager.IMPORTANCE_LOW)
            mChannel.setDescription("Notification channel for where-am-I application.")
            mChannel.enableLights(true)
            mChannel.lightColor = Color.GREEN
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationManager.createNotificationChannel(mChannel)

        }

        setupLocationSensor();

        locationViewModel.getLocation().observe(this, Observer { location ->
            binding.twGeoLatData.text = location.latitude.toString();
            binding.twGeoLonData.text = location.longitude.toString();
            val geoInfo = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            binding.twCountryData.text = geoInfo[0]?.countryName ?: "unknown"
            binding.twCityData.text = geoInfo[0]?.locality ?: "unknown"
            binding.twAdressData.text = "${geoInfo[0]?.thoroughfare ?: "unknown"} ${geoInfo[0]?.subThoroughfare ?: ""}"
            locationViewModel.userMarker.position = LatLng(location.latitude, location.longitude)
        })

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnPhoto.setOnClickListener {
            takePhoto();
        }

        setContentView(binding.root)
    }

    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSIONS)
    fun setupLocationSensor(){
        if(EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION)){

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    for (position in locationResult.locations){
                        locationViewModel.setLocation(position)
                    }
                }
            }

            val locationRequest = LocationRequest.create()?.apply {
                interval = 10000
                fastestInterval = 1000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            fusedLocationClient.lastLocation.addOnSuccessListener { location -> locationViewModel.setLocation(location)}

        } else {
            EasyPermissions.requestPermissions(this, "This app needs your location data to display it on map.", REQUEST_LOCATION_PERMISSIONS, ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val position = LatLng(0.0, 0.0)
        locationViewModel.userMarker = map.addMarker(MarkerOptions().position(position).title("Vaša trenutna lokacija."))
        map.moveCamera(CameraUpdateFactory.newLatLng(position))

       map.setOnMapClickListener {
           soundPool.play(pinSound, 1.0f, 1.0f, 1, 0, 1f)
           map.addMarker(MarkerOptions().position(it).icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(REQUEST_STORAGE_PERMISSIONS)
    fun takePhoto(){
        if(EasyPermissions.hasPermissions(this, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)){
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val location = locationViewModel.getLocation().value
            var place : String = location?.let { location -> geocoder.getFromLocation(location.latitude, location.longitude, 1)?.let { geoInfo -> geoInfo[0]?.locality ?: "unknown" } ?: "unknown" } ?: "unknown"
            val folder = File(externalMediaDirs[0], "Photos")
            folder.mkdirs();
            val file = File(folder,"IMG_${place}_${timeStamp}.jpg")
            file.createNewFile();
            imageViewModel.lastImage = file.absolutePath
            Log.i("app", imageViewModel.lastImage)
            val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)
            getImage.launch(uri)
        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to external files to store photo.", REQUEST_STORAGE_PERMISSIONS, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
        }
    }

    companion object{
        const val REQUEST_LOCATION_PERMISSIONS = 1;
        const val REQUEST_STORAGE_PERMISSIONS = 2;
    }
}