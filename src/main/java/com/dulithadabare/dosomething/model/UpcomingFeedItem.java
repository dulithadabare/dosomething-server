package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class UpcomingFeedItem
{
    private Event event;
    private boolean interested;
    private OffsetDateTime firstInterestedTime;

    public Event getEvent()
    {
        return event;
    }

    public void setEvent( Event event )
    {
        this.event = event;
    }

    public boolean isInterested()
    {
        return interested;
    }

    public void setInterested( boolean interested )
    {
        this.interested = interested;
    }

    public OffsetDateTime getFirstInterestedTime()
    {
        return firstInterestedTime;
    }

    public void setFirstInterestedTime( OffsetDateTime firstInterestedTime )
    {
        this.firstInterestedTime = firstInterestedTime;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.firstInterestedTime.toInstant().toEpochMilli();
    }
}

