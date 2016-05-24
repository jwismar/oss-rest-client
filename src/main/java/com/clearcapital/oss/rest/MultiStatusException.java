package com.clearcapital.oss.rest;

public class MultiStatusException extends Exception {

    private static final long serialVersionUID = -123149176704761552L;

    private MultiStatusResult result = null;

    MultiStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    MultiStatusException(Throwable cause) {
        super(cause);
    }

    public MultiStatusResult getResult() {
        return result;
    }

    public void setResult(MultiStatusResult result) {
        this.result = result;
    }
}
