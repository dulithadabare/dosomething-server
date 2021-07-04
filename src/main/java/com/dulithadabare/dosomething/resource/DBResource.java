package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.constant.AppNotificationType;
import com.dulithadabare.dosomething.facebook.PublicProfile;
import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.util.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.*;
import java.time.*;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Collectors;

public class DBResource
{
    FacebookResource facebookResource = new FacebookResource();

    @Autowired
    FirebaseCloudMessaging firebaseCloudMessaging = new FirebaseCloudMessaging();

    private final String DB_URL = "jdbc:mariadb://localhost:3306/dosomething_db";
    private final String DB_USER = "demoroot";
    private final String DB_PASS = "demoroot";

    private final String EVENT_SELECT = " e.id," +
            " e.creator_id," +
            " e.description, " +
            " e.created_time " +
            " FROM event e ";
    private final String CONFIRMED_EVENT_SELECT = " ce.id," +
            " ce.creator_id," +
            " ce.description, " +
            " ce.created_time " +
            "FROM confirmed_event ce ";

    private final String COMPLETE_USER_PROFILE_SELECT = "SELECT " +
            "u.id, " +
            "u.facebook_id, " +
            "u.firebase_uid, " +
            "u.name, " +
            "u.email, " +
            "u.latitude, " +
            "u.longitude " +
            "FROM user_profile u ";

    private final String PARTIAL_USER_PROFILE_SELECT = "SELECT " +
            "u.id, " +
            "u.name " +
            "FROM user_profile u ";

    public static Connection getConnection() throws SQLException, URISyntaxException
    {
        String dbUrl = System.getenv( "JDBC_DATABASE_URL" );
        return DriverManager.getConnection( dbUrl );
    }

    public HttpEntity<BasicResponse> getCompleteUserProfileById( Long userId )
    {
        UserProfile user_profile;
        try ( Connection conn = getConnection() )
        {
            user_profile = getCompleteUserProfileById( userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( user_profile ) );
    }

    public UserProfile getCompleteUserProfileById( Long userId, Connection conn ) throws SQLException
    {
        UserProfile userProfile = null;

        String sql = COMPLETE_USER_PROFILE_SELECT +
                "WHERE u.id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sql ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    userProfile = new UserProfile();
                    userProfile.loadCompleteProfileFromResultSet( rs );
                }

            }
        }

        return userProfile;
    }

    public HttpEntity<BasicResponse> getCurrentActivityByUserId( Long userId )
    {
        CurrentActivity currentActivity;

        try ( Connection conn = getConnection() )
        {
            currentActivity = loadCurrentActivityByUserId( userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( currentActivity ) );
    }

    public CurrentActivity loadCurrentActivityByUserId( Long userId, Connection conn ) throws SQLException
    {
        CurrentActivity currentActivity = null;

        String sqlSb = "SELECT " +
                "ca.user_id, " +
                "ca.event_id, " +
                "ca.updated_time " +
                "FROM current_activity ca " +
                "WHERE ca.user_id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    currentActivity = new CurrentActivity();
                    currentActivity.load( rs );
                }

            }

        }

        return currentActivity;
    }

    public HttpEntity<BasicResponse> createEvent( Event event, Long userId )
    {
        long eventId = -1;
        EventResponse createdEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                // Create event
                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event (" +
                        " creator_id, " +
                        " description " +
                        " ) VALUES ( ?, ? ) RETURNING id" ) )
                {
                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, userId );
                    ps.setString( count++, event.getDescription() );

                    try ( ResultSet rs = ps.executeQuery() )
                    {
                        //position result to first

                        while ( rs.next() )
                        {
                            int col = 1;
                            long id = rs.getLong( col++ );

                            eventId = id;
                        }

                    }

                }

                //Add event interest for creator
                addEventInterest( eventId, userId, conn );

                // Load updated event
                createdEvent = loadEventResponseById( eventId, userId, conn );
            }
            catch ( SQLException e )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw e;
            }

            conn.commit();
            conn.setAutoCommit( true );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( "Error", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( createdEvent ) );
    }

    public HttpEntity<BasicResponse> addEventInterest( long eventId, Long userId )
    {
        EventResponse eventResponse;
        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                addEventInterest( eventId, userId, conn );
                // Add new interest notification
                Event event = loadEventById( eventId, conn );
                addInterestNotification( eventId, userId, event.getCreatorId(), conn );
            }
            catch ( SQLException ex )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit( true );

            // Load updated event
            eventResponse = loadEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), com.dulithadabare.dosomething.model.BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( eventResponse ) );

    }

    public void addEventInterest( long eventId, Long userId, Connection conn ) throws SQLException
    {
        // Add new interested user to the event
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_interested ( event_id, user_id ) VALUES ( ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();
        }
    }

    public HttpEntity<BasicResponse> removeEventInterest( long eventId, Long userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                // Remove event interested user
                try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_interested  WHERE event_id = ? AND user_id = ?" ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, eventId );
                    ps.setLong( count++, userId );

                    //execute query
                    ps.executeUpdate();

                }
            }
            catch ( SQLException ex )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit( true );

            updatedEvent = loadEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public Event loadEventById( Long eventId, Connection conn ) throws SQLException
    {
        Event event = new Event();

        String sqlSb = "SELECT " + EVENT_SELECT +
                "WHERE e.id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                while ( rs.next() )
                {
                    event.load( rs );

                }
            }
        }

        return event;
    }

    public void addInterestNotification( long eventId, Long userId, Long eventCreatorId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO interest_notification ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
        {
            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, eventCreatorId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();
        }

        sendInterestMessage( eventId, userId, conn );
    }

    public void sendInterestMessage( long eventId, long userId, Connection conn) throws SQLException
    {
        Set<Long> friendIdList = getFriendIdList( userId, conn );
        Set<Long> interestedIdList = loadEventInterested( eventId, conn );
        Set<Long> interestedFriendIdList = interestedIdList.stream().filter( friendIdList::contains ).collect( Collectors.toSet());
        List<String> tokenList = getDeviceTokensByUserId( interestedFriendIdList, conn );
        Event event = loadEventById( eventId, conn );

        try
        {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification( Notification.builder()
                            .setTitle( event.getDescription() )
                            .setBody("A friend liked your idea" )
                            .build() )
//                    .putData("score", "850")
//                    .putData("time", "2:45")
                    .addAllTokens(tokenList)
                    .build();

            firebaseCloudMessaging.sendMultiMessage( message, tokenList );
        }
        catch ( FirebaseMessagingException e )
        {
            e.printStackTrace();
        }
    }

    public void sendPeekMessage( long eventId, long userId, long friendId, Connection conn) throws SQLException
    {
        Set<Long> friendIdList = new HashSet<>();
        friendIdList.add( friendId );
        List<String> tokenList = getDeviceTokensByUserId( friendIdList, conn );
        BasicProfile currUserProfile = getUserProfileById( userId, conn );
        Event event = loadEventById( eventId, conn );

        try
        {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification( Notification.builder()
                            .setTitle( event.getDescription() )
                            .setBody( currUserProfile.getDisplayName() + " Peeked at you" )
                            .build() )
                    .addAllTokens(tokenList)
                    .build();

            firebaseCloudMessaging.sendMultiMessage( message, tokenList );
        }
        catch ( FirebaseMessagingException e )
        {
            e.printStackTrace();
        }
    }

    public void sendInviteMessage( long confirmedEventId, Set<Long> invitedUserList, long userId, Connection conn ) throws SQLException
    {
        List<String> tokenList = getDeviceTokensByUserId( invitedUserList, conn );
        BasicProfile currUserProfile = getUserProfileById( userId, conn );
        Event event = loadConfirmedEventById( confirmedEventId, conn );

        try
        {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification( Notification.builder()
                            .setTitle( event.getDescription() )
                            .setBody(currUserProfile.getDisplayName() + " invited you" )
                            .build() )
                    .addAllTokens(tokenList)
                    .build();

            firebaseCloudMessaging.sendMultiMessage( message, tokenList );
        }
        catch ( FirebaseMessagingException e )
        {
            e.printStackTrace();
        }
    }

    public void sendJoinMessage( long confirmedEventId, long userId, Connection conn ) throws SQLException
    {
        Set<Long> friendIdList = getFriendIdList( userId, conn );
        Set<Long> activeIdList = loadCurrentActivityByEventId( confirmedEventId, conn ).keySet();
        Set<Long> activeFriendIdList = activeIdList.stream().filter( friendIdList::contains ).collect( Collectors.toSet());
        List<String> tokenList = getDeviceTokensByUserId( activeFriendIdList, conn );
        BasicProfile currUserProfile = getUserProfileById( userId, conn );
        Event event = loadConfirmedEventById( confirmedEventId, conn );

        try
        {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification( Notification.builder()
                            .setTitle( event.getDescription() )
                            .setBody( currUserProfile.getDisplayName() + " joined your event" )
                            .build() )
                    .addAllTokens(tokenList)
                    .build();

            firebaseCloudMessaging.sendMultiMessage( message, tokenList );
        }
        catch ( FirebaseMessagingException e )
        {
            e.printStackTrace();
        }
    }

    public List<String> getDeviceTokensByUserId( Set<Long> userIdList, Connection conn) throws SQLException
    {
        List<String> tokenList = new ArrayList<>();

        StringBuilder sqlSb =  new StringBuilder("SELECT DISTINCT token FROM device_token dt WHERE dt.user_id IN ( ");

        String delim = "";

        for ( Long userId : userIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " ) " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            for ( Long userId : userIdList )
            {
                ps.setLong( count++, userId );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                while ( rs.next() )
                {
                    int col = 1;
                    String token = rs.getString( col++ );

                    tokenList.add( token );
                }
            }
        }

        return tokenList;
    }

    public void removeInvalidDeviceTokens( Long userId, String token, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM device_token WHERE user_id = ? AND token = ? " ) )
        {
            int count = 1;

            ps.setLong( count++, userId );
            ps.setString( count++, token );

            //execute query
            ps.executeUpdate();
        }
    }

    public HttpEntity<BasicResponse> loadEventResponseById( long eventId, Long userId )
    {
        EventResponse eventResponse;

        try ( Connection conn = getConnection() )
        {
            eventResponse = loadEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( eventResponse ) );
    }

    public EventResponse loadEventResponseById( long eventId, Long userId, Connection conn ) throws SQLException
    {
        EventResponse eventResponse = new EventResponse();
        Set<Long> interestedList = loadEventInterested( eventId, conn );
        boolean isInterested = interestedList.contains( userId );

        Event event = loadEventById( eventId, conn );

        eventResponse.setEvent( event );
        eventResponse.setInterested( isInterested );

        return eventResponse;
    }

    public Set<Long> loadEventInterested( long eventId, Connection conn ) throws SQLException
    {
        Set<Long> interestedList = new HashSet<>();

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT ei.user_id FROM event_interested ei WHERE ei.event_id = ?" ) )
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
                    Long userId = rs.getLong( col++ );

                    interestedList.add( userId );
                }

            }
        }

        return interestedList;
    }

    public HttpEntity<BasicResponse> getInterestedPage( long eventId, Long userId, String pageKey )
    {
        DataListPage<InterestedFriendPageItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if ( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            List<InterestedFriendPageItem> feedItemList = loadInterestedFriendPage( eventId, userId, pageId, pageTimestampUtc, 21, conn );

            if ( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                InterestedFriendPageItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getUser().getUserId() + "/" + lastItem.getCreatedTime();
                dataListPage.setNextPageKey( nextPageKey );
                dataListPage.setItemList( feedItemList.subList( 0, 20 ) );
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dataListPage ) );
    }

    public List<InterestedFriendPageItem> loadInterestedFriendPage( long eventId, long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<InterestedFriendPageItem> feedItemList = new ArrayList<>();
        Map<Long, EventInterest> interestMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT ei.user_id, ei.created_time " );
        sqlSb.append( "FROM event_interested ei " );
        sqlSb.append( "WHERE ei.event_id = ? " );
        sqlSb.append( "AND ( ei.user_id IN ( SELECT friend_id FROM friend WHERE user_id = ? ) " );
        sqlSb.append( "OR ei.user_id  = ? ) " );
        if ( pageId != null && pageLastTimestampUtc != null )
        {
            sqlSb.append( "AND ( ei.created_time, ei.user_id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY ei.created_time DESC, ei.user_id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int pCount = 1;
            ps.setLong( pCount++, eventId );
            ps.setLong( pCount++, userId );
            ps.setLong( pCount++, userId );

            if ( pageId != null && pageLastTimestampUtc != null )
            {
                ps.setObject( pCount++, pageLastTimestampUtc );
                ps.setLong( pCount++, pageId );
            }

            ps.setInt( pCount++, limit );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;
                    Long interestedUserId = rs.getLong( col++ );
                    OffsetDateTime createdDt = rs.getObject( col++, OffsetDateTime.class );

                    EventInterest eventInterest = new EventInterest( eventId, interestedUserId );
                    eventInterest.setCreatedTime( createdDt );

                    interestMap.put( interestedUserId, eventInterest );
                }

            }
        }

        // Get interested friend ids
        Set<Long> interestedFriendIdList = interestMap.keySet();

        // Get interested friend details
        Map<Long, BasicProfile> interestedUserProfileMap = getUserProfileByList( interestedFriendIdList, conn );

        // Get friend visibility matrix for current user_profile
        Map<Long, List<Long>> visibilityMap = getInterestedVisibilityMatrix( eventId, conn );

        for ( Long interestedFriendId : interestedUserProfileMap.keySet() )
        {
            BasicProfile friendProfile = interestedUserProfileMap.get( interestedFriendId );
            EventInterest eventInterest = interestMap.get( interestedFriendId );

            boolean isPeekSent = false;
            boolean isPeekBack = false;

            if ( interestedFriendId != userId )
            {
                // Remove identifying details from non-visible friends

                List<Long> visibleToFriendList = visibilityMap.containsKey( interestedFriendId ) ? visibilityMap.get( interestedFriendId ) : new ArrayList<>();
                List<Long> visibleToCurrUserList = visibilityMap.containsKey( userId ) ? visibilityMap.get( userId ) : new ArrayList<>();

                if ( !visibleToCurrUserList.contains( interestedFriendId ) )
                {
                    friendProfile.setDisplayName( null );
                }
                if ( !visibleToCurrUserList.contains( interestedFriendId ) && visibleToFriendList.contains( userId ) )
                {
                    isPeekSent = true;
                }
                if ( visibleToCurrUserList.contains( interestedFriendId ) && !visibleToFriendList.contains( userId ) )
                {
                    isPeekBack = true;
                }
            }

            InterestedFriendPageItem interestedFriendFeedItem = new InterestedFriendPageItem();
            interestedFriendFeedItem.setUser( friendProfile );
            interestedFriendFeedItem.setPeekSent( isPeekSent );
            interestedFriendFeedItem.setPeekBack( isPeekBack );
            interestedFriendFeedItem.setCreatedTime( eventInterest.getCreatedTime() );

            feedItemList.add( interestedFriendFeedItem );
        }

        return feedItemList;
    }

    public BasicProfile getUserProfileById( Long userId, Connection conn ) throws SQLException
    {
        BasicProfile userProfile = null;

        try ( PreparedStatement ps = conn.prepareStatement( PARTIAL_USER_PROFILE_SELECT + " WHERE u.id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );


            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    userProfile = new BasicProfile();
                    userProfile.loadFromResultSet( rs );
                }

            }
        }

        return userProfile;
    }

    public Map<Long, BasicProfile> getUserProfileByList( Set<Long> userIdList, Connection conn ) throws SQLException
    {
        Map<Long, BasicProfile> userProfileList = new HashMap<>();

        if ( userIdList.isEmpty() )
        {
            return userProfileList;
        }

        StringBuilder friendSql = new StringBuilder( PARTIAL_USER_PROFILE_SELECT + " WHERE u.id IN ( " );

        String delim = " ";

        for ( Long participantUserId : userIdList )
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

            for ( Long userId : userIdList )
            {
                ps.setLong( pCount++, userId );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    BasicProfile participantUserProfile = new BasicProfile();
                    participantUserProfile.loadFromResultSet( rs );

                    userProfileList.put( participantUserProfile.getUserId(), participantUserProfile );
                }

            }
        }

        return userProfileList;
    }

    public Map<Long, List<Long>> getInterestedVisibilityMatrix( long eventId, Connection conn ) throws SQLException
    {
        Map<Long, List<Long>> visibilityMap = new HashMap<>();

        String sqlSb = "SELECT " +
                " user_id," +
                " friend_id " +
                "FROM event_visibility ev " +
                "WHERE ev.event_id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
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
                    Long userId = rs.getLong( col++ );
                    Long friendId = rs.getLong( col++ );

                    List<Long> visibleFriendList = new ArrayList<>();
                    visibleFriendList.add( friendId );

                    visibilityMap.merge( userId, visibleFriendList, ( currList, newList ) -> {
                        currList.addAll( newList );
                        return currList;
                    } );
                }

            }
        }

        return visibilityMap;
    }

    public HttpEntity<BasicResponse> sendVisibilityRequest( long eventId, Long userId, int friendId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                // Automatically add event interest
                addEventInterest( eventId, userId, conn );
                // Make user_profile visible to friend in the event.
                addEventVisibilityToFriend( eventId, userId, friendId, conn );

                // Add notifications to friend
                addPeekNotificationToFriend( eventId, userId, friendId, conn );
            }
            catch ( SQLException e )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw e;
            }

            conn.commit();
            conn.setAutoCommit( true );

            updatedEvent = loadEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException | ZoneRulesException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    private void addEventVisibilityToFriend( long eventId, Long userId, int friendId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_visibility ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? )" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            // Make curr user visible to the friend
            ps.setLong( count++, eventId );
            ps.setLong( count++, friendId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();

        }
    }

    private void addPeekNotificationToFriend( long eventId, Long userId, int friendId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO visibility_notification ( event_id, user_id, friend_id ) VALUES (?, ?, ?)" ) )
        {
            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, friendId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();
        }

        sendPeekMessage( eventId, userId, friendId, conn );
    }

    public HttpEntity<BasicResponse> createConfirmedEvent( ConfirmedEvent confirmedEvent, Long userId )
    {
        EventResponse createdEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                // Create confirmed event
                long confirmedEventId = -1L;

                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO confirmed_event (" +
                        " creator_id, " +
                        " description " +
                        " ) VALUES ( ?, ? ) RETURNING id" ) )
                {
                    ps.setFetchSize( 10 );

                    int count = 1;

                    ps.setLong( count++, userId );
                    ps.setString( count++, confirmedEvent.getDescription() );

                    //execute query
                    try ( ResultSet rs = ps.executeQuery() )
                    {
                        //position result to first

                        while ( rs.next() )
                        {
                            int col = 1;
                            long id = rs.getLong( col++ );

                            confirmedEventId = id;
                        }

                    }
                }

                confirmedEvent.setId( confirmedEventId );
                // Make creator active in the event
                joinEvent( confirmedEventId, userId, conn );

                //Add event invited users and send notifications to invited users
                addEventInviteNotificationByList( confirmedEventId, confirmedEvent.getInvitedList(), userId, conn );

                //Load event
                createdEvent = loadConfirmedEventResponseById( confirmedEventId, userId, conn );
            }
            catch ( SQLException ex )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit( true );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( createdEvent ) );
    }

    public HttpEntity<BasicResponse> joinEvent( Long eventId, Long userId )
    {
        CurrentActivity currentActivity;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                joinEvent( eventId, userId, conn );
                ConfirmedEvent event = loadConfirmedEventById( eventId, conn );
                addJoinNotification( eventId, userId, event.getCreatorId(), conn );
            }
            catch ( SQLException e )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw e;
            }
            conn.commit();
            conn.setAutoCommit( true );

            currentActivity = loadCurrentActivityByUserId( userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( currentActivity ) );
    }

    public void joinEvent( Long eventId, Long userId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO current_activity (" +
                " user_id," +
                " event_id" +
                " ) VALUES ( ?, ? ) " +
                "ON CONFLICT (user_id) DO UPDATE SET event_id = ?, updated_time = NOW()" ) )
        {
            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );
            ps.setLong( count++, eventId );

            //on duplicate key update params

            ps.setLong( count++, eventId );

            //execute query
            ps.executeUpdate();
        }
    }

    private void addJoinNotification( long confirmedEventId, Long userId, Long eventCreatorId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_join_notification ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
        {
            int count = 1;

            ps.setLong( count++, confirmedEventId );
            ps.setLong( count++, eventCreatorId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();

        }

        sendJoinMessage( confirmedEventId, userId, conn );
    }


    private void addEventInviteNotificationByList( long confirmedEventId, Set<Long> invitedUserList, long eventCreatorId, Connection conn ) throws SQLException
    {
        if ( !invitedUserList.isEmpty() )
        {
            StringBuilder updateSqlSb = new StringBuilder( "INSERT INTO event_invite_notification ( event_id, user_id, friend_id ) VALUES " );

            String delimiter = " ";

            for ( Long invitee : invitedUserList )
            {
                updateSqlSb.append( delimiter );
                updateSqlSb.append( "( ?, ?, ? )" );
                delimiter = ", ";
            }

            updateSqlSb.append( " ON CONFLICT DO NOTHING" );

            try ( PreparedStatement ps = conn.prepareStatement( updateSqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                for ( Long invitee : invitedUserList )
                {
                    ps.setLong( count++, confirmedEventId );
                    ps.setLong( count++, invitee );
                    ps.setLong( count++, eventCreatorId );
                }

                //execute query
                ps.executeUpdate();

            }
        }

        sendInviteMessage( confirmedEventId, invitedUserList, eventCreatorId, conn );
    }

    public HttpEntity<BasicResponse> getConfirmedEventById( long eventId, Long userId )
    {
        EventResponse confirmedEvent;

        try ( Connection conn = getConnection() )
        {
            confirmedEvent = loadConfirmedEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( confirmedEvent ) );
    }

    public EventResponse loadConfirmedEventResponseById( long confirmedEventId, long userId, Connection conn ) throws SQLException
    {
        EventResponse eventResponse = new EventResponse();

        CurrentActivity currentActivity = loadCurrentActivityByUserId( userId, conn );
        ConfirmedEvent event = loadConfirmedEventById( confirmedEventId, conn );

        eventResponse.setEvent( event );

        return eventResponse;
    }

    private ConfirmedEvent loadConfirmedEventById( long confirmedEventId, Connection conn ) throws SQLException
    {
        ConfirmedEvent event = null;

        String sqlSb = "SELECT " + CONFIRMED_EVENT_SELECT +
                " WHERE ce.id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1 );

            int count = 1;

            ps.setLong( count++, confirmedEventId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    event = new ConfirmedEvent();
                    event.load( rs );
                }

            }

        }
        return event;
    }

    public HttpEntity<BasicResponse> leaveEvent( Long userId )
    {
        CurrentActivity currentActivity;
        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                // Remove event interested user_profile
                try ( PreparedStatement ps = conn.prepareStatement( "UPDATE current_activity SET event_id = NULL, updated_time = NOW() WHERE user_id = ?" ) )
                {
                    int count = 1;

                    ps.setLong( count++, userId );

                    //execute query
                    ps.executeUpdate();
                }
            }
            catch ( SQLException ex )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit( true );

            currentActivity = loadCurrentActivityByUserId( userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( currentActivity ) );
    }

    public HttpEntity<BasicResponse> getActivePage( long eventId, Long userId, String pageKey )
    {
        DataListPage<ActivePageItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if ( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            List<ActivePageItem> feedItemList = loadActivePage( eventId, userId, pageId, pageTimestampUtc, 21, conn );

            if ( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                ActivePageItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getUser().getUserId() + "/" + lastItem.getActiveTime();
                dataListPage.setNextPageKey( nextPageKey );
                dataListPage.setItemList( feedItemList.subList( 0, 20 ) );
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dataListPage ) );
    }

    public List<ActivePageItem> loadActivePage( long eventId, long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<ActivePageItem> feedItemList = new ArrayList<>();
        Map<Long, CurrentActivity> currentActivityMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT ca.user_id, ca.event_id, ca.updated_time " );
        sqlSb.append( "FROM current_activity ca " );
        sqlSb.append( "WHERE ca.event_id = ? " );
        if ( pageId != null && pageLastTimestampUtc != null )
        {
            sqlSb.append( "AND ( ca.updated_time, ca.user_id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY ca.updated_time DESC, ca.user_id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int pCount = 1;
            ps.setLong( pCount++, eventId );

            if ( pageId != null && pageLastTimestampUtc != null )
            {
                ps.setObject( pCount++, pageLastTimestampUtc );
                ps.setLong( pCount++, pageId );
            }

            ps.setInt( pCount++, limit );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    CurrentActivity currentActivity = new CurrentActivity();
                    currentActivity.load( rs );

                    currentActivityMap.put( currentActivity.getUserId(), currentActivity );
                }

            }
        }

        // Get interested friend details
        Map<Long, BasicProfile> userProfileMap = getUserProfileByList( currentActivityMap.keySet(), conn );

        for ( Long participantId : currentActivityMap.keySet() )
        {
            CurrentActivity currentActivity = currentActivityMap.get( participantId );

            ActivePageItem activePageItem = new ActivePageItem();
            activePageItem.setUser( userProfileMap.get( participantId ) );
            activePageItem.setActiveTime( currentActivity.getUpdatedTime() );

            feedItemList.add( activePageItem );
        }

        return feedItemList;
    }

    public HttpEntity<BasicResponse> getAppNotifications( Long userId, String pageKey )
    {
        DataListPage<AppNotification> dataListPage = new DataListPage<>();
        List<AppNotification> notificationList;

        try ( Connection conn = getConnection() )
        {
            Long beforeId = null;
            OffsetDateTime beforeTimestampUtc = null;

            if ( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                beforeId = Long.parseLong( list[0] );
                beforeTimestampUtc = OffsetDateTime.parse( list[1] );
            }
            else
            {
                OffsetDateTime lastTimestampUtc = getLastTimestampUtc( userId, conn );
                refreshAppNotifications( userId, lastTimestampUtc, conn );
            }

            notificationList = loadAppNotifications( userId, beforeId, beforeTimestampUtc, 21, conn );

            if ( notificationList.size() <= 20 )
            {
                dataListPage.setItemList( notificationList );
            }
            else
            {
                AppNotification lastItem = notificationList.get( notificationList.size() - 2 );
                String nextPageKey = lastItem.getId() + "/" + lastItem.getCreatedDt();
                dataListPage.setNextPageKey( nextPageKey );
                dataListPage.setItemList( notificationList.subList( 0, 20 ) );
            }

        }
        catch ( SQLException | URISyntaxException | JsonProcessingException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dataListPage ) );
    }

    //TODO Performance
    public OffsetDateTime getLastTimestampUtc( long userId, Connection conn ) throws SQLException
    {
        OffsetDateTime lastTimestampUtc = null;

        // Get new notifications
        String sqlSb = "SELECT " +
                "i.created_time " +
                "FROM app_notification i " +
                "WHERE i.user_id = ? " +
                "ORDER BY i.created_time DESC " +
                "LIMIT 1";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;
                    OffsetDateTime createdUtc = rs.getObject( col++, OffsetDateTime.class );

                    lastTimestampUtc = createdUtc;
                }

            }
        }

        return lastTimestampUtc;
    }

    public void refreshAppNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException, JsonProcessingException
    {
        List<EventNotification> eventNotificationList = new ArrayList<>();

        eventNotificationList.addAll( loadInterestNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadPeekNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadEventInviteNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadEventJoinNotifications( userId, lastTimestampUtc, conn ) );

        List<AppNotification> appNotificationList = generateAppNotifications( eventNotificationList, conn );
        saveAppNotifications( userId, appNotificationList, conn );
    }

    public List<EventNotification> loadInterestNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException
    {
        // Get new notifications
        String sqlSb = "SELECT " +
                "i.event_id, " +
                "i.user_id, " +
                "i.friend_id, " +
                "i.created_time " +
                "FROM interest_notification i WHERE i.user_id = ? ";

        return loadEventNotifications( userId, lastTimestampUtc, conn, sqlSb, EventInterestNotification::new );
    }

    public List<EventNotification> loadPeekNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException
    {
        // Get new notifications
        String sqlSb = "SELECT " +
                "i.event_id, " +
                "i.user_id, " +
                "i.friend_id, " +
                "i.created_time " +
                "FROM visibility_notification i WHERE i.user_id = ?";

        return loadEventNotifications( userId, lastTimestampUtc, conn, sqlSb, EventPeekNotification::new );
    }

    public List<EventNotification> loadEventInviteNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException
    {
        // Get new notifications
        String sqlSb = "SELECT " +
                "i.event_id, " +
                "i.user_id, " +
                "i.friend_id, " +
                "i.created_time " +
                "FROM event_invite_notification i WHERE i.user_id = ? ";

        return loadEventNotifications( userId, lastTimestampUtc, conn, sqlSb, EventInviteNotification::new );
    }

    public List<EventNotification> loadEventJoinNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException
    {
        // Get new notifications
        String sqlSb = "SELECT " +
                "i.event_id, " +
                "i.user_id, " +
                "i.friend_id, " +
                "i.created_time " +
                "FROM event_join_notification i WHERE i.user_id = ? ";

        return loadEventNotifications( userId, lastTimestampUtc, conn, sqlSb, EventJoinNotification::new );
    }

    private List<EventNotification> loadEventNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn, String sqlSb, EventNotificationFactory factory ) throws SQLException
    {
        List<EventNotification> notificationList = new ArrayList<>();

        if ( lastTimestampUtc != null )
        {
            sqlSb += "AND i.created_time > ?";
        }

        Map<Long, List<NotificationModel>> notificationModelMap = new HashMap<>();

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {
            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            if ( lastTimestampUtc != null )
            {
                ps.setObject( count++, lastTimestampUtc );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;
                    long eventId = rs.getLong( col++ );
                    long appUserId = rs.getLong( col++ );
                    int friendId = rs.getInt( col++ );
                    OffsetDateTime createdUtc = rs.getObject( col++, OffsetDateTime.class );

                    NotificationModel notificationModel = new NotificationModel( eventId, friendId, createdUtc );

                    List<NotificationModel> notificationModelList = new ArrayList<>();
                    notificationModelList.add( notificationModel );

                    notificationModelMap.merge( eventId, notificationModelList, ( currList, newList ) -> {
                        currList.addAll( newList );
                        return currList;
                    } );
                }

            }
        }

        for ( Long eventId : notificationModelMap.keySet() )
        {
            List<NotificationModel> eventInterestList = notificationModelMap.get( eventId );

            List<Long> interestedFriendIdList = eventInterestList.stream().map( NotificationModel::getFriendId ).collect( Collectors.toList() );
            List<OffsetDateTime> createdTimeUtcList = eventInterestList.stream().map( NotificationModel::getCreatedTimeUtc ).sorted( Comparator.reverseOrder() ).collect( Collectors.toList() );

            EventNotification interestNotification = factory.construct();
            interestNotification.setEventId( eventId );
            interestNotification.setTimestampUtc( createdTimeUtcList.get( 0 ) );
            interestNotification.setData( interestedFriendIdList );

            notificationList.add( interestNotification );
        }
        return notificationList;
    }

    public List<AppNotification> generateAppNotifications( List<EventNotification> eventNotificationList, Connection conn ) throws SQLException, JsonProcessingException
    {
        List<AppNotification> notificationList = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();

        Set<Long> eventIdSet = new HashSet<>();
        ;
        Set<Long> confirmedEventIdSet = new HashSet<>();
        ;
        Set<Long> userIdSet = new HashSet<>();

        for ( EventNotification eventNotification : eventNotificationList )
        {
            if ( eventNotification.getType() == AppNotificationType.EVENT_INTEREST
                    || eventNotification.getType() == AppNotificationType.EVENT_PEEK
            )
            {
                eventIdSet.add( eventNotification.getEventId() );
            }
            else if ( eventNotification.getType() != AppNotificationType.NONE )
            {
                confirmedEventIdSet.add( eventNotification.getEventId() );
            }
            userIdSet.addAll( eventNotification.getData() );
        }

        Map<Long, Event> eventMap = loadEventByIdList( eventIdSet, conn );
        Map<Long, ConfirmedEvent> confirmedEventMap = loadConfirmedEventByIdList( confirmedEventIdSet, conn );
        Map<Long, BasicProfile> userProfileMap = getUserProfileByList( userIdSet, conn );

        for ( EventNotification eventNotification : eventNotificationList )
        {
            if ( eventNotification.getType() == AppNotificationType.EVENT_INTEREST )
            {
                List<Long> interestedUserIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                if ( interestedUserIdList.size() == 1 )
                {
                    message = "A friend is interested in the same idea";
                }
                else
                {
                    int count = interestedUserIdList.size();
                    message = ( count ) + "+ friends are interested in the same idea";
                }

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if ( eventNotification.getType() == AppNotificationType.EVENT_PEEK )
            {
                List<Long> friendIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                if ( friendIdList.size() == 1 )
                {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    message = friendProfile.getDisplayName() + " is peeking at you";
                }
                else
                {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    int count = friendIdList.size();
                    message = friendProfile.getDisplayName() + " and " + ( count ) + "+ friends are peeking at you";
                }

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if ( eventNotification.getType() == AppNotificationType.EVENT_INVITE )
            {
                List<Long> friendIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                Event event = confirmedEventMap.get( eventNotification.getEventId() );
                message = friendProfile.getDisplayName() + " is inviting you to the event " + event.getDescription();

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if ( eventNotification.getType() == AppNotificationType.EVENT_JOIN )
            {
                List<Long> friendIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                if ( friendIdList.size() == 1 )
                {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    message = friendProfile.getDisplayName() + " joined your event";
                }
                else
                {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    int count = friendIdList.size();
                    message = friendProfile.getDisplayName() + " and " + ( count ) + "+ friends joined your event";
                }

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
        }

        return notificationList;
    }

    public Map<Long, Event> loadEventByIdList( Set<Long> eventIdList, Connection conn ) throws SQLException
    {
        Map<Long, Event> eventMap = new HashMap<>();

        if ( eventIdList.isEmpty() )
        {
            return eventMap;
        }

        StringBuilder sqlSb = new StringBuilder( "SELECT " + EVENT_SELECT +
                "WHERE e.id IN ( " );

        String delim = " ";

        for ( Long eventId : eventIdList )
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

            for ( Long eventId : eventIdList )
            {
                ps.setLong( count++, eventId );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                while ( rs.next() )
                {
                    Event event = new Event();
                    event.load( rs );

                    eventMap.put( event.getId(), event );
                }
            }
        }

        return eventMap;
    }

    public Map<Long, ConfirmedEvent> loadConfirmedEventByIdList( Set<Long> eventIdList, Connection conn ) throws SQLException
    {
        Map<Long, ConfirmedEvent> eventMap = new HashMap<>();

        if ( eventIdList.isEmpty() )
        {
            return eventMap;
        }

        StringBuilder sqlSb = new StringBuilder( "SELECT " + CONFIRMED_EVENT_SELECT +
                "WHERE ce.id IN ( " );

        String delim = " ";

        for ( Long eventId : eventIdList )
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

            for ( Long eventId : eventIdList )
            {
                ps.setLong( count++, eventId );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                while ( rs.next() )
                {
                    ConfirmedEvent event = new ConfirmedEvent();
                    event.load( rs );

                    eventMap.put( event.getId(), event );
                }
            }
        }

        return eventMap;
    }

    public void saveAppNotifications( long userId, List<AppNotification> notificationList, Connection conn ) throws SQLException
    {
        if ( notificationList.isEmpty() )
        {
            return;
        }

        StringBuilder sqlSb = new StringBuilder(
                "INSERT INTO app_notification ( " +
                        "user_id, " +
                        "message, " +
                        "payload, " +
                        "type_id, " +
                        "created_time " +
                        ") VALUES "
        );

        String delim = " ";

        for ( AppNotification notification : notificationList )
        {
            sqlSb.append( delim );
            sqlSb.append( "( ?, ?, ?, ?, ? )" );
            delim = ", ";
        }

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {
            int count = 1;

            for ( AppNotification notification : notificationList )
            {
                ps.setLong( count++, userId );
                ps.setString( count++, notification.getMessage() );
                ps.setString( count++, notification.getPayload() );
                ps.setInt( count++, notification.getType() );
                ps.setObject( count++, notification.getCreatedDt() );
            }

            //execute query
            ps.executeUpdate();
        }
    }

    public List<AppNotification> loadAppNotifications( Long userId, Long beforeId, OffsetDateTime beforeTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<AppNotification> notificationList = new ArrayList<>();

        // Get new notifications
        StringBuilder sqlSb = new StringBuilder( "SELECT " +
                "i.id, " +
                "i.user_id, " +
                "i.message, " +
                "i.payload, " +
                "i.type_id, " +
                "i.created_time " +
                "FROM app_notification i " +
                "WHERE i.user_id = ? " );

        if ( beforeId != null && beforeTimestampUtc != null )
        {
            sqlSb.append( "AND ( i.id, i.created_time ) < ( ?, ? ) " );
        }

        sqlSb.append( "ORDER BY i.id DESC, i.created_time DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            if ( beforeId != null && beforeTimestampUtc != null )
            {
                ps.setLong( count++, beforeId );
                ps.setObject( count++, beforeTimestampUtc );
            }

            ps.setInt( count++, limit );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;
                    long id = rs.getLong( col++ );
                    long appUserId = rs.getLong( col++ );
                    String message = rs.getString( col++ );
                    String payload = rs.getString( col++ );
                    int typeId = rs.getInt( col++ );
                    OffsetDateTime createdUtc = rs.getObject( col++, OffsetDateTime.class );

                    AppNotification notification = new AppNotification();
                    notification.setId( id );
                    notification.setMessage( message );
                    notification.setType( typeId );
                    notification.setPayload( payload );
                    notification.setCreatedDt( createdUtc );

                    notificationList.add( notification );
                }

            }
        }

        return notificationList;
    }

    public HttpEntity<BasicResponse> getFriendCount( Long userId )
    {
        int friendCount = 0;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                String sqlSb = "SELECT COUNT(*) FROM friend f WHERE f.user_id = ?";

                try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, userId );

                    //execute query
                    try ( ResultSet rs = ps.executeQuery() )
                    {
                        //position result to first

                        while ( rs.next() )
                        {
                            int col = 1;
                            friendCount = rs.getInt( col++ );
                        }

                    }

                }
            }
            catch ( SQLException e )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw e;
            }
            conn.commit();
            conn.setAutoCommit( true );
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( friendCount ) );
    }

    public HttpEntity<BasicResponse> getPopsPage( Long userId, String pageKey )
    {
        DataListPage<UpcomingFeedItem> dataListPage = new DataListPage<>();
        List<UpcomingFeedItem> feedItemList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
            // Get Friend User Profiles from DB
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            // None of the user_profile's facebook friends are using the DoSomething app
            if ( friendIdList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if ( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            // Get events by friends
            feedItemList.addAll( loadPopsPageItemList( friendIdList, userId, pageId, pageTimestampUtc, 21, conn ) );

            if ( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                UpcomingFeedItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getEvent().getId() + "/" + lastItem.getFirstInterestedTime();
                dataListPage.setNextPageKey( nextPageKey );
                dataListPage.setItemList( feedItemList.subList( 0, 20 ) );
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dataListPage ) );
    }

    public Set<Long> getFriendIdList( Long userId, Connection conn ) throws SQLException
    {
        Set<Long> friendUserIdList = new HashSet<>();

        String friendSql = "SELECT f.friend_id FROM friend f WHERE f.user_id = ?";

        try ( PreparedStatement ps = conn.prepareStatement( friendSql ) )
        {

            ps.setFetchSize( 1000 );

            ps.setLong( 1, userId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int count = 1;

                    Long friendUserId = rs.getLong( count++ );

                    friendUserIdList.add( friendUserId );
                }

            }
        }

        return friendUserIdList;
    }

    public List<UpcomingFeedItem> loadPopsPageItemList( Set<Long> friendIdList, Long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<UpcomingFeedItem> feedItems = new ArrayList<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " );
        sqlSb.append(
                "e.id, " +
                        "e.description, " +
                        "MIN( ei.created_time ) first_interested_time  "
        );
        sqlSb.append( "FROM event_interested ei, event e  " );
        sqlSb.append( "WHERE ei.user_id IN ( " );

        String delim = " ";
        for ( Long friendId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " ) " ); //Close IN
        sqlSb.append( "AND e.id = ei.event_id  " );
        sqlSb.append( "GROUP BY e.id, e.description  " );
        if ( pageId != null && pageLastTimestampUtc != null )
        {
            sqlSb.append( "HAVING ( MIN( ei.created_time ), e.id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY first_interested_time DESC, e.id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int count = 1;

            for ( Long friendId : friendIdList )
            {
                ps.setLong( count++, friendId );
            }

            if ( pageId != null && pageLastTimestampUtc != null )
            {
                ps.setObject( count++, pageLastTimestampUtc );
                ps.setLong( count++, pageId );
            }

            ps.setLong( count++, limit );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first
                while ( rs.next() )
                {
                    int col = 1;

                    long eventId = rs.getLong( col++ );
                    String description = rs.getString( col++ );
                    OffsetDateTime firstInterestedTime = rs.getObject( col++, OffsetDateTime.class );
//                        String enUserId = rs.getString( col++ );
//                        String need = rs.getString( col++ );
//                        Date startDate = rs.getDate( col++ );
//                        Date endDate = rs.getDate( col++ );
//                        String dateScope = rs.getString( col++ );
//                        boolean isConfirmed = rs.getBoolean( col++ );

                    Set<Long> interestedList = loadEventInterested( eventId, conn );

                    UpcomingFeedItem feedItem = new UpcomingFeedItem();

                    Event event = new Event();
                    event.setId( eventId );
                    event.setDescription( description );

                    feedItem.setEvent( event );
                    feedItem.setFirstInterestedTime( firstInterestedTime );
                    feedItem.setInterested( interestedList.contains( userId ) );

                    feedItems.add( feedItem );
                }

            }
        }

        return feedItems;
    }

    public HttpEntity<BasicResponse> getNowPage( Long userId, String pageKey )
    {
        DataListPage<HappeningFeedItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            // Get Friend User Profiles from DB
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            // None of the user_profile's facebook friends are using the DoSomething app
            if ( friendIdList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if ( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            // Get active  events by friends
            List<HappeningFeedItem> feedItemList = loadNowPageItemList( friendIdList, userId, pageId, pageTimestampUtc, 21, conn );

            if ( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                HappeningFeedItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getEvent().getId() + "/" + lastItem.getStartTime();
                dataListPage.setNextPageKey( nextPageKey );
                dataListPage.setItemList( feedItemList.subList( 0, 20 ) );
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dataListPage ) );
    }

    public List<HappeningFeedItem> loadNowPageItemList( Set<Long> friendIdList, Long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<HappeningFeedItem> happeningFeedItemList = new ArrayList<>();

        Map<Long, ConfirmedEvent> eventMap = new LinkedHashMap<>();

        final String nonAggregateColumns = "e.id, e.description, e.creator_id, e.created_time ";

        StringBuilder sqlSb = new StringBuilder( "SELECT " );
        sqlSb.append( nonAggregateColumns );
        sqlSb.append( "FROM current_activity ca, confirmed_event e  " );
        sqlSb.append( "WHERE ca.event_id IS NOT NULL " );
        sqlSb.append( "AND ( ca.user_id = ? OR ca.user_id IN ( " );

        String delim = " ";
        for ( Long friendId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " ) ) " ); //Close IN
        sqlSb.append( "AND e.id = ca.event_id  " );
        sqlSb.append( "GROUP BY " + nonAggregateColumns + " " );
        if ( pageId != null && pageLastTimestampUtc != null )
        {
            sqlSb.append( "HAVING ( e.created_time, e.id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY e.created_time DESC, e.id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;
            ps.setLong( count++, userId );

            for ( Long friendId : friendIdList )
            {
                ps.setLong( count++, friendId );
            }

            if ( pageId != null && pageLastTimestampUtc != null )
            {
                ps.setObject( count++, pageLastTimestampUtc );
                ps.setLong( count++, pageId );
            }

            ps.setLong( count++, limit );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first
                while ( rs.next() )
                {
                    ConfirmedEvent event = new ConfirmedEvent();
                    event.load( rs );

                    eventMap.put( event.getId(), event );
                }
            }
        }

        for ( Long eventId : eventMap.keySet() )
        {
            ConfirmedEvent event = eventMap.get( eventId );
            Map<Long, CurrentActivity> activityMap = loadCurrentActivityByEventId( event.getId(), conn);

            Set<Long> activeFriendIdList = activityMap.keySet().stream().filter( e -> friendIdList.contains( e ) || e.equals( userId ) ).collect( Collectors.toSet());;

            Set<Long> firstThree = activeFriendIdList.stream().limit( 3 ).filter( Objects::nonNull ).collect( Collectors.toSet());
            Map<Long, BasicProfile> confirmedParticipantMap = getUserProfileByList( firstThree, conn );

            HappeningFeedItem happeningFeedItem = new HappeningFeedItem();
            happeningFeedItem.setEvent( event );
            happeningFeedItem.setActiveCount( activityMap.size() );
            happeningFeedItem.setStartTime( event.getCreatedTime() );
            happeningFeedItem.setActiveFriendList( new ArrayList<>( confirmedParticipantMap.values() ) );

            happeningFeedItemList.add( happeningFeedItem );
        }

        return happeningFeedItemList;
    }

    public Map<Long, CurrentActivity> loadCurrentActivityByEventId( Long eventId, Connection conn ) throws SQLException
    {
        Map<Long, CurrentActivity> activityMap = new HashMap<>();

        String sqlSb = "SELECT " +
                "ca.user_id, " +
                "ca.event_id, " +
                "ca.updated_time " +
                "FROM current_activity ca " +
                "WHERE ca.event_id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
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
                    CurrentActivity activity = new CurrentActivity();
                    activity.load( rs );

                    activityMap.put( activity.getUserId(), activity );
                }

            }

        }

        return activityMap;
    }

    public HttpEntity<BasicResponse> getFriendList( Long userId, String pageKey )
    {
        DataListPage<FriendFeedItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Long pageId = null;
            String pageName = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageName = list[1];
            }

            List<FriendFeedItem> feedItemList = loadFriendPage( userId, pageId, pageName, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                FriendFeedItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getUser().getUserId().toString() + "/" + lastItem.getUser().getDisplayName();
                dataListPage.setNextPageKey( nextPageKey );
                dataListPage.setItemList( feedItemList.subList( 0, 20 ) );
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( dataListPage ) );
    }

    public List<FriendFeedItem> loadFriendPage( long userId, Long pageId, String pageName, int limit, Connection conn ) throws SQLException
    {
        List<FriendFeedItem> feedItemList = new ArrayList<>();

        Map<Long, BasicProfile> basicProfileMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT u.id, u.name " );
        sqlSb.append( "FROM friend f, user_profile u " );
        sqlSb.append( "WHERE f.user_id = ? " );
        sqlSb.append( "AND f.friend_id = u.id " );
        if( pageId != null && pageName != null )
        {
            sqlSb.append( "AND ( u.name, u.id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY u.name DESC, u.id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int pCount = 1;
            ps.setLong( pCount++, userId );

            if( pageId != null && pageName != null )
            {
                ps.setString( pCount++, pageName );
                ps.setLong( pCount++, pageId );
            }

            ps.setInt( pCount++, limit );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first
                while ( rs.next() )
                {
                    BasicProfile friendProfile = new BasicProfile();
                    friendProfile.loadFromResultSet( rs );

                    basicProfileMap.put( friendProfile.getUserId(), friendProfile );
                }

            }
        }

        Set<Long> friendIdList = basicProfileMap.keySet();

        for ( Long friendId : friendIdList )
        {
            BasicProfile basicProfile = basicProfileMap.get( friendId );

            FriendFeedItem friendFeedItem = new FriendFeedItem();
            friendFeedItem.setUser( basicProfile );
            feedItemList.add( friendFeedItem );
        }

        return feedItemList;
    }

    public HttpEntity<BasicResponse> createUser( UserProfile userProfile )
    {
        long newUserId = 0L;
        UserProfile newUser;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO user_profile ( firebase_uid, name, email ) VALUES ( ?, ?, ? ) RETURNING id" ) )
                {
                    ps.setFetchSize( 1 );

                    int count = 1;

                    ps.setString( count++, userProfile.getFirebaseUid() );
                    ps.setString( count++, userProfile.getDisplayName() );
                    ps.setString( count++, userProfile.getEmail() );

                    //execute query
                    try ( ResultSet rs = ps.executeQuery() )
                    {
                        //position result to first

                        while ( rs.next() )
                        {
                            int col = 1;
                            long id = rs.getLong( col++ );

                            newUserId = id;
                        }

                    }
                }

                newUser = getCompleteUserProfileById( newUserId, conn );
            }
            catch ( SQLException e )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw e;
            }

            conn.commit();
            conn.setAutoCommit( true );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( "Error", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( newUser ) );
    }

    public HttpEntity<BasicResponse> logout( UserProfile userProfile, String deviceToken )
    {
        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                removeInvalidDeviceTokens( userProfile.getUserId(), deviceToken, conn );
            }
            catch ( SQLException e )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw e;
            }

            conn.commit();
            conn.setAutoCommit( true );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( "Error", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( userProfile.getUserId() ) );
    }

    public HttpEntity<BasicResponse> addUserToken( Long userId, String token )
    {
        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit( false );
            try
            {
                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO device_token ( user_id, token ) VALUES ( ?, ? ) ON CONFLICT DO NOTHING" ) )
                {
                    int count = 1;

                    ps.setLong( count++, userId );
                    ps.setString( count++, token );

                    //execute query
                    ps.executeUpdate();
                }
            }
            catch ( SQLException e )
            {
                conn.rollback();
                conn.setAutoCommit( true );
                throw e;
            }

            conn.commit();
            conn.setAutoCommit( true );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( "Error", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( userId ) );
    }

    @Deprecated
    public HttpEntity<BasicResponse> linkWithFacebook( String facebookUserToken, UserProfile userProfile )
    {
        long userId = -1L;
        UserProfile updatedProfile;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                // update user_profile details from facebook
                PublicProfile publicProfile = facebookResource.getPublicProfile( userProfile.getFacebookId(), facebookUserToken );
//            PictureResponse pictureResponse = facebookResource.getProfilePicture( userProfile.getFacebookId(), facebookUserToken );

                String createSql = "INSERT INTO user_profile (" +
                        " firebase_uid, " +
                        " facebook_id, " +
                        " name, " +
                        " email, " +
                        " time_zone " +
                        " ) VALUES ( ?, ?, ?, ?, ? ) " +
                        "ON CONFLICT (firebase_uid) DO UPDATE SET facebook_id = ?, name = ?, email = ? RETURNING id";

                try ( PreparedStatement ps = conn.prepareStatement( createSql ) )
                {
                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setString( count++, userProfile.getFirebaseUid() );
                    ps.setString( count++, publicProfile.getId() );
                    ps.setString( count++, publicProfile.getName() );
                    ps.setString( count++, publicProfile.getEmail() );
                    ps.setString( count++, "Asia/Colombo" );

                    ps.setString( count++, publicProfile.getId() );
                    ps.setString( count++, publicProfile.getName() );
                    ps.setString( count++, publicProfile.getEmail() );

                    //execute query
                    try ( ResultSet rs = ps.executeQuery() )
                    {
                        //position result to first

                        while ( rs.next() )
                        {
                            int col = 1;
                            long id = rs.getLong( col++ );

                            userId = id;
                        }

                    }
                }

                //TODO user_friends permission is broken for test users
                // Get User Friends from Facebook
//                Map<String, String> facebookFriendList = facebookResource.getFacebookFriends( userProfile.getFacebookId(), facebookUserToken );

                // Get friend user ids using the friend facebook ID
                Map<String, Integer> facebookIdUserIdMap = new HashMap<>();
/*
                StringBuilder friendSQLSb = new StringBuilder( "SELECT u.id, u.facebook_id FROM user_profile u WHERE  u.facebook_id IN (" );

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

                        while ( rs.next() )
                        {
                            int col = 1;
                            int id = rs.getInt( col++ );
                            String facebookId = rs.getString( col++ );

                            facebookIdUserIdMap.put( facebookId, id );
                        }

                    }
                }*/

                if( !facebookIdUserIdMap.isEmpty() )
                {
                    // Create Friends list
                    StringBuilder updateSqlSb = new StringBuilder( "INSERT INTO friend VALUES " );

                    String delimiter = " ";

                    for ( String facebookId : facebookIdUserIdMap.keySet() )
                    {
                        updateSqlSb.append( delimiter );
                        updateSqlSb.append( "(?, ?), (?, ?)" );
                        delimiter = ", ";
                    }

                    updateSqlSb.append( " ON CONFLICT DO NOTHING" );

                    try ( PreparedStatement ps = conn.prepareStatement( updateSqlSb.toString() ) )
                    {

                        ps.setFetchSize( 1000 );

                        int count = 1;

                        for ( String facebookId : facebookIdUserIdMap.keySet() )
                        {
                            ps.setLong( count++, userId );
                            ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                            ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                            ps.setLong( count++, userId );
                        }

                        //execute query
                        ps.executeUpdate();
                    }
                }
            }
            catch(SQLException e)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw e;
            }

            conn.commit();
            conn.setAutoCommit(true);

            updatedProfile = getCompleteUserProfileById( userId, conn );
        }
        catch ( SQLException | URISyntaxException | InterruptedException | IOException | AppException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedProfile ) );
    }

}

