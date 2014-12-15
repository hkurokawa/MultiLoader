package com.hkurokawa.multiloader.internal;

import com.hkurokawa.multiloader.OnCreateLoader;
import com.hkurokawa.multiloader.OnLoadFinished;
import com.hkurokawa.multiloader.OnLoaderReset;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.hkurokawa.multiloader.OnCreateLoader")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class MultiLoaderProcessor extends AbstractProcessor {
    public static final String SUFFIX = "$$LoaderInjector";
    public static final String ANDROID_PREFIX = "android.";
    public static final String JAVA_PREFIX = "java.";
    private static final List<Class<? extends Annotation>> ANNOTATIONS = Arrays.asList(
            OnCreateLoader.class, OnLoadFinished.class, OnLoaderReset.class
    );
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("Running processor [" + this + "].");

        for (Class<? extends Annotation> annotationClass : ANNOTATIONS) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotationClass)) {
                try {
                    // This should be guarded by the annotation's @Target but it's worth a check for safe casting.
                    if (!(element instanceof ExecutableElement) || element.getKind() != ElementKind.METHOD) {
                        throw new IllegalStateException(
                                String.format("@%s annotation must be on a method.", annotationClass.getSimpleName()));
                    }

                    final ExecutableElement executableElement = (ExecutableElement) element;
                    final TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

                    // Assemble information on the injection point.
                    final Annotation annotation = element.getAnnotation(annotationClass);
                    final Method annotationValue = annotationClass.getDeclaredMethod("value");
                    if (annotationValue.getReturnType() != int.class) {
                        throw new IllegalStateException(
                                String.format("@%s annotation value() type not int.", annotationClass));
                    }

                    final int id = (Integer) annotationValue.invoke(annotation);
                    final String name = executableElement.getSimpleName().toString();
                    final String targetType = enclosingElement.getQualifiedName().toString();
                    final String classPackage = getPackageName(enclosingElement);
                    final String enclosingClassName = getClassName(enclosingElement, classPackage);
                    final String className = enclosingClassName + SUFFIX + id;

                    // Verify that the method and its containing class are accessible via generated code.
                    boolean hasError = isInaccessibleViaGeneratedCode(annotationClass, "methods", element);
                    hasError |= isBindingInWrongPackage(annotationClass, element);

                    final ListenerClass listener = annotationClass.getAnnotation(ListenerClass.class);
                    if (listener == null) {
                        throw new IllegalStateException(
                                String.format("No @%s defined on @%s.", ListenerClass.class.getSimpleName(),
                                        annotationClass.getSimpleName()));
                    }

                    final ListenerMethod method = listener.method();

                    // Verify that the method has equal to or less than the number of parameters as the listener.
                    final List<? extends VariableElement> methodParameters = executableElement.getParameters();
                    if (methodParameters.size() > method.parameters().length) {
                        error(element, "@%s methods can have at most %s parameter(s). (%s.%s)",
                                annotationClass.getSimpleName(), method.parameters().length,
                                enclosingElement.getQualifiedName(), element.getSimpleName());
                        hasError = true;
                    }

                    // Verify method return type matches the listener.
                    TypeMirror returnType = executableElement.getReturnType();
                    if (returnType instanceof TypeVariable) {
                        TypeVariable typeVariable = (TypeVariable) returnType;
                        returnType = typeVariable.getUpperBound();
                    }
                    if (!returnType.toString().equals(method.returnType())) {
                        error(element, "@%s methods must have a '%s' return type. (%s.%s)",
                                annotationClass.getSimpleName(), method.returnType(),
                                enclosingElement.getQualifiedName(), element.getSimpleName());
                        hasError = true;
                    }

                    if (hasError) {
                        continue;
                    }

                    Parameter[] parameters = Parameter.NONE;
                    if (!methodParameters.isEmpty()) {
                        parameters = new Parameter[methodParameters.size()];
                        BitSet methodParameterUsed = new BitSet(methodParameters.size());
                        String[] parameterTypes = method.parameters();
                        for (int i = 0; i < methodParameters.size(); i++) {
                            VariableElement methodParameter = methodParameters.get(i);
                            TypeMirror methodParameterType = methodParameter.asType();
                            if (methodParameterType instanceof TypeVariable) {
                                TypeVariable typeVariable = (TypeVariable) methodParameterType;
                                methodParameterType = typeVariable.getUpperBound();
                            }

                            for (int j = 0; j < parameterTypes.length; j++) {
                                if (methodParameterUsed.get(j)) {
                                    continue;
                                }
                                if (isSubtypeOfType(methodParameterType, parameterTypes[j])) {
                                    parameters[i] = new Parameter(j, methodParameterType.toString());
                                    methodParameterUsed.set(j);
                                    break;
                                }
                            }
                            if (parameters[i] == null) {
                                StringBuilder builder = new StringBuilder();
                                builder.append("Unable to match @")
                                        .append(annotationClass.getSimpleName())
                                        .append(" method arguments. (")
                                        .append(enclosingElement.getQualifiedName())
                                        .append('.')
                                        .append(element.getSimpleName())
                                        .append(')');
                                for (int j = 0; j < parameters.length; j++) {
                                    Parameter parameter = parameters[j];
                                    builder.append("\n\n  Parameter #")
                                            .append(j + 1)
                                            .append(": ")
                                            .append(methodParameters.get(j).asType().toString())
                                            .append("\n    ");
                                    if (parameter == null) {
                                        builder.append("did not match any listener parameters");
                                    } else {
                                        builder.append("matched listener parameter #")
                                                .append(parameter.getListenerPosition() + 1)
                                                .append(": ")
                                                .append(parameter.getType());
                                    }
                                }
                                builder.append("\n\nMethods may have up to ")
                                        .append(method.parameters().length)
                                        .append(" parameter(s):\n");
                                for (String parameterType : method.parameters()) {
                                    builder.append("\n  ").append(parameterType);
                                }
                                builder.append(
                                        "\n\nThese may be listed in any order but will be searched for from top to bottom.");
                                error(executableElement, builder.toString());
                                continue;
                            }
                        }
                    }

                    final ListenerBinding binding = new ListenerBinding(name, Arrays.asList(parameters));
                    final StringBuilder builder = new StringBuilder();
                    builder.append("// Generated code from MultiLoader. Do not modify!\n");
                    builder.append("package ").append(classPackage).append(";\n\n");
                    builder.append("import android.app.LoaderManager;\n");
                    builder.append("import android.os.Bundle;\n");
                    builder.append("import android.content.Loader;\n");
                    builder.append("public class ").append(className).append(" implements LoaderManager.LoaderCallbacks<Object> {\n");
                    builder.append("    private " + enclosingClassName +" mActivity;\n");
                    builder.append('\n');
                    builder.append("    public void setActivity(" + enclosingClassName + " activity) {\n");
                    builder.append("        mActivity = activity;\n");
                    builder.append("    }\n");
                    builder.append('\n');
                    builder.append("    @Override\n");
                    builder.append("    public Loader<Object> onCreateLoader(int id, Bundle args) {\n");
                    builder.append("        return mActivity." + binding.getName() + "(id, args);\n");
                    builder.append("    }\n");
                    builder.append('\n');
                    builder.append("    @Override\n");
                    builder.append("    public void onLoadFinished(Loader<Object> loader, Object data) {\n");
                    builder.append("    }\n");
                    builder.append('\n');
                    builder.append("    @Override\n");
                    builder.append("    public void onLoaderReset(Loader<Object> loader) {\n");
                    builder.append("    }\n");
                    builder.append('\n');
                    builder.append("}\n");

                    try {
                        JavaFileObject jfo = filer.createSourceFile(classPackage + "." + className);
                        Writer writer = jfo.openWriter();
                        writer.write(builder.toString());
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        error(element, "Unable to write injector for type %s: %s", element, e.getMessage());
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return true;
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != ElementKind.CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(Modifier.PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith(ANDROID_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith(JAVA_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (otherType.equals(typeMirror.toString())) {
            return true;
        }
        if (!(typeMirror instanceof DeclaredType)) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
