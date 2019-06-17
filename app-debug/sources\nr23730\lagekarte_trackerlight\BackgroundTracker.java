package nr23730.lagekarte_trackerlight;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class BackgroundTracker extends Service {
    private static final int TWO_MINUTES = 240000;
    private int NOTIFICATION = R.string.app_name;
    private Location currentBest;
    private String deviceNumber;
    private NotificationManager mNM;
    Notification notification = null;
    int offset = 0;

    @SuppressLint({"WrongConstant"})
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        this.deviceNumber = getSharedPreferences("TrackerSettings", 0).getString("DeviceNumber", "0");
        if (this.deviceNumber.equals("0")) {
            return 2;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, BackgroundTracker.class), 0);
        if (VERSION.SDK_INT >= 26) {
            this.notification = new Builder(this).setSmallIcon(R.drawable.ic_launcher_background).setTicker(getString(R.string.starttext)).setWhen(System.currentTimeMillis()).setContentTitle(getString(R.string.started)).setContentText(getString(R.string.starttext)).setContentIntent(contentIntent).setChannelId(NotificationCompat.CATEGORY_STATUS).build();
        } else {
            this.notification = new Builder(this).setSmallIcon(R.drawable.ic_launcher_background).setTicker(getString(R.string.starttext)).setWhen(System.currentTimeMillis()).setContentTitle(getString(R.string.started)).setContentText(getString(R.string.starttext)).setContentIntent(contentIntent).build();
        }
        startForeground(this.NOTIFICATION, this.notification);
        StrictMode.setThreadPolicy(new ThreadPolicy.Builder().permitAll().build());
        this.mNM = (NotificationManager) getSystemService("notification");
        if (VERSION.SDK_INT >= 26) {
            this.mNM.createNotificationChannel(new NotificationChannel(NotificationCompat.CATEGORY_STATUS, getString(R.string.channel), 5));
        }
        LocationListener mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (BackgroundTracker.this.isBetterLocation(location, BackgroundTracker.this.currentBest)) {
                    BackgroundTracker.this.uploadPosition(location);
                    BackgroundTracker.this.currentBest = location;
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        LocationManager mLocationManager = (LocationManager) getSystemService("location");
        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") == 0 || ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") == 0) {
            mLocationManager.requestLocationUpdates("gps", 2000, 0.0f, mLocationListener);
            mLocationManager.requestLocationUpdates("network", 2000, 0.0f, mLocationListener);
        }
        return 1;
    }

    public void onDestroy() {
        super.onDestroy();
        this.mNM.cancel(this.NOTIFICATION);
    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while (i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private void uploadPosition(Location loc) {
        int batteryLevel = ((BatteryManager) getSystemService("batterymanager")).getIntProperty(4);
        HttpsURLConnection urlConnection = null;
        URL url = null;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("https://webapp.mobile-lagekarte.de/appservices/app-tracking-api/InsertData.php?id=");
            stringBuilder.append(getString(R.string.prefix));
            stringBuilder.append(this.deviceNumber);
            stringBuilder.append("&lat=");
            stringBuilder.append(loc.getLatitude());
            stringBuilder.append("&long=");
            stringBuilder.append(loc.getLongitude());
            stringBuilder.append("&orga=");
            stringBuilder.append(getString(R.string.account));
            stringBuilder.append("&plattform=golden-nougat-light&version=0.4&capacity=");
            stringBuilder.append(batteryLevel);
            url = new URL(stringBuilder.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        try {
            readStream(new BufferedInputStream(urlConnection.getInputStream()));
        } catch (IOException e22) {
            e22.printStackTrace();
        } catch (Throwable th) {
            urlConnection.disconnect();
        }
        urlConnection.disconnect();
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* Access modifiers changed, original: protected */
    public boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 240000;
        boolean isSignificantlyOlder = timeDelta < -240000;
        boolean z;
        if (timeDelta > 0) {
            z = true;
        } else {
            z = false;
        }
        if (isSignificantlyNewer) {
            return true;
        }
        if (isSignificantlyOlder) {
            return false;
        }
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());
        if (timeDelta > 180000 && isMoreAccurate) {
            return true;
        }
        if (timeDelta > 180000 && !isLessAccurate) {
            return true;
        }
        if (timeDelta <= 180000 || isSignificantlyLessAccurate || !isFromSameProvider) {
            return false;
        }
        return true;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 != null) {
            return provider1.equals(provider2);
        }
        return provider2 == null;
    }
}
