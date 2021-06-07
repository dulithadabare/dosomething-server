package com.dulithadabare.dosomething.model;

public class EventInvited
{
    private long eventId;
    private Long userId;
    private String description;
    private Boolean isConfirmed;

    public long getEventId()
    {
        return eventId;
    }

    public void setEventId( long eventId )
    {
        this.eventId = eventId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId( Long userId )
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

    public boolean isConfirmed()
    {
        return isConfirmed;
    }

    public void setConfirmed( boolean confirmed )
    {
        isConfirmed = confirmed;
    }
}

