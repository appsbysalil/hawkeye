package com.salilvnair.intellij.hawkeye.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.salilvnair.intellij.hawkeye.model.ResultEntry;
import com.salilvnair.intellij.hawkeye.service.JarScannerService;
import com.salilvnair.intellij.hawkeye.settings.HawkeyeSettings;
import com.salilvnair.intellij.hawkeye.ui.icon.HawkeyeIcons;
import com.salilvnair.intellij.hawkeye.ui.utils.WrapLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HawkeyeToolWindow {
    private final JPanel contentPanel;
    private final JBTextField searchField;
    private final JBTable resultTable;
    private final DefaultTableModel tableModel;
    private String classpath;

    /**
     * Constructor for the Pomerian Tool Window.
     */

    public HawkeyeToolWindow(Project project) {
        contentPanel = new JPanel(new BorderLayout());

        // Logo Panel
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JLabel logoLabel = new JLabel(HawkeyeIcons.HawkeyeLogo);
        logoPanel.add(logoLabel);


        // Top panel (Settings + Search Panel stacked)
        JPanel topWrapperPanel = new JPanel();
        topWrapperPanel.setLayout(new BoxLayout(topWrapperPanel, BoxLayout.Y_AXIS));


        // Settings Icon Panel (top-most)
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        JButton settingsButton = new JButton();
        settingsButton.setIcon(AllIcons.General.Settings);
        settingsButton.setToolTipText("Settings");
        settingsButton.setPreferredSize(new Dimension(24, 24));
        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setFocusPainted(false);
        settingsPanel.add(settingsButton);
        topWrapperPanel.add(logoPanel);
        topWrapperPanel.add(settingsPanel);

        // Search Panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(JBUI.Borders.empty(5, 10));
        searchField = new JBTextField();
        JButton searchButton = new JButton("Search");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        topWrapperPanel.add(searchPanel);

        // Add top wrapper to main content panel
        contentPanel.add(topWrapperPanel, BorderLayout.NORTH);

        // Table setup
        tableModel = new DefaultTableModel(new String[]{"JAR Name", "File", "Line"}, 0);
        resultTable = new JBTable(tableModel);
        contentPanel.add(new JBScrollPane(resultTable), BorderLayout.CENTER);

        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = resultTable.getSelectedRow();
                if (row >= 0) {
                    String jarName = (String) tableModel.getValueAt(row, 0);
                    String filePath = (String) tableModel.getValueAt(row, 1);
                    int lineNumber = (Integer) tableModel.getValueAt(row, 2);
                    showDecompiledClassPopup(project, jarName, filePath, lineNumber);
                }
            }
        });

        searchButton.addActionListener(e -> {
            String query = searchField.getText();
            if (query == null || query.isBlank()) {
                Messages.showErrorDialog("Please enter a search string.", "Pomerian");
                return;
            }
            tableModel.setRowCount(0);
            new Task.Backgroundable(project, "Searching dependencies") {
                public void run(@NotNull ProgressIndicator indicator) {
                    classpath = getProjectClasspath(project);
                    if (classpath == null || classpath.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog("Could not resolve project classpath.", "Pomerian"));
                        return;
                    }
                    ApplicationManager.getApplication().invokeLater(() -> {
//                        List<ResultEntry> results = JarScannerService.searchInProjectDependencies(project, query, classpath);
//                        if (results.isEmpty()) {
//                            Messages.showInfoMessage("No matches found.", "Pomerian");
//                        } else {
//                            for (ResultEntry entry : results) {
//                                tableModel.addRow(new Object[]{ entry.jarName, entry.fileName, entry.lineNumber });
//                            }
//                        }
                        new Task.Backgroundable(project, "Searching dependencies") {
                            List<ResultEntry> results;

                            public void run(@NotNull ProgressIndicator indicator) {
                                classpath = getProjectClasspath(project);
                                if (classpath == null || classpath.isEmpty()) {
                                    ApplicationManager.getApplication().invokeLater(() ->
                                            Messages.showErrorDialog("Could not resolve project classpath.", "Hawkeye"));
                                    return;
                                }
                                results = JarScannerService.searchInProjectDependencies(project, query, classpath);
                            }

                            @Override
                            public void onSuccess() {
                                if (results == null || results.isEmpty()) {
                                    Messages.showInfoMessage("No matches found.", "Hawkeye");
                                } else {
                                    tableModel.setRowCount(0);
                                    for (ResultEntry entry : results) {
                                        tableModel.addRow(new Object[]{ entry.jarName, entry.fileName, entry.lineNumber });
                                    }
                                }
                            }
                        }.queue();
                    });
                }
            }.queue();
        });

        settingsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                JPanel centerPanel = new JPanel(new GridLayout(3, 2, 10, 10));
                JTextField emojiField = new JTextField(HawkeyeSettings.getEmoji());
                JLabel lightColorLabel = new JLabel("  Light Theme Color");
                lightColorLabel.setOpaque(true);
                lightColorLabel.setBackground(HawkeyeSettings.getLightColor());
                JButton lightColorButton = new JButton("Pick Light Color");

                JLabel darkColorLabel = new JLabel("  Dark Theme Color");
                darkColorLabel.setOpaque(true);
                darkColorLabel.setBackground(HawkeyeSettings.getDarkColor());
                JButton darkColorButton = new JButton("Pick Dark Color");

                lightColorButton.addActionListener(c -> {
                    Color color = JColorChooser.showDialog(null, "Choose Light Theme Highlight Color", HawkeyeSettings.getLightColor());
                    if (color != null) {
                        HawkeyeSettings.setHighlightColors(color, HawkeyeSettings.getDarkColor());
                        lightColorLabel.setBackground(color);
                    }
                });

                darkColorButton.addActionListener(c -> {
                    Color color = JColorChooser.showDialog(null, "Choose Dark Theme Highlight Color", HawkeyeSettings.getDarkColor());
                    if (color != null) {
                        HawkeyeSettings.setHighlightColors(HawkeyeSettings.getLightColor(), color);
                        darkColorLabel.setBackground(color);
                    }
                });

//                panel.add(new JLabel("Highlight Emoji:"));
//                panel.add(emojiField);
//                panel.add(lightColorLabel);
//                panel.add(lightColorButton);
//                panel.add(darkColorLabel);
//                panel.add(darkColorButton);

                final Runnable[] refreshTags = new Runnable[1]; // holder to allow recursive reference
                JPanel tagPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
                JPanel tagActionPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 0, 4));
                tagPanel.setBorder(BorderFactory.createTitledBorder("Included File Types"));

                List<String> extensions = new ArrayList<>(HawkeyeSettings.getIncludedFileExtensions());
                JTextField tagField = new JTextField(10);
                JButton addTagButton = new JButton("Add");

                refreshTags[0] = () -> {
                    tagPanel.removeAll();
                    for (String ext : extensions) {
                        JPanel tag = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                        tag.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        JLabel tagLabel = new JLabel(ext);
                        JButton removeButton = new JButton();
                        removeButton.setIcon(AllIcons.General.Close);
                        removeButton.setToolTipText("Settings");
                        removeButton.setPreferredSize(new Dimension(24, 24));
                        removeButton.setBorderPainted(false);
                        removeButton.setContentAreaFilled(false);
                        removeButton.setFocusPainted(false);
                        removeButton.addActionListener(ev -> {
                            extensions.remove(ext);
                            refreshTags[0].run();
                        });
                        tag.add(tagLabel);
                        tag.add(removeButton);
                        tagPanel.add(tag);
                    }
                    tagPanel.revalidate();
                    tagPanel.repaint();
                };

                addTagButton.addActionListener(ae -> {
                    String ext = tagField.getText().trim();
                    if (!ext.isEmpty() && !extensions.contains(ext)) {
                        extensions.add(ext);
                        tagField.setText("");
                        refreshTags[0].run();
                    }
                });

                refreshTags[0].run();

                tagActionPanel.add(tagField);
                tagActionPanel.add(addTagButton);

                JPanel tagPanelContainer = new JPanel();
                tagPanelContainer.setLayout(new BoxLayout(tagPanelContainer, BoxLayout.Y_AXIS));
                tagPanelContainer.add(tagPanel);
                tagPanelContainer.add(tagActionPanel);


                centerPanel.add(new JLabel("Highlight Emoji:"));
                centerPanel.add(emojiField);
                centerPanel.add(lightColorLabel);
                centerPanel.add(lightColorButton);
                centerPanel.add(darkColorLabel);
                centerPanel.add(darkColorButton);

                panel.add(centerPanel, BorderLayout.CENTER);
                panel.add(tagPanelContainer, BorderLayout.SOUTH);
                panel.setPreferredSize(new Dimension(450, 250));


                int result = JOptionPane.showConfirmDialog(null, panel, "Hawkeye Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, HawkeyeIcons.HawkeyeIcon48);
                if (result == JOptionPane.OK_OPTION) {
                    HawkeyeSettings.setEmoji(emojiField.getText().trim());
                }
            }
        });
    }

    /**
     * Retrieves the classpath for the current project by collecting all module dependencies.
     * @param project The current project.
     * @return A string representing the classpath, with paths separated by the system path separator.
     */
    private String getProjectClasspath(Project project) {
        String pathSeparator = File.pathSeparator;
        return Arrays.stream(ModuleManager.getInstance(project).getModules())
                .flatMap(module -> OrderEnumerator.orderEntries(module)
                        .recursively()
                        .runtimeOnly()
                        .classes()
                        .getPathsList()
                        .getPathList()
                        .stream())
                .distinct()
                .collect(Collectors.joining(pathSeparator));
    }
    /**
     * Finds the path of a JAR file in the classpath by its name.
     * @param jarName The name of the JAR file to find (e.g., "example.jar").
     * @return The full path to the JAR file, or null if not found.
     */
    private String findJarPathByName(String jarName) {
        return Arrays.stream(classpath.split(File.pathSeparator))
                .filter(path -> path.endsWith(jarName))
                .findFirst()
                .orElse(null);
    }


    /**
     * Opens a class file in the editor based on the JAR name and file path.
     * @param project The current project.
     * @param jarName The name of the JAR file.
     * @param filePath The path of the class file inside the JAR.
     */
    private void openClassInEditor(Project project, String jarName, String filePath) {
        try {
            String jarPath = findJarPathByName(jarName);
            if (jarPath == null) {
                Messages.showErrorDialog("Cannot find jar in classpath: " + jarName, "Hawkeye");
                return;
            }

            String url = "jar://" + jarPath + "!/" + filePath;
            VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(url);

            if (vFile != null) {
                FileEditorManager.getInstance(project).openFile(vFile, true);
            } else {
                Messages.showErrorDialog("Could not locate file inside jar: " + filePath, "Hawkeye");
            }

        } catch (Exception e) {
            Messages.showErrorDialog("Error opening class: " + e.getMessage(), "Hawkeye");
        }
    }

    /**
     * Shows a popup with the decompiled class content.
     * @param project The current project.
     * @param jarName The name of the JAR file.
     * @param filePath The path of the class file inside
     * the JAR.
     * @param lineNumber The line number to highlight in the decompiled source.
     */

    private void showDecompiledClassPopup(Project project, String jarName, String filePath, int lineNumber) {
        try {
            String jarPath = findJarPathByName(jarName);
            if (jarPath == null) {
                Messages.showErrorDialog("Jar not found: " + jarName, "Hawkeye");
                return;
            }

            String fileUrl = "jar://" + jarPath + "!/" + filePath;
            VirtualFile classFile = VirtualFileManager.getInstance().findFileByUrl(fileUrl);
            if (classFile == null) {
                Messages.showErrorDialog("File not found in JAR: " + filePath, "Hawkeye");
                return;
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(classFile);
            if (psiFile == null) {
                Messages.showErrorDialog("Could not decompile class file.", "Hawkeye");
                return;
            }

            String decompiledSource = psiFile.getText();

            showCodePopupWithLineNumbers(project, jarName + " → " + filePath, decompiledSource, lineNumber);

        }
        catch (Exception e) {
            Messages.showErrorDialog("Error showing decompiled class: " + e.getMessage(), "Hawkeye");
        }
    }

    /**
     * Shows a popup with the decompiled code, highlighting the specified line.
     * @param project The current project.
     * @param title The title of the popup.
     * @param content The decompiled code content.
     * @param matchLine The line number to highlight (1-based).
     */
    private void showCodePopupWithLineNumbers(Project project, String title, String content, int matchLine) {
        Document document = EditorFactory.getInstance().createDocument(content);
        System.out.println("Decompiled lines in document during preview: " + document.getLineCount());
        EditorEx editor = (EditorEx) EditorFactory.getInstance().createEditor(document, project);
        editor.setViewer(true);

        // Enable line numbers and basic formatting
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setFoldingOutlineShown(false);

        // Add line highlighter (highlight matched line)
        TextAttributes highlightAttr = new TextAttributes();
        highlightAttr.setBackgroundColor(HawkeyeSettings.getHighlightColor());
        markupHighlightLine(editor, matchLine - 1, highlightAttr);

        // Add gutter icon on matched line
        addEmojiGutterIcon(editor, matchLine - 1, "👉");

        // Scroll to matched line
//        editor.getScrollingModel().scrollTo(new LogicalPosition(matchLine - 1, 0), ScrollType.CENTER);

        ApplicationManager.getApplication().invokeLater(() -> {
            editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(matchLine - 1, 0));
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        });


        // Create closeable title bar
        JLabel closeLabel = new JLabel(AllIcons.Actions.Close);
        closeLabel.setToolTipText("Close");
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("  " + title), BorderLayout.WEST);
        header.add(closeLabel, BorderLayout.EAST);
        header.setBorder(JBUI.Borders.empty(5, 10));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(header, BorderLayout.NORTH);
        contentPanel.add(editor.getComponent(), BorderLayout.CENTER);
        contentPanel.setPreferredSize(new Dimension(950, 700));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(contentPanel, editor.getComponent())
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .setFocusable(true)
                .createPopup();

        closeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                popup.cancel();
                EditorFactory.getInstance().releaseEditor(editor);
            }
        });

        popup.showInFocusCenter();
    }

    /**
     * Highlights a specific line in the editor with the given attributes.
     * @param editor The editor instance.
     * @param line The line number to highlight (0-based).
     * @param attributes The text attributes for highlighting.
     */
    private void markupHighlightLine(Editor editor, int line, TextAttributes attributes) {
        MarkupModel markupModel = editor.getMarkupModel();
        Document doc = editor.getDocument();
        if (line >= doc.getLineCount()) return;

        int startOffset = doc.getLineStartOffset(line);
        markupModel.addLineHighlighter(line, HighlighterLayer.SELECTION - 1, attributes);
    }

    /**
     * Adds an emoji gutter icon to the specified line in the editor.
     * @param editor The editor instance.
     * @param line The line number to add the icon (0-based).
     * @param emoji The emoji to display.
     */
    private void addEmojiGutterIcon(Editor editor, int line, String emoji) {
        MarkupModel markupModel = editor.getMarkupModel();
        Document doc = editor.getDocument();
        if (line >= doc.getLineCount()) return;
        RangeHighlighter highlighter = markupModel.addLineHighlighter(line, HighlighterLayer.ADDITIONAL_SYNTAX, null);

        highlighter.setGutterIconRenderer(new GutterIconRenderer() {
            @Override
            public Icon getIcon() {
                return new Icon() {
                    public int getIconWidth() { return 16; }
                    public int getIconHeight() { return 16; }
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                        g.drawString(HawkeyeSettings.getEmoji(), x, y + 12);
                    }
                };
            }

            @Override
            public boolean equals(Object obj) { return false; }
            @Override
            public int hashCode() { return Objects.hash(line); }
        });
    }

    /**
     * Returns the main content panel of the tool window.
     * @return The content panel.
     */
    public JPanel getComponent() {
        return contentPanel;
    }
}