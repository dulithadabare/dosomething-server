package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class CurrentActivity
{
    private Long userId;
    private Long eventId;
    private boolean isActive;
    private OffsetDateTime updatedTime;

    public void load( ResultSet rs ) throws SQLException
    {
        this.userId = rs.getLong( "user_id" );
        this.eventId = rs.getLong( "event_id" );
        this.updatedTime = rs.getObject( "updated_time", OffsetDateTime.class );
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId( Long userId )
    {
        this.userId = userId;
    }

    public Long getEventId()
    {
        return eventId;
    }

    public void setEventId( Long eventId )
    {
        this.eventId = eventId;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean active )
    {
        isActive = active;
    }

    public OffsetDateTime getUpdatedTime()
    {
        return updatedTime;
    }

    public void setUpdatedTime( OffsetDateTime updatedTime )
    {
        this.updatedTime = updatedTime;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.updatedTime.toInstant().toEpochMilli();
    }
}
