package com.dulithadabare.dosomething.resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component("FirebaseCloudMessaging")
public class FirebaseCloudMessaging
{
    public FirebaseCloudMessaging()
    {
    }

    public static void init()
    {
        String encodedString = System.getenv( "GOOGLE_APPLICATION_CREDENTIALS_BASE64" );
//        System.out.println(encodedString);
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        InputStream targetStream = new ByteArrayInputStream(decodedBytes);
        String decodedString = new String(decodedBytes);
//        System.out.println(decodedString);

        try
        {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials( GoogleCredentials.fromStream(targetStream))
                    .build();
            FirebaseApp.initializeApp(options);
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    public void sendMessage( List<String> registrationTokens ) throws FirebaseMessagingException
    {
        MulticastMessage message = MulticastMessage.builder()
                .setNotification( Notification.builder()
                        .setTitle( "A friend liked your idea" )
                        .setBody( "Pop name" )
                        .build() )
                .putData("score", "850")
                .putData("time", "2:45")
                .addAllTokens(registrationTokens)
                .build();

        BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();
            List<String> failedTokens = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    // The order of responses corresponds to the order of the registration tokens.
                    failedTokens.add(registrationTokens.get(i));
                }
            }

            System.out.println("List of tokens that caused failures: " + failedTokens);
        }

        System.out.println(response.getSuccessCount() + " messages were sent successfully");
    }

    public void sendMultiMessage( MulticastMessage message, List<String> registrationTokens ) throws FirebaseMessagingException
    {
        BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();
            List<String> failedTokens = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    // The order of responses corresponds to the order of the registration tokens.
                    failedTokens.add(registrationTokens.get(i));
                }
            }

            System.out.println("List of tokens that caused failures: " + failedTokens);
        }

        System.out.println(response.getSuccessCount() + " messages were sent successfully");
    }

    public static void main( String[] args )
    {
//        String encodedString = "ewogICJ0eXBlIjogInNlcnZpY2VfYWNjb3VudCIsCiAgInByb2plY3RfaWQiOiAiZG9zb21ldGhpbmctOTAxZWIiLAogICJwcml2YXRlX2tleV9pZCI6ICIyODgyNWY5NWRlMzU2YjVjYWJjOGM5YzZlZmI0OWUzMTAwOWY4YTg1IiwKICAicHJpdmF0ZV9rZXkiOiAiLS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tXG5NSUlFdlFJQkFEQU5CZ2txaGtpRzl3MEJBUUVGQUFTQ0JLY3dnZ1NqQWdFQUFvSUJBUUNySStwS2ZrUlJyQUxVXG5yYzFhVzhKd0tMYS8wUUYyT2NxYlRxSkxUM3FGUzdOU2R4K25DOVNkVXBVT1kzeWJsWTl2M2pLYjBsSlpveU5WXG5Vd0RWNDcrTCtVNHNTQ0VHUnMyWlVmTjcrTlJ3QURjUUxldVNWcUtnMEIvdGlwTWcrQTVMakNpOExJZUwzbFNxXG5UdkhXQkFsVU51UzhjVEhDLzd2MEowTHpiSVI3SjNYclUvQ2pIVWNScS9MWDdKbDA0RzNRSDdhUW5tektXN2ZXXG5iQnhDU0dyLzNyQ1g0azZQQzM5dllSblJDNWg0VWYxeE05S0M1SHo4RlQ2dTQ5bG1aRGtTRW5rVTZ6VFlSSDdhXG43bnZQbStUajZ0SGV0RE1BVzhhMmM2R2kySlNpQWxWVjVXamQ4b2xBaXdPRlFtbUsrdmlGd2J3QkFRdWY4Q0dLXG5hQ0FaQ0JXQkFnTUJBQUVDZ2dFQUErdlVXQWliQUNEYk9Wa1NIRUN5eGpQb25BZTJ1QStNaGFwemtrNHI0dU9UXG5pcWo0RXhmQmdDbExiMm5EVnIxQ2huelpwQUJacXE5UDBsY083UXJlMTk3OXFTUjZ2VHJnZVgwK1o3NWVNdWtuXG44MnlOSUd4d2krQlNnVnJTckMzcE9TT0NsUWJHa1JhbU9GMkxmcE1LczYyckd6WTVpMEkvTHA2SHJKUW92b1prXG43YUNLbHhNSkFCaVNBeWJnZHI4b3FHWEVva2U1eUpaVW9zNzhWU3hGMi9rQnE0NU9uZFE5THVudXRkb05hM3kvXG5qdmVyUTFhV1dsR0dYMmd5K2ZIZFRiblMxZG1YUGNOdnc5WUFCYXlUelhWL2JIRHBBZG0yYmpIUFhmNllWaDBoXG4rRTVsWUkzbk9oR0E2WFMybGdnVXZpMUNxMStZdjlLRTZML3pab3d3VVFLQmdRRGQxY1RjcGU2UHdqV2hTUTNQXG5hNG0xV1NMeXk5VFZUNVk0dkt6aFVrRTdyYm5KMHJBQ1pjZkkxcndGS2Fvb2hRZEkyRTZDRnBSTDIzb0FRNW85XG5rSk8vL0xUR1JxMXBKcWNLQ2F6c3pBUm9GVERWSXJyMzhLMjNIRFZYM2s3SkR2Wk85Z3BycnJBYVk3S3dlcHRCXG5VNnc2OTQ4c0Q1aFQwdGdYQ2VUSENiMWVrd0tCZ1FERmYyc0kzdVpxOGVlMDVWTXhqUVJ4bm1ZbkwrMVFnU21JXG5kQm1xVnVYTVpkZEtHZVd2czN2djR5Q0I3L0gvQ0EveTFxeTkrYlJyNUhyd1B4R09ZR0NnYnRsT2V0WDBjbzUzXG5RcEQxNS9CdTlUMHRvOGFvSEN4TzV2MFo3YzFScU1GRkkrczBkdGx6cEI2YUFSbjlEd2NTcjRzMDNUOFpSL2U2XG5rU1JsZEVuMEd3S0JnQ1RyUGM4bkFuUUVjMTU3UlFRZk5wVHExOGttcVM0ekI2STdyRjMzalltOEdPNkExUEFoXG5Oa1d6anlrZ3pRUGZWOTRFdWRRbW85bVlGazdTOFdtTGxUdGZlRXFEV3Jya1J1cUJ2N2pOSDNLZkREaDhoamRiXG5ZOTNVb1FBVnJIRUR2M1JXdUZkQmhHZHY3ajQydVgzK3lld3FaVGFGQmVuMWJpY3ZhMWJBeFRabkFvR0JBS1R2XG53S2ZyMzJxNUU5VVRIK05sN2F3bHFSSlpDRXdQVW14TzZaQkVlaTIxYjVMSEJmY3lZZkw1TUJEVUhybnFOY2VNXG5XVDllNXJxa1diZUNORnMvU2ZNeUgvRld6SkFWaTcwMXJuSnZmWEJ0QnVheXZCbHhUZCtuTVFFU2hFSUtPVDVRXG52UUZYc0VyUGFXOGNOR1FPeE0xcUpVdkU2Z2xtL1JDbVhrc01UeWE1QW9HQVhZcmc2VHRJRnE2M055bDFGVUdIXG5EdXFDMks2bUtMTitzS3FkUGFzamsvaDdGVEpYaG50czJlcXp2K21QL0hld3lOVUErNWhUTEx1YWN1bXVpQjJzXG5EaGtNeWlLV1pSVW5va3VzckFCam53VGRzWU5iSnoyWCtncUZGWTlFZjZPekV5QnlxTGxObFRPd3gzK01rbVR2XG56VnB5TU12Um9RMkdwbVF0ZzRjYkpHQT1cbi0tLS0tRU5EIFBSSVZBVEUgS0VZLS0tLS1cbiIsCiAgImNsaWVudF9lbWFpbCI6ICJmaXJlYmFzZS1hZG1pbnNkay04cTZkZkBkb3NvbWV0aGluZy05MDFlYi5pYW0uZ3NlcnZpY2VhY2NvdW50LmNvbSIsCiAgImNsaWVudF9pZCI6ICIxMDAxMzQ4NDIxNzM0MjMwMzEyNTkiLAogICJhdXRoX3VyaSI6ICJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20vby9vYXV0aDIvYXV0aCIsCiAgInRva2VuX3VyaSI6ICJodHRwczovL29hdXRoMi5nb29nbGVhcGlzLmNvbS90b2tlbiIsCiAgImF1dGhfcHJvdmlkZXJfeDUwOV9jZXJ0X3VybCI6ICJodHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9vYXV0aDIvdjEvY2VydHMiLAogICJjbGllbnRfeDUwOV9jZXJ0X3VybCI6ICJodHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9yb2JvdC92MS9tZXRhZGF0YS94NTA5L2ZpcmViYXNlLWFkbWluc2RrLThxNmRmJTQwZG9zb21ldGhpbmctOTAxZWIuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iCn0K";
//
//        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
//        String decodedString = new String(decodedBytes);
//        System.out.println(decodedString);
//        getConnection();
    }
}
