package com.dulithadabare.dosomething.model;

import java.util.Set;

public class ConfirmedEvent extends Event
{
    private Set<Long> invitedList;

    public Set<Long> getInvitedList()
    {
        return invitedList;
    }

    public void setInvitedList( Set<Long> invitedList )
    {
        this.invitedList = invitedList;
    }
}
