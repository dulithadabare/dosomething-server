package com.dulithadabare.dosomething.model;

import java.util.List;

public class ActivityFeedItem
{
    private String tag;
    private int activeFriendCount;
    private List<ActivityItem> activityItemList;

    public String getTag()
    {
        return tag;
    }

    public void setTag( String tag )
    {
        this.tag = tag;
    }

    public int getActiveFriendCount()
    {
        return activeFriendCount;
    }

    public void setActiveFriendCount( int activeFriendCount )
    {
        this.activeFriendCount = activeFriendCount;
    }

    public List<ActivityItem> getActivityItemList()
    {
        return activityItemList;
    }

    public void setActivityItemList( List<ActivityItem> activityItemList )
    {
        this.activityItemList = activityItemList;
    }
}
