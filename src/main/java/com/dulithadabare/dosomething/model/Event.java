package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Event
{
    public long id;
    public int creatorId;
    public String creatorDisplayName;
    public String tag;
    public String description;
    public boolean isConfirmed;
    public int interestedCount;
    public long timestamp;

    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getInt( "creator_id" );
        this.tag = rs.getString( "activity" );
        this.description = rs.getString( "description" );
        this.isConfirmed = rs.getBoolean( "is_confirmed" );
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

    public String getTag()
    {
        return tag;
    }

    public void setTag( String tag )
    {
        this.tag = tag;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public boolean isConfirmed()
    {
        return isConfirmed;
    }

    public void setConfirmed( boolean confirmed )
    {
        isConfirmed = confirmed;
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
