package kg.dos2.taxidriver;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS;
import static kg.dos2.taxidriver.Prefs.URL_GET_DISP_LIST;
import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_ORDER_LIST;
import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_SOS;
import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;

public class SOSFragment extends Fragment {

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

    private ArrayList<Disp> dispList = new ArrayList<>();
    private Firebase f;
    TextView sosInfoText;
    Button btn_call_disp = null, btn_call_drv = null;

    int flag = 0, arg_id = 0, arg_drv_id = 0, arg_state = 0;
    double arg_lat = 0, arg_lng = 0;
    String arg_phone = "0";

    public SOSFragment() {
        // Required empty public constructor
    }

    public void request(final int code) {
        String url = URL_GET_DISP_LIST;

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String s) {
                try {
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
                } catch (Exception e) {
                    Log.e(LOG_TAG, "sosfrag exc " + e);
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
                params.put("id", "0");
                params.put("city", String.valueOf(Prefs.getIntDef(getContext(), Prefs.CITY, 1)));
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

    private FragmentsListener callBack;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public static SOSFragment newInstance(Bundle bundle) {
        SOSFragment fragment = new SOSFragment();
        Bundle args = bundle;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            flag = 1;
            arg_id = getArguments().getInt("id");
            arg_drv_id = getArguments().getInt("dvid");
            arg_state = getArguments().getInt("state");
            arg_lat = getArguments().getDouble("lat");
            arg_lng = getArguments().getDouble("lng");
            arg_phone = getArguments().getString("phone");
        }
    }

    ChildEventListener ch = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            try {
                int val = Integer.parseInt(dataSnapshot.getValue().toString());
                if ( val == 0 ) {
                    if ( flag != 1 )
                        sosInfoText.setText("Ваш сигнал тревоги был отменен");
                    else sosInfoText.setText("Сигнал тревоги был отменен");
                } else if ( val == -1 ) {

                }
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(500);
                }
            } catch (Exception e) {
                
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_so, container, false);
        callBack.curFrag(FRAGMENT_SOS);

        f = new Firebase("https://dos-f9894.firebaseio.com/warn");
        f.addChildEventListener(ch);
        request(0);
        btn_call_disp = view.findViewById(R.id.btn_cll_disp);
        btn_call_disp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
                        if ( i < dispList.size() ) {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dispList.get(i).phone));
                            startActivity(intent);
                        }
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(lvSimple);
                builder.show();
            }
        });
        sosInfoText = view.findViewById(R.id.sosInfoText);
        btn_call_drv = view.findViewById(R.id.btn_cll_drv);
        if ( flag == 1 ) {
            sosInfoText.setText("Позывной #" + arg_drv_id + " послал сигнал тревоги!");
            btn_call_drv.setVisibility(View.VISIBLE);
            btn_call_drv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + arg_phone));
                    startActivity(intent);
                }
            });
            replaceFragment(MapFragment.newInstance(arg_lat, arg_lng, 1));
        }

        return view;
    }

    public void replaceFragment(Fragment fragment) {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        f.removeEventListener(ch);
    }
}
