package com.wordpress.onelifegroupnz.moaarknatural;

import android.app.Activity;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import androidx.fragment.app.Fragment;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.widget.SearchView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import java.util.List;

/**
 * This activity fragment implements search and filtering tools for the app.
 * - Search functionality
 * - Search Suggestions
 * - Search Type filter
 * Created by Nicholas Rowley on 14/03/2017.
 */

public class CustomSearchFragment extends Fragment {

    private SimpleCursorAdapter mAdapter;
    private GlobalAppData appData; //singleton instance of globalAppData

    private List<SearchSuggestion> suggestions;
    private MatrixCursor cursor;
    private View view;

    private static String lastSessionID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.search_fragment, container, false);

            if (((Activity)view.getContext()).toString().equals(lastSessionID)) {
                Log.d("SearchFrag", "Unnatural execution of activity detected. restarting application.");
                Intent intent = new Intent(((Activity) view.getContext()), SplashScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                lastSessionID = ((Activity)view.getContext()).toString();
            }

            if (appData == null) {
                final Thread fetchData = new Thread() {
                    public void run() {
                        appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT), getContext(), "");
                    }
                };
                fetchData.start();
                try {
                    fetchData.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            final String[] from = new String[] {"videoName"};
            final int[] to = new int[] {android.R.id.text1};
            mAdapter = new SimpleCursorAdapter(getActivity(),
                    android.R.layout.simple_list_item_1,
                    null,
                    from,
                    to,
                    CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            suggestions = appData.getSearchSuggestions();
        } else {
            // If we are returning from a configuration change:
            // "view" is still attached to the previous view hierarchy
            // so we need to remove it and re-attach it to the current one
            ViewGroup parent = (ViewGroup) view.getParent();
            parent.removeView(view);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //populate spinner and filter tools
        final Spinner spinner = view.findViewById(R.id.videoTypeSpinner);
        final ImageView spinnerArrow = view.findViewById(R.id.spinnerArrow);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), R.layout.spinner_item,
                getResources().getStringArray(R.array.search_video_options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        //allows the custom spinner image to click the spinner
        spinnerArrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                spinner.performClick();
            }
        });

        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();

        searchView.setSuggestionsAdapter(mAdapter);
        searchView.setIconifiedByDefault(false);
        // Getting selected (clicked) item suggestion
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionClick(int position) {
                if (cursor != null)
                {
                    //Proceed to Search Results
                    Intent intent = new Intent(getContext(), SearchResults.class);
                    intent.putExtra("searchInput", suggestions.get(cursor.getInt(0)).getFileName());
                    intent.putExtra("searchType", spinner.getSelectedItem().toString());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    searchView.clearFocus();
                    startActivity(intent);
                }
                return true;
            }

            @Override
            public boolean onSuggestionSelect(int position) {
                // Your code here
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Proceed to Search Results
                Intent intent = new Intent(getContext(), SearchResults.class);
                intent.putExtra("searchInput", searchView.getQuery().toString());
                intent.putExtra("searchType", spinner.getSelectedItem().toString());
                searchView.clearFocus();
                startActivity(intent);
                if (getContext() != null) {
                    if (!getContext().getClass().getSimpleName().equals("Home")) {
                        ((Activity) getContext()).finish();
                    }
                }
                return false;
            }

            //Runs when search bar text input is updated
            @Override
            public boolean onQueryTextChange(String s) {
                populateAdapter(s, spinner);
                return false;
            }
        });

    }

    /*Queries for information based on filters and text entered into the search bar*/
    private void populateAdapter(String query, Spinner searchTypeSpinner) {
        cursor = new MatrixCursor(new String[]{ BaseColumns._ID, "videoName" });
        for (int i=0; i<suggestions.size(); i++) {
            if (suggestions.get(i).getSearchType().equals(searchTypeSpinner.getSelectedItem().toString())
                    || searchTypeSpinner.getSelectedItem().toString().equals(view.getResources().getString(R.string.search_type_all)))
                if (suggestions.get(i).getFileName().toLowerCase().startsWith(query.toLowerCase()))
                    cursor.addRow(new Object[] {i, suggestions.get(i).getFileName()});
        }
        mAdapter.changeCursor(cursor);
    }

}
