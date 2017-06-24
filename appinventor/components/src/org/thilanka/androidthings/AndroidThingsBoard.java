package org.thilanka.androidthings;

import java.util.List;

import org.thilanka.messaging.domain.Action;
import org.thilanka.messaging.domain.Topic;
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
 * Non-visible component that models the Android Things Board connected to an
 * MQTT broker. This class acts as a mediator that relays messages between the
 * {@link AndroidThingsPin} objects.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
@DesignerComponent(description = "<p>Non-visible component that models the "
    + "Android Things Board connected to an MQTT broker. This class acts as a "
    + "mediator that relays messages between AndroidThingsPin objects.</p>",
    category = ComponentCategory.EXPERIMENTAL,
    nonVisible = true,
    iconName = "images/extension.png", 
    version = 1)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.1.1.jar, org.eclipse.paho.client.mqttv3-1.1.1.jar, gson-2.1.jar,"
    + " androidthings-messages-0.0.1-SNAPSHOT.jar")
public class AndroidThingsBoard extends AndroidNonvisibleComponent
    implements Component, AndroidThingsMessageListener {

  // Component default values
  private static final String DEFAULT_HARDWARE_PLATFORM = "RaspberryPi 3";
  private static final String DEFAULT_MESSAGING_HOST = "iot.eclipse.org";
  private static final int DEFAULT_MESSAGING_PORT_VALUE = 1883;
  private static final String DEFAULT_BOARD_IDENTIFIER = "Enter Your Board Id";

  private static final boolean DEBUG = true;
  private static final String LOG_TAG = "AppInventorAndroidThingsServer";

  private String mHardwarePlatform;
  private String mMessagingHost;
  private int mMessagingPort;
  private String mBoardIdentifier;
  private int mPins;
  private boolean pShutdown = false;

  private AndroidThingsMessagingService mAndroidThingsMessagingService;

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param pContainer the container that the component will be placed in
   */
  public AndroidThingsBoard(ComponentContainer pContainer) {
    super(pContainer.$form());

    if (DEBUG) {
      Log.d(LOG_TAG, "Inside the AndroidThingsServer Constructor.");
    }

    Activity context = pContainer.$context();
    Handler handler = new Handler(context.getMainLooper());

    mAndroidThingsMessagingService = new AndroidThingsMessagingService(context, handler);
    mAndroidThingsMessagingService.addListener(this);

  }

  /**
   * The Supported Android Things Hardware Platform. As of June 2017, the
   * supported platforms are Intel Edison, Intel Joule, NXP i.MX6UL, NXP i.MX7D
   * and RaspberryPi 3.
   *
   * @param hardwarePlatform
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = DEFAULT_HARDWARE_PLATFORM)
  @SimpleProperty(description = "Sets the supported Android Things Hardware "
      + "Platform such as Intel Edison, Intel Joule, NXP i.MX6UL, NXP i.MX7D "
      + "or RaspberryPi 3.",
      userVisible = true)
  public void HardwarePlatform(String hardwarePlatform) {
    mHardwarePlatform = hardwarePlatform;
  }

  /**
   * Set the string that uniquely identifies the Android Things board that is
   * being configured by the app.
   *
   * @param boardIdentifier
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, 
      defaultValue = DEFAULT_BOARD_IDENTIFIER)
  @SimpleProperty(description = "Sets the string that uniquely identifies the "
      + "Android Things board that is being configured by the app.", userVisible = true)
  public void BoardIdentifier(String boardIdentifier) {
    mBoardIdentifier = boardIdentifier;
  }

  /**
   * Returns the model of the Android Things supported Hardware Platform.
   *
   * @return model
   */
  @SimpleProperty(description = "Returns the Android Things supported Hardware Platform",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String HardwarePlatform() {
    return mHardwarePlatform;
  }

  /**
   * Returns the string that uniquely identifies the Android Things board that
   * is being configured by the app.
   * 
   * @return model
   */
  @SimpleProperty(description = "Returns the string that uniquely identifies the "
      + "Android Things board that is being configured by the app.", 
      category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String BoardIdentifier() {
    return mBoardIdentifier;
  }

  /**
   * The ipv4 address of the Messaging Server Host. Typically, an MQTT broker
   * would be hosted on this host.
   * 
   * @param messagingHost
   *          the server that runs the messaging service.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = DEFAULT_MESSAGING_HOST)
  @SimpleProperty(description = "The host name or the IPAddress of the server "
      + "acting as an MQTT broker or any other external MQTT broker "
      + "(such as iot.eclipse.org:1883) used in the communication.",
      userVisible = true)
  public void MessagingHost(String messagingHost) {
    mMessagingHost = messagingHost;
  }

  /**
   * Returns the ipv4 address of the Messaging Server Host.
   *
   * @return Ipv4Address
   */
  @SimpleProperty(description = "Returns the ipv4 address of the Messaging Server Host.",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public String MessagingHost() {
    return mMessagingHost;
  }

  /**
   * Returns the TCP/IP port that the Messaging Broker is running on.
   * 
   * @param port
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = DEFAULT_MESSAGING_PORT_VALUE + "")
  @SimpleProperty(description = "Returns the TCP/IP port that the Messaging Broker is running on.",
      userVisible = true)
  public void MessagingPort(int port) {
    if (port >= 1024 && port <= 65535) {
      mMessagingPort = port;
    } else {
      throw new ConnectionError("Please enter a valid port number. You entered " + port);
    }
  }

  /**
   * Returns the TCP/IP port that the Messaging Broker is running on.
   *
   * @return port
   */
  @SimpleProperty(description = "Returns the TCP/IP port that the Messaging Broker "
      + "is running on",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
  public int MessagingPort() {
    return mMessagingPort;
  }

  /**
   * Initializes the Android Things Board to send and receive MQTT messages.
   * 
   * @param hardwarePlatform
   * @param messagingHost
   * @param messagingPort
   */
  @SimpleFunction(description = "Initializes the AndroidThingsBoard component "
      + "with the given Hardware Platform, Messaging Host, Port and the identifier "
      + "given by the companion app running on the AndroidThings device.")
  public void Initialize(String identifier, String hardwarePlatform,
      String messagingHost, int messagingPort) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Initializing the AndroidThingsBoard's properties...");
    }
    mBoardIdentifier = identifier;
    mHardwarePlatform = hardwarePlatform;
    mMessagingHost = messagingHost;
    mMessagingPort = messagingPort;
  }
 
  /**
   * Checks if the given pin can work on the Android Things Board being used.
   * 
   * @param pinNumber
   * @return true if this pin is available on the specified Android Things Board
   *         model.
   */
  @SimpleFunction(description = "Returns true if this pin is available on the "
      + "specified Android Things Board model.")
  public boolean HasPin(int pinNumber) {
    return pinNumber <= mPins && pinNumber > 0;
  }

  /**
   * Shutdown the Android Things Board GPIO provider.
   */
  @SimpleFunction(description = "Shutdown the Android Things Board GPIO provider.")
  public void Shutdown() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Shutting down the Android Things Board...");
    }
    pShutdown = true;
    // TODO
    mAndroidThingsMessagingService.publish(getInternalTopic(), Action.SHUTDOWN.name());
    if (DEBUG) {
      Log.d(LOG_TAG, "Completed shutting down the Android Things Board.");
    }
  }

  /**
   * Determines the status of the RaspberryPi GPIO Provider.
   * 
   * @return true if the RaspberryPi GPIO provider has been shutdown.
   */
  @SimpleFunction(description = "Returns true if the RaspberryPi GPIO provider "
      + "has been shutdown.")
  public boolean IsShutdown() {
    return pShutdown;
  }

  /**
   * Connected pins have active devices attached to.
   * 
   * @return the number of connected clients.
   */
  @SimpleFunction(description = "Connected pins have active devices attached to.")
  public int ConnectedPins() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Getting the connected pins...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO pass the message
    return 0;
  }

  /**
   * Pins that now have disconnected devices.
   * 
   * @return the number of pins that were registered with the broker, but are
   *         now disconnected.
   */
  @SimpleFunction(description = "Pins that now have disconnected devices.")
  public int DisconnectedPins() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Getting the disconnected pins...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO pass the message
    return 0;
  }

  /**
   * Returns true if the given pin name is connected to a device.
   * 
   * @param pinName
   * @return true if the client with the given pin name is connected.
   */
  @SimpleFunction(description = "Returns true if the given pin name is "
      + "connected to a device.")
  public boolean IsPinConnected(String pinName) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Checking if the client is connected...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO
    return false;
  }
 
  /**
   * Indicates that the RaspberryPiServer has shutdown.
   * 
   * @return true when the server has shutdown.
   */
  @SimpleEvent(description = "Indicates that the RaspberryPiServer has shutdown.")
  public boolean HasShutdown() {
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if the RaspberryPi Server has shutdown.");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO subscribe to a message from the broker via the
    // RaspberryPiAppInventorCompanion
    return false;
  }

  /**
   * Indicates whether the given pin has connected to a device.
   * 
   * @return true when the given client has connected.
   */
  @SimpleEvent(description = "Indicates whether the given pin has connected to a"
      + " device.")
  public boolean PinConnected(String pinName) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if a client has connected.");
      Log.d(LOG_TAG, "To be implemented.");
    }
    // TODO subscribe to a message from the broker
    return false;
  }

  /**
   * Indicates whether the given pin has disconnected from the device that was
   * previously attached to.
   * 
   * @param pinName
   * @return true if the given pin has an active client
   */
  @SimpleEvent(description = "Indicates whether the given pin has disconnected "
      + "from the device that was previously attached to..")
  public boolean PinDisconnected(String pinName) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if a client has diconnected.");
      Log.d(LOG_TAG, "To be implemented. ");
    }
    // TODO subscribe to a message from the broker
    return false;
  }

  /**
   * Indicates that a message is received through MQTT.
   * 
   * @param topic
   * @param message
   */
  @Override
  @SimpleEvent(description = "Indicates that a message is received through MQTT.")
  public void MessageReceived(String topic, String message) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Mqtt Message " + message + " received on subject " + topic + ".");
    }
    if (topic != null && message != null && message.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MessageReceived", topic, message);
    }
  }

  /**
   * Indicates that a given message belonging to the given topics is sent
   * through MQTT.
   * 
   * @param topics
   * @param message
   */
  @Override
  @SimpleEvent(description = "Indicates that a given message belonging to the "
      + "given topics is sent through MQTT.")
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

  /**
   * Indicates that the MQTT connection was lost.
   * 
   * @param error
   */
  @Override
  @SimpleEvent(description = "Indicates that the MQTT connection was lost.")
  public void ConnectionLost(String error) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Connection via MQTT lost due to this error: " + error);
    }
    if (error != null) {
      EventDispatcher.dispatchEvent(this, "ConnectionLost", error);
    }
  }

  @Override
  public String toString() {
    return "RaspberryPiServer[ipv4Address:" + mMessagingHost + ", port:"
        + mMessagingPort
        + ", model:" + mHardwarePlatform + ", pins:" + mPins + "]";
  }

  protected String getInternalTopic() {
    StringBuilder topicBuilder = new StringBuilder();
    topicBuilder.append(Topic.INTERNAL.toString());
    topicBuilder.append(mBoardIdentifier);
    return topicBuilder.toString();
  }

}
