package com.dulithadabare.dosomething.facebook;

import java.util.List;

public class FriendsResponse
{
    private List<Friend> data;
    private Paging paging;
    private Summary summary;

    public FriendsResponse()
    {
    }

    public List<Friend> getData()
    {
        return data;
    }

    public void setData( List<Friend> data )
    {
        this.data = data;
    }

    public Paging getPaging()
    {
        return paging;
    }

    public void setPaging( Paging paging )
    {
        this.paging = paging;
    }

    public Summary getSummary()
    {
        return summary;
    }

    public void setSummary( Summary summary )
    {
        this.summary = summary;
    }
}
