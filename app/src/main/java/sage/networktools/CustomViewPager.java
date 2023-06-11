package sage.networktools;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomViewPager extends ViewPager {

    boolean swipeEnabled = true;

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(swipeEnabled)
            return super.onTouchEvent(ev);

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(swipeEnabled)
            return super.onInterceptTouchEvent(ev);

        return false;
    }

    public void setSwipingEnabled(boolean enabled) {
        swipeEnabled = enabled;
    }
}
