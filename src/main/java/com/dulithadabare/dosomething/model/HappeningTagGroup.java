package com.dulithadabare.dosomething.model;

import java.util.List;

public class HappeningTagGroup
{
    private String tag;
    private int activeFriendCount;
    private List<HappeningFeedItem> happeningFeedItemList;

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

    public List<HappeningFeedItem> getHappeningFeedItemList()
    {
        return happeningFeedItemList;
    }

    public void setHappeningFeedItemList( List<HappeningFeedItem> happeningFeedItemList )
    {
        this.happeningFeedItemList = happeningFeedItemList;
    }
}
