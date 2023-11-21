package com.example.arduinoconnector;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static java.security.AccessController.getContext;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.PowerManager;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.arduinoconnector.databinding.ActivityMainBinding;
import com.google.gson.Gson;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements FirstFragment.OnFirstFragmentEvent {
    private ActivityMainBinding binding;
    private Fragment currentFragment;
    private FirstFragment firstFragment;
    private SecondFragment secondFragment;
    private SharedViewModel sharedViewModel;
    private int sensorPositionSel = -1;
    private List<Sensor> sensors = new ArrayList<>();

    //Variables para que no apague la pantalla
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private Date dateReading, dateRecord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
        } catch (Exception ex) {
            String a = ex.getMessage();
        }
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        this.inicializar();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Ocultar la barra de navegación
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        //Evitar que se apague la pantalla
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        /*Intent kioskIntent = new Intent(this, MainActivity.class);
        kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(kioskIntent);*/

    }

    public void obtenerDirecciones(){
        try {
            JSONObject command = new JSONObject();
            command.put("cmd", "getAddr");
            String commandString = command.toString() + "\n"; // Añadir un carácter de nueva línea para indicar el fin del comando
            this.sendDataBluetooth(commandString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void inicializar(){
        this.inicializarSensores();
        this.CrearFragmentos();
        this.configurarBluetooth();
        this.IniciarServidor();
        //Iniciando timer
        dataRecordingTimer = new Timer();
        //this.resetDataFile();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dateReading = Date.from(Instant.now());
            dateRecord = dateReading;
        }
    }

    private void CrearFragmentos() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        this.firstFragment = new FirstFragment(this, sensors);
        this.secondFragment = new SecondFragment();

        transaction.add(R.id.fragment_container, this.firstFragment);
        transaction.commit();

        currentFragment = firstFragment; // Establece el fragmento actual

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Registra el receptor BluetoothStateReceiver
        /*IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);*/

    }

    private void inicializarSensores() {
        Sensor sensor;
        this.sensors = new ArrayList<>();
        sensor = new Sensor(0, 0, 0, "off", "idle", 0, 0);
        sensor.titulo = "Cuarto 1";
        sensor.cuartoNum = 1;
        this.sensors.add(sensor);
        sensor = new Sensor(0, 0, 0, "off", "idle", 0, 0);
        sensor.titulo = "Cuarto 2";
        sensor.cuartoNum = 2;
        this.sensors.add(sensor);
        sensor = new Sensor(0, 0, 0, "off", "idle", 0, 0);
        sensor.titulo = "Cuarto 3";
        sensor.cuartoNum = 3;
        this.sensors.add(sensor);
        sensor = new Sensor(0, 0, 0, "off", "idle", 0, 0);
        sensor.titulo = "Cuarto 4";
        sensor.cuartoNum = 4;
        this.sensors.add(sensor);
        sensor = new Sensor(0, 0, 0, "off", "idle", 0, 0);
        sensor.titulo = "Cuarto 5";
        sensor.cuartoNum = 5;
        this.sensors.add(sensor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    //-------------------------------------------------------------------
    //-------------------------------------------------------------------
    //----------------------ciclos de vida-------------------------------
    //-------------------------------------------------------------------
    //-------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        configurarBluetooth();
        try {
            if (mBluetoothSocket == null || !mBluetoothSocket.isConnected()) {
                connectBluetoothDevice();
            }
            this.setBluetoothConnectionControl(
                    mBluetoothSocket.isConnected() ?
                            FirstFragment.ConnectionStatusEnum.ONLINE :
                            FirstFragment.ConnectionStatusEnum.OFFLINE
            );
        } catch (Exception e) {
            e.printStackTrace();
            // Manejar la excepción aquí, por ejemplo, mostrando un mensaje de error al usuario.
            Toast.makeText(this, "Error al conectar con el dispositivo Bluetooth", Toast.LENGTH_SHORT).show();
        }

        // Asegúrate de mantener la pantalla encendida mientras la actividad está en primer plano
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE, "TuApp:WakeLockTag");
        wakeLock.acquire();
    }


    @Override
    public void onPause() {
        super.onPause();
        try {
            stopRecording();
            if(mBluetoothDataReadThread != null)
                mBluetoothDataReadThread.interrupt();
            if(mInputStream != null)
                mInputStream.close();
            if(mOutputStream != null)
                mOutputStream.close();
            mBluetoothSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Libera el WakeLock cuando la actividad se pausa o detiene
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    protected void onDestroy() {
        server.stop();
        super.onDestroy();
        stopRecording();
        // Desregistra el receptor BluetoothStateReceiver
        //unregisterReceiver(bluetoothStateReceiver);
    }

    //-------------------------------------------------------------------
    //-------------------------------------------------------------------
    //----------------------bluetooth------------------------------------
    //-------------------------------------------------------------------
    //-------------------------------------------------------------------

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;
    private OutputStream mOutputStream;
    private InputStream mInputStream;


    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID estándar para la comunicación serie
    private Thread mBluetoothDataReadThread;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show();
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void configurarBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_SHORT).show();
            //finish();
        }
    }

    private void connectBluetoothDevice() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {

            Set<BluetoothDevice> pairedDevices = null;
            try {
                pairedDevices = mBluetoothAdapter.getBondedDevices();
            } catch (SecurityException e) {
                Toast.makeText(this, "Permiso Bluetooth denegado", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    mBluetoothDevice = device;
                    break;
                }
            }

            try {
                if (mBluetoothSocket == null || !mBluetoothSocket.isConnected()) {
                    mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    mBluetoothSocket.connect();
                    mOutputStream = mBluetoothSocket.getOutputStream();
                    mInputStream = mBluetoothSocket.getInputStream();

                    if (mBluetoothDataReadThread != null && mBluetoothDataReadThread.isAlive()) {
                        mBluetoothDataReadThread.interrupt();
                    }

                    mBluetoothDataReadThread = createBluetoothDataReadThread();
                    mBluetoothDataReadThread.start();
                    this.obtenerDirecciones();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private boolean isBluetoothEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public void sendDataBluetooth(String data) {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Aca llegan todos los mensajes de bluetooth
    private Thread createBluetoothDataReadThread() {
        return new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder readMessage = new StringBuilder();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    bytes = mInputStream.read(buffer);
                    String currentMessage = new String(buffer, 0, bytes);
                    readMessage.append(currentMessage);

                    String[] jsonStrings = readMessage.toString().split("\n");

                    for (String jsonString : jsonStrings) {
                        if (isValidJson(jsonString)) {
                            final Activity activity = this;
                            // Actualiza la interfaz de usuario en el hilo principal
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processReceivedJson(jsonString);
                                }
                            });
                        }
                    }

                    // Elimina los JSON completos del buffer
                    int lastNewlineIndex = readMessage.lastIndexOf("\n");
                    if (lastNewlineIndex >= 0 && lastNewlineIndex < readMessage.length() - 1) {
                        readMessage.delete(0, lastNewlineIndex + 1);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    this.LoopReconnection();
                    break;
                }
            }
        });
    }

    private boolean isValidJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            // Verifica que el JSON tenga una propiedad "type"
            if (jsonObject.has("type")) {
                String typeValue = jsonObject.getString("type");

                // Verifica que el valor de "type" sea "data" o "addresses"
                if ("data".equals(typeValue) || "addresses".equals(typeValue)) {
                    return true;
                }
            }

            return false;
        } catch (JSONException e) {
            return false;
        }
    }



    private void startReconnection() {
        setBluetoothConnectionControl(FirstFragment.ConnectionStatusEnum.WAITING);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mBluetoothSocket.close();
                    while (mBluetoothSocket.isConnected()) {
                        // Espera que se desconecte
                        Thread.sleep(100);
                    }

                    for (int i = 0; i < 3; i++) {
                        connectBluetoothDevice();
                        if(mBluetoothSocket.isConnected())
                            break;
                        Thread.sleep(2000);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FirstFragment.ConnectionStatusEnum status =
                                    mBluetoothSocket.isConnected() ?
                                            FirstFragment.ConnectionStatusEnum.ONLINE :
                                            FirstFragment.ConnectionStatusEnum.OFFLINE;
                            setBluetoothConnectionControl(status);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

// Llamas a startReconnection desde el hilo principal cuando quieras iniciar la reconexión.

    private void LoopReconnection(){
        try {
            mBluetoothSocket.close();
            setBluetoothConnectionControl(FirstFragment.ConnectionStatusEnum.WAITING);

            while(mBluetoothSocket.isConnected()){
                //Se espera que se desconecte
                Thread.sleep(100);
            }

            for(int i = 0; i < 3; i++) {
                this.connectBluetoothDevice();
                if(mBluetoothSocket.isConnected())
                    break;
                Thread.sleep(2000);
            }

            setBluetoothConnectionControl(
                    mBluetoothSocket.isConnected() ?
                            FirstFragment.ConnectionStatusEnum.ONLINE :
                            FirstFragment.ConnectionStatusEnum.OFFLINE
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setBluetoothConnectionControl(FirstFragment.ConnectionStatusEnum isConnected){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentFragment instanceof FirstFragment) {
                    // FirstFragment está actualmente visible
                    sharedViewModel.setIsBluetoothConnected(isConnected);
                }
            }
        });
    }

    @Override
    public void onClickParameter(int position) {
        sensorPositionSel = position;
        if (!secondFragment.isDetached()){
            secondFragment.SetPosition(position);
            navigateToSecondFragment();
            //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        }
    }

    @Override
    public void onClickConectarDispositivo() {
        if(!mBluetoothSocket.isConnected()) {
            this.startReconnection();
        }else{
            FirstFragment.ConnectionStatusEnum status =
                    mBluetoothSocket.isConnected() ?
                            FirstFragment.ConnectionStatusEnum.ONLINE :
                            FirstFragment.ConnectionStatusEnum.OFFLINE;
            setBluetoothConnectionControl(status);
        }
    }

    @Override
    public void onCLickReconectarDispositivo() {
        this.startReconnection();
    }

    public void navigateToSecondFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if(secondFragment.isAdded()) {
            transaction.show(secondFragment);
        } else {
            transaction.add(R.id.fragment_container, secondFragment);
        }
        transaction.hide(firstFragment);
        transaction.commit();

        currentFragment = secondFragment; // Actualiza el fragmento actual
    }

    public void navigateToFirstFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if(firstFragment.isAdded()) {
            transaction.show(firstFragment);
        } else {
            transaction.add(R.id.fragment_container, firstFragment);
        }
        transaction.hide(secondFragment);
        transaction.commit();

        currentFragment = firstFragment; // Actualiza el fragmento actual
    }

    // Método para validar y procesar JSON
    private void processReceivedJson(String jsonString) {
        try {
            // Obtén el objeto JSON raíz
            JSONObject jsonObject = new JSONObject(jsonString);

            // Comprueba si existe un campo "type" en el JSON
            if (jsonObject.has("type")) {
                String messageType = jsonObject.getString("type");

                // Comprueba el tipo de mensaje y procesa según corresponda
                if ("data".equals(messageType)) {
                    // El mensaje es de tipo "data", por lo tanto, extrae la matriz "data"
                    if (jsonObject.has("data")) {
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        processSensorData(dataArray);
                    } else {
                        // El campo "data" no está presente en el mensaje
                        // Manejar según sea necesario
                    }
                } else if ("addresses".equals(messageType)) {
                    // El mensaje es de tipo "addresses", procesa según corresponda
                    if (jsonObject.has("addresses")) {
                        JSONArray addressesArray = jsonObject.getJSONArray("addresses");
                        processAddresses(addressesArray);
                    } else {
                        // El campo "data" no está presente en el mensaje
                        // Manejar según sea necesario
                    }
                } else {
                    // Tipo de mensaje desconocido
                    // Manejar según sea necesario
                }
            } else {
                // El campo "type" no está presente en el mensaje
                // Manejar según sea necesario
            }
        } catch (JSONException e) {
            // Maneja cualquier error en el análisis JSON
            e.printStackTrace();
        }
    }

    // Método para procesar datos de sensores a partir de una matriz JSON
    private void processSensorData(JSONArray dataArray) {
        SincronizarSensores(dataArray);
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = dateFormat.format(currentDate);

        // Iterar sobre la lista de sensores y agregar la fecha a cada uno
        for (Sensor sensor : sensors) {
            sensor.fecha = currentDate; // Supongamos que tienes un método setDate en la clase Sensor
        }

        dateReading = currentDate;

        if(!isRecording){
            startDataRecording();
        }

        if (currentFragment instanceof FirstFragment) {
            // FirstFragment está actualmente visible
            sharedViewModel.setSensorsDataFirstFragment(sensors);
        } else if (currentFragment instanceof SecondFragment) {
            // SecondFragment está actualmente visible
            sharedViewModel.setSensorPosition(sensorPositionSel);
            sharedViewModel.setSensorsDataSecondFragment(sensors);
        }
    }

    private void SincronizarSensores(JSONArray jsonArray){
        if(sensors != null) {
            try {
                boolean bndObtener = false;
                // Itera a través de la lista de dispositivos en el JSON
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    // Extrae los valores
                    int address = jsonObject.getInt("Address");
                    float temperature = (float) jsonObject.getDouble("Temperature");
                    float humidity = (float) jsonObject.getDouble("Humidity");
                    String valveState = jsonObject.getString("ValveState");
                    String mode = jsonObject.getString("Mode");
                    int identifier = jsonObject.getInt("Identifier");
                    float temperatureSet = (float) jsonObject.getDouble("TemperatureSet");

                    if(address != 0) {
                        // Encuentra el sensor correspondiente en la lista existente por dirección
                        for (int pos = 0; pos < sensors.size(); pos++) {
                            if (sensors.get(pos).address == 0 && !bndObtener) {
                                //obtenerDirecciones();
                                bndObtener = true;
                            }
                            if (sensors.get(pos).address == address) {
                                sensors.get(pos).temperature = temperature;
                                sensors.get(pos).humidity = humidity;
                                sensors.get(pos).valveState = valveState;
                                sensors.get(pos).mode = mode;
                                //sensors.get(pos).address = address;
                                sensors.get(pos).identifier = identifier;
                                sensors.get(pos).temperatureSet = temperatureSet;
                            }
                        }
                    }

                }
            } catch (JSONException e) {
                // Maneja cualquier error en el análisis JSON
                e.printStackTrace();
            }
        }
    }

    // Método para procesar direcciones a partir de una matriz JSON
    private void processAddresses(JSONArray addressesArray) {
        try {
            // Crea un arreglo de enteros para almacenar las direcciones
            int[] addresses = new int[addressesArray.length()];

            // Itera a través de la matriz de direcciones y lee cada entero
            for (int i = 0; i < addressesArray.length(); i++) {
                int address = addressesArray.getInt(i);
                addresses[i] = address;
            }

            if(addresses.length == sensors.size()){
                for(int i = 0; i <= sensors.size() - 1; i++){
                    sensors.get(i).address = addresses[i];
                }
            }
        } catch (JSONException e) {
            // Maneja cualquier error en el análisis JSON
            e.printStackTrace();
        }
    }






    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //--------------------------Estado bluetooth-----------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------

    /*
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        // El Bluetooth está encendido
                        sharedViewModel.setIsBluetoothConnected(isBluetoothConnected());
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        // El Bluetooth está apagado
                        sharedViewModel.setIsBluetoothConnected(false);
                        break;
                    // Otros casos según tus necesidades
                }
            }
        }
    };*/

    private boolean isBluetoothConnected() {
        if (mBluetoothSocket != null) {
            return mBluetoothSocket.isConnected();
        }
        return false;
    }


    private static final long PING_TIMEOUT = 5000; // Tiempo de espera para la respuesta pong en milisegundos


    //************************************************************************************
    //************************************************************************************
    //*********************************Servidor*******************************************
    //************************************************************************************
    //************************************************************************************


    private SensorServer server;

    private void IniciarServidor() {
        server = new SensorServer(8080, this);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getJsonData() {
        Gson gson = new Gson();
        String json = gson.toJson(sensors);

        return json;
    }

    public int getAddressSensor(int index){
        if(sensors != null){
            if(index >= 0 && index < sensors.size()){
                return sensors.get(index).address;
            }
        }
        return -1;
    }


    //************************************************************************************
    //************************************************************************************
    //*****************************Grabar datos*******************************************
    //************************************************************************************
    //************************************************************************************

    private Timer dataRecordingTimer;
    private boolean isRecording = false;

    private static final int LIMITE_CANTIDAD_DATOS = 15000;

    private long recordingInterval = 20000; // Intervalo de registro de datos en milisegundos (por ejemplo, 30 segundos)

    private void saveDataToFile(List<Sensor> sensorList) {
        try {
            JSONArray jsonArray; // Declara el JSONArray aquí

            // Verifica si el archivo ya existe
            File file = new File(getFilesDir(), "data.json");
            if (file.exists()) {
                // Si el archivo existe, carga el JSONArray existente
                String existingData = readDataFromFile(file);
                if(existingData.isEmpty()){
                    jsonArray = new JSONArray();
                }else {
                    jsonArray = new JSONArray(existingData);
                }
            } else {
                // Si el archivo no existe, crea un nuevo JSONArray
                jsonArray = new JSONArray();
            }

            // Convierte la lista de sensores a JSON y agrega al JSONArray existente
            if(dateRecord.compareTo(dateReading) != 0){
                //No guarda el mismo dato
                dateRecord = dateReading;

                for (Sensor sensor : sensorList) {
                    if (sensor.address != 0) {
                        JSONObject sensorJson = new JSONObject();
                        sensorJson.put("address", sensor.address);
                        sensorJson.put("temperature", sensor.temperature);
                        sensorJson.put("humidity", sensor.humidity);
                        sensorJson.put("valveSate", sensor.valveState);
                        sensorJson.put("mode", sensor.mode);
                        sensorJson.put("identifier", sensor.identifier);
                        sensorJson.put("temperatureSet", sensor.temperatureSet);
                        sensorJson.put("fecha", sensor.fecha);
                        jsonArray.put(sensorJson);
                    }
                }

                // Limita la cantidad de datos en el JSONArray
                int maxDataCount = LIMITE_CANTIDAD_DATOS; // Establece el límite máximo de datos
                if (jsonArray.length() > maxDataCount) {
                    int elementsToRemove = jsonArray.length() - maxDataCount;
                    for (int i = 0; i < elementsToRemove; i++) {
                        jsonArray.remove(0); // Elimina los elementos más antiguos
                    }
                }

                // Escribe el JSONArray completo en el archivo
                FileOutputStream fos = openFileOutput("data.json", Context.MODE_PRIVATE);
                fos.write(jsonArray.toString().getBytes());
                fos.close();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }



    public String readDataFromFile() {
        // Ruta del archivo en el almacenamiento interno
        String filePath = getFilesDir() + "/data.json";

        try {
            FileInputStream fis = new FileInputStream(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            fis.close();

            // El contenido del archivo está en stringBuilder.toString()
            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String readDataFromFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            fis.close();

            // El contenido del archivo está en stringBuilder.toString()
            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            // Maneja la excepción si el archivo no existe
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // Constructor o método de inicio
    private void startDataRecording() {
        if (!isRecording) {
            isRecording = true;
            dataRecordingTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // Código para guardar los datos a intervalos regulares
                    saveDataToFile(sensors);
                }
            }, recordingInterval, recordingInterval);
        }
    }

    private void resetDataFile() {
        try {
            // Elimina el archivo existente si existe
            File file = new File(getFilesDir(), "data.json");
            if (file.exists()) {
                file.delete();
            }

            // Crea un nuevo archivo vacío
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Método para guardar el último dato leído
    private void saveLastReadData() {
        // Agrega el último dato leído a tu lista de datos o almacénalo en un archivo/base de datos
        if(sensors != null) {
            saveDataToFile(sensors);
        }
    }

    // Método para detener el temporizador cuando sea necesario
    public void stopRecording() {
        dataRecordingTimer.cancel();
    }

}