package com.wordpress.onelifegroupnz.moaarknatural;

/**
 * This class stores the data for a single search suggestion.
 * Created by Nicholas Rowley on 21/03/2017.
 */

public class SearchSuggestion {
    private String searchType;
    private String fileName;

    public SearchSuggestion(String type, String sugestion) {
        searchType = type;
        fileName = sugestion;
    }

    public String getSearchType() {
        return searchType;
    }

    public String getFileName() {
        return fileName;
    }
}
