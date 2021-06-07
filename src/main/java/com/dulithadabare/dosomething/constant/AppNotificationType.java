package com.dulithadabare.dosomething.constant;

public enum AppNotificationType
{
    NONE(0),
    EVENT_INTEREST(1),
    EVENT_PEEK(2),
    EVENT_INVITE(3),
    EVENT_INVITE_ACCEPT(4),
    EVENT_START(5),
    EVENT_JOIN(6),
    EVENT_JOIN_ACCEPT(7);

    private final int id;

    AppNotificationType( int id )
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

}
