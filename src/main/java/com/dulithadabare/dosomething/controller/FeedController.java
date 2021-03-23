package com.dulithadabare.dosomething.controller;

import com.dulithadabare.dosomething.model.*;
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

        List<FeedItem> feedModelList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( "jdbc:mariadb://localhost:3306/dosomething_db", "demoroot", "demoroot" ) )
        {
            // Check if need to update friends list

            /*
            String checkSql = "SELECT u.last_checked_friends FROM user u WHERE u.facebook_id = ?";

            Timestamp lastCheckedFriends = null;

            try ( PreparedStatement ps = conn.prepareStatement( checkSql ) )
            {

                ps.setFetchSize( 1000 );
                ps.setString( 1, userId );

                try ( ResultSet rs = ps.executeQuery() )
                {
                    while ( rs.next() )
                    {
                        lastCheckedFriends = rs.getTimestamp( 1 );
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

            if ( lastCheckedFriends != null )
            {
                long currentTimestamp = System.currentTimeMillis();

                long diffMinutes = (currentTimestamp - lastCheckedFriends.getTime())/ (60 * 1000);

                if ( diffMinutes > 60 )
                {
                    // update friends from facebook
                }

            }*/

            // Update last checked friends timestamp

                /*try ( PreparedStatement ps = conn.prepareStatement( "UPDATE user SET last_checked_friends = NOW()" ) )
                {

                    ps.setFetchSize( 1000 );
                    ps.executeUpdate();

                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                    return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                }*/

            // Get Friend User Profiles from DB

            Map<Integer, UserProfile> friendUserProfileList = new HashMap<>();

            String friendSql = "SELECT u.id, u.firebase_uid, u.facebook_id, u.name FROM user u, friend f WHERE f.user_id = ? AND u.id = f.friend_id";

            try ( PreparedStatement ps = conn.prepareStatement( friendSql ) )
            {

                ps.setFetchSize( 1000 );

                ps.setInt( 1, userProfile.getUserId() );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int count = 1;

                        int friendUserId = rs.getInt( count++ );
                        String friendFacebookId = rs.getString( count++ );
                        String friendFirebaseUid = rs.getString( count++ );
                        String friendName = rs.getString( count++ );

                        UserProfile friendUserProfile = new UserProfile();

                        friendUserProfile.setUserId( friendUserId );
                        friendUserProfile.setFirebaseUid( friendFirebaseUid );
                        friendUserProfile.setFacebookId( friendFacebookId );
                        friendUserProfile.setDisplayName( friendName );

                        friendUserProfileList.put( friendUserId, friendUserProfile );
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

            // None of the user's facebook friends are using the DoSomething app

            if ( friendUserProfileList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            // Get events by friends

            StringBuilder sqlSb = new StringBuilder( "SELECT id," +
                    " user_id, need, " +
                    "start_date, " +
                    "end_date, " +
                    "date_scope, " +
                    "is_confirmed, " +
                    "description, " +
                    "( SELECT GROUP_CONCAT(ep.user_id) FROM event_participants ep WHERE ep.event_need_id = en.id ) participants, " +
                    "( SELECT GROUP_CONCAT(ei.user_id) FROM event_interested ei WHERE ei.event_need_id = en.id ) interested, " +
                    "( SELECT COUNT(*) > 0 FROM join_request jr WHERE jr.event_need_id = en.id AND jr.user_id = ? ) is_join_requested " +
                    "FROM event_need en " +
                    "WHERE en.user_id IN (" );

            String delim = " ";

            for ( int friendUserId : friendUserProfileList.keySet() )
            {
                sqlSb.append( delim );
                sqlSb.append( "?" );
                delim = ", ";
            }

            sqlSb.append( " )" );

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userProfile.getUserId() );

                for ( int friendUserId : friendUserProfileList.keySet() )
                {
                    ps.setInt( count++, friendUserId );
                }

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );

                        /*long eventId = rs.getLong( col++ );
                        String enUserId = rs.getString( col++ );
                        String name = rs.getString( col++ );
                        Date startDate = rs.getDate( col++ );
                        Date endDate = rs.getDate( col++ );
                        String dateScope = rs.getString( col++ );
                        boolean isConfirmed = rs.getBoolean( col++ );*/

                        String participants = rs.getString( "participants" );
                        String interested = rs.getString( "interested" );
                        boolean isJoinRequested = rs.getBoolean( "is_join_requested" );

                        List<Integer> interestedUserIdList = new ArrayList<>();

                        if ( interested != null )
                        {
                            interestedUserIdList = Arrays.stream( interested.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList());
                        }

                        List<Integer> participatingUserIdList = new ArrayList<>();

                        if ( participants != null )
                        {
                            participatingUserIdList = Arrays.stream( participants.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList());
                        }

                        boolean isParticipating = participatingUserIdList.contains( userProfile.getUserId() );
                        boolean isInterested = interestedUserIdList.contains( userProfile.getUserId() );

                        eventNeed.setName( isParticipating ? friendUserProfileList.get( eventNeed.getUserId() ).getDisplayName() : null );

                        long interestedFriendCount = interestedUserIdList.stream().filter( friendUserProfileList::containsKey ).count();
                        long participatingFriendCount = participatingUserIdList.stream().filter( friendUserProfileList::containsKey ).count();

                        FeedItem feedModel = new FeedItem();
                        feedModel.setEventNeed( eventNeed );
                        feedModel.setInterestedFriendCount( ( int ) interestedFriendCount );
                        feedModel.setParticipatingFriendCount( ( int ) participatingFriendCount );
                        feedModel.setInterested( isInterested );
                        feedModel.setParticipating( isParticipating );
                        feedModel.setJoinRequested( isJoinRequested );

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

}
