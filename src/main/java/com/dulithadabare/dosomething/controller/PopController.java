package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.BasicResponse;
import com.dulithadabare.dosomething.model.Event;
import com.dulithadabare.dosomething.model.UserProfile;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( "/pops" )
public class PopController
{
    private final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> addPop( @RequestBody Event event, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.createEvent( event, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getPops( @AuthenticationPrincipal Jwt jwt,
                                              @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getPopsPage( userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> getPopById( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.loadEventResponseById(  eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}/interested" )
    public HttpEntity<BasicResponse> getInterested( @PathVariable long eventId,
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

        return dbResource.getInterestedPage(  eventId, userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/interested" )
    public HttpEntity<BasicResponse> addInterest( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.addEventInterest( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/interested" )
    public HttpEntity<BasicResponse> removeInterest( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.removeEventInterest( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/peek/{friendId}" )
    public HttpEntity<BasicResponse> peek( @PathVariable long eventId, @PathVariable int friendId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.sendVisibilityRequest( eventId, userProfile.getUserId(), friendId );
    }
}
