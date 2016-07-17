package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import edu.mit.mqtt.raspberrypi.HeaderPin;
import edu.mit.mqtt.raspberrypi.Messages;
import edu.mit.mqtt.raspberrypi.model.device.PinDirection;
import edu.mit.mqtt.raspberrypi.model.device.PinProperty;
import edu.mit.mqtt.raspberrypi.model.device.PinValue;
import edu.mit.mqtt.raspberrypi.model.device.RaspberrryPiModel;
import edu.mit.mqtt.raspberrypi.model.messaging.Topic;

import com.google.appinventor.components.annotations.DesignerProperty;

import java.util.List;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.errors.ConnectionError;

/**
 * RaspberryPiPinClient models any device attached to a GPIO pin of the
 * RaspberryPi. This also acts as an MQTT Client, and will either publish or
 * subscribe to certain topic(s). For example, a temperature sensor attached to
 * the Raspberry Pi device can publish to the topic “temperature”, whereas an
 * LED light can subscribe to the topic “temperature”, and when a certain
 * temperature has exceeded it may be programmed to flash. The same LED light
 * can subscribe to the topic “message”, and can be programmed to flash in a
 * different sequence when a message is received.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
@DesignerComponent(version = YaVersion.RASPBERRYPI_COMPONENT_VERSION,
    description = "<p>A non-visible component that models any device that can" +
        " be attached to a pin of the Raspberry Pi.</p>",
    category = ComponentCategory.EXPERIMENTAL,
    nonVisible = true,
    iconName = "images/raspberryPi.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET, " + "android.permission.WAKE_LOCK, "
    + "android.permission.ACCESS_NETWORK_STATE, " + "android.permission.WRITE_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.0.2.jar, org.eclipse.paho.client.mqttv3-1.0.2.jar," +
    " gson-2.1.jar, raspberrypi-mqtt-messages-1.0-SNAPSHOT.jar")
public class RaspberryPiPinClient extends AndroidNonvisibleComponent implements Component, RaspberryPiMessageListener {

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = "RaspberryPiPinClient";

  private int pinNumber = -1;
  private boolean pinState = false; // false means OFF or LOW.
  private int pinMode;
  private int pullResistance;
  private String deviceName;
  private String mqttTopic;
  private String mqttMessage;
  private String lastWillTopic;
  private String lastWillMessage;
 
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

  private RaspberryPiServer raspberryPiServer;
  private RaspberryPiMessagingService mRaspberryPiMessagingService;

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param pContainer the container that the component will be placed in
   */
  public RaspberryPiPinClient(ComponentContainer pContainer) {
    super(pContainer.$form());
    if (DEBUG) {
      Log.d(LOG_TAG, "Inside the RaspberryPiPinclient Constructor.");
    }

    Activity context = pContainer.$context();
    Handler handler = new Handler(context.getMainLooper());

    mRaspberryPiMessagingService = new RaspberryPiMessagingService(context, handler);
    mRaspberryPiMessagingService.addListener(this);

  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = Component.RASPBERRYPI_PINCLIENT_NUMBER_VALUE + "")
  @SimpleProperty(description = "The assigned number for the pin in the RaspberryPi GPIO Header.",
      userVisible = true)
  public void PinNumber(int pPinNumber) {
    pinNumber = pPinNumber;
  }

  @SimpleProperty(description = "The assigned number for the pin in the RaspberryPi GPIO Header.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public int PinNumber() {
    return pinNumber;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = Component.RASPBERRYPI_PINCLIENT_STATE_VALUE + "")
  @SimpleProperty(description = "Designates whether the pin is on or off.",
      userVisible = true)
  public void PinState(boolean pPinState) {
    pinState = pPinState;
    if (pinNumber == -1) {
      throw new ConnectionError("Pin number not set!");
    }
    mPinState = pPinState ? PinValue.HIGH : PinValue.LOW;

    HeaderPin myPin = constructHeaderPin(PinProperty.PIN_STATE);
    String message = Messages.constructPinMessage(myPin);

    if (DEBUG) {
      Log.d(LOG_TAG, "Setting Pin " + pinNumber + " to " + myPin.value + " with this MQTT message: " + message);
    }
    mRaspberryPiMessagingService.publish(Topic.INTERNAL.toString(), message);
    if (DEBUG) {
      Log.d(LOG_TAG, "Set Pin " + pinNumber + " to " + myPin.value + " with this MQTT message: " + message);
    }
  }

  @SimpleProperty(description = "Designates whether the pin is on or off.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public boolean PinState() {
    return pinState;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
      defaultValue = Component.RASPBERRYPI_PINCLIENT_MODE_VALUE + "")
  @SimpleProperty(description = "Designates what mode this pin is in.", userVisible = true)
  public void PinMode(int pPinMode) {
    pinMode = pPinMode;
  }

  @SimpleProperty(description = "Designates what mode this pin is in.", userVisible = true)
  public int PinMode() {
    return pinMode;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = Component.RASPBERRYPI_PINCLIENT_PULLRESISTANCE_VALUE + "")
  @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.",
      userVisible = true)
  public void PullResistance(int pPullResistance) {
    pullResistance = pPullResistance;
  }

  @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public int PullResistance() {
    return pullResistance;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "Designates the type of device connected to the pin. For e.g. LED, TemperatureSensor",
      userVisible = true)
  public void DeviceName(String pDeviceName) {
    this.deviceName = pDeviceName;
  }

  @SimpleProperty(description = "Designates the type of device connected to the pin. For e.g. LED, TemperatureSensor",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String DeviceName() {
    return deviceName;
  }

  @SimpleProperty(description = "The topic of interest for this pin. For e.g. if the pin is attached to a " +
      "temperature sensor, the topic can be 'temperature'.",
      userVisible = true)
  public void MqttTopic(String pMqttTopic) {
    this.mqttTopic = pMqttTopic;
  }

  @SimpleProperty(description = "The topic of interest for this pin. For e.g. if the pin is attached to a " +
      "temperature sensor, the topic can be 'temperature'.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String MqttTopic() {
    return mqttTopic;
  }

  @SimpleProperty(description = "The message either sent to or received from the endpoint device attached to the pin. "
      + "For e.g. a TemperatureSensor can publish '80' as the payload.",
      userVisible = true)
  public void MqttMessage(String pMqttMessage) {
    this.mqttMessage = pMqttMessage;
  }

  @SimpleProperty(description = "The message either sent to or received from the endpoint device attached " +
      "to the pin. For e.g. a TemperatureSensor can publish '80' as the payload.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String MqttMessage() {
    return mqttMessage;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "The topic to publish the lastWillMessage.",
      userVisible = true)
  public void LastWillTopic(String pLastWillTopic) {
    this.lastWillTopic = pLastWillTopic;
  }

  @SimpleProperty(description = "The topic to publish the lastWillMessage.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String LastWillTopic() {
    return lastWillTopic;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "Message to be sent in the event the client disconnects.",
      userVisible = true)
  public void LastWillMessage(String pLastWillMessage) {
    this.lastWillMessage = pLastWillMessage;
  }

  @SimpleProperty(description = "Message to be sent in the event the client disconnects.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String LastWillMessage() {
    return lastWillMessage;
  }

  @SimpleFunction(description = "Changes the state of the pin from HIGH to LOW or vice versa.")
  public void Toggle() {
    pinState = !pinState;
  }

  @SimpleFunction(description = "Publish a message on the subject via the mqttBrokerEndpoint given. " +
      "The Publish method, takes three Parameters such as RaspberryPiServer, Topic and Message . ")
  public void Publish(final String topic, final String message) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Sending message " + message + " on topic " + topic);
    }
    if (raspberryPiServerConnected()) {
      mRaspberryPiMessagingService.publish(topic, message);
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
    if (raspberryPiServerConnected()) {
      mRaspberryPiMessagingService.subscribe(topic);
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
    if (raspberryPiServerConnected()) {
      mRaspberryPiMessagingService.unsubscribe(topic);
    }
    if (DEBUG) {
      Log.d(LOG_TAG, "Unsubscribed to messages on topic " + topic);
    }
  }

  /**
   * Connect to the MQTTBroker, and if the pinDirection is in, i.e. a sensor or
   * some other input is connected to the Raspberry Pi, a message is sent
   * announcing the pin that was registered.
   * 
   * @param raspberryPiServer
   * @param isOutput
   */
  @SimpleFunction(description = "Registers this pin with the RaspberryPiServer and designates the " +
      "directionality of the pin, i.e. whether it is input or output.")
  public void Register(int pin, boolean isOutput, RaspberryPiServer raspberryPiServer) {
    this.pinNumber = pin;
    this.mPinDirection = isOutput ? PinDirection.OUT : PinDirection.IN;
    this.raspberryPiServer = raspberryPiServer;
    if (DEBUG) {
      Log.d(LOG_TAG, "Registered " + this + " to " + raspberryPiServer + " with direction " + mPinDirection);
    }

    String ipv4Address = this.raspberryPiServer.Ipv4Address();
    int port = this.raspberryPiServer.Port();
    if (DEBUG) {
      Log.d(LOG_TAG, "Connecting to the RaspberryPiSever " + ipv4Address + ":" + port);
    }
    mRaspberryPiMessagingService.connect(ipv4Address, port);
    if (DEBUG) {
      Log.d(LOG_TAG, "Connected to the RaspberryPiSever " + ipv4Address + ":" + port);
    }

    if (mPinDirection.equals(PinDirection.IN)) {
      HeaderPin myPin = constructHeaderPin(PinProperty.REGISTER);
      String message = Messages.constructPinMessage(myPin);

      if (DEBUG) {
	      Log.d(LOG_TAG, "Registering Pin " + pinNumber
            + " with this RaspberryPiServer with this MQTT message: " + message);
      }
      mRaspberryPiMessagingService.publish(Topic.INTERNAL.toString(), message);
      if (DEBUG) {
	      Log.d(LOG_TAG, "Registering Pin " + pinNumber
            + " with this RaspberryPiServer with this MQTT message: " + message);
      }
      Subscribe(Topic.INTERNAL.toString());
    }
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed from HIGH to LOW, " +
      "or vice versa.")
  public void PinStateChanged() {
    if (DEBUG) {
      Log.d(LOG_TAG, "PinStateChanged: RaspberryPi pin " + pinNumber + " state changed to " + pinState + ".");
    }
    EventDispatcher.dispatchEvent(this, "PinStateChanged");
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed to HIGH.")
  public void PinStateChangedToHigh() {
    if (DEBUG) {
      Log.d(LOG_TAG, "PinStateChangedToHigh: RaspberryPi pin " + pinNumber + " state changed to " + pinState + ".");
    }
    EventDispatcher.dispatchEvent(this, "PinStateChangedToHigh");
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed to LOW.")
  public void PinStateChangedToLow() {
    if (DEBUG) {
      Log.d(LOG_TAG, "PinStateChangedToLow: RaspberryPi pin " + pinNumber + " state changed to " + pinState + ".");
    }
    EventDispatcher.dispatchEvent(this, "PinStateChangedToLow");
  }

  @SimpleEvent(description = "Event handler when the pin is connected to a device.")
  public void PinConnected() {
    if (DEBUG) {
      Log.d(LOG_TAG, "RaspberryPi pin " + pinNumber + " connected.");
    }
    // TODO what is the condition here?
    EventDispatcher.dispatchEvent(this, "PinConnected");
  }

  @Override
  @SimpleEvent(description = "Event handler when a message is received through MQTT.")
  public void MqttMessageReceived(String topic, String message) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Mqtt Message " + message + " received on subject " + topic + ".");
    }
    if (mPinDirection.equals(PinDirection.IN) && topic.equals(Topic.INTERNAL.toString())) {
      HeaderPin raspberryPiPin = Messages.deconstrctPinMessage(message);
      if (DEBUG) {
	      Log.d(LOG_TAG, "Received internal message for pin =" + raspberryPiPin);
      }
      if (raspberryPiPin.number == pinNumber) {
        if (raspberryPiPin.property.equals(PinProperty.PIN_STATE)) {
          if (raspberryPiPin.value.equals(PinValue.HIGH)) {
            if (!pinState) {
              pinState = true;
              PinStateChanged();
            }
            PinStateChangedToHigh();
          } else if (raspberryPiPin.value.equals(PinValue.LOW)) {
            if (pinState) {
              pinState = false;
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
      EventDispatcher.dispatchEvent(this, "MqttMessageReceived", topic, message);
    }
  }

  @Override
  @SimpleEvent(description = "Event handler when a message is sent through MQTT.")
  public void MqttMessageSent(List<String> topics, String message) {
    if (DEBUG) {
      StringBuilder topicBuilder = new StringBuilder();
      for (String topic : topics) {
	      topicBuilder.append(topic);
      }
      String allTopics = topicBuilder.toString();
      Log.d(LOG_TAG, "Mqtt Message " + message + " sent on subjects " + allTopics + ".");
    }
    if (topics != null && topics.size() > 0 && message != null && message.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MqttMessageSent", topics, message);
    }
  }

  @Override
  @SimpleEvent(description = "Event handler when a message the MQTT connection is lost.")
  public void MqttConnectionLost(String error) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Connection via MQTT lost due to this error: " + error);
    }
    if (error != null) {
      EventDispatcher.dispatchEvent(this, "MqttConnectionLost", error);
    }
  }

  private boolean raspberryPiServerConnected() {
    if (DEBUG) {
      Log.d(LOG_TAG, "RasberryPiServer = " + raspberryPiServer);
    }
    if (raspberryPiServer == null) {
      throw new ConnectionError(
          "The RaspberryPiPinClient must be registered with a RaspberryPiServer to perform the action.");
    }
    return true;
  }

  private HeaderPin constructHeaderPin(PinProperty pProperty) {
    HeaderPin myPin = new HeaderPin();
    myPin.number = pinNumber;
    myPin.property = pProperty;
    myPin.value = mPinState;
    myPin.direction = mPinDirection;
    myPin.raspberryPiModel = RaspberrryPiModel.fromString(raspberryPiServer.Model());
    myPin.label = deviceName;

    if (myPin.isInvalid()) {
      throw new ConnectionError("All the required properties for the RaspberryPiPinClient not set. " +
          "Please check pinNumber, pinDirection, and the raspberryPiServer. " +
          "The raspberryPiServer should have a model set.");
    }
    return myPin;
  }

  @Override
  public String toString() {
    return "RaspberryPiPinClient: [deviceName:" + deviceName
        + ", lastWillMessage:" + lastWillMessage + ", lastWillTopic:" + lastWillTopic + ", pinDirection:"
        + mPinDirection + ", mqttMessage:" + mqttMessage + ", mqttTopic:" + mqttTopic
        + ", mRaspberryPiMessagingService:" + mRaspberryPiMessagingService + ", pinMode:" + pinMode + ", pinNumber:"
        + pinNumber + ", pinState:" + pinState + ", pullResistance:" + pullResistance + ", raspberryPiServer:"
        + raspberryPiServer + "]";
  }

}