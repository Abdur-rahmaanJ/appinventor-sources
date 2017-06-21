package org.thilanka.raspberrypi;

import com.google.appinventor.components.runtime.errors.RuntimeError;

/**
 * This is for Connection Error that can be caused when using the MQTT protocol.
 *
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
public class ConnectionError extends RuntimeError {

  public ConnectionError(String pError) {
    super(pError);
  }
}
