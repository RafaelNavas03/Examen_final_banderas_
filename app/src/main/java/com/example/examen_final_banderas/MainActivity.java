package com.example.examen_final_banderas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.Manifest;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.examen_final_banderas.ml.Banderas;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.text.Text;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class MainActivity extends AppCompatActivity
        implements OnSuccessListener<Text>, OnFailureListener {

    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;

    Bitmap mSelectedImage;
    ImageView mImageView;
    ImageView imgBandera;
    TextView txtResults;
    RequestQueue requestQueue;

    String Prefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);
        imgBandera = findViewById(R.id.imgBandera);
        txtResults = findViewById(R.id.txtresults);
        requestQueue = Volley.newRequestQueue(this);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
    }

    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        txtResults.setText("Error al procesar imagen");
    }

    @Override
    public void onSuccess(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
        String resultados = "";
        if (blocks.size() == 0) {
            resultados = "No hay Texto";
        } else {
            for (int i = 0; i < blocks.size(); i++) {
                List<Text.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    List<Text.Element> elements = lines.get(j).getElements();
                    for (int k = 0; k < elements.size(); k++) {
                        resultados = resultados + elements.get(k).getText() + " ";
                    }
                }
            }
            resultados = resultados + "\n";
        }
        txtResults.setText(resultados);
    }

    public void PersonalizedModel(View v) {
        try {
            String[] etiquetas = {"AR", "BE", "BR", "CO", "CR", "EC", "ES", "FR", "GB", "MX", "PT", "SE", "UY"};
            Banderas model = Banderas.newInstance(getApplicationContext());
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(convertirImagenATensorBuffer(mSelectedImage));
            Banderas.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            txtResults.setText(obtenerEtiquetayProbabilidad(etiquetas, outputFeature0.getFloatArray()));

            Prefix = obtenerEtiquetayProbabilidad(etiquetas,
                    outputFeature0.getFloatArray());

            InfoPais(Prefix);

            model.close();
        } catch (Exception e) {
            txtResults.setText(e.getMessage());
        }
    }

    public void InfoPais(String Codigo) {
        String Prefijo = Codigo;
        String url = "http://www.geognos.com/api/en/countries/info/" + Prefijo + ".json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ParseoInfoPais(response, Prefijo);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleError();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void ParseoInfoPais(JSONObject response, String Prefijo) {
        try {
            JSONObject countryInfo = response.getJSONObject("Results");
            String countryName = countryInfo.getString("Name");
            JSONObject capitalInfo = countryInfo.getJSONObject("Capital");
            JSONObject GeoRectangle = countryInfo.getJSONObject("GeoRectangle");
            String west = GeoRectangle.getString("West");
            String east = GeoRectangle.getString("East");
            String north = GeoRectangle.getString("North");
            String south = GeoRectangle.getString("South");
            String capitalName = capitalInfo.getString("Name");
            String telPrefix = countryInfo.getString("TelPref");
            JSONObject countryCodes = countryInfo.getJSONObject("CountryCodes");
            String tld = countryCodes.getString("tld");
            String iso2 = countryCodes.getString("iso2");
            String iso3 = countryCodes.getString("iso3");
            String fips = countryCodes.getString("fips");
            String isoN = countryCodes.getString("isoN");

            String countryInfoText =
                    "País: " + countryName + "\n" +
                            "Capital: " + capitalName + "\n" +
                            "Coordenadas:" + "\n" +
                            "Oeste: " + west + "\n" +
                            "Este: " + east + "\n" +
                            "Norte: " + north + "\n" +
                            "Sur: " + south + "\n" +
                            "TelPref: " + telPrefix + "\n" +
                            "Códigos de país:" + "\n" +
                            "tld: " + tld + "\n" +
                            "Iso2: " + iso2 + "\n" +
                            "Iso3: " + iso3 + "\n" +
                            "Fips: " + fips + "\n" +
                            "isoN: " + isoN + "\n";

            txtResults.setText(countryInfoText);


            mostrarBandera(Prefijo);

        } catch (JSONException e) {
            e.printStackTrace();
            handleError();
        }
    }

    private void handleError() {

        txtResults.setText("Error obtener la información.");
    }


    public void mostrarBandera(String Prefijo) {
        String url = "http://www.geognos.com/api/en/countries/flag/" + Prefijo + ".png";

        // Carga la imagen desde la URL utilizando Volley
        ImageRequest imageRequest = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        
                        imgBandera.setImageBitmap(response);
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        
                        error.printStackTrace();
                        Log.e("Error", "Error al cargar la imagen de la bandera: " + error.getMessage());
                        txtResults.setText("Error al cargar la imagen de la bandera.");
                    }
                });

        // Agrega la solicitud a la cola de solicitudes
        requestQueue.add(imageRequest);
    }

    public ByteBuffer convertirImagenATensorBuffer(Bitmap mSelectedImage) {
        Bitmap imagen = Bitmap.createScaledBitmap(mSelectedImage, 224, 224, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        imagen.getPixels(intValues, 0, imagen.getWidth(), 0, 0, imagen.getWidth(), imagen.getHeight());
        int pixel = 0;
        for (int i = 0; i < imagen.getHeight(); i++) {
            for (int j = 0; j < imagen.getWidth(); j++) {
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        return byteBuffer;
    }

    public String obtenerEtiquetayProbabilidad(String[] etiquetas, float[] probabilidades) {
        float valorMayor = Float.MIN_VALUE;
        int pos = -1;
        for (int i = 0; i < probabilidades.length; i++) {
            if (probabilidades[i] > valorMayor) {
                valorMayor = probabilidades[i];
                pos = i;
            }
        }
        return etiquetas[pos];
    }
}
