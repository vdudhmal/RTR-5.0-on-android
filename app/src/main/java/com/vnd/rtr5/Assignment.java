package com.vnd.rtr5;

import java.util.ArrayList;
import java.util.List;

public class Assignment {
    private final String name;
    private final String className;
    private final List<Assignment> children;

    public Assignment(String name, String className) {
        this.name = name;
        this.className = className;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public List<Assignment> getChildren() {
        return children;
    }

    public void addChild(Assignment child) {
        children.add(child);
    }
}
