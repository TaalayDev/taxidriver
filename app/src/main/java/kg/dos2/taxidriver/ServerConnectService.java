package kg.dos2.taxidriver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS;
import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;

public class ServerConnectService extends Service {

    // private static Socket sock, talkieWalkieSock, AudioSock;
    PendingIntent pend;
    /*private static InputStreamReader input, talkieWalkieInput;
    private static BufferedOutputStream output;
    private static DataOutputStream talkieWalkieOutput;

    private static volatile Thread thread;
    private static volatile Thread talkieWalkieThread;

    static volatile String ip = "";*/

    public static final String CHANNEL_ID = "kg.dos2.taxidriver";
    private static volatile boolean flag = true;
    private static Context context;
    // DriverThread dth;

    private Firebase fdb;
    private StorageReference storageReference;


    public ServerConnectService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.chanel_name);
            String description = getString(R.string.chanel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onCreate() {
        context = this;
    }

    ChildEventListener ch = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            if (dataSnapshot.getKey().equals("state")) {
                Log.i(LOG_TAG, "getState: " + dataSnapshot.getValue().toString());
            } else if (dataSnapshot.getKey().equals("id")) {

            }
            request(1);
            try {
                pend.send(context, 3, new Intent().putExtra("list", s));
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        if ( intent != null ) {
            pend = (PendingIntent) intent.getParcelableExtra("pIntent");
        }
        Firebase.setAndroidContext(this);
        storageReference = FirebaseStorage.getInstance().getReference();
        fdb = new Firebase("https://dos-f9894.firebaseio.com/order");
        fdb.addChildEventListener(ch);
        new Firebase("https://dos-f9894.firebaseio.com/tw")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        if ( !Prefs.getBooleanDef(ServerConnectService.this, Prefs.RATSIA, true) ) {
                            /*Map<String, Object> week_scores = new HashMap<String, Object>();
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                week_scores.put(userSnapshot.child("username").getValue() + "/week_score", 0);
                            }
                            mRootRef.child("users").updateChildren(week_scores);
                            mWeekResetRef.setValue(true);*/
                            downloadFile("uploads/twf.mp3");
                        }
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });

        return super.onStartCommand(intent,flags,startID);
    }

    public void request(final int code) {
        String url = URL_GET_ACTIVE_ORDERS;

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    if ( code == 1 ) {
                        try {
                            JSONArray arr = new JSONArray(s);
                            Log.e("myimag", "scs 1 - " + s);
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                Order order = new Order();
                                int id = obj.getInt("ID");
                                int state = obj.getInt("State");
                                int drv = obj.getInt("Driver");
                                if ( drv == Prefs.getIntDef(ServerConnectService.this, Prefs.DRIVER_ID, 0) ) {
                                    pend.send(context, 4, new Intent().putExtra("order", obj.toString()));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                        // pend.send(context, 2, new Intent().putExtra("active_order_list", s));
                    }
                } catch (Exception e) {
                    Log.e("myimag", "catException " + e);
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("id", "" + 0);
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);
    }

    public void downloadFile(String url) {

        final StorageReference fileRef = storageReference.child(url);

        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/lastGetTalkieWalkieRecordFile.mp3");

        if ( f.exists() ) {
            f.delete();
        }
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileRef.getFile(f).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {

            }
        }).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    play();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(LOG_TAG, "fileDownloadFailure: " + e);
            }
        });
    }

    public static void send(String m) throws IOException {
        /*final String mm = m;
        final DataOutputStream dos = new DataOutputStream(DriverThread.getOutputStream());
        if ( dos != null ) {
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        dos.writeUTF(mm);
                        Log.i("mytaxisendToServer", "" + mm);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            th.start();
        }*/
    }

    public static void talkieWalkie(int i) {
        /*try {
            talkieWalkieOutput.writeInt(0);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public static void sendAudio() {
        /*File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/Messages/Audio/lastSendAudioRecordFile.3gp");
        final int musicLength = (int)(file.length());
        final byte[] music = new byte[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                if ( i < musicLength ) {
                    music[i] = dis.readByte();
                    i++;
                } else break;
            }
            dis.close();
        } catch (Throwable t) {
            Log.e("mytaxiT","Throwable " + t);
        }
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DataOutputStream dout = new DataOutputStream(DriverThread.getOutputStream());
                    if ( dout != null ) {
                        Log.d("mytaxiAudioSend", "length = " + musicLength);
                        dout.writeInt(musicLength);
                        dout.write(music);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();*/
    }

    public static void talkieWalkie() {
        /*File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AutoTaxi/lastSendTalkieWalkieRecordFile.3gp");
        final int musicLength = (int)(file.length());
        final byte[] music = new byte[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                if ( i < musicLength ) {
                    music[i] = dis.readByte();
                    i++;
                } else break;
            }
            dis.close();
        } catch (Throwable t) {
            Log.e("mytaxiT","Throwable " + t);
        }
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if ( talkieWalkieOutput != null ) {
                        talkieWalkieOutput.writeInt(musicLength);
                        talkieWalkieOutput.write(music);
                        Log.d("mytaxiTalkieWalkie", "length = " + musicLength);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();*/
    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    public void play() {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/AutoTaxi/lastGetTalkieWalkieRecordFile.mp3");
        final MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            FileInputStream fis = new FileInputStream(f);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
