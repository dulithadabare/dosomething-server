package com.dulithadabare.dosomething.model;

public class VisibilityRevealNotification extends EventNotification
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

    @Override
    public int getNotificationType()
    {
        return VISIBILITY_REVEAL_NOTIFICATION;
    }
}
