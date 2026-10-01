package com.closewise.fixture;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class FixtureActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView view = new TextView(this);
        view.setText("CloseWise automation fixture is running");
        view.setTextSize(22);
        view.setPadding(48, 96, 48, 48);
        setContentView(view);
    }
}
