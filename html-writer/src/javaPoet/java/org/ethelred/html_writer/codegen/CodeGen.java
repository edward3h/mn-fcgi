package org.ethelred.html_writer.codegen;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

public class CodeGen {
    public static void main(String[] args)
    {
        new CodeGen(Paths.get(args[0])).generate();
    }

    private static final String PACKAGE = "org.ethelred.html_writer";
    public static final ClassName HTML_WRITER_CLASS = ClassName.get(PACKAGE, "HtmlWriter");
    public static final ClassName ATTRIBUTES_CLASS = ClassName.get(PACKAGE, "Attributes");
    private static final Set<String> TAG_NAMES = Set.of(
        "a",
        "div", "span",
        "html", "head", "title", "body",
            "ul", "li"
    );

    private static final Set<String> ATTRIBUTE_NAMES = Set.of(
            // "class" is special cased below
            "id", "href"
    );

    private final Path outputDir;
    private TypeSpec.Builder tags;
    private TypeSpec.Builder attributes;
    private static final Set<TypeName> INNER_TYPES = Set.of(
            ArrayTypeName.of(HTML_WRITER_CLASS),
            ParameterizedTypeName.get(ClassName.get(Iterable.class), HTML_WRITER_CLASS),
            ParameterizedTypeName.get(ClassName.get(Stream.class), HTML_WRITER_CLASS)
    );

    public CodeGen(Path outputDir)
    {
        this.outputDir = outputDir;
    }


    private void generate()
    {
        tags = TypeSpec.classBuilder("Tags")
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(PACKAGE, "BaseTags"));
        attributes = TypeSpec.classBuilder("Attributes")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(ClassName.get(PACKAGE, "BaseAttributes"), ATTRIBUTES_CLASS));

        TAG_NAMES.forEach(this::addTag);

        // special case for class attribute for various reasons
        addClassAttribute();

        ATTRIBUTE_NAMES.forEach(this::addAttribute);

        JavaFile tagsFile = JavaFile.builder(PACKAGE, tags.build()).build();
        JavaFile attributesFile = JavaFile.builder(PACKAGE, attributes.build()).build();
        try
        {
            tagsFile.writeTo(outputDir);
            attributesFile.writeTo(outputDir);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private void addClassAttribute()
    {
        MethodSpec method = MethodSpec.methodBuilder("className")
                .addModifiers(Modifier.PUBLIC)
                .returns(ATTRIBUTES_CLASS)
                .addParameter(TypeName.get(String.class), "value")
                .addStatement("attr($S, $L, (a, b) -> a + \" \" + b)", "class", "value")
                .addStatement("return this")
                .build();
        attributes.addMethod(method);

        method = MethodSpec.methodBuilder("className")
                .addModifiers(Modifier.PUBLIC)
                .returns(ATTRIBUTES_CLASS)
                .addParameter(TypeName.get(String.class), "value")
                .addStatement("Attributes attributes = new Attributes()")
                .addStatement("return attributes.className($L)", "value")
                .build();
        tags.addMethod(method);
    }

    private void addAttribute(String attributeName)
    {
        MethodSpec method = MethodSpec.methodBuilder(attributeName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ATTRIBUTES_CLASS)
                .addParameter(TypeName.get(String.class), "value")
                .addStatement("attr($S, $L)", attributeName, "value")
                .addStatement("return this")
                .build();
        attributes.addMethod(method);

        method = MethodSpec.methodBuilder(attributeName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ATTRIBUTES_CLASS)
                .addParameter(TypeName.get(String.class), "value")
                .addStatement("Attributes a = new Attributes()")
                .addStatement("return a.$L($L)", attributeName, "value")
                .build();
        tags.addMethod(method);
    }

    private void addTag(String tagName)
    {
        INNER_TYPES.forEach(innerType -> {
            MethodSpec method = MethodSpec.methodBuilder(tagName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(HTML_WRITER_CLASS)
                    .addParameter(ATTRIBUTES_CLASS, "attributes")
                    .addParameter(innerType, "inner")
                    .varargs(innerType instanceof ArrayTypeName)
                    .addStatement("return tag($S, $L, $L)", tagName, "attributes", "inner")
                    .build();
            tags.addMethod(method);


            method = MethodSpec.methodBuilder(tagName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(HTML_WRITER_CLASS)
                    .addParameter(innerType, "inner")
                    .varargs(innerType instanceof ArrayTypeName)
                    .addStatement("return tag($S, $L, $L)", tagName, "Attributes.EMPTY", "inner")
                    .build();
            tags.addMethod(method);
        });
    }
}