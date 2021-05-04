package com.dulithadabare.dosomething.model;

public class HappeningFeedItem
{
    private ConfirmedEvent event;
    private int activeFriendCount;
    private boolean isInvited;
    private boolean isParticipant;
    private boolean isActive;

    public ConfirmedEvent getEvent()
    {
        return event;
    }

    public void setEvent( ConfirmedEvent event )
    {
        this.event = event;
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

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean active )
    {
        isActive = active;
    }
}
