package de.dytanic.cloudnet.launcher.update;

public interface IUpdater {

    boolean init(String url);

    String getRepositoryVersion();

    String getCurrentVersion();

    boolean installUpdate(String destinationBaseDirectory, String moduleDestinationBaseDirectory);

}