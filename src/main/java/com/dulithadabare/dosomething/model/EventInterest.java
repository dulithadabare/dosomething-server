package com.dulithadabare.dosomething.model;

import java.time.OffsetDateTime;

public class EventInterest
{
    private long eventId;
    private Long userId;
    private OffsetDateTime createdTime;

    public EventInterest( long eventId, Long userId )
    {
        this.eventId = eventId;
        this.userId = userId;
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

    public OffsetDateTime getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( OffsetDateTime createdTime )
    {
        this.createdTime = createdTime;
    }
}
