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
import com.joanzapata.android.kiss.api.annotation.ApplicationContext;
import com.joanzapata.android.kiss.api.annotation.Cached;
import com.joanzapata.android.kiss.api.annotation.Init;
import com.joanzapata.android.kiss.api.annotation.KissService;
import com.joanzapata.android.kiss.api.annotation.Ui;
import com.joanzapata.android.kiss.api.internal.BackgroundExecutor;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import static com.joanzapata.android.kiss.processors.utils.Utils.*;
import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.*;

@SupportedAnnotationTypes({"com.joanzapata.android.kiss.api.annotation.KissService"})
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
                logger.note("Writing " + classFile.toUri().getRawPath());
                Writer out = classFile.openWriter();
                JavaWriter javaWriter = new JavaWriter(out);

                // Spot all @Init annotated methods
                List<Element> initMethods = findElementsAnnotatedWith((TypeElement) minimServiceElement, Init.class);
                checkInitAnnotatedMethods(initMethods);

                // Start writing the file
                JavaWriter writer = javaWriter.emitPackage(elementPackage)
                        .emitImports(Kiss.class, BaseEvent.class, BackgroundExecutor.class)
                        .emitImports(
                                "android.os.Handler",
                                "android.os.Looper",
                                minimServiceElement.toString(),
                                "com.joanzapata.android.kiss.api.internal.KissCache",
                                "android.content.Context")
                        .emitEmptyLine()
                        .beginType(newElementName, "class", of(PUBLIC, FINAL), minimServiceElement.toString());

                // Create the emitter field
                writer.emitEmptyLine()
                        .emitField("Object", "emitter", of(PRIVATE, FINAL));

                // Create the UI thread handler
                writer.emitEmptyLine()
                        .emitField("Handler", "__handler", of(PRIVATE, FINAL), "new Handler(Looper.getMainLooper())");

                // Create flags for each @Init method
                writeCallFlags(writer, initMethods);

                // Generate a public constructor
                writer.emitEmptyLine()
                        .beginConstructor(of(PUBLIC), "Object", "emitter")
                        .emitStatement("this.emitter = emitter");

                // If any @ApplicationContext, inject it here
                List<Element> applicationContextFields = findElementsAnnotatedWith((TypeElement) minimServiceElement, ApplicationContext.class);
                for (Element applicationContextField : applicationContextFields) {
                    if (!isPublicOrProtectedField(applicationContextField))
                        throw new IllegalStateException("@ApplicationContext fields should be either public or protected");
                    writer.emitStatement("%s = Kiss.context", applicationContextField.getSimpleName());
                }

                // Call all init methods if flag not set already
                callInitMethods(writer, initMethods);

                writer.endConstructor();

                // Manage each method
                for (Element element : minimServiceElement.getEnclosedElements())
                    if (Utils.isMethod(element))
                        createDelegateMethod(writer, (ExecutableElement) element, newElementName);

                writer.endType();

                out.flush();
                out.close();
            }
            return true;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void callInitMethods(JavaWriter writer, List<Element> initMethods) throws IOException {
        for (Element initMethod : initMethods) {
            String methodName = initMethod.getSimpleName().toString();
            if (isStatic(initMethod)) {
                writer.beginControlFlow("if (!%s_called)", methodName)
                        .emitStatement("%s_called = true", methodName)
                        .emitStatement("%s()", methodName)
                        .endControlFlow();
            } else {
                writer.emitStatement("%s()", methodName);
            }
        }

    }

    private void writeCallFlags(JavaWriter writer, List<Element> initMethods) throws IOException {
        for (Element initMethod : initMethods) {
            if (!isStatic(initMethod)) continue;
            writer.emitEmptyLine()
                    .emitField("boolean", initMethod.getSimpleName().toString() + "_called",
                            isStatic(initMethod) ? of(PRIVATE, STATIC, VOLATILE) : of(PRIVATE, VOLATILE),
                            "false");
        }
    }

    private void createDelegateMethod(JavaWriter classWriter, ExecutableElement method, String newElementName) throws IOException {
        if (!isPublicOrProtectedMethod(method)) return;
        if (isAnnotatedWith(method, Init.class)) return;

        // Find all needed values for @Cache if any
        AnnotationMirror cachedAnnotation = getAnnotation(method, Cached.class);
        boolean isCached = cachedAnnotation != null;
        boolean isUiThread = getAnnotation(method, Ui.class) != null;
        boolean hasResult = !(method.getReturnType() instanceof NoType);

        if (isCached && !hasResult)
            throw new IllegalStateException("@Cached method should have a return value");

        String annotationCacheToParse = null;
        String cacheValueFromMethodSignatureToParse = defineKeyFromMethod(method);
        if (isCached) {
            String annotationValue = getAnnotationValue(cachedAnnotation, "key");
            annotationCacheToParse = annotationValue == null ?
                    cacheValueFromMethodSignatureToParse : annotationValue;
        }

        // Start the mimic method
        classWriter.emitEmptyLine()
                .beginMethod(
                        method.getReturnType().toString(),
                        method.getSimpleName().toString(),
                        method.getModifiers(),
                        Utils.formatParameters(method, true), null);

        // Check the cache in a background thread
        classWriter.emitField("String", "callId", of(FINAL), parseCacheKeyValue(cacheValueFromMethodSignatureToParse));

        if (isCached) {
            classWriter.emitField("String", "cacheKey", of(FINAL), parseCacheKeyValue(annotationCacheToParse));
            classWriter.emitStatement("BackgroundExecutor.execute(new Runnable() {\n" +
                    "    public void run() {\n" +
                    "        BaseEvent cache = KissCache.get(cacheKey, %s.class);\n" +
                    "        if (cache != null) Kiss.dispatch(emitter, cache.cached());\n" +
                    "    }\n" +
                    "}, callId, \"cache\")", method.getReturnType().toString());
        }

        String threadingPrefix = isUiThread ? "__handler.post(" : "BackgroundExecutor.execute(";
        String threadingSuffix = isUiThread ? ")" : ", callId, \"serial\")";

        // TODO If a similar task was already running/scheduled on the same serial, only check the cache.
        // Delegate the call to the user method in a background thread
        if (isCached) {
            // If cached with result
            classWriter.emitStatement(threadingPrefix +
                            "new Runnable() {\n" +
                            "    public void run() {\n" +
                            "        BaseEvent __event = %s.super.%s(%s);\n" +
                            "        __event.setQuery(callId);\n" +
                            "        if (__event != null) KissCache.store(cacheKey, __event);\n" +
                            "        Kiss.dispatch(emitter, __event);\n" +
                            "    }\n" +
                            "}" + threadingSuffix,
                    newElementName,
                    method.getSimpleName(),
                    formatParametersForCall(method)
            );
        } else if (hasResult) {
            // If not cached, but with result
            classWriter.emitStatement(threadingPrefix +
                            "new Runnable() {\n" +
                            "    public void run() {\n" +
                            "        BaseEvent __event = %s.super.%s(%s);\n" +
                            "        __event.setQuery(callId);\n" +
                            "        Kiss.dispatch(emitter, __event);\n" +
                            "    }\n" +
                            "}" + threadingSuffix,
                    newElementName,
                    method.getSimpleName(),
                    formatParametersForCall(method)
            );
        } else {
            // If no cache, no result
            classWriter.emitStatement(threadingPrefix +
                            "new Runnable() {\n" +
                            "    public void run() {\n" +
                            "        %s.super.%s(%s);\n" +
                            "    }\n" +
                            "}" + threadingSuffix,
                    newElementName,
                    method.getSimpleName(),
                    formatParametersForCall(method)
            );
        }

        if (hasResult) classWriter.emitStatement("return null");
        classWriter.endMethod();

    }

    /**
     * @param method The method to find a key for.
     * @return "class_name.method_name(param_value1, [param_value2, ...])"
     */
    private String defineKeyFromMethod(ExecutableElement method) {
        String className = method.getEnclosingElement().getSimpleName().toString();
        String methodName = method.getSimpleName().toString();
        String args = Utils.formatParametersForCacheKey(method);
        return className + "." + methodName + "(" + args + ")";
    }

    private void checkInitAnnotatedMethods(List<Element> initMethods) {
        for (Element initMethod : initMethods) {
            if (!isPublicOrProtectedMethod(initMethod)) {
                throw new IllegalArgumentException("@Init methods should be public or protected");
            }
        }
    }
}