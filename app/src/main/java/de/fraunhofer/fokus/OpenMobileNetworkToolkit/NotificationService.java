package de.fraunhofer.fokus.OpenMobileNetworkToolkit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.influxdb.client.domain.Run;

import java.util.Objects;

import de.fraunhofer.fokus.OpenMobileNetworkToolkit.DataProvider.DataProvider;
import de.fraunhofer.fokus.OpenMobileNetworkToolkit.Preferences.SPType;
import de.fraunhofer.fokus.OpenMobileNetworkToolkit.Preferences.SharedPreferencesGrouper;

public class NotificationService extends Service {

    private static final String TAG = "NotificationService";
    public NotificationManager nm;
    NotificationCompat.Builder builder;
    private Handler notificationHandler;
    private HandlerThread notificationHandlerThread;
    private SharedPreferencesGrouper spg;

    private GlobalVars gv;
    private DataProvider dp;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupNotificationUpdate() {
        Log.d(TAG, "setupNotificationUpdate");
        notificationHandlerThread = new HandlerThread("NotificationHandlerThread");
        notificationHandlerThread.start();
        notificationHandler = new Handler(Objects.requireNonNull(notificationHandlerThread.getLooper()));
        notificationHandler.post(servingCellNotificaiton);
    }

    private void stopNotificationUpdate() {
        Log.d(TAG, "stopNotificationUpdate");
        notificationHandler.removeCallbacks(servingCellNotificaiton);
        //builder.setContentText(null);
        //nm.notify(1, builder.build());
        nm.cancel(4);

        if (notificationHandlerThread != null) {
            notificationHandlerThread.quitSafely();
            try {
                notificationHandlerThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Exception happened!! "+e, e);
            }
            notificationHandlerThread = null;
        }
        onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        gv = GlobalVars.getInstance();
        dp = gv.get_dp();
        nm = getSystemService(NotificationManager.class);
        spg = SharedPreferencesGrouper.getInstance(this);
        spg.setListener((prefs, key) -> {
            if (key.equals("enable_radio_notification")) {
                if (prefs.getBoolean("enable_radio_notification", false)) {
                    if (prefs.getBoolean(key, false)) {
                        setupNotificationUpdate();
                    } else {
                        stopNotificationUpdate();
                    }
                }
            }
        }, SPType.default_sp);
        setupNotification();
        startForeground(4, builder.build());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupNotification() {
        // create intent for notifications
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        StringBuilder s = getStringBuilder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // create notification
            builder = new NotificationCompat.Builder(this, "OMNT_notification_channel")
                    .setContentTitle(getText(R.string.cell_notifcation))
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setColor(Color.WHITE)
                    .setContentIntent(pendingIntent)
                    // prevent to swipe the notification away
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(s))
                    // don't wait 10 seconds to show the notification
                    .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        } else {
            // create notification
            builder = new NotificationCompat.Builder(this, "OMNT_notification_channel")
                    .setContentTitle(getText(R.string.cell_notifcation))
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setColor(Color.WHITE)
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(s))
                    // prevent to swipe the notification away
                    .setOngoing(true);
        }
        setupNotificationUpdate();
    }
    private StringBuilder getStringBuilder(){
        StringBuilder s = dp.getRegisteredCells().get(0).getStringBuilder();
        return s;
    }
    private void updateNotification(){
        StringBuilder s = getStringBuilder();
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(s));
        nm.notify(4, builder.build());
    }

    Runnable servingCellNotificaiton = new Runnable() {
        @Override
        public void run() {
            updateNotification();
            notificationHandler.postDelayed(servingCellNotificaiton, 1000);

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
