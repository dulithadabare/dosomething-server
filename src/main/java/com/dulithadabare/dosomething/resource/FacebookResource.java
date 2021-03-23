package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.facebook.Friend;
import com.dulithadabare.dosomething.facebook.FriendsResponse;
import com.dulithadabare.dosomething.model.BasicResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class FacebookResource
{
    public Map<String, String> getFacebookFriends( String facebookId, String facebookUserToken )
    {
        Map<String, String> facebookFriendList = new HashMap<>();
        HttpClient httpClient = HttpClient.newBuilder()
                .version( HttpClient.Version.HTTP_1_1 )
                .connectTimeout( Duration.ofSeconds( 10 ) )
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri( URI.create( "https://graph.facebook.com/v10.0/" + facebookId + "/friends?access_token=" + facebookUserToken ) )
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
            }
        }
        catch ( IOException | InterruptedException e )
        {
            e.printStackTrace();
        }

        return facebookFriendList;
    }
}
