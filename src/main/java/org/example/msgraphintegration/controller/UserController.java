package org.example.msgraphintegration.controller;

import com.microsoft.graph.authentication.BaseAuthenticationProvider;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    // Helper method to set up Graph Client
    private GraphServiceClient<Request> getGraphClient(OAuth2AccessToken accessToken) {
        IAuthenticationProvider authProvider = request -> {
            return CompletableFuture.completedFuture("Bearer " + accessToken.getTokenValue());
        };

        return GraphServiceClient
                .builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }

    @GetMapping("/user")
    public String getUser(@RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient client) {
        OAuth2AccessToken accessToken = client.getAccessToken();
        GraphServiceClient<Request> graphClient = getGraphClient(accessToken);

        User user = graphClient.me()
                .buildRequest()
                .get();

        String username = user.userPrincipalName;
        String userDisplayName = user.displayName;

        return String.format("Username: %s, Name: %s", username, userDisplayName);
    }

    @GetMapping("/users")
    public List<String> getUsers(@RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient client) {
        GraphServiceClient graphClient = GraphServiceClient.builder()
                .authenticationProvider(new GraphAuthenticationProvider(client))
                .buildClient();

        List<String> users = graphClient
                .users()
                .buildRequest()
                .get()
                .getCurrentPage()
                .stream()
                .map(user -> user.displayName)
                .collect(Collectors.toList());

        return users;
    }

    private static class GraphAuthenticationProvider
            extends BaseAuthenticationProvider {

        private OAuth2AuthorizedClient graphAuthorizedClient;

        public GraphAuthenticationProvider(@Nonnull OAuth2AuthorizedClient graphAuthorizedClient) {
            this.graphAuthorizedClient = graphAuthorizedClient;
        }


        @Override
        public CompletableFuture<String> getAuthorizationTokenAsync(@Nonnull final URL requestUrl){
            return CompletableFuture.completedFuture(graphAuthorizedClient.getAccessToken().getTokenValue());
        }
    }
}

