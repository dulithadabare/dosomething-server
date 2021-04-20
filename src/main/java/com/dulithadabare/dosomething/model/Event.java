package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Event
{
    public long id;
    public int creatorId;
    public String creatorDisplayName;
    public String activity;
    public String description;
    public boolean isActive;
    public int interestedCount;
    public long timestamp;

    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getInt( "creator_id" );
        this.activity = rs.getString( "activity" );
        this.description = rs.getString( "description" );
        this.isActive = rs.getBoolean( "is_active" );
        this.timestamp = rs.getTimestamp( "timestamp" ).getTime();
    }

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId( int creatorId ) {
        this.creatorId = creatorId;
    }

    public String getCreatorDisplayName()
    {
        return creatorDisplayName;
    }

    public void setCreatorDisplayName( String creatorDisplayName )
    {
        this.creatorDisplayName = creatorDisplayName;
    }

    public String getActivity()
    {
        return activity;
    }

    public void setActivity( String activity )
    {
        this.activity = activity;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean active )
    {
        isActive = active;
    }

    public int getInterestedCount()
    {
        return interestedCount;
    }

    public void setInterestedCount( int interestedCount )
    {
        this.interestedCount = interestedCount;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( long timestamp )
    {
        this.timestamp = timestamp;
    }
}
