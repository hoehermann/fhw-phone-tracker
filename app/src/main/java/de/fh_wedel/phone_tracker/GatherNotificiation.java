package de.fh_wedel.phone_tracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * Created by marcus on 19/06/15.
 */
public class GatherNotificiation extends Notification {
    public static void Show(Context context,
                            NotificationCompat.Builder builder,
                            NotificationManager notificationManager) {
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, ListWLAN.class);


        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ListWLAN.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        // mId allows you to update the notification later on.
        notificationManager.notify(0, builder.build());
    }


    public static void ShowActive(NotificationManager notificationManager, Context context) {
        NotificationCompat.Builder builder;
        builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.abc_btn_check_material)
                .setContentTitle("Gathering BSSIDs ...")
                .setContentText("Hello World!");

        Show(context, builder, notificationManager);
    }

}
