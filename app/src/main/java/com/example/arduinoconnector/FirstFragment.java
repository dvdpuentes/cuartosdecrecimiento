package com.example.arduinoconnector;

import static java.security.AccessController.getContext;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.cardiomood.android.controls.gauge.SpeedometerGauge;
import com.github.anastr.speedviewlib.PointerSpeedometer;
import com.github.anastr.speedviewlib.SpeedView;
/*import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;*/

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirstFragment extends Fragment implements SensorAdapterRecycleView.OnSensorAdapterRVListener,
        BatteryReceiver.OnBatteryUpdateListener,
        WifiReceiver.OnWifiSignalUpdateListener{

    //private Button btnIniciarRX;
    private ListView listaSensores;
    private TextView horaSistema, batteryPercentageTV;
    private ImageView batteryIconIV;
    private Button connectionStatus;
    private RecyclerView rvSensores;
    private SensorAdapterRecycleView adapter;
    private List<Sensor> sensors = new ArrayList<>();
    private SharedViewModel sharedViewModel;
    private final Context context;
    private OnFirstFragmentEvent listener;
    private BatteryReceiver batteryReceiver;

    private WifiReceiver wifiReceiver;
    private ImageView wifiIconIV;


    public enum ConnectionStatusEnum{
        ONLINE,
        OFFLINE,
        WAITING
    }

    public FirstFragment() {
        context = null;
    }

    public FirstFragment(Context context, List<Sensor> sensors1) {
        this.context = context;
        this.listener = (OnFirstFragmentEvent) context;
        this.sensors = sensors1;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        this.inicializarControles(view);
        this.actualizarHora();

        return view;
    }

    private void inicializarControles(View view){

        rvSensores = view.findViewById(R.id.recyclerview_sensores);
        adapter = new SensorAdapterRecycleView(getActivity(), sensors);
        adapter.setOnSensorAdapterRVListener(this);
        rvSensores.setAdapter(adapter);
        horaSistema = view.findViewById(R.id.time_system);
        connectionStatus = view.findViewById(R.id.btn_connection_status);
        updateConnectionStatus(ConnectionStatusEnum.ONLINE);
        batteryIconIV = view.findViewById(R.id.battery_icon);
        batteryPercentageTV = view.findViewById(R.id.battery_percentage);
        wifiIconIV = view.findViewById(R.id.wifi_icon);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Si está en modo portrait, usa un LinearLayoutManager vertical
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
            rvSensores.setLayoutManager(linearLayoutManager);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Si está en modo landscape, usa un LinearLayoutManager horizontal
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
            rvSensores.setLayoutManager(linearLayoutManager);
        }


        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getSensorsDataFirstFragment().observe(getViewLifecycleOwner(), sensors -> {
            this.SetSensors(sensors);
        });

        sharedViewModel.getIsBluetoothConnected().observe(getViewLifecycleOwner(), new Observer<ConnectionStatusEnum>() {
            @Override
            public void onChanged(ConnectionStatusEnum isConnected) {
                updateConnectionStatus(isConnected);
                /*if (isConnected) {
                    // La conexión Bluetooth está activa
                } else {
                    // La conexión Bluetooth no está activa
                }*/
            }
        });

        connectionStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(listener != null){
                    listener.onClickConectarDispositivo();
                }
            }
        });

        connectionStatus.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(listener != null){
                    listener.onCLickReconectarDispositivo();
                }
                return false;
            }
        });

        batteryIconIV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showNetworkInfoDialog();
                return false;
            }
        });

        wifiIconIV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showNetworkInfoDialog();
                return false;
            }
        });
    }

    private void setGaugeValue(SpeedView speedometer, Float value) {
        speedometer.speedTo(value); // Establecer la velocidad actual
    }
    private void setGaugeValue(PointerSpeedometer speedometer, Float value) {
        speedometer.speedTo(value); // Establecer la velocidad actual
    }

    private void updateConnectionStatus(ConnectionStatusEnum isConnected) {
        if (isConnected == ConnectionStatusEnum.ONLINE) {
            connectionStatus.setText("Conectado");
            connectionStatus.setCompoundDrawablesWithIntrinsicBounds(0,0,0,R.drawable.ic_online);
            connectionStatus.setBackgroundResource(R.drawable.color_online);
        } else if(isConnected == ConnectionStatusEnum.OFFLINE)  {
            connectionStatus.setText("Desconectado");
            connectionStatus.setCompoundDrawablesWithIntrinsicBounds(0,0,0,R.drawable.ic_offline);
            connectionStatus.setBackgroundResource(R.drawable.color_offline);
        } else{
            connectionStatus.setText("Conectando");
            connectionStatus.setCompoundDrawablesWithIntrinsicBounds(0,0,0,R.drawable.ic_offline);
            connectionStatus.setBackgroundResource(R.drawable.color_waitline);
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    public void SetSensors(List<Sensor> senss){
        if(senss.size() > 0){
            for (int i = 0; i < sensors.size(); i++){
                if(adapter != null && sensors.get(i).address == 0){
                    Sensor nuevo = new Sensor(0, 0,0, "Off", "IDLE", sensors.get(i).identifier, 0);
                    SensorAdapterRecycleView.ViewHolder holder = (SensorAdapterRecycleView.ViewHolder) rvSensores.findViewHolderForAdapterPosition(i);
                    adapter.updateData(i, nuevo, holder);
                }
            }
            for (int i = 0; i < senss.size(); i++) {
                if(senss.get(i).address != 0) {
                    for (int pos = 0; pos < sensors.size(); pos++) {
                        if (sensors.get(pos).address == senss.get(i).address) {
                            if (adapter != null) {
                                Sensor nuevo = new Sensor(senss.get(i).address, senss.get(i).temperature,
                                        senss.get(i).humidity, senss.get(i).valveState, senss.get(i).mode,
                                        senss.get(i).identifier, senss.get(i).temperatureSet);

                                SensorAdapterRecycleView.ViewHolder holder = (SensorAdapterRecycleView.ViewHolder) rvSensores.findViewHolderForAdapterPosition(pos);
                                adapter.updateData(pos, nuevo, holder);

                                break;
                            }

                        }
                    }
                }
            }
        }
    }

    private void showNetworkInfoDialog() {
        // Inflar el diseño personalizado del cuadro de diálogo
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_network_info, null);

        // Obtener la dirección MAC y la dirección IP utilizando NetworkInfoHelper
        String macAddress = NetworkInfoHelper.getMacAddress(getActivity());
        String ipAddress = NetworkInfoHelper.getIPAddress(getActivity());

        // Actualizar los TextView en el cuadro de diálogo con la información
        TextView macAddressTextView = dialogView.findViewById(R.id.macAddressTextView);
        TextView ipAddressTextView = dialogView.findViewById(R.id.ipAddressTextView);

        macAddressTextView.setText("MAC Address: " + (macAddress != null ? macAddress : "Not available"));
        ipAddressTextView.setText("IP Address: " + (ipAddress != null ? ipAddress : "Not available"));

        // Crear el cuadro de diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView)
                .setTitle("Network Information")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Cierra el cuadro de diálogo cuando se hace clic en "OK"
                        dialog.dismiss();
                    }
                });

        // Mostrar el cuadro de diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
    {"cmd":"setTemp","adr":3,"Temp":35.0}
    {"cmd":"setId","adr":3,"Ident":18}
    {"cmd":"setValve","Adr":3,"State":"ON"}
    {"cmd":"setmode","Adr":3,"mode":"control"} / {"cmd":"setmode","Adr":3,"mode":"idle"}

    Comando para obtener las direcciones de cada uno de los dispositivos
    {"cmd":"getAdd"}
    */



    @Override
    public void onReleClick(int position) {
        try {
            Sensor sensor = sensors.get(position);

            JSONObject command = new JSONObject();
            command.put("cmd", "setValve");
            command.put("adr", sensor.address);
            if ("on".equalsIgnoreCase(sensor.valveState)){
                command.put("state", "on");
                //sensor.valveState = "OFF";
            }else{
                command.put("state", "off");
                //sensor.valveState = "ON";
            }
            String commandString = command.toString() + "\n"; // Añadir un carácter de nueva línea para indicar el fin del comando
            ((MainActivity) getActivity()).sendDataBluetooth(commandString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTemperatureControlChange(int position) {
        try {
            if (position >= 0 && position < sensors.size()) {
                Sensor sensor = sensors.get(position);

                JSONObject command = new JSONObject();
                command.put("cmd", "setTemp");
                command.put("adr", sensor.address);
                command.put("temp", sensor.temperatureSet);

                String commandString = command.toString() + "\n"; // Añadir un carácter de nueva línea para indicar el fin del comando
                ((MainActivity) getActivity()).sendDataBluetooth(commandString);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onModoChange(int position) {
        try {
            Sensor sensor = sensors.get(position);

            JSONObject command = new JSONObject();
            command.put("cmd", "setMode");
            command.put("adr", sensor.address);
            if("idle".equalsIgnoreCase(sensor.mode)){
                command.put("mode", "idle");
            }else{
                command.put("mode", "control");
            }

            String commandString = command.toString() + "\n"; // Añadir un carácter de nueva línea para indicar el fin del comando
            ((MainActivity) getActivity()).sendDataBluetooth(commandString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClickParametros(int position) {
        /*NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment);*/
        if(listener != null){
            listener.onClickParameter(position);
        }
    }

    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //--------------------------Ciclo de vida -------------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        registerBatteryReceiver();
        registerMinuteChangeReceiver();
        registerWifiReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterBatteryReceiver();
        unregisterMinuteChangeReceiver();
        unregisterWifiReceiver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //--------------------------Hora del sistema-----------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------

    private BroadcastReceiver minuteChangeReceiver;

    private void registerMinuteChangeReceiver() {
        // Define el intent filter para detectar cambios en el minuto
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);

        // Crea e instancia el BroadcastReceiver
        minuteChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                    // Cambio de minuto detectado, actualiza el TextView de la hora
                    actualizarHora();
                }
            }
        };

        // Registra el BroadcastReceiver
        requireContext().registerReceiver(minuteChangeReceiver, filter);
    }

    private void unregisterMinuteChangeReceiver() {
        // Anula el registro del BroadcastReceiver
        if (minuteChangeReceiver != null) {
            requireContext().unregisterReceiver(minuteChangeReceiver);
            minuteChangeReceiver = null;
        }
    }

    private void actualizarHora() {
        // Implementa la lógica para actualizar la hora en el TextView aquí
        // Obtener la hora actual con minutos y AM/PM
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        horaSistema.setText(currentTime);
        //((MainActivity) getActivity()).obtenerDirecciones();

    }

    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //---------------------------------Batería-------------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------

    @Override
    public void onBatteryUpdate(int percentage, boolean isCharging) {
        updateBatteryUI(percentage, isCharging);
    }

    private void updateBatteryUI(int percentage, boolean isCharging) {
        // Actualiza el TextView con el porcentaje de carga
        batteryPercentageTV.setText(Integer.toString(percentage) + "%");

        // Actualiza el ImageView con el ícono de la batería según el estado de carga
        if (isCharging) {
            // El dispositivo está cargando
            if(percentage == 100) {
                batteryIconIV.setImageResource(R.drawable.baseline_battery_charging_full_24);
            }else{
                batteryIconIV.setImageResource(R.drawable.baseline_bolt_24);
            }
        } else if (percentage >= 0 && percentage <= 20) {
            batteryIconIV.setImageResource(R.drawable.baseline_battery_1_bar_24);
        } else if (percentage > 20 && percentage <= 40) {
            batteryIconIV.setImageResource(R.drawable.baseline_battery_3_bar_24);
        } else if (percentage > 40 && percentage <= 60) {
            batteryIconIV.setImageResource(R.drawable.baseline_battery_5_bar_24);
        } else if (percentage > 60 && percentage <= 80) {
            batteryIconIV.setImageResource(R.drawable.baseline_battery_6_bar_24);
        } else {
            batteryIconIV.setImageResource(R.drawable.baseline_battery_full_24);
        }
    }

    private void registerBatteryReceiver(){
        batteryReceiver = new BatteryReceiver();
        batteryReceiver.setOnBatteryUpdateListener(this);

        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        requireActivity().registerReceiver(batteryReceiver, batteryFilter);
    }

    private void unregisterBatteryReceiver(){
        // Anula el registro del BatteryReceiver
        requireActivity().unregisterReceiver(batteryReceiver);
    }

    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //---------------------------------Wifi----------------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------

    @Override
    public void onWifiSignalUpdate(int wifiLevel) {
        updateWifiUI(wifiLevel);
    }

    private void updateWifiUI(int signalStrength) {
        // Verifica si el Wi-Fi está desconectado (por ejemplo, intensidad de señal <= 0)
        if (signalStrength <= 0) {
            wifiIconIV.setImageResource(R.drawable.baseline_wifi_off_24); // Wi-Fi desconectado
            //wifiStrengthTV.setText("Wi-Fi Off"); // Texto indicando que el Wi-Fi está apagado
        } else if (signalStrength < 50) {
            wifiIconIV.setImageResource(R.drawable.baseline_network_wifi_1_bar_24);
            //wifiStrengthTV.setText("Weak");
        } else if (signalStrength < 75) {
            wifiIconIV.setImageResource(R.drawable.baseline_network_wifi_2_bar_24);
            //wifiStrengthTV.setText("Fair");
        } else if (signalStrength < 90) {
            wifiIconIV.setImageResource(R.drawable.baseline_network_wifi_3_bar_24);
            //wifiStrengthTV.setText("Good");
        } else {
            wifiIconIV.setImageResource(R.drawable.baseline_signal_wifi_4_bar_24);
            //wifiStrengthTV.setText("Excellent");
        }
    }

    private void registerWifiReceiver(){
        wifiReceiver = new WifiReceiver();
        wifiReceiver.setOnWifiSignalUpdateListener(this);

        IntentFilter wifiFilter = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
        requireActivity().registerReceiver(wifiReceiver, wifiFilter);
    }

    private void unregisterWifiReceiver(){
        requireActivity().unregisterReceiver(wifiReceiver);
    }

    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //---------------------------Interface-----------------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------

    public interface OnFirstFragmentEvent {
        void onClickParameter(int position);
        void onClickConectarDispositivo();
        void onCLickReconectarDispositivo();
    }
}