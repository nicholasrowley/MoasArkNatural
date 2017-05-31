package com.wordpress.onelifegroupnz.moaarknatural;

/**
 * Stores information on a single Rss list item.
 * Created by Nicholas Rowley on 2/20/2017.
 */

public class RssItem {

    private final String title;
    private final String link;

    public RssItem(String title, String link) {
        this.title = title;
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }
}