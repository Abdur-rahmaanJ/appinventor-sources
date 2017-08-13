package org.thilanka.androidthings;

import org.thilanka.device.pin.PinProperty;
import org.thilanka.device.pin.PinValue;
import org.thilanka.messaging.domain.Action;
import org.thilanka.messaging.domain.Message;
import org.thilanka.messaging.domain.Payload;
import org.thilanka.messaging.domain.PeripheralIO;
import org.thilanka.messaging.error.ConnectionError;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;

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
@DesignerComponent(version = 1, description = "<p>A non-visible component that models a PWM device that can"
    + " be attached to a pin of an Android Things supported Hardware Platfrom.</p>", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "images/extension.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, "
    + "android.permission.WAKE_LOCK, "
    + "android.permission.ACCESS_NETWORK_STATE, "
    + "android.permission.WRITE_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.1.1.jar, org.eclipse.paho.client.mqttv3-1.1.1.jar, gson-2.1.jar,"
    + " androidthings-messages-0.0.1-SNAPSHOT.jar")
public class AndroidThingsPwm extends AndroidNonvisibleComponent
    implements Component {

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = AndroidThingsPwm.class.getSimpleName();

  private AndroidThingsBoard mAndroidThingsBoard;

  private AndroidThingsMessagingService mAndroidThingsMessagingService;

  private String mPwmName;

  private boolean mEnabled;

  private double mFrequency;

  private double mDutyCycle;

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param pContainer
   *          the container that the component will be placed in
   */
  public AndroidThingsPwm(ComponentContainer pContainer) {
    super(pContainer.$form());
    if (DEBUG) {
      Log.d(LOG_TAG,
          "Inside the AppInventorAndroidThingsPinClient Constructor.");
    }

    Activity context = pContainer.$context();
    Handler handler = new Handler(context.getMainLooper());

    mAndroidThingsMessagingService =
        new AndroidThingsMessagingService(context, handler);
  }

  @SimpleProperty(description = "Sets the PWM Pin Identifier .", userVisible = true)
  public void PwmName(String pwmName) {
    mPwmName = pwmName;
  }

  @SimpleProperty(description = "Gets the PWM Pin Identifier.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String PwmName() {
    return mPwmName;
  }

  @SimpleProperty(description = "Designates whether the PWM is enabled or not.", userVisible = true)
  public void Enabled(boolean enabled) {
    if (mPwmName == null || mPwmName.isEmpty()) {
      throw new ConnectionError("PWM Name not set!");
    }

    mEnabled = enabled;

    Payload payload = constructPayload(PinProperty.PIN_STATE,
        enabled ? PinValue.HIGH : PinValue.LOW, 0D);
    String message = Message.constructMessage(payload);

    if (DEBUG) {
      Log.d(LOG_TAG, "Setting PWM Pin " + mPwmName + " to " + enabled
          + " with this MQTT message: " + message);
    }

    mAndroidThingsMessagingService
        .publish(mAndroidThingsBoard.getPublishTopic(), message);
  }

  @SimpleProperty(description = "Designates whether the PWM is on or off.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public boolean Enabled() {
    return mEnabled;
  }

  @SimpleProperty(description = "Set the duty cycle. It must be between 1 and 100", userVisible = true)
  public void DutyCycle(double dutyCycle) {
    if (mPwmName == null || mPwmName.isEmpty()) {
      throw new ConnectionError("PWM Name not set!");
    }

    mDutyCycle = dutyCycle;

    Payload payload = constructPayload(PinProperty.DUTY_CYCLE, null, dutyCycle);
    String message = Message.constructMessage(payload);

    if (DEBUG) {
      Log.d(LOG_TAG, "Setting PWM Pin " + mPwmName + " with duty cycle "
          + dutyCycle
          + " with this MQTT message: " + message);
    }

    mAndroidThingsMessagingService
        .publish(mAndroidThingsBoard.getPublishTopic(), message);
  }

  @SimpleProperty(description = "Get the duty cycle.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public double DutyCycle() {
    return mDutyCycle;
  }

  @SimpleProperty(description = "Set the frequency of the signal.", userVisible = true)
  public void Frequency(double frequency) {
    if (mPwmName == null || mPwmName.isEmpty()) {
      throw new ConnectionError("PWM Name not set!");
    }

    mFrequency = frequency;

    Payload payload = constructPayload(PinProperty.FREQUENCY, null, frequency);
    String message = Message.constructMessage(payload);

    if (DEBUG) {
      Log.d(LOG_TAG, "Setting PWM Pin " + mPwmName + " with frequency "
          + frequency + " with this MQTT message: " + message);
    }

    mAndroidThingsMessagingService
        .publish(mAndroidThingsBoard.getPublishTopic(), message);
  }

  @SimpleProperty(description = "Get the frequency of the signal.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public double Frequency() {
    return mFrequency;
  }

  /**
   * Connect to the MQTTBroker.
   * @param pwmName
   * @param androidThingsBoard
   */
  @SimpleFunction(description = "Registers this pin with the AndroidThingsBoard "
      + "and designates the directionality of the pin, i.e. whether it is input "
      + "or output.")
  public void Register(String pwmName, AndroidThingsBoard androidThingsBoard) {
    mPwmName = pwmName;
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

  /**
   * Construct and return the payload using the parameters given.
   * 
   * @param pProperty
   * @param pValue
   * @param pDoubleValue
   * @return the payload
   */
  private Payload constructPayload(PinProperty pProperty, PinValue pValue,
      double pDoubleValue) {
    Payload payload = new Payload();
    payload.setPeripheralIO(PeripheralIO.PWM);
    payload.setAction(Action.EVENT);
    payload.setName(mPwmName);
    payload.setProperty(pProperty);
    payload.setValue(pValue);
    payload.setDoubleValue(pDoubleValue);
    String hardwarePlatform = mAndroidThingsBoard.HardwarePlatform();
    payload.setAndroidThingsBoard(hardwarePlatform);
    payload.setLabel(mPwmName);
    return payload;
  }
}
