package com.joanzap.minim.processors.utils;

import javax.annotation.processing.Messager;

import static javax.tools.Diagnostic.Kind.NOTE;

public class Logger {

    private Messager messager;

    public Logger(Messager messager) {
        this.messager = messager;
    }

    public void note(String message) {
        messager.printMessage(NOTE, message);
    }
}
