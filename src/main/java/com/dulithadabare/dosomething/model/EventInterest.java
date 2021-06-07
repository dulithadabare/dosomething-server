package com.dulithadabare.dosomething.model;

import java.time.OffsetDateTime;

public class EventInterest
{
    private long eventId;
    private Long userId;
    private OffsetDateTime createdTime;
    private String description;

    public EventInterest( long eventId, Long userId, String description )
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

    public OffsetDateTime getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( OffsetDateTime createdTime )
    {
        this.createdTime = createdTime;
    }
}
