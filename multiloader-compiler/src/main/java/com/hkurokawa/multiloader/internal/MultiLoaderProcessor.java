package com.hkurokawa.multiloader.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.hkurokawa.multiloader.OnCreateLoader")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class MultiLoaderProcessor extends AbstractProcessor {
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
//        System.out.println("Running processor [" + this + "].");
        for (TypeElement a : annotations) {
            try {
                JavaFileObject jfo = filer.createSourceFile("com.hkurokawa.example.multiloader.MainActivity$$LOADER_0");
                Writer writer = jfo.openWriter();
                writer.write("package com.hkurokawa.example.multiloader;");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(a, "Unable to write injector for type %s: %s", a, e.getMessage());
            }
        }
        return true;
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
