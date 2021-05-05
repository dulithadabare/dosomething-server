package com.dulithadabare.dosomething.model;

public class JoinRequest
{
    private UserProfile user;
    private long eventId;
    private long createdTime;

    public JoinRequest( UserProfile user, long eventId, long createdTime )
    {
        this.user = user;
        this.eventId = eventId;
        this.createdTime = createdTime;
    }

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
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
