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
    public HttpEntity<BasicResponse> createEvent( @RequestBody Event event, @AuthenticationPrincipal Jwt jwt  )
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

        return dbResource.updateConfirmedEvent( event, userProfile.getUserId() );
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
    public HttpEntity<BasicResponse> getPopularEventsByTag( @PathVariable String tag, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        String hashTag = "#" + tag;

        List<FeedItem> feedModelList = dbResource.getPopularEventsByTag( userProfile.getUserId(), hashTag );

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
    public HttpEntity<BasicResponse> addEventInterest( @PathVariable long eventId, @RequestBody EventInterest eventInterest, @AuthenticationPrincipal Jwt jwt, @RequestParam(name = "timestamp") long timestamp  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        eventInterest.setEventId( eventId );
        eventInterest.setUserId( userProfile.getUserId() );

        return dbResource.addEventInterest( eventId, userProfile.getUserId(), eventInterest, timestamp );
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
    @GetMapping( "/{eventId}/friends" )
    public HttpEntity<BasicResponse> getInterestedFriends( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getInterestedFriendList(  eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}/invitees" )
    public HttpEntity<BasicResponse> getInvitedUserList( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getInvitedUserList(  eventId, userProfile.getUserId() );
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
    @PostMapping( "/{eventId}/participants" )
    public HttpEntity<BasicResponse> acceptEventInvite( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.acceptEventInvite( eventId, userProfile.getUserId() ) ) );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/invites" )
    public HttpEntity<BasicResponse> declineEventInvite( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dbResource.declineEventInvite( eventId, userProfile.getUserId() ) ) );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/participants" )
    public HttpEntity<BasicResponse> cancelEventParticipation( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.cancelEventParticipation( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/join" )
    public HttpEntity<BasicResponse> addJoinRequest( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt, @RequestParam(name = "timestamp") long timestamp  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.addJoinRequest( eventId, userProfile.getUserId(), timestamp );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/join" )
    public HttpEntity<BasicResponse> removeJoinRequest( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.removeJoinRequest( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}/join" )
    public HttpEntity<BasicResponse> getJoinRequests( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getJoinRequests( eventId );
    }

    @CrossOrigin
    @PutMapping( "/{eventId}/join/{requesterId}" )
    public HttpEntity<BasicResponse> acceptJoinRequest( @PathVariable long eventId, @PathVariable int requesterId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.acceptJoinRequest( eventId, requesterId );
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

    @CrossOrigin
    @GetMapping( "/{confirmedEventId}/active" )
    public HttpEntity<BasicResponse> getActiveFriendListByEventId( @PathVariable long confirmedEventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getActiveFriendListByEventId( confirmedEventId, userProfile.getUserId() );
    }

}
