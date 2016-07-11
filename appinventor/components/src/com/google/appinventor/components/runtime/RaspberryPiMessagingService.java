package com.google.appinventor.components.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings.Secure;
import android.util.Log;
import edu.mit.mqtt.raspberrypi.model.messaging.Topic;

/**
 * The service that handles the communication between the clients and the
 * brokers using the MQTT protocol.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 *
 */
public class RaspberryPiMessagingService implements MqttCallback {

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = "RaspberryPiMessagingService";

  private static final boolean CLEAN_SESSION = true; // Start a clean session?
  private static final String DEVICE_ID_FORMAT = "andr_%s"; // Device ID Format
  private static final String THREAD_NAME = "Thread[" + LOG_TAG + "]";
  private static final String MQTT_URL_FORMAT = "tcp://%s:%d"; // URL Format

  public static final int QOS_0 = 0; // Delivery Once no confirmation
  public static final int QOS_1 = 1; // Delivery at least once with confirmation
  public static final int QOS_2 = 2; // Delivery only once with confirmation
  // with handshake

  private MqttClient mClient; // MQTT Client
  private boolean mStarted = false; // Is the Client started?
  private Handler mConnHandler; // Seperate Handler thread for networking
  private MemoryPersistence mMemStore; // MemoryStore
  private MqttConnectOptions mOpts; // Connection Options
  private Handler parentHandler; // Handler from main thread

  private String mDeviceId; // Device ID, Secure.ANDROID_ID
  private String mIpAdress;
  private int mPort;
  private List<RaspberryPiMessageListener> mListeners;

  public RaspberryPiMessagingService(Context pContext, Handler pHandler) {
    super();

    if (DEBUG) {
      Log.d(LOG_TAG, "Inside the RaspberryPiMessagingService constructor.");
    }

    mMemStore = new MemoryPersistence();
    mOpts = new MqttConnectOptions();
    mOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    mOpts.setCleanSession(CLEAN_SESSION);

    mDeviceId = String.format(DEVICE_ID_FORMAT, Secure.ANDROID_ID);

    HandlerThread thread = new HandlerThread(THREAD_NAME);
    thread.start();

    mConnHandler = new Handler(thread.getLooper());

    parentHandler = pHandler;

    mListeners = new ArrayList<RaspberryPiMessageListener>();
  }

  public synchronized void subscribe(final String pTopic) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Calling the Subscribe method with topic " + pTopic + ".");
    }

    if (!isConnected()) {
      connect(mIpAdress, mPort);
    }

    mConnHandler.post(new Runnable() {
      @Override
      public void run() {
	try {
	  if (DEBUG) {
	    Log.d(LOG_TAG, "Subscribing to topic:\t " + pTopic + ".");
	  }
	  if (mClient.getTopic(pTopic) != null) {
	    mClient.subscribe(pTopic, QOS_0);
	    if (DEBUG) {
	      Log.d(LOG_TAG, "Subscribed to topic:\t" + pTopic);
	    }
	  }
	} catch (MqttException e) {
	  Log.e(LOG_TAG, "Failed to subscribe:  Topic:\t" + pTopic + "\tError: " + e.getMessage());
	}
      }
    });
  }

  public synchronized boolean unsubscribe(final String pTopic) {
    if (!isConnected()) {
      return false;
    }
    mConnHandler.post(new Runnable() {
      @Override
      public void run() {
	try {
	  if (DEBUG) {
	    Log.d(LOG_TAG, "Unsubscribing from topic:\t " + pTopic + ".");
	  }
	  mClient.unsubscribe(pTopic);
	  if (DEBUG) {
	    Log.d(LOG_TAG, "Unsubscribed from topic:\t" + pTopic);
	  }
	} catch (MqttException e) {
	  Log.e(LOG_TAG, "Failed to unsubscribe:  Topic:\t" + pTopic + "\tError: " + e.getMessage());
	}
      }
    });
    return true;
  }

  public synchronized void publish(final String pTopic, final String pMessage) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Calling the Publish method.");
    }

    if (!isConnected()) {
      connect(mIpAdress, mPort);
    }

    mConnHandler.post(new Runnable() {
      @Override
      public void run() {

	try {
	  if (DEBUG) {
	    Log.d(LOG_TAG, "Attempting to send a message:  Topic:\t" + pTopic + "  Message:\t" + pMessage);
	  }

	  MqttMessage message = new MqttMessage(pMessage.getBytes());
	  message.setQos(QOS_0);

	  mClient.publish(pTopic, message);
	  if (DEBUG) {
	    Log.d(LOG_TAG, "Sent a message:  Topic:\t" + pTopic + "  Message:\t" + pMessage);
	  }
	  mClient.disconnect();
	} catch (MqttException e) {
	  Log.e(LOG_TAG, "Failed to send a message:  Topic:\t" + pTopic + "  Message:\t" + pMessage + "\tError: "
              + e.getMessage());
	  e.printStackTrace();
	}
      }
    });
  }

  public void addListener(RaspberryPiMessageListener pListener) {
    mListeners.add(pListener);
  }

  public void removeListener(RaspberryPiMessageListener pListener) {
    mListeners.remove(pListener);
  }

  /**
   * Connects to the RaspberryPi Server which acts as the MQTT broker
   *
   * @param pBrokerIPAddress
   *          The address of the broker (such as an ip address)
   * @param pBrokerPort
   *          The port to connect on
   */
  public synchronized void connect(final String pIpAddress, final int pPort) {
    if (isConnected()) {
      return;
    }

    mIpAdress = pIpAddress;
    mPort = pPort;

    String url = String.format(Locale.US, MQTT_URL_FORMAT, pIpAddress, pPort);
    if (DEBUG) {
      Log.d(LOG_TAG, "Connecting with URL: " + url);
    }
    try {
      if (DEBUG) {
	Log.d(LOG_TAG, "Connecting...");
      }
      mClient = new MqttClient(url, mDeviceId, mMemStore);
    } catch (MqttException e) {
      e.printStackTrace();
    }

    mConnHandler.post(new Runnable() {
      @Override
      public void run() {
	try {
	  mClient.connect(mOpts);

	  mClient.setCallback(RaspberryPiMessagingService.this);

	  mStarted = true; // Service is now connected

	  if (DEBUG) {
	    Log.d(LOG_TAG, "Successfully connected");
	  }

	} catch (MqttException e) {
	  Log.e(LOG_TAG, "Unable to connect to the RaspberryPiSrever with address " + pIpAddress + ":" + pPort);
	  e.printStackTrace();
	}
      }
    });
  }

  public synchronized void disconnect() {
    if (!mStarted) {
      Log.e(LOG_TAG, "Cannot disconnect from a service that is not running!");
      return;
    }

    if (mClient != null) {
      mConnHandler.post(new Runnable() {

	@Override
	public void run() {
	  try {
	    mClient.disconnect();
	  } catch (MqttException e) {
	    Log.e(LOG_TAG, "Failed to disconnect." + "\tError: " + e.getMessage());
	  }
	  mClient = null;
	  mStarted = false;
	}
      });
    }

  }

  /**
   * Verifies the client state
   *
   * @return true if connected, false if we aren't connected
   */
  private boolean isConnected() {
    if (mStarted && mClient != null && !mClient.isConnected()) {
      Log.e(LOG_TAG, "MQTT Client not connected.");
      if (DEBUG) {
	Log.e(LOG_TAG, "isConnected[mStarted:" + mStarted + ", mClient:" + mClient + "]");
      }
    }

    return mClient != null && (mStarted && mClient.isConnected());
  }

  /**
   * Attempts to reconnect if network is available when connection is lost
   *
   * @param Throwable
   *          pThrowable
   */
  @Override
  public void connectionLost(final Throwable pThrowable) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Connection Lost...");
    }
    mClient = null;

    for (final RaspberryPiMessageListener listener : mListeners) {
      parentHandler.post(new Runnable() {
	@Override
	public void run() {
	  listener.MqttConnectionLost(pThrowable.getLocalizedMessage());
	}
      });
    }

  }

  /**
   * Called when a message is successfully sent
   *
   * @param pMqttDeliveryToken
   *          delivery token
   */
  @Override
  public void deliveryComplete(final IMqttDeliveryToken pMqttDeliveryToken) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Delivery Successful!");
    }

    for (final RaspberryPiMessageListener listener : mListeners) {
      parentHandler.post(new Runnable() {
	@Override
	public void run() {
	  try {
	    listener.MqttMessageSent(Arrays.asList(pMqttDeliveryToken.getTopics()),
                pMqttDeliveryToken.getMessage().toString());
	  } catch (MqttException e) {
	    Log.e(LOG_TAG, "Error seding the delivery successful message " + e.getMessage());
	  }
	}
      });
    }
  }

  /**
   * Called when a message arrives, forwards it to registered listeners
   *
   * @param pTopic
   *          The topic the message arrived from
   * @param pMqttMessage
   *          The contents of the message
   */
  @Override
  public void messageArrived(final String pTopic, final MqttMessage pMqttMessage) {
    if (DEBUG) {
      Log.d(LOG_TAG, "  Topic:\t" + pTopic + "  Message:\t" + new String(pMqttMessage.getPayload()) + "  QoS:\t"
          + pMqttMessage.getQos());
    }

    for (final RaspberryPiMessageListener listener : mListeners) {
      parentHandler.post(new Runnable() {
	@Override
	public void run() {
	  String payloadLiteral = new String(pMqttMessage.getPayload());
	  listener.MqttMessageReceived(pTopic, payloadLiteral);
	}
      });
    }

  }
}
