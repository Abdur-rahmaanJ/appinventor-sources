package org.thilanka.androidthings;

import java.util.List;

import org.thilanka.device.pin.PinProperty;
import org.thilanka.messaging.domain.Action;
import org.thilanka.messaging.domain.Message;
import org.thilanka.messaging.domain.Payload;
import org.thilanka.messaging.domain.PeripheralIO;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

/**
 * {@link AndroidThingsPwm} models a PWM device attached to an output pin of a
 * Google Android Things supported hardware platform, such as a servo motor or a
 * speaker or an LCD screen display. We can apply a proportional control signal
 * to the external device using a digital output pin. For more information,
 * please see
 * <a href="https://developer.android.com/things/sdk/pio/pwm.html">https://
 * developer.android.com/things/sdk/pio/pwm.html</a>.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
@DesignerComponent(version = 1, description = "<p>A non-visible component that "
    + "models a Temperature Sensor avalable via the Bmx280SensorDriver that can"
    + " be attached to a pin of an Android Things supported Hardware Platfrom.</p>", 
    category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "images/extension.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, "
    + "android.permission.WAKE_LOCK, "
    + "android.permission.ACCESS_NETWORK_STATE, "
    + "android.permission.WRITE_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.1.1.jar, org.eclipse.paho.client.mqttv3-1.1.1.jar, gson-2.1.jar,"
    + " androidthings-messages-0.0.1-SNAPSHOT.jar")
public class AndroidThingsTemperatureSensor extends AndroidNonvisibleComponent
    implements Component, AndroidThingsMessageListener {

  private static final boolean DEBUG = true;

  private final static String LOG_TAG =
      AndroidThingsTemperatureSensor.class.getSimpleName();

  private AndroidThingsBoard mAndroidThingsBoard;

  private AndroidThingsMessagingService mAndroidThingsMessagingService;

  private double mTemperature;

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param pContainer
   *          the container that the component will be placed in
   */
  public AndroidThingsTemperatureSensor(ComponentContainer pContainer) {
    super(pContainer.$form());
    if (DEBUG) {
      Log.d(LOG_TAG,
          "Inside the " + AndroidThingsTemperatureSensor.class.getSimpleName()
              + " Constructor.");
    }

    Activity context = pContainer.$context();
    Handler handler = new Handler(context.getMainLooper());

    mAndroidThingsMessagingService =
        new AndroidThingsMessagingService(context, handler);
    mAndroidThingsMessagingService.addListener(this);
  }

  @SimpleProperty(description = "Gets the last reported temperature from the Temperature Sensor.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public double Temperature() {
    return mTemperature;
  }

  /**
   * Connect to the MQTTBroker.
   * 
   * @param pwmName
   * @param androidThingsBoard
   */
  @SimpleFunction(description = "Registers this temperature sensor with the AndroidThingsBoard ")
  public void Register(AndroidThingsBoard androidThingsBoard) {
    mAndroidThingsBoard = androidThingsBoard;
    if (DEBUG) {
      Log.d(LOG_TAG, "Registered " + this + " to " + androidThingsBoard);
    }
    String host = mAndroidThingsBoard.MessagingHost();
    int port = mAndroidThingsBoard.MessagingPort();
    if (DEBUG) {
      Log.d(LOG_TAG, "Connecting to the Messaging Server " + host + ":" + port);
    }
    mAndroidThingsMessagingService.connect(host, port);
    if (DEBUG) {
      Log.d(LOG_TAG, "Connected to the Messaging Server " + host + ":" + port);
    }
  }

  @SimpleFunction(description = "Monitor the temperature reported by the temperature sensor.")
  public void Monitor() {
    Payload payload = constructPayload(PinProperty.TEMPERATURE, Action.MONITOR);
    String message = Message.constructMessage(payload);

    if (DEBUG) {
      Log.d(LOG_TAG,
          "Sending the MQTT message to monitor temperatures from the sensor : "
              + message);
    }

    mAndroidThingsMessagingService
        .publish(mAndroidThingsBoard.getPublishTopic(), message);
    mAndroidThingsMessagingService
        .subscribe(mAndroidThingsBoard.getSubscribeTopic());
  }

  /**
   * Construct the payload.
   * 
   * @param pProperty
   * @param pAction
   * @return the payload
   */
  private Payload constructPayload(PinProperty pProperty, Action pAction) {
    Payload payload = new Payload();
    payload.setPeripheralIO(PeripheralIO.TEMPERATURE_SENSOR);
    payload.setAction(pAction);
    payload.setProperty(pProperty);
    String hardwarePlatform = mAndroidThingsBoard.HardwarePlatform();
    payload.setAndroidThingsBoard(hardwarePlatform);

    return payload;
  }

  @Override
  public void MessageReceived(String pTopic, String pMessage) {
    if (DEBUG) {
      Log.d(LOG_TAG,
          "Mqtt Message " + pMessage + " received on subject " + pTopic + ".");
    }
    Payload payload = Message.deconstrctMessage(pMessage);
    mTemperature = payload.getDoubleValue();
    TemperatureChanged();
    /**
     * Finally, we need to dispatch this message regardless of the topic of the
     * message.
     */
    EventDispatcher.dispatchEvent(this, "MessageReceived", pTopic, pMessage);
  }

  @SimpleEvent(description = "Event handler that indicates that the temperature of the sensor has changed. "
      + "or vice versa.")
  public double TemperatureChanged() {
    if (DEBUG) {
      Log.d(LOG_TAG, "TemperatureChanged to " + mTemperature + ".");
    }
    EventDispatcher.dispatchEvent(this, "TemperatureChanged");
    return mTemperature;
  }

  @Override
  public void MessageSent(List<String> topics, String message) {
    if (DEBUG) {
      StringBuilder topicBuilder = new StringBuilder();
      for (String topic : topics) {
        topicBuilder.append(topic);
      }
      String allTopics = topicBuilder.toString();
      Log.d(LOG_TAG,
          "Mqtt Message " + message + " sent on subjects " + allTopics + ".");
    }
    if (topics != null && topics.size() > 0 && message != null
        && message.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MessageSent", topics, message);
    }
  }

  @Override
  public void ConnectionLost(String error) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Connection via MQTT lost due to this error: " + error);
    }
    if (error != null) {
      EventDispatcher.dispatchEvent(this, "ConnectionLost", error);
    }
  }

}
