package com.salilvnair.intellij.hawkeye.model;

public class ResultEntry {
    public final String jarName;
    public final String fileName;
    public final int lineNumber;

    public ResultEntry(String jarName, String fileName, int lineNumber) {
        this.jarName = jarName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }
}
