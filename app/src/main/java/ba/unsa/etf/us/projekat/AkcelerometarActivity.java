package ba.unsa.etf.us.projekat;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AkcelerometarActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tekstOPromjenama, xPozicija, yPozicija, zPozicija;
    private Button okButton, backButton;

    private ImageView[] imageViews = new ImageView[4];
    private int[] greenImages = {R.drawable.greenup, R.drawable.greendown, R.drawable.greenleft, R.drawable.greenright};
    private int[] whiteImages = {R.drawable.whiteup, R.drawable.whitedown, R.drawable.whiteleft, R.drawable.whiteright};

    private float x, y, z;  // podaci akcelerometra
    private char novi_x = 'C', novi_y = 'C';
    private char stari_x = 'C', stari_y = 'C';
    private Sensor sensor;
    private SensorManager sensorManager;

    private static TCPClient tcpClient;
    private static String serverIP;
    private static int serverPort;

    public enum Position {
        UP(0), DOWN(1), LEFT(2), RIGHT(3), CENTER(4);

        private final int value;

        Position(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akcelerometar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent i = getIntent();
        serverIP = i.getStringExtra("serverIP");
        serverPort = i.getIntExtra("serverPort", 1234);

        tekstOPromjenama = findViewById(R.id.tekst);
        xPozicija = findViewById(R.id.xPozicija);
        yPozicija = findViewById(R.id.yPozicija);
        zPozicija = findViewById(R.id.zPozicija);
        okButton = findViewById(R.id.okButton);
        backButton = findViewById(R.id.backButton);

        imageViews[0] = findViewById(R.id.upArrowIV);
        imageViews[1] = findViewById(R.id.downArrowIV);
        imageViews[2] = findViewById(R.id.leftArrowIV);
        imageViews[3] = findViewById(R.id.rightArrowIV);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);

        new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        Toast.makeText(AkcelerometarActivity.this, "Uspješno povezano!", Toast.LENGTH_SHORT).show();

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 'K');
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 'B');
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];

        if (x > -3.0 && x < 3.0 && y > -2.5 && y < 2.5) {
            novi_x = novi_y = 'C';
            if (novi_x != stari_x || novi_y != stari_y)
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, novi_x);
            tekstOPromjenama.setText("No tilt");
            updateArrows(Position.CENTER);
        } else if (Math.abs(x) > Math.abs(y)) {
            if (x < -3.0) {
                updateAndSend('U', true);
                tekstOPromjenama.setText("Up");
                updateArrows(Position.UP);
            } else if (x > 3.0) {
                updateAndSend('D', true);
                tekstOPromjenama.setText("Down");
                updateArrows(Position.DOWN);
            }
        } else {
            if (y > 2.5) {
                updateAndSend('R', false);
                tekstOPromjenama.setText("Right");
                updateArrows(Position.RIGHT);
            } else if (y < -2.5) {
                updateAndSend('L', false);
                tekstOPromjenama.setText("Left");
                updateArrows(Position.LEFT);
            }
        }

        stari_x = novi_x;
        stari_y = novi_y;
        xPozicija.setText("X = " + x);
        yPozicija.setText("Y = " + y);
        zPozicija.setText("Z = " + z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    void updateAndSend(char data, boolean upAndDown) {
        if (upAndDown) {
            novi_x = data;
            if (novi_x != stari_x)
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, novi_x);
        } else {
            novi_y = data;
            if (novi_y != stari_y)
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, novi_y);
        }
    }

    private void updateArrows(Position pos) {
        for (int i = 0; i < imageViews.length; i++)
            if (i == pos.getValue())
                imageViews[i].setImageResource(greenImages[i]);
            else
                imageViews[i].setImageResource(whiteImages[i]);
    }

    public static class SendMessageTask extends AsyncTask<Character, Void, Void> {

        @Override
        protected Void doInBackground(Character... params) {
            // send the message
            tcpClient.sendMessage(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            super.onPostExecute(nothing);
        }
    }


    public static class DisconnectTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            tcpClient.stopClient();
            tcpClient = null;
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            super.onPostExecute(nothing);
        }
    }

    public static class ConnectTask extends AsyncTask<String, String, TCPClient> {
        @Override
        protected TCPClient doInBackground(String... message) {
            tcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    publishProgress(message);
                }
            }, serverIP, serverPort);

            tcpClient.run();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new DisconnectTask().execute();
    }
}
