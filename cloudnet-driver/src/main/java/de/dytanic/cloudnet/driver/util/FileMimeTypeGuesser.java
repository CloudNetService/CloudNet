package de.dytanic.cloudnet.driver.util;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class FileMimeTypeGuesser {

    // From Mozilla's Common MIME type list: https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
    private static final Map<String, String> FILE_TYPE_MAP = ImmutableMap.<String, String>builder()
            .put("aac", "audio/aac")
            .put("abw", "application/x-abiword")
            .put("arc", "application/x-freearc")
            .put("avi", "video/x-msvideo")
            .put("azw", "application/vnd.amazon.ebook")
            .put("bin", "application/octet-stream")
            .put("bmp", "image/bmp")
            .put("bz", "application/x-bzip")
            .put("bz2", "application/x-bzip2")
            .put("csh", "application/x-csh")
            .put("css", "text/css")
            .put("csv", "text/csv")
            .put("doc", "application/msword")
            .put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .put("eot", "application/vnd.ms-fontobject")
            .put("epub", "application/epub+zip")
            .put("gz", "application/gzip")
            .put("gif", "image/gif")
            .put("htm", "text/html")
            .put("html", "text/html")
            .put("ico", "image/vnd.microsoft.icon")
            .put("ics", "text/calendar")
            .put("jar", "application/java-archive")
            .put("jpeg", "image/jpeg")
            .put("jpg", "image/jpeg")
            .put("js", "text/javascript")
            .put("json", "application/json")
            .put("jsonld", "application/ld+json")
            .put("mid", "audio/midi")
            .put("midi", "audio/midi")
            .put("mjs", "text/javascript")
            .put("mp3", "audio/mpeg")
            .put("mpeg", "video/mpeg")
            .put("mpkg", "application/vnd.apple.installer+xml")
            .put("odp", "application/vnd.oasis.opendocument.presentation")
            .put("ods", "application/vnd.oasis.opendocument.spreadsheet")
            .put("odt", "application/vnd.oasis.opendocument.text")
            .put("oga", "audio/ogg")
            .put("ogv", "video/ogg")
            .put("ogx", "application/ogg")
            .put("opus", "audio/opus")
            .put("otf", "font/otf")
            .put("png", "image/png")
            .put("pdf", "application/pdf")
            .put("php", "application/x-httpd-php")
            .put("ppt", "application/vnd.ms-powerpoint")
            .put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
            .put("rar", "application/vnd.rar")
            .put("rtf", "application/rtf")
            .put("sh", "application/x-sh")
            .put("svg", "image/svg+xml")
            .put("swf", "application/x-shockwave-flasht")
            .put("tar", "application/x-tar")
            .put("tif", "image/tiff")
            .put("tiff", "image/tiff")
            .put("ts", "video/mp2t")
            .put("ttf", "font/ttf")
            .put("txt", "text/plain")
            .put("vsd", "application/vnd.visio")
            .put("wav", "audio/wav")
            .put("weba", "audio/webm")
            .put("webm", "video/webm")
            .put("webp", "image/webp")
            .put("woff", "font/woff")
            .put("woff2", "font/woff2")
            .put("xhtml", "application/xhtml+xml")
            .put("xls", "application/vnd.ms-excel")
            .put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .put("xml", "text/xml")
            .put("xul", "application/vnd.mozilla.xul+xml")
            .put("zip", "application/zip")
            .put("3gp", "video/3gpp")
            .put("3g2", "video/3gpp2")
            .put("7z", "application/x-7z-compressed")
            .build();

    private FileMimeTypeGuesser() {
        throw new UnsupportedOperationException();
    }

    public static String getFileType(String filePath) {
        String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
        // MIME type should be "application/octet-stream" if the file type is unknown
        return FILE_TYPE_MAP.getOrDefault(fileExtension, "application/octet-stream");
    }

    public static String getFileType(File file) {
        return getFileType(file.getName());
    }

    public static String getFileType(Path path) {
        return getFileType(path.toString());
    }


}
