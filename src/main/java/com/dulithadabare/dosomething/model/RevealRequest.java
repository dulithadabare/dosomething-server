package com.dulithadabare.dosomething.model;

public class RevealRequest
{
    private UserModel user;
    private EventNeed eventNeed;

    public RevealRequest( UserModel user, EventNeed eventNeed )
    {
        this.user = user;
        this.eventNeed = eventNeed;
    }

    public UserModel getUser()
    {
        return user;
    }

    public void setUser( UserModel user )
    {
        this.user = user;
    }

    public EventNeed getEventNeed()
    {
        return eventNeed;
    }

    public void setEventNeed( EventNeed eventNeed )
    {
        this.eventNeed = eventNeed;
    }
}
