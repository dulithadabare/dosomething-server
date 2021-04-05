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
    public HttpEntity<BasicResponse> createEvent( @RequestBody EventNeed eventNeed, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        List<FeedItem> createdEvent = dbResource.createEvent( eventNeed, userProfile.getUserId() );

        return new HttpEntity<>( new BasicResponse( createdEvent ) );
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
    @GetMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> getEventById( long eventId, @AuthenticationPrincipal Jwt jwt )
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
//    @PutMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> updateEvent( long eventId, @RequestBody EventNeed eventNeed, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.updateEvent(  eventNeed, userProfile.getUserId() ) ));
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/users" )
    public HttpEntity<BasicResponse> addEventInterest( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        List<FeedItem> updatedEvent = dbResource.addEventInterest( eventId, userProfile.getUserId() );

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
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

        List<FeedItem> updatedEvent = dbResource.removeEventInterest( eventId, userProfile.getUserId() );

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
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

        List<FeedItem> updatedEvent = dbResource.sendVisibilityRequest( eventId, userProfile.getUserId(), friendId );

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
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

        List<FeedItem> updatedEvent = dbResource.addEventVisibility( eventId, userProfile.getUserId(), friendId );

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
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

        List<FeedItem> updatedEvent = dbResource.removeEventVisibility( eventId, userProfile.getUserId() );

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

}
