package com.ambinusian.adab.all;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ambinusian.adab.R;
import com.ambinusian.adab.manager.APIManager;
import com.ambinusian.adab.manager.NetworkHelper;
import com.ambinusian.adab.preferences.UserPreferences;
import com.ambinusian.adab.recyclerview.course.CourseHolder;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.button.MaterialButton;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityLive extends AppCompatActivity{

    private Toolbar toolbar;
    private TextView className;
    private TextView classSession;
    private TextView textContent;
    private TextView textLiveNow;
    private TextView courseTitle;
    private TextView toolbarTitle;
    private Socket socket;
    private RelativeLayout loadingLayout;
    private RelativeLayout contentLoadingLayout;
    private ScrollView scrollViewMain;
    private UserPreferences userPreferences;
    private Integer sessionId;
    private Integer canTalk;
    private MaterialButton talkButton;
    private LinearLayout layoutButtons;
    private String courseLanguage;
    private Boolean singleResult = true; //keep that the speech recognizer just return single result, not double

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_session);

        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        sessionId = bundle.getInt("session_id");

        if (sessionId == null) finish();

        Log.d("ClassId", sessionId.toString());

        toolbar = findViewById(R.id.toolbar);
        className = findViewById(R.id.tv_class_name);
        classSession = findViewById(R.id.tv_class_session);
        textContent = findViewById(R.id.tv_content);
        textLiveNow = findViewById(R.id.tv_live_now);
        courseTitle = findViewById(R.id.tv_course_title);
        loadingLayout = findViewById(R.id.layout_loading);
        toolbarTitle = findViewById(R.id.toolbar_title);
        contentLoadingLayout = findViewById(R.id.layout_loading_content);
        scrollViewMain = findViewById(R.id.scrollview_main);
        talkButton = findViewById(R.id.button_talk);
        layoutButtons = findViewById(R.id.layout_buttons);
        APIManager apiManager = new APIManager(this);
        userPreferences = new UserPreferences(this);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbarTitle.setVisibility(View.GONE);
        scrollViewMain.setVisibility(View.GONE);

        //scroll always to bottom
        textContent.setMovementMethod(new ScrollingMovementMethod());

        //set high contrast if enabled
        if(userPreferences.getHighContrast()){
            setHighConstrastTheme();
        }

        // set text attributes
        setTextSize();
        setTextTypeface();

        apiManager.getClassDetails(userPreferences.getUserToken(), sessionId, new NetworkHelper.getClassDetails() {
            @Override
            public void onResponse(Boolean success, Map<String, Object> classDetails) {
                if (success) {
                    String courseTitleText = (String) classDetails.get("topic_title");
                    String classNameText = (String) classDetails.get("course_name");
                    String sessionText = getString(R.string.class_session) + " " + classDetails.get("session_th");
                    String content = (String) classDetails.get("content");
                    courseLanguage = (String) classDetails.get("course_lang");
                    canTalk = (int) classDetails.get("can_talk");

                    courseTitle.setText(courseTitleText);
                    className.setText(classNameText);
                    classSession.setText(sessionText);
                    textContent.setText(content);

                    Date endDate = null;
                    try {
                        endDate = new SimpleDateFormat("yy-MM-dd HH:mm").parse((String) classDetails.get("session_enddate"));
                    } catch (Exception e) { }



//                    if (Calendar.getInstance().getTime().before(endDate)) {
//                        toolbarTitle.setText(R.string.live_class_transcribe);
//                    } else {
//                        toolbarTitle.setText(R.string.class_transcribe_history);
//                    }

                    connectSocket();
                    loadingLayout.setVisibility(View.GONE);
                    toolbarTitle.setVisibility(View.VISIBLE);
                    scrollViewMain.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(int errorCode, String errorReason) {
                Log.e("Error", errorReason);
            }
        });
    }

    private void connectSocket() {
        try {
            socket = IO.socket("https://adab2.bearcats.dev");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        socket.connect();

        while (!socket.connected()) {
            Log.d("Socket.io", "connecting...");
        }

        // ganti "1" jadi transactionId tapi convert ke string pake String.valueOf(transactionId)
        socket.emit("join_room", String.valueOf(sessionId));

        if (socket.connected()) {
            AtomicReference<Boolean> currentlyTalking = new AtomicReference<>(false);

            Log.d("Socket.io", "oke bang sudah konek from lecturer");
            contentLoadingLayout.setVisibility(View.GONE);

            if (canTalk == 1) {
                // speech recognizer for lecturer
                layoutButtons.setVisibility(View.VISIBLE);
                toolbarTitle.setText(R.string.live_class_transcribe);
                checkPermission();
                final SpeechRecognizer mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                final Intent mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, courseLanguage);

                mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle bundle) {

                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        socket.emit("start_talking");
                    }

                    @Override
                    public void onRmsChanged(float v) {

                    }

                    @Override
                    public void onBufferReceived(byte[] bytes) {

                    }

                    @Override
                    public void onEndOfSpeech() {
                        socket.emit("stop_talking");
                    }

                    @Override
                    public void onError(int i) {
                        // listening again
                        if (currentlyTalking.get()) {
                            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                        }
                    }

                    @Override
                    public void onResults(Bundle bundle) {
                        if (singleResult){
                            //getting all the matches
                            ArrayList<String> matches = bundle
                                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                            //displaying the first match
                            if (matches != null)
                                socket.emit("message", matches.get(0));

                            // listening again
                            if (currentlyTalking.get()) {
                                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                            }
                            singleResult = false;
                        }

                        //after 100 ms, set back singleResult to true
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                singleResult = true;
                            }
                        },100);
                    }

                    @Override
                    public void onPartialResults(Bundle bundle) {

                    }

                    @Override
                    public void onEvent(int i, Bundle bundle) {

                    }
                });

                talkButton.setOnClickListener(v -> {
                    if (!currentlyTalking.get()) {
                        currentlyTalking.set(true);
                        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                        talkButton.setText("Stop Talking");

                        // don't let the device go to sleep
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        currentlyTalking.set(false);
                        mSpeechRecognizer.stopListening();
                        talkButton.setText("Start Talking");

                        // let the device go to sleep
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            }else{
                toolbarTitle.setText(R.string.class_transcribe_history);
            }


        } else {
            Log.d("Socket.io", "error");
        }

        socket.on("message", args -> {
            runOnUiThread(() -> {
                textContent.append(args[0].toString() + " ");
                scrollViewMain.fullScroll(View.FOCUS_DOWN);
                Log.d("message",args[0].toString());
            });
        });

        socket.on("start_talking", args -> {
            runOnUiThread(() -> textLiveNow.setVisibility(View.VISIBLE));
        });

        socket.on("stop_talking", args -> {
            runOnUiThread(() -> textLiveNow.setVisibility(View.INVISIBLE));
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(ActivityLive.this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(ActivityLive.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(ActivityLive.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            69);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                // Permission has already been granted
            }
        }
    }

    private void setHighConstrastTheme() {
        getTheme().applyStyle(R.style.Theme_Adab_HighContrast,true);
        toolbar.setBackgroundTintList(getResources().getColorStateList(android.R.color.black));
        findViewById(R.id.live_session_background).setBackgroundColor(getResources().getColor(android.R.color.black));
        courseTitle.setTextColor(getResources().getColorStateList(R.color.buttonColor));
        textContent.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void setTextSize(){
        //multiple of text size
        float textSize = userPreferences.getTextSize();
        className.setTextSize(TypedValue.COMPLEX_UNIT_PX, className.getTextSize() * textSize);
        classSession.setTextSize(TypedValue.COMPLEX_UNIT_PX, classSession.getTextSize() * textSize);
        textContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, textContent.getTextSize() * textSize);
        textLiveNow.setTextSize(TypedValue.COMPLEX_UNIT_PX, textLiveNow.getTextSize() * textSize);
        courseTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, courseTitle.getTextSize() * textSize);
        toolbarTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, toolbarTitle.getTextSize() * textSize);
    }

    private void setTextTypeface(){
        //get font type
        Typeface textTypeface = userPreferences.getTextTypeface();
        //set font type for each text view
        className.setTypeface(textTypeface, className.getTypeface().getStyle());
        classSession.setTypeface(textTypeface, classSession.getTypeface().getStyle());
        textContent.setTypeface(textTypeface, textContent.getTypeface().getStyle());
        textLiveNow.setTypeface(textTypeface, textLiveNow.getTypeface().getStyle());
        courseTitle.setTypeface(textTypeface, courseTitle.getTypeface().getStyle());
        toolbarTitle.setTypeface(textTypeface, toolbarTitle.getTypeface().getStyle());
    }
}
