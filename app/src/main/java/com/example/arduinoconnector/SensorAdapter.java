package com.example.arduinoconnector;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.components.Section;

import java.util.List;

public class SensorAdapter extends ArrayAdapter<Sensor> {
    private final Context context;
    private final List<Sensor> sensors;
    private LayoutInflater layoutInflater;
    private OnSensorAdapterListener listener;

    public void setOnSensorAdapterListener(OnSensorAdapterListener listener) {
        this.listener = listener;
    }


    public SensorAdapter(Context context, List<Sensor> sensors) {
        super(context, R.layout.fragment_sensor, sensors);
        this.context = context;
        this.sensors = sensors;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.fragment_sensor, parent, false);
            holder = new ViewHolder();
            holder.temperatureGauge = convertView.findViewById(R.id.fs_temperature_gauge);
            holder.humidityGauge = convertView.findViewById(R.id.fs_humidity_gauge);
            convertView.setTag(holder);
            setupGauge(holder.temperatureGauge, holder.humidityGauge);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Sensor sensor = sensors.get(position);

        Button btnRele = convertView.findViewById(R.id.fs_btn_rele);
        if ("on".equalsIgnoreCase(sensor.valveState)) {
            btnRele.setBackgroundResource(R.drawable.color_verde_on);
            btnRele.setText("On");
        } else {
            btnRele.setBackgroundResource(R.drawable.color_rojo_off);
            btnRele.setText("Off");
        }

        btnRele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cambia el estado del botón a amarillo (presionado)
                btnRele.setBackgroundResource(R.drawable.color_amarillo_wait);
                if (listener != null) {
                    listener.onReleClick(position);
                }
            }
        });

        ConstraintLayout controlContainer = convertView.findViewById(R.id.fs_control_container);
        ToggleButton modeStatus = convertView.findViewById(R.id.fs_mode_toggle);

        if("idle".equalsIgnoreCase(sensor.mode)) {
            controlContainer.setBackgroundResource(R.drawable.color_idle_mode);
            modeStatus.setChecked(false);
        }else{
            controlContainer.setBackgroundResource(R.drawable.color_control_mode);
            modeStatus.setChecked(true);
        }

        modeStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                controlContainer.setBackgroundResource(R.drawable.color_wait_mode);
                if(isChecked){
                    sensor.mode = "control";
                }else{
                    sensor.mode = "idle";
                }
                if (listener != null) {
                    listener.onModoChange(position);
                }
            }
        });

        // Utiliza las vistas en el holder para configurar los datos
        holder.temperatureGauge.speedTo(sensor.temperature);
        holder.humidityGauge.speedTo(sensor.humidity);
        //holder.address.setText(String.valueOf(sensor.address));
        //holder.idDispositivo.setText(String.valueOf(sensor.identifier));
        holder.temperatureGauge.speedTo(sensor.temperature);
        holder.humidityGauge.speedTo(sensor.humidity);
        //holder.tempControl.setText(String.valueOf(sensor.temperatureSet));
        return convertView;

    }

    static class ViewHolder {
        SpeedView temperatureGauge;
        SpeedView humidityGauge;
        // ... otras vistas ...
        //EditText address;
        //EditText idDispositivo;
    }


    @Override
    public int getCount() {
        return sensors.size();
    }

    @Nullable
    @Override
    public Sensor getItem(int position) {
        return sensors.get(position);
    }

    @Override
    public int getPosition(@Nullable Sensor item) {
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).identifier;
    }

    private void setupGauge(SpeedView temperatureGauge, SpeedView humidityGauge) {

        temperatureGauge.clearSections();
        temperatureGauge.addSections(new Section(0f, .5f, Color.BLACK)
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

    public interface OnSensorAdapterListener {
        void onReleClick(int position);
        void onTemperatureControlChange(int position);
        void onModoChange(int position);
    }
}

