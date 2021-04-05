package com.dulithadabare.dosomething.model;

public class ChangeRequest
{
    private long id;
    private long eventNeedId;
    private int userId;
    private int voteCount;
    private boolean isVoted;

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    public long getEventNeedId()
    {
        return eventNeedId;
    }

    public void setEventNeedId( long eventNeedId )
    {
        this.eventNeedId = eventNeedId;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId( int userId )
    {
        this.userId = userId;
    }

    public int getVoteCount()
    {
        return voteCount;
    }

    public void setVoteCount( int voteCount )
    {
        this.voteCount = voteCount;
    }

    public boolean isVoted()
    {
        return isVoted;
    }

    public void setVoted( boolean voted )
    {
        isVoted = voted;
    }
}
