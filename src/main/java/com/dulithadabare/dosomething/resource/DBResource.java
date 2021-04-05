package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.util.LocationHelper;
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

    private final String EVENT_SELECT = "SELECT id," +
            " user_id," +
            " activity, " +
            "start_date, " +
            "end_date, " +
            "date_scope, " +
            "start_time, " +
            "end_time, " +
            "time_scope, " +
            "( SELECT GROUP_CONCAT(ev.friend_id) FROM event_visibility ev WHERE ev.user_id = ? AND ev.event_need_id = en.id ) visible, " +
            "( SELECT GROUP_CONCAT(vr.user_id) FROM visibility_request vr WHERE vr.friend_id = ? AND vr.event_need_id = en.id ) requested, " +
            "( SELECT GROUP_CONCAT(ei.user_id) FROM event_interested ei WHERE ei.event_need_id = en.id ) interested ";

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

    public HttpEntity<BasicResponse> getFeed( int userId )
    {
        List<FeedItem> feedItemList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
//            updateFriendList();

            // Get Friend User Profiles from DB
            List<Integer> friendIdList = getFriendIdList( userId, conn );

            // None of the user's facebook friends are using the DoSomething app
            if ( friendIdList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            // Get events by friends

            feedItemList = getEventsCreatedByFriends( new ArrayList<>( friendIdList ), userId, conn );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedItemList ) );
    }

    public List<FeedItem> createEvent( EventNeed eventNeed, int userId )
    {
        List<FeedItem> createdEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {

            // Create event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_need " +
                    "(user_id ," +
                    " activity," +
                    " start_date," +
                    " end_date," +
                    " date_scope," +
                    " start_time," +
                    " end_time," +
                    " time_scope" +
                    " ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? )" ) )
            {
                Date startDate = eventNeed.getStartDate() != null && !eventNeed.getStartDate().isEmpty()  ? Date.valueOf( eventNeed.getStartDate() ) : null;
                Date endDate = eventNeed.getEndDate() != null && !eventNeed.getStartDate().isEmpty() ? Date.valueOf( eventNeed.getEndDate() ) : null;
                String dateScope = eventNeed.getDateScope() != null && !eventNeed.getDateScope().isEmpty() ? eventNeed.getDateScope() : null;

                Time startTime = eventNeed.getStartTime() != null && !eventNeed.getStartTime().isEmpty() ? Time.valueOf( LocalTime.parse(eventNeed.getStartTime()) ) : null;
                Time endTime = eventNeed.getEndTime() != null && !eventNeed.getStartTime().isEmpty() ? Time.valueOf( LocalTime.parse(eventNeed.getStartTime()) ) : null;
                String timeScope = eventNeed.getTimeScope() != null && !eventNeed.getTimeScope().isEmpty() ? eventNeed.getDateScope() : null;

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );
                ps.setString( count++, eventNeed.getActivity() );
                ps.setDate( count++, startDate );
                ps.setDate( count++, endDate );
                ps.setString( count++, dateScope );
                ps.setTime( count++, startTime );
                ps.setTime( count++, endTime );
                ps.setString( count++, timeScope );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Get created event id
            long eventId = getLastCreatedValueForEvent( conn );

            //Add event interest

            createdEvent = addEventInterest( eventId, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return createdEvent;
    }

    public List<FeedItem> updateEvent( EventNeed eventNeed, int userId ) {
        List<FeedItem> createdEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return updateEvent( eventNeed, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return createdEvent;

    }

    public List<FeedItem> updateEvent( EventNeed eventNeed, int userId, Connection conn )
    {
        List<FeedItem> createdEvent;

        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event_need SET " +
                " start_date = ?," +
                " end_date = ?," +
                " date_scope = ?," +
                " start_time = ?," +
                " end_time = ?," +
                " time_scope = ?" +
                " WHERE id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setDate( count++, Date.valueOf( eventNeed.getStartDate() ) );
            ps.setDate( count++, Date.valueOf( eventNeed.getEndDate() ) );
            ps.setString( count++, eventNeed.getDateScope() );
            ps.setTime( count++, Time.valueOf( LocalTime.parse(eventNeed.getStartTime()) ) );
            ps.setTime( count++, Time.valueOf( LocalTime.parse(eventNeed.getEndTime()) ) );
            ps.setString( count++, eventNeed.getTimeScope() );
            ps.setLong( count++, eventNeed.getId() );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        // Get created event id

        createdEvent = getEventById( eventNeed.getId(), userId, conn );

        return createdEvent;
    }

    public List<FeedItem> addEventInterest( long eventId, int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return  addEventInterest( eventId, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return  new ArrayList<>();

    }

    public List<FeedItem> addEventInterest( long eventId, int userId, Connection conn )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

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

    public List<FeedItem> sendVisibilityRequest( long eventId, int userId, int friendId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Make user visible to friends in the event.

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO visibility_request ( event_need_id, user_id, friend_id ) VALUES ( ?, ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, friendId ); // Current user is the friend in the request from the POV of the receiver
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

    public List<FeedItem> addEventVisibility( long eventId, int userId, int friendId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Make user visible to friend in the event.

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_visibility ( event_need_id, user_id, friend_id ) VALUES ( ?, ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, friendId );
                ps.setInt( count++, userId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Remove visibility request

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM visibility_request WHERE event_need_id = ? AND user_id = ? AND friend_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userId );
                ps.setInt( count++, friendId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Add notifications to friend

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO visibility_notification ( event_need_id, user_id, friend_id ) VALUES (?, ?, ?)" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, friendId );
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

    public List<FeedItem> removeEventVisibility( long eventId, int userId )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Remove user visibility

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_visibility  WHERE event_need_id = ? AND user_id = ?" ) )
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

    public HttpEntity<BasicResponse> getVisibilityRequests( int userId )
    {
        List<VisibilityRequest> notificationList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Get Event Visibility Requests

            String sqlSb = "SELECT " +
                    "u.name, " +
                    "vr.friend_id, " +
                    "en.id, " +
                    "en.user_id, " +
                    "en.activity,  " +
                    "en.start_date,  " +
                    "en.end_date,  " +
                    "en.date_scope,  " +
                    "en.start_time,  " +
                    "en.end_time,  " +
                    "en.time_scope  " +
                    "FROM visibility_request vr, event_need en, user u WHERE vr.user_id = ? AND vr.event_need_id = en.id AND vr.friend_id = u.id";

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
                        String friendName = rs.getString( col++ );
                        int friendUserId = rs.getInt( col++ );

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );

                        UserProfile friend = new UserProfile();
                        friend.setUserId( friendUserId );
                        friend.setDisplayName( friendName );

                        VisibilityRequest notification = new VisibilityRequest();
                        notification.setUser( friend );
                        notification.setEventNeed( eventNeed );

                        notificationList.add( notification );
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

        return new HttpEntity<>( new BasicResponse( notificationList ) );
    }


    public HttpEntity<BasicResponse> getVisibilityNotifications( int userId )
    {
        List<VisibilityNotification> notificationList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Get Event Requests

            String sqlSb = "SELECT " +
                    "u.name, " +
                    "vn.friend_id, " +
                    "en.id, " +
                    "en.user_id, " +
                    "en.activity,  " +
                    "en.start_date,  " +
                    "en.end_date,  " +
                    "en.date_scope,  " +
                    "en.start_time,  " +
                    "en.end_time,  " +
                    "en.time_scope  " +
                    "FROM visibility_notification vn, event_need en, user u WHERE vn.user_id = ? AND vn.event_need_id = en.id AND vn.friend_id = u.id";

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
                        String friendName = rs.getString( col++ );
                        int friendUserId = rs.getInt( col++ );

                        EventNeed eventNeed = new EventNeed();
                        eventNeed.load( rs );

                        UserProfile friend = new UserProfile();
                        friend.setUserId( friendUserId );
                        friend.setDisplayName( friendName );

                        VisibilityNotification notification = new VisibilityNotification();
                        notification.setUser( friend );
                        notification.setEventNeed( eventNeed );

                        notificationList.add( notification );
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

        return new HttpEntity<>( new BasicResponse( notificationList ) );
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

        List<Integer> friendIdList = getFriendIdList( userId, conn );

        String sqlSb = EVENT_SELECT + "FROM event_need en " +
                "WHERE en.id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setInt( count++, userId ); // for visible
            ps.setInt( count++, userId ); // for requests
            ps.setLong( count++, eventId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    FeedItem feedItem = getFeedItem( friendIdList, userId, conn, rs );

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

    public List<FeedItem> getEventsCreatedByUser( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            List<FeedItem> feedItems = new ArrayList<>();

            List<Integer> friendIdList = getFriendIdList( userId, conn );

            String sqlSb = EVENT_SELECT + "FROM event_need en " + "WHERE en.user_id = ? ";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId ); // for visible
                ps.setInt( count++, userId ); // for requests
                ps.setInt( count++, userId );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        FeedItem feedItem = getFeedItem( friendIdList, userId, conn, rs );
                        feedItem.getEventNeed().setName( "You" );
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
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }


    public List<FeedItem> getEventsCreatedByFriends( List<Integer> eventUserIdList, int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getEventsCreatedByFriends( eventUserIdList, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public List<FeedItem> getEventsCreatedByFriends( List<Integer> friendIdList, int userId, Connection conn )
    {
        List<FeedItem> feedItems = new ArrayList<>();

        StringBuilder sqlSb = new StringBuilder(  EVENT_SELECT + "FROM event_need en " +
                "WHERE " +
                "en.user_id IN ( " );

        String delim = " ";

        for ( int eventUserId : friendIdList )
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

            ps.setInt( count++, userId ); // for visible
            ps.setInt( count++, userId ); // for requests

            for ( int eventUserId : friendIdList )
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

                    FeedItem feedItem = getFeedItem( friendIdList, userId, conn, rs );

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

    private FeedItem getFeedItem( List<Integer> friendIdList, int userId, Connection conn, ResultSet rs ) throws SQLException
    {
        String visible = rs.getString( "visible" );
        String requested = rs.getString( "requested" );
        String interested = rs.getString( "interested" );

        List<Integer> interestedUserIdList = new ArrayList<>();

        if ( interested != null )
        {
            interestedUserIdList = Arrays.stream( interested.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList() );
        }

        List<Integer> interestedFriendIdList = new ArrayList<>();

        for ( Integer interestedUserId : interestedUserIdList )
        {
            if ( friendIdList.contains( interestedUserId ) )
            {
                interestedFriendIdList.add( interestedUserId );
            }
        }

        // Load interested friend profiles

        Map<Integer, UserProfile> interestedFriendMap = getUserProfiles( interestedFriendIdList , conn );

        List<Integer> requestedFriendList = new ArrayList<>();

        if ( requested != null )
        {
            requestedFriendList = Arrays.stream( requested.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList() );
        }

        List<Integer> visibleUserList = new ArrayList<>();

        if ( visible != null )
        {
            visibleUserList = Arrays.stream( visible.split( "," ) ).map( Integer::parseInt ).collect( Collectors.toList() );
        }

        //Load current user profile

        final UserProfile currUser = getUserProfiles( Arrays.asList( userId ), conn ).get( userId );

        // Remove identifying details from non-visible friends

        for ( Integer friendUserId : interestedFriendMap.keySet() )
        {
            UserProfile friendProfile = interestedFriendMap.get( friendUserId );

            updateInterestedFriendProfile( requestedFriendList, visibleUserList, currUser, friendProfile );
        }

        boolean isInterested = interestedUserIdList.contains( userId );
        boolean isAnonymous = !visibleUserList.contains( userId );

        EventNeed eventNeed = new EventNeed();
        eventNeed.load( rs, true );

        String name = null;
        if ( interestedFriendMap.containsKey( eventNeed.getUserId() ) )
        {
            name = interestedFriendMap.get( eventNeed.getUserId() ).getDisplayName();
        }

        eventNeed.setName( name );

        FeedItem feedItem = new FeedItem();
        feedItem.setEventNeed( eventNeed );
        feedItem.setInterestedFriendCount( interestedFriendIdList.size() );
        feedItem.setInterested( isInterested );
        feedItem.setAnonymous( isAnonymous );
        feedItem.setInterestedFriendList( new ArrayList<>(interestedFriendMap.values()) );
        return feedItem;
    }

    private void updateInterestedFriendProfile( List<Integer> requestedFriendList, List<Integer> visibleUserList, UserProfile currUser, UserProfile friendProfile )
    {
        double distanceInMeters = LocationHelper.distance( currUser.getLatitude(), friendProfile.getLatitude(), currUser.getLongitude(), friendProfile.getLongitude(), 0.0, 0.0 );
        System.out.println( friendProfile.getDisplayName() + " : " + distanceInMeters );

        String distance;
        if ( distanceInMeters <= 1000 )
        {
            distance = (int) Math.floor( distanceInMeters ) + "m";
        }
        else {
            double distanceInKm = distanceInMeters/1000;
            distance = (int) Math.floor( distanceInKm ) + "km";
        }

        friendProfile.setDistance( distance );

        if ( !visibleUserList.contains( friendProfile.getUserId() ) )
        {
            friendProfile.setFirebaseUid( null );
            friendProfile.setFacebookId( null );
            friendProfile.setDisplayName( null );
        }

        if ( requestedFriendList.contains( friendProfile.getUserId() ) )
        {
            friendProfile.setVisibilityRequested( true );
        }
    }


    public List<Integer> getFriendIdList( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getFriendIdList( userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public List<Integer> getFriendIdList( int userId, Connection conn )
    {
        List<Integer> friendUserIdList = new ArrayList<>();

        String friendSql = "SELECT f.friend_id FROM friend f WHERE f.user_id = ?";

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

                    friendUserIdList.add( friendUserId );
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

        return friendUserIdList;
    }

    public Map<Integer, UserProfile> getUserProfiles( List<Integer> userIdList )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getUserProfiles( userIdList, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public Map<Integer, UserProfile> getUserProfiles( List<Integer> userIdList, Connection conn )
    {
        Map<Integer, UserProfile> userProfileList = new HashMap<>();

        StringBuilder friendSql = new StringBuilder( "SELECT u.id, u.firebase_uid, u.facebook_id, u.name, u.longitude, u.latitude FROM user u WHERE u.id IN ( " );

        String delim = " ";

        for ( int participantUserId : userIdList )
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

            for ( int participantUserId : userIdList )
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

                    userProfileList.put( participantUserProfile.getUserId(), participantUserProfile );
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


        return userProfileList;
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

    public List<UserProfile> updateUserLocation( UserLocation userLocation, int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return updateUserLocation( userLocation, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public List<UserProfile> updateUserLocation( UserLocation userLocation, int userId, Connection conn )
    {
        List<UserProfile> updatedProfile = new ArrayList<>();

        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE user SET " +
                " longitude = ?," +
                " latitude = ?" +
                " WHERE id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setDouble( count++, userLocation.getLongitude() );
            ps.setDouble( count++, userLocation.getLatitude() );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        // Get created event id
        List<Integer> userIdList = new ArrayList<>();
        userIdList.add( userId );
        updatedProfile.add( getUserProfiles( userIdList, conn ).get( userId ) );

        return updatedProfile;
    }

}