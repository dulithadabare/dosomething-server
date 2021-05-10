package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Event
{
    protected long id;
    protected int creatorId;
    protected String description;
    protected boolean isConfirmed;
    protected int interestedCount;
    protected int visibilityPreference;
    protected long createdTime;
    protected long updatedTime;
    protected List<String> tagList = new ArrayList<>();

    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getInt( "creator_id" );
        this.description = rs.getString( "description" );
        this.isConfirmed = rs.getBoolean( "is_confirmed" );
        this.visibilityPreference = rs.getInt( "visibility_preference" );
        this.createdTime = rs.getTimestamp( "created_time" ).getTime();
        this.updatedTime = rs.getTimestamp( "updated_time" ).getTime();
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

    public int getVisibilityPreference()
    {
        return visibilityPreference;
    }

    public void setVisibilityPreference( int visibilityPreference )
    {
        this.visibilityPreference = visibilityPreference;
    }

    public List<String> getTagList()
    {
        return tagList;
    }

    public void setTagList( List<String> tagList )
    {
        this.tagList = tagList;
    }

    public long getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( long createdTime )
    {
        this.createdTime = createdTime;
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
