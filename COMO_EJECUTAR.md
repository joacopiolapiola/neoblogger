# Guía de Ejecución de Neoblogger en PC

Esta guía explica paso a paso cómo ejecutar la aplicación **Neoblogger** en tu ordenador para probar y visualizar todas las funcionalidades implementadas (diseñador visual, editor de código con Emmet, vista previa interactiva, importación/exportación y gestor de proyectos).

---

## Opción 1: Ejecutar usando el emulador de Android Studio (Recomendado)

Android Studio incluye un emulador oficial (**Android Virtual Device - AVD**) que simula un teléfono celular completo en una ventana de tu PC.

### Pasos:

1. **Abrir el proyecto en Android Studio**:
   - Inicia **Android Studio**.
   - Haz clic en **Open** y selecciona la carpeta del proyecto:
     `C:\Users\deminwn\Desktop\blogproject`
   - Espera unos segundos a que Gradle sincronice el proyecto.

2. **Abrir el Administrador de Dispositivos (Device Manager)**:
   - En la barra lateral derecha o menú superior de Android Studio, haz clic en **Device Manager** (ícono de un teléfono celular).
   - Si no tienes ningún dispositivo virtual creado:
     - Haz clic en **Create Device** (+).
     - Selecciona un teléfono (por ejemplo, **Pixel 7** o **Pixel 8**).
     - Selecciona la versión de sistema Android (**API 34** o **API 35**) y presiona **Finish**.

3. **Iniciar el Emulador y Ejecutar la App**:
   - Presiona el botón de **Play (▶)** al lado del dispositivo virtual en el Device Manager para encender el celular virtual.
   - En la barra superior de Android Studio, asegúrate de que esté seleccionado el módulo `app` y tu emulador.
   - Haz clic en el botón verde **Run 'app' (▶)** o presiona `Shift + F10`.
   - Android Studio compilará el proyecto e instalará Neoblogger en la ventana del emulador en pantalla.

---

## Opción 2: Instalar el APK en cualquier Emulador de Android para PC

Si utilizas un emulador de Android para PC (como **BlueStacks**, **LDPlayer**, **NoxPlayer** o **Android Studio AVD**):

### Pasos:

1. **Localizar el APK compilado**:
   - El archivo ejecutable APK ya generado se encuentra en:
     `C:\Users\deminwn\Desktop\blogproject\app\build\outputs\apk\debug\app-debug.apk`

2. **Instalar en el emulador**:
   - Inicia tu emulador preferido.
   - Arrastra y suelta el archivo `app-debug.apk` dentro de la ventana del emulador.
   - *(O bien, usa el botón "Instalar APK / Install APK" del emulador y selecciona esa ruta).*

3. **Abrir la app**:
   - Haz clic en el icono de **Neoblogger** en la pantalla de inicio del emulador.

---

## Opción 3: Ejecutar comandos desde la Terminal / PowerShell

Si deseas compilar e instalar la app directamente desde la consola usando `adb`:

### Comandos en PowerShell:

```powershell
# 1. Configurar variables de entorno requeridas
$env:JAVA_HOME = "C:\Users\deminwn\.jdks\jdk-17\jdk-17.0.10+7"
$env:ANDROID_HOME = "C:\Users\deminwn\AppData\Local\Android\Sdk"
$env:ANDROID_USER_HOME = "C:\Users\deminwn\.android"
Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue

# 2. Compilar el APK Debug
$gradle = "C:\Users\deminwn\.gradle\gradle-8.9\gradle-8.9\bin\gradle.bat"
& $gradle :app:assembleDebug --console=plain

# 3. Instalar en un emulador o dispositivo conectado por USB
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk

# 4. Iniciar la aplicación
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n com.neoblogger.app/.MainActivity
```

---

## Opción 4: Visualizar el Diseñador desde las Vistas Previas de Compose (Compose Preview)

Android Studio permite inspeccionar componentes de la interfaz de usuario de forma estática e interactiva sin necesidad de abrir un emulador completo:

1. En Android Studio, abre cualquiera de las pantallas ubicadas en:
   `app/src/main/java/com/neoblogger/app/ui/screens/`
   (por ejemplo, `DashboardScreen.kt`, `CodeTabScreen.kt` o `DesignTabScreen.kt`).
2. En la esquina superior derecha del editor de código, cambia la vista de **Code** a **Split** o **Design**.
3. Verás la vista previa en vivo (`@Preview`) renderizada en el panel lateral de Android Studio.

---

## 📱 Qué podrás probar al abrir la aplicación:

- **Proyectos**: Crear un nuevo sitio web o importar una carpeta/ZIP estático.
- **Pestaña Design**: Probar el lienzo interactivo, seleccionar elementos con un toque, cambiar colores/fuentes en el Inspector y cambiar entre modo *Responsive Flow* y *Free Positioning*.
- **Pestaña Code**: Probar el editor de texto, presionar **Expand Emmet** para expandir abreviaturas HTML/CSS (ejemplo: escribir `div.box>h1+p` y presionar el botón) y gestionar archivos con el File Explorer.
- **Pestaña Preview**: Probar la vista previa interactiva del sitio web con JavaScript activado, controles Back/Forward/Reload y advertencias de enlaces externos.
