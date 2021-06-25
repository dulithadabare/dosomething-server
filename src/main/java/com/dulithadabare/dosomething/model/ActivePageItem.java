package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class ActivePageItem
{
    BasicProfile user;
    OffsetDateTime activeTime;

    public BasicProfile getUser()
    {
        return user;
    }

    public void setUser( BasicProfile user )
    {
        this.user = user;
    }

    public OffsetDateTime getActiveTime()
    {
        return activeTime;
    }

    public void setActiveTime( OffsetDateTime activeTime )
    {
        this.activeTime = activeTime;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.activeTime != null ? this.activeTime.toInstant().toEpochMilli() : 0;
    }
}
