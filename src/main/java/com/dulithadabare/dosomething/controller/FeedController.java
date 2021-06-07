package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( "/feeds" )
public class FeedController
{
    private final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "/popular" )
    public HttpEntity<BasicResponse> getPopularFeed( @RequestBody PopularFeedRequest popularRequest,
                                                     @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getPopularNearbyFeed( popularRequest, userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/happening" )
    public HttpEntity<BasicResponse> getHappeningFeed( @AuthenticationPrincipal Jwt jwt,
                                                       @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getHappeningFeed( userProfile.getUserId(), pageKey );
    }

    @CrossOrigin
    @GetMapping( "/upcoming" )
    public HttpEntity<BasicResponse> getUpcomingEventFeed( @AuthenticationPrincipal Jwt jwt,
                                                           @RequestParam( name = "pageKey", required = false ) String pageKey
                                                           )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getUpcomingFeed( userProfile.getUserId(), pageKey );
    }

}
