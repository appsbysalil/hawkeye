package com.salilvnair.intellij.hawkeye.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.salilvnair.intellij.hawkeye.model.ResultEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarScannerService {
    public static void main(String[] args) {
        System.out.println(searchInM2("spring-boot"));
    }

    public static List<ResultEntry> searchInProjectDependencies(Project project, String query, String classpath) {
        List<ResultEntry> results = new ArrayList<>();
        String[] jars = classpath.split(File.pathSeparator);

        for (String jarPath : jars) {
            if (!jarPath.endsWith(".jar")) continue;
            try (JarFile jarFile = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || entry.getSize() == 0) continue;

                    String entryName = entry.getName();

                    // Handle .class files using IntelliJ decompiler
                    if (entryName.endsWith(".class")) {
                        String jarUrl = "jar://" + jarPath + "!/" + entryName;
                        VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(jarUrl);

                        if (vFile != null) {
                            PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
                            if (psiFile != null) {
                                String content = ApplicationManager.getApplication().runReadAction(
                                        (Computable<String>) psiFile::getText
                                );
                                String[] lines = content.split("\\n");
                                System.out.println(entryName + "----Lines in PSI content during search: " + lines.length);
                                for (int i = 0; i < lines.length; i++) {
                                    if (lines[i].contains(query)) {
                                        results.add(new ResultEntry(
                                                new File(jarPath).getName(),
                                                entryName,
                                                i + 1
                                        ));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // Handle regular text files (.java, .xml, .properties, etc.)
                    else {
                        try (InputStream is = jarFile.getInputStream(entry);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                            String line;
                            int lineNum = 1;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains(query)) {
                                    results.add(new ResultEntry(
                                            new File(jarPath).getName(),
                                            entryName,
                                            lineNum
                                    ));
                                    break;
                                }
                                lineNum++;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }

        return results;
    }

    public static List<String> searchInM2(String query) {
        List<String> results = new ArrayList<>();
        String m2Repo = System.getProperty("user.home") + "/.m2/repository";

        try (Stream<Path> paths = Files.walk(Paths.get(m2Repo))) {
            paths.filter(p -> p.toString().endsWith(".jar")).forEach(jarPath -> {
                try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().contains("pom.xml") || entry.getName().contains("pom.properties")) {
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                                if (content.contains(query)) {
                                    results.add(jarPath.toString());
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
}