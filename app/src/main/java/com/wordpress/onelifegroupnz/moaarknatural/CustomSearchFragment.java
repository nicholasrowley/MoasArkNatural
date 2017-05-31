package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.search_fragment, container, false);

            if (appData == null)
                appData = GlobalAppData.getInstance(getString(R.string.ACCESS_TOKEN), getContext(), "");

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

    //Each activity will provide their own onCreateOptions/menu
    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        //inflater.inflate(R.menu.menu, menu);
    }*/

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //populate spinner and filter tools
        final Spinner spinner = (Spinner) view.findViewById(R.id.videoTypeSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(view.getContext(), R.layout.spinner_item,
                getResources().getStringArray(R.array.search_video_options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        final SearchView searchView = (SearchView) MenuItemCompat
                .getActionView(menu.findItem(R.id.search));
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
                    intent.putExtra("searchInput", suggestions.get(cursor.getInt(position)).getFileName());
                    intent.putExtra("searchType", spinner.getSelectedItem().toString());
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
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                populateAdapter(s, spinner);
                return false;
            }
        });

    }

    // You must implements your logic to get data using OrmLite
    private void populateAdapter(String query, Spinner searchTypeSpinner) {
                /*{
                "Bauru", "Sao Paulo", "Rio de Janeiro",
                "Bahia", "Mato Grosso", "Minas Gerais",
                "Tocantins", "Rio Grande do Sul"
        };*/
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
