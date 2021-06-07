package com.dulithadabare.dosomething.model;

public class ActiveUser
{
    Long participantId;
    boolean isFriend;
    boolean isActive;


    public Long getParticipantId()
    {
        return participantId;
    }

    public void setParticipantId( Long participantId )
    {
        this.participantId = participantId;
    }

    public boolean isFriend()
    {
        return isFriend;
    }

    public void setFriend( boolean friend )
    {
        isFriend = friend;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public void setActive( boolean active )
    {
        isActive = active;
    }
}
