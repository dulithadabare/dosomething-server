package com.dulithadabare.dosomething.model;

import com.dulithadabare.dosomething.constant.AppNotificationType;

public class EventStartNotification extends EventNotification
{
    @Override
    public AppNotificationType getType()
    {
        return AppNotificationType.EVENT_START;
    }
}