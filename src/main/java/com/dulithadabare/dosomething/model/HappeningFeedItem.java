package com.dulithadabare.dosomething.model;

public class HappeningFeedItem extends ActiveFeedItem
{
    private ConfirmedEvent confirmedEvent;
    private int activeFriendCount;
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

    public int getActiveFriendCount()
    {
        return activeFriendCount;
    }

    public void setActiveFriendCount( int activeFriendCount )
    {
        this.activeFriendCount = activeFriendCount;
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
