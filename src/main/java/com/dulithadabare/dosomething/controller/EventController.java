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
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getEventsCreatedByUser( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        List<Integer> userIdList = new ArrayList<>();
        userIdList.add( userProfile.getUserId() );
        List<FeedItem> feedModelList = dbResource.getEventByUserIdList(userIdList, userProfile.getUserId() );

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
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createEvent( @RequestBody EventNeed eventNeed, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        List<FeedItem> createdEvent = dbResource.createEvent( eventNeed, userProfile.getUserId() );

        return new HttpEntity<>( new BasicResponse( createdEvent ) );
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
    @PutMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> confirmEvent( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.confirmEventInterest( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/participants" )
    public HttpEntity<BasicResponse> confirmEventParticipation( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.confirmEventParticipation( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/participants" )
    public HttpEntity<BasicResponse> removeEventParticipation( @PathVariable int eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.removeEventParticipation( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/join-requests" )
    public HttpEntity<BasicResponse> joinConfirmedEvent( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.joinConfirmedEvent( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/participants/{userId}" )
    public HttpEntity<BasicResponse> confirmJoinRequest( @PathVariable long eventId, @PathVariable int userId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.confirmJoinRequest( eventId, userId, userProfile.getUserId() );
    }
}
