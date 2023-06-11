package sage.networktools;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class BottomNavFragmentAdapter extends FragmentPagerAdapter {

    private static int NUM_ITEMS = 3;
    private DeviceDatabaseHelper databaseHelper;

    public BottomNavFragmentAdapter(FragmentManager fm, DeviceDatabaseHelper dbHelper) {
        super(fm);
        databaseHelper = dbHelper;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("dbHelper", databaseHelper);
        switch(position) {
            case 0:
                ScanFragment scanFragment = ScanFragment.newInstance(1, "Scan");
                scanFragment.setArguments(bundle);
                return scanFragment;
            case 1:
                HistoryFragment historyFragment = HistoryFragment.newInstance(0, "History");
                historyFragment.setArguments(bundle);
                return historyFragment;
            case 2:
                AlertsFragment alertsFragment = AlertsFragment.newInstance(2, "Alerts");
                alertsFragment.setArguments(bundle);
                return alertsFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }
}
