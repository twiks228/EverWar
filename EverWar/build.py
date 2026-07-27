import os
import sys
import subprocess
import urllib.request
import shutil
import glob

# ==================== НАСТРОЙКИ ====================
PROJECT_DIR = r"D:\Projects\VSCode\Plugins\EverWar\EverWar"
# Путь к серверу (если хочешь автокопирование)
SERVER_PLUGINS = r"D:\Server\plugins"
# ====================================================

def find_java():
    """Ищет Java 21 на компьютере"""
    
    # Сначала пробуем JAVA_HOME
    java_home = os.environ.get("JAVA_HOME", "")
    if java_home and os.path.exists(os.path.join(java_home, "bin", "java.exe")):
        print(f"✅ JAVA_HOME найден: {java_home}")
        return java_home
    
    # Популярные места установки Java
    possible_paths = []
    
    # Program Files
    for pf in [r"C:\Program Files\Java", r"C:\Program Files\Eclipse Adoptium",
               r"C:\Program Files\Microsoft", r"C:\Program Files\BellSoft",
               r"C:\Program Files\Amazon Corretto", r"C:\Program Files\Semeru",
               r"C:\Program Files\OpenJDK", r"C:\Program Files\Zulu",
               r"C:\Program Files (x86)\Java"]:
        if os.path.exists(pf):
            for item in os.listdir(pf):
                full = os.path.join(pf, item)
                java_exe = os.path.join(full, "bin", "java.exe")
                if os.path.exists(java_exe):
                    possible_paths.append(full)
    
    # IntelliJ bundled JDK
    idea_paths = [
        r"C:\Program Files\JetBrains",
        os.path.expanduser(r"~\.jdks"),
        os.path.expanduser(r"~\AppData\Local\JetBrains"),
    ]
    for idea_base in idea_paths:
        if os.path.exists(idea_base):
            for root, dirs, files in os.walk(idea_base):
                if "java.exe" in files and "bin" in root:
                    jdk_home = os.path.dirname(root)
                    if jdk_home not in possible_paths:
                        possible_paths.append(jdk_home)
                if root.count(os.sep) - idea_base.count(os.sep) > 4:
                    break
    
    # Scoop
    scoop_java = os.path.expanduser(r"~\scoop\apps\openjdk21\current")
    if os.path.exists(scoop_java):
        possible_paths.append(scoop_java)
    
    # Winget/MSIX
    local_app = os.path.expanduser(r"~\AppData\Local\Programs")
    if os.path.exists(local_app):
        for item in os.listdir(local_app):
            full = os.path.join(local_app, item)
            java_exe = os.path.join(full, "bin", "java.exe")
            if os.path.exists(java_exe):
                possible_paths.append(full)
    
    if not possible_paths:
        return None
    
    # Выбираем Java 21 если есть, иначе любую последнюю
    java21_paths = [p for p in possible_paths if "21" in p]
    
    if java21_paths:
        chosen = java21_paths[0]
    else:
        chosen = possible_paths[-1]  # берём последнюю
    
    print(f"✅ Java найдена: {chosen}")
    
    # Проверяем версию
    try:
        result = subprocess.run(
            [os.path.join(chosen, "bin", "java.exe"), "-version"],
            capture_output=True, text=True
        )
        version_output = result.stderr or result.stdout
        print(f"   Версия: {version_output.splitlines()[0] if version_output else 'unknown'}")
    except Exception:
        pass
    
    return chosen

def download_gradle_wrapper(project_dir):
    """Скачивает gradle-wrapper.jar"""
    gradle_dir = os.path.join(project_dir, "gradle", "wrapper")
    os.makedirs(gradle_dir, exist_ok=True)
    
    jar_path = os.path.join(gradle_dir, "gradle-wrapper.jar")
    
    if os.path.exists(jar_path) and os.path.getsize(jar_path) > 10000:
        print(f"✅ gradle-wrapper.jar уже есть ({os.path.getsize(jar_path)} байт)")
        return jar_path
    
    print("📥 Скачиваем gradle-wrapper.jar...")
    
    urls = [
        # Прямая ссылка на бинарник из Maven
        "https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.5/gradle-wrapper-8.5.jar",
        # GitHub releases
        "https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar",
    ]
    
    for url in urls:
        try:
            print(f"  Загружаем с: {url}")
            urllib.request.urlretrieve(url, jar_path)
            size = os.path.getsize(jar_path)
            if size > 10000:
                print(f"  ✅ Скачан! ({size} байт)")
                return jar_path
            else:
                print(f"  ⚠️  Файл слишком маленький ({size} байт), пробуем следующий...")
                os.remove(jar_path)
        except Exception as e:
            print(f"  ❌ Ошибка: {e}")
    
    return None

def create_gradle_wrapper_files(project_dir):
    """Создаёт файлы Gradle Wrapper"""
    gradle_dir = os.path.join(project_dir, "gradle", "wrapper")
    os.makedirs(gradle_dir, exist_ok=True)
    
    # gradle-wrapper.properties
    props_file = os.path.join(gradle_dir, "gradle-wrapper.properties")
    with open(props_file, "w", newline='\n') as f:
        f.write(
            "distributionBase=GRADLE_USER_HOME\n"
            "distributionPath=wrapper/dists\n"
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip\n"
            "networkTimeout=10000\n"
            "validateDistributionUrl=true\n"
            "zipStoreBase=GRADLE_USER_HOME\n"
            "zipStorePath=wrapper/dists\n"
        )
    print(f"✅ gradle-wrapper.properties создан")
    
    # gradlew.bat
    bat_file = os.path.join(project_dir, "gradlew.bat")
    with open(bat_file, "w", newline='\r\n') as f:
        f.write(
            "@rem Gradle startup script for Windows\n"
            "@if \"%DEBUG%\"==\"\" @echo off\n"
            "setlocal\n"
            "set DIRNAME=%~dp0\n"
            "if \"%DIRNAME%\"==\"\" set DIRNAME=.\n"
            "set CLASSPATH=%DIRNAME%gradle\\wrapper\\gradle-wrapper.jar\n"
            "if defined JAVA_HOME (\n"
            "  set JAVA_EXE=%JAVA_HOME%\\bin\\java.exe\n"
            ") else (\n"
            "  set JAVA_EXE=java.exe\n"
            ")\n"
            "if not exist \"%JAVA_EXE%\" set JAVA_EXE=java.exe\n"
            "\"%JAVA_EXE%\" -classpath \"%CLASSPATH%\" "
            "org.gradle.wrapper.GradleWrapperMain %*\n"
            "if \"%ERRORLEVEL%\"==\"0\" goto mainEnd\n"
            ":fail\n"
            "exit /b 1\n"
            ":mainEnd\n"
            "endlocal\n"
        )
    print(f"✅ gradlew.bat создан")

def build_with_gradle(project_dir, java_home):
    """Собирает проект через Gradle"""
    
    env = os.environ.copy()
    env["JAVA_HOME"] = java_home
    env["PATH"] = os.path.join(java_home, "bin") + os.pathsep + env.get("PATH", "")
    
    # Пробуем разные способы запуска
    
    # Способ 1: gradlew.bat
    gradlew_bat = os.path.join(project_dir, "gradlew.bat")
    if os.path.exists(gradlew_bat):
        print("\n🔨 Запускаем сборку через gradlew.bat...")
        result = subprocess.run(
            ["cmd", "/c", "gradlew.bat", "jar", "--info"],
            cwd=project_dir,
            env=env,
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='replace'
        )
        
        if result.returncode == 0:
            return True, result.stdout
        else:
            print(f"⚠️  gradlew.bat не сработал, пробуем напрямую...")
    
    # Способ 2: java -jar gradle-wrapper.jar
    jar_path = os.path.join(project_dir, "gradle", "wrapper", "gradle-wrapper.jar")
    if os.path.exists(jar_path):
        print("\n🔨 Запускаем сборку напрямую через jar...")
        java_exe = os.path.join(java_home, "bin", "java.exe")
        result = subprocess.run(
            [java_exe, "-jar", jar_path, "jar"],
            cwd=project_dir,
            env=env,
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='replace'
        )
        
        if result.returncode == 0:
            return True, result.stdout
        else:
            return False, result.stdout + "\n" + result.stderr
    
    return False, "Не найден ни gradlew.bat ни gradle-wrapper.jar"

def find_built_jar(project_dir):
    """Ищет собранный JAR файл"""
    patterns = [
        os.path.join(project_dir, "build", "libs", "*.jar"),
        os.path.join(project_dir, "build", "libs", "EverWar*.jar"),
    ]
    
    for pattern in patterns:
        jars = glob.glob(pattern)
        # Исключаем sources и javadoc
        jars = [j for j in jars if "sources" not in j and "javadoc" not in j]
        if jars:
            return max(jars, key=os.path.getmtime)
    
    return None

def main():
    print("="*60)
    print("    🔨 EverWar — Сборка плагина")
    print("="*60)
    print()
    
    # Проверяем что проект существует
    if not os.path.exists(PROJECT_DIR):
        print(f"❌ Папка проекта не найдена: {PROJECT_DIR}")
        print("Проверь путь в переменной PROJECT_DIR в начале скрипта")
        input("\nНажми Enter для выхода...")
        sys.exit(1)
    
    print(f"📁 Проект: {PROJECT_DIR}")
    
    # Шаг 1: Найти Java
    print("\n[1/4] Поиск Java...")
    java_home = find_java()
    
    if not java_home:
        print("\n❌ Java не найдена!")
        print("\nУстанови Java 21:")
        print("  1. Скачай: https://adoptium.net/temurin/releases/?version=21")
        print("  2. Установи")
        print("  3. Перезапусти этот скрипт")
        print("\nИли укажи путь вручную:")
        manual = input("Введи путь к JDK (например C:\\Program Files\\Java\\jdk-21): ").strip()
        if manual and os.path.exists(os.path.join(manual, "bin", "java.exe")):
            java_home = manual
        else:
            print("❌ Путь не найден")
            input("\nНажми Enter для выхода...")
            sys.exit(1)
    
    # Шаг 2: Создать Gradle Wrapper файлы
    print("\n[2/4] Создание Gradle Wrapper...")
    create_gradle_wrapper_files(PROJECT_DIR)
    
    # Шаг 3: Скачать gradle-wrapper.jar
    print("\n[3/4] Получение gradle-wrapper.jar...")
    jar_path = download_gradle_wrapper(PROJECT_DIR)
    
    if not jar_path:
        print("\n❌ Не удалось скачать gradle-wrapper.jar")
        print("\nСкачай вручную:")
        print("  URL: https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.5/gradle-wrapper-8.5.jar")
        print(f"  Сохрани как: {PROJECT_DIR}\\gradle\\wrapper\\gradle-wrapper.jar")
        input("\nНажми Enter для выхода...")
        sys.exit(1)
    
    # Шаг 4: Сборка
    print("\n[4/4] Сборка проекта...")
    print(f"  JAVA_HOME = {java_home}")
    print("  Это может занять 1-3 минуты при первом запуске")
    print("  (Gradle скачает зависимости)")
    
    success, output = build_with_gradle(PROJECT_DIR, java_home)
    
    # Показываем важные строки из вывода
    important_lines = []
    for line in output.split('\n'):
        if any(keyword in line for keyword in [
            'BUILD', 'ERROR', 'FAILED', 'error:', 'warning:',
            'JAR', ':jar', 'Task', 'Downloading', 'Resolving'
        ]):
            important_lines.append(line)
    
    if important_lines:
        print("\n--- Вывод сборки ---")
        for line in important_lines[-30:]:  # последние 30 важных строк
            print(f"  {line}")
    
    if success:
        jar_file = find_built_jar(PROJECT_DIR)
        
        print("\n" + "="*60)
        print("✅ СБОРКА УСПЕШНА!")
        print("="*60)
        
        if jar_file:
            size = os.path.getsize(jar_file)
            print(f"📦 JAR файл: {jar_file}")
            print(f"   Размер: {size:,} байт ({size//1024} KB)")
            
            # Копируем на сервер если папка существует
            if os.path.exists(SERVER_PLUGINS):
                dest = os.path.join(SERVER_PLUGINS, "EverWar.jar")
                shutil.copy2(jar_file, dest)
                print(f"\n✅ Скопирован на сервер: {dest}")
                print("   Перезапусти сервер для применения изменений")
            else:
                print(f"\n📋 Скопируй вручную в папку plugins/ сервера")
        else:
            print("⚠️  JAR не найден, но сборка прошла успешно")
            print(f"   Проверь папку: {PROJECT_DIR}\\build\\libs\\")
    else:
        print("\n" + "="*60)
        print("❌ СБОРКА НЕ УДАЛАСЬ")
        print("="*60)
        print("\nПолный вывод ошибок:")
        print(output[-3000:] if len(output) > 3000 else output)
        print("\nВозможные решения:")
        print("  1. Проверь что все .java файлы без ошибок (severity 8)")
        print("  2. Открой проект в IntelliJ IDEA — там удобнее смотреть ошибки")
        print("  3. Убедись что Java 21 установлена корректно")
    
    print()
    input("Нажми Enter для выхода...")

if __name__ == "__main__":
    main()