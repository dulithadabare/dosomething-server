package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.facebook.Friend;
import com.dulithadabare.dosomething.facebook.FriendsResponse;
import com.dulithadabare.dosomething.model.*;
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

    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createUser( @RequestParam String accessToken, @AuthenticationPrincipal Jwt jwt )
    {

        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        Integer userId = userProfile.getUserId();
        boolean isNewUser = userId < 0;

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            if ( isNewUser )
            {
                // Create User

                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO user (facebook_id , firebase_uid, name) VALUES ( ?, ?, ? )" ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setString( count++, userProfile.getFacebookId() );
                    ps.setString( count++, userProfile.getFirebaseUid() );
                    ps.setString( count++, userProfile.getDisplayName() );

                    //execute query
                    ps.executeUpdate();

                } catch ( SQLException e )
                {
                    e.printStackTrace();
                    return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                }

                // Get created user id from DB

                String userIdSql = "SELECT u.id FROM user u WHERE  u.firebase_uid = ?";

                try ( PreparedStatement ps = conn.prepareStatement( userIdSql ) )
                {

                    ps.setFetchSize( 1000 );

                    ps.setString( 1, userProfile.getFirebaseUid() );

                    //execute query
                    try ( ResultSet rs = ps.executeQuery() )
                    {
                        //position result to first

                        while ( rs.next() )
                        {
                            userId = rs.getInt( 1 );
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

                // Get User Friends from Facebook

                Map<String, String> facebookFriendList = new HashMap<>();
                HttpClient httpClient = HttpClient.newBuilder()
                        .version( HttpClient.Version.HTTP_1_1 )
                        .connectTimeout( Duration.ofSeconds( 10 ) )
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri( URI.create( "https://graph.facebook.com/v10.0/" + userProfile.getFacebookId() + "/friends?access_token=" + accessToken ) )
                        .build();

                try
                {
                    HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );

                    ObjectMapper mapper = new ObjectMapper().configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
                    FriendsResponse friendsResponse;
                    try
                    {
                        friendsResponse = mapper.readValue( response.body(), FriendsResponse.class );
                        for ( Friend friend : friendsResponse.getData() )
                        {
                            facebookFriendList.put( friend.getId(), friend.getName() );
                        }
                    }
                    catch ( JsonProcessingException e )
                    {
                        e.printStackTrace();
                        return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                    }
                }
                catch ( IOException | InterruptedException e )
                {
                    e.printStackTrace();
                    return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                }

                // Get friend user ids from DB

                Map<String, Integer> facebookIdUserIdMap = new HashMap<>();

                StringBuilder friendSQLSb = new StringBuilder("SELECT u.id, u.facebook_id FROM user u WHERE  u.facebook_id IN (");

                String delim = " ";

                for ( String facebookId : facebookFriendList.keySet() )
                {
                    friendSQLSb.append( delim );
                    friendSQLSb.append( "?" );
                    delim = ", ";
                }

                friendSQLSb.append( ")" );

                try ( PreparedStatement ps = conn.prepareStatement( friendSQLSb.toString() ) )
                {

                    ps.setFetchSize( 1000 );

                    int count =  1;

                    for ( String facebookId : facebookFriendList.keySet() )
                    {
                        ps.setString( count++, facebookId );
                    }

                    //execute query
                    try ( ResultSet rs = ps.executeQuery() )
                    {
                        //position result to first

                        int col = 1;
                        while ( rs.next() )
                        {
                            int id = rs.getInt( col++ );
                            String facebookId = rs.getString( col++ );

                            facebookIdUserIdMap.put( facebookId, id );
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

                // Create Friends list

                StringBuilder updateSqlSb = new StringBuilder( "INSERT IGNORE INTO friend VALUES " );

                String delimiter = " ";

                for ( String facebookId : facebookFriendList.keySet() )
                {
                    updateSqlSb.append( delimiter );
                    updateSqlSb.append( "(?, ?), (?, ?)" );
                    delimiter = ", ";
                }

                try ( PreparedStatement ps = conn.prepareStatement( updateSqlSb.toString() ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    for ( String facebookId : facebookFriendList.keySet() )
                    {
                        ps.setInt( count++, userId );
                        ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                        ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                        ps.setInt( count++, userId );
                    }

                    //execute query
                    ps.executeUpdate();

                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                    return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                }
            }

        } catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
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

        List<ConfirmRequest> confirmRequestList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Get Event Requests

            String sqlSb = "SELECT u.name, en.id, en.user_id, en.need,  en.start_date,  en.end_date,  en.date_scope,  en.is_confirmed, en.description  FROM confirm_request cr, event_need en, user u WHERE cr.user_id = ? AND cr.event_need_id = en.id AND en.user_id = u.id";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        String requesterName = rs.getString( col++ );
                        long eventId = rs.getLong( col++ );
                        int enUserId = rs.getInt( col++ );
                        String need = rs.getString( col++ );
                        Date startDate = rs.getDate( col++ );
                        Date endDate = rs.getDate( col++ );
                        String dateScope = rs.getString( col++ );
                        boolean isConfirmed = rs.getBoolean( col++ );
                        String description = rs.getString( col++ );

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.setId( eventId );
                        eventNeed.setUserId( enUserId );
                        eventNeed.setName( requesterName );
                        eventNeed.setNeed( need );
                        eventNeed.setStartDate( startDate.toString() );
                        eventNeed.setEndDate( endDate.toString() );
                        eventNeed.setDateScope( dateScope );
                        eventNeed.setDescription( description );
                        eventNeed.setConfirmed( isConfirmed );

                        UserProfile requesterUserProfile = new UserProfile();
                        requesterUserProfile.setUserId( eventNeed.getUserId() );
                        requesterUserProfile.setDisplayName( requesterName );

                        ConfirmRequest confirmRequest = new ConfirmRequest( );
                        confirmRequest.setUser( requesterUserProfile );
                        confirmRequest.setEventNeed( eventNeed );

                        confirmRequestList.add( confirmRequest );
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


        return new HttpEntity<>( new BasicResponse( confirmRequestList ) );
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

        List<JoinRequest> joinRequestList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Get Event Requests

            String sqlSb = "SELECT u.id requester_id, u.name requester_name, en.id, en.user_id, en.need,  en.start_date,  en.end_date,  en.date_scope,  en.is_confirmed, en.description  FROM join_request jr, event_need en, user u WHERE en.user_id = ? AND jr.event_need_id = en.id AND jr.user_id = u.id";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        int requesterId = rs.getInt( col++ );
                        String requesterName = rs.getString( col++ );
                        long eventId = rs.getLong( col++ );
                        int enUserId = rs.getInt( col++ );
                        String need = rs.getString( col++ );
                        Date startDate = rs.getDate( col++ );
                        Date endDate = rs.getDate( col++ );
                        String dateScope = rs.getString( col++ );
                        boolean isConfirmed = rs.getBoolean( col++ );
                        String description = rs.getString( col++ );

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.setId( eventId );
                        eventNeed.setUserId( enUserId );
                        eventNeed.setNeed( requesterName );
                        eventNeed.setNeed( need );
                        eventNeed.setStartDate( startDate.toString() );
                        eventNeed.setEndDate( endDate.toString() );
                        eventNeed.setDateScope( dateScope );
                        eventNeed.setDescription( description );
                        eventNeed.setConfirmed( isConfirmed );

                        UserProfile requesterUserProfile = new UserProfile();
                        requesterUserProfile.setUserId( requesterId );
                        requesterUserProfile.setDisplayName( requesterName );

                        JoinRequest joinRequest = new JoinRequest( );
                        joinRequest.setUser( requesterUserProfile );
                        joinRequest.setEventNeed( eventNeed );

                        joinRequestList.add( joinRequest );
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

        return new HttpEntity<>( new BasicResponse( joinRequestList ) );
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
