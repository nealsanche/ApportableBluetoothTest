package com.robotsandpencils.rendezvous;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class RendezvousManager {
    private static final String LOGTAG = "RendezvousManager";

    private static final int PORT = 7777;
    private static final int CLIENT_PORT = 7778;
    private static final int DISCOVERY_PORT = 7779;
    private static final int RECEIVE_TIMEOUT = 500;

    private final Context context;
    private AtomicBoolean isServer = new AtomicBoolean(false);
    private AtomicBoolean isClient = new AtomicBoolean(false);
    private InetAddress clientAddress;
    private InetAddress serverAddress;
    private int serverPort;
    private long timeStamp;
    private int localTieBreaker;

    private Thread serverThread;
    private Thread clientThread;

    private Throwable error = null;

    public RendezvousManager(Context context) {
        this.context = context;
    }

    private void startServerTask(final int port, final MultiplayerHandshakeDelegate delegate) {

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                Throwable error = null;
                String data = "Rendezvous";
                byte[] dataBytes = data.getBytes(Charset.forName("utf8"));

                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(PORT);
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                try {

                    if (socket != null) {
                        socket.setBroadcast(true);

                        DatagramPacket packet = new DatagramPacket(dataBytes, 0, dataBytes.length, getBroadcastAddress(), DISCOVERY_PORT);

                        boolean acknowledged = false;
                        while (!acknowledged && !isServer.get() && !isClient.get() && !Thread.currentThread().isInterrupted() && RendezvousManager.this.error == null) {
                            socket.send(packet);

                            try {
                                socket.setSoTimeout(RECEIVE_TIMEOUT);
                                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                                socket.receive(receivePacket);

                                if (!receivePacket.getAddress().getHostAddress().equals(getLocalAddress().getHostAddress())) {
                                    String req = new String(receivePacket.getData(), 0, receivePacket.getLength());

                                    if (req.equals("ACK")) {

                                        // Send an ACK back and also send port, timestamp and tiebreaker
                                        data = "ACK:" + port + ":" + timeStamp + ":" + localTieBreaker;
                                        dataBytes = data.getBytes(Charset.forName("utf8"));
                                        DatagramPacket responsePacket = new DatagramPacket(dataBytes, 0, dataBytes.length, receivePacket.getAddress(), receivePacket.getPort());

                                        // Triple send, this is UDP
                                        socket.send(responsePacket);
                                        socket.send(responsePacket);
                                        socket.send(responsePacket);
                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                // Expected and okay
                            }
                        }
                    }
                } catch (UnknownHostException e) {
                    error = e;
                } catch (SocketException e) {
                    error = e;
                } catch (IOException e) {
                    error = e;
                } finally {
                    if (socket != null) socket.close();
                }

                if (error != null) {
                    RendezvousManager.this.error = error;
                    delegate.rendevousError(error);
                }

                Log.v(LOGTAG, "Server thread finished.");
            }
        });

        serverThread.setName("Server thread.");
        serverThread.start();
    }

    private void startClientTask(final MultiplayerHandshakeDelegate delegate) {

        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {

                Throwable error = null;

                DatagramSocket clientSocket = null;
                try {
                    clientSocket = new DatagramSocket(DISCOVERY_PORT, getBroadcastAddress());
                } catch (IOException e) {
                    error = e;
                    RendezvousManager.this.error = e;
                }

                try {

                    boolean received = false;

                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

                    while (!received && !isServer.get() && !isClient.get() && !Thread.currentThread().isInterrupted() && RendezvousManager.this.error == null) {
                        try {
                            clientSocket.setSoTimeout(RECEIVE_TIMEOUT);
                            clientSocket.receive(receivePacket);

                            if (!receivePacket.getAddress().getHostAddress().equals(getLocalAddress().getHostAddress())) {
                                String req;

                                DatagramSocket sendSocket = new DatagramSocket(CLIENT_PORT);

                                String data = "ACK";
                                byte[] dataBytes = data.getBytes(Charset.forName("utf8"));
                                DatagramPacket responsePacket = new DatagramPacket(dataBytes, 0, dataBytes.length, receivePacket.getAddress(), receivePacket.getPort());

                                try {
                                    // Triple send, just in case since this is UDP
                                    sendSocket.send(responsePacket);
                                    sendSocket.send(responsePacket);
                                    sendSocket.send(responsePacket);

                                    receivePacket = new DatagramPacket(new byte[1024], 1024);
                                    sendSocket.receive(receivePacket);

                                    if (!receivePacket.getAddress().getHostAddress().equals(getLocalAddress().getHostAddress())) {
                                        req = new String(receivePacket.getData(), 0, receivePacket.getLength());

                                        if (req.startsWith("ACK:")) {

                                            String[] parts = req.split(":");

                                            serverPort = Integer.parseInt(parts[1]);
                                            long otherTimeStamp = Long.parseLong(parts[2]);
                                            int tieBreaker = Integer.parseInt(parts[3]);

                                            if (otherTimeStamp < timeStamp || (otherTimeStamp == timeStamp && tieBreaker < localTieBreaker)) {
                                                // We are the client, they are the server
                                                serverAddress = receivePacket.getAddress();
                                                delegate.clientNomination(serverAddress, serverPort);
                                                isClient.set(true);
                                            } else {
                                                // We are the server, they are the client
                                                clientAddress = receivePacket.getAddress();
                                                delegate.serverNomination(clientAddress);
                                                isServer.set(true);
                                            }

                                            received = true;
                                        }
                                    }

                                } finally {
                                    sendSocket.close();
                                }
                            }
                        } catch (SocketTimeoutException ex) {
                            continue;
                        }
                    }

                } catch (UnknownHostException e) {
                    error = e;
                } catch (SocketException e) {
                    error = e;
                } catch (IOException e) {
                    error = e;
                } finally {
                    if (clientSocket != null) clientSocket.close();
                }

                if (error != null) {
                    RendezvousManager.this.error = error;
                    delegate.rendevousError(error);
                }

                Log.v(LOGTAG, "Client thread finished.");
            }
        });

        clientThread.setName("Client Thread");
        clientThread.start();
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        if (broadcast == -1) throw new WifiNetworkUnavailableException("Wifi Network Unavailable.");
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    private InetAddress getLocalAddress() throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int addr = dhcp.ipAddress;
        if (addr == 0) throw new WifiNetworkUnavailableException("Wifi Network Unavailable.");
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((addr >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public void startRendezvous(int serverPort, MultiplayerHandshakeDelegate delegate) {
        isServer.set(false);
        isClient.set(false);

        // Generate a timestamp that will be used during nomination to choose the server
        timeStamp = System.currentTimeMillis();

        // Generate a random number, just in case both timeStamps are identical
        localTieBreaker = new SecureRandom().nextInt();

        error = null;
        startServerTask(serverPort, delegate);
        startClientTask(delegate);
    }

    public void cancelRendezvous() {
        if (serverThread.isAlive() && !serverThread.isInterrupted()) {
            serverThread.interrupt();
        }

        if (clientThread.isAlive() && !clientThread.isInterrupted()) {
            clientThread.interrupt();
        }
    }
}