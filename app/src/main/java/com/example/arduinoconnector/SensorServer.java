package com.example.arduinoconnector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;

public class SensorServer extends NanoHTTPD {
    private MainActivity mainActivity;

    public SensorServer(int port, MainActivity mainActivity) {
        super(port);
        try {
            // Cargar certificado y clave privada desde los assets
            InputStream keyStream = mainActivity.getAssets().open("key.pem");
            InputStream certStream = mainActivity.getAssets().open("cert.pem");

            // Crear fábrica de sockets SSL
            SSLServerSocketFactory sslServerSocketFactory = createSSLServerSocketFactory(certStream, keyStream);

            // Configurar NanoHTTPD para usar SSL
            makeSecure(sslServerSocketFactory, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.mainActivity = mainActivity;
    }

    private SSLServerSocketFactory createSSLServerSocketFactory(InputStream certStream, InputStream keyStream) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Cargar certificado y clave privada
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
        Certificate cert = cf.generateCertificate(certStream);

        PEMParser pemParser = new PEMParser(new InputStreamReader(keyStream));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PrivateKey privateKey;

        //if (object instanceof org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo){ // PEMEncryptedKeyPair) {
        if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
            // La clave privada está cifrada como PKCS8EncryptedPrivateKeyInfo
            PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) object;

            // Obtén un PEMDecryptorProvider para desencriptar la clave privada
            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build("BEOO9IBZOE".toCharArray());

            // Desencripta la clave privada
            InputDecryptorProvider pkcs8Prov = new JcePKCSPBEInputDecryptorProviderBuilder()
                    .setProvider(new BouncyCastleProvider())
                    .build("BEOO9IBZOE".toCharArray());

            PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(pkcs8Prov);

            // Convierte PrivateKeyInfo a PrivateKey
            privateKey = converter.getPrivateKey(privateKeyInfo);
        } else if (object instanceof PrivateKeyInfo) {
            // La clave privada no está cifrada
            privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
        } else {
            throw new Exception("Tipo de objeto no reconocido: " + object.getClass().getName());
        }

        // Crear KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("cert", cert);
        keyStore.setKeyEntry("key", privateKey, new char[]{}, new Certificate[]{cert});

        // Crear fábrica de sockets SSL
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, new char[]{});

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        return context.getServerSocketFactory();
    }

    @Override
    public Response serve(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri().toLowerCase();
        JSONObject jsonObject = new JSONObject();

        // Analizar la URI para determinar la acción en función de la solicitud
        if (uri.startsWith("/api/sensor1") || uri.startsWith("/api/sensor2") || uri.startsWith("/api/sensor3") ||
                uri.startsWith("/api/sensor4") || uri.startsWith("/api/sensor5")) {
            // Realiza la acción correspondiente para /sensor1
            String response = GetJsonSensor(uri.toString());
            return newFixedLengthResponse(Response.Status.OK, "application/json", response);
        } else {
            // Manejar otras rutas o acciones aquí
            String response = mainActivity.readDataFromFile();
            Gson gson = new Gson();
            return newFixedLengthResponse(Response.Status.OK, "application/json", response);
            /*String response = "Ruta no encontrada";
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", response);*/
        }
    }

    private String GetJsonSensor(String sel){
        String jsonString = "";// = mainActivity.getJsonData();

        String fileContent = mainActivity.readDataFromFile();
        if (fileContent != null) {

            int addr;
            Gson gson = new GsonBuilder().setDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").create();
            JSONArray jsonArray;
            List<Sensor> sensorList;
            try {
                jsonArray = new JSONArray(fileContent);
                sensorList = gson.fromJson(fileContent, new TypeToken<List<Sensor>>() {}.getType());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if(sel.equals("/api/sensor1") || sel.equals("api/sensor1")){
                addr = mainActivity.getAddressSensor(0);
            }else if(sel.equals("/api/sensor2") || sel.equals("api/sensor2")){
                addr = mainActivity.getAddressSensor(1);
            }else if(sel.equals("/api/sensor3") || sel.equals("api/sensor3")){
                addr = mainActivity.getAddressSensor(2);
            }else if(sel.equals("/api/sensor4") || sel.equals("api/sensor4")){
                addr = mainActivity.getAddressSensor(3);
            }else if(sel.equals("/api/sensor5") || sel.equals("api/sensor5")){
                addr = mainActivity.getAddressSensor(4);
            }else if(sel.equals("/api/sensores") || sel.equals("api/sensores")){
                //Se entenderá que la dirección 10 es que devuelve la iformaciónn de todos los dispositivos
                addr = 10;
            } else {
                addr = -1;
            }

            if(addr >= 0) {
                if(addr == 10){
                    jsonString = gson.toJson(sensorList);
                }else {
                    List<Sensor> sensoresFiltrados = sensorList.stream()
                            .filter(sensor -> sensor.address == addr)
                            .collect(Collectors.toList());
                    jsonString = gson.toJson(sensoresFiltrados);
                }
            }
        } else {
            // Manejar el caso en que ocurra un error al leer el archivo
        }

        return jsonString;
    }


}

