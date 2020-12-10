package kg.dos2.taxidriver;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

public class OrderInfoFragment extends BottomSheetDialogFragment {

    Order order;

    private MapView mapView;
    private FragmentsListener callBack;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public OrderInfoFragment(Order order) {
        this.order = order;
    }

    public static OrderInfoFragment newInstance(FragmentsListener callBack, Order order) {
        OrderInfoFragment fragment = new OrderInfoFragment(order);
        fragment.setCallBack(callBack);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getContext(), "pk.eyJ1IjoidGFhbGF5ZGV2IiwiYSI6ImNrM3JyemJqbzBkMnAzaHBzaTNtbWE0eDYifQ.6jS_pnpk66YaDc2EabZF4A");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_order_info, container, false);

        TextView headerText = view.findViewById(R.id.orderInfoHeaderText);
        headerText.setText("Заказ #" + order.getId());

        view.findViewById(R.id.orderInfoCloseIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OrderInfoFragment.this.dismiss();
            }
        });

        TextView addr1 = view.findViewById(R.id.orderInfoAddr1);
        addr1.setText(order.getAddr1());
        if ( !order.getAddr2().isEmpty() ) {
            TextView addr2 = view.findViewById(R.id.orderInfoAddr2);
            addr2.setText(order.getAddr2());
        }
        if ( !order.getDesc().isEmpty() ) {
            TextView desc = view.findViewById(R.id.orderInfoDesc);
            desc.setText(order.getDesc());
        } else {
            view.findViewById(R.id.orderInfoDescLay).setVisibility(View.GONE);
        }
        if ( order.getDist() > 0 ) {
            TextView dist = view.findViewById(R.id.orderInfoDist);
            dist.setText(order.getDist() + "КМ");
        }
        if ( order.getCost() > 0 ) {
            TextView price = view.findViewById(R.id.orderInfoPrice);
            price.setText(order.getCost() + "СОМ");
        }
        view.findViewById(R.id.orderInfoTakeBt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callBack.tryConnectToOrder(order.getId());
                OrderInfoFragment.this.dismiss();
            }
        });

        Log.e(TaxiActivity.LOG_TAG, "OrderInfo lat1 = " + order.getLat1() + ", lng1 = " + order.getLng1());

        if ( order.getLat1() > 0 && order.getLng1() > 0 ) {
            mapView = view.findViewById(R.id.orderInfoMapBox);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull MapboxMap mapboxMap) {
                    mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                        @Override
                        public void onStyleLoaded(@NonNull Style style) {
                            // Map is set up and the style has loaded. Now you can add data or make other map adjustments.

                        }
                    });
                    mapboxMap.getUiSettings().setZoomGesturesEnabled(true);
                    mapboxMap.getUiSettings().setCompassEnabled(true);
                    mapboxMap.getUiSettings().setRotateGesturesEnabled(true);

                    LatLng coordinate = new LatLng(order.getLat1(), order.getLng1());
                    CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 14);
                    mapboxMap.animateCamera(yourLocation);

                    MarkerOptions markerOptions = new MarkerOptions().position(coordinate)
                            .title("Клиент");
                    mapboxMap.addMarker(markerOptions);

                }
            });
        } else {
            ((LinearLayout)view.findViewById(R.id.orderInfoBodyLay)).removeView(view.findViewById(R.id.orderInfoMapLay));
        }

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm =  (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
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

    @Override
    public void onResume() {
        super.onResume();
        if ( mapView != null ) mapView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if ( mapView != null ) mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if ( mapView != null ) mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mapView != null ) mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if ( mapView != null ) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if ( mapView != null ) mapView.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if ( mapView != null ) mapView.onStart();
    }
}
