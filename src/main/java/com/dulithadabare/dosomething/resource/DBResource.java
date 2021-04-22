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

    private final String EVENT_SELECT = " e.id," +
            " e.creator_id," +
            " e.activity, " +
            " e.description, " +
            " e.is_confirmed, " +
            " e.timestamp " +
            " FROM event e ";
    private final String CONFIRMED_EVENT_SELECT = " ce.id," +
            " ce.creator_id," +
            " ce.activity, " +
            " ce.description, " +
            " ce.date, " +
            " ce.time, " +
            " ce.is_public, " +
            " ce.is_happening, " +
            " ce.timestamp " +
            "FROM confirmed_event ce ";

    public HttpEntity<BasicResponse> createAnonymousUser( UserProfile userProfile )
    {
        int newUserId;
        UserProfile newUser;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Create User

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO user ( firebase_uid ) VALUES ( ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, userProfile.getFirebaseUid() );

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
            newUser = getUserProfileById( newUserId, conn );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( newUser ) );
    }

    public HttpEntity<BasicResponse> linkWithFacebook( String facebookUserToken, UserProfile userProfile )
    {
        UserProfile updatedProfile;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // update user details from facebook

//            PublicProfile publicProfile = facebookResource.getPublicProfile( userProfile.getFacebookId(), facebookUserToken );
//            PictureResponse pictureResponse = facebookResource.getProfilePicture( userProfile.getFacebookId(), facebookUserToken );

            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE user SET name = ? )" ) )
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

            updatedProfile = getUserProfileById( userProfile.getUserId(), conn );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( updatedProfile ) );
    }

    public HttpEntity<BasicResponse> getActivityFeed( int userId )
    {
        List<ActivityFeedItem> feedItemList;

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

            feedItemList = getFriendActivity( new ArrayList<>( friendIdList ), userId, conn );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedItemList ) );
    }

    public HttpEntity<BasicResponse> getUpcomingEvents( int userId )
    {
        List<FeedItem> feedItemList;

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

            // Get confirmed events by friends

            feedItemList = getConfirmedEventsByFriends( new ArrayList<>( friendIdList ), userId, conn );

            // Get events by friends

            feedItemList.addAll( getEventsCreatedByFriends( friendIdList, userId, conn ) );
        }
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( feedItemList ) );
    }

    public HttpEntity<BasicResponse> startCurrentActivity( Activity activity, int userId )
    {
        Activity currentActivity = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Start event

            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE confirmed_event SET is_happening = TRUE WHERE id = ?" ) )
            {
                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, activity.getEventId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
                return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
            }

            // Start activity in the event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO current_activity (" +
                    " user_id," +
                    " event_id ," +
                    " updated_time" +
                    " ) VALUES ( ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE event_id = ?, updated_time = ?" ) )
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

                //Load event
                EventResponse event = getConfirmedEventById( activity.getEventId(), userId, conn );

                // update event tag
                addEventTag( event.getConfirmedEvent().getTag(), conn );

                //get current activity
                currentActivity = getCurrentActivityById( userId, conn );

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

        return new HttpEntity<>( new BasicResponse( currentActivity ) );
    }

    public HttpEntity<BasicResponse> stopCurrentActivity( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Remove event interested user

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM current_activity WHERE user_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

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

        return new HttpEntity<>( new BasicResponse( userId ) );
    }

    public Activity getCurrentActivityById( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getCurrentActivityById( userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return null;
    }

    public Activity getCurrentActivityById( int userId, Connection conn )
    {
        Activity activity = null;

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
                    activity = new Activity();
                    activity.load( rs );
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


        return activity;
    }

    public EventResponse createEvent( Event event, int userId )
    {
        EventResponse createdEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {

            // Create event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event (" +
                    " creator_id ," +
                    " activity," +
                    " description," +
                    " timestamp" +
                    " ) VALUES ( ?, ?, ?, ?)" ) )
            {
                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );
                ps.setString( count++, event.getTag() );
                ps.setString( count++, event.getDescription() );
                ps.setTimestamp( count++, new Timestamp( event.getTimestamp() ) );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Get created event id
            long eventId = getLastCreatedValueForEvent( conn );

            //Add event interest for creator
            EventInterest eventInterest = new EventInterest( eventId, userId, "Creator" );
            createdEvent = addEventInterest( eventId, userId, eventInterest, true, conn );

            // update event tag
            addEventTag( event.getTag(), conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return createdEvent;
    }

    public EventResponse createConfirmedEvent( ConfirmedEvent event, int userId )
    {
        EventResponse createdEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Create confirmed event

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO confirmed_event (" +
                    " creator_id," +
                    " activity," +
                    " description," +
                    " date," +
                    " time," +
                    " is_public," +
                    " timestamp" +
                    " ) VALUES ( ?, ?, ?, ?, ?, ?, ?)" ) )
            {
                Date date = event.getDate() != null && !event.getDate().isEmpty() ? Date.valueOf( event.getDate() ) : null;

                Time time = event.getTime() != null && !event.getTime().isEmpty() ? Time.valueOf( LocalTime.parse( event.getTime() ) ) : null;

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setInt( count++, userId );
                ps.setString( count++, event.getTag() );
                ps.setString( count++, event.getDescription() );
                ps.setDate( count++, date );
                ps.setTime( count++, time );
                ps.setBoolean( count++, event.isPublic() );
                ps.setTimestamp( count++, new Timestamp( event.getTimestamp() ) );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Get created event id
            long confirmedEventId = getLastCreatedValueForConfirmedEvent( conn );
            event.setId( confirmedEventId );

            //Add event participants

            StringBuilder insertSqlSb = new StringBuilder( "INSERT IGNORE INTO event_participant ( event_id, user_id ) VALUES " );

            String delim = " ";

            //For event creator
            insertSqlSb.append( "( ?, ? ) " );
            delim = ", ";

            for ( EventParticipant participant : event.getParticipantList() )
            {
                insertSqlSb.append( delim );
                insertSqlSb.append( "( ?, ? )" );
            }

            try ( PreparedStatement ps = conn.prepareStatement( insertSqlSb.toString() ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                // Add event creator as participant
                ps.setLong( count++, confirmedEventId );
                ps.setInt( count++, userId );

                for ( EventParticipant participant : event.getParticipantList() )
                {
                    ps.setLong( count++, confirmedEventId );
                    ps.setInt( count++, participant.getUserId() );
                }

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            List<VisibilityPair> visibilityPairList = new ArrayList<>();

            // Load from event interested visibility matrix

            String interestedVisibilitySql = "SELECT " +
                    "ev.user_id, " +
                    "ev.friend_id " +
                    "FROM event_visibility ev WHERE ev.event_id = ?";

            try ( PreparedStatement ps = conn.prepareStatement( interestedVisibilitySql ) )
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
                        int visibleUserId = rs.getInt( col++ );
                        int visibleToFriendId = rs.getInt( col++ );

                        VisibilityPair visibilityPair = new VisibilityPair( visibleUserId, visibleToFriendId );

                        visibilityPairList.add( visibilityPair );
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

            //Add event participant visibility

            // Make event creator visible to all invited users
            for ( EventParticipant participant : event.getParticipantList() )
            {
                VisibilityPair visibilityPair = new VisibilityPair( userId, participant.getUserId() );
                visibilityPairList.add( visibilityPair );
            }


            if ( !visibilityPairList.isEmpty() )
            {
                StringBuilder visibilitySqlSb = new StringBuilder( "INSERT IGNORE INTO participant_visibility ( event_id, user_id, friend_id ) VALUES " );

                delim = " ";

                for ( VisibilityPair visibilityPair : visibilityPairList )
                {
                    visibilitySqlSb.append( delim );
                    visibilitySqlSb.append( "( ?, ?, ? )" );
                    delim = ", ";
                }

                try ( PreparedStatement ps = conn.prepareStatement( visibilitySqlSb.toString() ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    for ( VisibilityPair visibilityPair : visibilityPairList )
                    {
                        ps.setLong( count++, confirmedEventId );
                        ps.setInt( count++, visibilityPair.getUserId() );
                        ps.setInt( count++, visibilityPair.getFriendId() );
                    }

                    //execute query
                    ps.executeUpdate();

                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                }
            }

            // Confirm event participation for creator
            addEventParticipation( confirmedEventId, userId, conn );

            // Add confirm notifications to invitees

            if ( !event.getParticipantList().isEmpty() )
            {
                StringBuilder updateSqlSb = new StringBuilder( "INSERT IGNORE INTO event_invite ( event_id, sender_id, receiver_id ) VALUES " );

                String delimiter = " ";

                for ( EventParticipant invitee : event.getParticipantList() )
                {
                    updateSqlSb.append( delimiter );
                    updateSqlSb.append( "(?, ?, ?)" );
                    delimiter = ", ";
                }

                try ( PreparedStatement ps = conn.prepareStatement( updateSqlSb.toString() ) )
                {

                    ps.setFetchSize( 1000 );

                    int count = 1;

                    for ( EventParticipant invitee : event.getParticipantList() )
                    {
                        ps.setLong( count++, confirmedEventId );
                        ps.setInt( count++, userId );
                        ps.setInt( count++, invitee.getUserId() );
                    }

                    //execute query
                    ps.executeUpdate();

                }
                catch ( SQLException e )
                {
                    e.printStackTrace();
                }
            }

            createdEvent = getConfirmedEventById( confirmedEventId, userId, conn );

            // Make original event inactive

            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event SET " +
                    " is_confirmed = TRUE" +
                    " WHERE id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;
                ps.setLong( count++, event.getEventId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // update event tag
            addEventTag( event.getTag(), conn );


        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return createdEvent;
    }

    public EventResponse updateConfirmedEvent( ConfirmedEvent event, int userId )
    {
        //TODO only creator with userId can update a confirmed event
        EventResponse updatedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            try ( PreparedStatement ps = conn.prepareStatement( "UPDATE confirmed_event SET " +
                    " date = ?," +
                    " time = ?," +
                    " is_public = ?" +
                    " WHERE id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                Date date = event.getDate() != null ? Date.valueOf( event.getDate() ) : null;
                Time time = event.getTime() != null ? Time.valueOf( LocalTime.parse( event.getTime() ) ) : null;

                ps.setDate( count++, date );
                ps.setTime( count++, time );
                ps.setBoolean( count++, event.isPublic() );
                ps.setLong( count++, event.getId() );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Get created event id

            updatedEvent = getConfirmedEventById( event.getId(), userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public EventResponse getConfirmedEventById( long eventId, int userId )
    {
        EventResponse confirmedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            confirmedEvent = getConfirmedEventById( eventId, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return confirmedEvent;
    }


    public EventResponse getConfirmedEventById( long eventId, int userId, Connection conn )
    {
        EventResponse eventResponse = null;

        String sqlSb = "SELECT " + CONFIRMED_EVENT_SELECT +
                " WHERE ce.id = ? ";

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

                    boolean isPublic = rs.getBoolean( "is_public" );

                    Map<Integer, EventParticipant> participantMap = getEventParticipants( eventId, conn );

                    Map<Integer, List<Integer>> visibilityMap = getVisibilityMatrix( eventId, conn );
                    updateParticipantVisibility( eventId, userId, visibilityMap, participantMap );

                    boolean isInvited = participantMap.containsKey( userId );
                    boolean isParticipating = false;

                    if ( isInvited )
                    {
                        isParticipating = participantMap.get( userId ).isConfirmed();
                    }

                    ConfirmedEvent event = new ConfirmedEvent();

                    if ( isPublic || isInvited )
                    {
                        event.load( rs );
                        event.setCreatorDisplayName( participantMap.get( event.creatorId ).getUser().getDisplayName() );
                        event.setParticipantList( new ArrayList<>( participantMap.values() ) );
                    }
                    else
                    {
                        event.loadPrivateEvent( rs );
                        event.setParticipantCount( participantMap.size() );
                        event.setParticipantList( new ArrayList<>() );
                    }

                    eventResponse.setConfirmedEvent( event );
                    eventResponse.setInvited( isInvited );
                    eventResponse.setParticipant( isParticipating );
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

    private void updateParticipantVisibility( long eventId, int userId, Map<Integer, List<Integer>> visibilityMap, Map<Integer, EventParticipant> participantMap )
    {
        for ( Integer participantId : participantMap.keySet() )
        {
            EventParticipant participant = participantMap.get( participantId );

            if ( !participant.isConfirmed() )
            {
                //if participant is not visible to current user
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

    public Map<Integer, EventParticipant> getEventParticipants( long eventId, Connection conn )
    {
        Map<Integer, EventParticipant> participantMap = new HashMap<>();

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
                    int participantId = rs.getInt( col++ );
                    boolean isConfirmed = rs.getBoolean( col++ );

                    EventParticipant participant = new EventParticipant();
                    participant.setUserId( participantId );
                    participant.setConfirmed( isConfirmed );

                    participantMap.put( participantId, participant );
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

        // Load participant profiles

        Map<Integer, UserProfile> participantProfileMap = getUserProfiles( new ArrayList<>( participantMap.keySet() ), conn );


        for ( Integer participantId : participantProfileMap.keySet() )
        {
            UserProfile user = participantProfileMap.get( participantId );
            EventParticipant participant = participantMap.get( participantId );
            participant.setUser( user );
        }

        return participantMap;
    }

    public  List<Integer> getVisibilityRequestedByUser( long eventId, int userId )
    {
        List<Integer> visibilityRequestedIdList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Get Event Visibility Requests

            String sqlSb = "SELECT " +
                    "vr.user_id " +
                    "FROM visibility_request vr WHERE vr.event_id = ? AND vr.friend_id = ?";

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
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return visibilityRequestedIdList;
    }

    public Map<Integer, List<Integer>> getVisibilityMatrix( long eventId, Connection conn )
    {
        Map<Integer, List<Integer>> visibilityMap = new HashMap<>();

        String sqlSb = "SELECT " +
                " user_id," +
                " friend_id " +
                "FROM participant_visibility pv " +
                "WHERE pv.event_id = ? ";

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

    public EventResponse acceptEventInvite( long eventId, int userId, int senderId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Add event participation

            addEventParticipation( eventId, userId, conn );

            // Remove event invite

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM event_invite WHERE event_id = ? AND sender_id = ? AND receiver_id = ?" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setLong( count++, eventId );
                ps.setInt( count++, senderId );
                ps.setInt( count++, userId );

                //execute query
                ps.executeUpdate();

            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }

            // Add accept notification

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO accept_notification ( event_id, user_id ) VALUES ( ?, ? )" ) )
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

            updatedEvent = getConfirmedEventById( eventId, userId, conn );

            // update event tag
            addEventTag( updatedEvent.getConfirmedEvent().getTag(), conn );

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public void addEventParticipation( long eventId, int userId, Connection conn )
    {
        // Add new interested user to the event

        try ( PreparedStatement ps = conn.prepareStatement( "UPDATE event_participant SET is_confirmed = TRUE WHERE event_id = ? AND user_id = ?" ) )
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
    }

    public EventResponse addEventInterest( long eventId, int userId, EventInterest eventInterest, boolean isCreator )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return addEventInterest( eventId, userId, eventInterest, isCreator, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return null;

    }

    public EventResponse addEventInterest( long eventId, int userId, EventInterest eventInterest, boolean isCreator, Connection conn )
    {
        EventResponse updatedEvent = null;

        // Add new interested user to the event

        try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_interested ( event_id, user_id, description ) VALUES ( ?, ?, ? )" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setLong( count++, eventId );
            ps.setInt( count++, userId );
            ps.setString( count++, eventInterest.getDescription() );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        // Add new interest notification

        try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO interest_notification ( event_id, user_id ) VALUES ( ?, ? )" ) )
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

        // update event tag

        addEventTag( updatedEvent.getEvent().getTag(), conn );

        return updatedEvent;
    }

    public EventResponse removeEventInterest( long eventId, int userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
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

    public List<FeedItem> addEventTag( String tag )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            addEventTag( tag, conn );

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;

    }

    public List<FeedItem> addEventTag( String tag, Connection conn )
    {
        List<FeedItem> updatedEvent = new ArrayList<>();

        // insert tag

        try ( PreparedStatement ps = conn.prepareStatement( "INSERT INTO popular_tag ( tag ) VALUES  ( ? ) ON DUPLICATE KEY UPDATE involved_count = involved_count + 1" ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            ps.setString( count++, tag );

            //execute query
            ps.executeUpdate();

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public EventResponse sendVisibilityRequest( long eventId, int userId, int friendId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Make user visible to friends in the event.

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO visibility_request ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? )" ) )
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

    public EventResponse addEventVisibility( long eventId, int userId, int friendId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Make user visible to friend in the event.

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO event_visibility ( event_id, user_id, friend_id ) VALUES ( ?, ?, ? ) , ( ?, ?, ? )" ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                // Make user visible to friend
                ps.setLong( count++, eventId );
                ps.setInt( count++, friendId );
                ps.setInt( count++, userId );

                // Make friend visible to user
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

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM visibility_request WHERE event_id = ? AND user_id = ? AND friend_id = ?" ) )
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

            try ( PreparedStatement ps = conn.prepareStatement( "INSERT IGNORE INTO visibility_notification ( event_id, user_id, friend_id ) VALUES (?, ?, ?)" ) )
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

    public EventResponse removeEventVisibility( long eventId, int userId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Remove user visibility

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
                    EVENT_SELECT +
                    " ,visibility_request vr, user u WHERE vr.user_id = ? AND vr.event_id = e.id AND vr.friend_id = u.id";

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
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( notificationList ) );
    }

    //TODO Decline visibility request

    public EventResponse declineVisibilityRequest( long eventId, int userId, int friendId )
    {
        EventResponse updatedEvent = null;

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            // Remove user visibility

            try ( PreparedStatement ps = conn.prepareStatement( "DELETE FROM visibility_request  WHERE event_id = ? AND user_id = ? AND friend_id = ?" ) )
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

            updatedEvent = getEventById( eventId, userId, conn );

        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return updatedEvent;
    }

    public HttpEntity<BasicResponse> getEventNotifications( int userId )
    {
        List<EventNotification> notificationList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
           notificationList.addAll( getInterestNotifications( userId, conn ) );
           notificationList.addAll( getVisibilityRevealNotifications( userId, conn ) );
           notificationList.addAll( getEventInviteNotifications( userId, conn ) );
           notificationList.addAll( getEventAcceptNotifications( userId, conn ) );
        }
        catch ( SQLException e )
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
                ", interest_notification i, user u WHERE e.creator_id = ? AND i.event_id = e.id AND i.user_id = u.id";

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
                ", accept_notification i, user u WHERE ce.creator_id = ? AND i.event_id = ce.id AND i.user_id = u.id";

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
                ", visibility_notification vn, user u WHERE vn.user_id = ? AND vn.event_id = e.id AND vn.friend_id = u.id";

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
                ", event_invite ei, user u WHERE ei.receiver_id = ? AND ei.event_id = ce.id AND ei.sender_id = u.id";

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

    public EventResponse getEventById( long eventId, int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getEventById( eventId, userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return null;
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
                    eventResponse = getFeedItem( friendIdList, userId, conn, rs );
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

    public List<FeedItem> getEventsCreatedByUser( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            List<FeedItem> feedItems = new ArrayList<>();

            List<Integer> friendIdList = getFriendIdList( userId, conn );

            // Load activities created by user

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
                        feedItem.getEvent().setCreatorDisplayName( "You" );
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

            // Load confirmed events created by user

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

                        Map<Integer, EventParticipant> participantMap = getEventParticipants( eventId, conn );

                        event.load( rs );
                        event.setCreatorDisplayName( participantMap.get( event.creatorId ).getUser().getDisplayName() );

                        feedItem.setConfirmedEvent( event );
                        feedItem.setParticipatingFriendCount( participantMap.size() );
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

    public List<FeedItem> getEventsByTag( int userId, String tag )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            List<FeedItem> feedItems = new ArrayList<>();
            List<Integer> friendIdList = getFriendIdList( userId, conn );

            // Load activities

            String sqlSb = "SELECT " +
                    EVENT_SELECT +
                    "WHERE e.activity = ?";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                int count = 1;

                ps.setString( count++, tag ); // for requests

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

                        event.setCreatorDisplayName( null );
                        feedItem.setEvent( event );

                        boolean isCreatorFriend = friendIdList.contains( event.creatorId );
                        boolean isFriendInterested = false;

                        for ( Integer friendId : friendIdList )
                        {
                            if ( eventInterestedMap.containsKey( friendId ) )
                            {
                                isFriendInterested = true;
                                break;
                            }
                        }

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

                            Map<Integer, EventParticipant> participantMap = getEventParticipants( eventId, conn );

                            event.load( rs );

                            feedItem.setConfirmedEvent( event );
                            feedItem.setParticipatingFriendCount( participantMap.size() );

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

    public HttpEntity<BasicResponse> getPopularTags( int userId )
    {
        List<PopularTag> popularTagList = new ArrayList<>();

        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            String sqlSb = "SELECT " +
                    " tag," +
                    " involved_count " +
                    "FROM popular_tag ORDER BY involved_count DESC LIMIT 20";

            try ( PreparedStatement ps = conn.prepareStatement( sqlSb ) )
            {

                ps.setFetchSize( 1000 );

                //execute query
                try ( ResultSet rs = ps.executeQuery() )
                {
                    //position result to first

                    while ( rs.next() )
                    {
                        int col = 1;
                        String tag = rs.getString( col++ );
                        int involvedCount = rs.getInt( col++ );

                        PopularTag popularTag = new PopularTag();
                        popularTag.setTag( tag );
                        popularTag.setInvolvedCount( involvedCount );

                        popularTagList.add( popularTag );
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
        catch ( SQLException e )
        {
            return new HttpEntity<>( new BasicResponse( e.getMessage(), BasicResponse.STATUS_ERROR ) );
        }

        return new HttpEntity<>( new BasicResponse( popularTagList ) );
    }

    public List<ActivityFeedItem> getFriendActivity( List<Integer> friendIdList, int userId, Connection conn )
    {
        List<ActivityFeedItem> feedItems = new ArrayList<>();
        List<Activity> activityList = new ArrayList<>();
        Map<Long, List<Integer>> activeUserMap = new HashMap<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " +
                "ca.user_id, " +
                "ca.event_id, " +
                "ca.updated_time " +
                "FROM current_activity ca " +
                "WHERE " +
                "ca.user_id IN ( " );

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
                    Activity activity = new Activity();
                    activity.load( rs );

                    activityList.add( activity );

                    List<Integer> activeUserIdList = new ArrayList<>();
                    activeUserIdList.add( activity.getUserId() );

                    activeUserMap.merge( activity.getEventId(), activeUserIdList, ( oldList, newList ) -> {
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

        // Get list of confirmed events

        Set<Long> eventIdSet = new HashSet<>();
        for ( Activity activity : activityList )
        {
            eventIdSet.add( activity.getEventId() );
        }

        //load event list

        List<EventResponse> eventResponseList = new ArrayList<>();

        for ( Long eventId : eventIdSet )
        {
            eventResponseList.add( getConfirmedEventById( eventId, userId, conn ) );
        }

        // load friend profiles

        Map<Integer, UserProfile> friendProfileMap = getUserProfiles( friendIdList, conn );

        //Load current activity for user

        Activity currActivity = getCurrentActivityById( userId );

        Map<String, List<ActivityItem>> tagMap = new HashMap<>();

        for ( EventResponse eventResponse : eventResponseList )
        {
            ConfirmedEvent event = eventResponse.getConfirmedEvent();

            List<Integer> activeUserList = activeUserMap.get( event.getId() );
            List<UserProfile> activeFriendList = activeUserList.stream().map( friendProfileMap::get ).collect( Collectors.toList());

            ActivityItem activityItem = new ActivityItem();
            activityItem.setEvent( event );
            activityItem.setActiveFriendList( activeFriendList );
            activityItem.setActiveFriendCount( activeFriendList.size() );
            activityItem.setInvited( eventResponse.isInvited() );
            activityItem.setParticipant( eventResponse.isParticipant() );
            boolean isActiveInEvent = currActivity != null && currActivity.getEventId() == event.getId();
            activityItem.setActive( isActiveInEvent );

            List<ActivityItem> activityItemList = new ArrayList<>();
            activityItemList.add( activityItem );

            tagMap.merge( event.getTag(),activityItemList, ( old, curr) -> {
                old.addAll( curr );
                return  old;
            } );
        }

        // Create Feed Item list
        for ( String tag : tagMap.keySet() )
        {
            List<ActivityItem> activityItemList = tagMap.get( tag );

            ActivityFeedItem feedItem = new ActivityFeedItem();
            feedItem.setTag( tag );
            feedItem.setActivityItemList( activityItemList );

            int activeFriendCount = 0;

            for ( ActivityItem activityItem : activityItemList )
            {
                activeFriendCount += activityItem.getActiveFriendCount();
            }

            feedItem.setActiveFriendCount( activeFriendCount );

            feedItems.add( feedItem );
        }

        return feedItems;
    }

    public List<FeedItem> getEventsCreatedByFriends( List<Integer> friendIdList, int userId, Connection conn )
    {
        List<FeedItem> feedItems = new ArrayList<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " + EVENT_SELECT +
                "WHERE " +
                "e.is_confirmed = FALSE " +
                "AND ( e.creator_id IN ( " );

        String delim = " ";

        for ( int eventUserId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " )" );
        sqlSb.append( " OR e.id IN ( SELECT event_id FROM event_interested ei WHERE ei.user_id IN ( " );

        delim = " ";

        for ( int eventUserId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " )  ) )" );
        sqlSb.append( " ORDER BY e.timestamp DESC" );

        try ( PreparedStatement ps = conn.prepareStatement( sqlSb.toString() ) )
        {

            ps.setFetchSize( 1000 );

            int count = 1;

            for ( int eventUserId : friendIdList )
            {
                ps.setInt( count++, eventUserId );
            }

            // For events friends are interested in
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
//                        boolean isConfirmed = rs.getBoolean( col++ );

                    FeedItem feedItem = new FeedItem();

                    Map<Integer, EventInterest> eventInterestMap = getEventInterested( eventId, conn );

                    Event event = new Event();
                    event.load( rs );
                    event.setInterestedCount( eventInterestMap.size() );

                    boolean isInterested = eventInterestMap.containsKey( userId );

                    feedItem.setEvent( event );
                    feedItem.setInterested( isInterested );

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

    public List<FeedItem> getConfirmedEventsByFriends( List<Integer> friendIdList, int userId, Connection conn )
    {
        List<FeedItem> feedItemList = new ArrayList<>();

        StringBuilder sqlSb = new StringBuilder( "SELECT " +
               CONFIRMED_EVENT_SELECT +
                "WHERE " +
                "ce.is_happening = FALSE " +
                "AND ce.creator_id IN ( " );

        String delim = " ";

        for ( int eventUserId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " )" );
        sqlSb.append( " OR ce.id IN ( SELECT event_id FROM event_participant ep WHERE ep.user_id IN ( " );

        delim = " ";

        for ( int eventUserId : friendIdList )
        {
            sqlSb.append( delim );
            sqlSb.append( "?" );
            delim = ", ";
        }

        sqlSb.append( " )  )" );
        sqlSb.append( " ORDER BY ce.timestamp DESC" );
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

                    Map<Integer, EventParticipant> participantMap = getEventParticipants( eventId, conn );

                    boolean isInvited = participantMap.containsKey( userId );
                    boolean isParticipating = false;

                    if ( isInvited )
                    {
                        isParticipating = participantMap.get( userId ).isConfirmed();
                    }

                    ConfirmedEvent event = new ConfirmedEvent();

                    if ( isPublic || isInvited )
                    {
                        event.load( rs );
                        event.setCreatorDisplayName( participantMap.get( event.creatorId ).getUser().getDisplayName() );
                        event.setParticipantCount( participantMap.size() );
                    }
                    else
                    {
                        event.loadPrivateEvent( rs );
                        event.setParticipantCount( participantMap.size() );
                    }

                    FeedItem feedItem = new FeedItem();
                    feedItem.setConfirmedEvent( event );
                    feedItem.setInvited( isInvited );
                    feedItem.setParticipant( isParticipating );

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
        // Add new interested user to the event

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

    private EventResponse getFeedItem( List<Integer> friendIdList, int userId, Connection conn, ResultSet rs ) throws SQLException
    {
        long eventId = rs.getLong( "id" );

        // Load interested friend profiles
        Map<Integer, EventInterest> eventInterestMap = getEventInterested( eventId, conn );

        List<Integer> interestedFriendIdList = new ArrayList<>();

        for ( Integer interestedUserId : eventInterestMap.keySet() )
        {
            if ( friendIdList.contains( interestedUserId ) )
            {
                interestedFriendIdList.add( interestedUserId );
            }
        }

        Map<Integer, UserProfile> interestedFriendMap = getUserProfiles( interestedFriendIdList, conn );
        Map<Integer, UserLocation> interestedFriendLocationMap = getUserLocation( interestedFriendIdList, conn );
        Map<Integer, List<Integer>> visibilityMap = getVisibilityMatrix( eventId, conn );
        List<Integer> requestedFriendList = getVisibilityRequestedByUser( eventId, userId );

        List<Integer> visibleUserList = new ArrayList<>();

        if ( visibilityMap.containsKey( userId ) )
        {
            visibleUserList = visibilityMap.get( userId );
        }

        //Load current user location
        final UserLocation currUserLocation = getUserLocation( Arrays.asList( userId ), conn ).get( userId );

        List<InterestedFriend> interestedFriendList = new ArrayList<>();

        for ( Integer friendUserId : interestedFriendMap.keySet() )
        {
            UserProfile friendProfile = interestedFriendMap.get( friendUserId );
            UserLocation friendLocation = interestedFriendLocationMap.get( friendUserId );
            EventInterest eventInterest = eventInterestMap.get( friendUserId );

            double distanceInMeters = LocationHelper.distance( currUserLocation.getLatitude(), friendLocation.getLatitude(), currUserLocation.getLongitude(), friendLocation.getLongitude(), 0.0, 0.0 );
            System.out.println( friendProfile.getDisplayName() + " : " + distanceInMeters );

            String distance;
            if ( distanceInMeters <= 1000 )
            {
                distance = ( int ) Math.floor( distanceInMeters ) + " m";
            }
            else
            {
                double distanceInKm = distanceInMeters / 1000;
                distance = ( int ) Math.floor( distanceInKm ) + " km";
            }

            // Remove identifying details from non-visible friends

            if ( !visibleUserList.contains( friendProfile.getUserId() ) )
            {
                friendProfile.setDisplayName( null );
            }

            InterestedFriend interestedFriend = new InterestedFriend();
            interestedFriend.setUser( friendProfile );
            interestedFriend.setDescription( eventInterest.getDescription() );
            interestedFriend.setDistance( distance );

            if ( requestedFriendList.contains( friendProfile.getUserId() ) )
            {
                interestedFriend.setVisibilityRequested( true );
            }

            interestedFriendList.add( interestedFriend );


        }

        boolean isInterested = eventInterestMap.containsKey( userId );

        EventResponse eventResponse = new EventResponse();

        Event event = new Event();
        event.load( rs );
        event.setInterestedCount( eventInterestMap.size() );

        String name = null;
        if ( interestedFriendMap.containsKey( event.getCreatorId() ) )
        {
            name = interestedFriendMap.get( event.getCreatorId() ).getDisplayName();
        }

        event.setCreatorDisplayName( name );
        eventResponse.setEvent( event );

        eventResponse.setInterested( isInterested );
        eventResponse.setInterestedFriendList( interestedFriendList );
        return eventResponse;
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

    public UserProfile getUserProfileById( int userId )
    {
        try ( Connection conn = DriverManager.getConnection( DB_URL, DB_USER, DB_PASS ) )
        {
            return getUserProfileById( userId, conn );
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
        }

        return new UserProfile(-1, null, null, null);
    }

    public UserProfile getUserProfileById( int userId, Connection conn )
    {
        UserProfile userProfile = null;

        try ( PreparedStatement ps = conn.prepareStatement( "SELECT u.id, u.facebook_id, u.firebase_uid, u.name, u.latitude, u.longitude FROM user u WHERE u.id = ? " ) )
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

        if ( userIdList.isEmpty() )
        {
            return userProfileList;
        }

        StringBuilder friendSql = new StringBuilder( "SELECT u.id, u.name FROM user u WHERE u.id IN ( " );

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

    public Map<Integer, UserLocation> getUserLocation( List<Integer> userIdList, Connection conn )
    {
        Map<Integer, UserLocation> userLocationMap = new HashMap<>();

        if ( userIdList.isEmpty() )
        {
            return userLocationMap;
        }

        StringBuilder friendSql = new StringBuilder( "SELECT u.id, u.latitude, u.longitude FROM user u WHERE u.id IN ( " );

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
