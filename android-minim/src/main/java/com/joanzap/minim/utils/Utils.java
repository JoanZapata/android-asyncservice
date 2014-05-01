package com.joanzap.minim.utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;

public class Utils {

    /** Get the package name of an element */
    public static String getElementPackageName(Element minimServiceElement) {
        Element parentElement = minimServiceElement.getEnclosingElement();
        if (parentElement == null) return "";
        if (!isPackage(parentElement)) return getElementPackageName(parentElement);

        Element topParentElement = parentElement.getEnclosingElement();
        if (topParentElement == null) return parentElement.getSimpleName().toString();
        else return getElementPackageName(topParentElement) + parentElement.getSimpleName().toString();
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
    public static List<String> formatParameters(ExecutableElement method) {
        List<String> out = new ArrayList<String>();
        for (VariableElement var : method.getParameters()) {
            out.add(var.getKind().toString());
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
}
