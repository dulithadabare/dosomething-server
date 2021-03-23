package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.resource.DBResource;
import net.minidev.json.JSONObject;
import netscape.javascript.JSObject;
import org.springframework.http.HttpEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping( "/events" )
public class EventController
{
    DBResource dbResource = new DBResource();

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

        List<FeedItem> feedModelList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Get Event Needs

            String sqlSb = "SELECT id," +
                    " user_id, need, " +
                    "start_date, " +
                    "end_date, " +
                    "date_scope, " +
                    "is_confirmed, " +
                    "description, " +
                    "( SELECT GROUP_CONCAT(ep.user_id) FROM event_participants ep WHERE ep.event_need_id = en.id ) participants, " +
                    "( SELECT GROUP_CONCAT(ei.user_id) FROM event_interested ei WHERE ei.event_need_id = en.id ) interested " +
                    "FROM event_need en " +
                    "WHERE en.user_id = ? ";

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

//                        long eventId = rs.getLong( col++ );
//                        String enUserId = rs.getString( col++ );
//                        String need = rs.getString( col++ );
//                        Date startDate = rs.getDate( col++ );
//                        Date endDate = rs.getDate( col++ );
//                        String dateScope = rs.getString( col++ );
//                        boolean isConfirmed = rs.getBoolean( col++ );

                        String participants = rs.getString( "participants" );
                        String interested = rs.getString( "interested" );

                        List<Integer> interestedUserIdList = new ArrayList<>();

                        if ( interested != null )
                        {
                            interestedUserIdList = Arrays.stream( interested.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList());
                        }

                        List<Integer> participatantIdList = new ArrayList<>();

                        if ( participants != null )
                        {
                            participatantIdList = Arrays.stream( participants.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList());
                        }

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );
                        eventNeed.setName( "You" );

                        // Load participating user profiles

                        Map<Integer, UserProfile> participantUserProfileList = new HashMap<>();

                        if ( eventNeed.isConfirmed() && !participatantIdList.isEmpty() )
                        {
                            participantUserProfileList = dbResource.loadParticipantProfiles( participatantIdList );
                        }

                        eventNeed.setParticipantList( new ArrayList<>( participantUserProfileList.values() ) );

                        FeedItem feedModel = new FeedItem();
                        feedModel.setEventNeed( eventNeed );

                        feedModelList.add( feedModel );
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

        return new HttpEntity<>( new BasicResponse( feedModelList ) );
    }

   /* @CrossOrigin
    @GetMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> fetchEventNeed( @PathVariable long eventId )
    {

        FeedItemModel feedModel = null;

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {

            // Save to friend's inbox

            String sql = "SELECT id," +
                    " user_id, name, " +
                    "start_date, " +
                    "end_date, " +
                    "date_scope, " +
                    "is_confirmed, " +
                    "( SELECT GROUP_CONCAT(ep.user_id) FROM event_participants ep WHERE ep.event_need_id = en.id ) participants, " +
                    "( SELECT GROUP_CONCAT(jr.joiner_id) FROM join_request jr WHERE jr.event_need_id = en.id ) requesters " +
                    "FROM event_need en " +
                    "WHERE en.id = ?";

            try ( PreparedStatement ps = conn.prepareStatement( sql ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        long id = rs.getLong( col++ );
                        String enUserId = rs.getString( col++ );
                        String name = rs.getString( col++ );
                        Date startDate = rs.getDate( col++ );
                        Date endDate = rs.getDate( col++ );
                        String dateScope = rs.getString( col++ );
                        boolean isConfirmed = rs.getBoolean( col++ );
                        String participants = rs.getString( col++ );
                        String requesters = rs.getString( col++ );

                        List<String> interestedUsers = new ArrayList<>();

                        if ( participants != null )
                        {
                            interestedUsers.addAll( Arrays.asList( participants.split( "," ) ) );
                        }

                        List<String> requestedUsers = new ArrayList<>();

                        if ( requesters != null )
                        {
                            requestedUsers.addAll( Arrays.asList( requesters.split( "," ) ) );
                        }

                        EventNeedModel eventNeedModel = new EventNeedModel();
                        eventNeedModel.setId( id );
                        eventNeedModel.setUserId( enUserId );
                        eventNeedModel.setName( facebookFriendList.get( enUserId ) );
                        eventNeedModel.setNeed( name );
                        eventNeedModel.setStartDate( startDate.toString() );
                        eventNeedModel.setEndDate( endDate.toString() );
                        eventNeedModel.setDateScope( dateScope );
                        eventNeedModel.setConfirmed( isConfirmed );

                        boolean isParticipating = interestedUsers.contains( userId );
                        boolean isRequested = requestedUsers.contains( userId );

                        feedModel = new FeedItemModel( eventNeedModel, "", interestedUsers.size(), isParticipating, isRequested );
                    }
                }
                catch ( SQLException e  )
                {
                    e.printStackTrace();
                    return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                }

            }
                catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

        }
            catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedModel ) );
    }*/


    @CrossOrigin
    @PostMapping( "" )
    public HttpEntity<BasicResponse> createEvent( @RequestBody EventNeed eventNeedModel, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {

            // Save to friend's inbox

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_need (user_id , need, start_date, end_date, date_scope, is_confirmed, description) VALUES ( ?, ?, ?, ?, ?, ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userProfile.getUserId() );
                ps.setString( count++, eventNeedModel.getNeed() );
                ps.setDate( count++, Date.valueOf( eventNeedModel.getStartDate() ) );
                ps.setDate( count++, Date.valueOf( eventNeedModel.getEndDate() ) );
                ps.setString( count++, eventNeedModel.getDateScope() );
                ps.setBoolean( count++, eventNeedModel.isConfirmed() );
                ps.setString( count++, eventNeedModel.getDescription() );

                //execute query
                ps.executeUpdate();

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

        return new HttpEntity<>( new BasicResponse( "Created Event",BasicResponse.STATUS_SUCCESS ) );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/users" )
    public HttpEntity<BasicResponse> addEventInterest( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt  )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Add new participant to the event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_interested ( event_need_id, user_id ) VALUES ( ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( BasicResponse.STATUS_SUCCESS ) );
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

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {

            // Save to friend's inbox

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_interested  WHERE event_need_id = ? AND user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( BasicResponse.STATUS_SUCCESS ) );
    }

    @CrossOrigin
    @PutMapping( "/{eventId}" )
    public HttpEntity<BasicResponse> confirmEvent( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Only the event OP can confirm the event

            int eventOPId = -1;

            try ( PreparedStatement ps = conn.prepareStatement( "SELECT en.user_id FROM event_need en WHERE  en.id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                ps.setLong( 1, eventId );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        eventOPId = rs.getInt( 1 );
                    }

                } catch ( SQLException e )
                {
                    e.printStackTrace();
                    return null;
                }
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return null;
            }

            if ( eventOPId != userProfile.getUserId() )
            {
                return new HttpEntity<>( new BasicResponse( "User does not have permission to confirm event", BasicResponse.STATUS_ERROR ) );
            }

            // Save to friend's inbox

            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event_need SET is_confirmed = true WHERE id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Get event interested users

            List<Integer> eventInterestedUsers = new ArrayList<>();

            try ( PreparedStatement ps = conn.prepareStatement( "SELECT ei.user_id FROM event_interested ei WHERE  ei.event_need_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                ps.setLong( 1, eventId );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int userId = rs.getInt( 1 );

                        eventInterestedUsers.add( userId );
                    }

                } catch ( SQLException e )
                {
                    e.printStackTrace();
                    return null;
                }
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return null;
            }

            if ( eventInterestedUsers.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse("Event Confirmed", BasicResponse.STATUS_SUCCESS ) );
            }

            // Send confirmation requests

            StringBuilder sqlSb = new StringBuilder("INSERT IGNORE INTO confirm_request ( event_need_id, user_id ) VALUES ");

            String delim = " ";

            for ( int interestedUser : eventInterestedUsers )
            {
                sqlSb.append( delim );
                sqlSb.append( "(?, ?)" );
                delim = ", ";
            }

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                for ( int interestedUser : eventInterestedUsers )
                {
                    ps.setLong( count++, eventId );
                    ps.setInt( count++, interestedUser );
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
        catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse("Event Confirmed", BasicResponse.STATUS_SUCCESS ) );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/participants" )
    public HttpEntity<BasicResponse> confirmEventParticipation( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Add new participant to the event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_participants ( event_need_id, user_id ) VALUES ( ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Delete confirm request

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM confirm_request  WHERE event_need_id = ? AND user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            //Remove from event interested users

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_interested  WHERE event_need_id = ? AND user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( BasicResponse.STATUS_SUCCESS ) );
    }

    @CrossOrigin
    @DeleteMapping( "/{eventId}/participants" )
    public HttpEntity<BasicResponse> removeEventParticipation( @PathVariable int eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {

            // Save to friend's inbox

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_participants  WHERE event_need_id = ? AND user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( BasicResponse.STATUS_SUCCESS ) );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/join-requests" )
    public HttpEntity<BasicResponse> joinConfirmedEvent( @PathVariable long eventId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Add join request

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO join_request ( event_need_id, user_id ) VALUES ( ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userProfile.getUserId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( BasicResponse.STATUS_SUCCESS ) );
    }

    @CrossOrigin
    @PostMapping( "/{eventId}/participants/{userId}" )
    public HttpEntity<BasicResponse> confirmJoinRequest( @PathVariable long eventId, @PathVariable int userId, @AuthenticationPrincipal Jwt jwt )
    {
        UserProfile userProfile = new UserProfile();
        userProfile.loadUserProfileFromJwt( jwt );

        if ( userProfile.getUserId() < 0 )
        {
            return new HttpEntity<>( new BasicResponse( "Invalid User", BasicResponse.STATUS_ERROR ) );
        }

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Add user to the event's revealed list

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_participants ( event_need_id, user_id ) VALUES (?, ?)" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Delete Join Request

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM join_request  WHERE event_need_id = ? AND user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( BasicResponse.STATUS_SUCCESS ) );
    }
}
