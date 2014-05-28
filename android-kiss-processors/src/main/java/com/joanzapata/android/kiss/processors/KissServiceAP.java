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

import com.joanzapata.android.kiss.api.BaseService;
import com.joanzapata.android.kiss.api.ErrorMapper;
import com.joanzapata.android.kiss.api.ErrorMessage;
import com.joanzapata.android.kiss.api.Message;
import com.joanzapata.android.kiss.api.annotation.ApplicationContext;
import com.joanzapata.android.kiss.api.annotation.Cached;
import com.joanzapata.android.kiss.api.annotation.ErrorManagement;
import com.joanzapata.android.kiss.api.annotation.Init;
import com.joanzapata.android.kiss.api.annotation.KissService;
import com.joanzapata.android.kiss.api.annotation.Serial;
import com.joanzapata.android.kiss.api.annotation.ThrowerParam;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.joanzapata.android.kiss.api.annotation.Serial.*;
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

                // Make sure all fields are static
                for (Element element : minimServiceElement.getEnclosedElements())
                    if (isField(element) && !isStatic(element))
                        throw new IllegalArgumentException("All fields in @KissService should be static, " + element.getSimpleName() + " is not.");

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

                String errorMapperClassName = findErrorMapperClassName(minimServiceElement);

                // Start writing the file
                JavaWriter writer = javaWriter.emitPackage(elementPackage)
                        .emitImports(Kiss.class, Message.class, BackgroundExecutor.class, ErrorMapper.class, ErrorMessage.class)
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

                // Create the error mapper
                writer.emitEmptyLine()
                        .emitField("ErrorMapper", "__errorMapper", of(PRIVATE, FINAL), "new " + errorMapperClassName + "()");

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
                    if (isMethod(element))
                        createDelegateMethod(writer, (ExecutableElement) element, newElementName);

                // Implement BaseService interface if needed
                if (implementsInterface((TypeElement) minimServiceElement, BaseService.class)) {
                    writer.emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("<T extends Message> T", "getCachedMessage", of(PUBLIC), "String", "key", "Class<T>", "returnType")
                            .emitStatement("return KissCache.get(key, returnType)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("void", "cacheMessage", of(PUBLIC), "String", "key", "Message", "object")
                            .emitStatement("KissCache.store(key, object)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("void", "sendMessage", of(PUBLIC), "Message", "message")
                            .emitStatement("Kiss.dispatch(emitter, message)")
                            .endMethod();
                }

                writer.endType();

                out.flush();
                out.close();
            }
            return true;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String findErrorMapperClassName(Element minimServiceElement) {
        AnnotationMirror kissServiceAnnotation = getAnnotation(minimServiceElement, KissService.class);
        Object value = getAnnotationValue(kissServiceAnnotation, "errorMapper");
        if (value == null)
            return ErrorMapper.DefaultErrorMapper.class.getCanonicalName();
        return value.toString();
    }

    private void callInitMethods(JavaWriter writer, List<Element> initMethods) throws IOException {
        for (Element initMethod : initMethods) {
            String methodName = initMethod.getSimpleName().toString();
            writer.beginControlFlow("if (!%s_called)", methodName)
                    .emitStatement("%s_called = true", methodName)
                    .emitStatement("%s()", methodName)
                    .endControlFlow();
        }

    }

    private void writeCallFlags(JavaWriter writer, List<Element> initMethods) throws IOException {
        for (Element initMethod : initMethods) {
            if (!isStatic(initMethod)) throw new IllegalArgumentException("@Init method " + initMethod.getSimpleName() + " should be static");
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

        // Define serial
        AnnotationMirror annotation = getAnnotation(method, Serial.class);
        String serial = annotation == null ? SERIAL_DEFAULT : (String) getAnnotationValue(annotation, "value");

        if (isCached) {
            classWriter.emitField("String", "cacheKey", of(FINAL), parseCacheKeyValue(annotationCacheToParse));
            classWriter.emitStatement("BackgroundExecutor.execute(new Runnable() {\n" +
                    "    public void run() {\n" +
                    "        Message cache = KissCache.get(cacheKey, %s.class);\n" +
                    "        if (cache != null) Kiss.dispatch(emitter, cache.cached());\n" +
                    "    }\n" +
                    "}, callId, \"%s\")", method.getReturnType().toString(), SERIAL_CHECK_CACHE);
        }

        String threadingPrefix = isUiThread ? "__handler.post(" : "BackgroundExecutor.execute(\n";
        String threadingSuffix = isUiThread ? ")" : ", callId, \"%s\")";

        // TODO If a similar task was already running/scheduled on the same serial, only check the cache.
        // Delegate the call to the user method in a background thread
        String runnableCode;
        StringWriter buffer = new StringWriter();
        JavaWriter inner = new JavaWriter(buffer);
        inner.emitPackage("");
        inner.beginType("Runnable()", "new");
        inner.emitAnnotation("Override");
        inner.beginMethod("void", "run", of(PUBLIC));

        beginErrorManagement(method, inner);

        if (isCached) {
            // If cached with result
            inner.emitStatement("Message __event = %s.super.%s(%s)",
                    newElementName,
                    method.getSimpleName(),
                    formatParametersForCall(method))
                    .emitStatement("if (__event == null) return")
                    .emitStatement("__event.setQuery(callId)")
                    .emitStatement("if (__event != null) KissCache.store(cacheKey, __event)")
                    .emitStatement("Kiss.dispatch(emitter, __event)");

        } else if (hasResult) {
            // If not cached, but with result
            inner.emitStatement("Message __event = %s.super.%s(%s)", newElementName,
                    method.getSimpleName(),
                    formatParametersForCall(method))
                    .emitStatement("if (__event == null) return")
                    .emitStatement("__event.setQuery(callId)")
                    .emitStatement("Kiss.dispatch(emitter, __event)");

        } else {
            // If no cache, no result
            inner.emitStatement("%s.super.%s(%s)", newElementName,
                    method.getSimpleName(),
                    formatParametersForCall(method));
        }

        endErrorManagement(method, inner);

        inner.endMethod();
        inner.endType();
        runnableCode = buffer.toString();
        classWriter.emitStatement(threadingPrefix + runnableCode + threadingSuffix, serial);

        if (hasResult) classWriter.emitStatement("return null");
        classWriter.endMethod();

    }

    private void beginErrorManagement(ExecutableElement method, JavaWriter inner) throws IOException {
        inner.beginControlFlow("try");
    }

    private void endErrorManagement(ExecutableElement method, JavaWriter inner) throws IOException {

        // End the try block
        inner.endControlFlow();

        // Retrieve values of ErrorManagement annotation on the method.
        List<ErrorCase> errorCases = new ArrayList<ErrorCase>();
        AnnotationMirror errorManagementAnnotation = getAnnotation(method, ErrorManagement.class);
        if (errorManagementAnnotation != null) {
            Iterable<AnnotationMirror> errorMappings = getAnnotationValue(errorManagementAnnotation, "value");
            for (AnnotationMirror errorMapping : errorMappings) {
                errorCases.add(new ErrorCase(
                        (Integer) getAnnotationValue(errorMapping, "on"),
                        (DeclaredType) getAnnotationValue(errorMapping, "send")
                ));
            }
        }

        // Add valures of ErrorManagement annotation on the class
        errorManagementAnnotation = getAnnotation(method.getEnclosingElement(), ErrorManagement.class);
        if (errorManagementAnnotation != null) {
            Iterable<AnnotationMirror> errorMappings = getAnnotationValue(errorManagementAnnotation, "value");
            for (AnnotationMirror errorMapping : errorMappings) {
                errorCases.add(new ErrorCase(
                        (Integer) getAnnotationValue(errorMapping, "on"),
                        (DeclaredType) getAnnotationValue(errorMapping, "send")
                ));
            }
        }
        // Begin the catch block
        inner.beginControlFlow("catch (Throwable __e)")
                .emitField("int", "code", of(FINAL), "__errorMapper.mapError(__e)")
                .emitStatement("ErrorMessage errorMessage = null");

        // Try to match the code with a message class to instantiate
        inner.beginControlFlow("if (code == -1)")
                .emitSingleLineComment("Ignore")
                .endControlFlow();

        for (ErrorCase errorCase : errorCases) {
            inner.beginControlFlow("else if (code == %s)", errorCase.code)
                    .emitStatement("Kiss.dispatch(emitter, new %s(%s))",
                            errorCase.className.toString(),
                            constructErrorMessageParams(errorCase.className, method, "__e"))
                    .emitStatement("return")
                    .endControlFlow();
        }

        // If no mapping could be found, delegate to global exception handler
        inner.emitStatement("Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), __e)")
                .endControlFlow();

    }

    /**
     * Construct the constructor args list from a throwing method.
     * @param targetType    The target type that will be constructed from this.
     * @param throwerMethod The method that implied the creation of the ErrorMessage. (will match with its params)
     * @param exceptionName The name to use to inject the exception.
     * @return param1, [param2, ...]
     */
    private String constructErrorMessageParams(DeclaredType targetType, ExecutableElement throwerMethod, String exceptionName) {
        for (Element element : targetType.asElement().getEnclosedElements()) {
            if (!isConstructor(element)) continue;

            ExecutableElement method = (ExecutableElement) element;

            StringBuilder params = new StringBuilder();
            for (VariableElement variableElement : method.getParameters()) {
                if (params.length() != 0) params.append(", ");
                AnnotationMirror throwerParamAnnotation = getAnnotation(variableElement, ThrowerParam.class);

                // The only authorized param without annotation should be an exception
                if (throwerParamAnnotation == null) {

                    boolean assignable = processingEnv.getTypeUtils().isAssignable(
                            variableElement.asType(),
                            processingEnv.getElementUtils().getTypeElement(Throwable.class.getCanonicalName()).asType());

                    if (!assignable)
                        throw new IllegalArgumentException("In an ErrorMessage, constructor params must be annotated with @ThrowerParam or be an exception.");

                    params.append(exceptionName);

                } else {
                    String throwerParamName = getAnnotationValue(throwerParamAnnotation, "value");

                    // Find matching param name in throwerMethod
                    VariableElement matchingParam = null;
                    for (VariableElement throwerMethodParam : throwerMethod.getParameters()) {
                        if (throwerMethodParam.getSimpleName().toString().equals(throwerParamName)) {
                            matchingParam = throwerMethodParam;
                        }
                    }

                    if (matchingParam == null) {
                        throw new IllegalArgumentException("Couldn't find a match for constructor param "
                                + variableElement.getSimpleName()
                                + " in thrower method "
                                + throwerMethod.getEnclosingElement().getSimpleName()
                                + "." + throwerMethod.getSimpleName());
                    }

                    boolean assignable = processingEnv.getTypeUtils().isAssignable(
                            variableElement.asType(),
                            matchingParam.asType());

                    if (!assignable)
                        throw new IllegalArgumentException("In " + targetType
                                + ", could not bind (" + variableElement.asType() + ") to ("
                                + matchingParam.asType() + ") for param ["
                                + throwerParamName + "]");

                    params.append(throwerParamName);
                }
            }
            return params.toString();
        }
        throw new IllegalStateException("Couldn't find a suitable constructor in " + targetType.toString());
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

    private static class ErrorCase {
        int code;
        DeclaredType className;

        ErrorCase(int code, DeclaredType className) {
            this.code = code;
            this.className = className;
        }
    }
}