package com.dulithadabare.dosomething.controller;
import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( "/users" )
public class UserController
{
    final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createAnonymousUser( @AuthenticationPrincipal Jwt jwt )
    {

        UserProfile authUser = new UserProfile();
        authUser.loadAnonymousUserProfileFromJwt( jwt );

        return dbResource.createAnonymousUser( authUser );
    }

    @CrossOrigin
    @PostMapping( "/facebook" )
    public HttpEntity<BasicResponse> linkWithFacebook( @RequestParam String accessToken, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadFacebookUserProfileFromJwt( jwt );

        return dbResource.linkWithFacebook( accessToken, authUser );
    }

    @CrossOrigin
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getUser( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadAnonymousUserProfileFromJwt( jwt );

        return dbResource.getCompleteUserProfileById( authUser.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/current-activity" )
    public HttpEntity<BasicResponse> getCurrentActivity( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadAnonymousUserProfileFromJwt( jwt );

        return dbResource.getCurrentActivityByUserId( authUser.getUserId() );
    }

    @CrossOrigin
    @PutMapping( "/location" )
    public HttpEntity<BasicResponse> updateUserLocation( @RequestBody UserLocation userLocation, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        if ( authUser.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.updateUserLocation( userLocation, authUser.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/visibility-requests" )
    public HttpEntity<BasicResponse> getVisibilityRequests( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        if ( authUser.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getVisibilityRequests( authUser.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/notifications" )
    public HttpEntity<BasicResponse> getEventNotifications( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        if ( authUser.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getEventNotifications( authUser.getUserId() );
    }
}
