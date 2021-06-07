package com.dulithadabare.dosomething.model;

import com.dulithadabare.dosomething.constant.AppNotificationType;

public class InviteAcceptNotification extends EventNotification
{
    @Override
    public AppNotificationType getType()
    {
        return AppNotificationType.EVENT_INVITE_ACCEPT;
    }
}
