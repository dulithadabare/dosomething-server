package com.dulithadabare.dosomething.model;

import com.dulithadabare.dosomething.resource.DBResource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfirmedEvent extends Event
{
    private long eventId;
    private String date;
    private String time;
    public boolean isPublic;
    public boolean isHappening;
    public List<InvitedUser> participantList;
    public List<InvitedUser> removedInvitedUserList;
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
        this.tag = rs.getString( "activity" );
        this.description = rs.getString( "description" );
        this.createdTime = rs.getTimestamp( "timestamp" ).getTime();

        Date date = rs.getDate( "date" );
        Time time = rs.getTime( "time" );

        this.date = date != null ? date.toString() : null;
        this.time = time != null ? time.toString() : null;

        this.visibilityPreference = rs.getInt( "visibility_preference" );
        this.isPublic = rs.getBoolean( "is_public" );
        this.isHappening = rs.getBoolean( "is_happening" );
    }

    public void loadPrivateEvent( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.tag = rs.getString( "activity" );
        this.isHappening = rs.getBoolean( "is_happening" );
        this.creatorId = -1;
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

    public List<InvitedUser> getParticipantList()
    {
        return participantList;
    }

    public void setParticipantList( List<InvitedUser> participantList )
    {
        this.participantList = participantList;
    }

    public List<InvitedUser> getRemovedInvitedUserList()
    {
        return removedInvitedUserList;
    }

    public void setRemovedInvitedUserList( List<InvitedUser> removedInvitedUserList )
    {
        this.removedInvitedUserList = removedInvitedUserList;
    }
}
