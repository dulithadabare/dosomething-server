package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class InviteFriendPageItem
{
    private BasicProfile user;
    private String description;
    private String distance;
    private String relationship;
    private boolean isPeekSent;
    private boolean isPeekBack;
    private boolean isInvited;
    private OffsetDateTime createdTime;

    public BasicProfile getUser()
    {
        return user;
    }

    public void setUser( BasicProfile user )
    {
        this.user = user;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getDistance()
    {
        return distance;
    }

    public void setDistance( String distance )
    {
        this.distance = distance;
    }

    public String getRelationship()
    {
        return relationship;
    }

    public void setRelationship( String relationship )
    {
        this.relationship = relationship;
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

    public boolean isInvited()
    {
        return isInvited;
    }

    public void setInvited( boolean invited )
    {
        isInvited = invited;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.createdTime.toInstant().toEpochMilli();
    }
}
