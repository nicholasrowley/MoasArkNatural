package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * An extension of the ListView widget to address the following issues
 * - Show the entrie list without need for ListView's internal scroll view
 * (Currently all items in the list must be the same height for this to work.)
 * Created by Nicholas Rowley on 2/20/2017.
 */

public class MyListView extends ListView {

    private android.view.ViewGroup.LayoutParams params;
    private int oldCount = 0;

    public MyListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        reSize();

        super.onDraw(canvas);
    }

    public void reSize(){
        if (getCount() != oldCount)
        {
            oldCount = getCount();
            int height = getChildAt(0).getHeight() + 1 + getDividerHeight();
            params = getLayoutParams();

            params.height = getCount() * height;
            setLayoutParams(params);
        }
    }

}
