package com.example.arduinoconnector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.arduinoconnector.databinding.FragmentSecondBinding;
import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.components.Section;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;

public class SecondFragment extends Fragment {

    private static final long REPEAT_DELAY1 = 500;
    private static final long REPEAT_DELAY2 = 1000;
    private static final int COUNTER_WAIT = 5;

    private FragmentSecondBinding binding;
    private SharedViewModel sharedViewModel;
    private Button btnBack;
    private SpeedView tempControl, humControl;
    private TextView tituloD, temperatureSet, actualTemperarure;
    private TextView identificadorD, direccionD;
    private Button btnRele, btnIncrementar, btnDecrementar, btnCambioId, btnCambioDir;
    private ToggleButton btnMode;
    private ConstraintLayout controlContainer;
    private Sensor sensor;
    private int position = -1;

    private Handler repeatUpdateHandler = new Handler();
    private boolean autoIncrement = false;
    private boolean autoDecrement = false;
    private long pressDuration = 0;
    private int cntWait = 0;
    private long repeatDelay;

    public void SetPosition(int pos){
        position = pos;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view;
        view = inflater.inflate(R.layout.fragment_second, container, false);
        this.iniciarControles(view);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).navigateToFirstFragment();
            }
        });

        btnRele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    btnRele.setBackgroundResource(R.drawable.color_amarillo_wait);
                    btnRele.setTag(R.drawable.color_amarillo_wait);
                    sensor.cntWait = COUNTER_WAIT;
                    JSONObject command = new JSONObject();
                    command.put("cmd", "setValve");
                    command.put("adr", sensor.address);
                    if ("on".equalsIgnoreCase(sensor.valveState)){
                        command.put("state", "off");
                        //sensor.valveState = "OFF";
                    }else{
                        command.put("state", "on");
                        //sensor.valveState = "ON";
                    }
                    String commandString = command.toString() + "\n"; // Añadir un carácter de nueva línea para indicar el fin del comando
                    ((MainActivity) getActivity()).sendDataBluetooth(commandString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        btnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    controlContainer.setBackgroundResource(R.drawable.color_wait_mode);
                    controlContainer.setTag(R.drawable.color_wait_mode);
                    sensor.cntWait = COUNTER_WAIT;

                    JSONObject command = new JSONObject();
                    command.put("cmd", "setMode");
                    command.put("adr", sensor.address);
                    if("idle".equalsIgnoreCase(sensor.mode)){
                        command.put("mode", "control");
                    }else{
                        command.put("mode", "idle");
                    }

                    String commandString = command.toString() + "\n"; // Añadir un carácter de nueva línea para indicar el fin del comando
                    ((MainActivity) getActivity()).sendDataBluetooth(commandString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Runnable r = new Runnable() {
            @Override
            public void run() {
                float currentValue = sensor.temperatureSet;
                if (autoIncrement) {
                    pressDuration += repeatDelay;
                    if (pressDuration < 5000) {
                        currentValue += 1;
                        repeatDelay = REPEAT_DELAY1;
                    } else {
                        currentValue += 10;
                        repeatDelay = REPEAT_DELAY2;
                    }
                    if (currentValue < 10){
                        currentValue = 10;
                    }else if (currentValue > 50){
                        currentValue = 50;
                    }
                    sensor.temperatureSet = currentValue;
                    temperatureSet.setText(currentValue + " ºC");
                    repeatUpdateHandler.postDelayed(this, repeatDelay);
                } else if (autoDecrement) {
                    pressDuration += repeatDelay;
                    if (pressDuration < 5000) { // Por ejemplo, menos de 1 segundo
                        currentValue -= 1;
                        repeatDelay = REPEAT_DELAY1;
                    } else {
                        currentValue -= 10;
                        repeatDelay = REPEAT_DELAY2;
                    }
                    if (currentValue < 10){
                        currentValue = 10;
                    }else if (currentValue > 50){
                        currentValue = 50;
                    }
                    sensor.temperatureSet = currentValue;
                    temperatureSet.setText(currentValue + " ºC");
                    repeatUpdateHandler.postDelayed(this, repeatDelay);
                }
            }
        };

        btnIncrementar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                try {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressDuration = 0; // Reinicia el tiempo
                            autoIncrement = true;
                            repeatDelay = REPEAT_DELAY1;
                            repeatUpdateHandler.postDelayed(r, 1);
                            temperatureSet.setTextColor(ContextCompat.getColor(getActivity(), R.color.wait_temperature_set));
                            temperatureSet.setTag(R.color.wait_temperature_set);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            autoIncrement = false;
                            repeatUpdateHandler.removeCallbacks(r);

                            JSONObject command = new JSONObject();
                            command.put("cmd", "setTemp");
                            command.put("adr", sensor.address);
                            command.put("temp", sensor.temperatureSet);

                            String commandString = command.toString() + "\n";
                            ((MainActivity) getActivity()).sendDataBluetooth(commandString);
                            sensor.cntWait = COUNTER_WAIT;
                            break;
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        });

        btnDecrementar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                try {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressDuration = 0; // Reinicia el tiempo
                            autoDecrement = true;
                            repeatUpdateHandler.postDelayed(r, 1);
                            temperatureSet.setTextColor(ContextCompat.getColor(getActivity(), R.color.wait_temperature_set));
                            temperatureSet.setTag(R.color.wait_temperature_set);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            autoDecrement = false;
                            repeatUpdateHandler.removeCallbacks(r);

                            JSONObject command = new JSONObject();
                            command.put("cmd", "setTemp");
                            command.put("adr", sensor.address);
                            command.put("temp", sensor.temperatureSet);

                            String commandString = command.toString() + "\n";
                            ((MainActivity) getActivity()).sendDataBluetooth(commandString);
                            sensor.cntWait = COUNTER_WAIT;
                            break;
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        });

        btnCambioId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.input_dialog, null);
                EditText editText = dialogView.findViewById(R.id.text_dialog);
                editText.setText(Integer.toString(sensor.identifier));
                editText.selectAll();

                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle("Cambio de indentificador")
                        .setView(dialogView)
                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Acción para el botón Aceptar
                                int inputValue = Integer.parseInt(editText.getText().toString());
                                try {
                                    //{"cmd":"setId","adr":3,"Ident":18}
                                    JSONObject command = new JSONObject();
                                    command.put("cmd", "setId");
                                    command.put("adr", sensor.address);
                                    command.put("Ident", inputValue);

                                    String commandString = command.toString() + "\n";
                                    ((MainActivity) getActivity()).sendDataBluetooth(commandString);
                                }catch (Exception ex){
                                    throw new RuntimeException(ex);
                                }

                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .create();

                alertDialog.show();

            }
        });

        btnCambioDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.input_dialog2, null);
                EditText editText = dialogView.findViewById(R.id.text_dialog);
                editText.setText(Integer.toString(sensor.address));
                editText.selectAll();

                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle("Cambio de dirección")
                        .setView(dialogView)
                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Acción para el botón Aceptar
                                int inputValue = Integer.parseInt(editText.getText().toString());
                                try {
                                    //{"cmd":"setId","adr":3,"Ident":18}
                                    JSONObject command = new JSONObject();
                                    command.put("cmd", "setDir");
                                    command.put("sensorNum", position + 1);
                                    command.put("nuevaDir", inputValue);

                                    String commandString = command.toString() + "\n";
                                    ((MainActivity) getActivity()).sendDataBluetooth(commandString);

                                    sensor.address = inputValue;
                                    direccionD.setText(Integer.toString(inputValue));
                                }catch (Exception ex){
                                    throw new RuntimeException(ex);
                                }

                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .create();

                alertDialog.show();

            }
        });

        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void iniciarControles(View view){
        btnBack = view.findViewById(R.id.ss_btn_back);
        tituloD = view.findViewById(R.id.ss_titulo);
        identificadorD = view.findViewById(R.id.ss_identificador);
        direccionD = view.findViewById(R.id.ss_direccion);
        tempControl = view.findViewById(R.id.ss_temperature_gauge);
        humControl = view.findViewById(R.id.ss_humidity_gauge);
        btnRele = view.findViewById(R.id.ss_btn_rele);
        btnMode = view.findViewById(R.id.ss_mode_toggle);
        btnIncrementar = view.findViewById(R.id.ss_btn_increment);
        btnDecrementar = view.findViewById(R.id.ss_btn_decrement);
        btnCambioId = view.findViewById(R.id.ss_btn_cambio_id);
        btnCambioDir = view.findViewById(R.id.ss_btn_cambio_direccion);
        controlContainer = view.findViewById(R.id.ss_contenedor);
        temperatureSet = view.findViewById(R.id.ss_temp_control_value);
        actualTemperarure = view.findViewById(R.id.ss_actual_temperature);
        this.setupGauge(tempControl, humControl);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getSensorsDataSecondFragment().observe(getViewLifecycleOwner(), sensors -> {
            this.SetSensors(sensors);
        });

        sharedViewModel.getSensorPosition().observe(getViewLifecycleOwner(), position -> {
            this.position = position;
        });
    }

    private void setupGauge(SpeedView temperatureGauge, SpeedView humidityGauge) {

        temperatureGauge.clearSections();
        temperatureGauge.addSections(new Section(0f, .5f, Color.WHITE)
                , new Section(.5f, .75f, Color.GREEN)
                , new Section(.75f, .875f, Color.YELLOW)
                , new Section(.875f, 1f, Color.RED));
        temperatureGauge.setSpeedometerWidth(15);
        temperatureGauge.speedTo(10);

        humidityGauge.clearSections();
        humidityGauge.addSections(new Section(0f, .1f, Color.LTGRAY)
                , new Section(.1f, .4f, Color.YELLOW)
                , new Section(.4f, .75f, Color.BLUE)
                , new Section(.75f, .9f, Color.RED));
        humidityGauge.setSpeedometerWidth(15);
        humidityGauge.speedTo(0);

    }

    public void SetSensors(List<Sensor> senss){
        if(senss.size() > 0 && position >= 0 && position < senss.size() ){
            if (sensor == null) {
                sensor = new Sensor(senss.get(position).address, senss.get(position).temperature, senss.get(position).humidity,
                        senss.get(position).valveState, senss.get(position).mode, senss.get(position).identifier,
                        senss.get(position).temperatureSet);
            }else {
                sensor.address = senss.get(position).address;
                sensor.temperature = senss.get(position).temperature;
                sensor.humidity = senss.get(position).humidity;
                sensor.valveState = senss.get(position).valveState;
                sensor.mode = senss.get(position).mode;
                sensor.identifier = senss.get(position).identifier;
                if (!autoIncrement && !autoDecrement)
                    sensor.temperatureSet = senss.get(position).temperatureSet;
            }
            sensor.titulo = senss.get(position).titulo;
            setControls();
        }
    }

    private void setControls(){
        if (sensor != null){
            if(tempControl != null) {
                tempControl.speedTo(sensor.temperature);
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                String formattedTemperature = decimalFormat.format(sensor.temperature);

// Mostrar el valor formateado en el TextView.
                actualTemperarure.setText(formattedTemperature + "ºC");
            }
            if(humControl != null)
                humControl.speedTo(sensor.humidity);
            if(tituloD != null)
                tituloD.setText(sensor.titulo);
            if(identificadorD != null)
                identificadorD.setText(Integer.toString(sensor.identifier));
            if(direccionD !=null)
                direccionD.setText(Integer.toString(sensor.address));

            if (sensor.cntWait > 0){
                sensor.cntWait--;

                if(controlContainer.getTag() != null && (int) controlContainer.getTag() == R.drawable.color_wait_mode &&
                        ((sensor.mode.equalsIgnoreCase("CONTROL") && btnMode.isChecked()) ||
                                (sensor.mode.equalsIgnoreCase("IDLE") && !btnMode.isChecked())))
                {
                    sensor.cntWait = 0;
                }
                if(btnRele.getTag() != null && (int) btnRele.getTag() == R.drawable.color_amarillo_wait && !btnRele.getText().equals(sensor.valveState)){
                    sensor.cntWait = 0;
                }
                if(temperatureSet.getTag() != null && (int) temperatureSet.getTag() == R.color.wait_temperature_set &&
                        Float.parseFloat(temperatureSet.getText().toString().replace("ºC", "")) == sensor.temperatureSet){
                    sensor.cntWait = 0;
                }


            }

            if(sensor.cntWait <= 0) {
                if (btnRele != null) {

                    if ("on".equalsIgnoreCase(sensor.valveState)) {
                        btnRele.setBackgroundResource(R.drawable.color_verde_on);
                        btnRele.setTag(R.drawable.color_verde_on);
                        btnRele.setText("On");
                    } else {
                        btnRele.setBackgroundResource(R.drawable.color_rojo_off);
                        btnRele.setTag(R.drawable.color_rojo_off);
                        btnRele.setText("Off");
                    }
                }
                if (btnMode != null) {
                    if ("idle".equalsIgnoreCase(sensor.mode)) {
                        controlContainer.setBackgroundResource(R.drawable.color_idle_mode);
                        controlContainer.setTag(R.drawable.color_idle_mode);
                        btnMode.setChecked(false);
                    } else {
                        controlContainer.setBackgroundResource(R.drawable.color_control_mode);
                        controlContainer.setTag(R.drawable.color_control_mode);
                        btnMode.setChecked(true);
                    }
                }

                if (temperatureSet != null) {
                    if (!autoDecrement && !autoIncrement) {
                        temperatureSet.setText(sensor.temperatureSet + " ºC");
                        temperatureSet.setTextColor(ContextCompat.getColor(getActivity(), R.color.normal_temperature_set));
                    }
                }
            }

        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}