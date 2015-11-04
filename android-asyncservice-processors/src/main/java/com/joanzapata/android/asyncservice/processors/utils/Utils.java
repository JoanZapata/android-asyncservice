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
package com.joanzapata.android.asyncservice.processors.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.lang.model.element.Modifier.*;

public class Utils {

    /** Get the package name of an element */
    public static String getElementPackageName(Element minimServiceElement) {
        // Iterates until we found a package or a null parent to start with
        Element parent = minimServiceElement.getEnclosingElement();
        if (!isPackage(minimServiceElement) && parent != null) return getElementPackageName(parent);

        // If it's a package, return full name
        if (isPackage(minimServiceElement))
            return ((PackageElement) minimServiceElement).getQualifiedName().toString();

        // Else, it means the parent is null, return empty string
        return "";
    }

    /** Returns true if the given element is a package */
    public static boolean isPackage(Element elem) {
        return elem.getKind().equals(ElementKind.PACKAGE);
    }

    /** Returns true if the given element is a package */
    public static boolean isPublicOrProtectedMethod(Element elem) {
        return isMethod(elem) && !elem.getModifiers().contains(PRIVATE);
    }

    /**
     * Get the method parameters in the form of
     * "type1", "name1", "type2", "name2", etc...
     */
    public static List<String> formatParameters(ExecutableElement method, boolean useFinal) {
        List<String> out = new ArrayList<String>();
        for (VariableElement var : method.getParameters()) {
            out.add((useFinal ? "final " : "") + var.asType());
            out.add(var.getSimpleName().toString());
        }
        return out;
    }

    /**
     * Get the method parameters in the form of
     * "name1, name2", etc...
     */
    public static String formatParametersForCall(ExecutableElement method) {
        StringBuilder builder = new StringBuilder();
        for (VariableElement var : method.getParameters()) {
            builder.append(var.getSimpleName().toString()).append(",");
        }

        // Remove the last ,
        if (builder.length() > 0)
            return builder.substring(0, builder.length() - 1);
        return builder.toString();
    }

    /**
     * Get the method parameters in the form of
     * "{name1}, {name2}", etc...
     */
    public static String formatParametersForCacheKey(ExecutableElement method) {
        StringBuilder builder = new StringBuilder();
        for (VariableElement var : method.getParameters()) {
            builder.append("{").append(var.getSimpleName().toString()).append("},");
        }

        // Remove the last ,
        if (builder.length() > 0)
            return builder.substring(0, builder.length() - 1);
        return builder.toString();
    }

    /**
     * Finds all elements (fields, methods) annotated with the given annotation in the given class element.
     */
    public static List<Element> findElementsAnnotatedWith(TypeElement element, Class<? extends Annotation> annotationClass) {

        // Find elements
        List<Element> out = new ArrayList<Element>();
        for (Element enclosedElement : element.getEnclosedElements()) {
            for (AnnotationMirror annotationMirror : enclosedElement.getAnnotationMirrors()) {
                if (equals(annotationMirror, annotationClass))
                    out.add(enclosedElement);
            }
        }

        // Then do a recursive call on super class if any
        if (element.getSuperclass() != null && element.getSuperclass() instanceof DeclaredType) {
            Element superClassElement = ((DeclaredType) element.getSuperclass()).asElement();
            if (superClassElement instanceof TypeElement)
                out.addAll(findElementsAnnotatedWith((TypeElement) superClassElement, annotationClass));
        }

        return out;
    }

    /** Return true if the given annotation mirror equals the given annotation class */
    private static boolean equals(AnnotationMirror annotationMirror, Class<? extends Annotation> annotationClass) {
        return annotationMirror.getAnnotationType().toString().equals(annotationClass.getName());
    }

    public static AnnotationMirror getAnnotation(Element element, Class<? extends Annotation> annotationClass) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationClass.getName())) {
                return annotationMirror;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(AnnotationMirror annotationMirror, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return (T) entry.getValue().getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static AnnotationValue getRawAnnotationValue(AnnotationMirror annotationMirror, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * something({x}, {y}) => "something(" + x + ", " + y + ")"
     */
    public static String parseCacheKeyValue(String annotationCacheToParse) {
        return "\"" + annotationCacheToParse
                .replace("{", "\" + ")
                .replace("}", "+ \"") + "\"";
    }

    public static boolean isPublicOrProtectedField(Element element) {
        return isField(element) && !element.getModifiers().contains(PRIVATE);
    }

    public static boolean isField(Element element) {
        return element.getKind().equals(ElementKind.FIELD);
    }

    public static boolean isStatic(Element element) {
        return element.getModifiers().contains(STATIC);
    }

    public static boolean isMethod(Element element) {
        return element.getKind().equals(ElementKind.METHOD);
    }

    public static boolean isAnnotatedWith(ExecutableElement method, Class<? extends Annotation> annotationClass) {
        return getAnnotation(method, annotationClass) != null;
    }

    public static boolean isPublicOrPackagePrivate(Element element) {
        return isPublic(element) || isPackagePrivate(element);
    }

    private static boolean isPackagePrivate(Element element) {
        return !element.getModifiers().contains(PRIVATE) &&
                !element.getModifiers().contains(PUBLIC) &&
                !element.getModifiers().contains(PROTECTED);
    }

    private static boolean isPublic(Element element) {
        return element.getModifiers().contains(PUBLIC);
    }

    public static boolean implementsInterface(TypeElement minimServiceElement, Class<?> baseServiceClass) {
        for (TypeMirror typeMirror : minimServiceElement.getInterfaces()) {
            if (typeMirror.toString().equals(baseServiceClass.getCanonicalName())) {
                return true;
            }
        }
        return false;

    }

    public static boolean isConstructor(Element element) {
        return element.getKind().equals(ElementKind.CONSTRUCTOR);
    }

    public static boolean isAssignable(ProcessingEnvironment processingEnv, VariableElement assignmentRightPart, Class<?> assignmentLeftPart) {
        return isAssignable(processingEnv, assignmentRightPart.asType(), assignmentLeftPart);
    }

    public static boolean isAssignable(ProcessingEnvironment processingEnv, TypeMirror assignmentRightPart, Class<?> assignmentLeftPart) {
        return processingEnv.getTypeUtils().isAssignable(
                assignmentRightPart,
                processingEnv.getElementUtils().getTypeElement(assignmentLeftPart.getCanonicalName()).asType());
    }

    /** Return true if the type has some generic parameter */
    public static boolean hasTypeParameters(ProcessingEnvironment processingEnv, TypeMirror typeMirror) {
        TypeElement element = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
        return element != null && !element.getTypeParameters().isEmpty();
    }

    public static boolean isVoid(ExecutableElement method) {
        return (method.getReturnType() instanceof NoType);
    }

    public static boolean hasPublicConstructor(TypeElement nullTypeElement) {
        for (Element element : nullTypeElement.getEnclosedElements()) {
            if (isConstructor(element) &&
                    isPublic(element) &&
                    ((ExecutableElement) element).getParameters().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAbstract(TypeElement nullTypeElement) {
        return nullTypeElement.getModifiers().contains(ABSTRACT);
    }
}
