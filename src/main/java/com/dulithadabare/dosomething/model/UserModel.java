package com.dulithadabare.dosomething.model;

public class UserModel
{
    private String facebookId;
    private String name;

    public UserModel( String facebookId, String name )
    {
        this.facebookId = facebookId;
        this.name = name;
    }

    public String getFacebookId()
    {
        return facebookId;
    }

    public void setFacebookId( String facebookId )
    {
        this.facebookId = facebookId;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }
}
