package com.clearcapital.oss.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;

import com.google.common.base.Objects;

public class MultiStatusResult implements Serializable {

    @Override
    public String toString() {
        return "MultiStatusResult [results=" + results + "]";
    }

    private static final long serialVersionUID = -8796880797939481598L;

    @Override
    public int hashCode() {
        return Objects.hashCode(results);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof MultiStatusResult) {
            MultiStatusResult that = (MultiStatusResult) object;
            return Objects.equal(this.results, that.results);
        }
        return false;
    }

    private ArrayList<Result> results;

    public MultiStatusResult() {
        results = null;
    }

    public MultiStatusResult(Map<Long, WebApplicationException> exceptions) {
        results = new ArrayList<>();
        for (Entry<Long, WebApplicationException> entry : exceptions.entrySet()) {
            results.add(new Result(entry.getKey(), entry.getValue().getResponse().getStatus(), entry.getValue().getMessage()));
        }
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = new ArrayList<>(results);
    }

}
