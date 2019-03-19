/*
 * Copyright (c) 2013-2015 by appPlant UG. All rights reserved.
 *
 * @APPPLANT_LICENSE_HEADER_START@
 *
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apache License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://opensource.org/licenses/Apache-2.0/ and read it before using this
 * file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 *
 * @APPPLANT_LICENSE_HEADER_END@
 */

package de.appplant.cordova.plugin.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;
import java.util.Random;

import de.appplant.cordova.plugin.notification.Action;

/**
 * Builder class for local notifications. Build fully configured local
 * notification specified by JSON object passed from JS side.
 */
public class Builder {
    private static final String DEFAULT_CHANNEL_ID = "LOCAL_NOTIFICATION_PLUGIN";
    private static final String DEFAULT_CHANNEL_DESCRIPTION = "Notifications scheduled locally by the user";

    // Application context passed by constructor
    private final Context context;

    // Notification options passed by JS
    private final Options options;

    // Receiver to handle the trigger event
    private Class<?> triggerReceiver;

    // Receiver to handle the clear event
    private Class<?> clearReceiver = ClearReceiver.class;

    // Activity to handle the click event
    private Class<?> clickActivity = ClickActivity.class;

    /**
     * Constructor
     *
     * @param context
     *      Application context
     * @param options
     *      Notification options
     */
    public Builder(Context context, JSONObject options) {
        this.context = context;
        this.options = new Options(context).parse(options);
    }

    /**
     * Constructor
     *
     * @param options
     *      Notification options
     */
    public Builder(Options options) {
        this.context = options.getContext();
        this.options = options;
    }

    /**
     * Set trigger receiver.
     *
     * @param receiver
     *      Broadcast receiver
     */
    public Builder setTriggerReceiver(Class<?> receiver) {
        this.triggerReceiver = receiver;
        return this;
    }

    /**
     * Set clear receiver.
     *
     * @param receiver
     *      Broadcast receiver
     */
    public Builder setClearReceiver(Class<?> receiver) {
        this.clearReceiver = receiver;
        return this;
    }

    /**
     * Set click activity.
     *
     * @param activity
     *      Activity
     */
    public Builder setClickActivity(Class<?> activity) {
        this.clickActivity = activity;
        return this;
    }

    /**
     * Creates the notification with all its options passed through JS.
     */
    public Notification build() {
        Uri sound = options.getSoundUri();
        NotificationCompat.BigTextStyle style;
        NotificationCompat.Builder builder;

        style = new NotificationCompat.BigTextStyle()
                .bigText(options.getText());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannelIfNecessary();
            builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }

        builder.setDefaults(0)
                .setContentTitle(options.getTitle())
                .setContentText(options.getText())
                .setNumber(options.getBadgeNumber())
                .setTicker(options.getText())
                .setSmallIcon(options.getSmallIcon())
                .setLargeIcon(options.getIconBitmap())
                .setAutoCancel(options.isAutoClear())
                .setOngoing(options.isOngoing())
                .setStyle(style)
                .setLights(options.getLedColor(), 500, 500);

        if (sound != null) {
            builder.setSound(sound);
        }

        applyDeleteReceiver(builder);
        applyContentReceiver(builder);

        return new Notification(context, options, builder, triggerReceiver);
    }

    /**
     *
     */

    private void createChannelIfNecessary() {
        // only call on Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            List<NotificationChannel> channels = notificationManager.getNotificationChannels();

            for (int i = 0; i < channels.size(); i++) {
                String id = channels.get(i).getId();
                if (DEFAULT_CHANNEL_ID.equals(id)) {
                    return;
                }
            }

            NotificationChannel dChannel = new NotificationChannel(DEFAULT_CHANNEL_ID,
                    DEFAULT_CHANNEL_DESCRIPTION, NotificationManager.IMPORTANCE_DEFAULT);
            dChannel.enableVibration(true);
            notificationManager.createNotificationChannel(dChannel);
        }
    }

    /**
     * Set intent to handle the delete event. Will clean up some persisted
     * preferences.
     *
     * @param builder
     *      Local notification builder instance
     */
    private void applyDeleteReceiver(NotificationCompat.Builder builder) {

        if (clearReceiver == null)
            return;

        Intent deleteIntent = new Intent(context, clearReceiver)
                .setAction(options.getIdStr())
                .putExtra(Options.EXTRA, options.toString());

        PendingIntent dpi = PendingIntent.getBroadcast(
                context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        builder.setDeleteIntent(dpi);
    }

    /**
     * Set intents to handle the click event and other actions. Will bring the app to
     * foreground.
     *
     * @param builder
     *      Local notification builder instance
     */
    private void applyContentReceiver(NotificationCompat.Builder builder) {

        if (clickActivity == null)
            return;

        Intent intent = new Intent(context, clickActivity)
                .putExtra(Options.EXTRA, new String[]{ options.toString(), null })
                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        int requestCode = new Random().nextInt();

        PendingIntent contentIntent = PendingIntent.getActivity(
                context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Action[] actionsArray = options.getActions();
        if (actionsArray != null && actionsArray.length > 0) {
            for (Action action : actionsArray) {
                builder.addAction(action.getIcon(), action.getTitle(), getPendingIntentForAction(action));
            }
        }

        builder.setContentIntent(contentIntent);
    }

    /**
     * Returns a new PendingIntent for a notification action, including the
     * action's identifier.
     *
     * @param action
     *      Notification action needing the PendingIntent
     */
    private PendingIntent getPendingIntentForAction(Action action) {
        Intent intent = new Intent(context, clickActivity)
                .putExtra(Options.EXTRA, new String[]{ options.toString(), action.getIdentifier() })
                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        int requestCode = new Random().nextInt();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return pendingIntent;
    }

}
