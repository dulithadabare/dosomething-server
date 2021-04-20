package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping( "/events" )
public class EventController
{
    private final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createEvent( @RequestBody Event event, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.createEvent( event, userProfile.getUserId() ) ) );
    }

    @CrossOrigin
    @PostMapping( "/confirmed-events" )
    public HttpEntity<BasicResponse> createConfirmedEvent( @RequestBody ConfirmedEvent event, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.createConfirmedEvent( event, userProfile.getUserId() ) ) );
    }

    @CrossOrigin
    @PutMapping( "/confirmed-events" )
    public HttpEntity<BasicResponse> updateConfirmedEvent( @RequestBody ConfirmedEvent event, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse(  dbResource.updateConfirmedEvent( event, userProfile.getUserId() ) ) );
    }

    @CrossOrigin
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getEventsCreatedByUser( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        List<FeedItem> feedModelList = dbResource.getEventsCreatedByUser( userProfile.getUserId() );

        return new HttpEntity<>( new BasicResponse( feedModelList ) );
    }

    @CrossOrigin
    @GetMapping( "/tags/{tag}" )
    public HttpEntity<BasicResponse> getEventsByTag( @PathVariable String tag, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        String hashTag = "#" + tag;

        List<FeedItem> feedModelList = dbResource.getEventsByTag( userProfile.getUserId(), hashTag );

        return new HttpEntity<>( new BasicResponse( feedModelList ) );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> getEventById( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.getEventById(  eventId, userProfile.getUserId() ) ));
    }

    @CrossOrigin
    @GetMapping( "/confirmed-events/{eventId}" )
    public HttpEntity<BasicResponse> getConfirmedEventById( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.getConfirmedEventById(  eventId, userProfile.getUserId() ) ));
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/users" )
    public HttpEntity<BasicResponse> addEventInterest( @PathVariable long eventId, @RequestBody EventInterest eventInterest, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        eventInterest.setEventId( eventId );
        eventInterest.setUserId( userProfile.getUserId() );

        return new HttpEntity<>( new BasicResponse( dbResource.addEventInterest( eventId, userProfile.getUserId(), eventInterest, false ) ) );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/users" )
    public HttpEntity<BasicResponse> removeEventInterest( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.removeEventInterest( eventId, userProfile.getUserId() ) ) );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/visibility-requests/{friendId}" )
    public HttpEntity<BasicResponse> sendVisibilityRequest( @PathVariable long eventId, @PathVariable int friendId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.sendVisibilityRequest( eventId, userProfile.getUserId(), friendId ) ) );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/visibility/{friendId}" )
    public HttpEntity<BasicResponse> addEventVisibility( @PathVariable long eventId, @PathVariable int friendId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.addEventVisibility( eventId, userProfile.getUserId(), friendId ) ) );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/participants/{creatorId}" )
    public HttpEntity<BasicResponse> acceptEventInvite( @PathVariable long eventId, @PathVariable int creatorId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.acceptEventInvite( eventId, userProfile.getUserId(), creatorId ) ) );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/visibility" )
    public HttpEntity<BasicResponse> removeEventVisibility( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.removeEventVisibility( eventId, userProfile.getUserId() ) ) );
    }

}
