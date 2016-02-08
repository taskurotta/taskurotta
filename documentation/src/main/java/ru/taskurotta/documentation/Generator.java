package ru.taskurotta.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: stukushin
 * Date: 23.09.2015
 * Time: 12:35
 */

class Generator {

    private static final String anchor = "<a name=\"%s\"></a>";
    private static final String newLine = System.getProperty("line.separator");
    private static final char tabChar = (char) 9;
    private static final char whiteSpaceChar = (char) 32;

    public static void main(String[] args) throws IOException, URISyntaxException {
        Path basePath = Paths.get(args[0]);
        Path documentIndexPath = Paths.get(args[1]);
        Path targetPath = Paths.get(args[2]);
        Path jsonMenuPath = Paths.get(args[3]);

        List<String> strings = Files.readAllLines(documentIndexPath);
        List<Path> paths = getPaths(strings, basePath);

        List<MenuItem> menuItems = createMenu(strings, basePath);
        String markdownContent = createMarkdownContent(paths);

        String singleHtml = createSingleHtml(markdownContent);
        saveSingleHtml(targetPath, singleHtml);

        saveMenuJson(jsonMenuPath, menuItems);
    }

    private static List<MenuItem> createMenu(List<String> strings, Path basePath) throws IOException {
        List<MenuItem> menuItems = new ArrayList<>();

        for (String s : strings) {
            int level = getLevel(s);
            String location = s.trim();
            Path path = basePath.resolve(location);
            String caption = getCaption(path);
            String anchor = getAnchor(path);

            menuItems.add(new MenuItem(level, anchor, caption));
        }

        MenuItem root = createMenuTree(new MenuItem(0, null, null), menuItems);

        return root.getChildren();
    }

    private static int getLevel(final String s) {
        String preparedString = s.replace(tabChar, whiteSpaceChar);
        int bias = 0;
        int length = preparedString.length();
        for (int i = 0; i < length; i++) {
            char c = preparedString.charAt(i);
            if (c == whiteSpaceChar) {
                bias++;
            }
        }
        return bias;
    }

    private static String getCaption(Path path) throws IOException {
        String caption = null;

        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("#")) {
                    int length = line.length();
                    for (int i = 0; i < length; i++) {
                        if (line.charAt(i) == '#') {
                            continue;
                        }

                        caption = line.substring(i, length).trim();
                        break;
                    }
                    break;
                }
            }
        }

        return caption;
    }

    private static String getAnchor(Path path) {
        return path.toFile().getName();
    }

    private static MenuItem createMenuTree(MenuItem root, List<MenuItem> menuItems) {
        int topLevel = menuItems.get(0).getLevel();
        List<MenuItem> subMenu = new ArrayList<>();
        for (MenuItem menuItem : menuItems) {
            int level = menuItem.getLevel();

            if (level == topLevel) {
                if (!subMenu.isEmpty()) {
                    int index = root.getChildren().size() - 1;
                    MenuItem item = root.getChildren().get(index);
                    root.getChildren().set(index, createMenuTree(item, subMenu));
                    subMenu.clear();
                }
                root.getChildren().add(menuItem);
            } else {
                subMenu.add(menuItem);
            }
        }

        return root;
    }

    private static List<Path> getPaths(List<String> strings, Path basePath) {
        return strings.stream().map(s -> basePath.resolve(s.trim())).collect(Collectors.toList());
    }

    private static String createMarkdownContent(List<Path> paths) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        for (Path path : paths) {
            // add anchor
            stringBuilder.append(String.format(anchor, getAnchor(path)));
            stringBuilder.append(newLine);

            // add content
            stringBuilder.append(new String(Files.readAllBytes(path)));
            stringBuilder.append(newLine);
            stringBuilder.append(newLine);
        }

        return stringBuilder.toString();
    }

    private static String createSingleHtml(String markdownContent) throws IOException {
        return new PegDownProcessor(Extensions.TABLES).markdownToHtml(markdownContent);
    }

    private static void saveSingleHtml(Path targetPath, String singleHtml) throws IOException {
        prepareForSaving(targetPath);
        Files.write(targetPath, singleHtml.getBytes());
    }

    private static void saveMenuJson(Path menuJsonPath, List<MenuItem> menuItems) throws IOException {
        prepareForSaving(menuJsonPath);
        ObjectMapper mapper = new ObjectMapper();
        try (OutputStream outputStream = Files.newOutputStream(menuJsonPath)) {
            mapper.writeValue(outputStream, menuItems);
        }
    }

    private static void prepareForSaving(Path path) throws IOException {
        Files.createDirectories(path.getParent());
    }

}
