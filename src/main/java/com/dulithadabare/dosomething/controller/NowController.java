package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.BasicResponse;
import com.dulithadabare.dosomething.model.ConfirmedEvent;
import com.dulithadabare.dosomething.model.UserProfile;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( "/now" )
public class NowController
{
    private final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createConfirmedEvent( @RequestBody ConfirmedEvent event, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.createConfirmedEvent( event, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getNowPage( @AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getNowPage( userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> getConfirmedEventById( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getConfirmedEventById(  eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/active" )
    public HttpEntity<BasicResponse> joinEvent( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.joinEvent( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/active" )
    public HttpEntity<BasicResponse> leaveEvent( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.leaveEvent( userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}/active" )
    public HttpEntity<BasicResponse> getActiveFriendListByEventId( @PathVariable long eventId,
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

        return dbResource.getActivePage( eventId, userProfile.getUserId(), pageKey );
    }
}
