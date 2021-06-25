package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public class HappeningFeedItem
{
    private ConfirmedEvent event;
    private int activeCount;
    private OffsetDateTime startTime;
    private List<BasicProfile> activeFriendList;

    public ConfirmedEvent getEvent()
    {
        return event;
    }

    public void setEvent( ConfirmedEvent event )
    {
        this.event = event;
    }

    public int getActiveCount()
    {
        return activeCount;
    }

    public void setActiveCount( int activeCount )
    {
        this.activeCount = activeCount;
    }

    public OffsetDateTime getStartTime()
    {
        return startTime;
    }

    public void setStartTime( OffsetDateTime startTime )
    {
        this.startTime = startTime;
    }

    public List<BasicProfile> getActiveFriendList()
    {
        return activeFriendList;
    }

    public void setActiveFriendList( List<BasicProfile> activeFriendList )
    {
        this.activeFriendList = activeFriendList;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.startTime.toInstant().toEpochMilli();
    }
}
