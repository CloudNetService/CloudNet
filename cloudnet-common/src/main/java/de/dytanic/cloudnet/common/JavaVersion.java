package de.dytanic.cloudnet.common;

import java.util.Arrays;
import java.util.Optional;

public enum JavaVersion {
    JAVA_8(8, 52D, "Java 8"),
    JAVA_9(9, 53D, "Java 9"),
    JAVA_10(10, 54D, "Java 10"),
    JAVA_11(11, 55D, "Java 11"),
    JAVA_12(12, 56D, "Java 12"),
    JAVA_13(13, 57D, "Java 13"),
    JAVA_14(14, 58D, "Java 14");

    private final int version;
    private final double versionId;
    private final String name;

    JavaVersion(int version, double versionId, String name) {
        this.version = version;
        this.versionId = versionId;
        this.name = name;
    }

    public int getVersion() {
        return this.version;
    }

    public double getVersionId() {
        return this.versionId;
    }

    public String getName() {
        return this.name;
    }

    public boolean isSupported(JavaVersion minJavaVersion, JavaVersion maxJavaVersion) {
        return this.versionId >= minJavaVersion.versionId && this.versionId <= maxJavaVersion.versionId;
    }

    public boolean isSupportedByMin(JavaVersion minRequiredJavaVersion) {
        return this.versionId >= minRequiredJavaVersion.versionId;
    }

    public boolean isSupportedByMax(JavaVersion maxRequiredJavaVersion) {
        return this.versionId <= maxRequiredJavaVersion.versionId;
    }

    public static JavaVersion getRuntimeVersion() {
        double versionId = Double.parseDouble(System.getProperty("java.class.version"));
        return fromVersionId(versionId).orElseThrow(() -> new IllegalStateException("Running on an unsupported java version " + versionId));
    }

    public static Optional<JavaVersion> fromVersionId(double versionId) {
        return Arrays.stream(values()).filter(javaVersion -> javaVersion.versionId == versionId).findFirst();
    }

    public static Optional<JavaVersion> fromVersion(int version) {
        return Arrays.stream(values()).filter(javaVersion -> javaVersion.version == version).findFirst();
    }

}
