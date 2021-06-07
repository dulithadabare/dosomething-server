package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.model.EventNotification;

@FunctionalInterface
public interface EventNotificationFactory
{
    EventNotification construct();
}