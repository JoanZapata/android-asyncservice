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
package com.joanzapata.android.asyncservice.processors;

import com.joanzapata.android.asyncservice.api.EnhancedService;
import com.joanzapata.android.asyncservice.api.ErrorMapper;
import com.joanzapata.android.asyncservice.api.Message;
import com.joanzapata.android.asyncservice.api.annotation.ApplicationContext;
import com.joanzapata.android.asyncservice.api.annotation.CacheThenCall;
import com.joanzapata.android.asyncservice.api.annotation.ErrorManagement;
import com.joanzapata.android.asyncservice.api.annotation.Id;
import com.joanzapata.android.asyncservice.api.annotation.Init;
import com.joanzapata.android.asyncservice.api.annotation.Null;
import com.joanzapata.android.asyncservice.api.annotation.Serial;
import com.joanzapata.android.asyncservice.api.annotation.ThrowerParam;
import com.joanzapata.android.asyncservice.api.annotation.Ui;
import com.joanzapata.android.asyncservice.api.internal.AsyncService;
import com.joanzapata.android.asyncservice.api.internal.BackgroundExecutor;
import com.joanzapata.android.asyncservice.processors.utils.Logger;
import com.joanzapata.android.asyncservice.processors.utils.Utils;
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
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.joanzapata.android.asyncservice.processors.utils.Utils.*;
import static java.util.EnumSet.of;
import static javax.lang.model.element.Modifier.*;

@SupportedAnnotationTypes({"com.joanzapata.android.asyncservice.api.annotation.AsyncService"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class AsyncServiceAP extends AbstractProcessor {

    public static final String GENERATED_CLASS_SUFFIX = "Impl";
    private Logger logger;

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        try {
            // Initialize a logger
            logger = new Logger(processingEnv.getMessager());

            // Retrieve @MinimService annotated elements
            Set<? extends Element> minimServices = roundEnvironment.getElementsAnnotatedWith(com.joanzapata.android.asyncservice.api.annotation.AsyncService.class);

            // Loop through elements
            for (Element minimServiceElement : minimServices) {
                logger.note("Processing @AsyncService on " + minimServiceElement);

                // Make sure all fields are static
                for (Element element : minimServiceElement.getEnclosedElements())
                    if (isField(element) && !isStatic(element))
                        logger.error(element, "All fields in @AsyncService should be static");

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

                // Emit general imports
                JavaWriter writer = javaWriter.emitPackage(elementPackage)
                        .emitImports(AsyncService.class,
                                Message.class,
                                BackgroundExecutor.class,
                                ErrorMapper.class,
                                Serializable.class,
                                List.class)
                        .emitImports(
                                "android.os.Handler",
                                "android.os.Looper",
                                minimServiceElement.toString(),
                                "com.joanzapata.android.asyncservice.api.internal.AsyncServiceCache",
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
                        logger.error(applicationContextField, "@ApplicationContext fields should be either public or protected");
                    writer.emitStatement("%s = AsyncService.context", applicationContextField.getSimpleName());
                }

                // Call all init methods if flag not set already
                callInitMethods(writer, initMethods);

                writer.endConstructor();

                // Manage each method
                for (Element element : minimServiceElement.getEnclosedElements())
                    if (isMethod(element))
                        createDelegateMethod(writer, (ExecutableElement) element, newElementName);

                // Implement EnhancedService interface if needed
                if (implementsInterface((TypeElement) minimServiceElement, EnhancedService.class)) {
                    writer.emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("<T extends Serializable> T", "getCached", of(PUBLIC), "String", "key", "Class<T>", "returnType")
                            .emitStatement("return AsyncServiceCache.get(key, returnType)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("<T extends Serializable> List<T>", "getCachedList", of(PUBLIC), "String", "key", "Class<T>", "returnType")
                            .emitStatement("return AsyncServiceCache.getList(key, returnType)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("void", "cache", of(PUBLIC), "String", "key", "Serializable", "object")
                            .emitStatement("AsyncServiceCache.store(key, object)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("void", "cacheList", of(PUBLIC), "String", "key", "List<? extends Serializable>", "object")
                            .emitStatement("AsyncServiceCache.storeList(key, object)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("void", "send", of(PUBLIC), "Object", "payload")
                            .emitStatement("Message message = new Message(payload)")
                            .emitStatement("message.setEmitter(emitter)")
                            .emitStatement("AsyncService.dispatch(message)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("void", "clearCache", of(PUBLIC), "String", "key")
                            .emitStatement("AsyncServiceCache.remove(key)")
                            .endMethod()
                            .emitEmptyLine()
                            .emitAnnotation(Override.class)
                            .beginMethod("void", "clearCache", of(PUBLIC))
                            .emitStatement("AsyncServiceCache.clear()")
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
        AnnotationMirror asyncServiceAnnotation = getAnnotation(minimServiceElement, com.joanzapata.android.asyncservice.api.annotation.AsyncService.class);
        Object value = getAnnotationValue(asyncServiceAnnotation, "errorMapper");
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
            if (!isStatic(initMethod))
                logger.error(initMethod, "@Init annotated method must be static.");

            writer.emitEmptyLine()
                    .emitField("boolean", initMethod.getSimpleName().toString() + "_called",
                            isStatic(initMethod) ? of(PRIVATE, STATIC, VOLATILE) : of(PRIVATE, VOLATILE),
                            "false");
        }
    }

    private void createDelegateMethod(JavaWriter classWriter, ExecutableElement method, String newElementName) throws IOException {
        if (!isPublicOrProtectedMethod(method)) return;
        if (isAnnotatedWith(method, Init.class)) return;

        // Find all needed values for @CacheThenCall if any
        AnnotationMirror cachedAnnotation = getAnnotation(method, CacheThenCall.class);
        boolean isCached = cachedAnnotation != null;
        boolean isUiThread = getAnnotation(method, Ui.class) != null;
        boolean hasResult = !isVoid(method);

        if (isCached && !hasResult)
            logger.error(method, cachedAnnotation, "@CacheThenCall annotated method should not return void.");

        if (hasResult && hasTypeParameters(processingEnv, method.getReturnType()))
            logger.error(method, "You can't use parametrized types in your method return type.");

        String annotationCacheToParse = null;
        String cacheValueFromMethodSignatureToParse = defineKeyFromMethod(method);
        if (isCached) {
            String annotationValue = getAnnotationValue(cachedAnnotation, "value");
            annotationCacheToParse = annotationValue == null ?
                    cacheValueFromMethodSignatureToParse : annotationValue;

            // If cached, return type should be serializable
            if (!isAssignable(processingEnv, method.getReturnType(), Serializable.class))
                logger.error(method, method.getReturnType() + " should implement Serializable in order to be cached.");
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
        String serial = annotation == null ? "__SERIAL_DEFAULT" : (String) getAnnotationValue(annotation, "value");

        // Define id
        annotation = getAnnotation(method, Id.class);
        String id = annotation == null ? null : (String) getAnnotationValue(annotation, "value");

        // Define null management
        AnnotationMirror nullAnnotation = getAnnotation(method, Null.class);
        boolean isNullManaged = nullAnnotation != null;
        if (isNullManaged && isVoid(method))
            logger.error(method, "You can't use @Null on a method with no return type.");
        TypeMirror nullClass = nullAnnotation == null ? null : (TypeMirror) getAnnotationValue(nullAnnotation, "value");

        // Manage illegal args on @Null
        if (isNullManaged) {
            TypeElement nullTypeElement = processingEnv.getElementUtils().getTypeElement(nullClass.toString());
            if (isAbstract(nullTypeElement))
                logger.error(method, nullAnnotation, "value", "The null message type should not be abstract.");
            if (!hasPublicConstructor(nullTypeElement))
                logger.error(method, nullAnnotation, "value", "The null message type must have a public no-arg constructor.");
        }

        if (isCached) {
            classWriter.emitField("String", "cacheKey", of(FINAL), parseCacheKeyValue(annotationCacheToParse));
            StringWriter buffer = new StringWriter();
            JavaWriter inner = new JavaWriter(buffer);
            inner.emitPackage("")
                    .beginType("Runnable()", "new")
                    .emitAnnotation("Override")
                    .beginMethod("void", "run", of(PUBLIC))
                    .emitStatement("%s cache = AsyncServiceCache.get(cacheKey, %s.class)", method.getReturnType(), method.getReturnType())
                    .emitStatement("if (cache == null) return")
                    .emitStatement("Message message = new Message(cache)")
                    .emitStatement("message.cached().setEmitter(emitter)")
                    .emitStatement("AsyncService.dispatch(message)")
                    .endMethod().endType();
            classWriter.emitStatement("BackgroundExecutor.execute(%s, callId, \"%s\")", buffer.toString(), "__SERIAL_CHECK_CACHE");
        }

        String threadingPrefix = isUiThread ? "__handler.post(" : "BackgroundExecutor.execute(\n";
        String threadingSuffix = isUiThread ? ")" : ", %s, \"%s\")";

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

        if (hasResult) {
            // If the method has result
            inner.emitStatement("%s __payload = %s.super.%s(%s)",
                    method.getReturnType(),
                    newElementName,
                    method.getSimpleName(),
                    formatParametersForCall(method))
                    .beginControlFlow("if (__payload == null)");

            if (isNullManaged) {
                inner.emitStatement("Message __message = new Message(new %s())", nullClass)
                        .emitStatement("__message.setEmitter(emitter)")
                        .emitStatement("AsyncService.dispatch(__message)")
                        .emitStatement("return");
            } else {
                inner.emitStatement("return");
            }

            inner.endControlFlow()
                    .emitStatement("Message __message = new Message(__payload)")
                    .emitStatement("__message.setQuery(callId)")
                    .emitStatement("__message.setEmitter(emitter)");

            // If it's cached, cache it
            if (isCached) inner.emitStatement("AsyncServiceCache.store(cacheKey, __payload)");

            // Then dispatch the message
            inner.emitStatement("AsyncService.dispatch(__message)");

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
        classWriter.emitStatement(threadingPrefix + runnableCode + threadingSuffix,
                id == null ? "callId" : parseCacheKeyValue(id), serial);

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

        // Add values of ErrorManagement annotation on the class
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
                .emitField("int", "code", of(FINAL), "__errorMapper.mapError(__e)");

        // Try to match the code with a message class to instantiate
        inner.beginControlFlow("if (code == -1)")
                .emitSingleLineComment("Ignore")
                .endControlFlow();

        for (ErrorCase errorCase : errorCases) {
            inner.beginControlFlow("else if (code == %s)", errorCase.code)
                    .emitStatement("Message __errorMessage = new Message(new %s(%s))",
                            errorCase.className.toString(),
                            constructErrorMessageParams(errorCase.className, method, "__e"))
                    .emitStatement("__errorMessage.setEmitter(emitter)")
                    .emitStatement("AsyncService.dispatch(__errorMessage)")
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
                    if (!isAssignable(processingEnv, variableElement, Throwable.class))
                        logger.error(variableElement, "In an ErrorMessage, constructor params must be annotated with @ThrowerParam or extend Throwable.");
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

                    // Full matched method name, for logs only
                    String matchedFullMethodName = throwerMethod.getEnclosingElement().getSimpleName()
                            + "." + throwerMethod.getSimpleName();

                    if (matchingParam == null)
                        logger.error(variableElement, "No parameter named " + throwerParamName
                                + " in " + matchedFullMethodName);

                    boolean assignable = processingEnv.getTypeUtils().isAssignable(
                            variableElement.asType(),
                            matchingParam.asType());

                    if (!assignable)
                        logger.error(variableElement, "Could not bind (" + variableElement.asType() + ") to ("
                                + matchingParam.asType() + ") in " + matchedFullMethodName);

                    params.append(throwerParamName);
                }
            }
            return params.toString();
        }

        logger.error(processingEnv.getElementUtils().getTypeElement(targetType.toString()),
                "Couldn't find a suitable constructor.");

        // Won't reach this point, logger.error() stops the build.
        return "";
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
        for (Element initMethod : initMethods)
            if (!isPublicOrProtectedMethod(initMethod))
                logger.error(initMethod, "@Init methods should be public or protected");
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