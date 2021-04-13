package com.dulithadabare.dosomething.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.security.oauth2.jwt.Jwt;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserProfile
{
    private Integer userId;
    private String firebaseUid;
    private String facebookId;
    private String displayName;
    @JsonIgnore
    private double longitude;
    @JsonIgnore
    private double latitude;

    public UserProfile()
    {

    }

    public UserProfile( Integer userId, String firebaseUid, String facebookId, String displayName )
    {
        this.userId = userId;
        this.firebaseUid = firebaseUid;
        this.facebookId = facebookId;
        this.displayName = displayName;
    }

    public Integer getUserId()
    {
        return userId;
    }

    public void setUserId( Integer userId )
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

    public void loadUserProfileFromJwt( Jwt jwt )
    {
        JSONObject firebase = ((JSONObject)jwt.getClaims().get( "firebase" ));
         this.firebaseUid = jwt.getClaimAsString( "user_id" );
         this.facebookId = String.valueOf((( JSONArray )((JSONObject)firebase.get( "identities" )).get( "facebook.com" )).get( 0 ));
        //TODO Cannot displayName from the JWT in Linking anonymous with Facebook flow
         this.displayName = jwt.getClaimAsString( "name" );
         this.userId = jwt.getClaim( "sub" );
    }

    public void loadFromResultSet( ResultSet rs ) throws SQLException
    {
        this.userId = rs.getInt( "id" );
//        this.firebaseUid = rs.getString( "firebase_uid" );
//        this.facebookId = rs.getString( "facebook_id" );
        this.displayName = rs.getString( "name" );
//        this.longitude = rs.getDouble( "longitude" );
//        this.latitude = rs.getDouble( "latitude" );
    }

}
