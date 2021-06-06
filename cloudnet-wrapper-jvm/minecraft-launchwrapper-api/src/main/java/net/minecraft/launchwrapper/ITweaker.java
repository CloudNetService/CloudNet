// Part of the Minecraft LaunchWrapper (https://github.com/Mojang/LegacyLauncher)
// CloudNet needs this class to be able to register a tweaker which corrects class loading done by the LaunchWrapper.

package net.minecraft.launchwrapper;

import java.io.File;
import java.util.List;

public interface ITweaker {

  void acceptOptions(List<String> args, File gameDir, final File assetsDir, String profile);

  void injectIntoClassLoader(LaunchClassLoader classLoader);

  String getLaunchTarget();

  String[] getLaunchArguments();

}
