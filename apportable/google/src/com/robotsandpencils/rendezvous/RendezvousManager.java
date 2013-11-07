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

    private Thread serverThread;
    private Thread clientThread;

    public RendezvousManager(Context context) {
        this.context = context;
    }

    private void startServerTask(final int port, final MultiplayerHandshakeDelegate delegate) {

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                String data = "Rendezvous";
                byte[] dataBytes = data.getBytes(Charset.forName("utf8"));

                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(PORT);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {

                    if (socket != null) {
                        socket.setBroadcast(true);

                        DatagramPacket packet = new DatagramPacket(dataBytes, 0, dataBytes.length, getBroadcastAddress(), DISCOVERY_PORT);

                        boolean acknowledged = false;
                        while (!acknowledged && !isServer.get() && !isClient.get() && !Thread.currentThread().isInterrupted()) {
                            socket.send(packet);

                            try {
                                socket.setSoTimeout(RECEIVE_TIMEOUT);
                                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                                socket.receive(receivePacket);

                                if (!receivePacket.getAddress().getHostAddress().equals(getLocalAddress().getHostAddress())) {
                                    String req = new String(receivePacket.getData(), 0, receivePacket.getLength());

                                    if (req.equals("ACK")) {

                                        // Send an ACK back and also send port
                                        data = "ACK:" + port;
                                        dataBytes = data.getBytes(Charset.forName("utf8"));
                                        DatagramPacket responsePacket = new DatagramPacket(dataBytes, 0, dataBytes.length, receivePacket.getAddress(), receivePacket.getPort());
                                        socket.send(responsePacket);

                                        if (!isClient.get() && isServer.compareAndSet(false, true)) {
                                            clientAddress = receivePacket.getAddress();
                                            acknowledged = true;

                                            delegate.serverNomination(clientAddress);
                                        }
                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                // Expected and okay
                            }
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) socket.close();
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

                DatagramSocket clientSocket = null;
                try {
                    clientSocket = new DatagramSocket(DISCOVERY_PORT, getBroadcastAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {

                    boolean received = false;

                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

                    while (!received && !isServer.get() && !isClient.get() && !Thread.currentThread().isInterrupted()) {
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
                                    sendSocket.send(responsePacket);
                                    receivePacket = new DatagramPacket(new byte[1024], 1024);
                                    sendSocket.receive(receivePacket);

                                    if (!receivePacket.getAddress().getHostAddress().equals(getLocalAddress().getHostAddress())) {
                                        req = new String(receivePacket.getData(), 0, receivePacket.getLength());

                                        if (req.startsWith("ACK:")) {

                                            serverPort = Integer.parseInt(req.substring(req.indexOf(':') + 1));

                                            if (!isServer.get() && isClient.compareAndSet(false, true)) {
                                                received = true;
                                                serverAddress = receivePacket.getAddress();
                                                delegate.clientNomination(serverAddress, serverPort);
                                            }
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

                    clientSocket.close();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    clientSocket.close();
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
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    private InetAddress getLocalAddress() throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int addr = dhcp.ipAddress;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((addr >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public void startRendezvous(int serverPort, MultiplayerHandshakeDelegate delegate) {
        isServer.set(false);
        isClient.set(false);
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