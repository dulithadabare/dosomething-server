package com.dulithadabare.dosomething.model;

import com.dulithadabare.dosomething.constant.AppNotificationType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.List;

public class EventNotification
{
    private long eventId;
    private List<Long> data;
    @JsonIgnore
    private OffsetDateTime timestampUtc;
    private AppNotificationType type;

    public long getEventId()
    {
        return eventId;
    }

    public void setEventId( long eventId )
    {
        this.eventId = eventId;
    }

    public List<Long> getData()
    {
        return data;
    }

    public void setData( List<Long> data )
    {
        this.data = data;
    }

    public OffsetDateTime getTimestampUtc()
    {
        return timestampUtc;
    }

    public void setTimestampUtc( OffsetDateTime timestampUtc )
    {
        this.timestampUtc = timestampUtc;
    }

    public AppNotificationType getType()
    {
        return AppNotificationType.NONE;
    }
}
