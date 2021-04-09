package com.dulithadabare.dosomething.model;

public class JoinRequest
{
    private UserProfile user;
    private Event event;

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
    {
        this.user = user;
    }

    public Event getEventNeed()
    {
        return event;
    }

    public void setEventNeed( Event event )
    {
        this.event = event;
    }
}
