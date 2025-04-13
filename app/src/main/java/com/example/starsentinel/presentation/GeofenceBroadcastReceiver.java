package com.example.starsentinel.presentation;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        assert geofencingEvent != null;
        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            Log.e("Geofence", "Geofencing Error: " + errorCode);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        if (triggeringGeofences != null) {
            for (Geofence geofence : triggeringGeofences) {
                String message = null;
                switch (geofenceTransition) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        message = "Entered safe zone: " + geofence.getRequestId();
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        message = "Exited safe zone: " + geofence.getRequestId();
                        break;
                    default:
                        Log.e("Geofence", "Invalid transition type: " + geofenceTransition);
                }

                if (message != null) {
                    showNotification(context, message);
                    Log.d("Geofence", message);
                }
            }
        }
    }

    private void showNotification(Context context, String message) {
        createNotificationChannel(context);

        int notificationId = (int) System.currentTimeMillis();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Star Sentinel Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            Log.e("Geofence", "Notification permission not granted", e);
        }
    }

    private void createNotificationChannel(Context context) {
        CharSequence name = "Geofence Alerts";
        String description = "Notifications for geofence events";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private static final String CHANNEL_ID = "geofence_alerts_channel";
}