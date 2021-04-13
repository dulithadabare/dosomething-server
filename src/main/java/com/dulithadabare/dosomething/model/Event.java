package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Event
{
    public long id;
    public int creatorId;
    public String creatorDisplayName;
    public String activity;
    public boolean isActive;
    public int interestedCount;

    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getInt( "creator_id" );
        this.activity = rs.getString( "activity" );
        this.isActive = rs.getBoolean( "is_active" );
    }

    private void loadOptionalParams( ResultSet rs )
    {
        try
        {
            String interested = rs.getString( "interested" );

            this.interestedCount = interested != null ? interested.split( "," ).length : 0;
        }
        catch ( SQLException e )
        {
            System.out.println("Optional values for Event not found. Setting defaults");
        }
    }

    public void loadFromConfirmedEvent( ConfirmedEvent event )
    {
        this.id = event.getId();
        this.creatorId = event.getCreatorId();
        this.activity = event.getActivity();

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
}
