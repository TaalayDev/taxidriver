package kg.dos2.taxidriver;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_NEWS;
import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;
import static kg.dos2.taxidriver.TaxiActivity.STATE_CARRIAGE_CLIENT_FROM_STREET;

public class NewsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private FragmentsListener callBack;
    LinearLayout reportslay;
    DataBaseHelper dbhelper;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public NewsFragment() {
        // Required empty public constructor
    }

    public static NewsFragment newInstance(String param1, String param2) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        callBack.curFrag(FRAGMENT_NEWS);
        reportslay = view.findViewById(R.id.reportLay);
        dbhelper = new DataBaseHelper(getContext());
        openChat();

        return view;
    }

    public void showReport(Bundle bundle) {
        View view = getLayoutInflater().inflate(R.layout.newslay, reportslay, false);
        TextView nordtxt = view.findViewById(R.id.textaddr1);
        view.findViewById(R.id.lay2).setVisibility(View.GONE);
        view.findViewById(R.id.divider6).setVisibility(View.GONE);
        view.findViewById(R.id.lay3).setVisibility(View.GONE);
        view.findViewById(R.id.divider7).setVisibility(View.GONE);
        if ( bundle.getInt("state") != STATE_CARRIAGE_CLIENT_FROM_STREET ) {
            nordtxt.setText(bundle.getString("addr1"));
            String addr2 = bundle.getString("addr2");
            if (!addr2.isEmpty() || !addr2.equals("")) {
                view.findViewById(R.id.lay2).setVisibility(View.VISIBLE);
                view.findViewById(R.id.divider6).setVisibility(View.VISIBLE);
                TextView nordaddr2 = view.findViewById(R.id.nwordaddr2);
                nordaddr2.setText(addr2);
            }
            if (!bundle.getString("desc").isEmpty() || !bundle.getString("desc").equals("")) {
                view.findViewById(R.id.lay3).setVisibility(View.VISIBLE);
                view.findViewById(R.id.divider7).setVisibility(View.VISIBLE);
                TextView norddesc = view.findViewById(R.id.textdesc);
                norddesc.setText(bundle.getString("desc"));
            }
        } else {
            nordtxt.setText("С улицы");
        }
        TextView nwordcat = view.findViewById(R.id.textcat);
        TextView textdist = view.findViewById(R.id.textdist);
        nwordcat.setText(bundle.getString("date"));
        textdist.setText(bundle.getFloat("dist") + " км");
        if (bundle.getInt("cost") != 0) {
            TextView nordp = view.findViewById(R.id.textprice);
            nordp.setText(bundle.getInt("cost") + " C");
        }
        reportslay.addView(view);
    }

    public void openChat()
    {
        try {
            reportslay.removeAllViews();
            Cursor cursor = dbhelper.getReadableDatabase()
                    .rawQuery("SELECT * FROM `reports`", null);
            Log.d(LOG_TAG, "newsfragment openchat " + cursor.getCount());
            while (cursor.moveToNext()) {
                Bundle bundle = new Bundle();
                bundle.putInt("id", cursor.getInt(cursor.getColumnIndex("id")));
                bundle.putString("addr1", cursor.getString(cursor.getColumnIndex("addr1")));
                bundle.putString("addr2", cursor.getString(cursor.getColumnIndex("addr2")));
                bundle.putString("date", cursor.getString(cursor.getColumnIndex("date")));
                bundle.putString("desc", cursor.getString(cursor.getColumnIndex("desc")));
                bundle.putString("phone", cursor.getString(cursor.getColumnIndex("phone")));
                bundle.putString("addr1", cursor.getString(cursor.getColumnIndex("addr1")));
                bundle.putInt("state", cursor.getInt(cursor.getColumnIndex("state")));
                bundle.putFloat("dist", cursor.getFloat(cursor.getColumnIndex("dist")));
                bundle.putInt("cost", cursor.getInt(cursor.getColumnIndex("cost")));
                showReport(bundle);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "newsfragment openchatexception " + e.getMessage());
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
