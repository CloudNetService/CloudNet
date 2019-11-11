package de.dytanic.cloudnet.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.console.ConsoleColor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class CreditsUtil {

    private static final String PIXEL_STRING = "▀ ";
    private static final String BLOCK_BETWEEN_IMAGES = "   ";

    public static final LogLevel LOG_LEVEL = new LogLevel("credits", "CREDITS", 1, false);

    private static JsonElement parseJsonFromURL(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.connect();
        try (InputStream inputStream = connection.getInputStream();
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static Collection<GitHubContributor> getContributors(String owner, String repo) throws IOException {
        return new Gson().fromJson(parseJsonFromURL("https://api.github.com/repos/" + owner + "/" + repo + "/contributors"), TypeToken.getParameterized(Collection.class, GitHubContributor.class).getType());
    }

    private static void getDataFromFile(BiConsumer<Pair<String, String>, Integer> acceptor) {
        Properties properties = new Properties();
        try (InputStream inputStream = CreditsUtil.class.getClassLoader().getResourceAsStream("files/gitHubContributor.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int topListSize = Integer.parseInt(properties.getProperty("topListSize", "3"));
        String repoOwner = properties.getProperty("repoOwner", "CloudNetService");
        String repo = properties.getProperty("repo", "CloudNet-v3");

        acceptor.accept(new Pair<>(repoOwner, repo), topListSize);
    }

    private static void getTopContributors(String repoOwner, String repo, int topListSize, BiConsumer<Collection<GitHubContributor>, Collection<GitHubContributor>> acceptor) throws IOException {
        List<GitHubContributor> contributors = getContributors(repoOwner, repo)
                .stream()
                .filter(gitHubContributor -> gitHubContributor.getType().equals("User"))
                .sorted(Collections.reverseOrder(Comparator.comparingLong(GitHubContributor::getContributions)))
                .collect(Collectors.toList());
        Collection<GitHubContributor> topContributors = contributors.subList(0, Math.min(contributors.size(), topListSize));
        acceptor.accept(contributors, topContributors);
    }

    public static void printContributorNames(ICommandSender sender, ILogger logger) {
        getDataFromFile((repoData, topListSize) -> {
            try {
                printContributorNames(sender, logger, topListSize, repoData.getFirst(), repoData.getSecond());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void printContributorImages(ICommandSender sender, ILogger logger) {
        getDataFromFile((repoData, topListSize) -> {
            try {
                printContributorImages(sender, logger, topListSize, repoData.getFirst(), repoData.getSecond());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void printContributorNames(ICommandSender sender, ILogger logger, int topListSize, String repoOwner, String repo) throws IOException {
        getTopContributors(repoOwner, repo, topListSize, (contributors, topContributors) -> {
            displayMessage(sender, logger, LanguageManager.getMessage("cloudnet-creator-display-name").replace("%name%", "Dytanic"));
            displayMessage(sender, logger, LanguageManager.getMessage("cloudnet-top-github-contributor-names")
                    .replace("%amount%", String.valueOf(topContributors.size()))
                    .replace("%names%", topContributors.stream().map(GitHubContributor::getName).collect(Collectors.joining(", ")))
            );
        });
    }

    public static void printContributorImages(ICommandSender sender, ILogger logger, int topListSize, String repoOwner, String repo) throws IOException {
        getTopContributors(repoOwner, repo, topListSize, (contributors, topContributors) -> {
            try {
                displayCreator(sender, logger);
                displayContributors(sender, logger, topContributors);
            } catch (IOException e) {
                e.printStackTrace();
            }

            displayMessage(sender, logger, " ",
                    LanguageManager.getMessage("cloudnet-github-contributor-info")
                            .replace("%amount%", String.valueOf(contributors.size()))
                            .replace("%contributions%", String.valueOf(contributors.stream().mapToLong(GitHubContributor::getContributions).sum()))
            );
        });
    }

    private static void displayCreator(ICommandSender sender, ILogger logger) throws IOException {
        displayMessage(sender, logger, LanguageManager.getMessage("cloudnet-creator-display-image"));

        List<String[]> images = new ArrayList<>(1);
        addImageToList(images, "Dytanic", CreditsUtil.class.getClassLoader().getResource("files/Dytanic.png"));

        displayImages(images, sender, logger);
    }

    private static void displayContributors(ICommandSender sender, ILogger logger, Collection<GitHubContributor> topContributors) throws IOException {
        displayMessage(sender, logger,
                LanguageManager.getMessage("cloudnet-top-github-contributor-images").replace("%amount%", String.valueOf(topContributors.size())),
                " "
        );

        List<String[]> images = new ArrayList<>(topContributors.size());
        for (GitHubContributor contributor : topContributors) {
            addImageToList(images, contributor.getName(), new URL(contributor.getAvatarURL()));
        }

        displayImages(images, sender, logger);
    }

    private static void displayImages(List<String[]> images, ICommandSender sender, ILogger logger) {
        StringBuilder builder = new StringBuilder();
        int length = images.stream().mapToInt(value -> value.length).min().orElse(0);
        for (int i = 0; i < length; i++) {
            for (String[] image : images) {
                builder.append(image[i]).append(BLOCK_BETWEEN_IMAGES);
            }
            builder.append('\n');
        }
        displayMessage(sender, logger, builder.toString().split("\n"));
    }

    private static void addImageToList(List<String[]> output, String username, URL avatarURL) throws IOException {
        BufferedImage avatar = new BufferedImage(32, 32, BufferedImage.TRANSLUCENT);
        BufferedImage fullAvatar = ImageIO.read(avatarURL);

        Graphics2D graphics2D = avatar.createGraphics();
        graphics2D.drawImage(fullAvatar, 0, 0, 32, 32, null);
        graphics2D.dispose();

        List<String> imageLines = new ArrayList<>(Arrays.asList(getImageAsString(avatar).split("\n")));
        StringBuilder builder = new StringBuilder("&f");
        int halfWithoutName = avatar.getWidth() / 2 - (username.length() / PIXEL_STRING.length()) + 2;
        for (int i = 0; i < halfWithoutName; i++) {
            builder.append(PIXEL_STRING);
        }
        builder.append(username);
        for (int i = 0; i < halfWithoutName; i++) {
            builder.append(PIXEL_STRING);
        }
        imageLines.add(0, builder.toString());
        output.add(imageLines.toArray(new String[0]));
    }

    private static String getImageAsString(BufferedImage image) {
        StringBuilder builder = new StringBuilder();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                Color color = rgb == 0 ? Color.WHITE : new Color(rgb);
                Arrays.stream(ConsoleColor.values()).min(Comparator.comparingInt(value ->
                        Math.abs(value.getColor().getRed() - color.getRed()) +
                                Math.abs(value.getColor().getGreen() - color.getGreen()) +
                                Math.abs(value.getColor().getBlue() - color.getBlue())
                )).ifPresent(nearestColor -> builder.append("&").append(nearestColor.getIndex()).append(PIXEL_STRING));
            }
            builder.append('\n');
        }
        return builder.substring(0, Math.max(0, builder.length() - 1));
    }

    private static void displayMessage(ICommandSender sender, ILogger logger, String... messages) {
        if (logger != null) {
            logger.log(LOG_LEVEL, messages);
        } else if (sender != null) {
            sender.sendMessage(messages);
        }
    }

}
