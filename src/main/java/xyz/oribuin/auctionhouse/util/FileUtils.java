package xyz.oribuin.auctionhouse.util;

import dev.rosewood.rosegarden.RosePlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class FileUtils {

    /**
     * Create a file from the plugin's resources
     *
     * @param rosePlugin The plugin
     * @param fileName   The file name
     * @return The file
     */
    @NotNull
    public static File createFile(@NotNull RosePlugin rosePlugin, @NotNull String fileName) {
        File file = new File(rosePlugin.getDataFolder(), fileName); // Create the file

        if (file.exists())
            return file;

        try (InputStream inStream = rosePlugin.getResource(fileName)) {
            if (inStream == null) {
                file.createNewFile();
                return file;
            }

            Files.copy(inStream, Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * Create a file in a folder from the plugin's resources
     *
     * @param rosePlugin The plugin
     * @param folderName The folder name
     * @param fileName   The file name
     * @return The file
     */
    @NotNull
    public static File createFile(@NotNull RosePlugin rosePlugin, @NotNull String folderName, @NotNull String fileName) {
        File folder = new File(rosePlugin.getDataFolder(), folderName); // Create the folder
        File file = new File(folder, fileName); // Create the file
        if (!folder.exists())
            folder.mkdirs();

        if (file.exists())
            return file;

        rosePlugin.getLogger().warning("File " + fileName + " does not exist in " + folderName + ", creating a new one.");

        try (InputStream stream = rosePlugin.getResource(folderName + "/" + fileName)) {
            if (stream == null) {
                file.createNewFile();
                return file;
            }

            Files.copy(stream, Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

}
