package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class InterestedFriendPageItem
{
    private BasicProfile user;
    private boolean isPeekSent;
    private boolean isPeekBack;
    private OffsetDateTime createdTime;

    public BasicProfile getUser()
    {
        return user;
    }

    public void setUser( BasicProfile user )
    {
        this.user = user;
    }

    public boolean isPeekSent()
    {
        return isPeekSent;
    }

    public void setPeekSent( boolean peekSent )
    {
        isPeekSent = peekSent;
    }

    public boolean isPeekBack()
    {
        return isPeekBack;
    }

    public void setPeekBack( boolean peekBack )
    {
        isPeekBack = peekBack;
    }

    public OffsetDateTime getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( OffsetDateTime createdTime )
    {
        this.createdTime = createdTime;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.createdTime.toInstant().toEpochMilli();
    }
}
