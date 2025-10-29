# Alternativa: Usar Java 11 em vez de Java 17

Se o auto-download do JDK 17 não funcionar ou se preferir usar Java 11, faça estas alterações:

## Em `app/build.gradle.kts`:

### Mudar as linhas 30-37 de:
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}
```

### Para:
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlinOptions {
    jvmTarget = "11"
}
```

### E mudar a linha 44-46 de:
```kotlin
kotlin {
    jvmToolchain(17)
}
```

### Para:
```kotlin
kotlin {
    jvmToolchain(11)
}
```

## Verificar JDK instalado:

No terminal/PowerShell:
```bash
java -version
```

Java 11 ou superior funciona perfeitamente com este projeto Android.
