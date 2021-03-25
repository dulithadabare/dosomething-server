package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EventNeed
{
    private long id;
    private int userId;
    private String name;
    private String verb;
    private String noun;
    private String startDate;
    private String endDate;
    private String dateScope;
    private String startTime;
    private String endTime;
    private String timeScope;
    private boolean isConfirmed;
    private List<UserProfile> participantList = new ArrayList<>();
    private int interestedCount;
    private int participatingCount;

    public void load( ResultSet rs) throws  SQLException{
        load( rs, false);
    }

    public void load( ResultSet rs, boolean isLoadOptional) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.userId = rs.getInt( "user_id" );
        this.verb = rs.getString( "verb" );
        this.noun = rs.getString( "noun" );
        this.startDate = rs.getDate( "start_date" ).toString();
        this.endDate = rs.getDate( "end_date" ).toString();
        this.dateScope = rs.getString( "date_scope" );
        this.startTime = rs.getTime( "start_time" ).toString();
        this.endTime = rs.getTime( "end_time" ).toString();
        this.timeScope = rs.getString( "time_scope" );
        this.isConfirmed = rs.getBoolean( "is_confirmed" );

        if ( isLoadOptional )
        {
            loadOptionalParams( rs );
        }
    }

    private void loadOptionalParams( ResultSet rs )
    {
        try
        {
            String participants = rs.getString( "participants" );
            String interested = rs.getString( "interested" );

            this.interestedCount = interested != null ? interested.split( "," ).length : 0;
            this.participatingCount =  participants != null ? participants.split( "," ).length : 0;
        }
        catch ( SQLException e )
        {
            System.out.println("Optional values for Event not found. Setting defaults");
        }
    }

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId( int userId) {
        this.userId = userId;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb( String verb ) {
        this.verb = verb;
    }

    public String getNoun()
    {
        return noun;
    }

    public void setNoun( String noun )
    {
        this.noun = noun;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDateScope()
    {
        return dateScope;
    }

    public void setDateScope( String dateScope )
    {
        this.dateScope = dateScope;
    }

    public String getStartTime()
    {
        return startTime;
    }

    public void setStartTime( String startTime )
    {
        this.startTime = startTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime( String endTime )
    {
        this.endTime = endTime;
    }

    public String getTimeScope()
    {
        return timeScope;
    }

    public void setTimeScope( String timeScope )
    {
        this.timeScope = timeScope;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed(boolean confirmed) {
        isConfirmed = confirmed;
    }

    public List<UserProfile> getParticipantList()
    {
        return participantList;
    }

    public int getInterestedCount()
    {
        return interestedCount;
    }

    public void setInterestedCount( int interestedCount )
    {
        this.interestedCount = interestedCount;
    }

    public int getParticipatingCount()
    {
        return participatingCount;
    }

    public void setParticipatingCount( int participatingCount )
    {
        this.participatingCount = participatingCount;
    }

    public void setParticipantList( List<UserProfile> participantList )
    {
        this.participantList = participantList;
    }
}
