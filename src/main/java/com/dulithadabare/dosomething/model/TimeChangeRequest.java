package com.dulithadabare.dosomething.model;

public class TimeChangeRequest extends ChangeRequest
{
    private String startTime;
    private String endTime;
    private String timeScope;

    public String getStartTime()
    {
        return startTime;
    }

    public void setStartTime( String startTime )
    {
        this.startTime = startTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime( String endTime )
    {
        this.endTime = endTime;
    }

    public String getTimeScope()
    {
        return timeScope;
    }

    public void setTimeScope( String timeScope )
    {
        this.timeScope = timeScope;
    }
}
