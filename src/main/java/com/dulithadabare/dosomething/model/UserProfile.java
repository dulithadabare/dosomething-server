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
    private String email;
    @JsonIgnore
    private double longitude;
    @JsonIgnore
    private double latitude;
    @JsonIgnore
    private int highSchoolId;
    @JsonIgnore
    private int universityId;
    @JsonIgnore
    private int workPLaceId;

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

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public int getHighSchoolId()
    {
        return highSchoolId;
    }

    public void setHighSchoolId( int highSchoolId )
    {
        this.highSchoolId = highSchoolId;
    }

    public int getUniversityId()
    {
        return universityId;
    }

    public void setUniversityId( int universityId )
    {
        this.universityId = universityId;
    }

    public int getWorkPLaceId()
    {
        return workPLaceId;
    }

    public void setWorkPLaceId( int workPLaceId )
    {
        this.workPLaceId = workPLaceId;
    }

    public void loadUserProfileFromJwt( Jwt jwt )
    {
         this.firebaseUid = jwt.getClaimAsString( "user_id" );
         this.userId = jwt.getClaim( "sub" );
    }

    public void loadFacebookUserProfileFromJwt( Jwt jwt )
    {
        JSONObject firebase = ((JSONObject)jwt.getClaims().get( "firebase" ));
        this.firebaseUid = jwt.getClaimAsString( "user_id" );
        this.facebookId = String.valueOf((( JSONArray )((JSONObject)firebase.get( "identities" )).get( "facebook.com" )).get( 0 ));
        this.displayName = jwt.getClaimAsString( "name" );
        this.userId = jwt.getClaim( "sub" );
    }

    public void loadAnonymousUserProfileFromJwt( Jwt jwt )
    {
        this.firebaseUid = jwt.getClaimAsString( "user_id" );
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

    public void loadCompleteProfileFromResultSet( ResultSet rs ) throws SQLException
    {
        this.userId = rs.getInt( "id" );
        this.firebaseUid = rs.getString( "firebase_uid" );
        this.facebookId = rs.getString( "facebook_id" );
        this.displayName = rs.getString( "name" );
        this.email = rs.getString( "email" );
        this.longitude = rs.getDouble( "longitude" );
        this.latitude = rs.getDouble( "latitude" );
        this.highSchoolId = rs.getInt( "high_school_id" );
        this.universityId = rs.getInt( "university_id" );
        this.workPLaceId = rs.getInt( "work_place_id" );
    }

}
