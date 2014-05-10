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
package com.joanzap.android.kiss.processors;

import com.joanzap.android.kiss.api.annotation.KissService;
import com.joanzap.android.kiss.api.internal.Kiss;
import com.joanzap.android.kiss.processors.utils.Logger;
import com.joanzap.android.kiss.processors.utils.Utils;
import com.squareup.javawriter.JavaWriter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.*;

@SupportedAnnotationTypes({"com.joanzap.android.kiss.api.annotation.KissService"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class KissServiceAP extends AbstractProcessor {

    public static final String GENERATED_CLASS_SUFFIX = "Impl";

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        try {

            // Initialize a logger
            Logger logger = new Logger(processingEnv.getMessager());

            // Retrieve @MinimService annotated elements
            Set<? extends Element> minimServices = roundEnvironment.getElementsAnnotatedWith(KissService.class);

            // Loop through elements
            for (Element minimServiceElement : minimServices) {
                logger.note("Processing @KissService on " + minimServiceElement);

                // Get name and package
                String elementName = minimServiceElement.getSimpleName().toString();
                String elementPackage = Utils.getElementPackageName(minimServiceElement);

                // Create the output file
                String newElementName = elementName + GENERATED_CLASS_SUFFIX;
                String targetFile = ((TypeElement) minimServiceElement).getQualifiedName() + GENERATED_CLASS_SUFFIX;
                JavaFileObject classFile = processingEnv.getFiler().createSourceFile(targetFile);
                logger.note(classFile.toUri().toString());
                Writer out = classFile.openWriter();
                JavaWriter writer = new JavaWriter(out);

                // Start writing the file
                JavaWriter classWriter = writer.emitPackage(elementPackage)
                        .emitImports(Kiss.class)
                        .emitImports(
                                minimServiceElement.toString(),
                                "android.content.Context")
                        .emitEmptyLine()
                        .beginType(newElementName, "class", of(PUBLIC, FINAL), minimServiceElement.toString());

                // Create the emitter field
                classWriter
                        .emitEmptyLine()
                        .emitField("Object", "emitter", of(PRIVATE, FINAL));

                // Generate a public constructor
                classWriter
                        .emitEmptyLine()
                        .beginConstructor(of(PUBLIC), "Object", "emitter")
                        .emitStatement("this.emitter = emitter")
                        .endConstructor();

                // Manage each method
                for (Element element : minimServiceElement.getEnclosedElements())
                    if (Utils.isPublicMethod(element))
                        createDelegateMethod(classWriter, (ExecutableElement) element);

                classWriter.endType();

                out.flush();
                out.close();
            }
            return true;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void createDelegateMethod(JavaWriter classWriter, ExecutableElement method) throws IOException {

        // Start the mimic method
        classWriter.emitEmptyLine()
                .beginMethod(
                        method.getReturnType().toString(),
                        method.getSimpleName().toString(),
                        method.getModifiers(),
                        Utils.formatParameters(method, true), null)

                        // Delegate the call to the user method
                .emitStatement("Kiss.dispatch(super.%s(%s))",
                        method.getSimpleName(),
                        Utils.formatParametersForCall(method))

                .emitStatement("return null")
                .endMethod();

    }
}