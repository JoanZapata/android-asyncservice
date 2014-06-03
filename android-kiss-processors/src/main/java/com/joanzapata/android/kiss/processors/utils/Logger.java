/**
 * Copyright 2014 Joan Zapata
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.joanzapata.android.kiss.processors.utils;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;

import static javax.tools.Diagnostic.Kind.*;

public class Logger {

    private Messager messager;

    public Logger(Messager messager) {
        this.messager = messager;
    }

    public void note(String message) {
        messager.printMessage(NOTE, message);
    }

    public void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);

        // Just in case the build doesn't stop with the instruction above
        throw new IllegalArgumentException(message);
    }

    public void error(ExecutableElement element, AnnotationMirror annotationMirror, String annotationValue, String message) {
        messager.printMessage(ERROR, message, element, annotationMirror, Utils.getRawAnnotationValue(annotationMirror, annotationValue));

        // Just in case the build doesn't stop with the instruction above
        throw new IllegalArgumentException(message);
    }

    public void error(Element element, AnnotationMirror annotationMirror, String message) {
        messager.printMessage(ERROR, message, element, annotationMirror);

        // Just in case the build doesn't stop with the instruction above
        throw new IllegalArgumentException(message);
    }
}
