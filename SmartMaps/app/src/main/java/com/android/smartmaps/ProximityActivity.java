package com.android.smartmaps;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;


public class ProximityActivity extends Activity implements TextToSpeech.OnInitListener {

	private TextToSpeech textToSpeech;

	String notificationTitle;
	String notificationContent;
	String tickerMessage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		textToSpeech = new TextToSpeech(this, this);
		
		boolean proximity_entering = getIntent().getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
		
		double lat = getIntent().getDoubleExtra("lat", 0);
		
		double lng = getIntent().getDoubleExtra("lng", 0);

		int distance = getIntent().getIntExtra("distance", 0);
		
		String strLocation = Double.toString(lat)+","+Double.toString(lng);
		
		if(proximity_entering) {
			if(distance == SmartMaps.BEEP_RADIUS) {
				try {
					//Android default Beep sound
					playSound(getBaseContext());
				} catch (IOException e) {
					e.printStackTrace();
				}

				Toast.makeText(getBaseContext(), "Entering the region 20m", Toast.LENGTH_LONG).show();
				notificationTitle = "20m Proximity - Entry";
				notificationContent = "Entered the region:" + strLocation;
				tickerMessage = "Entered the region:" + strLocation;
			}
			else if(distance == SmartMaps.VOICE_RADIUS) {
				playVoice();
				Toast.makeText(getBaseContext(), "Entering the region 100m", Toast.LENGTH_LONG).show();
			}
		}
		else
		{
			if(distance == SmartMaps.BEEP_RADIUS) {
				Toast.makeText(getBaseContext(),"Exiting the region 20m"  ,Toast.LENGTH_LONG).show();
				notificationTitle = "20m Proximity - Exit";
				notificationContent = "Exited the region:" + strLocation;
				tickerMessage = "Exited the region:" + strLocation;
			}
			else if(distance == SmartMaps.VOICE_RADIUS) {
				Toast.makeText(getBaseContext(), "Exiting the region 100m", Toast.LENGTH_LONG).show();
			}
		}


		Intent notificationIntent = new Intent(getApplicationContext(),NotificationView.class);
		
		/** Adding content to the notificationIntent, which will be displayed on 
		 * viewing the notification
		 */
		notificationIntent.putExtra("content", notificationContent );
		
		/** This is needed to make this intent different from its previous intents */
		notificationIntent.setData(Uri.parse("tel:/"+ (int)System.currentTimeMillis()));

		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		/** Creating different tasks for each notification. See the flag Intent.FLAG_ACTIVITY_NEW_TASK */
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        
		/** Getting the System service NotificationManager */
        NotificationManager nManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		
        /** Configuring notification builder to create a notification */
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
							.setWhen(System.currentTimeMillis())
							.setContentText(notificationContent)
							.setContentTitle(notificationTitle)
							.setSmallIcon(R.drawable.ic_launcher)
							.setAutoCancel(true)
							.setTicker(tickerMessage)
							.setContentIntent(pendingIntent)
							.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		
		
		/** Creating a notification from the notification builder */
		Notification notification = notificationBuilder.build();
		
		/** Sending the notification to system. 
		 * The first argument ensures that each notification is having a unique id 
		 * If two notifications share same notification id, then the last notification replaces the first notification 
		 * */
		nManager.notify((int)System.currentTimeMillis(), notification);
		
		/** Finishes the execution of this activity */
		finish();
		
		
	}

    /**
     * Function to play the Beep sound when entered into proximity zone.
     * @param context
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IllegalStateException
     * @throws IOException
     */
	private void playSound(Context context) throws IllegalArgumentException,
			SecurityException,
			IllegalStateException, IOException {

		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		MediaPlayer mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setDataSource(context, soundUri);
		final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
		}
	}

	private void playVoice() {
		if (textToSpeech != null) {
			String text = "Accident Prone Area Ahead";
			if (text != null) {
				if (textToSpeech.isSpeaking())
					textToSpeech.stop();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
				} else {
					textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
				}
			}
		}
	}

	@Override
	public void onInit(int status) {
		if (status==TextToSpeech.SUCCESS) {
			textToSpeech.setLanguage(Locale.getDefault());
		} else {
			textToSpeech = null;
			Toast.makeText(this, "Failed to initialize TextToSpeech engine.",
					Toast.LENGTH_SHORT).show();
		}
	}

    @Override
    protected void onStop() {
        super.onStop();

        textToSpeech.shutdown();
    }

}
