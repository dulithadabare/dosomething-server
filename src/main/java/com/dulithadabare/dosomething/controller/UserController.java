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
    public HttpEntity<BasicResponse> createAnonymousUser( @AuthenticationPrincipal Jwt jwt, @RequestHeader( "X-USER-TIMEZONE" ) String userTimeZone )
    {

        UserProfile authUser = new UserProfile();
        authUser.loadAnonymousUserProfileFromJwt( jwt );

        return dbResource.createAnonymousUser( authUser, userTimeZone );
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
    @PutMapping( "/timezone" )
    public HttpEntity<BasicResponse> updateUserTimeZone( @RequestParam( name = "timezone" ) String timezone, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        if ( authUser.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.updateUserTimezone( timezone, authUser.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/friends" )
    public HttpEntity<BasicResponse> getFriendsForUser( @AuthenticationPrincipal Jwt jwt,
                                                             @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getFriendList( userProfile.getUserId(), pageKey );
    }


    @CrossOrigin
    @GetMapping( "/friends/{friendId}" )
    public HttpEntity<BasicResponse> getFriendActivity( @PathVariable Long friendId,
                                                        @AuthenticationPrincipal Jwt jwt,
                                                        @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getFriendActivityList( userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @GetMapping( "/events" )
    public HttpEntity<BasicResponse> getEventsCreatedByUser( @AuthenticationPrincipal Jwt jwt,
                                                             @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getEventsForUser( userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @GetMapping( "/confirmed-events" )
    public HttpEntity<BasicResponse> getConfirmedEventsCreatedByUser( @AuthenticationPrincipal Jwt jwt,
                                                             @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getConfirmedEventsForUser( userProfile.getUserId(), pageKey );
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
    public HttpEntity<BasicResponse> getAppNotifications( @AuthenticationPrincipal Jwt jwt,
                                                          @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        if ( authUser.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getAppNotifications( authUser.getUserId(), pageKey );
    }
}
