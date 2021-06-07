package com.dulithadabare.dosomething.model;

public class JoinRequest
{
    private BasicProfile user;
    private long eventId;
    private long createdTime;

    public JoinRequest( BasicProfile user, long eventId, long createdTime )
    {
        this.user = user;
        this.eventId = eventId;
        this.createdTime = createdTime;
    }

    public BasicProfile getUser()
    {
        return user;
    }

    public void setUser( BasicProfile user )
    {
        this.user = user;
    }

    public long getEventId()
    {
        return eventId;
    }

    public void setEventId( long eventId )
    {
        this.eventId = eventId;
    }

    public long getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( long createdTime )
    {
        this.createdTime = createdTime;
    }
}
