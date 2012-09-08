package eu.ttbox.smstraker.service.receiver;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import eu.ttbox.smstraker.core.Intents;
import eu.ttbox.smstraker.domain.GeoTrack;
import eu.ttbox.smstraker.domain.GeoTrackSmsMsg;
import eu.ttbox.smstraker.domain.GeoTrackerProvider;
import eu.ttbox.smstraker.domain.geotrack.GeoTrackHelper;
import eu.ttbox.smstraker.service.SmsMsgActionHelper;
import eu.ttbox.smstraker.service.SmsMsgEncryptHelper;

/**
 * @see http://www.tutos-android.com/broadcast-receiver-android
 * {link http://mobiforge.com/developing/story/sms-messaging-android}
 * @author deostem
 * 
 */
public class SMSReceiver extends BroadcastReceiver {

    private static final String TAG = "SMSReceiver";

    private final String ACTION_RECEIVE_SMS = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        
        if (intent.getAction().equals(ACTION_RECEIVE_SMS)) {
            Log.d(TAG, "SMSReceiver : " + intent);
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");

                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                if (messages.length > 0) {
                    SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean deleteSms = appPreferences.getBoolean("smsMonitorDelete", false);
                    // context.getSharedPreferences("smsMonitorDelete",
                    // Context.MODE_PRIVATE).getBoolean(key, defValue);
                    for (SmsMessage message : messages) {
                        boolean isConsume = consumeMessage(context, message);
                        if (!isConsume) {
                            deleteSms = false;
                        }
                        if (deleteSms) {
                            Log.w(TAG, "Cancel wanting abortBroadcast for unexpected Sms Message " + message.getMessageBody());
                        }
                    }
                    if (deleteSms) {
                        abortBroadcast();
                    }

                }
            }
        }
    }

    private boolean consumeMessage(Context context, SmsMessage message) {
        boolean isConsume = false;
        final String messageBody = message.getMessageBody();
        final String phoneNumber = message.getDisplayOriginatingAddress();
        Log.w(TAG, "Consume SMS Geo Action : " + phoneNumber +  " / " + messageBody);
        // Decrypt Msg
        GeoTrackSmsMsg clearMsg = SmsMsgEncryptHelper.decodeSmsMessage(phoneNumber, messageBody);
        if (clearMsg != null && clearMsg.action != null) {
            Log.w(TAG, "Receive SMS Geo Action : " + clearMsg.action);
             if (SmsMsgEncryptHelper.ACTION_GEO_LOC.equals(clearMsg.action)) {
                Location loc = SmsMsgActionHelper.fromSmsMessage(clearMsg.body);
                if (loc != null) {
                    manangeNewLocation(context, phoneNumber, loc);
                    isConsume = true;
                }
            } else if (SmsMsgEncryptHelper.ACTION_GEO_PING.equals(clearMsg.action)) {
//                isConsume = true;
                Log.d(TAG, "Receive SMS GeoPing request");
                Toast.makeText(context, "Geo Ping from : " + phoneNumber, Toast.LENGTH_LONG).show();
                Intent intent = Intents.sendGeoPingResponse(context, phoneNumber);
                context.startService(intent);
             } 
        }
        return isConsume;
    }

    private void manangeNewLocation(Context context, String phoneNumber, Location loc) {
        if (loc != null) {
            GeoTrack geoPoint = new GeoTrack(phoneNumber, loc);
            ContentValues values =   GeoTrackHelper.getContentValues(geoPoint);
            context.getContentResolver().insert(GeoTrackerProvider.Constants.CONTENT_URI, values);
//            GeoTrackDatabase trackingBDD = new GeoTrackDatabase(context);
//            trackingBDD.open();
//            trackingBDD.insertTrackPoint(geoPoint);
//            trackingBDD.close();
            Toast.makeText(context,
                    "Message : " + new Date(loc.getTime()).toLocaleString() + " (" + loc.getLatitude() + "," + loc.getLongitude() + " ~ " + loc.getAccuracy() + ") from " + phoneNumber,
                    Toast.LENGTH_LONG).show();
        }

    }

}
