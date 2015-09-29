package ru.taskurotta.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.pegdown.PegDownProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: stukushin
 * Date: 23.09.2015
 * Time: 12:35
 */

class Generator {

    private static final String anchor = "<a href=\"#%s\"></a>%n";
    private static final String newLine = System.getProperty("line.separator");

    public static void main(String[] args) throws IOException, URISyntaxException, TemplateException {
        Path basePath = Paths.get(args[0]);
        Path documentIndexPath = Paths.get(args[1]);
        Path targetPath = Paths.get(args[2]);
        String templateName = args[3];
        Path jsonMenuPath = Paths.get(args[4]);

        final List<Path> paths = new ArrayList<>();
        try (BufferedReader bufferedReader = Files.newBufferedReader(documentIndexPath)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                paths.add(basePath.resolve(line.trim()));
            }
        }

        List<MenuItem> menuItems = createMenu(paths);
        String markdownContent = createMarkdownContent(paths);

        String singleHtml = createSingleHtml(menuItems, markdownContent, templateName);
        saveSingleHtml(targetPath, singleHtml);

        saveMenuJson(jsonMenuPath, menuItems);
    }

    private static List<MenuItem> createMenu(List<Path> paths) throws IOException {
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
                    String caption = null;

                    char[] chars = line.toCharArray();
                    int length = chars.length;
                    for (int i = 0; i < length; i++) {
                        char ch = chars[i];

                        if (ch == '#') {
                            level++;
                        } else {
                            caption = new String(Arrays.copyOfRange(chars, i, length)).trim();
                            break;
                        }
                    }

                    String anchor = getAnchor(path);
                    menuItems.add(new MenuItem(level, anchor, caption));
                    break;
                }
            } while (true);
        }

        MenuItem root = createMenuTree(new MenuItem(0, null, null), menuItems);

        return root.getChildren();
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

    private static String createSingleHtml(List<MenuItem> menuItems, String markdownContent, String templateName) throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_22);
        configuration.setClassForTemplateLoading(Generator.class, "/templates");
        configuration.setDefaultEncoding("UTF-8");

        Template template = configuration.getTemplate(templateName);

        PegDownProcessor pegDownProcessor = new PegDownProcessor();
        String htmlContent = pegDownProcessor.markdownToHtml(markdownContent);

        Map<String, Object> model = new HashMap<>();
        model.put("menuItems", menuItems);
        model.put("content", htmlContent);

        String singleHtml;
        try (StringWriter stringWriter = new StringWriter()) {
            template.process(model, stringWriter);
            singleHtml = stringWriter.toString();
        }

        return singleHtml;
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
