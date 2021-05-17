package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.constant.PrivacyPreference;
import com.dulithadabare.dosomething.model.*;
import com.dulithadabare.dosomething.util.LocationHelper;
import org.springframework.http.HttpEntity;

import java.net.URISyntaxException;
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

    class ActiveEvent
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

    public HttpEntity<BasicResponse> createAnonymousUser( UserProfile userProfile )
    {
        int newUserId = 0;
        UserProfile newUser;

        try ( Connection conn = getConnection() )
        {
            // Create User

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO user_profile ( firebase_uid ) VALUES ( ? ) RETURNING id" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, userProfile.getFirebaseUid() );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        int id = rs.getInt( col++ );

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
        UserProfile updatedProfile;

        try ( Connection conn = getConnection() )
        {
            // update user_profile details from facebook

//            PublicProfile publicProfile = facebookResource.getPublicProfile( userProfile.getFacebookId(), facebookUserToken );
//            PictureResponse pictureResponse = facebookResource.getProfilePicture( userProfile.getFacebookId(), facebookUserToken );

            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE user_profile SET name = ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, userProfile.getDisplayName() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Get User Friends from Facebook

            Map<String, String> facebookFriendList = facebookResource.getFacebookFriends( userProfile.getFacebookId(), facebookUserToken );

            // Get friend user_profile ids from DB

            Map<String, Integer> facebookIdUserIdMap = new HashMap<>();

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

            StringBuilder updateSqlSb = new StringBuilder( "INSERT INTO friend VALUES " );

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
                    ps.setInt( count++, userProfile.getUserId() );
                    ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                    ps.setInt( count++, facebookIdUserIdMap.get( facebookId ) );
                    ps.setInt( count++, userProfile.getUserId() );
                }

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            updatedProfile = getCompleteUserProfileById( userProfile.getUserId(), conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedProfile ) );
    }

    public HttpEntity<BasicResponse> getPopularNearbyFeed( PopularFeedRequest popularFeedRequest, int userId )
    {
        List<PopularNearbyFeedItem> popularNearbyFeedItemList;

        try ( Connection conn = getConnection() )
        {
            // Update friend list from facebook
            // updateFriendList();

            // Get Friend User Profiles from DB
            List<Integer> friendIdList = getFriendIdList( userId, conn );

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

    public HttpEntity<BasicResponse> getHappeningFeed( int userId )
    {
        List<ActiveFeedItem> activeFeedItemList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
//            updateFriendList();

            // Get Friend User Profiles from DB
            List<Integer> friendIdList = getFriendIdList( userId, conn );

            // None of the user_profile's facebook friends are using the DoSomething app
            if ( friendIdList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            // Get active  events by friends

            Map<Long, List<CurrentActivity>> activeFriendMap = loadCurrentActivityByFriendList( new ArrayList<>( friendIdList ), userId, conn );

            // Get list of confirmed events

            Set<Long> eventIdSet = activeFriendMap.keySet();

            //load event list

            List<EventResponse> eventResponseList = new ArrayList<>();

            //TODO PERFORMANCE load all events at once
            for ( Long eventId : eventIdSet )
            {
                eventResponseList.add( loadConfirmedEventById( eventId, userId, conn ) );
            }

            Map<String, List<HappeningFeedItem>> tagMap = new HashMap<>();

            for ( EventResponse eventResponse : eventResponseList )
            {
                ConfirmedEvent event = eventResponse.getConfirmedEvent();

                Map<Integer, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( event.getId(), conn );

                List<CurrentActivity> activeUserList = activeFriendMap.get( event.getId() );
                List<CurrentActivity> friendActivityList = activeUserList.stream().filter( e -> friendIdList.contains( e.getUserId() ) ).collect( Collectors.toList());
                // Sort by updated time ASC ( First updated time will be the first in the list)
                activeUserList.sort( Comparator.comparingLong( CurrentActivity::getUpdatedTime ) );
                long firstActiveTimestamp = activeUserList.get( 0 ).getUpdatedTime();

                HappeningFeedItem happeningFeedItem = new HappeningFeedItem();
                happeningFeedItem.setConfirmedEvent( event );
                happeningFeedItem.setActiveCount( friendActivityList.size() );
                happeningFeedItem.setInvited( eventResponse.isInvited() );
                happeningFeedItem.setParticipant( eventResponse.isParticipant() );
                happeningFeedItem.setCreatorFriend( friendIdList.contains( event.getCreatorId() ) );
                happeningFeedItem.setJoinRequested( eventJoinRequestMap.containsKey( userId ) );
                happeningFeedItem.setFirstActiveTimestamp( firstActiveTimestamp );

                activeFeedItemList.add( happeningFeedItem );
            }
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }
        // Sort active feed items by active friend count DESC.
        activeFeedItemList.sort( (o1, o2) -> Long.compare( o2.getActiveCount(), o1.getActiveCount() ) );

        return new HttpEntity<>( new BasicResponse( activeFeedItemList ) );
    }

    public HttpEntity<BasicResponse> getUpcomingFeed( int userId )
    {
        List<UpcomingFeedItem> feedItemList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
//            updateFriendList();

            // Get Friend User Profiles from DB
            List<Integer> friendIdList = getFriendIdList( userId, conn );

            // None of the user_profile's facebook friends are using the DoSomething app
            if ( friendIdList.isEmpty() )
            {
                return new HttpEntity<>( new BasicResponse( "No friends using DoSomething", BasicResponse.STATUS_ERROR ) );
            }

            // Get confirmed events by friends

            List<UpcomingFeedItem> eventList = loadUpcomingEvents( friendIdList, userId, conn );
            List<UpcomingFeedItem> confirmedEventList = loadUpcomingConfirmedEvents( friendIdList, userId, conn );

            // Get events by friends

            feedItemList.addAll( eventList );
            feedItemList.addAll( confirmedEventList );

            // Sort by latest timestamp
            feedItemList.sort( ( o1, o2 ) -> {
                Event firstEvent = o1.getEvent() != null ? o1.getEvent() : o1.getConfirmedEvent();
                Event secondEvent = o2.getEvent() != null ? o2.getEvent() : o2.getConfirmedEvent();

                return Long.compare( secondEvent.getCreatedTime(), firstEvent.getCreatedTime() );
            } );
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }
        return new HttpEntity<>( new BasicResponse( feedItemList ) );
    }

    public HttpEntity<BasicResponse> startCurrentActivity( CurrentActivity activity, int userId )
    {
        CurrentActivity currentActivity = null;

        try ( Connection conn = getConnection() )
        {
            // Start activity in the event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO current_activity (" +
                    " user_id," +
                    " event_id ," +
                    " updated_time" +
                    " ) VALUES ( ?, ?, ?) " +
                    "ON CONFLICT (user_id) DO UPDATE SET event_id = ?, updated_time = ?" ) )
            {
                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );
                ps.setLong( count++, activity.getEventId() );
                ps.setTimestamp( count++, new Timestamp( activity.getUpdatedTime() ) );

                //on duplicate key update params

                ps.setLong( count++, activity.getEventId() );
                ps.setTimestamp( count++, new Timestamp( activity.getUpdatedTime() ) );

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

            // Update confirmed event status
            updateConfirmedEventHappeningStatus(activity.getEventId(), conn);
        }
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( currentActivity ) );
    }

    public HttpEntity<BasicResponse> stopCurrentActivity( CurrentActivity currentActivity, int userId )
    {
        try ( Connection conn = getConnection() )
        {
            // Remove event interested user_profile

            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE current_activity SET event_id = NULL, updated_time = ? WHERE user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setTimestamp( count++, new Timestamp( currentActivity.getUpdatedTime() ) );
                ps.setInt( count++, userId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Update confirmed event status
            updateConfirmedEventHappeningStatus( currentActivity.getEventId(), conn);
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( userId ) );
    }

    // TODO Check if this Read and Update operation can cause errors such as incorrectly marking the event is not
    //  happening due to transaction isolation?
    public void updateConfirmedEventHappeningStatus( long confirmedEventId, Connection conn )
    {
        int activeCount = 0;

        // get current active count for event

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
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        boolean isHappening = false;

        if( activeCount > 0 )
        {
            isHappening = true;
        }

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

    public void updateUserActivityHistory( CurrentActivity activity, Connection conn )
    {
        CurrentActivity currentActivity;

        // Start event

        String sql = "INSERT INTO user_activity_history (" +
                " user_id," +
                " event_id ," +
                " updated_time" +
                " ) VALUES ( ?, ?, ?) " +
                "ON CONFLICT (user_id, event_id) DO UPDATE SET updated_time = ?";

        try ( PreparedStatement ps = conn.prepareStatement( sql ) )
        {
            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setInt( count++, activity.getUserId() );
            ps.setLong( count++, activity.getEventId() );
            ps.setTimestamp( count++, new Timestamp( activity.getUpdatedTime() ) );

            //on duplicate key update params

            ps.setTimestamp( count++, new Timestamp( activity.getUpdatedTime() ) );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

    }

    public HttpEntity<BasicResponse> getCurrentActivityByUserId( int userId )
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

    public CurrentActivity getCurrentActivityByUserId( int userId, Connection conn )
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

            ps.setInt( count++, userId );

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
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }


        return currentActivity;
    }

    public Map<Integer, List<CurrentActivity>> getRecentUserActivityHistory( List<Integer> userIdList, Connection conn ) throws SQLException
    {
        Map<Integer, List<CurrentActivity>> activityMap = new HashMap<>();

        // init map
        for ( Integer userId : userIdList )
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

        for ( Integer userId : userIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " ) " );
        sqlSb.append( "AND uah.updated_time >= ( NOW() - INTERVAL '7 DAY' )" );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            for ( Integer userId : userIdList )
            {
                ps.setInt( count++, userId );
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

    public List<CurrentActivity> getRecentUserActivityHistoryById( int userId, Connection conn ) throws SQLException
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

            ps.setInt( count++, userId );

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

    public HttpEntity<BasicResponse> createEvent( Event event, int userId )
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
                        " visibility_preference," +
                        " created_time" +
                        " ) VALUES ( ?, ?, ?, ? ) RETURNING id" ) )
                {
                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setInt( count++, userId );
                    ps.setString( count++, event.getDescription() );
                    ps.setInt( count++, event.getVisibilityPreference() );
                    ps.setTimestamp( count++, new Timestamp( event.getCreatedTime() ) );

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
                addEventInterest( eventId, userId, eventInterest, event.getCreatedTime(), conn );

                // Load updated event
                createdEvent = getEventById( eventId, userId, conn );
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

    public HttpEntity<BasicResponse> updateEvent( Event event, int userId )
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

            updatedEvent = getEventById( event.getId(), userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> createConfirmedEvent( ConfirmedEvent confirmedEvent, int userId )
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
                        " visibility_preference," +
                        " created_time" +
                        " ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id" ) )
                {
                    Date date = confirmedEvent.getDate() != null && !confirmedEvent.getDate().isEmpty() ? Date.valueOf( confirmedEvent.getDate() ) : null;

                    Time time = confirmedEvent.getTime() != null && !confirmedEvent.getTime().isEmpty() ? Time.valueOf( LocalTime.parse( confirmedEvent.getTime() ) ) : null;

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, confirmedEvent.getEventId() );
                    ps.setInt( count++, userId );
                    ps.setString( count++, confirmedEvent.getDescription() );
                    ps.setDate( count++, date );
                    ps.setTime( count++, time );
                    ps.setBoolean( count++, confirmedEvent.isPublic() );
                    ps.setInt( count++, confirmedEvent.getVisibilityPreference() );
                    ps.setTimestamp( count++, new Timestamp( confirmedEvent.getCreatedTime() ) );

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
                addEventInvitedUserByList( confirmedEventId, new ArrayList<>(confirmedEvent.getInvitedList()), conn );
                addEventInviteNotificationByList( confirmedEventId, new ArrayList<>(confirmedEvent.getInvitedList()), userId, conn );


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
                createdEvent = loadConfirmedEventById( confirmedEventId, userId, conn );
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

    public HttpEntity<BasicResponse> updateConfirmedEvent( ConfirmedEvent confirmedEvent, int userId )
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
                        " time = ?," +
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

                Map<Integer, EventInvited> oldEventInvitedMap = getEventInvited( confirmedEvent.getId(), conn );

                List<Integer> removedInvitedList = oldEventInvitedMap.keySet().stream().filter( e -> !confirmedEvent.getInvitedList().contains( e ) && e != userId ).collect( Collectors.toList());
                List<Integer> newInvitedList = confirmedEvent.getInvitedList().stream().filter( e -> !oldEventInvitedMap.containsKey( e ) ).collect( Collectors.toList());

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
            updatedEvent = loadConfirmedEventById( confirmedEvent.getId(), userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }
    public HttpEntity<BasicResponse> cancelConfirmedEvent( long confirmedEventId, int userId )
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
            updatedEvent = loadConfirmedEventById( confirmedEventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }


    private void addEventInvitedUser( long confirmedEventId, int invitedUserId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_participant ( event_id, user_id ) VALUES ( ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, confirmedEventId );
            ps.setInt( count++, invitedUserId );

            //execute query
            ps.executeUpdate();

        }
    }

    private void addEventInvitedUserByList( long confirmedEventId, List<Integer> invitedUserList, Connection conn ) throws SQLException
    {
        if ( !invitedUserList.isEmpty() )
        {
            StringBuilder insertSqlSb = new StringBuilder( "INSERT INTO event_participant ( event_id, user_id ) VALUES " );

            String delim = " ";

            for ( Integer invitedUser :invitedUserList )
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

                for ( Integer invitedUser : invitedUserList )
                {
                    ps.setLong( count++, confirmedEventId );
                    ps.setInt( count++, invitedUser );
                }

                //execute query
                ps.executeUpdate();

            }
        }
    }

    private void addEventInviteNotificationByList( long confirmedEventId, List<Integer> invitedUserList, int eventCreatorId, Connection conn ) throws SQLException
    {
        if ( !invitedUserList.isEmpty() )
        {
            StringBuilder updateSqlSb = new StringBuilder( "INSERT INTO event_invite ( event_id, sender_id, receiver_id ) VALUES " );

            String delimiter = " ";

            for ( Integer invitee : invitedUserList )
            {
                updateSqlSb.append( delimiter );
                updateSqlSb.append( "(?, ?, ?)" );
                delimiter = ", ";
            }

            updateSqlSb.append( " ON CONFLICT DO NOTHING" );

            try ( PreparedStatement ps = conn.prepareStatement( updateSqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                for ( Integer invitee : invitedUserList )
                {
                    ps.setLong( count++, confirmedEventId );
                    ps.setInt( count++, eventCreatorId );
                    ps.setInt( count++, invitee );
                }

                //execute query
                ps.executeUpdate();

            }
        }
    }

    public void removeEventInvitedUserByList( long eventId, List<Integer> removedInvitedUserList, Connection conn ) throws SQLException
    {
        if( !removedInvitedUserList.isEmpty() )
        {
            // Remove event invite
            StringBuilder inviteSqlSb = new StringBuilder("DELETE FROM event_invite WHERE event_id = ? AND receiver_id IN ( ");

            String delim = "";

            for ( Integer invitedUser : removedInvitedUserList )
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
                for ( Integer invitedUser : removedInvitedUserList )
                {
                    ps.setInt( count++, invitedUser );
                }

                //execute query
                ps.executeUpdate();

            }

            // Remove from event participant
            StringBuilder participantSqlSb = new StringBuilder("DELETE FROM event_participant WHERE event_id = ? AND user_id IN ( ");

            delim = "";

            for ( Integer invitedUser : removedInvitedUserList )
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

                for ( Integer invitedUser : removedInvitedUserList )
                {
                    ps.setInt( count++, invitedUser );
                }

                //execute query
                ps.executeUpdate();

            }
        }
    }

    public HttpEntity<BasicResponse> getConfirmedEventById( long eventId, int userId )
    {
        EventResponse confirmedEvent = null;

        try ( Connection conn = getConnection() )
        {
            confirmedEvent = loadConfirmedEventById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( confirmedEvent ) );
    }


    public EventResponse loadConfirmedEventById( long confirmedEventId, int userId, Connection conn )
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

                    Map<Integer, EventInvited> invitedMap = getEventInvited( confirmedEventId, conn );
                    Map<Integer, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( confirmedEventId, conn );

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
                    UserProfile participantUser = participant.getUser();
                    participantUser.setDisplayName( null );
                }

                if ( participantId == userId )
                {
                    UserProfile participantUser = participant.getUser();
                    participantUser.setDisplayName( "You" );
                }
            }
        }
    }

    public HttpEntity<BasicResponse> getInvitedUserList( long eventId, int userId )
    {
        List<InvitedUser> invitedUserList = new ArrayList<>();
        try ( Connection conn = getConnection() )
        {
            List<Integer> friendIdList = getFriendIdList( userId, conn );
            Map<Integer, EventInvited> invitedMap = getEventInvited( eventId, conn );
            Map<Integer, UserProfile> userProfileMap = getUserProfilesWithDetails( new ArrayList<>(invitedMap.keySet()), conn );
            Map<Integer, List<Integer>> visibilityMap = getInterestedVisibilityMatrix( eventId, conn );
            List<Integer> requestedFriendList = getVisibilityRequestedByUser( eventId, userId, conn );
            Map<Integer, List<CurrentActivity>> userActivityHistoryMap = getRecentUserActivityHistory( new ArrayList<>(invitedMap.keySet()), conn );
            UserProfile currUser = getCompleteUserProfileById( userId, conn );
            List<CurrentActivity> currUserRecentCurrentActivityHistory = getRecentUserActivityHistoryById( userId, conn );

            for ( Integer invitedId : invitedMap.keySet() )
            {
                EventInvited eventInvited = invitedMap.get( invitedId );
                UserProfile userProfile = userProfileMap.get( invitedId );

                boolean isFriend = true;
                String distance = null;
                String relationship = null;

                // Update invited user_profile visibility
                if ( !eventInvited.isConfirmed() )
                {
                    // Get the list of users the invited user_profile is visible to
                    List<Integer> visibleFriendList = visibilityMap.get( invitedId );

                    //if participant is not visible to current user_profile
                    if ( visibleFriendList == null || !visibleFriendList.contains( userId ) )
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
                invitedUser.setUser( userProfile );
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

    public HttpEntity<BasicResponse> getActiveFriendListByEventId( long eventId, int userId )
    {
        List<ActiveUser> activeUserList = new ArrayList<>();
        try ( Connection conn = getConnection() )
        {
            List<Integer> friendIdList = getFriendIdList( userId, conn );

            Map<Integer, CurrentActivity> currentActivityMap = new HashMap<>();

            StringBuilder sqlSb = new StringBuilder( "SELECT " +
                    "ca.user_id, " +
                    "ca.event_id, " +
                    "ca.updated_time " +
                    "FROM current_activity ca " +
                    "WHERE " +
                    "ca.event_id = ? " +
                    "AND ca.user_id IN ( ");

            String delim = " ";
            for ( int friendId : friendIdList )
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

                for ( int friendId : friendIdList )
                {
                    ps.setInt( count++, friendId );
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

            Map<Integer, UserProfile> userProfileMap = getUserProfilesWithDetails( new ArrayList<>(currentActivityMap.keySet()), conn );

            for ( CurrentActivity currentActivity : currentActivityMap.values() )
            {
                ActiveUser activeUser = new ActiveUser();
                activeUser.setUser( userProfileMap.get( currentActivity.getUserId() ) );
                activeUser.setCreatedTime( currentActivity.getUpdatedTime() );

                activeUserList.add( activeUser );
            }

        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( activeUserList ) );
    }

    public Map<Integer, EventInvited> getEventInvited( long eventId, Connection conn )
    {
        Map<Integer, EventInvited> invitedMap = new HashMap<>();

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
                    int invitedId = rs.getInt( col++ );
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

    public  List<Integer> getVisibilityRequestedByUser( long eventId, int userId, Connection conn )
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
            ps.setInt( count++, userId );

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

    public Map<Integer, List<Integer>> getInterestedVisibilityMatrix( long eventId, Connection conn )
    {
        Map<Integer, List<Integer>> visibilityMap = new HashMap<>();

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
                    int userId = rs.getInt( col++ );
                    int friendId = rs.getInt( col++ );

                    List<Integer> visibleToFriendList = new ArrayList<>();
                    visibleToFriendList.add( friendId );

                    visibilityMap.merge( userId, visibleToFriendList, ( currList, newList ) -> {
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

    public HttpEntity<BasicResponse> acceptEventInvite( long confirmedEventId, int userId )
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
                try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO accept_notification ( event_id, user_id ) VALUES ( ?, ? ) ON CONFLICT DO NOTHING" ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    ps.setLong( count++, confirmedEventId );
                    ps.setInt( count++, userId );

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

            // Load updated event
            updatedEvent = loadConfirmedEventById( confirmedEventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> declineEventInvite( long eventId, int userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            // Remove event invite
            removeEventInvite( eventId, userId, conn );

            // Load updated event
            updatedEvent = loadConfirmedEventById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    private void removeEventInvite( long eventId, int userId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_invite WHERE event_id = ? AND receiver_id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setInt( count++, userId );

            //execute query
            ps.executeUpdate();

        }
    }

    public void confirmEventParticipation( long eventId, int participantId, Connection conn ) throws SQLException
    {
        // Add new interested user_profile to the event
        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event_participant SET is_confirmed = TRUE WHERE event_id = ? AND user_id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setInt( count++, participantId );

            //execute query
            ps.executeUpdate();
        }
    }

    public HttpEntity<BasicResponse> cancelEventParticipation( long eventId, int userId )
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
                ps.setInt( count++, userId );

                //execute query
                ps.executeUpdate();

            }

            // Load updated event
            updatedEvent = loadConfirmedEventById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> addJoinRequest( long eventId, int userId, long updatedTime )
    {
        EventResponse updatedEvent;

        try ( Connection conn = getConnection() )
        {
            // Add new join request
            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_join_request ( event_id, requester_id, created_time ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, userId );
                ps.setTimestamp( count++, new Timestamp( updatedTime ) );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            //TODO NOTIFICATIONS

            // Load updated event
            updatedEvent = loadConfirmedEventById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> removeJoinRequest( long confirmedEventId, int userId )
    {
        EventResponse updatedEvent;

        try ( Connection conn = getConnection() )
        {
            // Add new join request
            removeJoinRequest( confirmedEventId, userId, conn );

            //Get updated event
            updatedEvent = loadConfirmedEventById( confirmedEventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public void removeJoinRequest( long eventId, int requesterId, Connection conn ) throws SQLException
    {
        // Delete join request
        try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_join_request WHERE event_id = ? AND requester_id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setInt( count++, requesterId );

            //execute query
            ps.executeUpdate();

        }
    }

    public HttpEntity<BasicResponse> getJoinRequests( long confirmedEventId )
    {
        List<JoinRequest> joinRequestList = new ArrayList<>();

        Map<Integer, EventJoinRequest> eventJoinRequestMap;
        try ( Connection conn = getConnection() )
        {
            // Get Join Requests
            eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( confirmedEventId, conn );

            Map<Integer, UserProfile> joinRequestedUserMap = getUserProfileByList( new ArrayList<>( eventJoinRequestMap.keySet() ), conn );

            for ( EventJoinRequest eventJoinRequest : eventJoinRequestMap.values() )
            {
                UserProfile user_profile = joinRequestedUserMap.get( eventJoinRequest.getUserId() );
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

    public Map<Integer, EventJoinRequest> getEventJoinRequestsByConfirmedEventId( long eventId, Connection conn )
    {
        Map<Integer, EventJoinRequest> eventJoinRequestMap = new HashMap<>();

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
                    int requesterId = rs.getInt( col++ );
                    long createdTime = rs.getTimestamp( col++ ).getTime();

                    EventJoinRequest eventJoinRequest = new EventJoinRequest();
                    eventJoinRequest.setEventId( jrConfirmedEventId );
                    eventJoinRequest.setUserId( requesterId );
                    eventJoinRequest.setCreatedTime( createdTime );

                    eventJoinRequestMap.put( requesterId, eventJoinRequest );
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

        return eventJoinRequestMap;
    }

    public HttpEntity<BasicResponse> acceptJoinRequest( long confirmedEventId, int requesterId )
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
            }
            catch(SQLException ex)
            {
                conn.rollback();
                conn.setAutoCommit(true);
                throw ex;
            }
            conn.commit();
            conn.setAutoCommit(true);

            //TODO NOTIFICATION to requester

            // Load updated event
            updatedEvent = loadConfirmedEventById( confirmedEventId, requesterId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> addEventInterest( long eventId, int userId, EventInterest eventInterest, long updatedTime )
    {
        EventResponse eventResponse;
        try ( Connection conn = getConnection() )
        {
            conn.setAutoCommit(false);
            try
            {
                addEventInterest( eventId, userId, eventInterest, updatedTime, conn );
                // Add new interest notification
                addInterestNotification( eventId, userId, conn );
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
            eventResponse = getEventById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( eventResponse ) );

    }

    public void addEventInterest( long eventId, int userId, EventInterest eventInterest, long updatedTime, Connection conn ) throws SQLException
    {
        // Add new interested user to the event
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_interested ( event_id, user_id, description ) VALUES ( ?, ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setInt( count++, userId );
            ps.setString( count++, eventInterest.getDescription() );

            //execute query
            ps.executeUpdate();

        }
    }

    public void addInterestNotification( long eventId, int userId, Connection conn ) throws SQLException
    {
        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO interest_notification ( event_id, user_id ) VALUES ( ?, ? ) ON CONFLICT DO NOTHING" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setInt( count++, userId );

            //execute query
            ps.executeUpdate();

        }
    }

    public HttpEntity<BasicResponse> removeEventInterest( long eventId, int userId )
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
                    ps.setInt( count++, userId );

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

            updatedEvent = getEventById( eventId, userId, conn );

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
            ps.setTimestamp( count++, new Timestamp( event.getCreatedTime() ) );

            //on duplicate key update
            ps.setTimestamp( count++, new Timestamp( event.getCreatedTime() ) );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public List<FeedItem> updatePopularActiveEvent( CurrentActivity currentActivity, Connection conn )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        // insert tag

        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO popular_active_event ( event_id, user_id, updated_time ) VALUES  ( ?, ?, ? ) ON DUPLICATE KEY UPDATE updated_time = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, currentActivity.getEventId() );
            ps.setInt( count++, currentActivity.getUserId() );
            ps.setTimestamp( count++, new Timestamp( currentActivity.getUpdatedTime() ) );

            //on duplicate key update
            ps.setTimestamp( count++, new Timestamp( currentActivity.getUpdatedTime() ) );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public HttpEntity<BasicResponse> sendVisibilityRequest( long eventId, int userId, int friendId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            //Add visibility request

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO visibility_request ( event_id, user_id, requester_id ) VALUES ( ?, ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, friendId );
                ps.setInt( count++, userId ); // Curr user_profile is the requester

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            updatedEvent = getEventById( eventId, userId, conn );

        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> addEventVisibility( long eventId, int userId, int friendId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = getConnection() )
        {
            // Make user_profile visible to friend in the event.

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO event_visibility ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? ) , ( ?, ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                // Make curr user_profile visible to the friend
                ps.setLong( count++, eventId );
                ps.setInt( count++, friendId );
                ps.setInt( count++, userId );

                // Make friend visible to the curr user_profile
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

            // Remove visibility request

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM visibility_request WHERE event_id = ? AND user_id = ? AND requester_id = ?" ) )
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

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO visibility_notification ( event_id, user_id, friend_id ) VALUES (?, ?, ?)" ) )
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
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> removeEventVisibility( long eventId, int userId )
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
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> getVisibilityRequests( int userId )
    {
        List<VisibilityRequest> notificationList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
            // Get Event Visibility Requests

            String sqlSb = "SELECT " +
                    "u.name, " +
                    "vr.requester_id, " +
                    EVENT_SELECT +
                    " ,visibility_request vr, user_profile u WHERE vr.user_id = ? AND vr.event_id = e.id AND vr.requester_id = u.id";

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
                        int requesterUserId = rs.getInt( col++ );

                        Event event = new Event();
                        event.load( rs );

                        UserProfile friend = new UserProfile();
                        friend.setUserId( requesterUserId );
                        friend.setDisplayName( requesterName );

                        VisibilityRequest notification = new VisibilityRequest();
                        notification.setUser( friend );
                        notification.setEvent( event );

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
        catch ( SQLException | URISyntaxException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }
        return new HttpEntity<>( new BasicResponse( notificationList ) );
    }

    //TODO Decline visibility request
    public HttpEntity<BasicResponse> declineVisibilityRequest( long eventId, int userId, int requesterId )
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
                ps.setInt( count++, userId );
                ps.setInt( count++, requesterId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            updatedEvent = getEventById( eventId, userId, conn );

        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedEvent ) );
    }

    public HttpEntity<BasicResponse> getEventNotifications( int userId )
    {
        List<EventNotification> notificationList = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
           notificationList.addAll( getInterestNotifications( userId, conn ) );
           notificationList.addAll( getVisibilityRevealNotifications( userId, conn ) );
           notificationList.addAll( getEventInviteNotifications( userId, conn ) );
           notificationList.addAll( getEventAcceptNotifications( userId, conn ) );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( notificationList ) );
    }

    public List<EventInterestNotification> getInterestNotifications( int userId, Connection conn )
    {
        List<EventInterestNotification> notificationList = new ArrayList<>();

        Set<Integer> userList = new HashSet<>();
        Map<Long, Event> eventMap = new HashMap<>();
        Map<Long, List<Integer>> eventInterestedMap = new HashMap<>();

        // Get new event interests

        String sqlSb = "SELECT " +
                "i.user_id, " +
                EVENT_SELECT +
                ", interest_notification i, user_profile u WHERE e.creator_id = ? AND i.event_id = e.id AND i.user_id = u.id";

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
                    int interestedUserId = rs.getInt( col++ );
                    long eventId = rs.getLong( col++ );

                    Event event = new Event();
                    event.load( rs );

                    userList.add( interestedUserId );
                    eventMap.putIfAbsent( eventId, event );

                    List<Integer> interestedUserIdList = new ArrayList<>();
                    interestedUserIdList.add( interestedUserId );

                    eventInterestedMap.merge( eventId, interestedUserIdList, ( currList, newList ) -> {
                        currList.add( interestedUserId );
                        return currList;
                    } );
                }

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return notificationList;
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return notificationList;
        }


        if ( eventMap.isEmpty() )
        {
            return notificationList;
        }

        for ( Long eventId : eventInterestedMap.keySet() )
        {
            List<Integer> interestedUserIdList = eventInterestedMap.get( eventId );
            EventInterestNotification interestNotification = new EventInterestNotification();

            Event event = eventMap.get( eventId );
            interestNotification.setEvent( event );
            interestNotification.setInterestedUserCount( interestedUserIdList.size() );

            notificationList.add( interestNotification );

        }

        // Remove interest notifications

//            StringBuilder deleteSb = new StringBuilder( "DELETE FROM interest_notification  WHERE event_id IN ( " );
//
//            String delim = "";
//
//            for ( Long eventId : eventInterestedMap.keySet() )
//            {
//                deleteSb.append( delim );
//                deleteSb.append( "?" );
//                delim = ", ";
//            }
//
//            deleteSb.append( " )" );
//            deleteSb.append( " AND user_id IN ( " );
//
//            delim = "";
//
//            for ( int interestedUserId : userList )
//            {
//                deleteSb.append( delim );
//                deleteSb.append( "?" );
//                delim = ", ";
//            }
//
//            deleteSb.append( " )" );
//
//            try ( PreparedStatement ps = conn.prepareStatement( deleteSb.toString() ) )
//            {
//
//                ps.setFetchSize( 1000 );
//
//                int count = 1;
//
//                for ( Long eventId : eventInterestedMap.keySet() )
//                {
//                    ps.setLong( count++, eventId );
//                }
//
//                for ( int interestedUserId : userList )
//                {
//                    ps.setInt( count++, interestedUserId );
//                }
//
//                //execute query
//                ps.executeUpdate();
//
//            }
//            catch ( SQLException e )
//            {
//                e.printStackTrace();
//            }

        return notificationList;
    }

    public List<EventAcceptNotification> getEventAcceptNotifications( int userId, Connection conn )
    {
        List<EventAcceptNotification> notificationList = new ArrayList<>();

        // Get new event interests

        String sqlSb = "SELECT " +
                "i.user_id, " +
                "u.name, " +
               CONFIRMED_EVENT_SELECT +
                ", accept_notification i, user_profile u WHERE ce.creator_id = ? AND i.event_id = ce.id AND i.user_id = u.id";

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
                    int participantId = rs.getInt( col++ );
                    String participantDisplayName = rs.getString( col++ );

                    ConfirmedEvent event = new ConfirmedEvent();
                    event.load( rs );

                    UserProfile participant = new UserProfile( participantId, null, null, participantDisplayName );

                    EventAcceptNotification notification = new EventAcceptNotification();
                    notification.setEvent( event );
                    notification.setUser( participant );

                    notificationList.add( notification );
                }

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return notificationList;
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return notificationList;
        }

        // Remove accept notifications

//            StringBuilder deleteSb = new StringBuilder( "DELETE FROM accept_notification  WHERE event_id IN ( " );
//
//            String delim = "";
//
//            for ( Long eventId : eventInterestedMap.keySet() )
//            {
//                deleteSb.append( delim );
//                deleteSb.append( "?" );
//                delim = ", ";
//            }
//
//            deleteSb.append( " )" );
//            deleteSb.append( " AND user_id IN ( " );
//
//            delim = "";
//
//            for ( int interestedUserId : userList )
//            {
//                deleteSb.append( delim );
//                deleteSb.append( "?" );
//                delim = ", ";
//            }
//
//            deleteSb.append( " )" );
//
//            try ( PreparedStatement ps = conn.prepareStatement( deleteSb.toString() ) )
//            {
//
//                ps.setFetchSize( 1000 );
//
//                int count = 1;
//
//                for ( Long eventId : eventInterestedMap.keySet() )
//                {
//                    ps.setLong( count++, eventId );
//                }
//
//                for ( int interestedUserId : userList )
//                {
//                    ps.setInt( count++, interestedUserId );
//                }
//
//                //execute query
//                ps.executeUpdate();
//
//            }
//            catch ( SQLException e )
//            {
//                e.printStackTrace();
//            }

        return notificationList;
    }

    public List<VisibilityRevealNotification> getVisibilityRevealNotifications( int userId, Connection conn )
    {
        List<VisibilityRevealNotification> notificationList = new ArrayList<>();

        // Get Event Requests

        String sqlSb = "SELECT " +
                "u.name, " +
                "vn.friend_id, " +
               EVENT_SELECT +
                ", visibility_notification vn, user_profile u WHERE vn.user_id = ? AND vn.event_id = e.id AND vn.friend_id = u.id";

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

                    Event event = new Event();
                    event.load( rs );

                    UserProfile friend = new UserProfile();
                    friend.setUserId( friendUserId );
                    friend.setDisplayName( friendName );

                    VisibilityRevealNotification notification = new VisibilityRevealNotification();
                    notification.setUser( friend );
                    notification.setEvent( event );

                    notificationList.add( notification );
                }

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return notificationList;
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return notificationList;
        }

        return notificationList;
    }

    public List<EventInviteNotification> getEventInviteNotifications( int userId, Connection conn )
    {
        List<EventInviteNotification> notificationList = new ArrayList<>();

        // Get Event Visibility Requests

        String sqlSb = "SELECT " +
                "u.name, " +
                "ei.sender_id, " +
                CONFIRMED_EVENT_SELECT +
                ", event_invite ei, user_profile u WHERE ei.receiver_id = ? AND ei.event_id = ce.id AND ei.sender_id = u.id";

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
                    long eventId = rs.getLong( col++ );

                    ConfirmedEvent event = new ConfirmedEvent();
                    event.load( rs );

                    UserProfile friend = new UserProfile();
                    friend.setUserId( friendUserId );
                    friend.setDisplayName( friendName );

                    EventInviteNotification notification = new EventInviteNotification();
                    notification.setUser( friend );
                    notification.setEvent( event );

                    notificationList.add( notification );
                }

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return notificationList;
            }
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return notificationList;
        }

        return notificationList;
    }

    public long getLastCreatedValueForEvent( Connection conn )
    {
        long eventId = -1L;

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT PREVIOUS VALUE FOR event_sequence" ) )
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

    public long getLastCreatedValueForConfirmedEvent( Connection conn )
    {
        long eventId = -1L;

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT PREVIOUS VALUE FOR confirmed_event_sequence" ) )
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

    public HttpEntity<BasicResponse> getEventById( long eventId, int userId )
    {
        EventResponse eventResponse;
        
        try ( Connection conn = getConnection() )
        {
            eventResponse = getEventById( eventId, userId, conn );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( eventResponse ) );
    }

    public EventResponse getEventById( long eventId, int userId, Connection conn )
    {
        EventResponse eventResponse = null;
        List<Integer> friendIdList = getFriendIdList( userId, conn );

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

                    Map<Integer, EventInterest> eventInterestMap = getEventInterested( eventId, conn );
                    List<String> tagList = getTagListByEventId( eventId, conn );

                    int interestedFriendCount = 0;

                    for ( Integer interestedUserId : eventInterestMap.keySet() )
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

    public HttpEntity<BasicResponse> getEventsCreatedByUser( int userId )
    {
        List<FeedItem> feedItems = new ArrayList<>();
        
        try ( Connection conn = getConnection() )
        {
            List<Integer> friendIdList = getFriendIdList( userId, conn );
            UserProfile currUser = getCompleteUserProfileById( userId, conn );

            // Load activities created by user_profile

            String sqlSb = "SELECT " + EVENT_SELECT +
                    "WHERE e.creator_id = ? ";

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
                        long eventId = rs.getLong( col++ );

                        Map<Integer, EventInterest> eventInterestMap = getEventInterested( eventId, conn );

                        Event event = new Event();
                        event.load( rs );
                        event.setInterestedCount( eventInterestMap.size() );

                        FeedItem feedItem = new FeedItem();
                        feedItem.setEvent( event );
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

            // Load confirmed events created by user_profile

            String confirmedSql = "SELECT " +
                    CONFIRMED_EVENT_SELECT +
                    "WHERE ce.creator_id = ? ";

            try ( PreparedStatement ps = conn.prepareStatement( confirmedSql ) )
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
                        long eventId = rs.getLong( "id" );
                        FeedItem feedItem = new FeedItem();

                        ConfirmedEvent event = new ConfirmedEvent();

                        Map<Integer, EventInvited> invitedMap = getEventInvited( eventId, conn );

                        event.load( rs );
                        event.setCreatorDisplayName( currUser.getDisplayName() );

                        feedItem.setConfirmedEvent( event );
                        feedItem.setParticipatingFriendCount( invitedMap.size() );
                        feedItem.setInterested( true );
                        feedItem.setInvited( true );
                        feedItem.setParticipant( true );

//                        feedItem.getConfirmedEvent().setCreatorDisplayName( "You" );
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
            
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedItems ) );
    }

    public HttpEntity<BasicResponse> getPopularEventsByTag( int userId, String tag )
    {
        List<FeedItem> feedItems = new ArrayList<>();

        try ( Connection conn = getConnection() )
        {
            List<Integer> friendIdList = getFriendIdList( userId, conn );

            List<FeedItem> eventList = loadPopularEvents( userId, tag, conn, friendIdList );
            List<FeedItem> confirmedEventList = loadPopularConfirmedEvents( userId, tag, conn, friendIdList );

            feedItems.addAll( eventList );
            feedItems.addAll( confirmedEventList );

            // Sort by latest timestamp
            feedItems.sort( ( o1, o2 ) -> {
                Event firstEvent = o1.getEvent() != null ? o1.getEvent() : o1.getConfirmedEvent();
                Event secondEvent = o2.getEvent() != null ? o2.getEvent() : o2.getConfirmedEvent();

                return Long.compare( secondEvent.getCreatedTime(), firstEvent.getCreatedTime() );
            } );
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedItems ) );
    }

    private List<FeedItem> loadPopularEvents( int userId, String tag, Connection conn, List<Integer> friendIdList )
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
            for ( int friendId : friendIdList )
            {
                sqlSb.append( delim );
                sqlSb.append( "?" );
                delim = ", ";
            }

            sqlSb.append( "         ) )" ); //Close IN and IN
            sqlSb.append( "     ) " ); //Close OR
            sqlSb.append( "     OR ( e.visibility_preference = " + PrivacyPreference.VISIBILITY_FRIENDS + " AND e.creator_id IN ( " );

            delim = " ";
            for ( int friendId : friendIdList )
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
                for ( int friendId : friendIdList )
                {
                    ps.setInt( count++, friendId );
                }

                // For VISIBILITY_FRIENDS
                for ( int friendId : friendIdList )
                {
                    ps.setInt( count++, friendId );
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


                    Map<Integer, EventInterest> eventInterestedMap = getEventInterested( eventId, conn );

                    List<Integer> interestedUserIdList = new ArrayList<>( eventInterestedMap.keySet() );
                    FeedItem feedItem = new FeedItem();

                    Event event = new Event();
                    event.load( rs );
                    event.setInterestedCount( interestedUserIdList.size() );

                    feedItem.setEvent( event );

                    boolean isCreatorFriend = friendIdList.contains( event.getCreatorId() );
                    boolean isInterested = eventInterestedMap.containsKey( userId );
                    boolean isFriendInterested = false;

                    for ( Integer friendId : friendIdList )
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

    private List<FeedItem> loadPopularConfirmedEvents( int userId, String tag, Connection conn, List<Integer> friendIdList )
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

                    Map<Integer, EventInvited> invitedMap = getEventInvited( eventId, conn );

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

    public List<PopularNearbyFeedItem> loadPopularNearbyFeedItemList( List<Long> confirmedEventIdList, List<Integer> friendIdList, Map<Long, List<CurrentActivity>> activeEventMap, int userId, Connection conn )
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

                    Map<Integer, EventInvited> invitedMap = getEventInvited( confirmedEvent.getId(), conn );
                    Map<Integer, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( confirmedEvent.getId(), conn );
                    Set<Integer> confirmedIdSet = invitedMap.values().stream().filter( EventInvited::isConfirmed ).map( EventInvited::getUserId ).collect( Collectors.toSet() );

                    boolean isInvited = invitedMap.containsKey( userId );
                    boolean isParticipating = confirmedIdSet.contains( userId );
                    boolean isCreatorFriend = friendIdList.contains( confirmedEvent.getCreatorId() );
                    boolean isFriendConfirmed = Collections.disjoint( confirmedIdSet, friendIdList );
                    boolean isJoinRequested = eventJoinRequestMap.containsKey( userId );
                    int participantCount = confirmedIdSet.size();
                    boolean isHappeningFeedItem = isInvited || isFriendConfirmed;

                    if ( confirmedEvent.isPublic() || isInvited )
                    {
                        UserProfile creatorUser = getUserProfileById( confirmedEvent.getCreatorId(), conn );
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

    public Map<Long, List<CurrentActivity>> loadCurrentActivityByFriendList( List<Integer> friendIdList, int userId, Connection conn )
    {
        Map<Long, List<CurrentActivity>> activeFriendMap = new HashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " +
                "ca.user_id, " +
                "ca.event_id, " +
                "ca.updated_time " +
                "FROM current_activity ca " +
                "WHERE " +
                "ca.event_id IS NOT NULL " +
                "AND ca.user_id IN ( " );

        String delim = " ";

        for ( int eventUserId : friendIdList )
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

    public List<UpcomingFeedItem> loadUpcomingEvents( List<Integer> friendIdList, int userId, Connection conn )
    {
        List<UpcomingFeedItem> feedItems = new ArrayList<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " );
        sqlSb.append( EVENT_SELECT );
        sqlSb.append( "WHERE " );
        sqlSb.append( "e.id IN ( SELECT event_id FROM event_interested ei WHERE ei.user_id IN ( " );

        String delim = " ";
        for ( int friendId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( "         ) )" ); //Close IN and IN

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            for ( int friendId : friendIdList )
            {
                ps.setInt( count++, friendId );
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

                    UpcomingFeedItem feedItem = new UpcomingFeedItem();

                    Map<Integer, EventInterest> eventInterestMap = getEventInterested( eventId, conn );

                    Event event = new Event();
                    event.load( rs );
                    event.setInterestedCount( eventInterestMap.size() );

                    boolean isInterested = eventInterestMap.containsKey( userId );
                    boolean isCreatorFriend = friendIdList.stream().anyMatch( e -> e == event.getCreatorId() );
                    boolean isFriendInterested = friendIdList.stream().anyMatch( eventInterestMap::containsKey );
                    int interestedFriendCount = ( int ) eventInterestMap.keySet().stream().filter( friendIdList::contains ).count();

                    feedItem.setEvent( event );
                    feedItem.setInterestedFriendCount( interestedFriendCount );
                    feedItem.setInterested( isInterested );
                    feedItem.setCreatorFriend( isCreatorFriend );
                    feedItem.setFriendInterested( isFriendInterested );

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

    public List<UpcomingFeedItem> loadUpcomingConfirmedEvents( List<Integer> friendIdList, int userId, Connection conn )
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

                    Map<Integer, EventInvited> invitedMap = getEventInvited( eventId, conn );
                    Map<Integer, EventJoinRequest> eventJoinRequestMap = getEventJoinRequestsByConfirmedEventId( eventId, conn );

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

    public HttpEntity<BasicResponse> getCompleteUserProfileById( int userId )
    {
        UserProfile user_profile = new UserProfile(-1, null, null, null);
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

    public UserProfile getCompleteUserProfileById( int userId, Connection conn ) throws SQLException
    {
        UserProfile userProfile = null;

        String sql = "SELECT " +
                "u.id, " +
                "u.facebook_id, " +
                "u.firebase_uid, " +
                "u.name, " +
                "u.latitude, " +
                "u.longitude, " +
                "u.high_school_id, " +
                "u.university_id, " +
                "u.work_place_id " +
                "FROM user_profile u " +
                "WHERE u.id = ? ";

        try ( PreparedStatement ps = conn.prepareStatement( sql  ) )
        {

            ps.setFetchSize( 1000 );

            int pCount = 1;

            ps.setInt( pCount++, userId );

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

    public HttpEntity<BasicResponse> getInterestedFriendList( long eventId, int userId )
    {
        List<InterestedFriend> interestedFriendList;
        
        try ( Connection conn = getConnection() )
        {
            Map<Integer, InterestedFriend> friendMap = getInterestedFriendList( eventId, userId, conn );
            interestedFriendList = new ArrayList<>(friendMap.values());
        }
        catch ( SQLException | URISyntaxException e )
        {
            e.printStackTrace();
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( interestedFriendList ) );
    }

    public Map<Integer, InterestedFriend> getInterestedFriendList( long eventId, int userId, Connection conn ) throws SQLException
    {
        Map<Integer, InterestedFriend> interestedFriendMap = new HashMap<>();

        List<Integer> friendIdList = getFriendIdList( userId, conn );
        Map<Integer, EventInterest> eventInterestMap = getEventInterested( eventId, conn );

        // Get interested friend ids
        List<Integer> interestedFriendIdList = new ArrayList<>();

        for ( Integer interestedUserId : eventInterestMap.keySet() )
        {
            if ( friendIdList.contains( interestedUserId ) || interestedUserId == userId )
            {
                interestedFriendIdList.add( interestedUserId );
            }
        }

        // Get interested friend details
        Map<Integer, UserProfile> interestedUserProfileMap = getUserProfilesWithDetails( interestedFriendIdList, conn );
        Map<Integer, List<CurrentActivity>> userActivityHistoryMap = getRecentUserActivityHistory( interestedFriendIdList, conn );

        // Get friend visibility matrix for current user_profile
        Map<Integer, List<Integer>> visibilityMap = getInterestedVisibilityMatrix( eventId, conn );
        List<Integer> requestedFriendList = getVisibilityRequestedByUser( eventId, userId, conn );

        //Load current user_profile details
        UserProfile currUser = getCompleteUserProfileById( userId, conn );
        List<CurrentActivity> currUserRecentCurrentActivityHistory = userActivityHistoryMap.get( userId );

        for ( Integer interestedUserId : interestedUserProfileMap.keySet() )
        {
            UserProfile friendProfile = interestedUserProfileMap.get( interestedUserId );
            EventInterest eventInterest = eventInterestMap.get( interestedUserId );
            List<CurrentActivity> recentCurrentActivityList = userActivityHistoryMap.get( interestedUserId );

            String distance = null;
            String relationship = null;

            if( interestedUserId != userId )
            {
                double distanceInMeters = LocationHelper.distance( currUser.getLatitude(), friendProfile.getLatitude(), currUser.getLongitude(), friendProfile.getLongitude(), 0.0, 0.0 );
                System.out.println( friendProfile.getDisplayName() + " : " + distanceInMeters );

                if ( distanceInMeters <= 10000 )
                {
                    distance = "nearby";
                }
                else
                {
                    distance = "far away";
                }

                // Remove identifying details from non-visible friends

                List<Integer> visibleToUserList = new ArrayList<>();

                if ( visibilityMap.containsKey( interestedUserId ) )
                {
                    visibleToUserList = visibilityMap.get( interestedUserId );
                }

                if ( visibleToUserList.isEmpty() || !visibleToUserList.contains( userId ) )
                {
                    friendProfile.setDisplayName( null );
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

            InterestedFriend interestedFriend = new InterestedFriend();
            interestedFriend.setUser( friendProfile );
            interestedFriend.setDescription( eventInterest.getDescription() );
            interestedFriend.setDistance( distance );
            interestedFriend.setRelationship( relationship );

            if ( requestedFriendList.contains( friendProfile.getUserId() ) )
            {
                interestedFriend.setVisibilityRequested( true );
            }

            interestedFriendMap.put( interestedUserId, interestedFriend );
        }

        return interestedFriendMap;
    }

    public UserProfile getUserProfileById( int userId, Connection conn )
    {
        UserProfile userProfile = null;

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT u.id, u.name FROM user_profile u WHERE u.id = ?" ) )
        {

            ps.setFetchSize( 1000 );

            int pCount = 1;

            ps.setInt( pCount++, userId );

            //execute query
            try ( ResultSet rs = ps.executeQuery() )
            {
                //position result to first

                while ( rs.next() )
                {
                    userProfile = new UserProfile();
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

    public Map<Integer, UserProfile> getUserProfileByList( List<Integer> userIdList, Connection conn )
    {
        Map<Integer, UserProfile> userProfileList = new HashMap<>();

        if ( userIdList.isEmpty() )
        {
            return userProfileList;
        }

        StringBuilder friendSql = new StringBuilder( "SELECT u.id, u.name FROM user_profile u WHERE u.id IN ( " );

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

    public Map<Integer, UserProfile> getUserProfilesWithDetails( List<Integer> userIdList, Connection conn )
    {
        Map<Integer, UserProfile> userProfileList = new HashMap<>();

        if ( userIdList.isEmpty() )
        {
            return userProfileList;
        }

        StringBuilder friendSql = new StringBuilder(  "SELECT " +
                "u.id, " +
                "u.facebook_id, " +
                "u.firebase_uid, " +
                "u.name, " +
                "u.latitude, " +
                "u.longitude, " +
                "u.high_school_id, " +
                "u.university_id, " +
                "u.work_place_id " +
                "FROM user_profile u " +
                "WHERE u.id IN ( " );

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
                    UserProfile profile = new UserProfile();
                    profile.loadCompleteProfileFromResultSet( rs );

                    userProfileList.put( profile.getUserId(), profile );
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

    public Map<Integer, EventInterest> getEventInterested( long eventId, Connection conn )
    {
        Map<Integer, EventInterest> interestMap = new HashMap<>();

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
                    int userId = rs.getInt( col++ );
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


    public void updateFriendList()
    {
        // Check if need to update friends list

            /*
            String checkSql = "SELECT u.last_checked_friends FROM user_profile u WHERE u.facebook_id = ?";

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

                /*try ( PreparedStatement ps = conn.prepareStatement( "UPDATE user_profile SET last_checked_friends = NOW()" ) )
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

    public HttpEntity<BasicResponse> updateUserLocation( UserLocation userLocation, int userId )
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

    public UserProfile updateUserLocation( UserLocation userLocation, int userId, Connection conn )
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

        return getUserProfileById( userId, conn );
    }

}
