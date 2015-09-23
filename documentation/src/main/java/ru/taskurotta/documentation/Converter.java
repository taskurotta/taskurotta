package ru.taskurotta.documentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * User: stukushin
 * Date: 23.09.2015
 * Time: 12:35
 */

public class Converter {

    private static final byte[] newLineBytes = "\r\n".getBytes();
    private static final String anchor = "<a name='%s'/>";

    static class MenuItem {
        private int level;
        private String anchor;
        private String caption;
        private List<MenuItem> menuItems = new ArrayList<>();

        public MenuItem(int level, String anchor, String caption) {
            this.level = level;
            this.anchor = anchor;
            this.caption = caption;
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        Path documentIndexPath = Paths.get(args[0]);
        Path targetPath = Paths.get(args[1]);

        Path basePath = documentIndexPath.getParent().getParent().getParent().getParent();
        final List<Path> paths = new ArrayList<>();
        try (BufferedReader bufferedReader = Files.newBufferedReader(documentIndexPath)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                paths.add(basePath.resolve(line.trim()));
            }
        }

        List<MenuItem> menuItems = prepareMenu(paths);
        String menu = createMenu(menuItems);

        prepare(targetPath);

        OpenOption[] openOptions = new OpenOption[] {StandardOpenOption.APPEND, StandardOpenOption.CREATE};
        try (OutputStream outputStream = Files.newOutputStream(targetPath, openOptions)) {
            outputStream.write(menu.getBytes());
            outputStream.write(newLineBytes);
            outputStream.write(newLineBytes);

            for (Path path : paths) {
                outputStream.write(newLineBytes);
                String currentAnchor = String.format(anchor, path.toFile().getName());
                outputStream.write(currentAnchor.getBytes());
                outputStream.write(newLineBytes);
                outputStream.write(newLineBytes);
                Files.copy(path, outputStream);
            }
        }

    }

    private static void prepare(Path targetPath) throws IOException {
        Files.deleteIfExists(targetPath);
        Files.createDirectories(targetPath.getParent());
        Files.createFile(targetPath);
    }

    private static List<MenuItem> prepareMenu(List<Path> paths) throws IOException {
        List<MenuItem> menuItems = new ArrayList<>();

        for (Path path : paths) {
            BufferedReader bufferedReader = Files.newBufferedReader(path);
            do {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();

                if (line.startsWith("#")) {
                    int level = 0;
                    char[] chars = line.toCharArray();
                    for (char ch : chars) {
                        if (ch == '#') {
                            level++;
                        } else {
                            break;
                        }
                    }

                    String name = path.toFile().getName();
                    String caption = name.substring(0, name.lastIndexOf('.'));
                    menuItems.add(new MenuItem(level, name, caption));
                    break;
                }
            } while (true);
        }

        MenuItem root = createMenuTree(new MenuItem(0, null, null), menuItems);

        return root.menuItems;
    }

    private static MenuItem createMenuTree(MenuItem root, List<MenuItem> menuItems) {
        int topLevel = menuItems.get(0).level;
        List<MenuItem> subMenu = new ArrayList<>();
        for (MenuItem menuItem : menuItems) {
            int level = menuItem.level;

            if (level == topLevel) {
                if (!subMenu.isEmpty()) {
                    int index = root.menuItems.size() - 1;
                    MenuItem item = root.menuItems.get(index);
                    root.menuItems.set(index, createMenuTree(item, subMenu));
                    subMenu.clear();
                }
                root.menuItems.add(menuItem);
            } else {
                subMenu.add(menuItem);
            }
        }

        return root;
    }

    private static String createMenu(List<MenuItem> menuItems) {
        String menuItemTemplate = "<a href='#%s'>%s</a>";
        String menu = "<ol>";

        for (MenuItem menuItem : menuItems) {
            menu += "<li>";
            menu += String.format(menuItemTemplate, menuItem.anchor, menuItem.caption);
            List<MenuItem> subMenu = menuItem.menuItems;
            if (!subMenu.isEmpty()) {
                menu += createMenu(subMenu);
            }
            menu += "</li>\r\n";
        }

        menu += "</ol>\r\n";

        return menu;
    }

}
