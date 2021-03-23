package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.model.BasicResponse;
import com.dulithadabare.dosomething.model.EventNeed;
import com.dulithadabare.dosomething.model.FeedItem;
import com.dulithadabare.dosomething.model.UserProfile;
import org.apache.catalina.User;
import org.springframework.http.HttpEntity;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DBResource
{
    private final String DB_URL = "jdbc:mariadb://localhost:3306/dosomething_db";
    private final String DB_USER = "demoroot";
    private final String DB_PASS = "demoroot";

    public List<FeedItem> loadEventById( long eventId, int userId )
    {
        List<FeedItem> feedItems = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            Map<Integer, UserProfile> friendUserProfileList = loadFriendProfiles( userId, conn );

            String sqlSb = "SELECT id," +
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
                    "WHERE en.id = ? ";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );
                ps.setLong( count++, eventId );

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

                        List<Integer> participantIdList = new ArrayList<>();

                        if ( participants != null )
                        {
                            participantIdList = Arrays.stream( participants.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList());
                        }

                        boolean isParticipating = participantIdList.contains( userId );
                        boolean isInterested = interestedUserIdList.contains( userId );
                        boolean isJoinRequested = rs.getBoolean( "is_join_requested" );

                        long interestedFriendCount = interestedUserIdList.stream().filter( friendUserProfileList::containsKey ).count();
                        long participatingFriendCount = participantIdList.stream().filter( friendUserProfileList::containsKey ).count();

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );
                        eventNeed.setName( null );

                        // Load participating user profiles

                        Map<Integer, UserProfile> participantUserProfileList = new HashMap<>();

                        if ( eventNeed.isConfirmed() && !participantIdList.isEmpty() )
                        {
                            participantUserProfileList = loadParticipantProfiles( participantIdList, conn );
                        }

                        eventNeed.setParticipantList( new ArrayList<>( participantUserProfileList.values() ) );

                        FeedItem feedItem = new FeedItem();
                        feedItem.setEventNeed( eventNeed );
                        feedItem.setInterestedFriendCount( ( int ) interestedFriendCount );
                        feedItem.setParticipatingFriendCount( ( int ) participatingFriendCount );
                        feedItem.setInterested( isInterested );
                        feedItem.setParticipating( isParticipating );
                        feedItem.setJoinRequested( isJoinRequested );

                        feedItems.add( feedItem );
                    }

                } catch ( SQLException e )
                {
                    e.printStackTrace();
                }
            } catch ( SQLException e )
            {
                e.printStackTrace();
            }

        } catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return  feedItems;
    }

    public Map<Integer, UserProfile> loadFriendProfiles( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return loadFriendProfiles( userId, conn);
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public Map<Integer, UserProfile> loadFriendProfiles( int userId, Connection conn )
    {
        Map<Integer, UserProfile> friendUserProfileList = new HashMap<>();

        String friendSql = "SELECT u.id, u.firebase_uid, u.facebook_id, u.name FROM user u, friend f WHERE f.user_id = ? AND u.id = f.friend_id";

        try ( PreparedStatement ps = conn.prepareStatement( friendSql ) )
        {

            ps.setFetchSize( 1000 );

            ps.setInt( 1, userId );

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
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return friendUserProfileList;
    }

    public Map<Integer, UserProfile> loadParticipantProfiles( List<Integer> participantIdList )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return loadParticipantProfiles( participantIdList, conn);
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public Map<Integer, UserProfile> loadParticipantProfiles( List<Integer> participantIdList, Connection conn )
    {
        Map<Integer, UserProfile> participantUserProfileList = new HashMap<>();

        StringBuilder friendSql = new StringBuilder( "SELECT u.id, u.firebase_uid, u.facebook_id, u.name FROM user u WHERE u.id IN ( " );

        String delim = " ";

        for ( int participantUserId : participantIdList )
        {
            friendSql.append( delim );
            friendSql.append( "?" );
            delim = ", ";
        }

        friendSql.append( " )" );

        try ( PreparedStatement ps = conn.prepareStatement( friendSql.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int pCount = 1;

            for ( int participantUserId : participantIdList )
            {
                ps.setInt( pCount++, participantUserId );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    UserProfile participantUserProfile = new UserProfile();
                    participantUserProfile.loadFromResultSet( rs );

                    participantUserProfileList.put( participantUserProfile.getUserId(), participantUserProfile );
                }

            } catch ( SQLException e )
            {
                e.printStackTrace();
            }
        } catch ( SQLException e )
        {
            e.printStackTrace();
        }


        return participantUserProfileList;
    }
}
