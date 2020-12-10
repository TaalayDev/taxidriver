package kg.dos2.taxidriver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS;
import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS_BY_CATEGORY;
import static kg.dos2.taxidriver.Prefs.URL_GET_CATEGORIES;
import static kg.dos2.taxidriver.Prefs.URL_GET_DISP_LIST;
import static kg.dos2.taxidriver.Prefs.URL_SEND_MESS;
import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_CHAT;
import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;

public class ChatFragment extends BottomSheetDialogFragment {

    // имена атрибутов для Map
    final String ATTRIBUTE_NAME_TEXT = "text";

    class Disp {
        int id = 0;
        String login = "";
        String fname = "";
        String lname = "";
        String phone = "";
        String email = "";
        Disp() { }
        Disp(int id, String login, String fname, String lname, String phone, String email) {
            this.id = id;
            this.login = login;
            this.fname = fname;
            this.lname = lname;
            this.phone = phone;
            this.email = email;
        }
    }

    private String mParam1;
    private String mParam2;
    private FragmentsListener callBack;
    private LinearLayout messagesLayout;
    private EditText editMessageText;
    private FrameLayout sendMessageButton;
    private FloatingActionButton fabCall;
    NestedScrollView chatScroll;
    private DataBaseHelper dbhelper;

    private ArrayList<Disp> dispList = new ArrayList<>();

    private int code = 0;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(FragmentsListener callBack, Bundle args) {
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        fragment.setCallBack(callBack);
        return fragment;
    }

    public void request(int code, int getter, String mess) {
        String url = URL_SEND_MESS;
        if ( code == 1 ) url = URL_GET_DISP_LIST;
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
                    Log.e(LOG_TAG, "chat request code " + code + " " + s);
                    if (code == 2) {
                        JSONArray arr = new JSONArray(s);
                        Log.e("myimag", "products size - " + arr.length());
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            final int idd = obj.getInt("id");
                            String login = obj.getString("login");
                            String fname = obj.getString("fname");
                            String lname = obj.getString("lname");
                            String phone = obj.getString("phone");
                            String email = obj.getString("email");
                            Disp disp = new Disp(idd, login, fname, lname, phone, email);
                            dispList.add(disp);
                        }
                    }
                } catch (JSONException jse) {
                    Log.e(LOG_TAG, "chfr json exc " + jse.getMessage());
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("myimag", "VolleyError: " + volleyError);
                request(code, getter, mess);
            }
        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("city", String.valueOf(Prefs.getIntDef(getContext(), Prefs.CITY, 1)));
                if ( code == 0 || code == 5 ) {
                    params.put("code", "" + code);
                    params.put("getter", "" + getter);
                    params.put("sender", "" + Prefs.getIntDef(getContext(), Prefs.DRIVER_ID, 0));
                    params.put("text", mess);
                } else if ( code == 1 ) {
                    params.put("id", "0");
                }
                return params;
            }

            @Override
            public Priority getPriority() {
                return Priority.IMMEDIATE;
            }
        };
        RequestQueue rQueue = Volley.newRequestQueue(getContext());
        rQueue.add(request);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            code = getArguments().getInt("code", 0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        callBack.curFrag(FRAGMENT_CHAT);
        ImageView close = view.findViewById(R.id.chat_dialog_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatFragment.this.dismiss();
            }
        });
        messagesLayout = view.findViewById(R.id.messagesLay);
        chatScroll = view.findViewById(R.id.chat2Scroll);

        fabCall = view.findViewById(R.id.fab_call);
        fabCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( code == 0 ) {
                    // упаковываем данные в понятную для адаптера структуру
                    ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>(
                            dispList.size());
                    Map<String, Object> m;
                    for (int i = 0; i < dispList.size(); i++) {
                        m = new HashMap<String, Object>();
                        m.put(ATTRIBUTE_NAME_TEXT, dispList.get(i).fname + " " + dispList.get(i).lname);
                        data.add(m);
                    }

                    // массив имен атрибутов, из которых будут читаться данные
                    String[] from = { ATTRIBUTE_NAME_TEXT };
                    // массив ID View-компонентов, в которые будут вставлять данные
                    int[] to = { R.id.textView10 };

                    // создаем адаптер
                    SimpleAdapter sAdapter = new SimpleAdapter(getContext(), data, R.layout.disp_list_item,
                            from, to);

                    // определяем список и присваиваем ему адаптер
                    ListView lvSimple = new ListView(getContext());
                    lvSimple.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    lvSimple.setAdapter(sAdapter);
                    lvSimple.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (i < dispList.size()) {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dispList.get(i).phone));
                                startActivity(intent);
                            }
                        }
                    });
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setView(lvSimple);
                    builder.show();
                } else {
                    String number = Order.current.getPhone();
                    Uri call = Uri.parse("tel:" + number);
                    Intent surf = new Intent(Intent.ACTION_DIAL, call);
                    startActivity(surf);
                }
            }
        });
        editMessageText = view.findViewById(R.id.edit_message_text);
        sendMessageButton = view.findViewById(R.id.send_msg_view);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mess = editMessageText.getText().toString();
                if ( !mess.isEmpty() ) {
                    int getter = 0;
                    if ( code != 0 ) {
                        if ( Order.current != null )
                            if ( Order.current.getClientId() > 0 )
                                getter = Order.current.getClientId();
                    }
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", 0);
                    bundle.putInt("mCode", code);
                    bundle.putInt("getter", 0);
                    bundle.putInt("sender", Prefs.getIntDef(getContext(), Prefs.DRIVER_ID, 0));
                    bundle.putString("getterLogin", "");
                    bundle.putString("senderLogin", Prefs.getString(getContext(), Prefs.DRIVER));
                    bundle.putString("message", mess);
                    bundle.putString("date", "");
                    bundle.putInt("getterIsRead", 0);
                    request(code, getter, mess);
                    editMessageText.setText("");
                    hideKeyboard(editMessageText);
                    showMessage(bundle);
                    dbhelper.insertMessage(bundle);
                } else {
                    Toast.makeText(getContext(), "Введите текст сообщения!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dbhelper = new DataBaseHelper(getContext());
        openChat(code);

        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });

        TextView chat_with = view.findViewById(R.id.tv_chat_with);
        if ( code != 0 ) {
            chat_with.setText("Чат с клиентом");
        }
        request(1, 0,"");
        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        callBack.dialogDismiss();
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm =  (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public void showMessage(Bundle bundle) {
        if ( bundle.getInt("mCode") == 1 || bundle.getInt("mCode") == 4 ) {
            View view = getLayoutInflater().inflate(R.layout.getmess, messagesLayout, false);
            TextView tv = (TextView) view.findViewById(R.id.get_message_text);
            tv.setText(bundle.getString("message"));
            messagesLayout.addView(view);
        } else if ( bundle.getInt("mCode") == code ) {
            View view = getLayoutInflater().inflate(R.layout.sendmess, messagesLayout, false);
            TextView tv = (TextView) view.findViewById(R.id.send_message_text);
            tv.setText(bundle.getString("message"));
            messagesLayout.addView(view);
        }
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    int diffCode(int code) {
        switch (code) {
            case 0:
                return 1;
            case 5:
                return 4;
            default:
                return 0;
        }
    }

    public void openChat(int code)
    {
        try {
            messagesLayout.removeAllViews();
            String query = "SELECT * FROM `chat` WHERE (`mCode` = " + code + " OR `mCode` = " + diffCode(code) + " )";
            if ( code != 0 ) {
                int client = Order.current.getClientId();
                // query += " AND ( `Sender` = " + client + " OR `Getter` = " + client + " )";
            }
            Log.e(LOG_TAG, "openChat code " + code + "; " + query);
            Cursor cursor = dbhelper.getReadableDatabase().rawQuery(query, null);
            while (cursor.moveToNext()) {
                Bundle bundle = new Bundle();
                bundle.putInt("id", cursor.getInt(cursor.getColumnIndex("id")));
                bundle.putInt("mCode", cursor.getInt(cursor.getColumnIndex("mCode")));
                bundle.putInt("sender", cursor.getInt(cursor.getColumnIndex("sender")));
                bundle.putInt("getter", cursor.getInt(cursor.getColumnIndex("getter")));
                bundle.putString("senderLogin", cursor.getString(cursor.getColumnIndex("senderLogin")));
                bundle.putString("getterLogin", cursor.getString(cursor.getColumnIndex("getterLogin")));
                bundle.putString("message", cursor.getString(cursor.getColumnIndex("message")));
                bundle.putString("date", cursor.getString(cursor.getColumnIndex("date")));
                bundle.putInt("getterIsRead", cursor.getInt(cursor.getColumnIndex("getterIsRead")));
                showMessage(bundle);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "openChat(" + code + "):Exception " + e);
        }
    }

    public boolean onBackPressed() {
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

}
