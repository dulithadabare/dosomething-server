package com.dulithadabare.dosomething.model;

public class InterestedFriend
{
    private UserProfile user;
    private String description;
    private String distance;
    private boolean isVisibilityRequested;

    public UserProfile getUser()
    {
        return user;
    }

    public void setUser( UserProfile user )
    {
        this.user = user;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getDistance()
    {
        return distance;
    }

    public void setDistance( String distance )
    {
        this.distance = distance;
    }

    public boolean isVisibilityRequested()
    {
        return isVisibilityRequested;
    }

    public void setVisibilityRequested( boolean visibilityRequested )
    {
        isVisibilityRequested = visibilityRequested;
    }
}
