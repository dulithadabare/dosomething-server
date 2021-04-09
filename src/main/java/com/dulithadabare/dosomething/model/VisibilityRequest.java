package com.dulithadabare.dosomething.model;

public class VisibilityRequest extends EventNotification
{
    private UserProfile user;

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
    {
        this.user = user;
    }
}
