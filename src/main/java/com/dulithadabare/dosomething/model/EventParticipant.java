package com.dulithadabare.dosomething.model;

public class EventParticipant
{
    private UserProfile user;
    private int userId;
    private boolean isConfirmed;

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
    {
        this.user = user;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId( int userId )
    {
        this.userId = userId;
    }

    public boolean isConfirmed()
    {
        return isConfirmed;
    }

    public void setConfirmed( boolean confirmed )
    {
        isConfirmed = confirmed;
    }
}
