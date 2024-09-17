import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.WrapLayout;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

public class BuilderWindow {
    private JPanel builderContent = new JPanel();
    private static JPanel fieldsCheckboxPanel;
    public static JTextArea queryTextArea;
    public static ComboBox<String> pickedFields;
    public static ComboBox<String> orderByFields;
    public static JPanel addedFilters;
    public static JPanel sortingFilters;
    public static JButton andButton = new JButton("AND");
    public static JButton orButton = new JButton("OR");

    public BuilderWindow() throws IOException {
        builderContent.setLayout(new GridLayout(1, 2, 15, 0));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Object"));

        ComboBox<String> objectPicklist = new ComboBox<>(BuilderWindowLogic.getObjects());
        objectPicklist.setMaximumSize(new Dimension(500, 30));
        leftPanel.add(objectPicklist, BorderLayout.NORTH);

        fieldsCheckboxPanel = new JPanel();
        fieldsCheckboxPanel.setLayout(new BoxLayout(fieldsCheckboxPanel, BoxLayout.X_AXIS));

        JScrollPane selectionPane = new JBScrollPane(fieldsCheckboxPanel);
        selectionPane.setBorder(BorderFactory.createTitledBorder("Available Fields"));
        leftPanel.add(selectionPane, BorderLayout.CENTER);

        builderContent.add(leftPanel);

        JPanel conditionsAndQueryPanel = new JPanel();
        conditionsAndQueryPanel.setLayout(new GridLayout(1, 2, 15, 0));

        JPanel conditionsPanel = new JPanel(new BorderLayout());
        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        conditionsPanel.setBorder(BorderFactory.createTitledBorder("Filters"));
        conditionsPanel.setMaximumSize(conditionsPanel.getPreferredSize());

        JPanel fieldSelectorPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        fieldSelectorPanel.add(new JLabel("Field"), gbc);
        gbc.weightx = 1;
        gbc.gridx = 1;
        gbc.gridy = 0;
        fieldSelectorPanel.add(pickedFields = new ComboBox<>(), gbc);

        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 1;
        fieldSelectorPanel.add(new JLabel("Operator"), gbc);
        gbc.weightx = 1;
        gbc.gridx = 1;
        gbc.gridy = 1;
        ComboBox<String> operators = new ComboBox<>(new String[]{"=", "=:", "!=", "!=:", ">", "<", ">=", "<="
                ,"in", "not in", "like", "includes", "excludes", "starts with", "ends with", "contains"});
        fieldSelectorPanel.add(operators, gbc);

        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 2;
        fieldSelectorPanel.add(new JLabel("Value"), gbc);
        gbc.weightx = 1;
        gbc.gridx = 1;
        gbc.gridy = 2;
        JTextField valueField = new JTextField();
        fieldSelectorPanel.add(valueField, gbc);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc1 = new GridBagConstraints();

        gbc1.gridx = 0;
        gbc1.gridy = 0;
        andButton.setEnabled(false);
        andButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BuilderWindowLogic.selectedCondition = "AND";
                orButton.setEnabled(true);
                andButton.setEnabled(false);
            }
        });
        buttonsPanel.add(andButton, gbc1);

        gbc1.gridx = 1;
        gbc1.gridy = 0;
        orButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BuilderWindowLogic.selectedCondition = "OR";
                orButton.setEnabled(false);
                andButton.setEnabled(true);
            }
        });
        buttonsPanel.add(orButton, gbc1);

        gbc1.gridx = 2;
        gbc1.gridy = 0;
        JButton addFilterButton = new JButton("Add");
        addFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!valueField.getText().isEmpty()) {
                    String filter = pickedFields.getItem() + " " + operators.getItem() + ' ' + valueField.getText();
                    BuilderWindowLogic.addFilter(filter);
                }
            }
        });
        buttonsPanel.add(addFilterButton, gbc1);

        addedFilters = new JPanel(new WrapLayout());

        JPanel orderByPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        gbc2.weightx = 0;
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        orderByPanel.add(new JLabel("Order By"), gbc2);
        gbc2.weightx = 1;
        gbc2.gridx = 1;
        gbc2.gridy = 0;
        orderByPanel.add(orderByFields = new ComboBox<>(), gbc2);
        gbc2.weightx = 1;
        gbc2.gridx = 2;
        gbc2.gridy = 0;
        ComboBox<String> sortingOptions = new ComboBox<String>(new String[]{"ASC", "DESC"});
        orderByPanel.add(sortingOptions, gbc2);
        gbc2.weightx = 1;
        gbc2.gridx = 3;
        gbc2.gridy = 0;
        ComboBox<String> nullsOptions = new ComboBox<String>(new String[]{"NULLS...", "NULLS FIRST", "NULLS LAST"});
        orderByPanel.add(nullsOptions, gbc2);
        gbc1.gridx = 4;
        gbc1.gridy = 0;
        JButton addOrderByButton = new JButton("Add");
        addOrderByButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filter = orderByFields.getItem() + " " + sortingOptions.getItem() + ' ';
                if (!nullsOptions.getItem().equals("NULLS...")) {
                    filter += nullsOptions.getItem();
                }
                BuilderWindowLogic.addSortingFilter(filter);
            }
        });
        orderByPanel.add(addOrderByButton);

        sortingFilters = new JPanel(new WrapLayout());

        JPanel limitPanel = new JPanel();
        limitPanel.add(new JLabel("Limit"));
        JTextField limitInput = new JTextField();
        limitInput.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateLimit();
            }
            public void removeUpdate(DocumentEvent e) {
                updateLimit();
            }
            public void insertUpdate(DocumentEvent e) {
                updateLimit();
            }
            public void updateLimit() {
                if (!BuilderWindowLogic.objectName_0.equals("")) {
                    BuilderWindowLogic.handleLimitUpdate(limitInput.getText());
                }
            }
        });
        limitPanel.add(limitInput);

        conditionsPanel.add(fieldSelectorPanel);
        conditionsPanel.add(buttonsPanel);
        conditionsPanel.add(addedFilters);
        conditionsPanel.add(orderByPanel);
        conditionsPanel.add(sortingFilters);
        conditionsPanel.add(limitPanel);
        conditionsAndQueryPanel.add(conditionsPanel);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Query"));
        queryTextArea = new JTextArea();
        queryTextArea.setLineWrap(true);
        queryTextArea.setWrapStyleWord(true);
        queryTextArea.setEditable(false);
        JScrollPane queryScrollPane = new JBScrollPane(queryTextArea);
        rightPanel.add(queryScrollPane, BorderLayout.CENTER);
        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection(queryTextArea.getText());
                Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard ();
                clp.setContents (stringSelection, null);
            }
        });
        rightPanel.add(copyButton, BorderLayout.PAGE_END);
        conditionsAndQueryPanel.add(rightPanel);
        builderContent.add(conditionsAndQueryPanel);

        objectPicklist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedObject = objectPicklist.getSelectedItem().toString();
                List<List<String>> fields = null;
                BuilderWindowLogic.clearFields(5);
                try {
                    fields = BuilderWindowLogic.getSobjectFields(selectedObject, true, 0);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                fieldsCheckboxPanel.removeAll();
                BuilderWindowLogic.queryLevel = 0;
                queryTextArea.setText("");
                buildSelectionArea(fields);
            }
        });
    }

    public JPanel getContent() {
        return builderContent;
    }

    private static void buildSelectionArea(List<List<String>> fields) {
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setName(String.valueOf(BuilderWindowLogic.queryLevel));
        for (int i = 0; i < fields.get(0).size(); i++) {
            JCheckBox checkBox = new JCheckBox(fields.get(0).get(i));
            checkBox.setName(String.valueOf(BuilderWindowLogic.queryLevel));
            checkBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BuilderWindowLogic.processCheckbox(Integer.parseInt(checkBox.getName()), checkBox.isSelected(), checkBox.getText());
                }
            });
            fieldsPanel.add(checkBox);
        }
        fieldsPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        if (fields.size() > 1 && BuilderWindowLogic.queryLevel < 4) {
            for (int i = 0; i < fields.get(1).size(); i++) {
                String objectName = fields.get(1).get(i);
                JButton parentButton = new JButton(objectName + " >");
                parentButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                parentButton.setName(String.valueOf(BuilderWindowLogic.queryLevel));
                parentButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (Component c : fieldsPanel.getComponents()) {
                            if (c.getName() != null && c.getName().equals(parentButton.getName())) {
                                c.setEnabled(true);
                            }
                        }
                        parentButton.setEnabled(false);
                        int buttonLevel = Integer.parseInt(parentButton.getName());
                        List<List<String>> parentFields = null;
                        for (Component c : fieldsCheckboxPanel.getComponents()) {
                            if (Integer.parseInt(c.getName()) > buttonLevel) {
                                fieldsCheckboxPanel.remove(c);
                                BuilderWindowLogic.clearFields(Integer.parseInt(c.getName()));
                            }
                        }
                        BuilderWindowLogic.writeQuery();
                        try {
                            parentFields = BuilderWindowLogic.getSobjectFields(objectName, false, buttonLevel);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        buildSelectionArea(parentFields);
                    }
                });
                fieldsPanel.add(parentButton, BorderLayout.NORTH);
            }
            fieldsPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        }
        JBScrollPane scrollPane = new JBScrollPane(fieldsPanel);
        scrollPane.setPreferredSize(new Dimension(150, 100));
        scrollPane.setName(fieldsPanel.getName());
        fieldsCheckboxPanel.add(scrollPane, BorderLayout.NORTH);
        fieldsCheckboxPanel.revalidate();
        fieldsCheckboxPanel.repaint();
    }
}