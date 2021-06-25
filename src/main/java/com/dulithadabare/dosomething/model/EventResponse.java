package com.dulithadabare.dosomething.model;

public class EventResponse
{
    private Event event;
    private boolean isInterested;
    private boolean isActive;

    public Event getEvent()
    {
        return event;
    }

    public void setEvent( Event event )
    {
        this.event = event;
    }

    public boolean isInterested()
    {
        return isInterested;
    }

    public void setInterested( boolean interested )
    {
        isInterested = interested;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean active )
    {
        isActive = active;
    }
}
