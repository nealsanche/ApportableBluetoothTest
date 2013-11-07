package com.robotsandpencils.rendezvous;

import java.net.InetAddress;

/**
 * Created by neal on 11/6/2013.
 */
public interface MultiplayerHandshakeDelegate {
    void clientNomination(InetAddress serverAddress, int serverPort);
    void serverNomination(InetAddress clientAddress);
}
