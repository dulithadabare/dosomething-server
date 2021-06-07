package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Event
{
    protected long id;
    protected Long creatorId;
    protected String description;
    protected boolean isConfirmed;
    protected int interestedCount;
    protected int visibilityPreference;
    protected OffsetDateTime createdTime;
    protected OffsetDateTime updatedTime;
    protected List<String> tagList = new ArrayList<>();

    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getLong( "creator_id" );
        this.description = rs.getString( "description" );
        this.isConfirmed = rs.getBoolean( "is_confirmed" );
        this.visibilityPreference = rs.getInt( "visibility_preference" );
        this.createdTime = rs.getObject( "created_time", OffsetDateTime.class );
        this.updatedTime = rs.getObject( "updated_time", OffsetDateTime.class );
    }

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId( Long creatorId ) {
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

    public OffsetDateTime getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( OffsetDateTime createdTime )
    {
        this.createdTime = createdTime;
    }

    public OffsetDateTime getUpdatedTime()
    {
        return updatedTime;
    }

    public void setUpdatedTime( OffsetDateTime updatedTime )
    {
        this.updatedTime = updatedTime;
    }
}
