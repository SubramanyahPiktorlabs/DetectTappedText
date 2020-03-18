package com.piktorlabs.imagetextrecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.piktorlabs.imagetextrecognizer.GraphicUtils.GraphicOverlay;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Hey";
    private ImageView imageView;
    private Button captureImage, selectImage, detectText;
    private TextView detectedText;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap imageBitmap;
    @BindView(R.id.graphic_overlay) GraphicOverlay mGraphicOverlay;
    Point[] points;
    List<int[]> coOrdinatesList = new ArrayList<int[]>();
    HashMap<String, RectF> CoOrdinates = new HashMap<String, RectF>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addTouchListner();

        captureImage = findViewById(R.id.capture_image);
//        selectImage = findViewById(R.id.select_image);
        detectText = findViewById(R.id.detect_text);
        imageView = findViewById(R.id.image_view);
        detectedText = findViewById(R.id.image_text);

        captureImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
                detectedText.setText("");
            }
        });
        detectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                detectImageText();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            CoOrdinates.clear();
            imageView.setImageBitmap(imageBitmap);
            detectImageText();
        }
    }

    private void addTouchListner(){
        imageView = findViewById(R.id.image_view);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                imageView.getLocationOnScreen(viewCords);
//                float x = event.getX();
//                float y = event.getY();

                Matrix inverse = new Matrix();
                ((ImageView) v).getImageMatrix().invert(inverse);
                float[] touchPoint = new float[]{event.getX(), event.getY()};
                inverse.mapPoints(touchPoint);
                float x = (float) touchPoint[0];
                float y = (float) touchPoint[1];

                String msg = String.format("Position: (%.2f,%.2f)",x,y);
                for(String i : CoOrdinates.keySet()){
                    float left,top,right,bottom;
                    left = CoOrdinates.get(i).left;
                    right = CoOrdinates.get(i).right;
                    top = CoOrdinates.get(i).top;
                    bottom = CoOrdinates.get(i).bottom;
//                  if(CoOrdinates.get(i).contains(x,y)) {
                    if(x>=left && x<=right && y>=bottom && y<=top ){
                        Toast.makeText(MainActivity.this, i, Toast.LENGTH_LONG).show();
                        return true;
                    }
                    else{
                        Toast.makeText(MainActivity.this,"Not Matched",Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                return false;
            }
        });
    }
    private void detectImageText(){
//        BitmapDrawable bitmapDrawable = (BitmapDrawable)imageView.getDrawable();
//        Bitmap imageBitMap = bitmapDrawable.getBitmap();

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if(!textRecognizer.isOperational()){
            Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show();
        }else{
            Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();

            SparseArray items = textRecognizer.detect(frame);
            List<TextBlock> blocks = new ArrayList<TextBlock>();

            TextBlock myItem = null;
            for (int i = 0; i < items.size(); ++i)
            {
                myItem = (TextBlock)items.valueAt(i);

                //Add All TextBlocks to the `blocks` List
                blocks.add(myItem);

            }

            Paint rectPaint = new Paint();
            rectPaint.setColor(Color.WHITE);
            rectPaint.setStyle(Paint.Style.STROKE);
            rectPaint.setStrokeWidth(1.0f);
            
            Bitmap tempBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(tempBitmap);
            canvas.drawBitmap(imageBitmap, 0, 0, null);

            for (TextBlock textBlock : blocks){
                List<? extends Text> textLines = textBlock.getComponents();
                for(Text currentLine : textLines){
                    List<? extends Text> words = currentLine.getComponents();
                    for(Text currentWord : words){
                        RectF rect = new RectF(currentWord.getBoundingBox());
                        CoOrdinates.put(currentWord.getValue(),rect);
                        rectPaint.setColor(Color.BLACK);
                        canvas.drawRect(rect,rectPaint);
                        imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
                    }
                }
            }
        }
    }

    //below methods are not in use
    private void detectTextFromImage() {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector firebaseVisionTextDetector = FirebaseVision.getInstance().getVisionTextDetector();
        firebaseVisionTextDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                displayTextFromimage(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Error:" +e.getMessage() , Toast.LENGTH_LONG).show();

                Log.d("Error: " , e.getMessage());
            }
        });
    }
    private void displayTextFromimage(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        if(blockList.size() == 0)
        {
            Toast.makeText(this,"No Text found in Image", Toast.LENGTH_LONG).show();
        }
        else {
            BitmapDrawable bitmapDrawable = (BitmapDrawable)imageView.getDrawable();
            Bitmap imageBitMap = bitmapDrawable.getBitmap();

            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

            if(!textRecognizer.isOperational()){
                Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show();
            }
            else{
                Frame frame = new Frame.Builder().setBitmap(imageBitMap).build();

                SparseArray items = textRecognizer.detect(frame);
                List<TextBlock> blocks = new ArrayList<TextBlock>();

                TextBlock myItem = null;
                for (int i = 0; i < items.size(); ++i)
                {
                    myItem = (TextBlock)items.valueAt(i);

                    //Add All TextBlocks to the `blocks` List
                    blocks.add(myItem);

                }
                Toast.makeText(this,items.size(),Toast.LENGTH_LONG).show();

                Bitmap tempBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(imageBitmap, 0, 0, null);

                for(int i=0;i<blocks.size();i++){
                    List<FirebaseVisionText.Line> Lines = blockList.get(i).getLines();
                    for(int j=0;j<Lines.size();j++){
                        List<FirebaseVisionText.Element> elements = Lines.get(j).getElements();
//                    for(int k=0;k<elements.size();k++){
//                        Rect rect = elements.getBoundingBox
//                    }
                        for(FirebaseVisionText.Element e:elements){
                            Rect rect = e.getBoundingBox();
                            points = e.getCornerPoints();
                        }
                    }
                }
            }
//            for(FirebaseVisionText.Block block: firebaseVisionText.getBlocks())
//            {
//                String text = block.getText();
//                detectedText.setText(text);
//            }
        }
    }
}
