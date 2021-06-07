package com.dulithadabare.dosomething.model;

import java.time.OffsetDateTime;

public class NotificationModel
{
    private long eventId;
    private long friendId;
    private OffsetDateTime createdTimeUtc;

    public NotificationModel( long eventId, long friendId, OffsetDateTime createdTimeUtc )
    {
        this.eventId = eventId;
        this.friendId = friendId;
        this.createdTimeUtc = createdTimeUtc;
    }

    public long getEventId()
    {
        return eventId;
    }

    public void setEventId( long eventId )
    {
        this.eventId = eventId;
    }

    public long getFriendId()
    {
        return friendId;
    }

    public void setFriendId( long friendId )
    {
        this.friendId = friendId;
    }

    public OffsetDateTime getCreatedTimeUtc()
    {
        return createdTimeUtc;
    }

    public void setCreatedTimeUtc( OffsetDateTime createdTimeUtc )
    {
        this.createdTimeUtc = createdTimeUtc;
    }
}
