package com.dulithadabare.dosomething.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BasicProfile
{
    private Long userId;
    private String displayName;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId( Long userId )
    {
        this.userId = userId;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    public void loadFromResultSet( ResultSet rs ) throws SQLException
    {
        this.userId = rs.getLong( "id" );
        this.displayName = rs.getString( "name" );
    }
}
