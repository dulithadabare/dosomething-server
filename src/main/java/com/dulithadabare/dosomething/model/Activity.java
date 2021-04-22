package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Activity
{
    private int userId;
    private long eventId;
    private boolean isActive;
    private long updatedTime;

    public void load( ResultSet rs ) throws SQLException
    {
        this.userId = rs.getInt( "user_id" );
        this.eventId = rs.getLong( "event_id" );
        this.updatedTime = rs.getTimestamp( "updated_time" ).getTime();
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId( int userId )
    {
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

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean active )
    {
        isActive = active;
    }

    public long getUpdatedTime()
    {
        return updatedTime;
    }

    public void setUpdatedTime( long updatedTime )
    {
        this.updatedTime = updatedTime;
    }
}
