package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class EventFeedItem
{
    private Event event;
    private int interestedFriendCount;
    private OffsetDateTime interestedTime;

    public Event getEvent()
    {
        return event;
    }

    public void setEvent( Event event )
    {
        this.event = event;
    }

    public int getInterestedFriendCount()
    {
        return interestedFriendCount;
    }

    public void setInterestedFriendCount( int interestedFriendCount )
    {
        this.interestedFriendCount = interestedFriendCount;
    }

    public OffsetDateTime getInterestedTime()
    {
        return interestedTime;
    }

    public void setInterestedTime( OffsetDateTime interestedTime )
    {
        this.interestedTime = interestedTime;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.interestedTime.toInstant().toEpochMilli();
    }
}
