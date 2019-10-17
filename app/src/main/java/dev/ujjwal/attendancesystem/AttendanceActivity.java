package dev.ujjwal.attendancesystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceActivity extends AppCompatActivity implements View.OnClickListener {

    String TAG = "logtag";

    ImageView imageView;
    Button button;
    TextView textViewLatLong, textViewAddress;

    static final int REQUEST_TAKE_PHOTO = 1;

    String currentPhotoPath;

    final static int REQUEST_CHECK_SETTINGS = 199;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    Double latitude, longitude;
    String address;

    Boolean isFileSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        init();

        location();
        startLocationUpdates();

        button.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            permission();
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void permission() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //Log.i(TAG, "All location settings are satisfied.");
                        try {
                            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                startLocationUpdates();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(AttendanceActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            //Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    private void init() {
        imageView = findViewById(R.id.attendance_image);
        button = findViewById(R.id.attendance_capture_image);
        textViewLatLong = findViewById(R.id.attendance_lat_long);
        textViewAddress = findViewById(R.id.attendance_address);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.attendance_capture_image:
                isFileSaved = false;
                dispatchTakePictureIntent();
                break;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "dev.ujjwal.attendancesystem",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.i(TAG, currentPhotoPath);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            setPic();
        }
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        imageView.setImageBitmap(bitmap);
        createAttendanceJSON();
    }

    private void location() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Log.i(TAG, location.getLatitude() + "  " + location.getLongitude());

                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    String str = "";
                    try {
                        List<Address> listAddress = geocoder.getFromLocation(latitude, longitude, 1);
                        if (listAddress != null && listAddress.size() > 0) {
                            //Toast.makeText(MainActivity.this, listAddress.get(0).toString(), Toast.LENGTH_SHORT).show();
                            listAddress.get(0).getSubThoroughfare();
                            listAddress.get(0).getLocality();
                            listAddress.get(0).getPostalCode();
                            listAddress.get(0).getCountryName();
                            listAddress.get(0).getThoroughfare();

                            listAddress.get(0).getSubAdminArea();
                            listAddress.get(0).getPremises();
                            listAddress.get(0).getLocale();
                            listAddress.get(0).getFeatureName();
                            listAddress.get(0).getAdminArea();
                            listAddress.get(0).getSubLocality();


                            if (listAddress.get(0).getPremises() != null) {
                                str += listAddress.get(0).getPremises() + ", ";
                            }
                            if (listAddress.get(0).getSubLocality() != null) {
                                str += listAddress.get(0).getSubLocality() + ", ";
                            }
                            if (listAddress.get(0).getSubThoroughfare() != null) {
                                str += listAddress.get(0).getSubThoroughfare() + ", ";
                            }
                            if (listAddress.get(0).getThoroughfare() != null) {
                                str += listAddress.get(0).getThoroughfare() + ", ";
                            }
                            if (listAddress.get(0).getLocality() != null) {
                                str += listAddress.get(0).getLocality() + ", ";
                            }
                            if (listAddress.get(0).getAdminArea() != null) {
                                str += listAddress.get(0).getAdminArea() + ", ";
                            }
                            if (listAddress.get(0).getCountryName() != null) {
                                str += listAddress.get(0).getCountryName() + ", ";
                            }
                            if (listAddress.get(0).getPostalCode() != null) {
                                str += listAddress.get(0).getPostalCode();
                            }
                            address = str;
                            Log.i(TAG, address);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (!isFileSaved & latitude != null & longitude != null) {
                        textViewLatLong.setText("Latitude: " + latitude + "\nLongitude: " + longitude);
                        textViewAddress.setText(address);
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void createAttendanceJSON() {
//        Date date = new Date();
//        long time = date.getTime(); //Time in Milliseconds
//        Timestamp ts = new Timestamp(time);
//        Log.i(TAG, ts.toString());
        String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        Log.i(TAG, timeStamp);

        Gson gson = new Gson();
        Attendance attendance = new Attendance();
        attendance.setImage(currentPhotoPath);
        attendance.setTimestamp(timeStamp);
        attendance.setLatitude(latitude);
        attendance.setLongitude(longitude);
        attendance.setAddress(address);
        String json = gson.toJson(attendance);
        isFileSaved = true;

        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = openFileOutput(Constants.JSON_ATTENDANCE_FILE, MODE_PRIVATE);
            fileOutputStream.write(json.getBytes());

            Log.i(TAG, getFilesDir() + "/" + Constants.JSON_ATTENDANCE_FILE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                    readAttendanceJSON();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void readAttendanceJSON() {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = openFileInput(Constants.JSON_ATTENDANCE_FILE);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String text;

            while ((text = bufferedReader.readLine()) != null) {
                stringBuilder.append(text + "\n");
            }

            Log.i(TAG, stringBuilder.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
