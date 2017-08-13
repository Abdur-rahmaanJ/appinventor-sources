package org.thilanka.androidthings;

import java.util.List;

import org.thilanka.device.pin.PinDirection;
import org.thilanka.device.pin.PinProperty;
import org.thilanka.device.pin.PinValue;
import org.thilanka.messaging.domain.Action;
import org.thilanka.messaging.domain.Message;
import org.thilanka.messaging.domain.Payload;
import org.thilanka.messaging.domain.PeripheralIO;
import org.thilanka.messaging.error.ConnectionError;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

/**
 * {@link AndroidThingsGPIO} models any device attached to a GPIO pin of a Google
 * Android Things supported hardware platform. This also acts as an MQTT Client,
 * and will either publish or subscribe to certain topic(s). For example, a
 * temperature sensor attached to the board can publish to the topic
 * “temperature”, whereas an LED light can subscribe to the topic “temperature”,
 * and when a certain temperature has exceeded it may be programmed to flash.
 * The same LED light can subscribe to the topic “message”, and can be
 * programmed to flash in a different sequence when a message is received.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
@DesignerComponent(version = 1,
    description = "<p>A non-visible component that models a GPIO based device that can"
        +
        " be attached to a pin of an Android Things supported Hardware Platfrom.</p>",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, " + "android.permission.WAKE_LOCK, "
    + "android.permission.ACCESS_NETWORK_STATE, " + "android.permission.WRITE_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.1.1.jar, org.eclipse.paho.client.mqttv3-1.1.1.jar, gson-2.1.jar,"
    + " androidthings-messages-0.0.1-SNAPSHOT.jar")
public class AndroidThingsGPIO extends AndroidNonvisibleComponent implements Component, AndroidThingsMessageListener {

  // Component default values
  private static final String DEFAULT_PIN_NAME = "GPIO_34";
  private static final String DEFAULT_CONNECTED_DEVICE =
      "For e.g. LED, TemperatureSensor";

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = AndroidThingsGPIO.class.getSimpleName();

  private String mPinName;
  private boolean mIsOn = false; // false means OFF or LOW.
  private int mPinMode;
  private int mPullResistance;
  private String mConnectedDeviceName;
  private String mTopic;
  private String mMessage;
  private String mLastWillTopic;
  private String mLastWillMessage;
 
  /**
   * Designates if this pin receives inputs (i.e. sensors attached) or sends
   * output (i.e. LED indicator lights)
   */
  private PinDirection mPinDirection = PinDirection.OUT;
  /**
   * Designates the default value of this pin. This member variable is directly
   * related to boolean pinState.
   */
  private PinValue mPinState = PinValue.LOW;

  private AndroidThingsBoard mAndroidThingsBoard;

  private AndroidThingsMessagingService mAndroidThingsMessagingService;

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param pContainer
   *          the container that the component will be placed in
   */
  public AndroidThingsGPIO(ComponentContainer pContainer) {
    super(pContainer.$form());
    if (DEBUG) {
      Log.d(LOG_TAG, "Inside the AppInventorAndroidThingsPinClient Constructor.");
    }

    Activity context = pContainer.$context();
    Handler handler = new Handler(context.getMainLooper());

    mAndroidThingsMessagingService =
        new AndroidThingsMessagingService(context, handler);
    mAndroidThingsMessagingService.addListener(this);

  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = DEFAULT_PIN_NAME)
  @SimpleProperty(description = "The designated name for the Pin as specified in "
      + "the Android Things pinout.",
      userVisible = true)
  public void PinName(String pinName) {
    mPinName = pinName;
  }

  @SimpleProperty(description = "The assigned number for the pin in the Android "
      + "Things GPIO Header.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String PinName() {
    return mPinName;
  }

  @SimpleProperty(description = "Designates whether the pin is on or off.",
      userVisible = true)
  public void PinState(boolean pinState) {
    mIsOn = pinState;
    if (mPinName == null || mPinName.isEmpty()) {
      throw new ConnectionError("Pin Name not set!");
    }
    mPinState = pinState ? PinValue.HIGH : PinValue.LOW;

    Payload myPin = constructPayload(PinProperty.PIN_STATE, Action.EVENT);
    String message = Message.constructMessage(myPin);

    if (DEBUG) {
      Log.d(LOG_TAG, "Setting Pin " + mPinName + " to " + myPin.getValue()
          + " with this MQTT message: " + message);
    }

    Publish(mAndroidThingsBoard.getPublishTopic(), message);

    if (DEBUG) {
      Log.d(LOG_TAG, "Set Pin " + mPinName + " to " + myPin.getValue()
          + " with this MQTT message: " + message);
    }
  }

  @SimpleProperty(description = "Designates whether the pin is on or off.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public boolean PinState() {
    return mIsOn;
  }

  @SimpleProperty(description = "Designates what mode this pin is in.", userVisible = true)
  public void PinMode(int pinMode) {
    mPinMode = pinMode;
  }

  @SimpleProperty(description = "Designates what mode this pin is in.", userVisible = true)
  public int PinMode() {
    return mPinMode;
  }

  @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.",
      userVisible = true)
  public void PullResistance(int pullResistance) {
    mPullResistance = pullResistance;
  }

  @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public int PullResistance() {
    return mPullResistance;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, 
      defaultValue = DEFAULT_CONNECTED_DEVICE)
  @SimpleProperty(description = "Designates the type of device connected to the pin. "
      + "For e.g. LED, TemperatureSensor",
      userVisible = true)
  public void ConnectedDeviceName(String connectedDeviceName) {
    mConnectedDeviceName = connectedDeviceName;
  }

  @SimpleProperty(description = "Designates the type of device connected to the pin. "
      + "For e.g. LED, TemperatureSensor",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String ConnectedDeviceName() {
    return mConnectedDeviceName;
  }

  @SimpleProperty(description = "The topic of interest for this pin. For e.g. "
      + "if the pin is attached to a temperature sensor, the topic can be "
      + "'temperature'.",
      userVisible = true)
  public void Topic(String topic) {
    mTopic = topic;
  }

  @SimpleProperty(description = "The topic of interest for this pin. For e.g. "
      + "if the pin is attached to a temperature sensor, the topic can be "
      + "'temperature'.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String Topic() {
    return mTopic;
  }

  @SimpleProperty(description = "The message either sent to or received from the "
      + "endpoint device attached to the pin. For e.g. a TemperatureSensor can "
      + "publish '80' as the payload.",
      userVisible = true)
  public void Message(String message) {
    mMessage = message;
  }

  @SimpleProperty(description = "The message either sent to or received from the "
      + "endpoint device attached to the pin. For e.g. a TemperatureSensor can "
      + "publish '80' as the payload.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String Message() {
    return mMessage;
  }

  @SimpleProperty(description = "The topic to publish the lastWillMessage.",
      userVisible = true)
  public void LastWillTopic(String lastWillTopic) {
    mLastWillTopic = lastWillTopic;
  }

  @SimpleProperty(description = "The topic to publish the lastWillMessage.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String LastWillTopic() {
    return mLastWillTopic;
  }

  @SimpleProperty(description = "Message to be sent in the event the client disconnects.",
      userVisible = true)
  public void LastWillMessage(String lastWillMessage) {
    mLastWillMessage = lastWillMessage;
  }

  @SimpleProperty(description = "Message to be sent in the event the client disconnects.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String LastWillMessage() {
    return mLastWillMessage;
  }

  @SimpleFunction(description = "Changes the state of the pin from HIGH to LOW or vice versa.")
  public void Toggle() {
    mIsOn = !mIsOn;
  }

  @SimpleFunction(description = "Publish a message on the subject via the mqtt broker.")
  public void Publish(final String topic, final String message) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Sending message " + message + " on topic " + topic);
    }
    if (isMessagingServerConnected()) {
      mAndroidThingsMessagingService.publish(topic, message);
    }
    if (DEBUG) {
      Log.d(LOG_TAG, "Sent message " + message + " on topic " + topic);
    }
  }

  @SimpleFunction(description = "Subscribes to a topic on the given subject at the given mqttBrokerEndpoint. " +
      "The Subscribe, method has two parameters such as  String pMqttBrokerEndpoint, String pTopic . ")
  public synchronized void Subscribe(final String topic) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Subscribing to messages on topic " + topic);
    }
    if (isMessagingServerConnected()) {
      mAndroidThingsMessagingService.subscribe(topic);
    }
    if (DEBUG) {
      Log.d(LOG_TAG, "Subscribed to messages on topic " + topic);
    }
  }

  @SimpleFunction(description = "Unsubscribes to a topic on the given subject. The UnSubsribe, "
      + " method takes the parameter String pTopic . ")
  public void Unsubscribe(String topic) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Unsubscribing to messages on topic " + topic);
    }
    if (isMessagingServerConnected()) {
      mAndroidThingsMessagingService.unsubscribe(topic);
    }
    if (DEBUG) {
      Log.d(LOG_TAG, "Unsubscribed to messages on topic " + topic);
    }
  }

  /**
   * Connect to the MQTTBroker, and if the pinDirection is 'in', i.e. a sensor
   * or some other input is connected to the Android Things Board, a message is
   * sent announcing the pin that was registered. * @param pinName
   * @param pinName
   * @param androidThingsBoard
   * @param isOutput
   */
  @SimpleFunction(description = "Registers this pin with the AndroidThingsBoard "
      + "and designates the directionality of the pin, i.e. whether it is input "
      + "or output.")
  public void Register(String pinName, boolean isOutput,
      AndroidThingsBoard androidThingsBoard) {
    mPinName = pinName;
    mPinDirection = isOutput ? PinDirection.OUT : PinDirection.IN;
    mAndroidThingsBoard = androidThingsBoard;
    if (DEBUG) {
      Log.d(LOG_TAG, "Registered " + this + " to " + androidThingsBoard
          + " with direction " + mPinDirection);
    }

    String host = mAndroidThingsBoard.MessagingHost();
    int port = mAndroidThingsBoard.MessagingPort();
    if (DEBUG) {
      Log.d(LOG_TAG,
          "Connecting to the Messaging Server " + host + ":" + port);
    }
    mAndroidThingsMessagingService.connect(host, port);
    if (DEBUG) {
      Log.d(LOG_TAG, "Connected to the Messaging Server " + host + ":" + port);
    }

    if (mPinDirection == PinDirection.IN) {
      Payload myPin =
          constructPayload(PinProperty.REGISTER, Action.REGISTER);
      String message = Message.constructMessage(myPin);

      if (DEBUG) {
        Log.d(LOG_TAG, "Registering Pin " + mPinName
            + " with this " + AndroidThingsBoard.class.getSimpleName()
            + " with this MQTT message: " + message);
      }
      Publish(mAndroidThingsBoard.getPublishTopic(), message);
    }

    /*
     * Regardless of the direction of the pin App Inventor should subscribe to
     * the incoming topic. The topic App Inventor should subscribe is in the
     * form of <board_identifier>/appinventor
     */
    Subscribe(androidThingsBoard.getSubscribeTopic());
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed from HIGH to LOW, " +
      "or vice versa.")
  public void PinStateChanged() {
    if (DEBUG) {
      Log.d(LOG_TAG, "PinStateChanged: " + getClass().getSimpleName()
          + " pin " + mPinName
          + " state changed to " + mIsOn + ".");
    }
    EventDispatcher.dispatchEvent(this, "PinStateChanged");
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed to HIGH.")
  public void PinStateChangedToHigh() {
    if (DEBUG) {
      Log.d(LOG_TAG, "PinStateChangedToHigh: " + getClass().getSimpleName()
          + " pin " + mPinName
          + " state changed to " + mIsOn + ".");
    }
    EventDispatcher.dispatchEvent(this, "PinStateChangedToHigh");
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed to LOW.")
  public void PinStateChangedToLow() {
    if (DEBUG) {
      Log.d(LOG_TAG, "PinStateChangedToLow: " + getClass().getSimpleName()
          + " pin " + mPinName
          + " state changed to " + mIsOn + ".");
    }
    EventDispatcher.dispatchEvent(this, "PinStateChangedToLow");
  }

  @Override
  @SimpleEvent(description = "Event handler when a message is received through MQTT.")
  public void MessageReceived(String topic, String message) {
    if (DEBUG) {
      Log.d(LOG_TAG,
          "Mqtt Message " + message + " received on subject " + topic + ".");
    }
    if (mPinDirection.equals(PinDirection.IN)
        && topic.equals(mAndroidThingsBoard.getSubscribeTopic())) {
      Payload payload = Message.deconstrctMessage(message);
      if (DEBUG) {
        Log.d(LOG_TAG, "Received internal message for pin " + mPinName
            + ". Payload =" + payload);
      }
      if (payload.getName().equals(mPinName)) {
        if (payload.getProperty().equals(PinProperty.PIN_STATE)) {
          if (payload.getValue().equals(PinValue.HIGH)) {
            if (!mIsOn) {
              mIsOn = true;
              PinStateChanged();
            }
            PinStateChangedToHigh();
          } else if (payload.getValue().equals(PinValue.LOW)) {
            if (mIsOn) {
              mIsOn = false;
              PinStateChanged();
            }
            PinStateChangedToLow();
          }
        }
      }
    }
    /**
     * Finally, we need to dispatch this message regardless of the topic of the
     * message.
     */
    if (topic != null && message != null && message.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MessageReceived", topic,
          message);
    }
  }

  @Override
  @SimpleEvent(description = "Event handler when a message is sent through MQTT.")
  public void MessageSent(List<String> topics, String message) {
    if (DEBUG) {
      StringBuilder topicBuilder = new StringBuilder();
      for (String topic : topics) {
              topicBuilder.append(topic);
      }
      String allTopics = topicBuilder.toString();
      Log.d(LOG_TAG, "Mqtt Message " + message + " sent on subjects " + allTopics + ".");
    }
    if (topics != null && topics.size() > 0 && message != null && message.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MessageSent", topics, message);
    }
  }

  @Override
  @SimpleEvent(description = "Event handler when a message the MQTT connection is lost.")
  public void ConnectionLost(String error) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Connection via MQTT lost due to this error: " + error);
    }
    if (error != null) {
      EventDispatcher.dispatchEvent(this, "ConnectionLost", error);
    }
  }

  private boolean isMessagingServerConnected() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Messaging Server = " + mAndroidThingsBoard.MessagingHost()
          + ":" + mAndroidThingsBoard.MessagingPort());
    }
    if (mAndroidThingsBoard == null) {
      throw new ConnectionError(
          "The " + AndroidThingsGPIO.class.getSimpleName()
              + " must be registered with a "
              + AndroidThingsBoard.class.getSimpleName()
              + " to perform the action.");
    }
    return true;
  }

  private Payload constructPayload(PinProperty pProperty, Action pAction) {
    Payload payload = new Payload();
    payload.setPeripheralIO(PeripheralIO.GPIO);
    payload.setAction(pAction);
    payload.setName(mPinName);
    payload.setProperty(pProperty);
    payload.setValue(mPinState);
    payload.setDirection(mPinDirection);
    String hardwarePlatform = mAndroidThingsBoard.HardwarePlatform();
    payload.setAndroidThingsBoard(hardwarePlatform);
    payload.setLabel(mConnectedDeviceName);

    if (payload.isInvalid()) {
      throw new ConnectionError("All the required properties for the "
          + AndroidThingsGPIO.class.getSimpleName() + " not set. "
          + "Please check pinNumber, pinDirection, and the "
          + AndroidThingsBoard.class.getSimpleName() + " values. " + "The "
          + AndroidThingsBoard.class.getSimpleName()
          + " should have a hardware platform set.");
    }
    return payload;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": [ConnectedDeviceName:"
        + mConnectedDeviceName + ", LastWillMessage:" + mLastWillMessage
        + ", LastWillTopic:" + mLastWillTopic + ", PinDirection:"
        + mPinDirection + ", mqttMessage:" + mMessage + ", mqttTopic:" + mTopic
        + ", AndroidThingsMessagingService:" + mAndroidThingsMessagingService
        + ", PinMode:" + mPinMode + ", PinName:" + mPinName + ", IsOn:" + mIsOn
        + ", PullResistance:" + mPullResistance + ", AndroidThingsBoard:"
        + mAndroidThingsBoard + "]";
  }

}
