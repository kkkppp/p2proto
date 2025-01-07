package org.p2proto.model;

import lombok.Data;

import java.util.List;
@Data
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
}
