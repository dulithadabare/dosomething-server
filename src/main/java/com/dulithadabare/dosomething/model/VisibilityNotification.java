package com.dulithadabare.dosomething.model;

public class VisibilityNotification
{
    private UserProfile user;
    private EventNeed eventNeed;

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
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
