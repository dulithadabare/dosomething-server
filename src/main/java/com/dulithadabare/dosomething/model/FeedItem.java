package com.dulithadabare.dosomething.model;

import java.util.ArrayList;
import java.util.List;

public class FeedItem
{
    private Event event;
    private ConfirmedEvent confirmedEvent;
    private int interestedFriendCount;
    private int participatingFriendCount;
    private boolean isAnonymous;
    private boolean isInterested;
    private List<InterestedFriend> interestedFriendList = new ArrayList<>();
    private List<Integer> requestedFriendList = new ArrayList<>();
    private List<UserProfile> visibleFriendList = new ArrayList<>();

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

    public List<UserProfile> getVisibleFriendList()
    {
        return visibleFriendList;
    }

    public void setVisibleFriendList( List<UserProfile> visibleFriendList )
    {
        this.visibleFriendList = visibleFriendList;
    }

    public List<InterestedFriend> getInterestedFriendList()
    {
        return interestedFriendList;
    }

    public void setInterestedFriendList( List<InterestedFriend> interestedFriendList )
    {
        this.interestedFriendList = interestedFriendList;
    }

    public List<Integer> getRequestedFriendList()
    {
        return requestedFriendList;
    }

    public void setRequestedFriendList( List<Integer> requestedFriendList )
    {
        this.requestedFriendList = requestedFriendList;
    }
}
