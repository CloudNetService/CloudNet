package de.dytanic.cloudnet.util;

import com.google.gson.annotations.SerializedName;

public class GitHubContributor {
    @SerializedName("login")
    private String name;
    private long id;
    @SerializedName("node_id")
    private String nodeId;
    @SerializedName("avatar_url")
    private String avatarURL;
    @SerializedName("gravatar_id")
    private String gravatarID;
    private String url;
    @SerializedName("html_url")
    private String htmlURL;
    @SerializedName("followers_url")
    private String followersURL;
    @SerializedName("following_url")
    private String followingURL;
    @SerializedName("gists_url")
    private String gistsURL;
    @SerializedName("starred_url")
    private String starredURL;
    @SerializedName("subscriptions_url")
    private String subscriptionsURL;
    @SerializedName("organizations_url")
    private String organizationsURL;
    @SerializedName("repos_url")
    private String reposURL;
    @SerializedName("events_url")
    private String eventsURL;
    @SerializedName("received_events_url")
    private String receivedEventsUrl;
    private String type;
    @SerializedName("site_admin")
    private boolean siteAdmin;
    private long contributions;

    public GitHubContributor(String name, long id, String nodeId, String avatarURL, String gravatarID, String url, String htmlURL,
                             String followersURL, String followingURL, String gistsURL, String starredURL, String subscriptionsURL,
                             String organizationsURL, String reposURL, String eventsURL, String receivedEventsUrl, String type,
                             boolean siteAdmin, long contributions) {
        this.name = name;
        this.id = id;
        this.nodeId = nodeId;
        this.avatarURL = avatarURL;
        this.gravatarID = gravatarID;
        this.url = url;
        this.htmlURL = htmlURL;
        this.followersURL = followersURL;
        this.followingURL = followingURL;
        this.gistsURL = gistsURL;
        this.starredURL = starredURL;
        this.subscriptionsURL = subscriptionsURL;
        this.organizationsURL = organizationsURL;
        this.reposURL = reposURL;
        this.eventsURL = eventsURL;
        this.receivedEventsUrl = receivedEventsUrl;
        this.type = type;
        this.siteAdmin = siteAdmin;
        this.contributions = contributions;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    public long getId() {
        return this.id;
    }

    public String getAvatarURL() {
        return this.avatarURL;
    }

    public String getFollowersURL() {
        return this.followersURL;
    }

    public String getFollowingURL() {
        return this.followingURL;
    }

    public String getGistsURL() {
        return this.gistsURL;
    }

    public String getGravatarID() {
        return this.gravatarID;
    }

    public String getHtmlURL() {
        return this.htmlURL;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getOrganizationsURL() {
        return this.organizationsURL;
    }

    public String getStarredURL() {
        return this.starredURL;
    }

    public String getType() {
        return this.type;
    }

    public long getContributions() {
        return this.contributions;
    }

    public String getEventsURL() {
        return this.eventsURL;
    }

    public String getSubscriptionsURL() {
        return this.subscriptionsURL;
    }

    public String getReceivedEventsUrl() {
        return this.receivedEventsUrl;
    }

    public String getReposURL() {
        return this.reposURL;
    }

    public boolean isSiteAdmin() {
        return this.siteAdmin;
    }
}
