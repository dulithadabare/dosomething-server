package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class AppNotification
{
    private long id;
    private String message;
    private String payload;
    private OffsetDateTime createdDt;
    private int type;

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    public String getPayload()
    {
        return payload;
    }

    public void setPayload( String payload )
    {
        this.payload = payload;
    }

    public OffsetDateTime getCreatedDt()
    {
        return createdDt;
    }

    public void setCreatedDt( OffsetDateTime createdDt )
    {
        this.createdDt = createdDt;
    }

    public int getType()
    {
        return type;
    }

    public void setType( int type )
    {
        this.type = type;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.createdDt.toInstant().toEpochMilli();
    }
}
