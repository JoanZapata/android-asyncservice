package com.joanzap.minim.processors.utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;

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
    public static boolean isPublicMethod(Element elem) {
        return elem.getKind().equals(ElementKind.METHOD) &&
                elem.getModifiers().contains(PUBLIC);
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
            builder.append(var.getSimpleName().toString());
        }
        if (builder.length() > 0) builder.substring(0, builder.length() - 1);
        return builder.toString();
    }

    /**
     * Return the qualified name of a class given its package name and class name
     * @param packageName Example "com.foo"
     * @param className   Example "Bar"
     * @return Example "com.foo.Bar"
     */
    public static String getFullName(String packageName, String className) {
        if (isNullOrEmpty(packageName)) return className;
        else return packageName + "." + className;
    }

    /**
     * Return true if the given string is null or empty.
     */
    private static boolean isNullOrEmpty(String elementPackage) {
        return elementPackage == null || elementPackage.isEmpty();
    }
}
