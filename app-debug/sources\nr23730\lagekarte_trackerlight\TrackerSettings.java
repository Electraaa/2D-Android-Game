package nr23730.lagekarte_trackerlight;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class TrackerSettings extends AppCompatActivity {
    private EditText mDeviceNumber;
    private EditText mPasswordView;
    private boolean permissionsGranted = false;

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_tracker_settings);
        this.mDeviceNumber = (EditText) findViewById(R.id.txtDevice);
        this.mDeviceNumber.setText(getSharedPreferences("TrackerSettings", 0).getString("DeviceNumber", "0"));
        this.mPasswordView = (EditText) findViewById(R.id.txtPassword);
        this.mPasswordView.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id != 6 && id != 0) {
                    return false;
                }
                TrackerSettings.this.attemptLogin();
                return true;
            }
        });
        ((Button) findViewById(R.id.btnSave)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                TrackerSettings.this.attemptLogin();
            }
        });
        this.permissionsGranted = mayRequestLocation();
    }

    private boolean mayRequestLocation() {
        if (VERSION.SDK_INT < 23 || checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0) {
            return true;
        }
        if (shouldShowRequestPermissionRationale("android.permission.ACCESS_FINE_LOCATION")) {
            Snackbar.make(this.mDeviceNumber, (int) R.string.permission_rationale, -2).setAction(17039370, new OnClickListener() {
                @TargetApi(23)
                public void onClick(View v) {
                    TrackerSettings.this.requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1);
                }
            });
            return false;
        }
        requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, 1);
        return this.permissionsGranted;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length == 1 && grantResults[0] == 0) {
            this.permissionsGranted = true;
        }
    }

    private void attemptLogin() {
        this.mDeviceNumber.setError(null);
        this.mPasswordView.setError(null);
        String deviceNumber = this.mDeviceNumber.getText().toString();
        String password = this.mPasswordView.getText().toString();
        boolean cancel = false;
        View focusView = null;
        if (!(TextUtils.isEmpty(password) || isPasswordValid(password))) {
            this.mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = this.mPasswordView;
            cancel = true;
        }
        if (TextUtils.isEmpty(deviceNumber)) {
            this.mDeviceNumber.setError(getString(R.string.error_field_required));
            focusView = this.mDeviceNumber;
            cancel = true;
        } else if (isDeviceNumberValid(deviceNumber) == 0) {
            this.mDeviceNumber.setError(getString(R.string.error_invalid_email));
            focusView = this.mDeviceNumber;
            cancel = true;
        }
        if (cancel) {
            focusView.requestFocus();
        } else if (this.permissionsGranted) {
            Editor editor = getPreferences(null).edit();
            editor.putString("DeviceNumber", deviceNumber);
            editor.commit();
            getApplicationContext().startService(new Intent(getApplicationContext(), BackgroundTracker.class));
        }
    }

    private int isDeviceNumberValid(String deviceNumber) {
        return Integer.valueOf(deviceNumber).intValue();
    }

    private boolean isPasswordValid(String password) {
        return password.equals(getString(R.string.password));
    }
}
