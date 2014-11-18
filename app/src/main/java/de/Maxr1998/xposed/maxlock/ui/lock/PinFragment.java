package de.Maxr1998.xposed.maxlock.ui.lock;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import de.Maxr1998.xposed.maxlock.AuthenticationSucceededListener;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class PinFragment extends Fragment implements View.OnClickListener {

    public AuthenticationSucceededListener authenticationSucceededListener;
    ViewGroup rootView;
    String requestPkg;
    View pinMainLayout, mInputView;
    TextView titleView;
    ImageButton mDeleteButton;
    SharedPreferences pref;
    View[] pinButtons;
    TextView[] pb;
    private StringBuilder key;
    private TextView mInputText;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            authenticationSucceededListener = (AuthenticationSucceededListener) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(getActivity().getClass().getSimpleName() + "must implement AuthenticationSucceededListener to use this fragment", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // Preferences
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Strings
        requestPkg = getArguments().getString(Common.INTENT_EXTRAS_PKG_NAME);
        key = new StringBuilder("");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Main Views
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_pin, container, false);
        pinMainLayout = rootView.findViewById(R.id.pin_main_layout);
        titleView = (TextView) rootView.findViewById(R.id.title_view);
        //titleView.setTextColor(Util.getTextColor(getActivity()));

        // Views
        mInputView = rootView.findViewById(R.id.inputView);
        mInputText = (TextView) mInputView;
        mInputText.setText(genPass(key));
        mDeleteButton = (ImageButton) rootView.findViewById(R.id.delete_input);
        mDeleteButton.setOnClickListener(this);

        pinButtons = new View[]{
                rootView.findViewById(R.id.pin1),
                rootView.findViewById(R.id.pin2),
                rootView.findViewById(R.id.pin3),
                rootView.findViewById(R.id.pin4),
                rootView.findViewById(R.id.pin5),
                rootView.findViewById(R.id.pin6),
                rootView.findViewById(R.id.pin7),
                rootView.findViewById(R.id.pin8),
                rootView.findViewById(R.id.pin9),
                rootView.findViewById(R.id.pin0),
                rootView.findViewById(R.id.pin_ok)
        };
        pb = new TextView[pinButtons.length];
        for (int i = 0; i < pinButtons.length; i++) {
            pb[i] = (TextView) pinButtons[i];
            pb[i].setOnClickListener(this);
        }

        //personalizeUI();

        // Dimens
        int statusBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        int navBarHeight = getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android"));
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            pinMainLayout.setBackground(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        } else {
            pinMainLayout.setBackgroundDrawable(Util.getResizedBackground(getActivity(), screenWidth, screenHeight));
        }
        if (getActivity().getClass().getName().equals("de.Maxr1998.xposed.maxlock.ui.LockActivity")) {
            View gapTop = rootView.findViewById(R.id.top_gap);
            View gapBottom = rootView.findViewById(R.id.bottom_gap);
            if (screenWidth < screenHeight) {
                gapTop.getLayoutParams().height = statusBarHeight;
                gapBottom.getLayoutParams().height = navBarHeight;
            } else if (screenWidth > screenHeight) {
                gapTop.getLayoutParams().width = statusBarHeight;
                gapBottom.getLayoutParams().width = navBarHeight;
            }
        }
        titleView.setText(Util.getApplicationNameFromPackage(requestPkg, getActivity()));
        titleView.setCompoundDrawablesWithIntrinsicBounds(Util.getApplicationIconFromPackage(requestPkg, getActivity()), null, null, null);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        for (View v : pinButtons) {
            if (view.getId() == v.getId()) {
                String t = ((TextView) view).getText().toString();
                if (!t.equals(getString(android.R.string.ok))) {
                    key.append(t);
                    mInputText.setText(genPass(key));
                    /*if (Util.checkInput(key.toString(), Common.KEY_PIN, getActivity(), requestPkg)) {
                        authenticationSucceededListener.onAuthenticationSucceeded();
                    }*/
                } else {
                    if (Util.checkInput(key.toString(), Common.KEY_PIN, getActivity(), requestPkg)) {
                        authenticationSucceededListener.onAuthenticationSucceeded();
                    }
                }
            }
        }
        if (view.getId() == mDeleteButton.getId()) {
            key.setLength(0);
            mInputText.setText(genPass(key));
        }
    }

    String genPass(StringBuilder str) {
        StringBuilder x = new StringBuilder("");
        for (int i = 0; i < str.length(); i++) {
            x.append("\u2022");
        }
        return x.toString();
    }
}