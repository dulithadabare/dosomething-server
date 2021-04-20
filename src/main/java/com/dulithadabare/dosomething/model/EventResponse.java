package com.dulithadabare.dosomething.model;

import java.util.ArrayList;
import java.util.List;

public class EventResponse
{
    private Event event;
    private ConfirmedEvent confirmedEvent;
    private List<InterestedFriend> interestedFriendList = new ArrayList<>();
    private boolean isInterested;
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

    public List<InterestedFriend> getInterestedFriendList()
    {
        return interestedFriendList;
    }

    public void setInterestedFriendList( List<InterestedFriend> interestedFriendList )
    {
        this.interestedFriendList = interestedFriendList;
    }

    public boolean isInterested()
    {
        return isInterested;
    }

    public void setInterested( boolean interested )
    {
        isInterested = interested;
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
