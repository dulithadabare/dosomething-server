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
    public List<EventParticipant> participantList;
    private int participantCount;

    public ConfirmedEvent()
    {
        this.isConfirmed = true;
    }

    @Override
    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getInt( "creator_id" );
        this.tag = rs.getString( "activity" );
        this.description = rs.getString( "description" );
        this.timestamp = rs.getTimestamp( "timestamp" ).getTime();

        Date date = rs.getDate( "date" );
        Time time = rs.getTime( "time" );

        this.date = date != null ? date.toString() : null;
        this.time = time != null ? time.toString() : null;

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

    public void loadFromResultSet( long eventId, int userId, DBResource dbResource, Connection conn, ResultSet rs ) throws SQLException
    {
        this.id = eventId;
        this.creatorId = rs.getInt( "creator_id" );
        this.tag = rs.getString( "activity" );

        Date date = rs.getDate( "date" );
        Time time = rs.getTime( "time" );

        this.date = date.toString();
        this.time = time.toString();

        Map<Integer, EventParticipant> participantMap = dbResource.getEventParticipants( eventId, conn );

        this.creatorDisplayName = participantMap.get( this.creatorId ).getUser().getDisplayName();
        this.participantList = new ArrayList<>( participantMap.values() );
        this.isPublic = rs.getBoolean( "is_public" );
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

    public int getParticipantCount()
    {
        return participantCount;
    }

    public void setParticipantCount( int participantCount )
    {
        this.participantCount = participantCount;
    }

    public List<EventParticipant> getParticipantList()
    {
        return participantList;
    }

    public void setParticipantList( List<EventParticipant> participantList )
    {
        this.participantList = participantList;
    }
}
