package io.github.sgpublic.exsp.core

import com.squareup.javapoet.*
import io.github.sgpublic.exsp.ExPreferenceProcessor
import io.github.sgpublic.exsp.annotations.ExSharedPreference
import io.github.sgpublic.exsp.annotations.ExValue
import io.github.sgpublic.exsp.util.SharedPreferenceType
import io.github.sgpublic.exsp.util.getterName
import io.github.sgpublic.exsp.util.setterName
import io.github.sgpublic.exsp.util.supported
import java.util.*
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

object PreferenceCompiler {
    fun apply(env: RoundEnvironment) {
        for (element: Element in env.getElementsAnnotatedWith(ExSharedPreference::class.java)) {
            if (element !is TypeElement) {
                continue
            }
            applySingle(element)
        }
    }

    private fun applySingle(element: TypeElement) {
        val anno = element.getAnnotation(ExSharedPreference::class.java)

        val originType = ClassName.get(element)
        val origin = element.simpleName.toString()
        val spName = "\"" + anno.name + "\""

        val pkg = element.qualifiedName.let {
            val tmp = it.substring(0, it.length - origin.length)
            if (tmp.last() == '.') {
                return@let tmp.substring(0, tmp.length - 1)
            } else {
                return@let tmp
            }
        }

        val impl = TypeSpec.classBuilder(origin + "_Impl")
            .superclass(originType)
            .addModifiers(Modifier.PUBLIC)

        MethodSpec.methodBuilder("getSharedPreference")
            .addModifiers(Modifier.PRIVATE)
            .addStatement("return \$T.getSharedPreference($spName, ${anno.mode})",
                ExPreferenceProcessor.ExPreference
            )
            .returns(ClassName.get(ExPreferenceProcessor.SharedPreferences))
            .let {
                impl.addMethod(it.build())
            }

        MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addStatement("return super.hashCode()")
            .returns(Int::class.java)
            .let {
                impl.addMethod(it.build())
            }

        MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Object::class.java, "o")
            .addAnnotation(Override::class.java)
            .addStatement("return super.equals(o)")
            .returns(Boolean::class.java)
            .let {
                impl.addMethod(it.build())
            }

        val save = MethodSpec.methodBuilder("save")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(originType, "data")
            .returns(TypeName.VOID)

        for (field: Element in element.enclosedElements) {
            val defVal = field.getAnnotation(ExValue::class.java)?.defVal
            if (field !is VariableElement) {
                continue
            }
            if (field.modifiers.contains(Modifier.FINAL)) {
                continue
            }
            val type = ClassName.get(field.asType())

            val name = field.getAnnotation(ExValue::class.java).key.takeIf { it != "" }
                ?: field.simpleName.toString().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            val conf: String = "\"" + name + "\""

            val getter = MethodSpec.methodBuilder(field.getterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(type)
            getter.addStatement("SharedPreferences sp = getSharedPreference()")

            val setter = MethodSpec.methodBuilder(field.setterName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(type, "value")
                .addStatement("SharedPreferences.Editor editor = getSharedPreference().edit()")


            var convertedType = type
            if (type.supported()) {
                setter.addStatement("\$T converted = value", type)
                getter.addStatement("\$T origin", type)
            } else {
                val convertedElement = ConverterCompiler.getTarget(ExPreferenceProcessor.asElement(field.asType())!!)
                setter.addStatement("\$T converted = \$T.toPreference(\$T.class, value)",
                    convertedElement, ExPreferenceProcessor.ExConverters, type)
                convertedType = ClassName.get(convertedElement)
                getter.addStatement("\$T origin", convertedType)
            }

            when (SharedPreferenceType.of(convertedType)) {
                SharedPreferenceType.BOOLEAN -> {
                    getter.addStatement("origin = sp.getBoolean($conf, $defVal)")
                    setter.addStatement("editor.putBoolean($conf, converted)")
                }
                SharedPreferenceType.INT -> {
                    getter.addStatement("origin = sp.getInt($conf, $defVal)")
                    setter.addStatement("editor.putInt($conf, converted)")
                }
                SharedPreferenceType.LONG -> {
                    getter.addStatement("origin = sp.getLong($conf, $defVal)")
                    setter.addStatement("editor.putLong($conf, converted)")
                }
                SharedPreferenceType.FLOAT -> {
                    getter.addStatement("origin = sp.getFloat($conf, $defVal)")
                    setter.addStatement("editor.putFloat($conf, value)")
                }
                SharedPreferenceType.STRING -> {
                    getter.addStatement("origin = sp.getString($conf, \"$defVal\")")
                    setter.addStatement("editor.putString($conf, converted)")
                }
                SharedPreferenceType.STRING_SET -> {
                    getter.addStatement("origin = sp.getStringSet($conf, \"$defVal\")")
                    setter.addStatement("editor.putStringSet($conf, converted)")
                }
            }
            if (type.supported()) {
                getter.addStatement("return origin")
            } else {
                getter.addStatement("return \$T.fromPreference(\$T.class, origin)",
                    ExPreferenceProcessor.ExConverters, type)
            }
            setter.addStatement("editor.apply()")

            save.addStatement("${field.setterName()}(data.${field.getterName()}())")

            impl.addMethod(getter.build())
            impl.addMethod(setter.build())
        }

        impl.addMethod(save.build())

        JavaFile.builder(pkg, impl.build())
            .build().writeTo(ExPreferenceProcessor.mFiler)
    }
}