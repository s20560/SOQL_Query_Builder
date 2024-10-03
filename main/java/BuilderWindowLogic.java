import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BuilderWindowLogic {

    static File zipFolder;
    static Set<String> addedFilters = new LinkedHashSet<>() {};
    static String selectedCondition = "AND";
    static Set<String> sortingFilters = new LinkedHashSet<>() {};
    static String limit = "";
    static Map<Integer, SoqlObject> addedObjects = new TreeMap<>(){};
    static final int objectsTreeLimit = 5;
    static String[] operators = new String[]{"=", "=:", "!=", "!=:", ">", "<", ">=", "<="
            ,"in", "not in", "like", "includes", "excludes", "starts with", "ends with", "contains"};
    static String[] sortingDirections = new String[]{"ASC", "DESC"};
    static String[] nullsOptions = new String[]{"NULLS...", "NULLS FIRST", "NULLS LAST"};

    private static void getSchemaFolder() {
        String projectRoot = ProjectManager.getInstance().getOpenProjects()[0].getBasePath();
        try {
            File projectFolder = new File(projectRoot + "\\IlluminatedCloud");
            File connectionDirectory = new File(projectFolder.listFiles()[0].getPath());
            for (File file : connectionDirectory.listFiles()) {
                if (file.getName().equals("OfflineSymbolTable.zip")) {
                    zipFolder = file;
                }
            }
        } catch (Exception e) {
            System.out.println("Offline Symbol Table not found");
        }
    }

    private static String processLine(String line) {
        return line.replaceFirst("\\s*global\\s", "").replaceAll(".$","");
    }

    public static String[] getObjects() throws IOException {
        getSchemaFolder();
        List<String> sobjects = new ArrayList<>();
        String regex = "^Schema/";
        Pattern pattern = Pattern.compile(regex);

        try (ZipFile zipFile = new ZipFile(zipFolder.getPath())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Matcher matcher = pattern.matcher(entry.getName());
                if (matcher.find()) {
                    sobjects.add(entry.getName().replaceAll("Schema/", "").replaceAll(".cls", ""));
                }
            }
        }
        Collections.sort(sobjects);
        String[] sobjectArray = new String[sobjects.size()];
        return sobjects.toArray(sobjectArray);
    }

    public static SoqlObject getSobject(String selectedObject, boolean isRoot) throws IOException {
        SoqlObject soqlObject = new SoqlObject();
        if (!isRoot) {
            String objectLabel = selectedObject;
            selectedObject = addedObjects.get(addedObjects.size() - 1).relationByType.get(selectedObject);
            soqlObject.level = addedObjects.size();
            soqlObject.objectName = objectLabel;
        } else {
            soqlObject.level = 0;
            soqlObject.objectName = selectedObject;
        }

        String regex = "Schema/" + selectedObject + ".cls";
        Pattern pattern = Pattern.compile(regex);

        try (ZipFile zipFile = new ZipFile(zipFolder.getPath())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Map<String, String> fieldsMap = new HashMap<>();
            Map<String, String> relationMap = new HashMap<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Matcher matcher = pattern.matcher(entry.getName());
                if (matcher.find()) {
                    try (InputStream inputStream = zipFile.getInputStream(entry);
                         Scanner scanner = new Scanner(inputStream)) {
                        String fieldsRegex = "^\\s*global\\s+(Blob|Boolean|Date|Datetime|Decimal|Double|Id|Integer|Long|String)";
                        Pattern fieldsPattern = Pattern.compile(fieldsRegex);
                        String relationRegex = "^\\s*global\\s+(?!List<|static)(?!Blob|Boolean|Date|Datetime|Decimal|Double|Id|Integer|Long|String)[\\w<>]+\\s+\\w+";
                        Pattern relationPattern = Pattern.compile(relationRegex);
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            Matcher fieldsMatcher = fieldsPattern.matcher(line);
                            Matcher relationsMatcher = relationPattern.matcher(line);
                            if (fieldsMatcher.find()) {
                                String[] readyLine = processLine(line).split("\\s");
                                fieldsMap.put(readyLine[1], readyLine[0]);
                            } else if (relationsMatcher.find()) {
                                String[] readyLine = processLine(line).split("\\s");
                                relationMap.put(readyLine[1], readyLine[0]);
                            }
                        }
                    }
                    break;
                }
            }
            soqlObject.fieldsByType = fieldsMap;
            soqlObject.relationByType = relationMap;
        }
        addedObjects.put(soqlObject.level, soqlObject);
        return soqlObject;
    }

    public static void processCheckbox(int checkboxLevel, boolean isChecked, String fieldName) {
        if (isChecked) {
            addedObjects.get(checkboxLevel).addedFields.add(fieldName);
        } else {
            addedObjects.get(checkboxLevel).addedFields.remove(fieldName);
        }
        BuilderWindow.addedFilters.removeAll();
        BuilderWindow.sortingFilters.removeAll();
        addedFilters.clear();
        sortingFilters.clear();
        writeQuery();
        updateFiltersList();
    }

    public static void addFilter(String filter) {
        if (!addedFilters.contains(filter)) {
            JLabel addedFilter = createFilterLabel(filter);
            addedFilter.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    addedFilters.removeIf(next -> next.equals(addedFilter.getName()));
                    for (Component c : BuilderWindow.addedFilters.getComponents()) {
                        if (c.getName().equals(addedFilter.getName())) {
                            BuilderWindow.addedFilters.remove(c);
                        }
                    }
                    BuilderWindow.addedFilters.revalidate();
                    BuilderWindow.addedFilters.repaint();
                    writeQuery();
                }
            });
            BuilderWindow.addedFilters.add(addedFilter);
            BuilderWindow.addedFilters.revalidate();
            BuilderWindow.addedFilters.repaint();
        }
        addedFilters.add(filter);
        writeQuery();
    }

    public static void addSortingFilter(String filter) {
        if (!sortingFilters.contains(filter)) {
            JLabel addedFilter = createFilterLabel(filter);
            addedFilter.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    sortingFilters.removeIf(next -> next.equals(addedFilter.getName()));
                    for (Component c : BuilderWindow.sortingFilters.getComponents()) {
                        if (c.getName().equals(addedFilter.getName())) {
                            BuilderWindow.sortingFilters.remove(c);
                        }
                    }
                    BuilderWindow.sortingFilters.revalidate();
                    BuilderWindow.sortingFilters.repaint();
                    writeQuery();
                }
            });
            BuilderWindow.sortingFilters.add(addedFilter);
            BuilderWindow.sortingFilters.revalidate();
            BuilderWindow.sortingFilters.repaint();
        }
        sortingFilters.add(filter);
        writeQuery();
    }

    public static void handleLimitUpdate(String newLimit) {
        limit = newLimit;
        writeQuery();
    }

    public static JLabel createFilterLabel(String filter) {
        JLabel addedFilter = new JLabel("   " + filter + "      X   ");
        Border blackLine = BorderFactory.createLineBorder(JBColor.BLACK);
        addedFilter.setBorder(blackLine);
        addedFilter.setName(filter);
        return addedFilter;
    }

    public static void updateFiltersList() {
        List<String> fieldsForWhereClause = new ArrayList<>();
        for(var entry : addedObjects.entrySet()) {
            if(!entry.getValue().addedFields.isEmpty()) {
                String path = getObjectPath(entry.getValue().level);
                for(String field : entry.getValue().addedFields) {
                   fieldsForWhereClause.add(path + field);
                }
            }
        }

        BuilderWindow.pickedFields.removeAllItems();
        BuilderWindow.orderByFields.removeAllItems();
        BuilderWindow.pickedFields.addItem("Id");
        BuilderWindow.orderByFields.addItem("Id");
        for (String field : fieldsForWhereClause) {
            BuilderWindow.pickedFields.addItem(field);
            BuilderWindow.orderByFields.addItem(field);
        }
    }

    public static void writeQuery() {
        StringBuilder query = new StringBuilder("SELECT Id");
        for(var entry : addedObjects.entrySet()) {
            if(!entry.getValue().addedFields.isEmpty()) {
                String path = getObjectPath(entry.getValue().level);
                for (String field : entry.getValue().addedFields) {
                    query.append(",\n\t")
                            .append(path)
                            .append(field);
                }
            }
        }
        query.append("\nFROM ").append(addedObjects.get(0).objectName);
        if (!addedFilters.isEmpty()) {
            query.append("\nWHERE ");
            boolean isFirst = true;
            for (String filter : addedFilters) {
                if (isFirst) {
                    query.append(filter).append(" ");
                    isFirst = false;
                } else {
                    query.append("\n").append(selectedCondition).append(" ").append(filter).append(" ");
                }
            }
        }
        if (!sortingFilters.isEmpty()) {
            query.append("\nORDER BY ");
            boolean isFirst = true;
            for (String filter : sortingFilters) {
                if (isFirst) {
                    query.append(filter);
                    isFirst = false;
                } else {
                    query.append(", ").append(filter);
                }
            }
        }
        if (!limit.isEmpty() && Integer.parseInt(limit) > 0) {
            query.append("\nLIMIT ").append(limit);
        }
        BuilderWindow.queryTextArea.setText(query.toString());
    }

    static public void clearLevel(int clearLevel) {
        addedObjects.remove(clearLevel);
        clearComponents();
    }

    static void clearAll() {
        addedObjects.clear();
        clearComponents();
    }

    static private String getObjectPath(int level) {
        StringBuilder path = new StringBuilder();
        for (var entry : addedObjects.entrySet()) {
            if (entry.getKey() > 0 && entry.getKey() <= level) {
                path.append(entry.getValue().objectName).append(".");
            }
        }
        return path.toString();
    }

    static private void clearComponents() {
        BuilderWindow.pickedFields.removeAllItems();
        BuilderWindow.addedFilters.removeAll();
        BuilderWindow.orderByFields.removeAllItems();
        BuilderWindow.sortingFilters.removeAll();
        addedFilters.clear();
        sortingFilters.clear();
        limit = "";
        BuilderWindow.queryTextArea.setText("");
    }

}
