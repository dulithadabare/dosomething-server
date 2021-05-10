package com.dulithadabare.dosomething.model;

public class PopularNearbyFeedItem
{
    private ConfirmedEvent confirmedEvent;
    private int activeFriendCount;
    private int activeCount;
    private boolean isRelevantEvent;
    private boolean isInvited;
    private boolean isParticipant;
    private boolean isCreatorFriend;
    private boolean isJoinRequested;
    private long firstActiveTimestamp;

    public ConfirmedEvent getConfirmedEvent()
    {
        return confirmedEvent;
    }

    public void setConfirmedEvent( ConfirmedEvent confirmedEvent )
    {
        this.confirmedEvent = confirmedEvent;
    }

    public int getActiveCount()
    {
        return activeCount;
    }

    public void setActiveCount( int activeCount )
    {
        this.activeCount = activeCount;
    }

    public int getActiveFriendCount()
    {
        return activeFriendCount;
    }

    public void setActiveFriendCount( int activeFriendCount )
    {
        this.activeFriendCount = activeFriendCount;
    }

    public boolean isRelevantEvent()
    {
        return isRelevantEvent;
    }

    public void setRelevantEvent( boolean relevantEvent )
    {
        isRelevantEvent = relevantEvent;
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

    public long getFirstActiveTimestamp()
    {
        return firstActiveTimestamp;
    }

    public void setFirstActiveTimestamp( long firstActiveTimestamp )
    {
        this.firstActiveTimestamp = firstActiveTimestamp;
    }
}
