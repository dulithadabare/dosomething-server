package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.constant.AppNotificationType;
import com.dulithadabare.dosomething.constant.PrivacyPreference;
import com.dulithadabare.dosomething.facebook.PublicProfile;
import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.util.AppException;
import com.dulithadabare.dosomething.util.LocationHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Collectors;

public class DBResource
{
    FacebookResource facebookResource = new FacebookResource();

    private final String DB_URL = "jdbc:mariadb://localhost:3306/dosomething_db";
    private final String DB_USER = "demoroot";
    private final String DB_PASS = "demoroot";

    private final String EVENT_SELECT = " e.id," +
            " e.creator_id," +
            " e.description, " +
            " e.visibility_preference, " +
            " e.is_confirmed, " +
            " e.updated_time, " +
            " e.created_time " +
            " FROM event e ";
    private final String CONFIRMED_EVENT_SELECT = " ce.id," +
            " ce.event_id," +
            " ce.creator_id," +
            " ce.description, " +
            " ce.date, " +
            " ce.time, " +
            " ce.visibility_preference, " +
            " ce.is_public, " +
            " ce.is_happening, " +
            " ce.is_cancelled, " +
            " ce.updated_time, " +
            " ce.created_time " +
            "FROM confirmed_event ce ";

    private final String COMPLETE_USER_PROFILE_SELECT = "SELECT " +
            "u.id, " +
            "u.facebook_id, " +
            "u.firebase_uid, " +
            "u.name, " +
            "u.email, " +
            "u.latitude, " +
            "u.longitude, " +
            "u.high_school_id, " +
            "u.university_id, " +
            "u.work_place_id, " +
            "u.time_zone " +
            "FROM user_profile u ";

    private final String PARTIAL_USER_PROFILE_SELECT = "SELECT " +
            "u.id, " +
            "u.name " +
            "FROM user_profile u ";

    static class ActiveEvent
    {
        private long eventId;
        private List<CurrentActivity> currentActivityList;

        public long getEventId()
        {
            return eventId;
        }

        public void setEventId( long eventId )
        {
            this.eventId = eventId;
        }

        public List<CurrentActivity> getCurrentActivityList()
        {
            return currentActivityList;
        }

        public void setCurrentActivityList( List<CurrentActivity> currentActivityList )
        {
            this.currentActivityList = currentActivityList;
        }
    }

    public static Connection getConnection() throws SQLException, URISyntaxException
    {
//        URI dbUri = new URI(System.getenv("JDBC_DATABASE_URL"));
//
//        String username = dbUri.getUserInfo().split(":")[0];
//        String password = dbUri.getUserInfo().split(":")[1];
//        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?sslmode=require";

        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        return DriverManager.getConnection(dbUrl);
    }

    public HttpEntity<BasicResponse> createAnonymousUser( UserProfile userProfile, String timeZone )
    {
        long newUserId = 0L;
        UserProfile newUser;

        try ( Connection conn = getConnection() )
        {
            // Create User

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO user_profile ( firebase_uid, time_zone ) VALUES ( ?, ? ) RETURNING id" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, userProfile.getFirebaseUid() );
                ps.setString( count++, timeZone );

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

            // Get created user_profile id from DB

//            newUserId = getLastCreatedValueForUser( conn );
            newUser = getCompleteUserProfileById( newUserId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( newUser ) );
    }

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

    public HttpEntity<BasicResponse> getPopularNearbyFeed( PopularFeedRequest popularFeedRequest, Long userId )
    {
        List<PopularNearbyFeedItem> popularNearbyFeedItemList;

        try ( Connection conn = getConnection() )
        {
            // Update friend list from facebook
            // updateFriendList();

            // Get Friend User Profiles from DB
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            // Get active  events by friends
            Map<Long, List<CurrentActivity>> activeEventMap = loadPopularActivityList( popularFeedRequest, conn );
            List<ActiveEvent> activeEventList = new ArrayList<>();

            for ( Long activeEventId : activeEventMap.keySet() )
            {
                ActiveEvent activeEvent = new ActiveEvent();
                activeEvent.setEventId( activeEventId );
                activeEvent.setCurrentActivityList( activeEventMap.get( activeEventId ) );

                activeEventList.add( activeEvent );
            }

            // Sort current active events by active user_profile count DESC
            activeEventList.sort( ( o1, o2 ) -> Integer.compare( o2.currentActivityList.size(), o1.currentActivityList.size() ) );

            // Get the top 20 most active events
            List<ActiveEvent> popularActiveEventList = new ArrayList<>();
            for ( int i = 0; i < 20 && i < activeEventList.size() ; i++ )
            {
                popularActiveEventList.add( activeEventList.get( i ) );
            }

            // Get list of popular event Ids
            List<Long> eventIdSet = popularActiveEventList.stream().map( ActiveEvent::getEventId ).collect( Collectors.toList());

            popularNearbyFeedItemList = loadPopularNearbyFeedItemList( eventIdSet, friendIdList, activeEventMap, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        // Sort event by active count DESC.
        // The event with the most active users will be the first in the list.
        popularNearbyFeedItemList.sort( (o1, o2) -> Long.compare( o2.getActiveCount(), o1.getActiveCount() ) );

        return new HttpEntity<>( new BasicResponse( popularNearbyFeedItemList ) );
    }

    public HttpEntity<BasicResponse> getHappeningFeed( Long userId, String pageKey )
    {
        DataListPage<HappeningFeedItem> dataListPage = new DataListPage<>();

        List<ActiveFeedItem> activeFeedItemList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
//            updateFriendList();

            // Get Friend User Profiles from DB
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            // None of the user_profile's facebook friends are using the DoSomething app
            if ( friendIdList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            // Get active  events by friends
            List<HappeningFeedItem> feedItemList = loadHappeningFeedItemList( friendIdList, userId, pageId, pageTimestampUtc, 21, conn );

            if( feedItemList.size() <= 20 )
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

    public HttpEntity<BasicResponse> getUpcomingFeed( Long userId, String pageKey )
    {
        DataListPage<UpcomingFeedItem> dataListPage = new DataListPage<>();
        List<UpcomingFeedItem> feedItemList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
//            updateFriendList();

            // Get Friend User Profiles from DB
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            // None of the user_profile's facebook friends are using the DoSomething app
            if ( friendIdList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            // Get events by friends
            feedItemList.addAll( loadUpcomingEvents( friendIdList, userId, pageId, pageTimestampUtc, 21, conn ) );

            if( feedItemList.size() <= 20 )
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

    public HttpEntity<BasicResponse> startCurrentActivity( CurrentActivity activity, Long userId )
    {
        CurrentActivity currentActivity = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                // Start activity in the event
                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO current_activity (" +
                        " user_id," +
                        " event_id" +
                        " ) VALUES ( ?, ? ) " +
                        "ON CONFLICT (user_id) DO UPDATE SET event_id = ?, updated_time = NOW()" ) )
                {
                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, userId );
                    ps.setLong( count++, activity.getEventId() );

                    //on duplicate key update params

                    ps.setLong( count++, activity.getEventId() );

                    //execute query
                    ps.executeUpdate();
                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                    return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
                }
                //get current activity
                currentActivity = getCurrentActivityByUserId( userId, conn );

                // update user_profile activity history
                updateUserActivityHistory( currentActivity, conn );

                // TODO Check if this can send multiple notifications if two users start the event at the same time
                int activeCount = getEventActiveCount( activity.getEventId(), conn );

                if( activeCount == 1 )
                {
                    // Event was started by the user
                    updateConfirmedEventHappeningStatus(activity.getEventId(), true, conn);

                    //Add notification to participants
                    Map<Long, EventInvited> eventInvitedMap = getEventInvited( activity.getEventId(), conn );
                    List<Long> participantList = eventInvitedMap.values().stream().filter( EventInvited::isConfirmed ).map( EventInvited::getUserId ).collect( Collectors.toList());
                    addEventStartNotification( activity.getEventId(), participantList, conn );
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
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( currentActivity ) );
    }

    public HttpEntity<BasicResponse> stopCurrentActivity( CurrentActivity currentActivity, Long userId )
    {
        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                // Remove event interested user_profile
                try ( PreparedStatement ps = conn.prepareStatement( "UPDATE current_activity SET event_id = NULL, updated_time = NOW() WHERE user_id = ?" ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, userId );

                    //execute query
                    ps.executeUpdate();

                }

                int activeCount = getEventActiveCount( currentActivity.getEventId(), conn );

                if( activeCount == 1 )
                {
                    // Event was stopped
                    updateConfirmedEventHappeningStatus( currentActivity.getEventId(), false, conn);
                }
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( userId ) );
    }

    public void updateConfirmedEventHappeningStatus( long confirmedEventId, boolean isHappening, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE confirmed_event SET is_happening = ? WHERE id = ?" ) )
        {
            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setBoolean( count++, isHappening );
            ps.setLong( count++, confirmedEventId );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }
    }

    private int getEventActiveCount( long confirmedEventId, Connection conn ) throws SQLException
    {
        int activeCount = 0;

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT COUNT(*) FROM current_activity ca WHERE ca.event_id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, confirmedEventId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first
                while ( rs.next() )
                {
                    int col = 1;
                    activeCount = rs.getInt( col++ );
                }
            }
        }
        return activeCount;
    }

    public void updateUserActivityHistory( CurrentActivity activity, Connection conn )
    {
        CurrentActivity currentActivity;

        // Start event

        String sql = "INSERT INTO user_activity_history (" +
                " user_id," +
                " event_id ," +
                " updated_time" +
                " ) VALUES ( ?, ?, NOW() ) " +
                "ON CONFLICT (user_id, event_id) DO UPDATE SET updated_time = NOW()";

        try ( PreparedStatement ps = conn.prepareStatement( sql ) )
        {
            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, activity.getUserId() );
            ps.setLong( count++, activity.getEventId() );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

    }

    public HttpEntity<BasicResponse> getCurrentActivityByUserId( Long userId )
    {
        CurrentActivity currentActivity;

        try ( Connection conn = getConnection() )
        {
            currentActivity = getCurrentActivityByUserId( userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( currentActivity ) );
    }

    public CurrentActivity getCurrentActivityByUserId( Long userId, Connection conn ) throws SQLException
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

    public Map<Long, CurrentActivity> getCurrentActivityByUserIdList( Set<Long> userIdList, Connection conn ) throws SQLException
    {
        Map<Long, CurrentActivity> currentActivityMap = new HashMap<>();

        if ( userIdList.isEmpty() )
        {
            return currentActivityMap;
        }

        StringBuilder sqlSb = new StringBuilder("SELECT " +
                "ca.user_id, " +
                "ca.event_id, " +
                "ca.updated_time " +
                "FROM current_activity ca " +
                "WHERE ca.user_id IN ( ");

        String delim = " ";

        for ( Long userId : userIdList )
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

            for ( Long userId : userIdList )
            {
                ps.setLong( count++, userId );
            }

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

        return currentActivityMap;
    }

    public Map<Long, List<CurrentActivity>> getRecentUserActivityHistory( Set<Long> userIdList, Connection conn ) throws SQLException
    {
        Map<Long, List<CurrentActivity>> activityMap = new HashMap<>();

        // init map
        for ( Long userId : userIdList )
        {
            activityMap.put( userId, new ArrayList<>() );
        }

        if( userIdList.isEmpty() )
        {
            return activityMap;
        }

        StringBuilder sqlSb = new StringBuilder("SELECT " +
                "uah.user_id, " +
                "uah.event_id, " +
                "uah.updated_time " +
                "FROM user_activity_history uah " +
                "WHERE uah.user_id IN ( ");

        String delim = "";

        for ( Long userId : userIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " ) " );
        sqlSb.append( "AND uah.updated_time >= ( NOW() - INTERVAL '7 DAY' ) " );
        sqlSb.append( "ORDER BY uah.updated_time DESC" );

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
                //position result to first

                while ( rs.next() )
                {
                    CurrentActivity currentActivity = new CurrentActivity();
                    currentActivity.load( rs );

                    List<CurrentActivity> currentActivityList = new ArrayList<>();
                    currentActivityList.add( currentActivity );

                    activityMap.merge( currentActivity.getUserId(), currentActivityList, ( old, curr) -> {
                        old.addAll( curr );
                        return old;
                    } );
                }

            }

        }

        return activityMap;
    }

    public List<CurrentActivity> getRecentUserActivityHistoryById( Long userId, Connection conn ) throws SQLException
    {
        List<CurrentActivity> currentActivityList = new ArrayList<>();

        String sqlSb = "SELECT " +
                "uah.user_id, " +
                "uah.event_id, " +
                "uah.updated_time " +
                "FROM user_activity_history uah " +
                "WHERE uah.user_id = ? " +
                "AND uah.updated_time >= ( NOW() - INTERVAL '7 DAY' )";

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
                    CurrentActivity currentActivity = new CurrentActivity();
                    currentActivity.load( rs );

                    currentActivityList.add( currentActivity );
                }

            }
        }

        return currentActivityList;
    }

    public HttpEntity<BasicResponse> createEvent( Event event, Long userId )
    {
        long eventId = -1;
        EventResponse createdEvent = null;

        try ( Connection conn = getConnection() )
        {

            conn.setAutoCommit(false);
            try
            {
                // Create event
                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event (" +
                        " creator_id ," +
                        " description," +
                        " visibility_preference" +
                        " ) VALUES ( ?, ?, ? ) RETURNING id" ) )
                {
                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, userId );
                    ps.setString( count++, event.getDescription() );
                    ps.setInt( count++, 0 ); //TODO remove visibility preference

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

                if( !event.getTagList().isEmpty() )
                {
                    StringBuilder tagSqlSb = new StringBuilder(
                            "INSERT INTO event_tag (" +
                                    " event_id ," +
                                    " tag " +
                                    " ) VALUES "
                    );

                    String delim = "";

                    for ( String tag : event.getTagList() )
                    {
                        tagSqlSb.append( delim );
                        tagSqlSb.append( "( ?, ?)" );
                        delim = ", ";
                    }

                    tagSqlSb.append( "ON CONFLICT DO NOTHING" );

                    try ( PreparedStatement ps = conn.prepareStatement( tagSqlSb.toString() ) )
                    {
                        ps.setFetchSize( 1000 );

                        int count = 1;

                        for ( String tag : event.getTagList() )
                        {
                            ps.setLong( count++, eventId );
                            ps.setString( count++, tag );
                        }
                        //execute query
                        ps.executeUpdate();
                    }
                }

                //Add event interest for creator
                EventInterest eventInterest = new EventInterest( eventId, userId, "Creator" );
                addEventInterest( eventId, userId, eventInterest, conn );

                // Load updated event
                createdEvent = loadEventResponseById( eventId, userId, conn );
            }
            catch(SQLException e)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw e;
            }

            conn.commit();
            conn.setAutoCommit(true);
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( "Error", BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( createdEvent ) );
    }

    public List<String> getTagListByEventId( long eventId, Connection conn )
    {
        List<String> tagList = new ArrayList<>();

        String friendSql = "SELECT et.tag FROM event_tag et WHERE et.event_id = ?";

        try ( PreparedStatement ps = conn.prepareStatement( friendSql ) )
        {

            ps.setFetchSize( 1000 );

            ps.setLong( 1, eventId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int count = 1;

                    String tag = rs.getString( count++ );

                    tagList.add( tag );
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

        return tagList;
    }

    public HttpEntity<BasicResponse> updateEvent( Event event, Long userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            // Update event
            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event SET " +
                    " description = ?," +
                    " visibility_preference = ?," +
                    " WHERE id = ? " ) )
            {
                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, event.getDescription() );
                ps.setInt( count++, event.getVisibilityPreference() );
                ps.setLong( count++, event.getId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            updatedEvent = loadEventResponseById( event.getId(), userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> createConfirmedEvent( ConfirmedEvent confirmedEvent, Long userId )
    {
        EventResponse createdEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                // Create confirmed event
                long confirmedEventId = -1L;

                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO confirmed_event (" +
                        " event_id," +
                        " creator_id," +
                        " description," +
                        " date," +
                        " time," +
                        " is_public," +
                        " visibility_preference" +
                        " ) VALUES ( ?, ?, ?, ?, ?, ?, ? ) RETURNING id" ) )
                {
                    Date date = confirmedEvent.getDate() != null && !confirmedEvent.getDate().isEmpty() ? Date.valueOf( confirmedEvent.getDate() ) : null;

                    Time time = confirmedEvent.getTime() != null && !confirmedEvent.getTime().isEmpty() ? Time.valueOf( LocalTime.parse( confirmedEvent.getTime() ) ) : null;

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, confirmedEvent.getEventId() );
                    ps.setLong( count++, userId );
                    ps.setString( count++, confirmedEvent.getDescription() );
                    ps.setDate( count++, date );
                    ps.setTime( count++, time );
                    ps.setBoolean( count++, confirmedEvent.isPublic() );
                    ps.setInt( count++, confirmedEvent.getVisibilityPreference() );

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

                //Add event creator as participant
                addEventInvitedUser( confirmedEventId, userId, conn );
                confirmEventParticipation( confirmedEventId, userId, conn );

                //Add event invited users and send notifications to invited users
                addEventInvitedUserByList( confirmedEventId, confirmedEvent.getInvitedList(), conn );
                addEventInviteNotificationByList( confirmedEventId, confirmedEvent.getInvitedList(), userId, conn );


                // Mark original event as confirmed
                try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event SET " +
                        " is_confirmed = TRUE" +
                        " WHERE id = ?" ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;
                    ps.setLong( count++, confirmedEventId );

                    //execute query
                    ps.executeUpdate();

                }

                //Load event
                createdEvent = loadConfirmedEventResponseById( confirmedEventId, userId, conn );
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( createdEvent ) );
    }

    public HttpEntity<BasicResponse> addInvites( ConfirmedEvent confirmedEvent, Long userId )
    {
        EventResponse createdEvent;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                Map<Long, EventInvited> oldEventInvitedMap = getEventInvited( confirmedEvent.getId(), conn );

                Set<Long> removedInvitedList = oldEventInvitedMap.keySet().stream().filter( e -> !confirmedEvent.getInvitedList().contains( e ) && !e.equals( userId ) ).collect( Collectors.toSet());
                Set<Long> newInvitedList = confirmedEvent.getInvitedList().stream().filter( e -> !oldEventInvitedMap.containsKey( e ) ).collect( Collectors.toSet());

                // Add new invited users and send event notifications
                addEventInvitedUserByList( confirmedEvent.getId(), newInvitedList, conn );
                addEventInviteNotificationByList( confirmedEvent.getId(), newInvitedList, userId, conn );

                // Delete removed invited users
                removeEventInvitedUserByList( confirmedEvent.getId(), removedInvitedList, conn );

                //Load event
                createdEvent = loadConfirmedEventResponseById( confirmedEvent.getId(), userId, conn );
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch ( SQLException | URISyntaxException | NullPointerException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( createdEvent ) );
    }

    public HttpEntity<BasicResponse> updateConfirmedEvent( ConfirmedEvent confirmedEvent, Long userId )
    {
        //TODO only creator with userId can update a confirmed event
        EventResponse updatedEvent;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                try ( PreparedStatement ps = conn.prepareStatement( "UPDATE confirmed_event SET " +
                        " date = ?," +
                        " time = ?" +
                        " WHERE id = ?" ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    Date date = confirmedEvent.getDate() != null ? Date.valueOf( confirmedEvent.getDate() ) : null;
                    Time time = confirmedEvent.getTime() != null ? Time.valueOf( LocalTime.parse( confirmedEvent.getTime() ) ) : null;

                    ps.setDate( count++, date );
                    ps.setTime( count++, time );
                    ps.setLong( count++, confirmedEvent.getId() );

                    //execute query
                    ps.executeUpdate();
                }

                Map<Long, EventInvited> oldEventInvitedMap = getEventInvited( confirmedEvent.getId(), conn );

                Set<Long> removedInvitedList = oldEventInvitedMap.keySet().stream().filter( e -> !confirmedEvent.getInvitedList().contains( e ) && !e.equals( userId ) ).collect( Collectors.toSet());
                Set<Long> newInvitedList = confirmedEvent.getInvitedList().stream().filter( e -> !oldEventInvitedMap.containsKey( e ) ).collect( Collectors.toSet());

                // Add new invited users and send event notifications
                addEventInvitedUserByList( confirmedEvent.getId(), newInvitedList, conn );
                addEventInviteNotificationByList( confirmedEvent.getId(), newInvitedList, userId, conn );

                // Delete removed invited users
                removeEventInvitedUserByList( confirmedEvent.getId(), removedInvitedList, conn );
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);

            //Load updated event
            updatedEvent = loadConfirmedEventResponseById( confirmedEvent.getId(), userId.intValue(), conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }
    public HttpEntity<BasicResponse> cancelConfirmedEvent( long confirmedEventId, Long userId )
    {
        //TODO only creator with userId can cancel a confirmed event
        EventResponse updatedEvent;

        try ( Connection conn = getConnection() )
        {
            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE confirmed_event SET " +
                    " is_cancelled = TRUE" +
                    " WHERE id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, confirmedEventId );

                //execute query
                ps.executeUpdate();
            }

            //Load updated event
            updatedEvent = loadConfirmedEventResponseById( confirmedEventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }


    private void addEventInvitedUser( long confirmedEventId, Long invitedUserId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_participant ( event_id, user_id ) VALUES ( ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, confirmedEventId );
            ps.setLong( count++, invitedUserId );

            //execute query
            ps.executeUpdate();

        }
    }

    private void addEventInvitedUserByList( long confirmedEventId, Set<Long> invitedUserList, Connection conn ) throws SQLException
    {
        if ( !invitedUserList.isEmpty() )
        {
            StringBuilder insertSqlSb = new StringBuilder( "INSERT INTO event_participant ( event_id, user_id ) VALUES " );

            String delim = " ";

            for ( Long invitedUser :invitedUserList )
            {
                insertSqlSb.append( delim );
                insertSqlSb.append( "( ?, ? )" );
                delim = ", ";
            }

            insertSqlSb.append( " ON CONFLICT DO NOTHING" );

            try ( PreparedStatement ps = conn.prepareStatement( insertSqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                for ( Long invitedUser : invitedUserList )
                {
                    ps.setLong( count++, confirmedEventId );
                    ps.setLong( count++, invitedUser );
                }

                //execute query
                ps.executeUpdate();

            }
        }
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
    }

    public void removeEventInvitedUserByList( long eventId, Set<Long> removedInvitedUserList, Connection conn ) throws SQLException
    {
        if( !removedInvitedUserList.isEmpty() )
        {
            // Remove event invite
            StringBuilder inviteSqlSb = new StringBuilder("DELETE FROM event_invite WHERE event_id = ? AND receiver_id IN ( ");

            String delim = "";

            for ( Long invitedUser : removedInvitedUserList )
            {
                inviteSqlSb.append( delim );
                inviteSqlSb.append( "?" );
                delim = ", ";
            }

            inviteSqlSb.append( " )" );

            try ( PreparedStatement ps = conn.prepareStatement( inviteSqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                for ( Long invitedUser : removedInvitedUserList )
                {
                    ps.setLong( count++, invitedUser );
                }

                //execute query
                ps.executeUpdate();

            }

            // Remove from event participant
            StringBuilder participantSqlSb = new StringBuilder("DELETE FROM event_participant WHERE event_id = ? AND user_id IN ( ");

            delim = "";

            for ( Long invitedUser : removedInvitedUserList )
            {
                participantSqlSb.append( delim );
                participantSqlSb.append( "?" );
                delim = ", ";
            }

            participantSqlSb.append( " )" );

            try ( PreparedStatement ps = conn.prepareStatement( participantSqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );

                for ( Long invitedUser : removedInvitedUserList )
                {
                    ps.setLong( count++, invitedUser );
                }

                //execute query
                ps.executeUpdate();

            }
        }
    }

    public HttpEntity<BasicResponse> getConfirmedEventById( long eventId, Long userId )
    {
        EventResponse confirmedEvent = null;

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
        EventResponse eventResponse = null;

        String sqlSb = "SELECT " + CONFIRMED_EVENT_SELECT +
                " WHERE ce.id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, confirmedEventId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    eventResponse = new EventResponse();

                    boolean isPublic = rs.getBoolean( "is_public" );

                    Map<Long, EventInvited> invitedMap = getEventInvited( confirmedEventId, conn );
                    Map<Long, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( confirmedEventId, conn );

                    boolean isInvited = invitedMap.containsKey( userId );
                    boolean isJoinRequested = eventJoinRequestMap.containsKey( userId );
                    boolean isParticipating = false;

                    if ( isInvited )
                    {
                        isParticipating = invitedMap.get( userId ).isConfirmed();
                    }

                    int participantCount = 0;

                    for ( EventInvited eventInvited : invitedMap.values() )
                    {
                        if( eventInvited.isConfirmed() ) {
                            participantCount++;
                        }
                    }

                    ConfirmedEvent event = new ConfirmedEvent();

                    if ( isPublic || isInvited )
                    {
                        event.load( rs );
                        UserProfile creatorUser = getCompleteUserProfileById( event.getCreatorId(), conn );
                        event.setCreatorDisplayName( creatorUser.getDisplayName() );

                    }
                    else
                    {
                        event.loadPrivateEvent( rs );
                    }

                    List<String> tagList = getTagListByEventId( event.getEventId(), conn );
                    event.setTagList( tagList );

                    eventResponse.setConfirmedEvent( event );
                    eventResponse.setInvited( isInvited );
                    eventResponse.setParticipant( isParticipating );
                    eventResponse.setJoinRequested( isJoinRequested );
                    event.setParticipantCount( participantCount );
                }

            }
         
        }

        return eventResponse;
    }

    private void updateParticipantVisibility( long eventId, int userId, Map<Integer, List<Integer>> visibilityMap, Map<Integer, InvitedUser> participantMap )
    {
        for ( Integer participantId : participantMap.keySet() )
        {
            InvitedUser participant = participantMap.get( participantId );

            if ( !participant.isConfirmed() )
            {
                //if participant is not visible to current user_profile
                List<Integer> friendList = visibilityMap.get( participantId );

                if ( friendList == null || friendList.contains( userId ) )
                {
                    BasicProfile participantUser = participant.getUser();
                    participantUser.setDisplayName( null );
                }

                if ( participantId == userId )
                {
                    BasicProfile participantUser = participant.getUser();
                    participantUser.setDisplayName( "You" );
                }
            }
        }
    }

    @Deprecated
    public HttpEntity<BasicResponse> getInvitedUserList( long eventId, Long userId )
    {
        List<InvitedUser> invitedUserList = new ArrayList<>();
        try ( Connection conn = getConnection() )
        {
            Set<Long> friendIdList = getFriendIdList( userId, conn );
            Map<Long, EventInvited> invitedMap = getEventInvited( eventId, conn );
            Map<Long, UserProfile> userProfileMap = getUserProfilesWithDetails( invitedMap.keySet(), conn );
            Map<Long, List<Long>> visibilityMap = getInterestedVisibilityMatrix( eventId, conn );
            List<Integer> requestedFriendList = getVisibilityRequestedByUser( eventId, userId, conn );
            Map<Long, List<CurrentActivity>> userActivityHistoryMap = getRecentUserActivityHistory( invitedMap.keySet(), conn );
            UserProfile currUser = getCompleteUserProfileById( userId, conn );
            List<CurrentActivity> currUserRecentCurrentActivityHistory = getRecentUserActivityHistoryById( userId, conn );

            for ( Long invitedId : invitedMap.keySet() )
            {
                EventInvited eventInvited = invitedMap.get( invitedId );
                UserProfile userProfile = userProfileMap.get( invitedId );

                boolean isFriend = true;
                String distance = null;
                String relationship = null;

                // Update invited user_profile visibility
                if ( !eventInvited.isConfirmed() )
                {
                    List<Long> visibleToFriendList = visibilityMap.containsKey( invitedId ) ? visibilityMap.get( invitedId ) : new ArrayList<>();
                    List<Long> visibleToCurrUserList = visibilityMap.containsKey( userId ) ? visibilityMap.get( userId ) : new ArrayList<>();

                    if ( !visibleToCurrUserList.contains( invitedId )  )
                    {
                        userProfile.setDisplayName( null );
                    }

                    isFriend = friendIdList.contains( invitedId );

                    List<CurrentActivity> recentCurrentActivityList = userActivityHistoryMap.get( invitedId );

                    double distanceInMeters = LocationHelper.distance( currUser.getLatitude(), userProfile.getLatitude(), currUser.getLongitude(), userProfile.getLongitude(), 0.0, 0.0 );

                    if ( distanceInMeters <= 10000 )
                    {
                        distance = "nearby";
                    }
                    else
                    {
                        distance = "far away";
                    }

                    //Determine relationship
                    boolean isRecentlyMet = false;
                    for ( CurrentActivity currentActivity : recentCurrentActivityList )
                    {
                        for ( CurrentActivity currUserCurrentActivity : currUserRecentCurrentActivityHistory )
                        {
                            if ( currentActivity.getEventId() == currUserCurrentActivity.getEventId() )
                            {
                                isRecentlyMet = true;
                                break;
                            }
                        }
                    }

                    if ( isRecentlyMet )
                    {
                        relationship = "Interacted recently";
                    }
                }

                InvitedUser invitedUser = new InvitedUser();
//                invitedUser.setUser( userProfile );
                invitedUser.setUserId( userProfile.getUserId() );
                invitedUser.setConfirmed( eventInvited.isConfirmed() );
                invitedUser.setFriend( isFriend );
                invitedUser.setRelationship( relationship );
                invitedUser.setDistance( distance );
                invitedUser.setVisibilityRequested( requestedFriendList.contains( invitedId ) );

                invitedUserList.add( invitedUser );
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( invitedUserList ) );
    }

    public HttpEntity<BasicResponse> getActiveFriendListByEventId( long eventId, Long userId )
    {
        List<ActivePageItem> activePageItemList = new ArrayList<>();
        try ( Connection conn = getConnection() )
        {
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            Map<Long, CurrentActivity> currentActivityMap = new HashMap<>();

            StringBuilder sqlSb = new StringBuilder( "SELECT " +
                    "ca.user_id, " +
                    "ca.event_id, " +
                    "ca.updated_time " +
                    "FROM current_activity ca " +
                    "WHERE " +
                    "ca.event_id = ? " +
                    "AND ca.user_id IN ( ");

            String delim = " ";
            for ( Long friendId : friendIdList )
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

                ps.setLong( count++, eventId );

                for ( Long friendId : friendIdList )
                {
                    ps.setLong( count++, friendId );
                }

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
                catch ( SQLException e )
                {
                    e.printStackTrace();
                }
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            Map<Long, BasicProfile> userProfileMap = getUserProfileByList( currentActivityMap.keySet(), conn );

            for ( CurrentActivity currentActivity : currentActivityMap.values() )
            {
                ActivePageItem activePageItem = new ActivePageItem();
                activePageItem.setUser( userProfileMap.get( currentActivity.getUserId() ) );
                activePageItem.setActiveTime( currentActivity.getUpdatedTime() );

                activePageItemList.add( activePageItem );
            }

        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( activePageItemList ) );
    }

    public HttpEntity<BasicResponse> getActivePage( long eventId, Long userId, String pageKey )
    {
        DataListPage<ActivePageItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Long pageId = null;
            Boolean pageIsFriend = null;
            Boolean pageIsActive = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageIsFriend = Boolean.parseBoolean( list[1] );
                pageIsActive = Boolean.parseBoolean( list[2] );
            }

            List<ActivePageItem> feedItemList = loadActivePage( eventId, userId, pageId, pageIsFriend, pageIsActive, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                ActivePageItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getUser().getUserId() + "/" + lastItem.isFriend() + "/" + lastItem.isActive();
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

    public List<ActivePageItem> loadActivePage( long eventId, long userId, Long pageId, Boolean pageIsFriend, Boolean pageIsActive, int limit, Connection conn ) throws SQLException
    {
        List<ActivePageItem> feedItemList = new ArrayList<>();
        Map<Long, ActiveUser> participantMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT ei.user_id, " );
        sqlSb.append( "( ei.user_id IN ( SELECT f.friend_id FROM friend f WHERE f.user_id = ? ) OR ei.user_id = ? ) is_friend, " );
        sqlSb.append( "( ei.event_id IN ( SELECT ca.event_id FROM current_activity ca WHERE ca.user_id = ei.user_id ) ) is_active " );
        sqlSb.append( "FROM event_participant ei " );
        sqlSb.append( "WHERE ei.event_id = ? " );
        sqlSb.append( "AND ei.is_confirmed = TRUE " );
        if( pageId != null && pageIsFriend != null && pageIsActive != null )
        {
            sqlSb.append( "AND ( is_active, is_friend, ei.user_id ) < ( ?, ?, ? ) " );
        }
        sqlSb.append( "ORDER BY is_active DESC, is_friend DESC, ei.user_id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int pCount = 1;
            ps.setLong( pCount++, userId );
            ps.setLong( pCount++, userId );
            ps.setLong( pCount++, eventId );

            if( pageId != null && pageIsFriend != null && pageIsActive != null )
            {
                ps.setBoolean( pCount++, pageIsActive );
                ps.setBoolean( pCount++, pageIsFriend );
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
                    Long participantId = rs.getLong( col++ );
                    boolean isFriend = rs.getBoolean( col++ );
                    boolean isActive = rs.getBoolean( col++ );

                    ActiveUser activeUser = new ActiveUser();
                    activeUser.setParticipantId( participantId );
                    activeUser.setFriend( isFriend );
                    activeUser.setActive( isActive );

                    participantMap.put( participantId, activeUser );
                }

            }
        }

        // Get interested friend details
        Map<Long, BasicProfile> userProfileMap = getUserProfileByList( participantMap.keySet(), conn );
        Map<Long, CurrentActivity> userActivityMap = getCurrentActivityByUserIdList(participantMap.keySet(), conn);

        for ( Long participantId : participantMap.keySet() )
        {
            ActiveUser activeUser = participantMap.get( participantId );
            CurrentActivity currentActivity = userActivityMap.get( participantId );

            OffsetDateTime activeTime = activeUser.isActive() ? currentActivity.getUpdatedTime() : null;

            ActivePageItem activePageItem = new ActivePageItem();
            activePageItem.setUser( userProfileMap.get( participantId ) );
            activePageItem.setActive( activeUser.isActive() );
            activePageItem.setActiveTime( activeTime );
            activePageItem.setFriend( activeUser.isFriend() );

            feedItemList.add( activePageItem );
        }

        return feedItemList;
    }

    public Map<Long, EventInvited> getEventInvited( long eventId, Connection conn )
    {
        Map<Long, EventInvited> invitedMap = new HashMap<>();

        String sqlSb = "SELECT " +
                " user_id," +
                " is_confirmed " +
                "FROM event_participant ep " +
                "WHERE ep.event_id = ? ";

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
                    Long invitedId = rs.getLong( col++ );
                    boolean isConfirmed = rs.getBoolean( col++ );

                    EventInvited eventInvited = new EventInvited();
                    eventInvited.setEventId( eventId );
                    eventInvited.setUserId( invitedId );
                    eventInvited.setConfirmed( isConfirmed );

                    invitedMap.put( invitedId, eventInvited );
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

        return invitedMap;
    }

    public  List<Integer> getVisibilityRequestedByUser( long eventId, Long userId, Connection conn )
    {
        List<Integer> visibilityRequestedIdList = new ArrayList<>();

        // Get Event Visibility Requests

        String sqlSb = "SELECT " +
                "vr.user_id " +
                "FROM visibility_request vr WHERE vr.event_id = ? AND vr.requester_id = ?";

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, userId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;
                    int friendUserId = rs.getInt( col++ );

                    visibilityRequestedIdList.add( friendUserId );
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

        return visibilityRequestedIdList;
    }

    public Map<Long, List<Long>> getInterestedVisibilityMatrix( long eventId, Connection conn )
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
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return visibilityMap;
    }

    public HttpEntity<BasicResponse> acceptEventInvite( long confirmedEventId, Long userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                // Add event participation
                confirmEventParticipation( confirmedEventId, userId, conn );

                // Remove event invite
                removeEventInvite( confirmedEventId, userId, conn );

                // Add accept notification
                updatedEvent = loadConfirmedEventResponseById( confirmedEventId, userId, conn );
                Long senderId = updatedEvent.getConfirmedEvent().getCreatorId();
                addEventInviteAcceptNotification( confirmedEventId, userId, senderId, conn );
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);

            // Load updated event
            updatedEvent = loadConfirmedEventResponseById( confirmedEventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> declineEventInvite( long eventId, Long userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            // Remove event invite
            removeEventInvite( eventId, userId, conn );

            // Load updated event
            updatedEvent = loadConfirmedEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    private void removeEventInvite( long eventId, Long userId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_invite WHERE event_id = ? AND receiver_id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();

        }
    }

    public void confirmEventParticipation( long eventId, Long participantId, Connection conn ) throws SQLException
    {
        // Add new interested user_profile to the event
        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event_participant SET is_confirmed = TRUE WHERE event_id = ? AND user_id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, participantId );

            //execute query
            ps.executeUpdate();
        }
    }

    public HttpEntity<BasicResponse> cancelEventParticipation( long eventId, Long userId )
    {
        EventResponse updatedEvent;

        // Add new interested user_profile to the event
        try ( Connection conn = getConnection() )
        {
            // Remove event participation
            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event_participant SET is_confirmed = FALSE WHERE event_id = ? AND user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setLong( count++, userId );

                //execute query
                ps.executeUpdate();

            }

            // Load updated event
            updatedEvent = loadConfirmedEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> addJoinRequest( long eventId, Long userId, long updatedTime )
    {
        EventResponse updatedEvent;

        try ( Connection conn = getConnection() )
        {
            // Add new join request
            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_join_request ( event_id, requester_id ) VALUES ( ?, ? ) ON CONFLICT DO NOTHING" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setLong( count++, userId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Add Notifications
            Long eventCreatorId = loadConfirmedEventResponseById( eventId, userId, conn ).getConfirmedEvent().getCreatorId();
            addJoinNotification( eventId, userId, eventCreatorId, conn );

            // Load updated event
            updatedEvent = loadConfirmedEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> removeJoinRequest( long confirmedEventId, Long userId )
    {
        EventResponse updatedEvent;

        try ( Connection conn = getConnection() )
        {
            // Add new join request
            removeJoinRequest( confirmedEventId, userId, conn );

            //Get updated event
            updatedEvent = loadConfirmedEventResponseById( confirmedEventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public void removeJoinRequest( long eventId, Long requesterId, Connection conn ) throws SQLException
    {
        // Delete join request
        try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_join_request WHERE event_id = ? AND requester_id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, requesterId );

            //execute query
            ps.executeUpdate();

        }
    }

    public HttpEntity<BasicResponse> getJoinRequests( long confirmedEventId )
    {
        List<JoinRequest> joinRequestList = new ArrayList<>();

        Map<Long, EventJoinRequest> eventJoinRequestMap;
        try ( Connection conn = getConnection() )
        {
            // Get Join Requests
            eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( confirmedEventId, conn );

            Map<Long, BasicProfile> joinRequestedUserMap = getUserProfileByList( eventJoinRequestMap.keySet(), conn );

            for ( EventJoinRequest eventJoinRequest : eventJoinRequestMap.values() )
            {
                BasicProfile user_profile = joinRequestedUserMap.get( eventJoinRequest.getUserId() );
                JoinRequest joinRequest = new JoinRequest( user_profile, eventJoinRequest.getEventId(), eventJoinRequest.getCreatedTime() );

                joinRequestList.add( joinRequest );
            }

        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( joinRequestList ) );
    }

    public Map<Long, EventJoinRequest> getEventJoinRequestsByConfirmedEventId( long eventId, Connection conn ) throws SQLException
    {
        Map<Long, EventJoinRequest> eventJoinRequestMap = new HashMap<>();

        String selectSql = "SELECT " +
                "jr.event_id, " +
                "jr.requester_id, " +
                "jr.created_time " +
                "FROM event_join_request jr WHERE jr.event_id = ?";

        try ( PreparedStatement ps = conn.prepareStatement( selectSql ) )
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

                    long jrConfirmedEventId = rs.getLong( col++ );
                    Long requesterId = rs.getLong( col++ );
                    long createdTime = rs.getTimestamp( col++ ).getTime();

                    EventJoinRequest eventJoinRequest = new EventJoinRequest();
                    eventJoinRequest.setEventId( jrConfirmedEventId );
                    eventJoinRequest.setUserId( requesterId );
                    eventJoinRequest.setCreatedTime( createdTime );

                    eventJoinRequestMap.put( requesterId, eventJoinRequest );
                }

            }
        }

        return eventJoinRequestMap;
    }

    public HttpEntity<BasicResponse> acceptJoinRequest( long confirmedEventId, Long userId, Long requesterId )
    {
        EventResponse updatedEvent;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                // Add event participation
                addEventInvitedUser( confirmedEventId, requesterId, conn );
                confirmEventParticipation( confirmedEventId, requesterId, conn );

                // Remove join request
                removeJoinRequest( confirmedEventId, requesterId, conn );

                //Add notification
                addJoinAcceptNotification( confirmedEventId, userId, requesterId, conn );
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);

            // Load updated event
            updatedEvent = loadConfirmedEventResponseById( confirmedEventId, requesterId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> addEventInterest( long eventId, Long userId, EventInterest eventInterest )
    {
        EventResponse eventResponse;
        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                addEventInterest( eventId, userId, eventInterest, conn );
                // Add new interest notification
                Event event = loadEventById( eventId, conn );
                addInterestNotification( eventId, userId, event.getCreatorId(), conn );
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);

            // Load updated event
            eventResponse = loadEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( eventResponse ) );

    }

    public void addEventInterest( long eventId, Long userId, EventInterest eventInterest, Connection conn ) throws SQLException
    {
        // Add new interested user to the event
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_interested ( event_id, user_id, description ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, userId );
            ps.setString( count++, eventInterest.getDescription() );

            //execute query
            ps.executeUpdate();

        }
    }

    public HttpEntity<BasicResponse> removeEventInterest( long eventId, Long userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
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
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);

            updatedEvent = loadEventResponseById( eventId, userId, conn );

        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public List<FeedItem> updatePopularConfirmedEvent( ConfirmedEvent event, Connection conn )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        // insert tag

        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO popular_confirmed_event ( event_id, updated_time ) VALUES  ( ?, ? ) ON DUPLICATE KEY UPDATE updated_time = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, event.getId() );
//            ps.setTimestamp( count++, new Timestamp( event.getCreatedTime() ) );

            //on duplicate key update
//            ps.setTimestamp( count++, new Timestamp( event.getCreatedTime() ) );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public HttpEntity<BasicResponse> sendVisibilityRequest( long eventId, Long userId, int friendId, String userTimeZone )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                // Automatically add event interest
                EventInterest eventInterest = new EventInterest( eventId, userId, "Peek" );
                addEventInterest( eventId, userId, eventInterest, conn );
                // Make user_profile visible to friend in the event.
                addEventVisibilityToFriend( eventId, userId, friendId, conn );

                // Add notifications to friend
                addPeekNotificationToFriend( eventId, userId, friendId, conn );
            }
            catch(SQLException e)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw e;
            }

            conn.commit();
            conn.setAutoCommit(true);

            updatedEvent = loadEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException | ZoneRulesException  e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    private void addVisibilityRequest( long eventId, int userId, int friendId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO visibility_request ( event_id, user_id, requester_id ) VALUES ( ?, ?, ? )" ) )
        {
            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, friendId );
            ps.setLong( count++, userId ); // Curr user_profile is the requester

            //execute query
            ps.executeUpdate();

        }
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

    public void addInterestNotification( long eventId, Long userId, Long eventCreatorId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO interest_notification ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setLong( count++, eventCreatorId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();

        }
    }

    private void addEventInviteAcceptNotification( long confirmedEventId, Long userId, Long senderId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO invite_accept_notification ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, confirmedEventId );
            ps.setLong( count++, senderId );
            ps.setLong( count++, userId );

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
    }

    private void addJoinAcceptNotification( long confirmedEventId, Long userId, Long requesterId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO join_accept_notification ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
        {
            int count = 1;

            ps.setLong( count++, confirmedEventId );
            ps.setLong( count++, requesterId );
            ps.setLong( count++, userId );

            //execute query
            ps.executeUpdate();

        }
    }

    private void addEventStartNotification( long eventId, List<Long> userIdList, Connection conn ) throws SQLException
    {
        StringBuilder stringBuilder = new StringBuilder("INSERT INTO event_start_notification ( event_id, user_id ) VALUES ");

        String delim = " ";

        for ( Long userId : userIdList )
        {
            stringBuilder.append( delim );
            stringBuilder.append( "(?, ?)" );
            delim = ", ";
        }

        try ( PreparedStatement ps = conn.prepareStatement( stringBuilder.toString() ) )
        {
            int count = 1;

            for ( Long userId : userIdList )
            {
                ps.setLong( count++, eventId );
                ps.setLong( count++, userId );
            }

            //execute query
            ps.executeUpdate();
        }
    }

    private OffsetDateTime getCurrentTime( String userTimeZone )
    {
        LocalDateTime dt = LocalDateTime.now();
        ZoneId zone = ZoneId.of( userTimeZone );
        ZonedDateTime zdt = dt.atZone(zone); // user current timestamp
        OffsetDateTime offsetDateTime = zdt.toOffsetDateTime();
        return offsetDateTime;
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
    }

    public HttpEntity<BasicResponse> removeEventVisibility( long eventId, Long userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            // Remove user_profile visibility

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_visibility  WHERE event_id = ? AND user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setLong( count++, userId );

                //execute query
                ps.executeUpdate();

            }

            updatedEvent = loadEventResponseById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse>  getVisibilityRequests( Long userId )
    {
        List<VisibilityRequest> notificationList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
            // Get Event Visibility Requests
            String sqlSb = "SELECT " +
                    "u.name, " +
                    "vr.requester_id, " +
                    "vr.created_time, " +
                    EVENT_SELECT +
                    " ,visibility_request vr, user_profile u " +
                    "WHERE vr.user_id = ? " +
                    "AND ( vr.created_time, vr.event_id, vr.requester_id) < ( ?, ?, ? )" +
                    "AND vr.event_id = e.id " +
                    "AND vr.requester_id = u.id " +
                    "ORDER BY vr.created_time DESC, vr.event_id DESC, vr.requester_id DESC " +
                    "LIMIT 10";

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
                        String requesterName = rs.getString( col++ );
                        Long requesterUserId = rs.getLong( col++ );
                        OffsetDateTime createdDt = rs.getObject( col++, OffsetDateTime.class );

                        Event event = new Event();
                        event.load( rs );

                        UserProfile friend = new UserProfile();
                        friend.setUserId( requesterUserId );
                        friend.setDisplayName( requesterName );

                        VisibilityRequest notification = new VisibilityRequest();
                        notification.setId( event.getId() + "-" + friend.getUserId() + "-" + userId );
                        notification.setUser( friend );
                        notification.setCreatedTimeUtc( createdDt.toInstant().toEpochMilli() );

                        notificationList.add( notification );
                    }

                }
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        // Sort by latest timestamp
        notificationList.sort( ( o1, o2 ) -> Long.compare( o2.getCreatedTimeUtc(), o1.getCreatedTimeUtc() ) );

        return new HttpEntity<>( new BasicResponse( notificationList ) );
    }

    //TODO Decline visibility request
    public HttpEntity<BasicResponse> declineVisibilityRequest( long eventId, Long userId, int requesterId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            // Remove user_profile visibility

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM visibility_request  WHERE event_id = ? AND user_id = ? AND requester_id = ?" ) )
            {
                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setLong( count++, userId );
                ps.setInt( count++, requesterId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            updatedEvent = loadEventResponseById( eventId, userId, conn );

        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public void refreshAppNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException, JsonProcessingException
    {
        List<EventNotification> eventNotificationList = new ArrayList<>();

        eventNotificationList.addAll( loadInterestNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadPeekNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadEventInviteNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadInviteAcceptNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadEventJoinNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadJoinAcceptNotifications( userId, lastTimestampUtc, conn ) );
        eventNotificationList.addAll( loadEventStartNotifications( userId, lastTimestampUtc, conn ) );

        List<AppNotification> appNotificationList = generateAppNotifications( eventNotificationList, conn );
        saveAppNotifications( userId, appNotificationList, conn );
    }

    public HttpEntity<BasicResponse> getAppNotifications( Long userId, String pageKey )
    {
        DataListPage<AppNotification> dataListPage = new DataListPage<>();
        List<AppNotification> notificationList;

        try ( Connection conn = getConnection() )
        {
            Long beforeId = null;
            OffsetDateTime beforeTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
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

            if( notificationList.size() <= 20 )
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

    public List<AppNotification> generateAppNotifications( List<EventNotification> eventNotificationList, Connection conn ) throws SQLException, JsonProcessingException
    {
        List<AppNotification> notificationList = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();

        Set<Long> eventIdSet = new HashSet<>();;
        Set<Long> confirmedEventIdSet = new HashSet<>();;
        Set<Long> userIdSet = new HashSet<>();

        for ( EventNotification eventNotification : eventNotificationList )
        {
            if( eventNotification.getType() == AppNotificationType.EVENT_INTEREST
                    || eventNotification.getType() == AppNotificationType.EVENT_PEEK
            )
            {
                eventIdSet.add( eventNotification.getEventId() );
            } else if ( eventNotification.getType() != AppNotificationType.NONE ) {
                confirmedEventIdSet.add( eventNotification.getEventId() );
            }
            userIdSet.addAll( eventNotification.getData() );
        }

        Map<Long, Event> eventMap = loadEventByIdList( eventIdSet, conn );
        Map<Long, ConfirmedEvent> confirmedEventMap = loadConfirmedEventByIdList( confirmedEventIdSet, conn );
        Map<Long, BasicProfile> userProfileMap = getUserProfileByList( userIdSet, conn );

        for ( EventNotification eventNotification : eventNotificationList )
        {
            if( eventNotification.getType() == AppNotificationType.EVENT_INTEREST )
            {
                List<Long> interestedUserIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                if( interestedUserIdList.size() == 1 )
                {
                    message = "A friend is interested in the same idea";
                } else {
                    int count = interestedUserIdList.size();
                    message = (count) + "+ friends are interested in the same idea";
                }

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if( eventNotification.getType() == AppNotificationType.EVENT_PEEK )
            {
                List<Long> friendIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                if( friendIdList.size() == 1 )
                {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    message = friendProfile.getDisplayName() + " is peeking at you";
                }
                else {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    int count = friendIdList.size();
                    message = friendProfile.getDisplayName() + " and " + (count) + "+ friends are peeking at you";
                }

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if( eventNotification.getType() == AppNotificationType.EVENT_INVITE )
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
            else if( eventNotification.getType() == AppNotificationType.EVENT_INVITE_ACCEPT )
            {
                List<Long> friendIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                if( friendIdList.size() == 1 )
                {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    message = friendProfile.getDisplayName() + " accepted your invite";
                }
                else {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    int count = friendIdList.size();
                    message = friendProfile.getDisplayName() + " and " + (count) + "+ friends accepted your invite";
                }

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if( eventNotification.getType() == AppNotificationType.EVENT_JOIN )
            {
                List<Long> friendIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                if( friendIdList.size() == 1 )
                {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    message = friendProfile.getDisplayName() + " wants to join your event";
                }
                else {
                    BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                    int count = friendIdList.size();
                    message = friendProfile.getDisplayName() + " and " + (count) + "+ friends want to join your event";
                }

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if( eventNotification.getType() == AppNotificationType.EVENT_JOIN_ACCEPT )
            {
                List<Long> friendIdList = eventNotification.getData();

                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                BasicProfile friendProfile = userProfileMap.get( friendIdList.get( 0 ) );
                message = friendProfile.getDisplayName() + " accepted your join request";

                AppNotification notification = new AppNotification();
                notification.setMessage( message );
                notification.setType( eventNotification.getType().getId() );
                notification.setPayload( payload );
                notification.setCreatedDt( eventNotification.getTimestampUtc() );

                notificationList.add( notification );
            }
            else if( eventNotification.getType() == AppNotificationType.EVENT_START )
            {
                String message;
                String payload = mapper.writeValueAsString( eventNotification );

                Event event = confirmedEventMap.get( eventNotification.getEventId() );
                message = event.getDescription() + " has started";

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

    public List<AppNotification> loadAppNotifications( Long userId, Long beforeId, OffsetDateTime beforeTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<AppNotification> notificationList = new ArrayList<>();

        // Get new notifications
        StringBuilder sqlSb =  new StringBuilder("SELECT " +
                "i.id, " +
                "i.user_id, " +
                "i.message, " +
                "i.payload, " +
                "i.type_id, " +
                "i.created_time " +
                "FROM app_notification i " +
                "WHERE i.user_id = ? ");

       if( beforeId != null && beforeTimestampUtc != null )
        {
            sqlSb.append(  "AND ( i.id, i.created_time ) < ( ?, ? ) " );
        }

        sqlSb.append( "ORDER BY i.id DESC, i.created_time DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            if( beforeId != null && beforeTimestampUtc != null )
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

    public List<EventNotification> loadInviteAcceptNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException
    {
        // Get new notifications
        String sqlSb = "SELECT " +
                "i.event_id, " +
                "i.user_id, " +
                "i.friend_id, " +
                "i.created_time " +
                "FROM invite_accept_notification i WHERE i.user_id = ? ";

        return loadEventNotifications( userId, lastTimestampUtc, conn, sqlSb, InviteAcceptNotification::new );
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

    public List<EventNotification> loadJoinAcceptNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException
    {
        // Get new notifications
        String sqlSb = "SELECT " +
                "i.event_id, " +
                "i.user_id, " +
                "i.friend_id, " +
                "i.created_time " +
                "FROM join_accept_notification i WHERE i.user_id = ? ";

        return loadEventNotifications( userId, lastTimestampUtc, conn, sqlSb, JoinAcceptNotification::new );
    }

    public List<EventStartNotification> loadEventStartNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn ) throws SQLException
    {
        List<EventStartNotification> notificationList = new ArrayList<>();

        Map<Long, List<NotificationModel>> notificationModelMap = new HashMap<>();

        // Get new notifications
        String sqlSb = "SELECT " +
                "i.event_id, " +
                "i.user_id, " +
                "i.created_time " +
                "FROM event_start_notification i WHERE i.user_id = ? ";

        if( lastTimestampUtc != null )
        {
            sqlSb += "AND i.created_time > ?";
        }

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            if( lastTimestampUtc != null )
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
                    OffsetDateTime createdUtc = rs.getObject( col++, OffsetDateTime.class );

                    NotificationModel notificationModel = new NotificationModel( eventId, -1L, createdUtc );

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

            List<OffsetDateTime> createdTimeUtcList = eventInterestList.stream().map( NotificationModel::getCreatedTimeUtc ).sorted( Comparator.reverseOrder() ).collect( Collectors.toList() );

            EventStartNotification eventNotification = new EventStartNotification();
            eventNotification.setEventId( eventId );
            eventNotification.setTimestampUtc( createdTimeUtcList.get( 0 ) );
            eventNotification.setData( new ArrayList<>() );

            notificationList.add( eventNotification );

        }

        return notificationList;
    }

    private List<EventNotification> loadEventNotifications( Long userId, OffsetDateTime lastTimestampUtc, Connection conn, String sqlSb, EventNotificationFactory factory ) throws SQLException
    {
        List<EventNotification> notificationList = new ArrayList<>();

        if( lastTimestampUtc != null )
        {
            sqlSb += "AND i.created_time > ?";
        }

        Map<Long, List<NotificationModel>> notificationModelMap = new HashMap<>();

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
        {
            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            if( lastTimestampUtc != null )
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

            List<Long> interestedFriendIdList = eventInterestList.stream().map( NotificationModel::getFriendId ).collect( Collectors.toList());
            List<OffsetDateTime> createdTimeUtcList = eventInterestList.stream().map( NotificationModel::getCreatedTimeUtc ).sorted( Comparator.reverseOrder() ).collect( Collectors.toList() );

            EventNotification interestNotification = factory.construct();
            interestNotification.setEventId( eventId );
            interestNotification.setTimestampUtc( createdTimeUtcList.get( 0 ) );
            interestNotification.setData( interestedFriendIdList );

            notificationList.add( interestNotification );
        }
        return notificationList;
    }

    public void saveAppNotifications( long userId, List<AppNotification> notificationList, Connection conn ) throws SQLException
    {
        if( notificationList.isEmpty() )
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
        EventResponse eventResponse = null;
        Set<Long> friendIdList = getFriendIdList( userId, conn );

        String sqlSb = "SELECT " + EVENT_SELECT +
                "WHERE e.id = ?";

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
                    eventResponse = new EventResponse();

                    Map<Long, EventInterest> eventInterestMap = loadEventInterested( eventId, conn );
                    List<String> tagList = getTagListByEventId( eventId, conn );

                    int interestedFriendCount = 0;

                    for ( Long interestedUserId : eventInterestMap.keySet() )
                    {
                        if( friendIdList.contains( interestedUserId ) )
                        {
                            interestedFriendCount++;
                        }
                    }

                    boolean isInterested = eventInterestMap.containsKey( userId );

                    Event event = new Event();
                    event.load( rs );
                    event.setInterestedCount( eventInterestMap.size() );
                    event.setTagList( tagList );

                    eventResponse.setEvent( event );
                    eventResponse.setInterestedFriendCount( interestedFriendCount );
                    eventResponse.setCreatorFriend( friendIdList.contains( event.getCreatorId() ) );
                    eventResponse.setInterested( isInterested );
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


        return eventResponse;
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

    public Map<Long, Event> loadEventByIdList( Set<Long> eventIdList, Connection conn ) throws SQLException
    {
        Map<Long, Event> eventMap = new HashMap<>();

        if( eventIdList.isEmpty() )
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

        if( eventIdList.isEmpty() )
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

    public HttpEntity<BasicResponse> getEventsForUser( Long userId, String pageKey )
    {
        DataListPage<EventFeedItem> dataListPage = new DataListPage<>();
        
        try ( Connection conn = getConnection() )
        {
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            List<EventFeedItem> feedItemList = loadEventsForUser( friendIdList, userId, pageId, pageTimestampUtc, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                EventFeedItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getEvent().getId() + "/" + lastItem.getInterestedTime();
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

    public HttpEntity<BasicResponse> getConfirmedEventsForUser( Long userId, String pageKey )
    {
        DataListPage<ConfirmedEventFeedItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            List<ConfirmedEventFeedItem> feedItemList = loadConfirmedEventsForUser( friendIdList, userId, pageId, pageTimestampUtc, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                ConfirmedEventFeedItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getEvent().getId() + "/" + lastItem.getCreatedTime();
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

    public List<EventFeedItem> loadEventsForUser( Set<Long> friendIdList, Long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<EventFeedItem> feedItems = new ArrayList<>();

        final String nonAggregateColumns = "e.id, e.description ";

        StringBuilder sqlSb = new StringBuilder( "SELECT " );
        sqlSb.append( nonAggregateColumns + ", ei.created_time " );
        sqlSb.append( "FROM event_interested ei, event e  " );
        sqlSb.append( "WHERE ei.user_id = ? " );
        if( pageId != null && pageLastTimestampUtc != null )
        {
            sqlSb.append( "AND ( ei.created_time, ei.event_id ) < ( ?, ? ) " );
        }
        sqlSb.append( "AND ei.event_id = e.id " );
        sqlSb.append( "ORDER BY ei.created_time DESC, ei.event_id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int count = 1;

            ps.setLong( count++, userId );

            if( pageId != null && pageLastTimestampUtc != null )
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
                    OffsetDateTime interestedTime = rs.getObject( col++, OffsetDateTime.class );

                    Map<Long, EventInterest> eventInterestMap = loadEventInterested( eventId, conn );

                    Event event = new Event();
                    event.setId( eventId );
                    event.setDescription( description );
                    event.setInterestedCount( eventInterestMap.size() );

                    int interestedFriendCount = ( int ) eventInterestMap.keySet().stream().filter( friendIdList::contains ).count();

                    EventFeedItem feedItem = new EventFeedItem();
                    feedItem.setEvent( event );
                    feedItem.setInterestedFriendCount( interestedFriendCount );
                    feedItem.setInterestedTime( interestedTime );

                    feedItems.add( feedItem );
                }

            }
        }

        return feedItems;
    }

    public List<ConfirmedEventFeedItem> loadConfirmedEventsForUser( Set<Long> friendIdList, Long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<ConfirmedEventFeedItem> happeningFeedItemList = new ArrayList<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " );
        sqlSb.append( CONFIRMED_EVENT_SELECT );
        sqlSb.append( "WHERE ce.creator_id = ? " );
        if( pageId != null && pageLastTimestampUtc != null )
        {
            sqlSb.append( "AND ( ce.updated_time, ce.id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY ce.updated_time DESC, ce.id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, userId );

            if( pageId != null && pageLastTimestampUtc != null )
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

                    Map<Long, EventInvited> invitedMap = getEventInvited( eventId, conn );
                    Map<Long, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( eventId, conn );
                    int activeCount = getEventActiveCount( eventId, conn );

                    boolean isInvited = invitedMap.containsKey( userId );
                    boolean isJoinRequested = eventJoinRequestMap.containsKey( userId );
                    boolean isParticipating = false;
                    Set<Long> confirmedParticipantIdList = invitedMap.values().stream().filter( EventInvited::isConfirmed ).map( EventInvited::getUserId ).collect( Collectors.toSet());
                    Map<Long, BasicProfile> confirmedParticipantMap = getUserProfileByList( confirmedParticipantIdList, conn );

                    if ( isInvited )
                    {
                        isParticipating = invitedMap.get( userId ).isConfirmed();
                    }

                    ConfirmedEvent event = new ConfirmedEvent();
                    event.load( rs );
                    event.setParticipantCount( confirmedParticipantIdList.size() );

                    ConfirmedEventFeedItem happeningFeedItem = new ConfirmedEventFeedItem();
                    happeningFeedItem.setEvent( event );
                    happeningFeedItem.setActiveCount( activeCount );
                    happeningFeedItem.setInvited( isInvited );
                    happeningFeedItem.setParticipant( isParticipating );
                    happeningFeedItem.setCreatorFriend( friendIdList.contains( event.getCreatorId() ) );
                    happeningFeedItem.setJoinRequested( isJoinRequested );
                    happeningFeedItem.setCreatedTime( event.getCreatedTime() );
                    happeningFeedItem.setConfirmedParticipantList( new ArrayList<>( confirmedParticipantMap.values() ) );

                    happeningFeedItemList.add( happeningFeedItem );
                }

            }
        }

        return happeningFeedItemList;
    }

    public HttpEntity<BasicResponse> getPopularEventsByTag( Long userId, String tag )
    {
        List<FeedItem> feedItems = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
            Set<Long> friendIdList = getFriendIdList( userId, conn );

            List<FeedItem> eventList = loadPopularEvents( userId, tag, conn, friendIdList );
            List<FeedItem> confirmedEventList = loadPopularConfirmedEvents( userId, tag, conn, friendIdList );

            feedItems.addAll( eventList );
            feedItems.addAll( confirmedEventList );

            // Sort by latest timestamp
//            feedItems.sort( ( o1, o2 ) -> {
//                Event firstEvent = o1.getEvent() != null ? o1.getEvent() : o1.getConfirmedEvent();
//                Event secondEvent = o2.getEvent() != null ? o2.getEvent() : o2.getConfirmedEvent();
//
//                return Long.compare( secondEvent.getCreatedTime(), firstEvent.getCreatedTime() );
//            } );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedItems ) );
    }

    private List<FeedItem> loadPopularEvents( Long userId, String tag, Connection conn, Set<Long> friendIdList ) throws SQLException
    {
        List<FeedItem> feedItems = new ArrayList<>();
        // Load events
        StringBuilder sqlSb = new StringBuilder( "SELECT " );
        sqlSb.append( EVENT_SELECT );
        sqlSb.append( "WHERE e.activity = ? " );
        sqlSb.append( "AND e.is_cancelled = FALSE " );
        sqlSb.append( "AND ( " );
        sqlSb.append(  "    ( e.visibility_preference = " + PrivacyPreference.VISIBILITY_PUBLIC + " ) " );
        if ( !friendIdList.isEmpty() )
        {
            sqlSb.append(  "        OR ( " );
            sqlSb.append(  "            e.visibility_preference = " + PrivacyPreference.VISIBILITY_FRIENDS_OF_FRIENDS + " AND e.id IN ( SELECT event_id FROM event_interested ei WHERE ei.user_id IN ( " );

            String delim = " ";
            for ( Long friendId : friendIdList )
            {
                sqlSb.append( delim );
                sqlSb.append( "?" );
                delim = ", ";
            }

            sqlSb.append( "         ) )" ); //Close IN and IN
            sqlSb.append( "     ) " ); //Close OR
            sqlSb.append( "     OR ( e.visibility_preference = " + PrivacyPreference.VISIBILITY_FRIENDS + " AND e.creator_id IN ( " );

            delim = " ";
            for ( Long friendId : friendIdList )
            {
                sqlSb.append( delim );
                sqlSb.append( "?" );
                delim = ", ";
            }

            sqlSb.append( "             ) " ); // Close IN
            sqlSb.append( "      ) " ); //Close OR
        }
        sqlSb.append( " )" ); //Close AND

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setString( count++, tag ); // for requests

            if ( !friendIdList.isEmpty() )
            {
                // For VISIBILITY_FRIENDS_OF_FRIENDS
                for ( Long friendId : friendIdList )
                {
                    ps.setLong( count++, friendId );
                }

                // For VISIBILITY_FRIENDS
                for ( Long friendId : friendIdList )
                {
                    ps.setLong( count++, friendId );
                }
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;

                    long eventId = rs.getLong( col++ );
//                        String enUserId = rs.getString( col++ );
//                        String need = rs.getString( col++ );
//                        Date startDate = rs.getDate( col++ );
//                        Date endDate = rs.getDate( col++ );
//                        String dateScope = rs.getString( col++ );
//                        boolean isConfirmed = rs.getBoolean( col++ );


                    Map<Long, EventInterest> eventInterestedMap = loadEventInterested( eventId, conn );

                    Set<Long> interestedUserIdList = eventInterestedMap.keySet();
                    FeedItem feedItem = new FeedItem();

                    Event event = new Event();
                    event.load( rs );
                    event.setInterestedCount( interestedUserIdList.size() );

                    feedItem.setEvent( event );

                    boolean isCreatorFriend = friendIdList.contains( event.getCreatorId() );
                    boolean isInterested = eventInterestedMap.containsKey( userId );
                    boolean isFriendInterested = false;

                    for ( Long friendId : friendIdList )
                    {
                        if ( eventInterestedMap.containsKey( friendId ) )
                        {
                            isFriendInterested = true;
                            break;
                        }
                    }

                    feedItem.setInterested( isInterested );
                    feedItem.setCreatorFriend( isCreatorFriend );
                    feedItem.setFriendInterested( isFriendInterested );
                    feedItem.setInterestedFriendCount( interestedUserIdList.size() );

                    feedItems.add( feedItem );
                }

            }
        }

        return feedItems;
    }

    private List<FeedItem> loadPopularConfirmedEvents( Long userId, String tag, Connection conn, Set<Long> friendIdList )
    {
        List<FeedItem> feedItems = new ArrayList<>();
        // Load confirmed events

        String confirmedSql = "SELECT " +
                CONFIRMED_EVENT_SELECT +
                "WHERE ce.activity = ? ";

        try ( PreparedStatement psc = conn.prepareStatement( confirmedSql ) )
        {

            psc.setFetchSize( 1000 );

            int param = 1;

            psc.setString( param++, tag );

            //execute query
            try ( ResultSet rs = psc.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    long eventId = rs.getLong( "id" );

                    FeedItem feedItem = new FeedItem();

                    ConfirmedEvent event = new ConfirmedEvent();

                    Map<Long, EventInvited> invitedMap = getEventInvited( eventId, conn );

                    boolean isInvited = invitedMap.containsKey( userId );
                    boolean isParticipant = false;
                    if ( isInvited )
                    {
                        isParticipant = invitedMap.get( userId ).isConfirmed();
                    }

                    event.load( rs );

                    feedItem.setConfirmedEvent( event );
                    feedItem.setParticipatingFriendCount( invitedMap.size() );
                    feedItem.setInvited( isInvited );
                    feedItem.setParticipant( isParticipant );

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

    public HttpEntity<BasicResponse> getPopularTags( long userTimestamp )
    {
        Map<String, PopularTag> popularTagMap = new HashMap<>();
        Map<String, Set<Long>> popularEventTagMap = new HashMap<>();
        Map<String, Set<Long>> popularConfirmedEventTagMap = new HashMap<>();
        Map<String, Set<Long>> popularActiveEventTagMap = new HashMap<>();

        try ( Connection conn = getConnection() )
        {
            // load popular events in the last hour
            String sqlSb = "SELECT " +
                    " pe.event_id," +
                    " e.activity " +
                    "FROM popular_event pe, event e " +
                    "WHERE e.id = pe.event_id " +
                    "AND e.is_confirmed = FALSE " +
                    "AND pe.updated_time >= DATE_SUB( ?, INTERVAL 1 HOUR) ";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;
                ps.setTimestamp( count++, new Timestamp( userTimestamp ) );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        long eventId = rs.getLong( col++ );
                        String tag = rs.getString( col++ );

                        Set<Long> eventIdList = new HashSet<>();
                        eventIdList.add( eventId );

                        popularEventTagMap.merge( tag, eventIdList, (old, curr) -> {
                            old.addAll( curr );
                            return old;
                        } );
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

            // load popular confirmed events in the last hour
            String confirmedEventSql = "SELECT " +
                    " pce.event_id," +
                    " ce.activity " +
                    "FROM popular_confirmed_event pce, confirmed_event ce " +
                    "WHERE ce.id = pce.event_id " +
                    "AND ce.is_happening = FALSE " +
                    "AND pce.updated_time >= DATE_SUB( ?, INTERVAL 1 HOUR) ";

            try ( PreparedStatement ps = conn.prepareStatement( confirmedEventSql ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;
                ps.setTimestamp( count++, new Timestamp( userTimestamp ) );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        long eventId = rs.getLong( col++ );
                        String tag = rs.getString( col++ );

                        Set<Long> eventIdList = new HashSet<>();
                        eventIdList.add( eventId );

                        popularConfirmedEventTagMap.merge( tag, eventIdList, (old, curr) -> {
                            old.addAll( curr );
                            return old;
                        } );
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

            // load popular active events in the last hour
            String activeEventSql = "SELECT " +
                    " pae.event_id," +
                    " ce.activity " +
                    "FROM popular_active_event pae, confirmed_event ce " +
                    "WHERE ce.id = pae.event_id " +
                    "AND pae.updated_time >= DATE_SUB( ?, INTERVAL 1 HOUR) ";

            try ( PreparedStatement ps = conn.prepareStatement( activeEventSql ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;
                ps.setTimestamp( count++, new Timestamp( userTimestamp ) );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        long eventId = rs.getLong( col++ );
                        String tag = rs.getString( col++ );

                        Set<Long> eventIdList = new HashSet<>();
                        eventIdList.add( eventId );

                        popularActiveEventTagMap.merge( tag, eventIdList, (old, curr) -> {
                            old.addAll( curr );
                            return old;
                        } );
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
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }
        for ( String tag : popularEventTagMap.keySet() )
        {
            int eventCount = popularEventTagMap.get( tag ).size();
            PopularTag popularTag = new PopularTag();
            popularTag.setTag( tag );
            popularTag.setInvolvedCount( eventCount );

            popularTagMap.merge( tag, popularTag, (old, curr) -> {
                old.setInvolvedCount( old.getInvolvedCount() + curr.getInvolvedCount() );
                return old;
            });
        }

        for ( String tag : popularConfirmedEventTagMap.keySet() )
        {
            int eventCount = popularConfirmedEventTagMap.get( tag ).size();
            PopularTag popularTag = new PopularTag();
            popularTag.setTag( tag );
            popularTag.setInvolvedCount( eventCount );

            popularTagMap.merge( tag, popularTag, (old, curr) -> {
                old.setInvolvedCount( old.getInvolvedCount() + curr.getInvolvedCount() );
                return old;
            });
        }

        for ( String tag : popularActiveEventTagMap.keySet() )
        {
            int eventCount = popularActiveEventTagMap.get( tag ).size();
            PopularTag popularTag = new PopularTag();
            popularTag.setTag( tag );
            popularTag.setInvolvedCount( eventCount );

            popularTagMap.merge( tag, popularTag, (old, curr) -> {
                old.setInvolvedCount( old.getInvolvedCount() + curr.getInvolvedCount() );
                return old;
            });
        }

        List<PopularTag> popularTagList = new ArrayList<>(popularTagMap.values());
        return new HttpEntity<>( new BasicResponse( popularTagList ) );
    }

    public Map<Long, List<CurrentActivity>> loadPopularActivityList( PopularFeedRequest popularFeedRequest, Connection conn )
    {
        Map<Long, List<CurrentActivity>> activeFriendMap = new HashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " +
                "ca.user_id, " +
                "ca.event_id, " +
                "ca.updated_time " +
                "FROM current_activity ca " +
                "WHERE " +
                "ca.event_id IS NOT NULL " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    CurrentActivity currentActivity = new CurrentActivity();
                    currentActivity.load( rs );

                    List<CurrentActivity> currentActivityList = new ArrayList<>();
                    currentActivityList.add( currentActivity );

                    activeFriendMap.merge( currentActivity.getEventId(), currentActivityList, ( oldList, newList ) -> {
                        oldList.addAll( newList );
                        return oldList;
                    } );

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

        return activeFriendMap;
    }

    public List<PopularNearbyFeedItem> loadPopularNearbyFeedItemList( List<Long> confirmedEventIdList, Set<Long> friendIdList, Map<Long, List<CurrentActivity>> activeEventMap, Long userId, Connection conn )
    {
        List<PopularNearbyFeedItem> popularNearbyFeedItems = new ArrayList<>();

        if( confirmedEventIdList.isEmpty() )
        {
            return popularNearbyFeedItems;
        }

        StringBuilder sqlSb = new StringBuilder("SELECT " + CONFIRMED_EVENT_SELECT +
                " WHERE ce.id IN ( ");

        String delim = "";

        for ( Long confirmedEventId : confirmedEventIdList )
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

            for ( Long confirmedEventId : confirmedEventIdList )
            {
                ps.setLong( count++, confirmedEventId );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first
                while ( rs.next() )
                {
                    ConfirmedEvent confirmedEvent = new ConfirmedEvent();
                    confirmedEvent.load( rs );

                    Map<Long, EventInvited> invitedMap = getEventInvited( confirmedEvent.getId(), conn );
                    Map<Long, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( confirmedEvent.getId(), conn );
                    Set<Long> confirmedIdSet = invitedMap.values().stream().filter( EventInvited::isConfirmed ).map( EventInvited::getUserId ).collect( Collectors.toSet() );

                    boolean isInvited = invitedMap.containsKey( userId );
                    boolean isParticipating = confirmedIdSet.contains( userId );
                    boolean isCreatorFriend = friendIdList.contains( confirmedEvent.getCreatorId() );
                    boolean isFriendConfirmed = Collections.disjoint( confirmedIdSet, friendIdList );
                    boolean isJoinRequested = eventJoinRequestMap.containsKey( userId );
                    int participantCount = confirmedIdSet.size();
                    boolean isHappeningFeedItem = isInvited || isFriendConfirmed;

                    if ( confirmedEvent.isPublic() || isInvited )
                    {
                        BasicProfile creatorUser = getUserProfileById( confirmedEvent.getCreatorId(), conn );
                        confirmedEvent.setCreatorDisplayName( creatorUser.getDisplayName() );

                    }
                    confirmedEvent.setParticipantCount( participantCount );


                    List<CurrentActivity> activeUserList = activeEventMap.get( confirmedEvent.getId() );
                    List<CurrentActivity> friendActivityList = activeUserList.stream().filter( e -> friendIdList.contains( e.getUserId() ) ).collect( Collectors.toList());

                    PopularNearbyFeedItem nearbyFeedItem = new PopularNearbyFeedItem();
                    nearbyFeedItem.setRelevantEvent( isHappeningFeedItem );
                    nearbyFeedItem.setConfirmedEvent( confirmedEvent );
                    nearbyFeedItem.setActiveCount( activeUserList.size() );
                    nearbyFeedItem.setActiveFriendCount( friendActivityList.size() );
                    nearbyFeedItem.setInvited( isInvited );
                    nearbyFeedItem.setParticipant( isParticipating );
                    nearbyFeedItem.setCreatorFriend( isCreatorFriend );
                    nearbyFeedItem.setJoinRequested( isJoinRequested );

                    popularNearbyFeedItems.add( nearbyFeedItem );
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

        return popularNearbyFeedItems;
    }

    public List<HappeningFeedItem> loadHappeningFeedItemList( Set<Long> friendIdList, Long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<HappeningFeedItem> happeningFeedItemList = new ArrayList<>();

        final String nonAggregateColumns = "e.id, e.description, e.creator_id, e.is_public";

        StringBuilder sqlSb = new StringBuilder( "SELECT " );
        sqlSb.append( nonAggregateColumns + ", MIN( ca.updated_time ) start_time  " );
        sqlSb.append( "FROM current_activity ca, confirmed_event e  " );
        sqlSb.append( "WHERE ca.event_id IS NOT NULL " );
        sqlSb.append( "AND ca.user_id IN ( " );

        String delim = " ";
        for ( Long friendId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " ) " ); //Close IN
        sqlSb.append( "AND e.id = ca.event_id  " );
        sqlSb.append( "GROUP BY " + nonAggregateColumns + " " );
        if( pageId != null && pageLastTimestampUtc != null )
        {
            sqlSb.append( "HAVING ( MIN( ca.created_time ), e.id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY start_time DESC, e.id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            for ( Long friendId : friendIdList )
            {
                ps.setLong( count++, friendId );
            }

            if( pageId != null && pageLastTimestampUtc != null )
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
                    Long creatorId = rs.getLong( col++ );
                    boolean isPublic = rs.getBoolean( col++ );
                    OffsetDateTime startTime = rs.getObject( col++, OffsetDateTime.class );

                    Map<Long, EventInvited> invitedMap = getEventInvited( eventId, conn );
                    Map<Long, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( eventId, conn );
                    int activeCount = getEventActiveCount( eventId, conn );

                    boolean isInvited = invitedMap.containsKey( userId );
                    boolean isJoinRequested = eventJoinRequestMap.containsKey( userId );
                    boolean isParticipating = false;
                    Set<Long> confirmedParticipantIdList = invitedMap.values().stream().filter( EventInvited::isConfirmed ).map( EventInvited::getUserId ).collect( Collectors.toSet());
                    Map<Long, BasicProfile> confirmedParticipantMap = getUserProfileByList( confirmedParticipantIdList, conn );

                    if ( isInvited )
                    {
                        isParticipating = invitedMap.get( userId ).isConfirmed();
                    }

                    ConfirmedEvent event = new ConfirmedEvent();
                    event.setId( eventId );
                    event.setCreatorId( creatorId );
                    event.setDescription( description );
                    event.setPublic( isPublic );
                    event.setParticipantCount( confirmedParticipantIdList.size() );

                    HappeningFeedItem happeningFeedItem = new HappeningFeedItem();
                    happeningFeedItem.setEvent( event );
                    happeningFeedItem.setActiveCount(activeCount );
                    happeningFeedItem.setInvited( isInvited );
                    happeningFeedItem.setParticipant( isParticipating );
                    happeningFeedItem.setCreatorFriend( friendIdList.contains( creatorId ) );
                    happeningFeedItem.setJoinRequested( isJoinRequested );
                    happeningFeedItem.setStartTime( startTime );
                    happeningFeedItem.setConfirmedParticipantList( new ArrayList<>( confirmedParticipantMap.values() ) );

                    happeningFeedItemList.add( happeningFeedItem );
                }

            }
        }

        return happeningFeedItemList;
    }

    public List<UpcomingFeedItem> loadUpcomingEvents( Set<Long> friendIdList, Long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn )
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
        if( pageId != null && pageLastTimestampUtc != null )
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

            if( pageId != null && pageLastTimestampUtc != null )
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

                    UpcomingFeedItem feedItem = new UpcomingFeedItem();

                    Map<Long, EventInterest> eventInterestMap = loadEventInterested( eventId, conn );

                    Event event = new Event();
                    event.setId( eventId );
                    event.setDescription( description );
                    event.setInterestedCount( eventInterestMap.size() );

                    boolean isInterested = eventInterestMap.containsKey( userId );
//                    boolean isCreatorFriend = friendIdList.stream().anyMatch( e -> e == event.getCreatorId() );
//                    boolean isFriendInterested = friendIdList.stream().anyMatch( eventInterestMap::containsKey );
                    int interestedFriendCount = ( int ) eventInterestMap.keySet().stream().filter( friendIdList::contains ).count();

                    feedItem.setEvent( event );
                    feedItem.setInterestedFriendCount( interestedFriendCount );
                    feedItem.setInterested( isInterested );
//                    feedItem.setCreatorFriend( isCreatorFriend );
//                    feedItem.setFriendInterested( isFriendInterested );
                    feedItem.setFirstInterestedTime( firstInterestedTime );

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

    public List<UpcomingFeedItem> loadUpcomingConfirmedEvents( List<Integer> friendIdList, long userId, Connection conn )
    {
        List<UpcomingFeedItem> feedItemList = new ArrayList<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " +
               CONFIRMED_EVENT_SELECT +
                "WHERE " +
                "ce.is_cancelled = FALSE " +
                "AND ce.is_happening = FALSE " +
                "AND ( ce.creator_id IN ( " );

        String delim = " ";

        for ( int eventUserId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " )" );
        sqlSb.append( " OR ce.id IN ( SELECT event_id FROM event_participant ep WHERE ep.is_confirmed = TRUE AND ep.user_id IN ( " );

        delim = " ";

        for ( int eventUserId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " ) ) )" );
        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            for ( int eventUserId : friendIdList )
            {
                ps.setInt( count++, eventUserId );
            }

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

                    long eventId = rs.getLong( col++ );
//                        String enUserId = rs.getString( col++ );
//                        String need = rs.getString( col++ );
//                        Date startDate = rs.getDate( col++ );
//                        Date endDate = rs.getDate( col++ );
//                        String dateScope = rs.getString( col++ );
                    boolean isPublic = rs.getBoolean( "is_public" );

                    Map<Long, EventInvited> invitedMap = getEventInvited( eventId, conn );
                    Map<Long, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( eventId, conn );

                    boolean isInvited = invitedMap.containsKey( userId );
                    boolean isParticipating = false;

                    if ( isInvited )
                    {
                        isParticipating = invitedMap.get( userId ).isConfirmed();
                    }
                    boolean isJoinRequested = eventJoinRequestMap.containsKey( userId );

                    int participantCount = ( int ) invitedMap.values().stream().filter( EventInvited::isConfirmed ).count();

                    ConfirmedEvent event = new ConfirmedEvent();

                    if ( isPublic || isInvited )
                    {
                        event.load( rs );
                        UserProfile creatorUser = getCompleteUserProfileById( event.getCreatorId(), conn );
                        event.setCreatorDisplayName( creatorUser.getDisplayName() );
                        event.setParticipantCount( participantCount );
                    }
                    else
                    {
                        event.loadPrivateEvent( rs );
                    }

                    UpcomingFeedItem feedItem = new UpcomingFeedItem();
                    feedItem.setConfirmedEvent( event );
                    feedItem.setInvited( isInvited );
                    feedItem.setParticipant( isParticipating );
                    feedItem.setJoinRequested( isJoinRequested );

                    feedItemList.add( feedItem );
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

        return feedItemList;
    }

    public void markConfirmedEventPublic( long eventId, Connection conn )
    {
        // Add new interested user_profile to the event

        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event SET is_public = TRUE WHERE id = ?" ) )
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
        }
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

        try ( PreparedStatement ps = conn.prepareStatement( sql  ) )
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

    public HttpEntity<BasicResponse> getInterestedFriendList( long eventId, Long userId, String pageKey )
    {
        DataListPage<InterestedFriendFeedItem> dataListPage = new DataListPage<>();
        
        try ( Connection conn = getConnection() )
        {
            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            List<InterestedFriendFeedItem> feedItemList = loadInterestedFriendPage( eventId, userId, pageId, pageTimestampUtc, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                InterestedFriendFeedItem lastItem = feedItemList.get( feedItemList.size() - 2 );
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

    public HttpEntity<BasicResponse> getInviteFriendPage( Long confirmedEventId, long eventId, Long userId, String pageKey )
    {
        DataListPage<InviteFriendPageItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            List<InviteFriendPageItem> feedItemList = loadInviteFriendPage( confirmedEventId, eventId, userId, pageId, pageTimestampUtc, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                InviteFriendPageItem lastItem = feedItemList.get( feedItemList.size() - 2 );
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

    public HttpEntity<BasicResponse> getInvitedList( long eventId, Long userId, String pageKey )
    {
        DataListPage<InvitedUser> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Boolean pageId = null;
            Long pageUserId = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Boolean.parseBoolean( list[0] );
                pageUserId = Long.parseLong( list[1] );
            }

            List<InvitedUser> feedItemList = loadInvitedPage( eventId, userId, pageId, pageUserId, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                InvitedUser lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.isConfirmed() + "/" + lastItem.getUser().getUserId();
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

    public HttpEntity<BasicResponse> getFriendActivityList( Long userId, String pageKey )
    {
        DataListPage<ActivityHistoryFeedItem> dataListPage = new DataListPage<>();

        try ( Connection conn = getConnection() )
        {
            Long pageId = null;
            OffsetDateTime pageTimestampUtc = null;

            if( pageKey != null && !pageKey.isEmpty() )
            {
                String[] list = pageKey.split( "/" );
                pageId = Long.parseLong( list[0] );
                pageTimestampUtc = OffsetDateTime.parse( list[1] );
            }

            List<ActivityHistoryFeedItem> feedItemList = loadFriendActivityPage( userId, pageId, pageTimestampUtc, 21, conn );

            if( feedItemList.size() <= 20 )
            {
                dataListPage.setItemList( feedItemList );
            }
            else
            {
                ActivityHistoryFeedItem lastItem = feedItemList.get( feedItemList.size() - 2 );
                String nextPageKey = lastItem.getEvent().getId() + "/" + lastItem.getActiveTime();
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

    public BasicProfile getUserProfileById( Long userId, Connection conn )
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
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
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

    public Map<Long, UserProfile> getUserProfilesWithDetails( Set<Long> userIdList, Connection conn ) throws SQLException
    {
        Map<Long, UserProfile> userProfileList = new HashMap<>();

        if ( userIdList.isEmpty() )
        {
            return userProfileList;
        }

        StringBuilder friendSql = new StringBuilder(  COMPLETE_USER_PROFILE_SELECT +
                " WHERE u.id IN ( " );

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

            for ( Long participantUserId : userIdList )
            {
                ps.setLong( pCount++, participantUserId );
            }

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    UserProfile profile = new UserProfile();
                    profile.loadCompleteProfileFromResultSet( rs );

                    userProfileList.put( profile.getUserId(), profile );
                }

            }
        }

        return userProfileList;
    }

    public Map<Integer, UserLocation> getUserLocation( List<Integer> userIdList, Connection conn )
    {
        Map<Integer, UserLocation> userLocationMap = new HashMap<>();

        if ( userIdList.isEmpty() )
        {
            return userLocationMap;
        }

        StringBuilder friendSql = new StringBuilder( "SELECT u.id, u.latitude, u.longitude FROM user_profile u WHERE u.id IN ( " );

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
                    int col = 1;
                    int userId = rs.getInt( col++ );
                    double latitude = rs.getDouble( col++ );
                    double longitude = rs.getDouble( col++ );

                    UserLocation userLocation = new UserLocation( latitude, longitude);

                    userLocationMap.put( userId, userLocation );
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


        return userLocationMap;
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

//        Map<Long, CurrentActivity> currentActivityMap = getCurrentActivityByUserIdList( friendIdList, conn );
//        Map<Long, List<CurrentActivity>> userActivityHistoryMap = getRecentUserActivityHistory( friendIdList, conn );
//
//        Map<Long, CurrentActivity> activityMap = new HashMap<>();
//        for ( Long friendId : userActivityHistoryMap.keySet() )
//        {
//            List<CurrentActivity> activityList = userActivityHistoryMap.get( friendId );
//
//            if( activityList != null && !activityList.isEmpty() )
//            {
//                activityMap.put( friendId, activityList.get( 0 ) );
//            }
//        }
//
//        Set<Long> eventIdList = new HashSet<>();
//        eventIdList.addAll( currentActivityMap.values().stream().map( CurrentActivity::getEventId ).filter( Objects::nonNull ).collect( Collectors.toSet()) );
//        eventIdList.addAll( activityMap.values().stream().map( CurrentActivity::getEventId ).filter( Objects::nonNull ).collect( Collectors.toSet()) );
//
//        Map<Long, Event> eventMap = loadEventByIdList( eventIdList, conn );

        for ( Long friendId : friendIdList )
        {
            BasicProfile basicProfile = basicProfileMap.get( friendId );

//            CurrentActivity currentActivity = currentActivityMap.get( friendId );
//            List<CurrentActivity> activityHistory = userActivityHistoryMap.get( friendId );
//
//            String activeEvent = null;
//            String lastActiveEvent = null;
//
//            if( currentActivity != null && currentActivity.getEventId() != null )
//            {
//                activeEvent = eventMap.get( currentActivity.getEventId() ).getDescription();
//            }
//            if( activeEvent == null )
//            {
//                if( activityHistory != null && !activityHistory.isEmpty() && activityHistory.get( 0 ).getEventId() != null )
//                {
//                    lastActiveEvent = eventMap.get( activityHistory.get( 0 ).getEventId() ).getDescription();
//                }
//            }

            FriendFeedItem friendFeedItem = new FriendFeedItem();
            friendFeedItem.setUser( basicProfile );
//            friendFeedItem.setActiveEvent( activeEvent );
//            friendFeedItem.setLastActiveEvent( lastActiveEvent );

            feedItemList.add( friendFeedItem );
        }

        return feedItemList;
    }

    public List<InterestedFriendFeedItem> loadInterestedFriendPage( long eventId, long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<InterestedFriendFeedItem> feedItemList = new ArrayList<>();
        Map<Long, EventInterest> interestMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT ei.user_id, ei.description, ei.created_time " );
        sqlSb.append( "FROM event_interested ei " );
        sqlSb.append( "WHERE ei.event_id = ? " );
        sqlSb.append( "AND ei.user_id IN ( SELECT friend_id FROM friend WHERE user_id = ? ) " );
        sqlSb.append( "OR ei.user_id  = ? " );
        if( pageId != null && pageLastTimestampUtc != null )
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

            if( pageId != null && pageLastTimestampUtc != null )
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
                    String description = rs.getString( col++ );
                    OffsetDateTime createdDt = rs.getObject( col++, OffsetDateTime.class );

                    EventInterest eventInterest = new EventInterest( eventId, interestedUserId, description );
                    eventInterest.setCreatedTime( createdDt );

                    interestMap.put( interestedUserId, eventInterest );
                }

            }
        }

        // Get interested friend ids
        Set<Long> interestedFriendIdList = interestMap.keySet();

        // Get interested friend details
        Map<Long, BasicProfile> interestedUserProfileMap = getUserProfileByList( interestedFriendIdList, conn );
        Map<Long, List<CurrentActivity>> userActivityHistoryMap = getRecentUserActivityHistory( interestedFriendIdList, conn );

        // Get friend visibility matrix for current user_profile
        Map<Long, List<Long>> visibilityMap = getInterestedVisibilityMatrix( eventId, conn );
        List<Integer> requestedFriendList = getVisibilityRequestedByUser( eventId, userId, conn );

        //Load current user_profile details
        UserProfile currUser = getCompleteUserProfileById( userId, conn );
        List<CurrentActivity> currUserRecentCurrentActivityHistory = userActivityHistoryMap.containsKey( userId ) ? userActivityHistoryMap.get(  userId ) : new ArrayList<>();

        for ( Long interestedFriendId : interestedUserProfileMap.keySet() )
        {
            BasicProfile friendProfile = interestedUserProfileMap.get( interestedFriendId );
            EventInterest eventInterest = interestMap.get( interestedFriendId );
            List<CurrentActivity> recentCurrentActivityList = userActivityHistoryMap.get( interestedFriendId );

            String relationship = null;
            boolean isPeekSent = false;
            boolean isPeekBack = false;

            if( interestedFriendId != userId )
            {
                // Remove identifying details from non-visible friends

                List<Long> visibleToFriendList = visibilityMap.containsKey( interestedFriendId ) ? visibilityMap.get( interestedFriendId ) : new ArrayList<>();
                List<Long> visibleToCurrUserList = visibilityMap.containsKey( userId ) ? visibilityMap.get( userId ) : new ArrayList<>();

                if ( !visibleToCurrUserList.contains( interestedFriendId )  )
                {
                    friendProfile.setDisplayName( null );
                }
                if ( !visibleToCurrUserList.contains( interestedFriendId ) && visibleToFriendList.contains( userId )  )
                {
                    isPeekSent = true;
                }
                if ( visibleToCurrUserList.contains( interestedFriendId ) && !visibleToFriendList.contains( userId )  )
                {
                    isPeekBack = true;
                }

                //Determine relationship
                boolean isRecentlyMet = false;
                for ( CurrentActivity currentActivity : recentCurrentActivityList )
                {
                    for ( CurrentActivity currUserCurrentActivity : currUserRecentCurrentActivityHistory )
                    {
                        if ( currentActivity.getEventId() == currUserCurrentActivity.getEventId() )
                        {
                            isRecentlyMet = true;
                            break;
                        }
                    }
                }

                if ( isRecentlyMet )
                {
                    relationship = "Interacted recently";
                }
            }

            InterestedFriendFeedItem interestedFriendFeedItem = new InterestedFriendFeedItem();
            interestedFriendFeedItem.setUser( friendProfile );
            interestedFriendFeedItem.setDescription( eventInterest.getDescription() );
            interestedFriendFeedItem.setDistance( "nearby" );
            interestedFriendFeedItem.setRelationship( relationship );
            interestedFriendFeedItem.setPeekSent( isPeekSent );
            interestedFriendFeedItem.setPeekBack( isPeekBack );
            interestedFriendFeedItem.setCreatedTime( eventInterest.getCreatedTime() );

            feedItemList.add( interestedFriendFeedItem );
        }

        return feedItemList;
    }

    public List<InviteFriendPageItem> loadInviteFriendPage( Long confirmedEventId, long eventId, long userId, Long pageId, OffsetDateTime pageLastTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<InviteFriendPageItem> feedItemList = new ArrayList<>();
        Map<Long, EventInterest> interestMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT ei.user_id, ei.description, ei.created_time " );
        sqlSb.append( "FROM event_interested ei " );
        sqlSb.append( "WHERE ei.event_id = ? " );
        sqlSb.append( "AND ei.user_id IN ( SELECT friend_id FROM friend WHERE user_id = ? ) " );
        sqlSb.append( "OR ei.user_id  = ? " );
        if( pageId != null && pageLastTimestampUtc != null )
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

            if( pageId != null && pageLastTimestampUtc != null )
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
                    String description = rs.getString( col++ );
                    OffsetDateTime createdDt = rs.getObject( col++, OffsetDateTime.class );

                    EventInterest eventInterest = new EventInterest( eventId, interestedUserId, description );
                    eventInterest.setCreatedTime( createdDt );

                    interestMap.put( interestedUserId, eventInterest );
                }

            }
        }

        // Get interested friend ids
        Set<Long> interestedFriendIdList = interestMap.keySet();

        // Get interested friend details
        Map<Long, BasicProfile> interestedUserProfileMap = getUserProfileByList( interestedFriendIdList, conn );
        Map<Long, List<CurrentActivity>> userActivityHistoryMap = getRecentUserActivityHistory( interestedFriendIdList, conn );

        // Get friend visibility matrix for current user_profile
        Map<Long, List<Long>> visibilityMap = getInterestedVisibilityMatrix( eventId, conn );
        List<Integer> requestedFriendList = getVisibilityRequestedByUser( eventId, userId, conn );

        //Load current user_profile details
        UserProfile currUser = getCompleteUserProfileById( userId, conn );
        List<CurrentActivity> currUserRecentCurrentActivityHistory = userActivityHistoryMap.containsKey( userId ) ? userActivityHistoryMap.get(  userId ) : new ArrayList<>();

        Map<Long, EventInvited> eventInvitedMap = getEventInvited( confirmedEventId, conn );

        for ( Long interestedFriendId : interestedUserProfileMap.keySet() )
        {
            BasicProfile friendProfile = interestedUserProfileMap.get( interestedFriendId );
            EventInterest eventInterest = interestMap.get( interestedFriendId );
            List<CurrentActivity> recentCurrentActivityList = userActivityHistoryMap.get( interestedFriendId );

            String relationship = null;
            boolean isPeekSent = false;
            boolean isPeekBack = false;
            boolean isInvited = true;

            if( interestedFriendId != userId )
            {
                // Remove identifying details from non-visible friends

                List<Long> visibleToFriendList = visibilityMap.containsKey( interestedFriendId ) ? visibilityMap.get( interestedFriendId ) : new ArrayList<>();
                List<Long> visibleToCurrUserList = visibilityMap.containsKey( userId ) ? visibilityMap.get( userId ) : new ArrayList<>();

                if ( !visibleToCurrUserList.contains( interestedFriendId )  )
                {
                    friendProfile.setDisplayName( null );
                }
                if ( !visibleToCurrUserList.contains( interestedFriendId ) && visibleToFriendList.contains( userId )  )
                {
                    isPeekSent = true;
                }
                if ( visibleToCurrUserList.contains( interestedFriendId ) && !visibleToFriendList.contains( userId )  )
                {
                    isPeekBack = true;
                }

                //Determine relationship
                boolean isRecentlyMet = false;
                for ( CurrentActivity currentActivity : recentCurrentActivityList )
                {
                    for ( CurrentActivity currUserCurrentActivity : currUserRecentCurrentActivityHistory )
                    {
                        if ( currentActivity.getEventId() == currUserCurrentActivity.getEventId() )
                        {
                            isRecentlyMet = true;
                            break;
                        }
                    }
                }

                if ( isRecentlyMet )
                {
                    relationship = "Interacted recently";
                }

                if( !eventInvitedMap.containsKey( interestedFriendId ) )
                {
                    isInvited = false;
                }
            }

            InviteFriendPageItem interestedFriendFeedItem = new InviteFriendPageItem();
            interestedFriendFeedItem.setUser( friendProfile );
            interestedFriendFeedItem.setDescription( eventInterest.getDescription() );
            interestedFriendFeedItem.setDistance( "nearby" );
            interestedFriendFeedItem.setRelationship( relationship );
            interestedFriendFeedItem.setPeekSent( isPeekSent );
            interestedFriendFeedItem.setPeekBack( isPeekBack );
            interestedFriendFeedItem.setInvited( isInvited );
            interestedFriendFeedItem.setCreatedTime( eventInterest.getCreatedTime() );

            feedItemList.add( interestedFriendFeedItem );
        }

        return feedItemList;
    }

    public List<InvitedUser> loadInvitedPage( long eventId, long userId, Boolean pageId, Long pageUserId, int limit, Connection conn ) throws SQLException
    {
        List<InvitedUser> feedItemList = new ArrayList<>();
        Map<Long, EventInvited> invitedMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT ei.user_id, ei.is_confirmed " );
        sqlSb.append( "FROM event_participant ei " );
        sqlSb.append( "WHERE ei.event_id = ? " );
        if( pageId != null && pageUserId != null )
        {
            sqlSb.append( "AND ( ei.is_confirmed, ei.user_id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY ei.is_confirmed DESC, ei.user_id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int pCount = 1;
            ps.setLong( pCount++, eventId );

            if( pageId != null && pageUserId != null )
            {
                ps.setBoolean( pCount++, pageId );
                ps.setLong( pCount++, pageUserId );
            }

            ps.setInt( pCount++, limit );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;
                    Long invitedUserId = rs.getLong( col++ );
                    boolean isConfirmed = rs.getBoolean( col++ );

                    EventInvited eventInvited = new EventInvited();
                    eventInvited.setEventId( eventId );
                    eventInvited.setUserId( invitedUserId );
                    eventInvited.setConfirmed( isConfirmed );

                    invitedMap.put( invitedUserId, eventInvited );
                }

            }
        }

//        Set<Long> friendIdList = getFriendIdList( userId, conn );
        Map<Long, BasicProfile> userProfileMap = getUserProfileByList( invitedMap.keySet(), conn );
        Map<Long, List<Long>> visibilityMap = getInterestedVisibilityMatrix( eventId, conn );
//        List<Integer> requestedFriendList = getVisibilityRequestedByUser( eventId, userId, conn );
//        Map<Long, List<CurrentActivity>> userActivityHistoryMap = getRecentUserActivityHistory( invitedMap.keySet(), conn );
//        UserProfile currUser = getCompleteUserProfileById( userId, conn );
//        List<CurrentActivity> currUserRecentCurrentActivityHistory = getRecentUserActivityHistoryById( userId, conn );

        for ( Long invitedId : invitedMap.keySet() )
        {
            EventInvited eventInvited = invitedMap.get( invitedId );
            BasicProfile userProfile = userProfileMap.get( invitedId );

            // Update invited user_profile visibility
            if ( !eventInvited.isConfirmed() )
            {
                List<Long> visibleToFriendList = visibilityMap.containsKey( invitedId ) ? visibilityMap.get( invitedId ) : new ArrayList<>();
                List<Long> visibleToCurrUserList = visibilityMap.containsKey( userId ) ? visibilityMap.get( userId ) : new ArrayList<>();

                if ( !visibleToCurrUserList.contains( invitedId )  )
                {
                    userProfile.setDisplayName( null );
                }
            }

            InvitedUser invitedUser = new InvitedUser();
            invitedUser.setUser( userProfile );
            invitedUser.setUserId( userProfile.getUserId() );
            invitedUser.setConfirmed( eventInvited.isConfirmed() );
//            invitedUser.setFriend( isFriend );
//            invitedUser.setRelationship( relationship );
//            invitedUser.setDistance( distance );
//            invitedUser.setVisibilityRequested( requestedFriendList.contains( invitedId ) );

            feedItemList.add( invitedUser );
        }

        return feedItemList;
    }

    public List<ActivityHistoryFeedItem> loadFriendActivityPage( long userId, Long pageId, OffsetDateTime pageTimestampUtc, int limit, Connection conn ) throws SQLException
    {
        List<ActivityHistoryFeedItem> feedItemList = new ArrayList<>();

        Map<Long, CurrentActivity> activityMap = new LinkedHashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " +
                "uah.user_id, " +
                "uah.event_id, " +
                "uah.updated_time " );
        sqlSb.append( "FROM user_activity_history uah " );
        sqlSb.append( "WHERE uah.user_id = ? " );
        if( pageId != null && pageTimestampUtc != null )
        {
            sqlSb.append( "AND ( uah.updated_time, uah.event_id ) < ( ?, ? ) " );
        }
        sqlSb.append( "ORDER BY uah.updated_time DESC, uah.event_id DESC " );
        sqlSb.append( "LIMIT ? " );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( limit );

            int pCount = 1;
            ps.setLong( pCount++, userId );

            if( pageId != null && pageTimestampUtc != null )
            {
                ps.setObject( pCount++, pageTimestampUtc );
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

                    activityMap.put( currentActivity.getEventId(), currentActivity );
                }

            }
        }

        Set<Long> eventIdList = activityMap.keySet();
        Map<Long, ConfirmedEvent> eventMap = loadConfirmedEventByIdList( eventIdList, conn );
        Set<Long> friendIdList = getFriendIdList( userId, conn );

        for ( Long eventId : activityMap.keySet() )
        {
            CurrentActivity activity = activityMap.get( eventId );
            ConfirmedEvent event = eventMap.get( eventId );

            Map<Long, EventInvited> invitedMap = getEventInvited( eventId, conn );

            Map<Long, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( eventId, conn );
            int activeCount = getEventActiveCount( eventId, conn );

            boolean isInvited = invitedMap.containsKey( userId );
            boolean isJoinRequested = eventJoinRequestMap.containsKey( userId );
            boolean isParticipating = false;
            Set<Long> confirmedParticipantIdList = invitedMap.values().stream().filter( EventInvited::isConfirmed ).map( EventInvited::getUserId ).collect( Collectors.toSet());
            Map<Long, BasicProfile> confirmedParticipantMap = getUserProfileByList( confirmedParticipantIdList, conn );

            if ( isInvited )
            {
                isParticipating = invitedMap.get( userId ).isConfirmed();
            }

            event.setParticipantCount( confirmedParticipantIdList.size() );

            ActivityHistoryFeedItem feedItem = new ActivityHistoryFeedItem();
            feedItem.setEvent( event );
            feedItem.setActiveCount( activeCount );
            feedItem.setInvited( isInvited );
            feedItem.setParticipant( isParticipating );
            feedItem.setCreatorFriend( friendIdList.contains( event.getCreatorId() ) );
            feedItem.setJoinRequested( isJoinRequested );
            feedItem.setActiveTime( activity.getUpdatedTime() );
            feedItem.setConfirmedParticipantList( new ArrayList<>( confirmedParticipantMap.values() ) );

            feedItemList.add( feedItem );
        }


        return feedItemList;
    }

    public Map<Long, EventInterest> loadEventInterested( long eventId, Connection conn )
    {
        Map<Long, EventInterest> interestMap = new HashMap<>();

        StringBuilder friendSql = new StringBuilder( "SELECT ei.user_id, ei.description FROM event_interested ei WHERE ei.event_id = ?" );

        try ( PreparedStatement ps = conn.prepareStatement( friendSql.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int pCount = 1;
            ps.setLong( pCount++, eventId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    int col = 1;
                    Long userId = rs.getLong( col++ );
                    String description = rs.getString( col++ );

                    EventInterest eventInterest = new EventInterest( eventId, userId, description );

                    interestMap.put( userId, eventInterest );
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


        return interestMap;
    }

    public HttpEntity<BasicResponse> updateUserLocation( UserLocation userLocation, Long userId )
    {
        UserProfile updatedUser;
        
        try ( Connection conn = getConnection() )
        {
            updatedUser = updateUserLocation( userLocation, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedUser ) );
    }

    public UserProfile updateUserLocation( UserLocation userLocation, Long userId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE user_profile SET " +
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

        return getCompleteUserProfileById( userId, conn );
    }

    public HttpEntity<BasicResponse> updateUserTimezone( String timezone, Long userId )
    {
        UserProfile updatedUser;

        try ( Connection conn = getConnection() )
        {
            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE user_profile SET " +
                    " time_zone = ? " +
                    " WHERE id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, timezone );
                ps.setLong( count++, userId );

                //execute query
                ps.executeUpdate();

            }

            updatedUser = getCompleteUserProfileById( userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedUser ) );
    }

}
