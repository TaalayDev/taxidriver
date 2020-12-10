package kg.dos2.taxidriver;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DriverThread extends Thread {
    PendingIntent pend;
    Context context;
    public static Socket sock;
    int audioState;

    private SQLiteDatabase sqlDB;

    DriverThread(Context context, PendingIntent pend, SQLiteDatabase sql) {
        this.pend = pend;
        this.context = context;
        sqlDB = sql;
    }

    public void run() {
        try {
            sock = new Socket("192.168.42.26", 1204);
            Log.v("mytaxiservice", "connect to server");
            while ( true ) {
                try {
                    receive();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
            //Log.v("mytestservice", "receive");
        } catch (UnknownHostException e) {
            Log.v("mytaxiexception", "UHException " + e);
        } catch (IOException e) {
            Log.v("mytaxiexception", "IOException " + e);
            e.printStackTrace();
        }
    }

    public void receive() throws IOException, PendingIntent.CanceledException {
        if ( audioState != 0 ) {
            saveAudio(audioState);
        } else {
            DataInputStream dis = new DataInputStream(sock.getInputStream());
            String m = dis.readUTF();//new BufferedReader(input).readLine();
            if (m == null)
                return;
            parseResult(m);
            // Log.i("mytaxi1", "" + m);
            pend.send(context, 1, new Intent().putExtra("receive", m));
            // MainActivity.parseFromServer(m);
            Log.v("mytaxireceiveFromServer", "" + m);
        }
    }

    public void parseResult(String result) {
        ContentValues values = new ContentValues();
        //View view;
        if (result.contains("MyAudioMsg")) {
            result = result.replace("MyAudioMsg ", "");
            if (result.contains("toDispatcher")) {
                audioState = 1;
                return;
            } else if (result.contains("toClient")) {
                audioState = 2;
                return;
            }
        } else if ( result.contains("audioFromClient") ) {
            audioState = 3;
        } else
        if (result.equals("connected")) {

        } else
        if (result.contains("Message")) {
            result = result.replace("Message ", "");
            /*//view = inflater.inflate(R.layout.getmess, messagesLayout, false);
            //((TextView) view.findViewById(R.id.get_message_text)).setText(result);
            values.put("msg_to", "toMe");
            values.put("msg_text", result);
            values.put("msg_from", "Client");
            sqlDB.insert("messages", null, values);
            //messagesLayout.addView(view);*/
        } else if (result.contains("MyMsg")) {
            result = result.replace("MyMsg ", "");
            if (result.contains("toClient")) {
                result = result.replace("toClient ", "");
                /*values.put("msg_to", "toMe");
                values.put("msg_text", result);
                values.put("msg_from", "Me");
                values.put("which", "client");
                values.put("isAudio", "noaudio");
                values.put("audioPath", "null");
                sqlDB.insert("messages", null, values);*/
            } else if (result.contains("toDispatcher")) {
                result = result.replace("toDispatcher ", "");
                /*values.put("msg_to", "toMe");
                values.put("msg_text", result);
                values.put("msg_from", "Me");
                values.put("which", "dispatcher");
                values.put("isAudio", "noaudio");
                values.put("audioPath", "null");
                sqlDB.insert("messages", null, values);*/
            } else {
                /*values.put("msg_to", "toMe");
                values.put("msg_text", result);
                values.put("msg_from", "Me");
                values.put("which", "drivers");
                values.put("isAudio", "noaudio");
                values.put("audioPath", "null");
                sqlDB.insert("messages", null, values);*/
            }
            /*view = inflater.inflate(R.layout.sendmess, messagesLayout, false);
            ((TextView) view.findViewById(R.id.send_message_text)).setText(result);
            messagesLayout.addView(view);*/
        } else if (result.contains("setState")) {
            result = result.replace("setState ", "");
            //updateState(result);
        } else if (result.startsWith("order")) {
            result = result.replace("order ", "");
            /*orderNum = Integer.parseInt(result.split(" ")[0]);
            result = result.replace(result.split(" ")[0] + " ", "");
            view = inflater.inflate(R.layout.neworder, orderl, false);
            ((TextView) view.findViewById(R.id.newordertext)).setText("" + result);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    send("syncToOrder " + orderNum);
                }
            });
            orderl.addView(view);*/
        } else if (result.equals("allowToTakeOrderFromStreet")) {
            /*buttonOrderStreet.setText("КЛИЕНТ");
            buttonOrderStreet.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.client), null, null);
            buttonOrderStreet.setEnabled(false);
            orderList.setVisibility(View.GONE);
            taksometer.setVisibility(View.VISIBLE);
            mapLayout.setVisibility(View.VISIBLE);*/
        } else if (result.equals("synced")) {
            //visibleSettings(5);
        } else if (result.equals("clientGetDriverArrive")) {
            /*visibleSettings(4);
            buttonOrderStreet.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.client), null, null);
            buttonOrderStreet.setText("КЛИЕНТ");*/
        }
    }

    public void saveAudio(int z) {
        int result;
        ContentValues values = new ContentValues();
        try {
            Log.i("mytaxisaveaud","saveAudio(" + z +")");
            DataInputStream dinp = new DataInputStream(sock.getInputStream());
            if ( dinp != null ) {
                result = dinp.readInt();
                byte[] m = new byte[result];
                dinp.readFully(m, 0, m.length);
                Log.v("mytaxiresult", "length: " + result);
                if (result > 0) {
                    File file = null;
                    String name = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
                    file = new File(Environment.getExternalStorageDirectory() + "/AutoTaxi/Messages/Audio/" + name + ".3gp");
                    if (file.exists())
                        file.delete();

                    file.createNewFile();

                    OutputStream os = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(os);
                    DataOutputStream dos = new DataOutputStream(bos);
                    dos.write(m, 0, m.length);
                    dos.close();

                    values.put("msg_to", "toMe");
                    values.put("msg_text", result);
                    if ( z != 3 && z != 4 )
                        values.put("msg_from", "Me");
                    else if ( z == 3 )
                        values.put("msg_from", "Client");
                    else if ( z == 4 )
                        values.put("msg_from", "Dispatcher");
                    if ( z == 2 || z == 3 )
                        values.put("which", "client");
                    else if ( z == 1 || z == 4 )
                        values.put("which", "dispatcher");
                    values.put("isAudio", "audio");
                    values.put("audioPath","" + Environment.getExternalStorageDirectory() + "/AutoTaxi/Messages/Audio/" + name + ".3gp");
                    sqlDB.insert("messages", null, values);
                    try {
                        if ( z == 1 || z == 4 )
                            pend.send(context, 1, new Intent().putExtra("receive", "newMyAudioMsg1"));
                        else if ( z == 2 || z == 3 )
                            pend.send(context, 1, new Intent().putExtra("receive", "newMyAudioMsg2"));
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                    Log.v("mytaxiSaveAudio", "save");
                    // TaxiActivity.getAudioMessage = 0;
                    audioState = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static OutputStream getOutputStream() throws IOException {
        return sock.getOutputStream();
    }

}
