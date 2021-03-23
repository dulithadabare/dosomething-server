package com.dulithadabare.dosomething.model;

public class FeedItem
{
    private EventNeed eventNeed;
    private int interestedFriendCount;
    private int participatingFriendCount;
    private boolean isParticipating;
    private boolean isInterested;
    private boolean isJoinRequested;
    private boolean isConfirmRequestReceived; // Is Event Participation Requested by OP

    public EventNeed getEventNeed()
    {
        return eventNeed;
    }

    public void setEventNeed( EventNeed eventNeed )
    {
        this.eventNeed = eventNeed;
    }

    public boolean isParticipating()
    {
        return isParticipating;
    }

    public void setParticipating( boolean participating )
    {
        isParticipating = participating;
    }

    public int getInterestedFriendCount()
    {
        return interestedFriendCount;
    }

    public void setInterestedFriendCount( int interestedFriendCount )
    {
        this.interestedFriendCount = interestedFriendCount;
    }

    public int getParticipatingFriendCount()
    {
        return participatingFriendCount;
    }

    public void setParticipatingFriendCount( int participatingFriendCount )
    {
        this.participatingFriendCount = participatingFriendCount;
    }

    public boolean isInterested()
    {
        return isInterested;
    }

    public void setInterested( boolean interested )
    {
        isInterested = interested;
    }

    public boolean isJoinRequested()
    {
        return isJoinRequested;
    }

    public void setJoinRequested( boolean joinRequested )
    {
        isJoinRequested = joinRequested;
    }

    public boolean isConfirmRequestReceived()
    {
        return isConfirmRequestReceived;
    }

    public void setConfirmRequestReceived( boolean confirmRequestReceived )
    {
        isConfirmRequestReceived = confirmRequestReceived;
    }
}
