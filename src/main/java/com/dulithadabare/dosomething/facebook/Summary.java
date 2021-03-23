package com.dulithadabare.dosomething.facebook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Summary
{
    @JsonProperty("total_count")
    private int totalCount;

    public Summary()
    {
    }

    public int getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount( int totalCount )
    {
        this.totalCount = totalCount;
    }
}
