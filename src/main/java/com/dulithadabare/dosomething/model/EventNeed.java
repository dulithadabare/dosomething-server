package com.dulithadabare.dosomething.model;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

public class EventNeed
{
    private long id;
    private int userId;
    private String name;
    private String activity;
    private String startDate;
    private String endDate;
    private String dateScope;
    private String startTime;
    private String endTime;
    private String timeScope;
    private int interestedCount;

    public void load( ResultSet rs) throws  SQLException{
        load( rs, false);
    }

    public void load( ResultSet rs, boolean isLoadOptional) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.userId = rs.getInt( "user_id" );
        this.activity = rs.getString( "activity" );

        Date startDate = rs.getDate( "start_date" );
        Date endDate = rs.getDate( "end_date" );

        this.startDate = startDate != null ? startDate.toString() : null;
        this.endDate = endDate != null ? endDate.toString() : null;
        this.dateScope = rs.getString( "date_scope" );

        Time startTime = rs.getTime( "start_time" );
        Time endTime = rs.getTime( "end_time" );

        this.startTime = startTime != null ? startTime.toString() : null;
        this.endTime = endTime != null ? endTime.toString() : null;
        this.timeScope = rs.getString( "time_scope" );

        if ( isLoadOptional )
        {
            loadOptionalParams( rs );
        }
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

    public String getActivity()
    {
        return activity;
    }

    public void setActivity( String activity )
    {
        this.activity = activity;
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

    public int getInterestedCount()
    {
        return interestedCount;
    }

    public void setInterestedCount( int interestedCount )
    {
        this.interestedCount = interestedCount;
    }
}
