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

import com.joanzapata.android.kiss.api.BaseEvent;
import com.joanzapata.android.kiss.api.annotation.InjectService;
import com.joanzapata.android.kiss.api.annotation.Result;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.joanzapata.android.kiss.api.annotation.Result.Sender.ALL;
import static com.joanzapata.android.kiss.processors.utils.Utils.*;
import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.*;

@SupportedAnnotationTypes({"com.joanzapata.android.kiss.api.annotation.Result", "com.joanzapata.android.kiss.api.annotation.InjectService"})
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

            // Generates "public final class XXXInjector extends Injector<XXX>"
            writer.emitPackage(packageName)
                    .emitImports(Kiss.class, Injector.class, BaseEvent.class)
                    .emitEmptyLine()
                    .beginType(simpleName + INJECTOR_SUFFIX, "class", of(PUBLIC, FINAL), "Injector<" + simpleName + ">")
                    .emitEmptyLine();

            // Generates "protected void inject(XXX target) { ..."
            writer.emitAnnotation(Override.class)
                    .beginMethod("void", "inject", of(PROTECTED), simpleName, "target");

            // Here, inject all services
            List<Element> elementsAnnotatedWith = findElementsAnnotatedWith(enclosingElement, InjectService.class);
            for (Element element : elementsAnnotatedWith) {
                if (element.getModifiers().contains(PUBLIC)) {
                    writer.emitStatement("target.%s = new %s(target)", element.getSimpleName(), element.asType().toString() + KissServiceAP.GENERATED_CLASS_SUFFIX);
                }
            }

            // End of inject()
            writer.endMethod().emitEmptyLine();

            // Generates "protected void dispatch(XXX target, BaseEvent event)"
            writer.emitAnnotation(Override.class)
                    .beginMethod("void", "dispatch", of(PROTECTED), simpleName, "target", BaseEvent.class.getSimpleName(), "event");

            // Here, dispatch events to methods
            List<Element> responseReceivers = findElementsAnnotatedWith(enclosingElement, Result.class);
            for (Element responseReceiver : responseReceivers) {
                ExecutableElement annotatedMethod = (ExecutableElement) responseReceiver;
                List<? extends VariableElement> parameters = annotatedMethod.getParameters();
                assertThat(parameters.size() <= 1, "@InjectResponse annotated methods should have exactly one parameter.");

                // Define event type given parameter or @InjectResponse value
                String eventType;
                if (parameters.size() == 1) {
                    eventType = parameters.get(0).asType().toString();
                } else {
                    DeclaredType parameterTypeClass = getAnnotationValue(getAnnotation(annotatedMethod, Result.class), "value");
                    assertThat(parameterTypeClass != null, "@InjectResponse on a no-arg method should have a value.");
                    eventType = parameterTypeClass.toString();
                }

                // Define whether we should check emitter or not dependeing on the annotation value
                VariableElement from = getAnnotationValue(getAnnotation(annotatedMethod, Result.class), "from");
                boolean checkEmitter = !ALL.toString().equals("" + from);

                // Write the code to call the user method
                if (checkEmitter) writer.beginControlFlow("if (event.getEmitter() == getTarget())");
                writer.beginControlFlow("if (event instanceof %s)", eventType)
                        .emitStatement("target.%s((%s) event)", annotatedMethod.getSimpleName(), eventType)
                        .endControlFlow();
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