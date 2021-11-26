package com.pacsapp.pacsapp.treetable;

import javafx.beans.property.SimpleStringProperty;

public class TreeDataProperty {
    private SimpleStringProperty name;
    private SimpleStringProperty value;

    public SimpleStringProperty nameProperty() {
        if (name == null) {
            name = new SimpleStringProperty(this, "name");
        }
        return name;
    }

    public SimpleStringProperty valueProperty() {
        if (value == null) {
            value = new SimpleStringProperty(this, "value");
        }
        return value;
    }

    public TreeDataProperty(String name, String value) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String fName) {
        name.set(fName);
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String fName) {
        value.set(fName);
    }
}
