package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.model.*;
import org.springframework.http.HttpEntity;

import java.sql.*;
import java.sql.Date;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class DBResource
{
    FacebookResource facebookResource = new FacebookResource();

    private final String DB_URL = "jdbc:mariadb://localhost:3306/dosomething_db";
    private final String DB_USER = "demoroot";
    private final String DB_PASS = "demoroot";

    public List<FeedItem> createEvent( EventNeed eventNeed, int userId )
    {
        List<FeedItem> createdEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {

            // Save to friend's inbox

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_need " +
                    "(user_id ," +
                    " verb," +
                    " noun," +
                    " start_date," +
                    " end_date," +
                    " date_scope," +
                    " start_time," +
                    " end_time," +
                    " time_scope," +
                    " is_confirmed ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );
                ps.setString( count++, eventNeed.getVerb() );
                ps.setString( count++, eventNeed.getNoun() );
                ps.setDate( count++, Date.valueOf( eventNeed.getStartDate() ) );
                ps.setDate( count++, Date.valueOf( eventNeed.getEndDate() ) );
                ps.setString( count++, eventNeed.getDateScope() );
                ps.setTime( count++, Time.valueOf( LocalTime.parse(eventNeed.getStartTime()) ) );
                ps.setTime( count++, Time.valueOf( LocalTime.parse(eventNeed.getEndTime()) ) );
                ps.setString( count++, eventNeed.getTimeScope() );
                ps.setBoolean( count++, eventNeed.isConfirmed() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Get created event id

            long eventId = getLastCreatedValueForEvent( conn );
            createdEvent = getEventById( eventId, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return createdEvent;
    }

    public List<FeedItem> addEventInterest( long eventId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Add new interested user to the event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_interested ( event_need_id, user_id ) VALUES ( ?, ? )" ) )
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
            }

            // Load updated event

            updatedEvent = getEventById( eventId, userId, conn );

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public List<FeedItem> removeEventInterest( long eventId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Remove event interested user

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_interested  WHERE event_need_id = ? AND user_id = ?" ) )
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
            }

            updatedEvent = getEventById( eventId, userId, conn );

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public HttpEntity<BasicResponse> confirmEventInterest( long eventId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
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

                }
                catch ( SQLException e )
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

            if ( eventOPId != userId )
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
                        int interestedUser = rs.getInt( 1 );

                        eventInterestedUsers.add( interestedUser );
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

            if ( eventInterestedUsers.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "Event Confirmed", BasicResponse.STATUS_SUCCESS ) );
            }

            // Send confirmation requests

            StringBuilder sqlSb = new StringBuilder( "INSERT IGNORE INTO confirm_request ( event_need_id, user_id ) VALUES " );

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
            }

            updatedEvent = getEventById( eventId, userId );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> confirmEventParticipation( long eventId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Add new participant to the event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_participants ( event_need_id, user_id ) VALUES ( ?, ? )" ) )
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

            // Delete confirm request

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM confirm_request  WHERE event_need_id = ? AND user_id = ?" ) )
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

            //Remove from event interested users

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_interested  WHERE event_need_id = ? AND user_id = ?" ) )
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

            updatedEvent = getEventById( eventId, userId );

        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> removeEventParticipation( long eventId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Save to friend's inbox

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_participants  WHERE event_need_id = ? AND user_id = ?" ) )
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

            updatedEvent = getEventById( eventId, userId );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> joinConfirmedEvent( long eventId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Add join request

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO join_request ( event_need_id, user_id ) VALUES ( ?, ? )" ) )
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

            updatedEvent = getEventById( eventId, userId );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> confirmJoinRequest( long eventId, int requesterUserId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Add user to the event's revealed list

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_participants ( event_need_id, user_id ) VALUES (?, ?)" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, requesterUserId );

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
                ps.setInt( count++, requesterUserId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            updatedEvent = getEventById( eventId, userId );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> getFeed( int userId )
    {
        List<FeedItem> feedItemList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            updateFriendList();
            // Get Friend User Profiles from DB

            Map<Integer, UserProfile> friendUserProfileList = getFriendProfiles( userId, conn );

            // None of the user's facebook friends are using the DoSomething app

            if ( friendUserProfileList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            // Get events by friends

            feedItemList = getEventByUserIdList( new ArrayList<>( friendUserProfileList.keySet() ), userId, conn );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedItemList ) );
    }

    public HttpEntity<BasicResponse> createUser( String facebookUserToken, UserProfile userProfile )
    {
        int newUserId = -1;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
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

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Get created user id from DB

            newUserId = getLastCreatedValueForUser( conn );

            // Get User Friends from Facebook

            Map<String, String> facebookFriendList = facebookResource.getFacebookFriends( userProfile.getFacebookId(), facebookUserToken );

            // Get friend user ids from DB

            Map<String, Integer> facebookIdUserIdMap = new HashMap<>();

            StringBuilder friendSQLSb = new StringBuilder( "SELECT u.id, u.facebook_id FROM user u WHERE  u.facebook_id IN (" );

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

                int count = 1;

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
                    ps.setInt( count++, newUserId );
                    ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                    ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                    ps.setInt( count++, newUserId );
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
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( newUserId ) );
    }

    public HttpEntity<BasicResponse> getConfirmRequests( int userId )
    {
        List<ConfirmRequest> confirmRequestList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Get Event Requests

            String sqlSb = "SELECT " +
                    "u.name, " +
                    "en.id, " +
                    "en.user_id, " +
                    "en.verb,  " +
                    "en.noun,  " +
                    "en.start_date,  " +
                    "en.end_date,  " +
                    "en.date_scope,  " +
                    "en.start_time,  " +
                    "en.end_time,  " +
                    "en.time_scope,  " +
                    "en.is_confirmed " +
                    "FROM confirm_request cr, event_need en, user u WHERE cr.user_id = ? AND cr.event_need_id = en.id AND en.user_id = u.id";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        String requesterName = rs.getString( col++ );

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );

                        UserProfile requesterUserProfile = new UserProfile();
                        requesterUserProfile.setUserId( eventNeed.getUserId() );
                        requesterUserProfile.setDisplayName( requesterName );

                        ConfirmRequest confirmRequest = new ConfirmRequest();
                        confirmRequest.setUser( requesterUserProfile );
                        confirmRequest.setEventNeed( eventNeed );

                        confirmRequestList.add( confirmRequest );
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
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( confirmRequestList ) );
    }

    public HttpEntity<BasicResponse> getJoinRequests( int userId )
    {
        List<JoinRequest> joinRequestList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Get Join Requests

            String sqlSb = "SELECT" +
                    " u.id requester_id," +
                    " u.name requester_name," +
                    " en.id," +
                    " en.user_id," +
                    " en.verb," +
                    " en.noun," +
                    " en.start_date," +
                    " en.end_date," +
                    " en.date_scope," +
                    " en.start_time," +
                    " en.end_time," +
                    " en.time_scope," +
                    " en.is_confirmed," +
                    " en.description" +
                    "  FROM join_request jr, event_need en, user u WHERE en.user_id = ? AND jr.event_need_id = en.id AND jr.user_id = u.id";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        int requesterId = rs.getInt( col++ );
                        String requesterName = rs.getString( col++ );

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );

                        UserProfile requesterUserProfile = new UserProfile();
                        requesterUserProfile.setUserId( requesterId );
                        requesterUserProfile.setDisplayName( requesterName );

                        JoinRequest joinRequest = new JoinRequest();
                        joinRequest.setUser( requesterUserProfile );
                        joinRequest.setEventNeed( eventNeed );

                        joinRequestList.add( joinRequest );
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
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( joinRequestList ) );
    }

    public long getLastCreatedValueForEvent( Connection conn )
    {
        long eventId = -1L;

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT PREVIOUS VALUE FOR event_need_sequence" ) )
        {
            try ( ResultSet rs = ps.executeQuery() )
            {
                while ( rs.next() )
                {
                    eventId = rs.getLong( 1 );
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

        return eventId;
    }

    public int getLastCreatedValueForUser( Connection conn )
    {
        int userId = -1;

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT PREVIOUS VALUE FOR user_sequence" ) )
        {
            try ( ResultSet rs = ps.executeQuery() )
            {
                while ( rs.next() )
                {
                    userId = rs.getInt( 1 );
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

        return userId;
    }

    public List<FeedItem> getEventById( long eventId, int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getEventById( eventId, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public List<FeedItem> getEventById( long eventId, int userId, Connection conn )
    {
        List<FeedItem> feedItems = new ArrayList<>();

        Map<Integer, UserProfile> friendUserProfileList = getFriendProfiles( userId, conn );

        String sqlSb = "SELECT id," +
                " user_id," +
                " verb, " +
                " noun, " +
                "start_date, " +
                "end_date, " +
                "date_scope, " +
                "start_time, " +
                "end_time, " +
                "time_scope, " +
                "is_confirmed, " +
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
                        interestedUserIdList = Arrays.stream( interested.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList() );
                    }

                    List<Integer> participantIdList = new ArrayList<>();

                    if ( participants != null )
                    {
                        participantIdList = Arrays.stream( participants.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList() );
                    }

                    boolean isParticipating = participantIdList.contains( userId );
                    boolean isInterested = interestedUserIdList.contains( userId );
                    boolean isJoinRequested = rs.getBoolean( "is_join_requested" );

                    long interestedFriendCount = interestedUserIdList.stream().filter( friendUserProfileList::containsKey ).count();
                    long participatingFriendCount = participantIdList.stream().filter( friendUserProfileList::containsKey ).count();

                    EventNeed eventNeed = new EventNeed();
                    eventNeed.load( rs, true );
                    eventNeed.setName( isParticipating ? friendUserProfileList.get( eventNeed.getUserId() ).getDisplayName() : null );

                    // Load participating user profiles

                    Map<Integer, UserProfile> participantUserProfileList = new HashMap<>();

                    if ( eventNeed.isConfirmed() && !participantIdList.isEmpty() )
                    {
                        participantUserProfileList = getParticipantProfiles( participantIdList, conn );
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


        return feedItems;
    }

    public List<FeedItem> getEventByUserIdList( List<Integer> eventUserIdList, int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getEventByUserIdList( eventUserIdList, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public List<FeedItem> getEventByUserIdList( List<Integer> eventUserIdList, int userId, Connection conn )
    {
        List<FeedItem> feedItems = new ArrayList<>();

        Map<Integer, UserProfile> friendUserProfileList = getFriendProfiles( userId, conn );

        StringBuilder sqlSb = new StringBuilder( "SELECT id," +
                " user_id," +
                " verb, " +
                " noun, " +
                "start_date, " +
                "end_date, " +
                "date_scope, " +
                "start_time, " +
                "end_time, " +
                "time_scope, " +
                "is_confirmed, " +
                "( SELECT GROUP_CONCAT(ep.user_id) FROM event_participants ep WHERE ep.event_need_id = en.id ) participants, " +
                "( SELECT GROUP_CONCAT(ei.user_id) FROM event_interested ei WHERE ei.event_need_id = en.id ) interested, " +
                "( SELECT COUNT(*) > 0 FROM join_request jr WHERE jr.event_need_id = en.id AND jr.user_id = ? ) is_join_requested " +
                "FROM event_need en " +
                "WHERE en.user_id IN ( " );

        String delim = " ";

        for ( int eventUserId : eventUserIdList )
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

            ps.setInt( count++, userId );

            for ( int eventUserId : eventUserIdList )
            {
                ps.setInt( count++, eventUserId );
            }

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
                    boolean isJoinRequested = rs.getBoolean( "is_join_requested" );

                    List<Integer> interestedUserIdList = new ArrayList<>();

                    if ( interested != null )
                    {
                        interestedUserIdList = Arrays.stream( interested.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList() );
                    }

                    List<Integer> participantIdList = new ArrayList<>();

                    if ( participants != null )
                    {
                        participantIdList = Arrays.stream( participants.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList() );
                    }

                    boolean isParticipating = participantIdList.contains( userId );
                    boolean isInterested = interestedUserIdList.contains( userId );

                    long interestedFriendCount = interestedUserIdList.stream().filter( friendUserProfileList::containsKey ).count();
                    long participatingFriendCount = participantIdList.stream().filter( friendUserProfileList::containsKey ).count();

                    EventNeed eventNeed = new EventNeed();
                    eventNeed.load( rs );

                    String name = null;

                    if ( isParticipating && friendUserProfileList.containsKey( eventNeed.getUserId() ) )
                    {
                        name = friendUserProfileList.get( eventNeed.getUserId() ).getDisplayName();
                    }
                    else if ( eventNeed.getUserId() == userId )
                    {
                        name = "You";
                    }

                    eventNeed.setName( name );

                    // Load participating user profiles

                    Map<Integer, UserProfile> participantUserProfileList = new HashMap<>();

                    if ( eventNeed.isConfirmed() && !participantIdList.isEmpty() )
                    {
                        participantUserProfileList = getParticipantProfiles( participantIdList, conn );
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


        return feedItems;
    }


    public Map<Integer, UserProfile> getFriendProfiles( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getFriendProfiles( userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public Map<Integer, UserProfile> getFriendProfiles( int userId, Connection conn )
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

    public Map<Integer, UserProfile> getParticipantProfiles( List<Integer> participantIdList )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getParticipantProfiles( participantIdList, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public Map<Integer, UserProfile> getParticipantProfiles( List<Integer> participantIdList, Connection conn )
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


        return participantUserProfileList;
    }

    public void updateFriendList()
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
    }
}
