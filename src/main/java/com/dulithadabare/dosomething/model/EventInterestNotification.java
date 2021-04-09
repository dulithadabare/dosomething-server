package com.dulithadabare.dosomething.model;

import java.util.List;

public class EventInterestNotification extends EventNotification
{
    private List<UserProfile> interestedUserList;
    private int interestedUserCount;

    public List<UserProfile> getInterestedUserList()
    {
        return interestedUserList;
    }

    public void setInterestedUserList( List<UserProfile> interestedUserList )
    {
        this.interestedUserList = interestedUserList;
    }

    public int getInterestedUserCount()
    {
        return interestedUserCount;
    }

    public void setInterestedUserCount( int interestedUserCount )
    {
        this.interestedUserCount = interestedUserCount;
    }

    @Override
    public int getNotificationType()
    {
        return EVENT_INTEREST_NOTIFICATION;
    }
}
