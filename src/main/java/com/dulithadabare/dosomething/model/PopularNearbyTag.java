package com.dulithadabare.dosomething.model;

import java.util.List;

public class PopularNearbyTag
{
    private String tag;
    private int activeCount;;
    private int activeFriendCount;
    private List<PopularNearbyFeedItem> popularNearbyFeedItemList;

    public String getTag()
    {
        return tag;
    }

    public void setTag( String tag )
    {
        this.tag = tag;
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

    public List<PopularNearbyFeedItem> getPopularNearbyFeedItemList()
    {
        return popularNearbyFeedItemList;
    }

    public void setPopularNearbyFeedItemList( List<PopularNearbyFeedItem> popularNearbyFeedItemList )
    {
        this.popularNearbyFeedItemList = popularNearbyFeedItemList;
    }

}
