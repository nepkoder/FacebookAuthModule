package nepkoder.github.com.fbauthmodule;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccessToken;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.PhoneNumber;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FBKitLoginActivity extends AppCompatActivity {

    Context context;
    private static final int FRAMEWORK_REQUEST_CODE = 1;
    private int nextPermissionsRequestCode = 4000;
    private final Map<Integer, OnCompleteListener> permissionsListeners = new HashMap<>();
    RelativeLayout progress_bar_parent;
    CallbackManager callbackManager;
    LoginButton loginButton;

    private interface OnCompleteListener {
        void onComplete();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fbkit_login);
        context = getBaseContext();
        callbackManager = CallbackManager.Factory.create(); //facebook authentication


        if(com.facebook.AccessToken.getCurrentAccessToken()!= null){
            //facebook authentication
            startActivity(new Intent(FBKitLoginActivity.this, HomePageActivity.class));
            Toast.makeText(this, "Already Logged In", Toast.LENGTH_SHORT).show();
            finish();
        }
        else if(AccountKit.getCurrentAccessToken() != null){
            //facebook kit
            Toast.makeText(context, "You Already Logged In", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(FBKitLoginActivity.this, HomePageActivity.class));
            finish();
        }
        else{
            Toast.makeText(context, "Please Login", Toast.LENGTH_SHORT).show();
        }



        //facebook authentication
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("email","public_profile"));

        //facebook authentication
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                callUserAuth(loginResult);
            }

            @Override
            public void onCancel() {
                Toast.makeText(context, "Cancel", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(context, ""+exception, Toast.LENGTH_SHORT).show();
             }
        });
    }



    //facebook kit auth
    public void onLoginEmail(final View view) {
        if(AccountKit.getCurrentAccessToken() != null){
            Toast.makeText(context, "You Already Logged In", Toast.LENGTH_SHORT).show();
        }else{
        onLogin(LoginType.EMAIL);
            progress_bar_parent.setVisibility(View.VISIBLE);
        }
    }
    //facebook kit auth
    public void onLoginPhone(final View view) {
        if(AccountKit.getCurrentAccessToken() != null){
            Toast.makeText(context, "You Already Logged In", Toast.LENGTH_SHORT).show();
        }else {
            onLogin(LoginType.PHONE);
            progress_bar_parent.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data); //facebook authentication required
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FRAMEWORK_REQUEST_CODE) {
            return;
        }
        //facebook kit authentication
        final String toastMessage;
        final AccountKitLoginResult loginResult = AccountKit.loginResultWithIntent(data);
        if (loginResult == null || loginResult.wasCancelled()) {
            toastMessage = "Login Cancelled";
        } else if (loginResult.getError() != null) {
            toastMessage = loginResult.getError().getErrorType().getMessage();
            Toast.makeText(context, "Please try again", Toast.LENGTH_SHORT).show();
        } else {
            final AccessToken accessToken = loginResult.getAccessToken();
            final String[] phone = new String[1];
            final String[] email = new String[1];
            final long tokenRefreshIntervalInSeconds = loginResult.getTokenRefreshIntervalInSeconds();
            if (accessToken != null) {
                toastMessage = "Success:" + accessToken.getAccountId() + tokenRefreshIntervalInSeconds;
                data.getExtras().get("phone");
                AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                    @Override
                    public void onSuccess(final Account account) {
                        String user_phone = null;
                        String user_email = null;
                        String accountKitId = account.getId();
                        PhoneNumber phoneNumber = account.getPhoneNumber();

                        if(phoneNumber!=null) {
                             user_phone = phoneNumber.toString().trim();
                        }
                        if(account.getEmail()!=null) {
                             user_email = "" + account.getEmail();
                        }

//                        facebook login data handle with email or phone
//                        email or phone verify vayo.. aba handle garney kaam phone ra email lai

                    }

                    @Override
                    public void onError(final AccountKitError error) {
                    }
                });
            } else {
                toastMessage = "Unknown response type";
            }
        }
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }



    //facebook kit auth
    private void onLogin(final LoginType loginType) {
        final Intent intent = new Intent(this, AccountKitActivity.class);
        final AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder = new AccountKitConfiguration.AccountKitConfigurationBuilder(loginType, AccountKitActivity.ResponseType.TOKEN);
        final AccountKitConfiguration configuration = configurationBuilder.build();
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, configuration);
        OnCompleteListener completeListener = new OnCompleteListener() {
            @Override
            public void onComplete() {
                startActivityForResult(intent, FRAMEWORK_REQUEST_CODE);
            }
        };
        switch (loginType) {
            case EMAIL:
                if (!isGooglePlayServicesAvailable()) {
                    final OnCompleteListener getAccountsCompleteListener = completeListener;
                    completeListener = new OnCompleteListener() {
                        @Override
                        public void onComplete() {
                            requestPermissions(Manifest.permission.GET_ACCOUNTS, R.string.permissions_get_accounts_title, R.string.permissions_get_accounts_message, getAccountsCompleteListener);
                        }
                    };
                }
                break;
            case PHONE:
                if (configuration.isReceiveSMSEnabled() && !canReadSmsWithoutPermission()) {
                    final OnCompleteListener receiveSMSCompleteListener = completeListener;
                    completeListener = new OnCompleteListener() {
                        @Override
                        public void onComplete() {
                            requestPermissions(Manifest.permission.RECEIVE_SMS, R.string.permissions_receive_sms_title, R.string.permissions_receive_sms_message, receiveSMSCompleteListener);
                        }
                    };
                }
                if (configuration.isReadPhoneStateEnabled() && !isGooglePlayServicesAvailable()) {
                    final OnCompleteListener readPhoneStateCompleteListener = completeListener;
                    completeListener = new OnCompleteListener() {
                        @Override
                        public void onComplete() {
                            requestPermissions(Manifest.permission.READ_PHONE_STATE, R.string.permissions_read_phone_state_title, R.string.permissions_read_phone_state_message, readPhoneStateCompleteListener);
                        }
                    };
                }
                break;
        }
        completeListener.onComplete();
    }

    //facebook kit auth
    private boolean isGooglePlayServicesAvailable() {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int googlePlayServicesAvailable = apiAvailability.isGooglePlayServicesAvailable(this);
        return googlePlayServicesAvailable == ConnectionResult.SUCCESS;
    }

    //facebook kit auth
    private boolean canReadSmsWithoutPermission() {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int googlePlayServicesAvailable = apiAvailability.isGooglePlayServicesAvailable(this);
        if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
            return true;
        }

        return false;
    }

    //facebook kit auth
    private void requestPermissions(final String permission, final int rationaleTitleResourceId, final int rationaleMessageResourceId, final OnCompleteListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }

        checkRequestPermissions(permission, rationaleTitleResourceId, rationaleMessageResourceId, listener);
    }

    //facebook kit auth
    @TargetApi(23)
    private void checkRequestPermissions(final String permission, final int rationaleTitleResourceId, final int rationaleMessageResourceId, final OnCompleteListener listener) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }

        final int requestCode = nextPermissionsRequestCode++;
        permissionsListeners.put(requestCode, listener);

        if (shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(this).setTitle(rationaleTitleResourceId).setMessage(rationaleMessageResourceId).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    requestPermissions(new String[]{permission}, requestCode);
                }
            }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            // ignore and clean up the listener
                            permissionsListeners.remove(requestCode);
                        }
                    }).setIcon(android.R.drawable.ic_dialog_alert).show();
        } else {

            requestPermissions(new String[]{permission}, requestCode);
        }
    }

    //facebook kit auth
    @TargetApi(23)
    @SuppressWarnings("unused")
    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String permissions[], final @NonNull int[] grantResults) {
        final OnCompleteListener permissionsListener = permissionsListeners.remove(requestCode);
        if (permissionsListener != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionsListener.onComplete();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    //facebook authentication login
    private void callUserAuth(LoginResult loginResult) {

        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.v("LoginActivity", response.toString());

                        try {
                            // Application code
                           String email = object.getString("email");
                            String birthday = object.getString("birthday"); // 01/31/1980 format
                            Log.d("myauth","\n email : "+email);

                            // aru further data handle garney aba...

                        }catch (Exception e){
                             e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email,gender,birthday");
        request.setParameters(parameters);
        request.executeAsync();

    }

//    logut facebook app config
    public void onLogout(View view) {
        LoginManager.getInstance().logOut();
        AccountKit.logOut();
        if(AccountKit.getCurrentAccessToken() == null){
            Toast.makeText(context, "LogOut Complete", Toast.LENGTH_SHORT).show();
        }
    }
}
