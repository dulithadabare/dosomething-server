package com.dulithadabare.dosomething.model;

public class InvitedUser
{
    private UserProfile user;
    private int userId;
    private boolean isConfirmed;
    private String distance;
    private String relationship;
    private boolean isVisibilityRequested;
    private boolean isFriend;

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

    public String getDistance()
    {
        return distance;
    }

    public void setDistance( String distance )
    {
        this.distance = distance;
    }

    public String getRelationship()
    {
        return relationship;
    }

    public void setRelationship( String relationship )
    {
        this.relationship = relationship;
    }

    public boolean isVisibilityRequested()
    {
        return isVisibilityRequested;
    }

    public void setVisibilityRequested( boolean visibilityRequested )
    {
        isVisibilityRequested = visibilityRequested;
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
