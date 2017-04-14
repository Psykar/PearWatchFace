package com.seapip.thomas.pear;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends Fragment implements View.OnTouchListener {
    /* Paint */
    Paint mOverlayPaint;
    private View mView;
    private int mRow;
    private int mCol;
    private SharedPreferences mPrefs;

    public static final int COLOR_REQUEST = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRow = getArguments().getInt("row");
        mCol = getArguments().getInt("col");
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());

        mOverlayPaint = new Paint();
        mOverlayPaint.setColor(Color.argb(192, 0, 0, 0));
        mView = new View(getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                canvas.drawRect(0, 0, getWidth(), getHeight(), mOverlayPaint);
                for (SettingsOverlay moduleOverlay : getSettingModuleOverlays()) {
                    moduleOverlay.draw(canvas);
                }
            }
        };
        mView.setOnTouchListener(this);
        return mView;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            SettingsOverlay active = null;
            for (SettingsOverlay moduleOverlay : getSettingModuleOverlays()) {
                if (moduleOverlay.contains((int) event.getX(), (int) event.getY())) {
                    active = moduleOverlay;
                }
            }
            if (active != null) {
                if (active.getActive()) {
                    Intent intent = active.getIntent();
                    if (intent != null) {
                        startActivityForResult(intent, active.getRequestCode());
                    }
                }
                for (SettingsOverlay moduleOverlay : getSettingModuleOverlays()) {
                    moduleOverlay.setActive(false);
                }
                active.setActive(true);
            }
            mView.invalidate();
        }
        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String title;
            SharedPreferences.Editor editor = mPrefs.edit();
            switch (requestCode) {
                case COLOR_REQUEST:
                    title = data.getStringExtra("color_name");
                    editor.putString("settings_color_name", title);
                    editor.putInt("settings_color_value", data.getIntExtra("color_value", 0));
                    editor.apply();
                    getSettingModuleOverlays().get(0).setTitle(title);
                    break;
                default:
                    for (int id : ModularWatchFaceService.COMPLICATION_IDS) {
                        if (id == requestCode) {
                            title = "OFF";
                            if (data != null) {
                                ComplicationProviderInfo providerInfo =
                                        data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
                                if (providerInfo != null) {
                                    title = providerInfo.providerName;
                                }
                            }
                            getSettingModuleOverlays().get(requestCode).setTitle(title);
                            return;
                        }
                    }
                    break;
            }
        }
    }

    private ArrayList<SettingsOverlay> getSettingModuleOverlays() {
        return ((SettingsActivity) getActivity()).getSettingModuleOverlays(mRow, mCol);
    }
}
