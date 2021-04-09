package com.dulithadabare.dosomething.model;

public class EventNotification
{
    public static final int EVENT_INTEREST_NOTIFICATION = 1;
    public static final int VISIBILITY_REVEAL_NOTIFICATION = 2;
    public static final int EVENT_INVITE_NOTIFICATION = 3;

    private Event event;
    private int notificationType;

    public Event getEvent()
    {
        return event;
    }

    public void setEvent( Event event )
    {
        this.event = event;
    }

    public int getNotificationType()
    {
        return 0;
    }
}
