package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

        return dbResource.createEvent( event, userProfile.getUserId() );
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

        return dbResource.createConfirmedEvent( event, userProfile.getUserId() );
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
    @DeleteMapping( "/confirmed-events/{confirmedEventId}" )
    public HttpEntity<BasicResponse> cancelConfirmedEvent( @PathVariable long confirmedEventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.cancelConfirmedEvent( confirmedEventId, userProfile.getUserId() );
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

        return dbResource.getPopularEventsByTag( userProfile.getUserId(), hashTag );
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

        return dbResource.loadEventResponseById(  eventId, userProfile.getUserId() );
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

        return dbResource.getConfirmedEventById(  eventId, userProfile.getUserId() );
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

        return dbResource.addEventInterest( eventId, userProfile.getUserId(), eventInterest );
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

        return dbResource.removeEventInterest( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}/friends" )
    public HttpEntity<BasicResponse> getInterestedFriends( @PathVariable long eventId,
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

        return dbResource.getInterestedFriendList(  eventId, userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @GetMapping( "/{confirmedEventId}/invites" )
    public HttpEntity<BasicResponse> getInviteFriendsPage( @PathVariable Long confirmedEventId,
                                                           @AuthenticationPrincipal Jwt jwt,
                                                           @RequestParam( name = "eventId", required = false ) Long eventId,
                                                           @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getInviteFriendPage( confirmedEventId,  eventId, userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @PostMapping( "/{confirmedEventId}/invites" )
    public HttpEntity<BasicResponse> addInvites( @PathVariable Long confirmedEventId,
                                                 @RequestBody ConfirmedEvent event,
                                                 @AuthenticationPrincipal Jwt jwt
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.addInvites( event, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/{eventId}/invitees" )
    public HttpEntity<BasicResponse> getInvitedUserList( @PathVariable long eventId,
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

        return dbResource.getInvitedList(  eventId, userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/peek/{friendId}" )
    public HttpEntity<BasicResponse> sendVisibilityRequest( @PathVariable long eventId, @PathVariable int friendId, @AuthenticationPrincipal Jwt jwt, @RequestHeader("X-USER-TIMEZONE") String userTimeZone  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.sendVisibilityRequest( eventId, userProfile.getUserId(), friendId, userTimeZone );
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

        return dbResource.acceptEventInvite( eventId, userProfile.getUserId() );
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

        return dbResource.declineEventInvite( eventId, userProfile.getUserId() );
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
    public HttpEntity<BasicResponse> acceptJoinRequest( @PathVariable long eventId, @PathVariable Long requesterId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.acceptJoinRequest( eventId, userProfile.getUserId(), requesterId );
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

        return dbResource.removeEventVisibility( eventId, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/{confirmedEventId}/active" )
    public HttpEntity<BasicResponse> getActiveFriendListByEventId( @PathVariable long confirmedEventId,
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

        return dbResource.getActivePage( confirmedEventId, userProfile.getUserId(), pageKey );
    }

}
