package de.dytanic.cloudnet.launcher.version.util;


import java.util.Date;

public class GitCommit {

    public GitCommit(String sha, String message, GitCommitAuthor author) {
        this.sha = sha;
        this.message = message;
        this.author = author;
    }

    private boolean known = true;

    private String sha;
    private String message;
    private GitCommitAuthor author;

    public GitCommit(String sha) {
        this.sha = sha;

        this.known = false;
    }

    public static GitCommit unknown() {
        return new GitCommit("unknown");
    }

    public GitCommit() {
    }

    public boolean isKnown() {
        return known;
    }

    public String getShortenedSha() {
        return this.sha.substring(0, 7);
    }

    public String getSha() {
        return sha;
    }

    public String getMessage() {
        return message;
    }

    public GitCommitAuthor getAuthor() {
        return author;
    }

    public static class GitCommitAuthor {
        private String name;
        private String email;
        private Date date;

        public GitCommitAuthor(String name, String email, Date date) {
            this.name = name;
            this.email = email;
            this.date = date;
        }

        public GitCommitAuthor() {
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public Date getDate() {
            return date;
        }

    }

}
