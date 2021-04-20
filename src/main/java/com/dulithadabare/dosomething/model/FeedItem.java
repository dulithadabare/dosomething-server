package com.dulithadabare.dosomething.model;

public class FeedItem
{
    private Event event;
    private ConfirmedEvent confirmedEvent;
    private int interestedFriendCount;
    private int participatingFriendCount;
    private boolean isAnonymous;
    private boolean isInterested;
    private boolean isCreatorFriend;
    private boolean isFriendInterested;
    private boolean isInvited;
    private boolean isParticipant;

    public Event getEvent()
    {
        return event;
    }

    public void setEvent( Event event )
    {
        this.event = event;
    }

    public ConfirmedEvent getConfirmedEvent()
    {
        return confirmedEvent;
    }

    public void setConfirmedEvent( ConfirmedEvent confirmedEvent )
    {
        this.confirmedEvent = confirmedEvent;
    }

    public boolean isAnonymous()
    {
        return isAnonymous;
    }

    public void setAnonymous( boolean anonymous )
    {
        isAnonymous = anonymous;
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

    public boolean isCreatorFriend()
    {
        return isCreatorFriend;
    }

    public void setCreatorFriend( boolean creatorFriend )
    {
        isCreatorFriend = creatorFriend;
    }

    public boolean isFriendInterested()
    {
        return isFriendInterested;
    }

    public void setFriendInterested( boolean friendInterested )
    {
        isFriendInterested = friendInterested;
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
}
