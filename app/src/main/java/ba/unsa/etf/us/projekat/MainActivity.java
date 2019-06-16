package ba.unsa.etf.us.projekat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    private EditText et_serverIP;
    private EditText et_serverPort;

    private String serverIP;
    private Integer serverPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_serverIP = findViewById(R.id.et_serverIP);
        et_serverPort = findViewById(R.id.et_serverPort);

        findViewById(R.id.connectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputIsValid()) {
                    Intent intent = new Intent(MainActivity.this, AccelerometerActivity.class);
                    intent.putExtra("serverIP", serverIP);
                    intent.putExtra("serverPort", serverPort);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "IP Adresa/Port nisu validni i/ili dostižni!", Toast.LENGTH_SHORT).show();
                    et_serverIP.setError("Unesite ponovo!");
                    et_serverPort.setError("Unesite ponovo!");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        et_serverIP.setError(null);
        et_serverPort.setError(null);
    }

    private boolean inputIsValid() {
        serverIP = et_serverIP.getText().toString();
        serverPort = Integer.valueOf(et_serverPort.getText().toString());

        try {
            if (Patterns.IP_ADDRESS.matcher(serverIP).matches() && serverPort > 0 && serverPort < 65535)
                return serverIsReachable(serverIP, serverPort);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean serverIsReachable(final String ip, final int port) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result = executor.submit(new Callable<Boolean>() {
            public Boolean call() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(ip);
                    Socket socket = new Socket(serverAddr, port);
                    socket.close();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        try {
            return result.get();
        } catch (Exception exception) {
            return false;
        }
    }
}
