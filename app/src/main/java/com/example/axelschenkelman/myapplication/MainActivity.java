package com.example.axelschenkelman.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static int personaCargadas =0;
    static final int cantJugadores=1;
    public static boolean gano = false;
    static int jugadaNumero ;
    static ArrayList<Integer> listaRandoms = new ArrayList<>();
    private final int PICK_IMAGE = 1;
    static final int PICK_CONTACT_REQUEST = 1;
    private ProgressDialog detectionProgressDialog;
     static FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("7ee6f47e2524476f90117bbf056760cb");
    public static boolean jugando = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button1 = (Button) findViewById(R.id.button1);
        PonerImagenInicial();


        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!jugando) {
                    //Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    //gallIntent.setType("image/*");
                    //startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    // start camera activity
                    startActivityForResult(intent, PICK_IMAGE);
                }else {

                    PickFaces();
                    jugadaNumero = 0;
                    Toast.makeText(getApplicationContext(), "Estoy seguro de que perderas de vuelta..", Toast.LENGTH_SHORT).show();
                }
            }
        });;
        Button buttonJugar = (Button) findViewById(R.id.button);
        buttonJugar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allFaces.size() > 0) {
                    if (!jugando) {
                        Button b = (Button) v;
                        PonerImagenInicial();
                        b.setText("ES ESTE?");


                        Button buttonBorrar = (Button) findViewById(R.id.button2);
                        buttonBorrar.setVisibility(View.VISIBLE);
                        Button buttonPistas = (Button) findViewById(R.id.button3);
                        buttonPistas.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(), "Estoy seguro de que perderas..", Toast.LENGTH_SHORT).show();



                    } else {
                       // Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                       // gallIntent.setType("image/*");
                       // startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                        // start camera activity
                        startActivityForResult(intent, PICK_IMAGE);
                    }
                    jugando = true;
                    PickFaces();
                    jugadaNumero = 0;
                    final Button button1 = (Button) findViewById(R.id.button1);
                    button1.setText("TE RENDIS? ELIGO A OTRO");

            }else{
                Toast.makeText(getApplicationContext(), "No hay personas cargadas", Toast.LENGTH_SHORT).show();
            }
        }});;


        detectionProgressDialog = new ProgressDialog(this);
    }

    private void PonerImagenInicial() {

        ImageView imageView = (ImageView) findViewById(R.id.imageView1);
        imageView.setImageBitmap(null);
        Drawable d = getResources().getDrawable(android.R.drawable.ic_menu_help);
        imageView.setImageDrawable(d);
    }

    public void BorrarCargados(View v){
        personaCargadas =0;
        PonerImagenInicial();
        pistasDadas =0;
        gano = false;
        Button buttonBorrar = (Button) v;
        buttonBorrar.setVisibility(View.INVISIBLE);
        Button buttonPistas = (Button) findViewById(R.id.button3);
        buttonPistas.setVisibility(View.INVISIBLE);
        Button buttonJugar = (Button) findViewById(R.id.button);
        final Button button1 = (Button) findViewById(R.id.button1);
        button1.setText("CARGAR PERSONAS");
        buttonJugar.setText("JUGAR");
        allFaces.clear();
        listaRandoms.clear();
        selectedFaces.clear();
        jugadaNumero =0;
        jugando = false;

        Toast.makeText(getApplicationContext(), "Las caras cargadas han sido borradas", Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       /* if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);
                detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } */

        if (requestCode == PICK_CONTACT_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                    imageView.setImageBitmap(bitmap);
                    detectAndFrame(bitmap);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    // Detect faces by uploading face images
// Frame faces after detection
    private void detectAndFrame(final Bitmap imageBitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());


        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    FaceServiceClient.FaceAttributeType.values()           // returnFaceAttributes: a string like "age, gender"
                            );
                            if (result == null)
                            {
                                publishProgress("Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(
                                    String.format("Detection Finished. %d face(s) detected",
                                            result.length));
                            return result;
                        } catch (Exception e) {
                            publishProgress("Detection failed");
                            return null;
                        }
                    }
                    @Override
                    protected void onPreExecute() {

                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {

                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {

                        detectionProgressDialog.dismiss();
                        if (result == null) return; //Mostrar hubo un error
                        ImageView imageView = (ImageView)findViewById(R.id.imageView1);
                        imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result,getApplicationContext()));
                        if(gano){
                            Toast.makeText(getApplicationContext(), "Ganaste!", Toast.LENGTH_LONG).show();
                        }
                        imageBitmap.recycle();
                    }
                };
        detectTask.execute(inputStream);

    }

    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces, Context context) {
        ArrayList<String> resultado = new ArrayList<>();
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        if(faces.length>0) {
            if (!jugando) {
                LoadFaces(faces);
            } else {
                if(faces.length == cantJugadores) {
                    jugadaNumero++;
                    resultado = CheckOrder(faces, context);
                }
            }
        }

        Paint paint2 = new Paint();
        paint2.setAntiAlias(true);
        paint2.setStyle(Paint.Style.FILL);
        paint2.setTextSize(35);
        paint.setColor(Color.YELLOW);
        paint2.setColor(Color.YELLOW);
        int stokeWidth = 5;
        paint.setStrokeWidth(stokeWidth);
        paint2.setStrokeWidth(1);

        int contAux=0;
        if (faces != null) {
            for (Face face : faces) {
                personaCargadas++;
                FaceRectangle faceRectangle = face.faceRectangle;
                String isSmiling = "no sonrie";
                if(face.faceAttributes.smile >0.5){
                    isSmiling = "sonriendo! :)";
                }
                String sexo = "femenino";
                if(face.faceAttributes.gender.contentEquals("male")){
                    sexo = "masculino";
                }

                canvas.drawText(sexo,faceRectangle.left + faceRectangle.width,faceRectangle.top,paint2);
                canvas.drawText(isSmiling,faceRectangle.left + faceRectangle.width,faceRectangle.top +30,paint2);

                if(!jugando){
                     paint.setColor(Color.YELLOW);
                    canvas.drawText(personaCargadas+"",faceRectangle.left + faceRectangle.width,faceRectangle.top -30,paint2);


                }
                else{
                    String estaCara = resultado.get(contAux);
                    contAux++;
                    if(estaCara =="B")
                    {

                        paint.setColor(Color.GREEN);
                        paint2.setColor(Color.GREEN);
                        canvas.drawText("EUREKA!",faceRectangle.left + faceRectangle.width,faceRectangle.top+60,paint2);
                        gano = true;

                    }else if(estaCara =="B"){
                        paint.setColor(Color.YELLOW);
                    }
                    else{
                        paint.setColor(Color.RED);
                        paint2.setColor(Color.RED);
                        canvas.drawText("INCORRECTO",faceRectangle.left + faceRectangle.width,faceRectangle.top+60,paint2);
                    }
                }

                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }


        return bitmap;
    }
    static ArrayList<Face> allFaces = new ArrayList<>();

    static void LoadFaces(Face[] faces){
         for(int i =0; i< faces.length; i++){
             allFaces.add(faces[i]);
         }
    }
    static ArrayList<Face> selectedFaces = new ArrayList<>();

    int pistasDadas =0;
    public void DarPista(View v){
        String textoPista="";
        if(pistasDadas ==0){
            String sexo = selectedFaces.get(0).faceAttributes.gender;
            if(sexo.contains("male")){
                textoPista += "Es hombre";
            }else {
                textoPista += "Es mujer";
            }
        }
        else if(pistasDadas ==1){
            double smile = selectedFaces.get(0).faceAttributes.smile;
            if(smile > 0.5){
                textoPista += "Está sonriendo";
            }else{
                textoPista += "No está sonriendo";
            }
        }
        else{
            textoPista = "No hay más pistas!";
        }
        Toast.makeText(getApplicationContext(), textoPista, Toast.LENGTH_LONG).show();
        pistasDadas++;
    }

    static void PickFaces(){
        listaRandoms.clear();
        selectedFaces.clear();
        while (listaRandoms.size() < cantJugadores) {
            Random r = new Random();
            int Result = r.nextInt(allFaces.size() - 0) + 0;
            while (listaRandoms.contains(Result)) {

                Result = r.nextInt(allFaces.size() - 0) + 0;
            }
            listaRandoms.add(Result);
        }

        for(int i =0; i< listaRandoms.size(); i++){
            selectedFaces.add(allFaces.get(listaRandoms.get(i)));
        }
       // selectedFaces.add(allFaces.get(0));
    }


    static ArrayList<String> CheckOrder(Face[] faces,Context context){
        ArrayList<String> result = new ArrayList<>();

        try{
        if(Build.VERSION.SDK_INT>9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        for(int i =0; i< selectedFaces.size(); i++){
            for(int j =0; j< faces.length; j++) {


                if (faceServiceClient.verify(selectedFaces.get(i).faceId, faces[j].faceId).isIdentical)
                {
                    if(i==j) {
                        result.add("B");
                    }
                    else{
                        result.add("R");
                    }
                }
            }
            if(result.size() == i) {
                result.add("M");;
            }
        }
        }catch (Exception e){
            Log.d("Exception ",e.getMessage());
        }
        //Toast.makeText(context, currentState,Toast.LENGTH_LONG).show();

        return  result;




    }
}
