package source;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Mp3Grouper {
    private final Path sourceDir;
    private final Map<String, Set<Path>> groupedByArtist = new HashMap<>();
    private final Map<String, Set<Path>> groupedByGenre = new HashMap<>();
    private final Map<String, Set<Path>> groupedByYear = new HashMap<>();

    public Mp3Grouper(Path sourceDir) {
        this.sourceDir = sourceDir;
    }

    public void groupFiles() {
        try {
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".mp3"))
                    .forEach(this::processFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processFile(Path file) {
        Mp3Parser parser = new Mp3Parser();
        Metadata metadata = new Metadata();
        ContentHandler handler = new DefaultHandler();
        ParseContext parseCtx = new ParseContext();

        try {
            parser.parse(Files.newInputStream(file), handler, metadata, parseCtx);

            // For controlled logging, you can enable this debug flag
            boolean debug = false;
            if (debug) {
                for (String name : metadata.names()) {
                    System.out.println(name + ": " + metadata.get(name));
                }
            }

            String artist = getMetadataValue(metadata, "xmpDM:artist");
            String genre = getMetadataValue(metadata, "xmpDM:genre", "genre", "ID3:genre");
            String year = getMetadataValue(metadata, "xmpDM:releaseDate", "dcterms:created", "Year", "ID3:year");

            if (artist == null) artist = "Без артиста - Неизвестен";
            if (genre == null) genre = "Без жанра - Неизвестен";
            if (year == null) {
                year = "Без даты - Неизвестен";
            } else {
                year = year.split("-")[0]; // Use only the year part
            }

            groupedByArtist.computeIfAbsent(artist, k -> new HashSet<>()).add(file);
            groupedByGenre.computeIfAbsent(genre, k -> new HashSet<>()).add(file);
            groupedByYear.computeIfAbsent(year, k -> new HashSet<>()).add(file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMetadataValue(Metadata metadata, String... keys) {
        for (String key : keys) {
            String value = metadata.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    public Map<String, Set<Path>> getGroupedByArtist() {
        return groupedByArtist;
    }

    public Map<String, Set<Path>> getGroupedByGenre() {
        return groupedByGenre;
    }

    public Map<String, Set<Path>> getGroupedByYear() {
        return groupedByYear;
    }

//    public static void main(String[] args) {
//        Path mp3Dir = Paths.get("C:\\Users\\dimab\\Downloads\\Telegram Desktop\\mp3ex");
//        Mp3Grouper grouper = new Mp3Grouper(mp3Dir);
//        grouper.groupFiles();
//
//        System.out.println("Grouped by Artist: " + grouper.getGroupedByArtist());
//        System.out.println("Grouped by Genre: " + grouper.getGroupedByGenre());
//        System.out.println("Grouped by Year: " + grouper.getGroupedByYear());
//    }
}
