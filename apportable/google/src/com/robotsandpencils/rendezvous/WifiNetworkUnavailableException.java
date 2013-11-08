package com.robotsandpencils.rendezvous;

import java.io.IOException;

/**
 * This exception will be thrown if the Wifi Network cannot be determined.
 * Created by neal on 11/8/2013.
 */
public class WifiNetworkUnavailableException extends IOException {
    public WifiNetworkUnavailableException(String detailMessage) {
        super(detailMessage);
    }
}
