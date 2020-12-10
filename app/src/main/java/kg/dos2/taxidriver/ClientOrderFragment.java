package kg.dos2.taxidriver;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import static kg.dos2.taxidriver.TaxiActivity.FRAGMENT_CLIENT_ORDER;

public class ClientOrderFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private FragmentsListener callBack;

    public void setCallBack(FragmentsListener callBack) {
        this.callBack = callBack;
    }

    public ClientOrderFragment() {
        // Required empty public constructor
    }

    public static ClientOrderFragment newInstance(String param1, String param2) {
        ClientOrderFragment fragment = new ClientOrderFragment();
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
        callBack.curFrag(FRAGMENT_CLIENT_ORDER);
        return inflater.inflate(R.layout.fragment_client_order, container, false);
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
