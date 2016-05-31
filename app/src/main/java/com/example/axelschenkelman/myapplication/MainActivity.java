package com.example.axelschenkelman.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
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

    static final int cantJugadores=3;
    static int jugadaNumero ;
    static ArrayList<Integer> listaRandoms = new ArrayList<>();
    private final int PICK_IMAGE = 1;
    static final int PICK_CONTACT_REQUEST = 1;
    private ProgressDialog detectionProgressDialog;
     static FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("7ee6f47e2524476f90117bbf056760cb");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                //gallIntent.setType("image/*");
                //startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // start camera activity
                startActivityForResult(intent, PICK_IMAGE);
            }
        });
        while (listaRandoms.size() < cantJugadores) {
            Random r = new Random();
            int Result = r.nextInt(cantJugadores - 0) + 0;
            while (listaRandoms.contains(Result)) {

                Result = r.nextInt(cantJugadores - 0) + 0;
            }
            listaRandoms.add(Result);
        }
        jugadaNumero = 0;

        detectionProgressDialog = new ProgressDialog(this);


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
                                    null           // returnFaceAttributes: a string like "age, gender"
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
                        imageBitmap.recycle();
                    }
                };
        detectTask.execute(inputStream);
    }

    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces, Context context) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        int stokeWidth = 2;
        paint.setStrokeWidth(stokeWidth);
        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        if(jugadaNumero == 0){
            SaveFaces(faces);
        }
        jugadaNumero++;
        if(faces.length==cantJugadores) {

            CheckOrder(faces,context);
        }
        else{

            //No hay X caras
        }
        return bitmap;
    }
    static Face[] firstFaces = new Face[cantJugadores];
    static void SaveFaces(Face[] faces){
         for(int i =0; i< faces.length; i++){
             firstFaces[i] = faces[listaRandoms.get(i)];
         }
    }

    static void CheckOrder(Face[] faces,Context context){

        String currentState="";
        try{
            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
             }
        for(int i =0; i< firstFaces.length; i++){
            for(int j =0; j< faces.length; j++) {

                if (faceServiceClient.verify(firstFaces[i].faceId,faces[j].faceId).isIdentical)
                {
                    if(i==j) {
                        currentState += "B";
                    }
                    else{
                        currentState += "R";
                    }
                }
            }
            if(currentState.length() == i) {
                currentState += "M";
            }
        }
        }catch (Exception e){
            Log.d("Exception ",e.getMessage());
        }

            Toast.makeText(context, currentState,
                    Toast.LENGTH_LONG).show();



    }
}
