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
package com.joanzapata.android.kiss.processors;

import com.joanzapata.android.kiss.api.Message;
import com.joanzapata.android.kiss.api.annotation.InjectService;
import com.joanzapata.android.kiss.api.annotation.OnMessage;
import com.joanzapata.android.kiss.api.internal.Injector;
import com.joanzapata.android.kiss.api.internal.Kiss;
import com.joanzapata.android.kiss.processors.utils.Logger;
import com.joanzapata.android.kiss.processors.utils.Utils;
import com.squareup.javawriter.JavaWriter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.joanzapata.android.kiss.api.annotation.OnMessage.Sender.ALL;
import static com.joanzapata.android.kiss.processors.utils.Utils.*;
import static java.util.Arrays.asList;
import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.*;

@SupportedAnnotationTypes({"com.joanzapata.android.kiss.api.annotation.Result", "com.joanzapata.android.kiss.api.annotation.InjectService"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class InjectAP extends AbstractProcessor {

    public static final String INJECTOR_SUFFIX = "Injector";
    private final List<String> managedTypes = new ArrayList<String>();
    private Logger logger;

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        logger = new Logger(processingEnv.getMessager());

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

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private void manageType(TypeElement enclosingElement, Logger logger) {

        // Make sure we don't process twice the same type
        String simpleName = enclosingElement.getSimpleName().toString();
        String qualifiedName = enclosingElement.getQualifiedName().toString();
        String packageName = Utils.getElementPackageName(enclosingElement);
        if (managedTypes.contains(qualifiedName)) return;
        managedTypes.add(qualifiedName);

        // Prepare the output file
        try {
            JavaFileObject classFile = processingEnv.getFiler().createSourceFile(qualifiedName + INJECTOR_SUFFIX);
            logger.note("Writing " + classFile.toUri().getRawPath());
            Writer out = classFile.openWriter();
            JavaWriter writer = new JavaWriter(out);

            writer.emitPackage(packageName);

            // Initial imports
            writer.emitImports(
                    Kiss.class,
                    Injector.class,
                    Message.class,
                    Set.class,
                    HashSet.class)
                    .emitImports(
                            "android.os.Handler",
                            "android.os.Looper");

            // Generates "public final class XXXInjector extends Injector<XXX>"
            writer.emitEmptyLine()
                    .beginType(simpleName + INJECTOR_SUFFIX, "class", of(PUBLIC, FINAL), "Injector<" + simpleName + ">");

            // Generate a handler to execute runnables on the UI Thread
            writer.emitEmptyLine()
                    .emitField("Handler", "__handler", of(PRIVATE, FINAL), "new Handler(Looper.getMainLooper())");

            // Keep trace of when a method has received data which is not from cache
            writer.emitEmptyLine()
                    .emitField("Set<String>", "__receivedFinalResponses", of(PRIVATE, FINAL), "new HashSet<String>()");

            // Generates "protected void inject(XXX target) { ..."
            writer.emitEmptyLine()
                    .emitAnnotation(Override.class)
                    .beginMethod("void", "inject", of(PROTECTED), simpleName, "target");

            // Here, inject all services
            List<Element> elementsAnnotatedWith = findElementsAnnotatedWith(enclosingElement, InjectService.class);
            for (Element element : elementsAnnotatedWith) {
                if (isPublicOrPackagePrivate(element)) {
                    writer.emitStatement("target.%s = new %s(target)", element.getSimpleName(), element.asType().toString() + KissServiceAP.GENERATED_CLASS_SUFFIX);
                }
            }

            // End of inject()
            writer.endMethod().emitEmptyLine();

            // Generates "protected void dispatch(XXX target, Message event)"
            writer.emitAnnotation(Override.class)
                    .beginMethod("void", "dispatch", of(PROTECTED), "final " + simpleName, "target", "final " + Message.class.getSimpleName(), "event");

            // Once the user has received a "remote" result, make sure no cache is sent anymore
            writer.emitField("boolean", "__hasBeenReceivedAlready", of(FINAL), "event.getQuery() != null && __receivedFinalResponses.contains(event.getQuery())")
                    .emitStatement("if (event.isCached() && __hasBeenReceivedAlready) return")
                    .emitStatement("if (!__hasBeenReceivedAlready && !event.isCached()) __receivedFinalResponses.add(event.getQuery())");

            // Here, dispatch events to methods
            List<Element> responseReceivers = findElementsAnnotatedWith(enclosingElement, OnMessage.class);
            for (Element responseReceiver : responseReceivers) {
                ExecutableElement annotatedMethod = (ExecutableElement) responseReceiver;
                List<? extends VariableElement> parameters = annotatedMethod.getParameters();

                if (parameters.size() > 1)
                    logger.error(responseReceiver, "@OnMessage annotated methods can't have more than 1 argument");

                // Define event type given parameter or @InjectResponse value
                List<String> eventTypes;
                boolean hasArg = parameters.size() == 1;
                if (hasArg) {
                    TypeMirror typeMirror = parameters.get(0).asType();
                    eventTypes = asList(typeMirror.toString());
                    if (hasTypeParameters(processingEnv, typeMirror))
                        logger.error(parameters.get(0), "You can't receive typed parameters in @OnMessage annotated methods");

                } else {
                    AnnotationMirror annotationMirror = getAnnotation(annotatedMethod, OnMessage.class);
                    List<AnnotationValue> parameterTypeClasses = getAnnotationValue(annotationMirror, "value");

                    // Validate each parameter type given in the annotation
                    eventTypes = new ArrayList<String>();
                    for (AnnotationValue value : parameterTypeClasses) {
                        DeclaredType parameterTypeClass = (DeclaredType) value.getValue();
                        if (parameterTypeClass == null)
                            logger.error(annotatedMethod, "Either declare an argument or give @OnMessage a value.");
                        if (hasTypeParameters(processingEnv, parameterTypeClass))
                            logger.error(annotatedMethod, annotationMirror, "value", "You can't receive typed parameters in @OnMessage method");
                        eventTypes.add(parameterTypeClass.toString());
                    }

                }

                // Define whether we should check emitter or not dependeing on the annotation value
                VariableElement from = getAnnotationValue(getAnnotation(annotatedMethod, OnMessage.class), "from");
                boolean checkEmitter = !ALL.toString().equals("" + from);

                // Write the code to call the user method
                if (checkEmitter) writer.beginControlFlow("if (event.getEmitter() == getTarget())");

                // Create a new inner class for the Runnable to run on UI thread
                StringWriter buffer = new StringWriter();
                JavaWriter inner = new JavaWriter(buffer);
                inner.emitPackage("")
                        .beginType("Runnable()", "new")
                        .emitAnnotation("Override")
                        .beginMethod("void", "run", of(PUBLIC));
                if (hasArg) inner.emitStatement("target.%s((%s) event.getPayload())",
                        annotatedMethod.getSimpleName(), eventTypes.get(0));
                else inner.emitStatement("target.%s()",
                        annotatedMethod.getSimpleName());
                inner.endMethod().endType();

                // For each type (can be multiple)
                for (int i = 0; i < eventTypes.size(); i++) {
                    String eventType = eventTypes.get(i);
                    writer.beginControlFlow("%sif (event.getPayload() instanceof %s)", i != 0 ? "else " : "", eventType)
                            .emitStatement("__handler.post(%s)", buffer.toString())
                            .endControlFlow();
                }

                if (checkEmitter) writer.endControlFlow();
            }

            // End of inject();
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