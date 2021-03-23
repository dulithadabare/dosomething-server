package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping( "/feeds" )
public class FeedController
{
    private final DBResource dbResource = new DBResource();

    @CrossOrigin
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getFeed( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getFeed( userProfile.getUserId() );
    }

}
