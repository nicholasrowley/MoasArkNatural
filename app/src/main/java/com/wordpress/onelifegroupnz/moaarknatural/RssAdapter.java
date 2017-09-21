package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for loading new items in the RSS list view.
 * Created by Nicholas Rowley on 2/20/2017.
 */

public class RssAdapter extends BaseAdapter {

    private final List<RssItem> items;
    private final Context context;
    private ViewHolder holder;

    public RssAdapter(Context context, List<RssItem> items) {

        //To remove the feeds that don't point to a website post.
        List<RssItem> itemsToKeep = new ArrayList<>();
        for ( RssItem item : items ) {
            if (!item.getLink().equals("https://moasarknaturalnz.com")){
                itemsToKeep.add(item);
            }
        }

        this.items = itemsToKeep;
        this.context = context;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.rss_item, null);
            holder = new ViewHolder();
            holder.itemTitle = convertView.findViewById(R.id.itemTitle);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.itemTitle.setText(items.get(position).getTitle());
        holder.itemTitle.setHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 68, context.getResources().getDisplayMetrics()));
        holder.itemTitle.setGravity(Gravity.CENTER_VERTICAL);
        return convertView;
    }

    static class ViewHolder {
        TextView itemTitle;
    }
}
