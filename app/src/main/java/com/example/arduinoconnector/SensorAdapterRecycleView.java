package com.example.arduinoconnector;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.components.Section;

import java.text.DecimalFormat;
import java.util.List;

public class SensorAdapterRecycleView extends RecyclerView.Adapter<SensorAdapterRecycleView.ViewHolder> {

    private static final long REPEAT_DELAY = 500;
    private static final int COUNTER_WAIT = 5;
    private final Context context;
    private final List<Sensor> sensors;
    private OnSensorAdapterRVListener listener;

    private Handler repeatUpdateHandler = new Handler();
    private boolean autoIncrement = false;
    private boolean autoDecrement = false;
    private long pressDuration = 0;
    private int cntWait = 0;

    public SensorAdapterRecycleView(Context context, List<Sensor> sensors) {
        this.context = context;
        this.sensors = sensors;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.fragment_sensor, parent, false);
        return new ViewHolder(itemView);
    }

    public void setOnSensorAdapterRVListener(SensorAdapterRecycleView.OnSensorAdapterRVListener listener) {
        this.listener = listener;
    }

    private boolean isUserInteracting = false; // Paso 1: Introduce la variable

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sensor sensor = sensors.get(position);

        this.setupGauge(holder.temperatureGauge, holder.humidityGauge);
        holder.title.setText(sensor.titulo);

        if ("on".equalsIgnoreCase(sensor.valveState)) {
            holder.btnRele.setBackgroundResource(R.drawable.color_verde_on);
            holder.btnRele.setText("On");
        } else {
            holder.btnRele.setBackgroundResource(R.drawable.color_rojo_off);
            holder.btnRele.setText("Off");
        }
        holder.btnRele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cambia el estado del botón a amarillo (presionado)
                holder.btnRele.setBackgroundResource(R.drawable.color_amarillo_wait);
                sensors.get(position).cntWait = COUNTER_WAIT;
                if ("on".equalsIgnoreCase(sensors.get(position).valveState)){
                    sensors.get(position).valveState = "OFF";
                }else {
                    sensors.get(position).valveState = "ON";
                }
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    listener.onReleClick(pos);
                }
            }
        });

        Runnable r = new Runnable() {
            @Override
            public void run() {
                float currentValue = sensors.get(position).temperatureSet;
                if (autoIncrement) {
                    pressDuration += REPEAT_DELAY;
                    if (pressDuration < 4000) {
                        currentValue += 1;
                    } else {
                        currentValue += 10;
                    }
                    if (currentValue < 10){
                        currentValue = 10;
                    }else if (currentValue > 50){
                        currentValue = 50;
                    }
                    sensors.get(position).temperatureSet = currentValue;
                    holder.tempValue.setText(currentValue + " ºC");
                    repeatUpdateHandler.postDelayed(this, REPEAT_DELAY);
                } else if (autoDecrement) {
                    pressDuration += REPEAT_DELAY;
                    if (pressDuration < 4000) { // Por ejemplo, menos de 1 segundo
                        currentValue -= 1;
                    } else {
                        currentValue -= 10;
                    }
                    if (currentValue < 10){
                        currentValue = 10;
                    }else if (currentValue > 50){
                        currentValue = 50;
                    }
                    sensors.get(position).temperatureSet = currentValue;
                    holder.tempValue.setText(currentValue + " ºC");
                    repeatUpdateHandler.postDelayed(this, REPEAT_DELAY);
                }
            }
        };

        holder.btnIncrement.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressDuration = 0; // Reinicia el tiempo
                        autoIncrement = true;
                        repeatUpdateHandler.postDelayed(r, 1);
                        //holder.tempValue.setTextColor(ContextCompat.getColor(context, R.color.wait_temperature_set));
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        autoIncrement = false;
                        repeatUpdateHandler.removeCallbacks(r);
                        if (listener != null) {
                            int pos = holder.getAdapterPosition();
                            listener.onTemperatureControlChange(pos);
                        }
                        sensors.get(position).cntWait = COUNTER_WAIT;
                        break;
                }
                return false;
            }
        });

        holder.btnDecrement.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressDuration = 0; // Reinicia el tiempo
                        autoDecrement = true;
                        repeatUpdateHandler.postDelayed(r, 1);
                        //holder.tempValue.setTextColor(ContextCompat.getColor(context, R.color.wait_temperature_set));
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        autoDecrement = false;
                        repeatUpdateHandler.removeCallbacks(r);
                        if (listener != null) {
                            int pos = holder.getAdapterPosition();
                            listener.onTemperatureControlChange(pos);
                        }
                        sensors.get(position).cntWait = COUNTER_WAIT;
                        break;
                }
                return false;
            }
        });

        holder.btnConfigurar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    listener.onClickParametros(pos);
                }
            }
        });

        holder.tempValue.setText(sensor.temperatureSet + " ºC");

        if("idle".equalsIgnoreCase(sensor.mode)) {
            holder.controlContainer.setBackgroundResource(R.drawable.color_idle_mode);
            holder.modeStatus.setChecked(false);
        }else{
            holder.controlContainer.setBackgroundResource(R.drawable.color_control_mode);
            holder.modeStatus.setChecked(true);
        }

        holder.modeStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = holder.getAdapterPosition();
                holder.controlContainer.setBackgroundResource(R.drawable.color_wait_mode);
                sensors.get(position).cntWait = COUNTER_WAIT;
                if(((AppCompatToggleButton) view).isChecked()){
                    sensor.mode = "control";
                }else{
                    sensor.mode = "idle";
                }
                if (listener != null) {
                    listener.onModoChange(pos);
                }
            }
        });


        // Utiliza las vistas en el holder para configurar los datos
        //holder.address.setText(String.valueOf(sensor.address));
        //holder.idDispositivo.setText(String.valueOf(sensor.identifier));
        holder.temperatureGauge.speedTo(sensor.temperature);
        holder.humidityGauge.speedTo(sensor.humidity);
        //holder.tempControl.setText(String.valueOf(sensor.temperatureSet));
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    private void setupGauge(SpeedView temperatureGauge, SpeedView humidityGauge) {

        temperatureGauge.clearSections();
        temperatureGauge.addSections(new Section(0f, .5f, Color.WHITE)
                , new Section(.5f, .75f, Color.GREEN)
                , new Section(.75f, .875f, Color.YELLOW)
                , new Section(.875f, 1f, Color.RED));
        temperatureGauge.setSpeedometerWidth(3);
        temperatureGauge.speedTo(0);

        humidityGauge.clearSections();
        humidityGauge.addSections(new Section(0f, .1f, Color.LTGRAY)
                , new Section(.1f, .4f, Color.YELLOW)
                , new Section(.4f, .75f, Color.BLUE)
                , new Section(.75f, .9f, Color.RED));
        humidityGauge.setSpeedometerWidth(3);
        humidityGauge.speedTo(0);

    }

    public void updateData(int position, Sensor nuevo, ViewHolder holder){
        Sensor sensor = sensors.get(position);
        if (sensor.address == nuevo.address && holder != null){
            sensor.temperature = nuevo.temperature;
            sensor.humidity = nuevo.humidity;
                    //sensors.set(position, nuevo);

            holder.temperatureGauge.speedTo(nuevo.temperature);
            holder.humidityGauge.speedTo(nuevo.humidity);


            if (sensor.cntWait > 0){
                sensor.cntWait--;
                if(sensor.mode.equalsIgnoreCase("IDLE")){
                    if (sensor.valveState.equalsIgnoreCase(nuevo.valveState) && sensor.mode.equalsIgnoreCase(nuevo.mode) && sensor.temperatureSet == nuevo.temperatureSet){
                        sensor.cntWait = 0;
                    }
                }else{
                    if (sensor.mode.equalsIgnoreCase(nuevo.mode) && sensor.temperatureSet == nuevo.temperatureSet){
                        sensor.cntWait = 0;
                    }
                }

            }

            if(holder.tvTempValueTR != null){
                DecimalFormat f = new DecimalFormat("00.0");
                holder.tvTempValueTR.setText(f.format(sensor.temperature) + " ºC");
            }

            if (sensor.cntWait <= 0){
                if ("on".equalsIgnoreCase(nuevo.valveState)) {
                    holder.btnRele.setBackgroundResource(R.drawable.color_verde_on);
                    holder.btnRele.setText("On");
                    sensor.valveState = "ON";
                } else {
                    holder.btnRele.setBackgroundResource(R.drawable.color_rojo_off);
                    holder.btnRele.setText("Off");
                    sensor.valveState = "OFF";
                }

                if("idle".equalsIgnoreCase(sensor.mode)) {
                    holder.controlContainer.setBackgroundResource(R.drawable.color_idle_mode);
                    holder.modeStatus.setChecked(false);
                }else{
                    holder.controlContainer.setBackgroundResource(R.drawable.color_control_mode);
                    holder.modeStatus.setChecked(true);
                }
                sensor.mode = nuevo.mode;

                if (!autoDecrement && !autoIncrement){
                    holder.tempValue.setText(nuevo.temperatureSet + " ºC");
                    //holder.tempValue.setTextColor(ContextCompat.getColor(context, R.color.normal_temperature_set));
                    sensor.temperatureSet = nuevo.temperatureSet;
                }

                if(sensor.address == 0){
                    holder.fondoSensor.setBackgroundResource(R.drawable.color_fondo_sensor_bloqueado);
                }else{
                    holder.fondoSensor.setBackgroundResource(R.drawable.color_fondo_sensor);
                }

                sensor.cntWait = 0;
            }
            //if (holder.btnRele.getBackground() )

        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        SpeedView temperatureGauge;
        SpeedView humidityGauge;
        Button btnRele;
        ConstraintLayout controlContainer;
        ConstraintLayout fondoSensor;
        ToggleButton modeStatus;
        Button btnIncrement;
        Button btnDecrement;
        TextView tempValue;
        TextView title;
        Button btnConfigurar;
        TextView tvTempValueTR;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            temperatureGauge = itemView.findViewById(R.id.fs_temperature_gauge);
            humidityGauge = itemView.findViewById(R.id.fs_humidity_gauge);
            btnRele = itemView.findViewById(R.id.fs_btn_rele);
            controlContainer = itemView.findViewById(R.id.fs_control_container);
            modeStatus = itemView.findViewById(R.id.fs_mode_toggle);
            btnIncrement = itemView.findViewById(R.id.fs_btn_increment);
            btnDecrement = itemView.findViewById(R.id.fs_btn_decrement);
            tempValue = itemView.findViewById(R.id.fs_temp_control_value);
            title = itemView.findViewById(R.id.fs_title);
            btnConfigurar = itemView.findViewById(R.id.fs_btn_parameters);
            fondoSensor = itemView.findViewById(R.id.fs_fondo);
            tvTempValueTR = itemView.findViewById(R.id.fs_temperature_value);
        }
    }

    public interface OnSensorAdapterRVListener {
        void onReleClick(int position);
        void onTemperatureControlChange(int position);
        void onModoChange(int position);
        void onClickParametros(int position);
    }
}
