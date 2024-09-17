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
    static String objectName_0 = "";
    static Map<String, String> fieldsByType_0 = new HashMap<String, String>();
    static Map<String, String> relationByType_0 = new HashMap<String, String>();
    static List<String> addedFields_0 = new ArrayList<String>();
    static String objectName_1 = "";
    static Map<String, String> fieldsByType_1 = new HashMap<String, String>();
    static Map<String, String> relationByType_1 = new HashMap<String, String>();
    static List<String> addedFields_1 = new ArrayList<String>();
    static String objectName_2 = "";
    static Map<String, String> fieldsByType_2 = new HashMap<String, String>();
    static Map<String, String> relationByType_2 = new HashMap<String, String>();
    static List<String> addedFields_2 = new ArrayList<String>();
    static String objectName_3 = "";
    static Map<String, String> fieldsByType_3 = new HashMap<String, String>();
    static Map<String, String> relationByType_3 = new HashMap<String, String>();
    static List<String> addedFields_3 = new ArrayList<String>();
    static String objectName_4 = "";
    static Map<String, String> fieldsByType_4 = new HashMap<String, String>();
    static Map<String, String> relationByType_4 = new HashMap<String, String>();
    static List<String> addedFields_4 = new ArrayList<String>();
    static Set<String> addedFilters = new LinkedHashSet<>() {};
    static String selectedCondition = "AND";
    static Set<String> sortingFilters = new LinkedHashSet<String>() {};
    static String limit = "";

    static int queryLevel = 0;

    private static File getSchemaFolder() {
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

        return zipFolder;
    }

    private static String processLine(String line) {
        return line.replaceFirst("\\s*global\\s", "").replaceAll(".$","");
    }

    public static String[] getObjects() throws IOException {
        getSchemaFolder();
        List<String> sobjects = new ArrayList<String>();
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

    public static List<List<String>> getSobjectFields(String selectedObject, boolean isRoot, int buttonLevel) throws IOException {
        if (!isRoot) {
            String objectLabel = selectedObject;
            selectedObject = getRelationsMapByNumber(buttonLevel).get(selectedObject);
            queryLevel = buttonLevel + 1;
            setCurrentLevelObject(objectLabel);
        } else {
            setCurrentLevelObject(selectedObject);
        }

        String regex = "Schema/" + selectedObject + ".cls";
        Pattern pattern = Pattern.compile(regex);

        try (ZipFile zipFile = new ZipFile(zipFolder.getPath())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Map<String, String> fieldsMap = new HashMap<String, String>();
            Map<String, String> relationMap = new HashMap<String, String>();
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
            setCurrentLevelFieldMap(fieldsMap);
            setCurrentLevelRelationMap(relationMap);
        }

        List<String> fieldEntries = new ArrayList<String>();
        for (var entry : getCurrentLevelFieldMap().entrySet()) {
            fieldEntries.add(entry.getKey());
        }
        Collections.sort(fieldEntries);

        List<String> relationEntries = new ArrayList<String>();
        for (var entry : getCurrentLevelRelationsMap().entrySet()) {
            relationEntries.add(entry.getKey());
        }
        Collections.sort(relationEntries);

        List<List<String>> allEntries = new ArrayList<List<String>>();
        allEntries.add(fieldEntries);
        allEntries.add(relationEntries);
        return allEntries;
    }

    public static void processCheckbox(int checkboxLevel, boolean isChecked, String fieldName) {
        if (isChecked) {
            getAddedListByNumber(checkboxLevel).add(fieldName);
        } else {
            getAddedListByNumber(checkboxLevel).remove(fieldName);
        }

        BuilderWindow.addedFilters.removeAll();
        BuilderWindow.sortingFilters.removeAll();
        addedFilters.clear();
        sortingFilters.clear();
        writeQuery();
        updateComboBox();
    }

    public static void addFilter(String filter) {
        if (!addedFilters.contains(filter)) {
            JLabel addedFilter = createFilterLabel(filter);
            addedFilter.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    for (Iterator<String> it = addedFilters.iterator(); it.hasNext();) {
                        String next = it.next();
                        if (next.equals(addedFilter.getName())) {
                            it.remove();
                        }
                    }
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
                    for (Iterator<String> it = sortingFilters.iterator(); it.hasNext();) {
                        String next = it.next();
                        if (next.equals(addedFilter.getName())) {
                            it.remove();
                        }
                    }
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

    public static void updateComboBox() {
        List<String> fieldsForWhereClause = new ArrayList<String>();
        if (!addedFields_0.isEmpty()) {
            fieldsForWhereClause.addAll(addedFields_0);
        }
        if (!addedFields_1.isEmpty()) {
            for (String field : addedFields_1) {
                fieldsForWhereClause.add(objectName_1 + '.' + field);
            }
        }
        if (!addedFields_2.isEmpty()) {
            for (String field : addedFields_2) {
                fieldsForWhereClause.add(objectName_1 + '.' + objectName_2 + '.' + field);
            }
        }
        if (!addedFields_3.isEmpty()) {
            for (String field : addedFields_3) {
                fieldsForWhereClause.add(objectName_1 + '.' + objectName_2 + '.' + objectName_3 + '.' +field);
            }
        }
        if (!addedFields_4.isEmpty()) {
            for (String field : addedFields_4) {
                fieldsForWhereClause.add(objectName_1 + '.' + objectName_2 + '.' + objectName_3 + '.' + objectName_4 + '.' +field);
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
        if (!addedFields_0.isEmpty()) {
            for (String field : addedFields_0) {
                query.append(",\n\t")
                        .append(field);
            }
        }
        if (!addedFields_1.isEmpty()) {
            for (String field : addedFields_1) {
                query.append(",\n\t")
                        .append(objectName_1).append(".")
                        .append(field);
            }
        }
        if (!addedFields_2.isEmpty()) {
            for (String field : addedFields_2) {
                query.append(",\n\t")
                        .append(objectName_1).append(".")
                        .append(objectName_2).append(".")
                        .append(field);
            }
        }
        if (!addedFields_3.isEmpty()) {
            for (String field : addedFields_3) {
                query.append(",\n\t")
                        .append(objectName_1).append(".")
                        .append(objectName_2).append(".")
                        .append(objectName_3).append(".")
                        .append(field);
            }
        }
        if (!addedFields_4.isEmpty()) {
            for (String field : addedFields_4) {
                query.append(",\n\t")
                        .append(objectName_1).append(".")
                        .append(objectName_2).append(".")
                        .append(objectName_3).append(".")
                        .append(objectName_4).append(".")
                        .append(field);
            }
        }
        query.append("\nFROM ").append(objectName_0);
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
        if (!limit.equals("") && Integer.parseInt(limit) > 0) {
            query.append("\nLIMIT ").append(limit);
        }
        BuilderWindow.queryTextArea.setText(query.toString());
    }

    static public void clearFields(int clearLevel) {
        switch (clearLevel) {
            case 0:
                objectName_0 = "";
                fieldsByType_0.clear();
                relationByType_0.clear();
                addedFields_0.clear();
                break;
            case 1:
                objectName_1 = "";
                fieldsByType_1.clear();
                relationByType_1.clear();
                addedFields_1.clear();
                break;
            case 2:
                objectName_2 = "";
                fieldsByType_2.clear();
                relationByType_2.clear();
                addedFields_2.clear();
                break;
            case 3:
                objectName_3 = "";
                fieldsByType_3.clear();
                relationByType_3.clear();
                addedFields_3.clear();
                break;
            case 4:
                objectName_4 = "";
                fieldsByType_4.clear();
                relationByType_4.clear();
                addedFields_4.clear();
                break;
            default:
                objectName_0 = "";
                fieldsByType_0.clear();
                relationByType_0.clear();
                addedFields_0.clear();
                objectName_1 = "";
                fieldsByType_1.clear();
                relationByType_1.clear();
                addedFields_1.clear();
                objectName_2 = "";
                fieldsByType_2.clear();
                relationByType_2.clear();
                addedFields_2.clear();
                objectName_3 = "";
                fieldsByType_3.clear();
                relationByType_3.clear();
                addedFields_3.clear();
                objectName_4 = "";
                fieldsByType_4.clear();
                relationByType_4.clear();
                addedFields_4.clear();
                BuilderWindow.pickedFields.removeAllItems();
                BuilderWindow.addedFilters.removeAll();
                BuilderWindow.orderByFields.removeAllItems();
                BuilderWindow.sortingFilters.removeAll();
                addedFilters.clear();
                sortingFilters.clear();
                limit = "";
                BuilderWindow.queryTextArea.setText("");
                break;
        };
    }

    static public void setCurrentLevelObject(String objectName) {
        switch (queryLevel) {
            case 0:
                objectName_0 = objectName;
                break;
            case 1:
                objectName_1 = objectName;
                break;
            case 2:
                objectName_2 = objectName;
                break;
            case 3:
                objectName_3 = objectName;
                break;
            case 4:
                objectName_4 = objectName;
                break;
        };
    }

    static public String getCurrentLevelObject() {
        return switch (queryLevel) {
            case 0 -> objectName_0;
            case 1 -> objectName_1;
            case 2 -> objectName_2;
            case 3 -> objectName_3;
            case 4 -> objectName_4;
            default -> null;
        };
    }

    static public Map<String, String> getCurrentLevelFieldMap() {
        return switch (queryLevel) {
            case 0 -> fieldsByType_0;
            case 1 -> fieldsByType_1;
            case 2 -> fieldsByType_2;
            case 3 -> fieldsByType_3;
            case 4 -> fieldsByType_4;
            default -> null;
        };
    }

    static public void setCurrentLevelFieldMap(Map<String, String> currentLevelFieldMap) {
        switch (queryLevel) {
            case 0:
                fieldsByType_0 = currentLevelFieldMap;
                break;
            case 1:
                fieldsByType_1 = currentLevelFieldMap;
                break;
            case 2:
                fieldsByType_2 = currentLevelFieldMap;
                break;
            case 3:
                fieldsByType_3 = currentLevelFieldMap;
                break;
            case 4:
                fieldsByType_4 = currentLevelFieldMap;
                break;
        };
    }

    static public Map<String, String> getCurrentLevelRelationsMap() {
        return switch (queryLevel) {
            case 0 -> relationByType_0;
            case 1 -> relationByType_1;
            case 2 -> relationByType_2;
            case 3 -> relationByType_3;
            case 4 -> relationByType_4;
            default -> null;
        };
    }

    static public Map<String, String> getRelationsMapByNumber(int number) {
        return switch (number) {
            case 0 -> relationByType_0;
            case 1 -> relationByType_1;
            case 2 -> relationByType_2;
            case 3 -> relationByType_3;
            case 4 -> relationByType_4;
            default -> null;
        };
    }

    static public void setCurrentLevelRelationMap(Map<String, String> currentLevelRelationMap) {
        switch (queryLevel) {
            case 0:
                relationByType_0 = currentLevelRelationMap;
                break;
            case 1:
                relationByType_1 = currentLevelRelationMap;
                break;
            case 2:
                relationByType_2 = currentLevelRelationMap;
                break;
            case 3:
                relationByType_3 = currentLevelRelationMap;
                break;
            case 4:
                relationByType_4 = currentLevelRelationMap;
                break;
        };
    }

    static public List<String> getAddedListByNumber(int number) {
        return switch (number) {
            case 0 -> addedFields_0;
            case 1 -> addedFields_1;
            case 2 -> addedFields_2;
            case 3 -> addedFields_3;
            case 4 -> addedFields_4;
            default -> null;
        };
    }
}
