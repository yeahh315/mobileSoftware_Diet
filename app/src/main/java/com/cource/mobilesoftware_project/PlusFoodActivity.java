package com.cource.mobilesoftware_project;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class PlusFoodActivity extends AppCompatActivity {

    Calendar myCalendar = Calendar.getInstance();

    ArrayList<KcalItem> kcalData = new ArrayList<>();

    private String latitude;
    private String longitude;

    DatePickerDialog.OnDateSetListener myDatePicker = (view, year, month, dayOfMonth) -> {
        myCalendar.set(Calendar.YEAR, year);
        myCalendar.set(Calendar.MONTH, month);
        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateLabel();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plus_food_activity);

        //?????? ??? ?????? ??????
        TextView function_text = findViewById(R.id.plus_explain); //????????? ?????? ??????

        String content = function_text.getText().toString(); //????????? ?????????.
        SpannableString spannableString = new SpannableString(content); //?????? ??????

        String word ="????????? ????????????";
        int start = content.indexOf(word);
        int end = start + word.length();

        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#EC7357")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        function_text.setText(spannableString);

        //?????? ?????? ?????????
        EditText et_Date = findViewById(R.id.Date);
        et_Date.setOnClickListener(v -> new DatePickerDialog(PlusFoodActivity.this, myDatePicker, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show());

        final EditText et_time = findViewById(R.id.Time);
        et_time.setOnClickListener(v -> {
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);                //???????????? : +9
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(PlusFoodActivity.this, (timePicker, selectedHour, selectedMinute) -> {
                String state = "AM";
                // ????????? ????????? 12??? ???????????? "PM"?????? ?????? ??? -12???????????? ?????? (ex : PM 6??? 30???)
                if (selectedHour > 12) {
                    selectedHour -= 12;
                    state = "PM";
                }
                // EditText??? ????????? ?????? ??????
                et_time.setText(state + " " + selectedHour + "??? " + selectedMinute + "???");
            }, hour, minute, false); // true??? ?????? 24?????? ????????? TimePicker ??????
            mTimePicker.setTitle("Select Time");
            mTimePicker.show();
        });

        Button popupButton = findViewById(R.id.PopupButton);

        findViewById(R.id.PopupButton).setOnClickListener(view -> {
            final PopupMenu popupMenu = new PopupMenu(getApplicationContext(),view);
            getMenuInflater().inflate(R.menu.meal_main,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.morning)
                    popupButton.setText(R.string.morning);
                else if (menuItem.getItemId() == R.id.lunch)
                    popupButton.setText(R.string.lunch);
                else if(menuItem.getItemId() == R.id.dinner)
                    popupButton.setText(R.string.dinner);
                else
                    popupButton.setText(R.string.snack);
                return false;
            });
            popupMenu.show();
        });

        ImageView imageView = findViewById(R.id.imageView);
        final Bitmap[] img_bitmap = new Bitmap[1];

        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Glide.with(this)
                            .asBitmap()
                            .load(result.getData().getData())
                            .override(127, 93)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    imageView.setImageBitmap(resource);
                                    img_bitmap[0] = resource;
                                }
                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                });

        TextView place = findViewById(R.id.place);

        ActivityResultLauncher<Intent> googleMapLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent intent = result.getData();
                        place.setText(intent.getStringExtra("place"));
                        latitude = intent.getStringExtra("lati");
                        longitude = intent.getStringExtra("longi");
                    }
                }
        );

        findViewById(R.id.input).setOnClickListener(view -> {

            final Bundle bundle = new Bundle();

            String date=((EditText)findViewById(R.id.Date)).getText().toString();
            String time=((EditText)findViewById(R.id.Time)).getText().toString();
            String food_category= popupButton.getText().toString();
            String food_name = ((EditText)findViewById(R.id.food_name)).getText().toString();
            String food_cnt = ((EditText)findViewById(R.id.food_cnt)).getText().toString();
            String food_summary = ((EditText)findViewById(R.id.editTextTextMultiLine)).getText().toString();
            String img_name= date + time + "_"+food_category + ".jpg";
            String place_string = place.getText().toString();

            /*
             * ?????? ??????*/
            if(food_category.equals("?????? ??????")){
                showWrongInput("?????? ??????");
                return;
            }
            if(place_string.equals("")){
                showWrongInput("??????");
                return;
            }
            if(food_name.equals("")){
                showWrongInput("?????? ??????");
                return;
            }
            if(food_cnt.equals("")){
                showWrongInput("?????? ??????");
                return;
            }
            if(date.equals("")){
                showWrongInput("??????");
                return;
            }
            if(time.equals("")){
                showWrongInput("??????");
                return;
            }
            if(food_summary.equals("")){
                showWrongInput("?????? ??????");
                return;
            }

            bundle.putString("date",date);
            bundle.putString("time",time);
            bundle.putString("food_category",food_category);
            bundle.putString("food_name",food_name);
            bundle.putInt("food_cnt",Integer.parseInt(food_cnt));
            bundle.putString("food_summary", food_summary);
            bundle.putString("latitude", latitude);
            bundle.putString("longitude", longitude);

            saveBitmapToJpeg(img_bitmap[0], img_name);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            img_bitmap[0].compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
            byte[] byteArray = stream.toByteArray() ;
            bundle.putByteArray("bm", byteArray);

            //kcal data ????????????
            readKcalData();

            int tmp_kcal = 400;
            tmp_kcal = get_kcal(food_name);

            bundle.putInt("food_kcal", tmp_kcal * Integer.parseInt(food_cnt));


            ContentValues addValues = new ContentValues();
            addValues.put(MyContentProvider.DATE,date);
            addValues.put(MyContentProvider.TIME,time);
            addValues.put(MyContentProvider.CATEGORY,food_category);
            addValues.put(MyContentProvider.NAME,food_name);
            addValues.put(MyContentProvider.CNT,Integer.parseInt(food_cnt));
            addValues.put(MyContentProvider.KCAL, tmp_kcal * Integer.parseInt(food_cnt));
;           addValues.put(MyContentProvider.SUMMERY,food_summary);
            addValues.put(MyContentProvider.BYTE, byteArray);
            addValues.put(MyContentProvider.PLACE, place_string);
            addValues.put(MyContentProvider.LATITUDE, latitude);
            addValues.put(MyContentProvider.LONGITUDE, longitude);

            try {
//                Toast.makeText(getApplicationContext(), "?????? ?????? ??????", Toast.LENGTH_SHORT).show();

                getContentResolver().insert(MyContentProvider.CONTENT_URI, addValues);
//                CustomPopupPlus customDialog = new CustomPopupPlus(PlusFoodActivity.this,bundle);
//                customDialog.show();
                Intent intent = new Intent(getApplicationContext(), CustomPopup.class);
                intent.putExtra("Bundle", bundle);
                startActivity(intent);
                finish();
            }
            catch (Exception e){
                Toast.makeText(getApplicationContext(), "?????? ?????? ??????", Toast.LENGTH_SHORT).show();
            }
        });

        imageView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        findViewById(R.id.place).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PlusFoodActivity.this,GoogleMapActivity.class);
                googleMapLauncher.launch(intent);
            }
        });
    }

    private void showWrongInput(String input){
        Log.d("wrong input2",input);
        AlertDialog.Builder msgBuilder = new AlertDialog.Builder(this)
                .setTitle("?????? ??????!")
                .setMessage(input+"??? ???????????????");
        AlertDialog msgDlg = msgBuilder.create();
        msgDlg.show();
    }

    private void saveBitmapToJpeg(Bitmap bitmap, String name) {
        //??????????????? ?????? ????????? ???????????????.
        File storage = getFilesDir();
        //????????? ?????? ??????
        String fileName = name;
        //storage ??? ?????? ??????????????? ???????????????.
        File tempFile = new File(storage, fileName);
        try {
            // ???????????? ??? ????????? ???????????????.
            tempFile.createNewFile();
            // ????????? ??? ??? ?????? ???????????? ???????????????.
            FileOutputStream out = new FileOutputStream(tempFile);
            // compress ????????? ????????? ???????????? ???????????? ???????????????.
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            // ????????? ????????? ???????????????.
            out.close();
        } catch (FileNotFoundException e) {
            Log.e("MyTag","FileNotFoundException : " + e.getMessage());
        } catch (IOException e) {
            Log.e("MyTag","IOException : " + e.getMessage());
        }
    }


    private void updateLabel() {
        String myFormat = "yyyy-MM-dd";    // ????????????   2021/07/26
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.KOREA);

        EditText et_date = findViewById(R.id.Date);
        et_date.setText(sdf.format(myCalendar.getTime()));
    }

    public void gotoBack(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //?????? ??? ????????? ????????? ????????? ??????
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View focusView = getCurrentFocus();
        if (focusView != null) {
            Rect rect = new Rect();
            focusView.getGlobalVisibleRect(rect);
            int x = (int) ev.getX(), y = (int) ev.getY();
            if (!rect.contains(x, y)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                focusView.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    //????????? ???????????? ??????
    private void readKcalData(){
        InputStream is = getResources().openRawResource(R.raw.foods_kcal);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

        String line = "";
        try{
            while((line = reader.readLine()) != null){
                String[] tokens = line.split(",");

                KcalItem sample = new KcalItem();
                sample.setIndex( Integer.parseInt(tokens[0]));
                sample.setF_kcal( (int)(Float.parseFloat(tokens[1])));
                sample.setF_name(tokens[2]);

                kcalData.add(sample);

            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private int get_kcal(String name){
        Random random = new Random();
        for(int i = 0; i < kcalData.size(); i++){
            if(name.equals(kcalData.get(i).getF_name())){
                return kcalData.get(i).getF_kcal();
            }
        }
        return 350 - random.nextInt(200); //????????? ???
    }
}
