package com.joanzap.minim.processors;

import com.joanzap.minim.api.BaseEvent;
import com.joanzap.minim.api.internal.Injector;
import com.joanzap.minim.processors.utils.Logger;
import com.joanzap.minim.processors.utils.Utils;
import com.squareup.javawriter.JavaWriter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.*;

@SupportedAnnotationTypes({"com.joanzap.minim.api.annotation.InjectResponse", "com.joanzap.minim.api.annotation.InjectService"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class InjectAP extends AbstractProcessor {

    public static final String INJECTOR_SUFFIX = "Injector";
    private final List<String> managedTypes = new ArrayList<String>();

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {

        // Initialize a logger
        Logger logger = new Logger(processingEnv.getMessager());

        // Get holding class
        for (TypeElement typeElement : typeElements) {
            Set<? extends Element> annotatedElements = roundEnvironment.getElementsAnnotatedWith(typeElement);
            for (Element annotatedElement : annotatedElements) {
                TypeElement enclosingElement = (TypeElement) annotatedElement.getEnclosingElement();
                manageType(enclosingElement, logger);
            }
        }

        return true;

    }

    private void manageType(TypeElement enclosingElement, Logger logger) {

        // Make sure we don't process twice the same type
        String singleName = enclosingElement.getSimpleName().toString();
        String qualifiedName = enclosingElement.getQualifiedName().toString();
        String packageName = Utils.getElementPackageName(enclosingElement);
        if (managedTypes.contains(qualifiedName)) return;
        managedTypes.add(qualifiedName);

        // Prepare the output file
        try {
            JavaFileObject classFile = processingEnv.getFiler().createSourceFile(qualifiedName + "Injector");
            logger.note("Writing " + classFile.toUri().getRawPath());
            Writer out = classFile.openWriter();
            JavaWriter writer = new JavaWriter(out);

            // Generates "public final class XXXInjector extends Injector<XXX>"
            writer.emitPackage(packageName)
                    .emitEmptyLine()
                    .emitImports(Injector.class, BaseEvent.class)
                    .emitEmptyLine()
                    .beginType(singleName + INJECTOR_SUFFIX, "class", of(PUBLIC, FINAL), "Injector<" + singleName + ">")
                    .emitEmptyLine();

            // Generates "protected void inject(XXX target)"
            writer.emitAnnotation(Override.class)
                    .beginMethod("void", "inject", of(PROTECTED), singleName, "target");
            // Here, inject services
            writer.endMethod().emitEmptyLine();

            // Generates "protected void dispatch(XXX target, BaseEvent event)"
            writer.emitAnnotation(Override.class)
                    .beginMethod("void", "dispatch", of(PROTECTED), singleName, "target", BaseEvent.class.getSimpleName(), "event");
            // Here, dispatch events to methods
            writer.endMethod().emitEmptyLine();

            // End of file
            writer.endType();
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new IllegalStateException("Error while create the injector for " + qualifiedName, e);
        }

    }

}