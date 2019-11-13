import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Compiler {

    private static final File JAVA_DIR = new File(System.getProperty("user.dir") + "/src/main/java");

    private final Set<String> basePackages;

    private final Map<Class<?>, File> classMap = new HashMap<>();

    public Compiler(String... basePackages) {
        this.basePackages = Arrays.stream(basePackages).collect(Collectors.toSet());
    }

    public void writeToFile(Class<?>... classes) throws Exception {
        System.out.println("java dir: " + JAVA_DIR.getAbsolutePath());
        FileUtils.deleteQuietly(JAVA_DIR);
        JAVA_DIR.mkdirs();
        for (Class<?> clazz : classes) {
            if (clazz.isInterface()) {
                writeToFile(clazz);
            }
        }
    }

    private void writeToFile(Class<?> clazz) throws Exception {
        if (isIgnored(clazz)) {
            return;
        }
        File javaFile = new File(JAVA_DIR, clazz.getName().replace('.', File.separatorChar).concat(".java"));
        javaFile.getParentFile().mkdirs();
        this.classMap.put(clazz, javaFile);
        if (clazz.isInterface()) {
            writetoFile4Interface(clazz, javaFile);
        } else if (clazz.isEnum()) {
            writetoFile4Enum(clazz, javaFile);
        } else {
            writetoFile4Pojo(clazz, javaFile);
        }
    }

    private void writeToFile(Type type) throws Exception {
        if (type instanceof Class) {
            writeToFile((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            writeToFile((ParameterizedType) type);
        }
    }

    private void writeToFile(ParameterizedType parameterizedType) throws Exception {
        writeToFile(parameterizedType.getRawType());
        for (Type type : parameterizedType.getActualTypeArguments()) {
            writeToFile(type);
        }
    }

    private void writetoFile4Interface(Class<?> clazz, File javaFile) throws Exception {
        if (!clazz.isInterface()) {
            return;
        }
        if (javaFile.exists()) {
            javaFile.delete();
        }
        try (FileWriter fw = new FileWriter(javaFile)) {
            BufferedWriter writer = new BufferedWriter(fw);
            doWritePackageName(writer, clazz);
            doWriteBegin4Interface(writer, clazz);
            doWriteContent4Interface(writer, clazz);
            doWriteEnd(writer);
            writer.flush();
        }
    }

    private void writetoFile4Enum(Class<?> clazz, File javaFile) throws Exception {
        if (!clazz.isEnum()) {
            return;
        }
        if (javaFile.exists()) {
            javaFile.delete();
        }
        try (FileWriter fw = new FileWriter(javaFile)) {
            BufferedWriter writer = new BufferedWriter(fw);
            doWritePackageName(writer, clazz);
            writer.newLine();
            writer.append("@lombok.Getter");
            writer.newLine();
            writer.append("@lombok.AllArgsConstructor");
            doWriteBegin4Enum(writer, clazz);
            doWriteContent4Enum(writer, clazz);
            doWriteEnd(writer);
            writer.flush();
        }
    }

    private void writetoFile4Pojo(Class<?> clazz, File javaFile) throws Exception {
        if (javaFile.exists()) {
            javaFile.delete();
        }
        try (FileWriter fw = new FileWriter(javaFile)) {
            BufferedWriter writer = new BufferedWriter(fw);
            doWritePackageName(writer, clazz);
            writer.newLine();
            writer.append("@lombok.Data");
            doWriteBegin4Class(writer, clazz);
            doWriteContent4Pojo(writer, clazz);
            doWriteEnd(writer);
            writer.flush();
        }
    }

    private void doWritePackageName(BufferedWriter writer, Class<?> clazz) throws Exception {
        Package pkg = clazz.getPackage();
        if (pkg != null) {
            writer.append("package ").append(pkg.getName()).append(";");
        }
    }

    private void doWriteBegin4Class(BufferedWriter writer, Class<?> clazz) throws Exception {
        writer.newLine();
        writer.append(clazz.toGenericString().replace(clazz.getName(), clazz.getSimpleName()));
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            writer.append(" extends ").append(superclass.getName());
            writeToFile(superclass);
        }
        Class<?>[] interfaceClasses = clazz.getInterfaces();
        if (interfaceClasses != null && interfaceClasses.length > 0) {
            writer.append(" implements ").append(Joiner.on(", ").join(Arrays.stream(interfaceClasses).map(Class::getName).iterator()));
        }
        writer.append(" {");
    }

    private void doWriteBegin4Interface(BufferedWriter writer, Class<?> clazz) throws Exception {
        writer.newLine();
//        writer.append("")
        writer.append(clazz.toGenericString().replace(clazz.getName(), clazz.getSimpleName()));
        Class<?>[] interfaceClasses = clazz.getInterfaces();
        if (interfaceClasses != null && interfaceClasses.length > 0) {
            writer.append(" implements ").append(Joiner.on(", ").join(Arrays.stream(interfaceClasses).map(Class::getName).iterator()));
        }
        writer.append(" {");
    }

    private void doWriteBegin4Enum(BufferedWriter writer, Class<?> clazz) throws Exception {
        writer.newLine();
//        writer.append("")
        writer.append(clazz.toGenericString().replace("final", "").replace(clazz.getName(), clazz.getSimpleName()));
        Class<?>[] interfaceClasses = clazz.getInterfaces();
        if (interfaceClasses != null && interfaceClasses.length > 0) {
            writer.append(" implements ").append(Joiner.on(", ").join(Arrays.stream(interfaceClasses).map(Class::getName).iterator()));
        }
        writer.append(" {");
    }

    private void doWriteEnd(BufferedWriter writer) throws Exception {
        writer.newLine();
        writer.append("}");
    }

    private void doWriteContent4Interface(BufferedWriter writer, Class<?> clazz) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            Type returnType = method.getGenericReturnType();
            //返回类型写入java文件
            writeToFile(returnType);

            writer.newLine();
            writer.append(returnType.getTypeName()).append(" ").append(method.getName());

            Type[] parameterTypes = method.getGenericParameterTypes();
            if (parameterTypes.length == 0) {
                writer.append("()");
            } else {
                StringBuilder parameterTypesBuilder = new StringBuilder();
                parameterTypesBuilder.append('(');
                for (int i = 0; i < parameterTypes.length; i++) {
                    Type parameterType = parameterTypes[i];
                    parameterTypesBuilder.append(parameterType.getTypeName()).append(' ').append("var").append(i).append(',');
                    //参数类型写入java文件
                    writeToFile(parameterType);
                }
                parameterTypesBuilder.setCharAt(parameterTypesBuilder.length() - 1, ')');
                writer.append(parameterTypesBuilder);
            }

            Type[] exceptionTypes = method.getGenericExceptionTypes();
            if (exceptionTypes.length > 0) {
                writer.append(" throws ").append(Joiner.on(", ").join(Arrays.stream(exceptionTypes).map(Type::getTypeName).iterator()));
                //异常类型写入java文件
                //TODO: writeToFile(parameterType);
            }
            writer.append(";");
        }
    }

    private void doWriteContent4Enum(BufferedWriter writer, Class<?> clazz) throws Exception {
        Object[] enums = (Object[]) clazz.getDeclaredMethod("values").invoke(clazz);
        if (enums == null || enums.length == 0) {
            return;
        }
        Method nameMethod = clazz.getMethod("name");
        Field[] fields = Arrays.stream(clazz.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).toArray(Field[]::new);
        if (fields == null || fields.length == 0) {
            for (Object item : enums) {
                writer.newLine();
                writer.append(nameMethod.invoke(item).toString()).append(",");
            }
            writer.newLine();
            writer.append(';');
        } else {
            for (Object item : enums) {
                writer.newLine();
                StringBuilder sb = new StringBuilder();
                for (Field field : fields) {
                    doWriteFieldValue(sb, item, field);
                    sb.append(',');
                }
                sb.setCharAt(sb.length() - 1, ')');
                writer.append(nameMethod.invoke(item).toString()).append("(").append(sb).append(",");
            }
            writer.newLine();
            writer.append(';');

            for (Field field : fields) {
                writer.newLine();
                writer.append(field.toGenericString().replace(clazz.getName().concat("."), "")).append(";");
//                main(field.getGenericType());
            }
        }
    }

    private void doWriteContent4Pojo(BufferedWriter writer, Class<?> clazz) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                if (field.getName().equals("serialVersionUID")) {
                    field.setAccessible(true);
                    writer.newLine();
                    writer.append("private static final long serialVersionUID = ").append(field.get(clazz).toString()).append("L;");
                }
            } else {
                writer.newLine();
                writer.append(field.toGenericString().replace(clazz.getName().concat("."), "")).append(";");
                writeToFile(field.getGenericType());
            }
        }
    }

    private void doWriteFieldValue(StringBuilder sb, Object obj, Field field) throws IllegalAccessException {
        field.setAccessible(true);
        Object value = field.get(obj);
        if (value instanceof CharSequence) {
            sb.append('"').append(value.toString()).append('"');
        } else if (value instanceof Long) {
            sb.append(value.toString()).append('L');
        } else if (value instanceof Short) {
            sb.append("(short)").append(value.toString());
        } else { //TODO
            sb.append(value.toString());
        }
    }

    private boolean isIgnored(Class<?> clazz) {
        if (this.classMap.containsKey(clazz)) {
            return true;
        }
        String className = clazz.getName();
        for (String basePackage : this.basePackages) {
            if (className.startsWith(basePackage)) {
                return false;
            }
        }
        return true;
    }
}
