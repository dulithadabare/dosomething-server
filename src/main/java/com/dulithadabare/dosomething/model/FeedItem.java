package com.dulithadabare.dosomething.model;

import java.util.ArrayList;
import java.util.List;

public class FeedItem
{
    private Event event;
    private int interestedFriendCount;
    private int participatingFriendCount;
    private boolean isAnonymous;
    private boolean isInterested;
    private List<UserProfile> interestedFriendList = new ArrayList<>();
    private List<Integer> requestedFriendList = new ArrayList<>();
    private List<UserProfile> visibleFriendList = new ArrayList<>();

    public Event getEventNeed()
    {
        return event;
    }

    public void setEventNeed( Event event )
    {
        this.event = event;
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

    public List<UserProfile> getInterestedFriendList()
    {
        return interestedFriendList;
    }

    public void setInterestedFriendList( List<UserProfile> interestedFriendList )
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
