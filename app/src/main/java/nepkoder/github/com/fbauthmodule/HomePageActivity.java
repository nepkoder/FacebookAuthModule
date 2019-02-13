package nepkoder.github.com.fbauthmodule;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.facebook.accountkit.AccountKit;
import com.facebook.login.LoginManager;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
    }

    public void logout(View view) {
        LoginManager.getInstance().logOut();
        AccountKit.logOut();
        startActivity(new Intent(HomePageActivity.this, FBKitLoginActivity.class));
        finish();
    }
}