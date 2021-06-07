package com.dulithadabare.dosomething.model;

public class VisibilityRequest extends EventNotification
{
    private String id;
    private UserProfile user;
    private long createdTimeUtc;

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
    {
        this.user = user;
    }

    public long getCreatedTimeUtc()
    {
        return createdTimeUtc;
    }

    public void setCreatedTimeUtc( long createdTimeUtc )
    {
        this.createdTimeUtc = createdTimeUtc;
    }
}
