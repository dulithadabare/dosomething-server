package com.dulithadabare.dosomething.facebook;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Cursors
{
    private String after;
    private String before;

    public Cursors()
    {
    }

    public String getAfter()
    {
        return after;
    }

    public void setAfter( String after )
    {
        this.after = after;
    }

    public String getBefore()
    {
        return before;
    }

    public void setBefore( String before )
    {
        this.before = before;
    }
}
