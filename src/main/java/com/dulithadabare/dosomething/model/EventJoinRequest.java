package com.dulithadabare.dosomething.model;

public class EventJoinRequest
{
    private long eventId;
    private int userId;
    private long createdTime;

    public long getEventId()
    {
        return eventId;
    }

    public void setEventId( long eventId )
    {
        this.eventId = eventId;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId( int userId )
    {
        this.userId = userId;
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
