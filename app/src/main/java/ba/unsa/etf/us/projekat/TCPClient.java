package ba.unsa.etf.us.projekat;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

class TCPClient {

    // Server info
    private String SERVER_IP;
    private int SERVER_PORT;

    // Used to send messages
    private PrintWriter outBuffer;

    // Sends message received notifications
    private OnMessageReceived messageListener;

    // While this is true, the server will continue running
    private boolean running = false;

    // Used to read messages from the server
    private BufferedReader inBuffer;

    // Message from the server
    private String serverMessage;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     **/
    TCPClient(OnMessageReceived listener, String serverIP, int serverPort) {
        messageListener = listener;
        SERVER_IP = serverIP;
        SERVER_PORT = serverPort;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     **/
    void sendMessage(final char message) {
        if (outBuffer != null) {
            outBuffer.print(message);
            outBuffer.flush();
        }
    }

    /**
     * Close the connection and release
     **/
    void stopClient() {
        running = false;
        if (outBuffer != null) {
            outBuffer.flush();
            outBuffer.close();
        }

        messageListener = null;
        inBuffer = null;
        outBuffer = null;
        serverMessage = null;
    }

    void run() {
        running = true;
        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            try (Socket socket = new Socket(serverAddr, SERVER_PORT)) {
                outBuffer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                inBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (running) {
                    serverMessage = inBuffer.readLine();
                    if (serverMessage != null && messageListener != null)
                        messageListener.messageReceived(serverMessage);
                }

            } catch (Exception e) {
                Log.e("TCPClient ", "Exception: ", e);
            }

            // The socket must be closed. It is not possible to reconnect to this socket
            // after it is closed, which means a new socket instance has to be created.
        } catch (Exception e) {
            Log.e("TCPClient ", "Exception: ", e);
        }
    }

    // Declare the interface. The method messageReceived(String message) must be implemented in the Activity
    // class at on AsyncTask doInBackground
    public interface OnMessageReceived {
        void messageReceived(String message);
    }
}

