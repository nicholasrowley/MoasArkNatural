package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;

/**
 * Fragment activity for loading the Rss list view.
 * Created by Nicholas Rowley on 2/20/2017.
 */

public class RssFragment extends Fragment implements AdapterView.OnItemClickListener {

    private ProgressBar progressBar;
    private MyListView listView;
    private View view;
    private RssAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_layout, container, false);
            progressBar = view.findViewById(R.id.progressBar);
            listView = view.findViewById(R.id.listView);
            listView.setOnItemClickListener(this);
            startService();
        } else {
            // If we are returning from a configuration change:
            // "view" is still attached to the previous view hierarchy
            // so we need to remove it and re-attach it to the current one
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            } else {
                Log.d("Test", "NULL");
            }
        }
        return view;
    }

    private void startService() {
        Intent intent = new Intent(getActivity(), RssService.class);
        intent.putExtra(RssService.RECEIVER, resultReceiver);
        getActivity().startService(intent);
    }

    /**
     * Once the {@link RssService} finishes its task, the result is sent to this ResultReceiver.
     */
    private final ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
        @SuppressWarnings("unchecked")
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            List<RssItem> items = (List<RssItem>) resultData.getSerializable(RssService.ITEMS);
            if (items != null) {
                adapter = new RssAdapter(getActivity(), items);
                listView.setAdapter(adapter);
            } else {
                Toast.makeText(getActivity(), "An error occured while downloading the rss feed.",
                        Toast.LENGTH_LONG).show();
            }
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RssAdapter adapter = (RssAdapter) parent.getAdapter();
        RssItem item = (RssItem) adapter.getItem(position);
        Uri uri = Uri.parse(item.getLink().replaceAll(" ", "%20"));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
