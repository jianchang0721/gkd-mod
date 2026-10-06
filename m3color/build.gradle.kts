// 内联的 m3color (Kyant0/m3color, Apache-2.0): material-color-utilities 的 Java 实现.
// 内联而不是走 JitPack, 避免构建时依赖第三方在线构建.
plugins {
    `java-library`
}

group = "com.kyant"
version = "2026.1"

// 上游源码使用 Java 14+ 的 switch 表达式/规则, 所以不能压到 11
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // 仅编译期注解; 运行期不需要, 且宿主已包含 androidx.annotation
    compileOnly("androidx.annotation:annotation:1.11.0")
    compileOnly("com.google.errorprone:error_prone_annotations:2.49.0")
}
