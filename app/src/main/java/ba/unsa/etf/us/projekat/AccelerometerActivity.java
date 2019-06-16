package ba.unsa.etf.us.projekat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    private TextView status;
    private TextView accelerometer_x, accelerometer_y, accelerometer_z;

    private ImageView[] imageViews = new ImageView[4];
    private int[] greenImages = {R.drawable.greenup, R.drawable.greendown, R.drawable.greenleft, R.drawable.greenright};
    private int[] whiteImages = {R.drawable.whiteup, R.drawable.whitedown, R.drawable.whiteleft, R.drawable.whiteright};

    // C - Center
    // L - Left
    // U - Up
    // R - Right
    // D - Down
    // K - Ok
    // B - Back
    private char new_x = 'C', new_y = 'C';
    private char old_x = 'C', old_y = 'C';

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
        setContentView(R.layout.activity_accelerometer);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = getIntent();
        serverIP = intent.getStringExtra("serverIP");
        serverPort = intent.getIntExtra("serverPort", 1234);

        status = findViewById(R.id.tekst);
        accelerometer_x = findViewById(R.id.xPozicija);
        accelerometer_y = findViewById(R.id.yPozicija);
        accelerometer_z = findViewById(R.id.zPozicija);

        imageViews[0] = findViewById(R.id.upArrowIV);
        imageViews[1] = findViewById(R.id.downArrowIV);
        imageViews[2] = findViewById(R.id.leftArrowIV);
        imageViews[3] = findViewById(R.id.rightArrowIV);

        // Get Accelerometer
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);

        new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        Toast.makeText(AccelerometerActivity.this, "Connection established!", Toast.LENGTH_SHORT).show();


        findViewById(R.id.okButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 'K');
            }
        });

        findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 'B');
            }
        });
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (x > -3.0 && x < 3.0 && y > -2.5 && y < 2.5) {
            new_x = new_y = 'C';
            if (new_x != old_x || new_y != old_y)
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new_x);
            status.setText("Centar");
            updateArrows(Position.CENTER);
        } else if (Math.abs(x) > Math.abs(y)) {
            if (x < -3.0) {
                updateAndSend('U', true);
                status.setText("Gore");
                updateArrows(Position.UP);
            } else if (x > 3.0) {
                updateAndSend('D', true);
                status.setText("Dolje");
                updateArrows(Position.DOWN);
            }
        } else {
            if (y > 2.5) {
                updateAndSend('R', false);
                status.setText("Desno");
                updateArrows(Position.RIGHT);
            } else if (y < -2.5) {
                updateAndSend('L', false);
                status.setText("Lijevo");
                updateArrows(Position.LEFT);
            }
        }

        old_x = new_x;
        old_y = new_y;

        // Yikes...
        accelerometer_x.setText("X = " + (double) Math.round(x * 1000000d) / 1000000d);
        accelerometer_y.setText("Y = " + (double) Math.round(y * 1000000d) / 1000000d);
        accelerometer_z.setText("Z = " + (double) Math.round(z * 1000000d) / 1000000d);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    void updateAndSend(char data, boolean upOrDown) {
        if (upOrDown) {
            new_x = data;
            if (new_x != old_x)
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new_x);
        } else {
            new_y = data;
            if (new_y != old_y)
                new SendMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new_y);
        }
    }

    private void updateArrows(Position pos) {
        for (int i = 0; i < imageViews.length; i++)
            if (i == pos.getValue())
                imageViews[i].setImageResource(greenImages[i]);
            else
                imageViews[i].setImageResource(whiteImages[i]);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new DisconnectTask().execute();
    }

    public static class SendMessageTask extends AsyncTask<Character, Void, Void> {

        @Override
        protected Void doInBackground(Character... params) {
            tcpClient.sendMessage(params[0]);
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
}
