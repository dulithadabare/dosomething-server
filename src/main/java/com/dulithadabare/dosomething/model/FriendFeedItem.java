package com.dulithadabare.dosomething.model;

public class FriendFeedItem
{
    private BasicProfile user;
    private String activeEvent;
    private String lastActiveEvent;

    public BasicProfile getUser()
    {
        return user;
    }

    public void setUser( BasicProfile user )
    {
        this.user = user;
    }

    public String getActiveEvent()
    {
        return activeEvent;
    }

    public void setActiveEvent( String activeEvent )
    {
        this.activeEvent = activeEvent;
    }

    public String getLastActiveEvent()
    {
        return lastActiveEvent;
    }

    public void setLastActiveEvent( String lastActiveEvent )
    {
        this.lastActiveEvent = lastActiveEvent;
    }
}
