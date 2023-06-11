package sage.networktools;

import android.Manifest.permission;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import java.util.List;

public class MainActivity extends AppCompatActivity implements AlertsFragment.OnFragmentInteractionListener, HistoryFragment.OnFragmentInteractionListener, ScanFragment.OnFragmentInteractionListener {

    RelativeLayout mainWrapper;
    BottomNavFragmentAdapter adapter;
    BottomBar bottomNavigationView;
    CustomViewPager viewPager;
    DeviceDatabaseHelper dbHelper;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
        }
        /*if(getActionBar() != null)
            getActionBar().show();
            */

        dbHelper = new DeviceDatabaseHelper(getApplicationContext());
        adapter = new BottomNavFragmentAdapter(getSupportFragmentManager(), dbHelper);

        viewPager = findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setSwipingEnabled(false);
        viewPager.setAdapter(adapter);

        bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnTabSelectListener(tabId -> {
            if (tabId == R.id.action_scan)
                viewPager.setCurrentItem(0, false);

            if (tabId == R.id.action_history)
                viewPager.setCurrentItem(1, false);

            if (tabId == R.id.action_alerts)
                viewPager.setCurrentItem(2, false);
        });
        viewPager.setCurrentItem(0, false);

        /*
        mainWrapper = (RelativeLayout) findViewById(R.id.main_wrapper);
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_up);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mainWrapper.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mainWrapper.startAnimation(animation);
        */



        /*

        Show Ad

        AdView mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId(getString(R.string.banner_ad_unit_id));

        mainWrapper.addView(mAdView, 1);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAdView.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mAdView.setLayoutParams(params);

        mAdView.loadAd(new AdRequest.Builder().build());*/

        Dexter.withContext(this)
                .withPermissions(
                        permission.INTERNET,
                        permission.ACCESS_NETWORK_STATE,
                        permission.ACCESS_WIFI_STATE,
                        permission.CHANGE_NETWORK_STATE,
                        permission.CHANGE_WIFI_MULTICAST_STATE,
                        permission.ACCESS_FINE_LOCATION,
                        permission.ACCESS_COARSE_LOCATION
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {
                if(report.areAllPermissionsGranted()) {
                    //all permissions granted
                }else{
                    //report.getGrantedPermissionResponses();
                }
            }
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
