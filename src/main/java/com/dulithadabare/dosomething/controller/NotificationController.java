package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.BasicResponse;
import com.dulithadabare.dosomething.model.UserProfile;
import com.dulithadabare.dosomething.resource.DBResource;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping( "/notifications" )
public class NotificationController
{
    final DBResource dbResource = new DBResource();

    @CrossOrigin
    @GetMapping( "" )
    public HttpEntity<BasicResponse> getAppNotifications( @AuthenticationPrincipal Jwt jwt,
                                                          @RequestParam( name = "pageKey", required = false ) String pageKey
    )
    {
        UserProfile authUser = new UserProfile();
        authUser.loadUserProfileFromJwt( jwt );

        if ( authUser.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getAppNotifications( authUser.getUserId(), pageKey );
    }
}
