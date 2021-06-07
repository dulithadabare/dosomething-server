package com.dulithadabare.dosomething.model;

import com.dulithadabare.dosomething.constant.AppNotificationType;

public class EventPeekNotification extends EventNotification
{
    @Override
    public AppNotificationType getType()
    {
        return AppNotificationType.EVENT_PEEK;
    }
}

