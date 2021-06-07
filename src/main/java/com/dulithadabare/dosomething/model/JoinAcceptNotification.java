package com.dulithadabare.dosomething.model;

import com.dulithadabare.dosomething.constant.AppNotificationType;

public class JoinAcceptNotification extends EventNotification
{
    @Override
    public AppNotificationType getType()
    {
        return AppNotificationType.EVENT_JOIN_ACCEPT;
    }
}