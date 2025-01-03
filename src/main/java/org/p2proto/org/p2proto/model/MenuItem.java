package org.p2proto.model;

import java.util.List;

public class MenuItem {
    private String title;
    private String url;
    private List<MenuItem> children;

    // Constructors
    public MenuItem() {}

    public MenuItem(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public MenuItem(String title, String url, List<MenuItem> children) {
        this.title = title;
        this.url = url;
        this.children = children;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<MenuItem> getChildren() {
        return children;
    }

    public void setChildren(List<MenuItem> children) {
        this.children = children;
    }
}
