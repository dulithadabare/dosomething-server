package com.dulithadabare.dosomething.resource;

import com.dulithadabare.dosomething.facebook.Friend;
import com.dulithadabare.dosomething.facebook.FriendsResponse;
import com.dulithadabare.dosomething.facebook.PictureResponse;
import com.dulithadabare.dosomething.facebook.PublicProfile;
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
    public static void main( String[] args )
    {
        ObjectMapper mapper = new ObjectMapper().configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

        String responseBody = "{\"data\":{\"height\":50,\"is_silhouette\":true,\"url\":\"https:\\/\\/scontent.fcmb1-1.fna.fbcdn.net\\/v\\/t1.30497-1\\/cp0\\/c15.0.50.50a\\/p50x50\\/84628273_176159830277856_972693363922829312_n.jpg?_nc_cat=1&ccb=1-3&_nc_sid=12b3be&_nc_ohc=StVn30emtYcAX8-TVEx&_nc_ht=scontent.fcmb1-1.fna&tp=27&oh=2f136ec82f78918fdfbf399d0a40517a&oe=609A07B8\",\"width\":50}}";
        PictureResponse pictureResponse;
        try
        {
            pictureResponse = mapper.readValue( responseBody, PictureResponse.class );
            System.out.println( pictureResponse.getData().getUrl() );
        }
        catch ( JsonProcessingException e )
        {
            e.printStackTrace();
        }

    }

    public PublicProfile getPublicProfile( String facebookId, String facebookUserToken )
    {
        PublicProfile friendsResponse = null;

        HttpClient httpClient = HttpClient.newBuilder()
                .version( HttpClient.Version.HTTP_1_1 )
                .connectTimeout( Duration.ofSeconds( 10 ) )
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri( URI.create( "https://graph.facebook.com/v10.0/" + facebookId + "&access_token=" + facebookUserToken ) )
                .build();

        try
        {
            HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );

            ObjectMapper mapper = new ObjectMapper().configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
            try
            {
                friendsResponse = mapper.readValue( response.body(), PublicProfile.class );
                //TODO save name to database
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

        return friendsResponse;
    }
    public PictureResponse getProfilePicture( String facebookId, String facebookUserToken )
    {
        PictureResponse pic = null;
        HttpClient httpClient = HttpClient.newBuilder()
                .version( HttpClient.Version.HTTP_1_1 )
                .connectTimeout( Duration.ofSeconds( 10 ) )
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri( URI.create( "https://graph.facebook.com/v10.0/" + facebookId + "/picture?redirect=false&access_token=" + facebookUserToken ) )
                .build();

        try
        {
            HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );

            ObjectMapper mapper = new ObjectMapper().configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
            try
            {
                pic = mapper.readValue( response.body(), PictureResponse.class );
                //TODO downlaod image
                //TODO save image to Cloud Bucket
                //TODO save image URL to database
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

        return pic;
    }

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
