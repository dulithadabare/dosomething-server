package com.dulithadabare.dosomething.controller;
import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping( "/users" )
public class UserController
{
    final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createUser( @RequestParam String accessToken, @AuthenticationPrincipal Jwt jwt )
    {

        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        Integer userId = userProfile.getUserId();
        boolean isNewUser = userId < 0;

        if ( isNewUser )
        {
            return dbResource.createUser( accessToken, userProfile );
        }

        return new HttpEntity<>( new BasicResponse( userId ) );
    }

    @CrossOrigin
    @PutMapping( "/location" )
    public HttpEntity<BasicResponse> updateUserLocation( @RequestBody UserLocation userLocation, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.updateUserLocation( userLocation, userProfile.getUserId() ) ) );
    }

    @CrossOrigin
    @GetMapping( "/visibility-requests" )
    public HttpEntity<BasicResponse> getVisibilityRequests( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getVisibilityRequests( userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/notifications" )
    public HttpEntity<BasicResponse> getNotificationsByType( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getVisibilityNotifications( userProfile.getUserId() );
    }

}
