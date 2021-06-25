package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.BasicResponse;
import com.dulithadabare.dosomething.model.UserProfile;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/settings" )
public class SettingController
{
    final DBResource dbResource = new DBResource();

    @CrossOrigin
    @GetMapping( "/friend-count" )
    public HttpEntity<BasicResponse> getFriendCount( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        if ( authUser.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getFriendCount( authUser.getUserId() );
    }
}
