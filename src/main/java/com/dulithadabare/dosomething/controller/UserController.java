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
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getUser( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        return dbResource.getCompleteUserProfileById( authUser.getUserId() );
    }

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createUser( @RequestBody UserProfile userProfile, @AuthenticationPrincipal Jwt jwt )
    {
        return dbResource.createUser( userProfile );
    }

    @CrossOrigin
    @PostMapping( "/tokens" )
    public HttpEntity<BasicResponse> saveUserToken(  @RequestParam( name = "token" ) String token,
                                                     @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.addUserToken( userProfile.getUserId(), token );
    }

    @CrossOrigin
    @GetMapping( "/current-activity" )
    public HttpEntity<BasicResponse> getCurrentActivity( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        return dbResource.getCurrentActivityByUserId( authUser.getUserId() );
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
    @PostMapping( "/logout" )
    public HttpEntity<BasicResponse> logout( @RequestParam( name = "token" ) String token,
                                                     @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.logout( userProfile, token );
    }
}
