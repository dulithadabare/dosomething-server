package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class Event
{
    protected long id;
    protected Long creatorId;
    protected String description;
    protected OffsetDateTime createdTime;

    public void load( ResultSet rs ) throws SQLException
    {
        this.id = rs.getLong( "id" );
        this.creatorId = rs.getLong( "creator_id" );
        this.description = rs.getString( "description" );
        this.createdTime = rs.getObject( "created_time", OffsetDateTime.class );
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

    public OffsetDateTime getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( OffsetDateTime createdTime )
    {
        this.createdTime = createdTime;
    }
}
