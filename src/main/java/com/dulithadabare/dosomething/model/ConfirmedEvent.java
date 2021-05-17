package com.dulithadabare.dosomething.model;

import java.sql.*;
import java.util.Set;

public class ConfirmedEvent extends Event
{
    private String creatorDisplayName;
    private long eventId;
    private String date;
    private String time;
    private boolean isPublic;
    private boolean isHappening;
    private boolean isCancelled;
    private Set<Integer> invitedList;
    private int participantCount;

    public ConfirmedEvent()
    {
        this.isConfirmed = true;
    }

    @Override
    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.eventId = rs.getLong( "event_id" );
        this.creatorId = rs.getInt( "creator_id" );
        this.description = rs.getString( "description" );
        this.updatedTime = rs.getTimestamp( "updated_time" ).getTime();
        this.createdTime = rs.getTimestamp( "created_time" ).getTime();

        Date date = rs.getDate( "date" );
        Time time = rs.getTime( "time" );

        this.date = date != null ? date.toString() : null;
        this.time = time != null ? time.toString() : null;

        this.visibilityPreference = rs.getInt( "visibility_preference" );
        this.isPublic = rs.getBoolean( "is_public" );
        this.isHappening = rs.getBoolean( "is_happening" );
        this.isCancelled = rs.getBoolean( "is_cancelled" );
    }

    public void loadPrivateEvent( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.description = rs.getString( "description" );
        this.updatedTime = rs.getTimestamp( "updated_time" ).getTime();
        this.createdTime = rs.getTimestamp( "created_time" ).getTime();
        this.isHappening = rs.getBoolean( "is_happening" );
        this.creatorId = -1;
    }

    public String getCreatorDisplayName()
    {
        return creatorDisplayName;
    }

    public void setCreatorDisplayName( String creatorDisplayName )
    {
        this.creatorDisplayName = creatorDisplayName;
    }

    public long getEventId()
    {
        return eventId;
    }

    public void setEventId( long eventId )
    {
        this.eventId = eventId;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate( String date )
    {
        this.date = date;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime( String time )
    {
        this.time = time;
    }

    public boolean isPublic()
    {
        return isPublic;
    }

    public void setPublic( boolean aPublic )
    {
        isPublic = aPublic;
    }

    public boolean isHappening()
    {
        return isHappening;
    }

    public void setHappening( boolean happening )
    {
        isHappening = happening;
    }

    public int getParticipantCount()
    {
        return participantCount;
    }

    public void setParticipantCount( int participantCount )
    {
        this.participantCount = participantCount;
    }

    public Set<Integer> getInvitedList()
    {
        return invitedList;
    }

    public void setInvitedList( Set<Integer> invitedList )
    {
        this.invitedList = invitedList;
    }

    public boolean isCancelled()
    {
        return isCancelled;
    }

    public void setCancelled( boolean cancelled )
    {
        isCancelled = cancelled;
    }
}
