package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public class ActivityHistoryFeedItem
{
    private ConfirmedEvent event;
    private int activeCount;
    private boolean isInvited;
    private boolean isParticipant;
    private boolean isCreatorFriend;
    private boolean isJoinRequested;
    private OffsetDateTime activeTime;
    private List<BasicProfile> confirmedParticipantList;

    public ConfirmedEvent getEvent()
    {
        return event;
    }

    public void setEvent( ConfirmedEvent event )
    {
        this.event = event;
    }

    public int getActiveCount()
    {
        return activeCount;
    }

    public void setActiveCount( int activeCount )
    {
        this.activeCount = activeCount;
    }

    public boolean isInvited()
    {
        return isInvited;
    }

    public void setInvited( boolean invited )
    {
        isInvited = invited;
    }

    public boolean isParticipant()
    {
        return isParticipant;
    }

    public void setParticipant( boolean participant )
    {
        isParticipant = participant;
    }

    public boolean isCreatorFriend()
    {
        return isCreatorFriend;
    }

    public void setCreatorFriend( boolean creatorFriend )
    {
        isCreatorFriend = creatorFriend;
    }

    public boolean isJoinRequested()
    {
        return isJoinRequested;
    }

    public void setJoinRequested( boolean joinRequested )
    {
        isJoinRequested = joinRequested;
    }

    public OffsetDateTime getActiveTime()
    {
        return activeTime;
    }

    public void setActiveTime( OffsetDateTime activeTime )
    {
        this.activeTime = activeTime;
    }

    public List<BasicProfile> getConfirmedParticipantList()
    {
        return confirmedParticipantList;
    }

    public void setConfirmedParticipantList( List<BasicProfile> confirmedParticipantList )
    {
        this.confirmedParticipantList = confirmedParticipantList;
    }

    @JsonProperty("timestampUtc")
    public long getTimestampUtc()
    {
        return this.activeTime.toInstant().toEpochMilli();
    }
}
