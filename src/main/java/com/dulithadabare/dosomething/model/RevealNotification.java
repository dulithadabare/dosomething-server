package com.dulithadabare.dosomething.model;

public class RevealNotification
{
    private UserModel user;
    private EventNeed eventNeed;

    public RevealNotification( UserModel user, EventNeed eventNeed )
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
