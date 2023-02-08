package com.example.IDP;

import java.util.Arrays;
import java.util.List;

class SamlAttribute {
    private String name;
    private List<String> values;

    public SamlAttribute(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    public SamlAttribute(String name, String value) {
        this.name = name;
        this.values = Arrays.asList(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public String getValue() {
        return String.join(", ", values);
    }

    @Override
    public String toString() {
        return "SamlAttribute{" +
                "name='" + name + '\'' +
                ", values=" + values +
                '}';
    }
}
