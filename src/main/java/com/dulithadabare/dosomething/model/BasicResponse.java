package com.dulithadabare.dosomething.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BasicResponse {
    private int status;
    private String message;
    private List<Object> data;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_ERROR = -1;
    private final String SUCCESS;
    private final String ERROR;

    private BasicResponse() {
        this.SUCCESS = "SUCCESS";
        this.ERROR = "ERROR";
        this.status = 1;
        this.message = "SUCCESS";
        this.data = new ArrayList();
    }

    public BasicResponse(Object data) {
        this();
        this.addData(data);
    }

    public BasicResponse(Object data, String message) {
        this();
        this.addData(data);
        this.message = message;
    }

    public BasicResponse(String message, int status) {
        this();
        this.status = status;
        this.message = message;
    }

    private void addData(Object data) {
        if (data instanceof Collection) {
            List<?> dataList = (List)data;
            if (dataList.isEmpty()) {
                this.message = "ERROR";
                this.status = -1;
            } else {
                this.data.addAll(dataList);
            }
        } else if (data == null) {
            this.message = "ERROR";
            this.status = -1;
        } else {
            this.data.add(data);
        }

    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Object> getData() {
        return this.data;
    }

    public void setData(List<Object> data) {
        this.data = data;
    }
}
