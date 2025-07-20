package com.salilvnair.intellij.hawkeye.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.salilvnair.intellij.hawkeye.model.ResultEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarScannerService {

    private static final int THREAD_POOL_SIZE = 6;

    public static List<ResultEntry> searchInProjectDependencies(Project project, String query, String classpath) {
        List<ResultEntry> allResults = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();

        for (String jarPath : classpath.split(File.pathSeparator)) {
            if (!jarPath.endsWith(".jar")) continue;
            futures.add(executor.submit(() -> scanJar(project, jarPath, query, allResults)));
        }

        // Wait for all tasks to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception ignored) {}
        }

        executor.shutdown();
        return allResults;
    }

    private static void scanJar(Project project, String jarPath, String query, List<ResultEntry> resultCollector) {
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getSize() == 0) continue;

                String entryName = entry.getName();

                if (entryName.endsWith(".class")) {
                    String jarUrl = "jar://" + jarPath + "!/" + entryName;
                    VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(jarUrl);

                    if (vFile != null) {
                        PsiFile psiFile  = ApplicationManager.getApplication().runReadAction(
                                (com.intellij.openapi.util.Computable<PsiFile>) () ->
                                        PsiManager.getInstance(project).findFile(vFile)
                        );
                        if (psiFile != null) {
                            String content = ApplicationManager.getApplication().runReadAction(
                                    (Computable<String>) psiFile::getText
                            );
                            String[] lines = content.split("\\n");
                            for (int i = 0; i < lines.length; i++) {
                                if (lines[i].contains(query)) {
                                    resultCollector.add(new ResultEntry(
                                            new File(jarPath).getName(), entryName, i + 1
                                    ));
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    try (InputStream is = jarFile.getInputStream(entry);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        int lineNum = 1;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains(query)) {
                                resultCollector.add(new ResultEntry(
                                        new File(jarPath).getName(), entryName, lineNum
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }
}
