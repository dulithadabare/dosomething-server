package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class ActivePageItem
{
    BasicProfile user;
    boolean isFriend;
    boolean isActive;
    OffsetDateTime activeTime;

    public BasicProfile getUser()
    {
        return user;
    }

    public void setUser( BasicProfile user )
    {
        this.user = user;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean active )
    {
        isActive = active;
    }

    public OffsetDateTime getActiveTime()
    {
        return activeTime;
    }

    public void setActiveTime( OffsetDateTime activeTime )
    {
        this.activeTime = activeTime;
    }

    public boolean isFriend()
    {
        return isFriend;
    }

    public void setFriend( boolean friend )
    {
        isFriend = friend;
    }
}
