package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.Activity;
import com.dulithadabare.dosomething.model.BasicResponse;
import com.dulithadabare.dosomething.model.Event;
import com.dulithadabare.dosomething.model.UserProfile;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( "/activities" )
public class ActivityController
{

    private final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> addCurrentActivity( @RequestBody Activity activity, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.addCurrentActivity( activity, userProfile.getUserId() );
    }

    @CrossOrigin
    @DeleteMapping( "" )
    public HttpEntity<BasicResponse> removeCurrentActivity( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.removeCurrentActivity( userProfile.getUserId() );
    }
}
