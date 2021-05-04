package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Event
{
    protected long id;
    protected int creatorId;
    protected String creatorDisplayName;
    protected String tag;
    protected String description;
    protected boolean isCancelled;
    protected boolean isConfirmed;
    protected int interestedCount;
    protected int visibilityPreference;
    protected int interestPreference;
    protected long createdTime;

    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getInt( "creator_id" );
        this.tag = rs.getString( "activity" );
        this.description = rs.getString( "description" );
        this.isCancelled = rs.getBoolean( "is_cancelled" );
        this.isConfirmed = rs.getBoolean( "is_confirmed" );
        this.visibilityPreference = rs.getInt( "visibility_preference" );
        this.interestPreference = rs.getInt( "interest_preference" );
        this.createdTime = rs.getTimestamp( "timestamp" ).getTime();
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

    public boolean isCancelled()
    {
        return isCancelled;
    }

    public void setCancelled( boolean cancelled )
    {
        isCancelled = cancelled;
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

    public int getInterestPreference()
    {
        return interestPreference;
    }

    public void setInterestPreference( int interestPreference )
    {
        this.interestPreference = interestPreference;
    }

    public long getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( long createdTime )
    {
        this.createdTime = createdTime;
    }
}
