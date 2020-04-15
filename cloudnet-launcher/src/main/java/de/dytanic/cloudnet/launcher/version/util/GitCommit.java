package de.dytanic.cloudnet.launcher.version.util;


import java.util.Date;

public class GitCommit {

    private static final String UNKNOWN_COMMIT_SHA = "unknown";

    public static final GitCommit UNKNOWN = new GitCommit(UNKNOWN_COMMIT_SHA, null, null);

    private String sha;
    private String message;
    private GitCommitAuthor author;

    public GitCommit(String sha, String message, GitCommitAuthor author) {
        this.sha = sha;
        this.message = message;
        this.author = author;
    }

    public GitCommit() {
    }

    public boolean isKnown() {
        return this.sha != null && !this.sha.equalsIgnoreCase(UNKNOWN_COMMIT_SHA);
    }

    public boolean hasInformation() {
        return this.sha != null && this.message != null && this.author != null;
    }

    public long getTime() {
        return this.author == null || this.author.date == null ? -1 : this.author.date.getTime();
    }

    public String getShortenedSha() {
        return this.sha.length() > 6 ? this.sha.substring(0, 7) : UNKNOWN_COMMIT_SHA;
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
