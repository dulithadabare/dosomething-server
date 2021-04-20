package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Activity
{
    public int creatorId;
    public String creatorDisplayName;
    public String tag;
    public String description;
    public boolean isActive;
    public long updatedTime;

    public void load( ResultSet rs ) throws SQLException
    {
        this.creatorId = rs.getInt( "creator_id" );
//        this.creatorDisplayName = rs.getString( "creator_name" );
        this.tag = rs.getString( "tag" );
        this.description = rs.getString( "description" );
        this.isActive = rs.getBoolean( "is_active" );
        this.updatedTime = rs.getTimestamp( "timestamp" ).getTime();
    }

    public int getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId( int creatorId )
    {
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
