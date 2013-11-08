package com.robotsandpencils.rendezvous;

import android.content.Context;
import android.os.Handler;

import java.net.InetAddress;

/**
 * A wrapper around the rendezvous process that delegates callbacks to native methods.
 * Created by neal on 11/6/2013.
 */
public class Rendezvous {

    private final RendezvousManager rendezvousManager;

    /**
     * Native method to handle a client nomination. Keep in mind the server may
     * take a while to set up its socket, so retry connections for a while.
     *
     * @param serverAddress the IP address of the server.
     * @param serverPort the port the server will be listening on.
     */
    private native void handleClientNomination(String serverAddress, int serverPort);

    /**
     * Native method to handle a server nomination. The server should start a listen
     * socket and wait for connection from the given client address.
     * @param clientAddress the client address to expect a connection from.
     */
    private native void handleServerNomination(String clientAddress);

    /**
     * Native method to handle a rendezvous error. When there is an error, the rendezvous is
     * also cancelled.
     * @param errorMessage the error message. This is not a message that should be directly displayed
     *                     to a user.
     */
    private native void handleRendezvousError(String errorMessage);

    private Handler handler;

    public Rendezvous(Context context) {
        rendezvousManager = new RendezvousManager(context);
        handler = new Handler();
    }

    /**
     * Start a rendezvous.
     * @param serverPort the server port to announce to the client.
     */
    public void startRendezvous(int serverPort) {

        rendezvousManager.startRendezvous(serverPort, new MultiplayerHandshakeDelegate() {
            @Override
            public void clientNomination(final InetAddress serverAddress, final int serverPort) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleClientNomination(serverAddress.getHostAddress(), serverPort);
                    }
                });
            }

            @Override
            public void serverNomination(final InetAddress clientAddress) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleServerNomination(clientAddress.getHostAddress());
                    }
                });
            }

            @Override
            public void rendevousError(final Throwable error) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleRendezvousError(error.getMessage());
                    }
                });
            }
        });
    }

    /**
     * Cancel a rendevous.
     */
    public void cancelRendezvous() {
        rendezvousManager.cancelRendezvous();
    }
}
