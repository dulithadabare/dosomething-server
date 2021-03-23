package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.facebook.Friend;
import com.dulithadabare.dosomething.facebook.FriendsResponse;
import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.catalina.User;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.sql.Date;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping( "/users" )
public class UserController
{
    final DBResource dbResource = new DBResource();

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createUser( @RequestParam String accessToken, @AuthenticationPrincipal Jwt jwt )
    {

        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        Integer userId = userProfile.getUserId();
        boolean isNewUser = userId < 0;

        if ( isNewUser )
        {
            return dbResource.createUser( accessToken, userProfile );
        }

        return new HttpEntity<>( new BasicResponse( userId ) );
    }

    @CrossOrigin
    @GetMapping( "/confirm-requests" )
    public HttpEntity<BasicResponse> getConfirmRequests( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getConfirmRequests( userProfile.getUserId() );
    }

    @CrossOrigin
    @GetMapping( "/join-requests" )
    public HttpEntity<BasicResponse> getJoinRequests( @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        return dbResource.getJoinRequests( userProfile.getUserId() );
    }


    @CrossOrigin
    @GetMapping( "/involved-events" )
    public HttpEntity<BasicResponse> getRevealNotifications( @PathVariable String userId )
    {
        List<RevealNotification> revealNotificationList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Get Event Requests

            String sqlSb = "SELECT rn.revealer_id, u.name, en.id, en.user_id, en.need,  en.start_date,  en.end_date,  en.date_scope,  en.is_confirmed, en.description  FROM reveal_notification rn, event_need en, user u WHERE rn.user_id = ? AND rn.event_need_id = en.id AND rn.revealer_id = u.facebook_id";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, userId );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        String revealerUserId = rs.getString( col++ );
                        String revealerName = rs.getString( col++ );

                        /*long eventId = rs.getLong( col++ );
                        String enUserId = rs.getString( col++ );
                        String name = rs.getString( col++ );
                        Date startDate = rs.getDate( col++ );
                        Date endDate = rs.getDate( col++ );
                        String dateScope = rs.getString( col++ );
                        boolean isConfirmed = rs.getBoolean( col++ );*/

                        UserModel revealer = new UserModel( revealerUserId, revealerName );

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );

                        eventNeed.setName( null );
                        eventNeed.setParticipantList( new ArrayList<>() );

                        RevealNotification revealNotification = new RevealNotification( revealer, eventNeed );

                        revealNotificationList.add( revealNotification );
                    }

                } catch ( SQLException e )
                {
                    e.printStackTrace();
                    return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                }
            } catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

        } catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }


        return new HttpEntity<>( new BasicResponse( revealNotificationList ) );
    }


}
