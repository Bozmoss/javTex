package com.example.latex.compiler;

public class Compiler {
    private String source = "";

    public Compiler(String sourceString) {
        source = sourceString;
    }

    public void updateSource(String sourceString) {
        source = sourceString;
    }
}