package kg.dos2.taxidriver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS;
import static kg.dos2.taxidriver.Prefs.URL_GET_ACTIVE_ORDERS_BY_CATEGORY;
import static kg.dos2.taxidriver.Prefs.URL_GET_CATEGORIES;
import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_ORDER_LIST;
import static kg.dos2.taxidriver.TaxiActivity.LOG_TAG;

public class OrdersListFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    Timer timer = new Timer();

    ArrayList<Order> ordersList = new ArrayList<>();
    ProgressBar pbar;
    String category = "---";

    class Category {
        int id = 0;
        String name = "";
        int ordCount = 0;
        int parent = 0;
        Category() { }
        Category(int id, String name, int ordCount, int parent) {
            this.id = id;
            this.name = name;
            this.ordCount = ordCount;
            this.parent = parent;
        }
    }

    private LinearLayout orderListLay;

    private FragmentsListener callBack;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public OrdersListFragment() {
        // Required empty public constructor
    }

    public static OrdersListFragment newInstance(String param1, String param2) {
        OrdersListFragment fragment = new OrdersListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if ( getActivity() != null )
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Date d = new Date();
                        if (((d.getSeconds() % 5) == 0)) {
                            request(2, 0);
                        }
                    }
                });
            }
        }, 10, 1000L);
    }

    @Override
    public void onResume() {
        super.onResume();
        startTimer();
        Log.e(LOG_TAG, "OrderListFragment onResume");
        // request(2, 0);
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
        View view = inflater.inflate(R.layout.fragment_orders_list, container, false);
        orderListLay = view.findViewById(R.id.orderl);
        pbar = view.findViewById(R.id.progressBar);
        callBack.curFrag(FRAGMENT_ORDER_LIST);
        if ( !(Prefs.getIntDef(getContext(), Prefs.DPAYD, 1) > 0) ||
                !(Prefs.getIntDef(getContext(), Prefs.BALANCE, 0) > 0)  ) {
            ScrollView sv = view.findViewById(R.id.sv);
            sv.setVisibility(View.GONE);
            pbar.setVisibility(View.GONE);
            LinearLayout nballay = view.findViewById(R.id.err_no_balance_lay);
            nballay.setVisibility(View.VISIBLE);
        }
        // startTimer();
        return view;
    }

    void balanceChanged() {
        ScrollView sv = getView().findViewById(R.id.sv);
        LinearLayout nballay = getView().findViewById(R.id.err_no_balance_lay);
        if ( !(Prefs.getIntDef(getContext(), Prefs.DPAYD, 1) > 0) ||
                !(Prefs.getIntDef(getContext(), Prefs.BALANCE, 0) > 0)  ) {
            sv.setVisibility(View.GONE);
            pbar.setVisibility(View.GONE);
            nballay.setVisibility(View.VISIBLE);
        } else {
            if ( sv.getVisibility() == View.GONE ) {
                sv.setVisibility(View.VISIBLE);
                nballay.setVisibility(View.GONE);
            }
        }
    }

    public void ordersChanged() {
        if ( Prefs.getIntDef(getContext(), Prefs.DPAYD, 1) > 0 &&
                Prefs.getIntDef(getContext(), Prefs.BALANCE, 0) > 0  ) {
            request(2, 0);
        }
    }

    private void clearList() {
        ordersList.clear();
        orderListLay.removeAllViews();
    }

    public void addOrder(Order order) {
        ordersList.add(order);
        View view = getLayoutInflater().inflate(R.layout.neworder, orderListLay, false);
        TextView nordtxt = view.findViewById(R.id.nwordaddr1);
        nordtxt.setText(order.getAddr1());

        if ( order.getAddr2().isEmpty() || order.getAddr2().equals("") ) {
            view.findViewById(R.id.nwordlay2).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.nwordlay2).setVisibility(View.VISIBLE);
            TextView nordaddr2 = view.findViewById(R.id.nwordaddr2);
            nordaddr2.setText(order.getAddr2());
        }
        if ( !order.getDate().isEmpty() ) {
            TextView nordtime = view.findViewById(R.id.nwordtime);
            nordtime.setText(order.getDate());
        }
        if ( order.getDesc().isEmpty() || order.getDesc().equals("") ) {

        }
        if ( order.getCost() == 0 ) {
            view.findViewById(R.id.nwordprice).setVisibility(View.GONE);
        } else {
            TextView nordp = view.findViewById(R.id.nwordprice);
            nordp.setText(order.getCost() + " C");
        }
        TextView nwordcat = view.findViewById(R.id.nwordcat);
        nwordcat.setText(category);

        view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OrderInfoFragment.newInstance(callBack, order)
                        .show(getChildFragmentManager(), "OrderInfoFragment");
            }
        });
        view.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Взять заказ?");
                builder.setMessage("Вы действительно хотите взять этот заказ?");
                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callBack.tryConnectToOrder(order.getId());
                    }
                });
                builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();

            }
        });

        view.setTag("order_"+order.getId()+"_view");
        orderListLay.addView(view);
    }

    public void addCat(Category cat) {
        View view = getLayoutInflater().inflate(R.layout.lay_cat_item, orderListLay, false);
        TextView cattext = view.findViewById(R.id.tv_cat_text);
        cattext.setText(cat.name);

        if (cat.ordCount == 0) {
            view.findViewById(R.id.ord_count).setVisibility(View.GONE);
        } else {
            TextView tvOrdCount = view.findViewById(R.id.ord_count);
            tvOrdCount.setVisibility(View.VISIBLE);
            tvOrdCount.setText(String.format(Locale.getDefault(), "%d", cat.ordCount));
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                category = cat.name;
                request(2, cat.id);
                // lcatid = cat.id;
            }
        });

        orderListLay.addView(view);
    }

    private boolean isOrdersListContains(int orderid) {
        for ( int i = 0; i < ordersList.size(); i++ ) {
            if ( ordersList.get(i).getId() == orderid )
                return true;
        }
        return false;
    }

    private boolean isArrCont(JSONArray arr, int orderid) throws JSONException {
        for (int i = 0; i < arr.length(); i++) {
            if ( arr.getJSONObject(i).getInt("ID") == orderid )
                return true;
        }
        return false;
    }

    private void checkOrdersList(JSONArray arr) {
        for ( int i = 0; i < ordersList.size(); i++ ) {
            try {
                int orderid = ordersList.get(i).getId();
                if ( !isArrCont(arr, orderid) ) {
                    Log.e(LOG_TAG, "!isArrCont(" + orderid + ")");
                    orderListLay.removeView(orderListLay.findViewWithTag("order_" + orderid + "_view"));
                    ordersList.remove(i);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void request(final int code, final int temp) {
        String url = URL_GET_ACTIVE_ORDERS;
        if ( ordersList.size() == 0 )
            pbar.setVisibility(View.VISIBLE);
        if ( getContext() != null )
        Volley.newRequestQueue(getContext())
                .add(new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        try {
                            // clearList();
                            if ( pbar.getVisibility() == View.VISIBLE )
                                pbar.setVisibility(View.GONE);
                            if ( code == 0 || code == 2 ) {
                                // st = 1;
                                JSONArray arr = new JSONArray(s);
                                Log.e(LOG_TAG, "products size - " + arr.length());
                                Log.e(LOG_TAG, "products - " + s);
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject obj = arr.getJSONObject(i);
                                    if ( obj.getInt("Driver") == 0 ) {
                                        int id = obj.getInt("ID");
                                        if ( !isOrdersListContains(id) ) {
                                            Log.e(LOG_TAG, "!isOrdersListContains(" + id + ")");
                                            Order order = new Order();
                                            order.setId(id);
                                            order.setState(obj.getInt("State"));
                                            order.setTp(obj.getInt("tp"));
                                            order.setAddr1(obj.getString("Addr1"));
                                            order.setAddr2(obj.getString("Addr2"));
                                            order.setDesc(obj.getString("Desc"));
                                            order.setCat(obj.getInt("cat"));
                                            order.setCost(obj.getInt("cost"));
                                            order.setDist((float) obj.getDouble("dist"));
                                            order.setLat1((float) obj.getDouble("lat1"));
                                            order.setLng1((float) obj.getDouble("lng1"));
                                            order.setDate(obj.getString("DateTime"));
                                            addOrder(order);
                                        } else Log.e(LOG_TAG, "isOrdersListContains(" + id + ")");
                                    }
                                }
                                checkOrdersList(arr);
                            } else if ( code == 1 ) {
                                // st = 0;
                                JSONArray arr = new JSONArray(s);
                                Log.e(LOG_TAG, "products size - " + arr.length());
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject obj = arr.getJSONObject(i);
                                    final int id = obj.getInt("ID");
                                    if ( id != 1 ) {
                                        String name = obj.getString("cat");
                                        int ordCount = obj.getInt("ordCount");
                                        int parent = obj.getInt("parent");
                                        Category cat = new Category(id, name, ordCount, parent);
                                        addCat(cat);
                                    }
                                }
                            }
                        } catch (JSONException je) {
                            Log.e("myimag", "json exception " + je);
                        } catch (Exception e) {
                            Log.e("myimag", "catException " + e);
                        }
                    }

                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.e("myimag", "VolleyError: " + volleyError);
                        request(code, temp);
                    }
                }) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        HashMap<String, String> params = new HashMap<>();
                        params.put("id", "" + 0);
                        params.put("trf", String.valueOf(Prefs.getIntDef(getContext(), Prefs.MY_TRF, 0)));
                        params.put("city", String.valueOf(Prefs.getIntDef(getContext(), Prefs.CITY, 1)));
                        if ( code == 2 ) params.put("cat", String.valueOf(temp));
                        return params;
                    }
                    @Override
                    public Priority getPriority() {
                        return Priority.IMMEDIATE;
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(LOG_TAG, "OrderListFragment onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        timer.cancel();
        Log.e(LOG_TAG, "OrderListFragment onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(LOG_TAG, "OrderListFragment onDestroy");
    }

    public boolean onBackPressed() {
        /*if ( st == 1 ) {
            request(1, 0);
            return false;
        }*/
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e(LOG_TAG, "OrderListFragment onDetach");
    }

}
