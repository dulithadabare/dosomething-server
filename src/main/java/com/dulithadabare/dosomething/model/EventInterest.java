package com.dulithadabare.dosomething.model;

public class EventInterest
{
    private long eventId;
    private int userId;
    private String description;

    public EventInterest( long eventId, int userId, String description )
    {
        this.eventId = eventId;
        this.userId = userId;
        this.description = description;
    }

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

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }
}
