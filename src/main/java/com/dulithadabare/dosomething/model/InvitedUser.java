package com.dulithadabare.dosomething.model;

public class InvitedUser
{
    private BasicProfile user;
    private Long userId;
    private Boolean isConfirmed;
    private String distance;
    private String relationship;
    private boolean isVisibilityRequested;
    private boolean isFriend;

    public BasicProfile getUser()
    {
        return user;
    }

    public void setUser( BasicProfile user )
    {
        this.user = user;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId( Long userId )
    {
        this.userId = userId;
    }

    public Boolean isConfirmed()
    {
        return isConfirmed;
    }

    public void setConfirmed( Boolean confirmed )
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
