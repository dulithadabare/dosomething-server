package com.dulithadabare.dosomething.model;

public class ActiveUser
{
    UserProfile user;
    long createdTime;

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
    {
        this.user = user;
    }

    public long getCreatedTime()
    {
        return createdTime;
    }

    public void setCreatedTime( long createdTime )
    {
        this.createdTime = createdTime;
    }
}
