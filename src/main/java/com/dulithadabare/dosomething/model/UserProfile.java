package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.oauth2.jwt.Jwt;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserProfile
{
    private Long userId;
    private String firebaseUid;
    private String facebookId;
    private String displayName;
    private String email;
    @JsonIgnore
    private double longitude;
    @JsonIgnore
    private double latitude;

    public UserProfile()
    {

    }

    public UserProfile( Long userId, String firebaseUid, String facebookId, String displayName )
    {
        this.userId = userId;
        this.firebaseUid = firebaseUid;
        this.facebookId = facebookId;
        this.displayName = displayName;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId( Long userId )
    {
        this.userId = userId;
    }

    public String getFirebaseUid()
    {
        return firebaseUid;
    }

    public void setFirebaseUid( String firebaseUid )
    {
        this.firebaseUid = firebaseUid;
    }

    public String getFacebookId()
    {
        return facebookId;
    }

    public void setFacebookId( String facebookId )
    {
        this.facebookId = facebookId;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public void setLongitude( double longitude )
    {
        this.longitude = longitude;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public void setLatitude( double latitude )
    {
        this.latitude = latitude;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public void loadUserProfileFromJwt( Jwt jwt )
    {
         this.firebaseUid = jwt.getClaimAsString( "user_id" );
         this.userId = jwt.getClaim( "sub" );
    }

    public void loadFromResultSet( ResultSet rs ) throws SQLException
    {
        this.userId = rs.getLong( "id" );
        this.displayName = rs.getString( "name" );
    }

    public void loadCompleteProfileFromResultSet( ResultSet rs ) throws SQLException
    {
        this.userId = rs.getLong( "id" );
        this.firebaseUid = rs.getString( "firebase_uid" );
        this.facebookId = rs.getString( "facebook_id" );
        this.displayName = rs.getString( "name" );
        this.email = rs.getString( "email" );
        this.longitude = rs.getDouble( "longitude" );
        this.latitude = rs.getDouble( "latitude" );
    }
}
