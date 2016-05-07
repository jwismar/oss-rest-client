package com.clearcapital.oss.rest;

import java.util.List;

import javax.ws.rs.core.GenericType;

import com.google.common.base.Objects;

public class DemoRestableObject {

    public static final GenericType<List<DemoRestableObject>> listGenericType = new GenericType<List<DemoRestableObject>>() {
    };

    private String entry;

    public DemoRestableObject() {
    }

    public String getEntry() {
        return entry;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(this,entry);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DemoRestableObject) {
            DemoRestableObject that = (DemoRestableObject)obj;
            return Objects.equal(entry, that.entry);
        }
        return false;
    }

    static public Builder builder() {
        return new Builder();
    }

    static public class Builder {

        DemoRestableObject result = new DemoRestableObject();

        DemoRestableObject build() {
            return result;
        }

        Builder setEntry(String value) {
            result.entry = value;
            return this;
        }
    }
}