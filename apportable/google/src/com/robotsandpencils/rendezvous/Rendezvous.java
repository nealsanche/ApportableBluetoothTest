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

    private native void handleClientNomination(String serverAddress, int serverPort);

    private native void handleServerNomination(String clientAddress);

    private Handler handler;

    public Rendezvous(Context context) {
        rendezvousManager = new RendezvousManager(context);
        handler = new Handler();
    }

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
        });
    }

    public void cancelRendezvous() {
        rendezvousManager.cancelRendezvous();
    }
    
}
