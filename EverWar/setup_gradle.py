import os
import urllib.request
import shutil
import subprocess
import sys

def setup_and_build():
    """
    Настраивает Gradle Wrapper и собирает проект EverWar
    """
    
    # Определяем путь к проекту
    project_dir = r"D:\Projects\VSCode\Plugins\EverWar\EverWar"
    
    if not os.path.exists(project_dir):
        print(f"❌ Папка не найдена: {project_dir}")
        print("Укажи правильный путь к проекту!")
        sys.exit(1)
    
    print(f"📁 Проект: {project_dir}")
    
    # Создаём структуру Gradle Wrapper
    gradle_dir = os.path.join(project_dir, "gradle", "wrapper")
    os.makedirs(gradle_dir, exist_ok=True)
    
    # 1. gradle-wrapper.properties
    props = os.path.join(gradle_dir, "gradle-wrapper.properties")
    with open(props, "w", newline='\n') as f:
        f.write(
            "distributionBase=GRADLE_USER_HOME\n"
            "distributionPath=wrapper/dists\n"
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip\n"
            "networkTimeout=10000\n"
            "validateDistributionUrl=true\n"
            "zipStoreBase=GRADLE_USER_HOME\n"
            "zipStorePath=wrapper/dists\n"
        )
    print(f"✅ Создан: gradle-wrapper.properties")
    
    # 2. gradlew.bat для Windows
    bat = os.path.join(project_dir, "gradlew.bat")
    with open(bat, "w", newline='\r\n') as f:
        f.write(
            "@rem ##########################################################################\n"
            "@rem  Gradle startup script for Windows\n"
            "@rem ##########################################################################\n"
            "@if \"%DEBUG%\"==\"\" @echo off\n"
            "@rem Set local scope\n"
            "setlocal\n"
            "set DIRNAME=%~dp0\n"
            "if \"%DIRNAME%\"==\"\" set DIRNAME=.\n"
            "set APP_BASE_NAME=%~n0\n"
            "set APP_HOME=%DIRNAME%\n"
            "@rem Find java.exe\n"
            "if defined JAVA_HOME goto findJavaFromJavaHome\n"
            "set JAVA_EXE=java.exe\n"
            "%JAVA_EXE% -version >NUL 2>&1\n"
            "if \"%ERRORLEVEL%\"==\"0\" goto execute\n"
            "echo ERROR: JAVA_HOME is not set and no 'java' command found.\n"
            "goto fail\n"
            ":findJavaFromJavaHome\n"
            "set JAVA_HOME=%JAVA_HOME:\"=%\n"
            "set JAVA_EXE=%JAVA_HOME%/bin/java.exe\n"
            "if exist \"%JAVA_EXE%\" goto execute\n"
            "echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%\n"
            "goto fail\n"
            ":execute\n"
            "set CLASSPATH=%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar\n"
            "\"%JAVA_EXE%\" -classpath \"%CLASSPATH%\" "
            "org.gradle.wrapper.GradleWrapperMain %*\n"
            ":fail\n"
            "rem Set variable GRADLE_EXIT_CONSOLE if you need exit code.\n"
            "exit /b 1\n"
        )
    print(f"✅ Создан: gradlew.bat")
    
    # 3. Скачиваем gradle-wrapper.jar
    jar_path = os.path.join(gradle_dir, "gradle-wrapper.jar")
    
    if not os.path.exists(jar_path):
        print("📥 Скачиваем gradle-wrapper.jar...")
        
        # Несколько источников на случай недоступности
        urls = [
            "https://github.com/nicowillis/gradle-wrapper-jar/raw/main/gradle-wrapper.jar",
            "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar",
        ]
        
        downloaded = False
        for url in urls:
            try:
                print(f"  Пробуем: {url}")
                urllib.request.urlretrieve(url, jar_path)
                downloaded = True
                print(f"  ✅ Скачан!")
                break
            except Exception as e:
                print(f"  ⚠️  Ошибка: {e}")
        
        if not downloaded:
            print("\n❌ Не удалось скачать gradle-wrapper.jar автоматически.")
            print("Скачай вручную по одной из ссылок:")
            for url in urls:
                print(f"  {url}")
            print(f"\nПоложи файл сюда: {jar_path}")
            print("\nПосле этого запусти сборку вручную:")
            print(f"  cd {project_dir}")
            print("  .\\gradlew.bat jar")
            return
    else:
        print(f"✅ gradle-wrapper.jar уже существует")
    
    # 4. Запускаем сборку
    print("\n🔨 Запускаем сборку...")
    print(f"Директория: {project_dir}")
    
    result = subprocess.run(
        ["cmd", "/c", "gradlew.bat", "jar", "--stacktrace"],
        cwd=project_dir,
        capture_output=True,
        text=True,
        encoding='utf-8',
        errors='replace'
    )
    
    print("\n--- ВЫВОД GRADLE ---")
    print(result.stdout[-3000:] if len(result.stdout) > 3000 else result.stdout)
    
    if result.returncode == 0:
        jar_file = os.path.join(project_dir, "build", "libs", "EverWar-1.0.0.jar")
        print("\n" + "="*50)
        print("✅ СБОРКА УСПЕШНА!")
        print(f"📦 JAR: {jar_file}")
        print("\nСкопируй JAR в папку plugins/ сервера")
        
        # Автокопирование если хочешь
        server_plugins = r"D:\Server\plugins"
        if os.path.exists(server_plugins):
            dest = os.path.join(server_plugins, "EverWar-1.0.0.jar")
            shutil.copy2(jar_file, dest)
            print(f"✅ Скопирован в: {dest}")
    else:
        print("\n--- ОШИБКИ ---")
        print(result.stderr[-2000:] if len(result.stderr) > 2000 else result.stderr)
        print("\n❌ Сборка не удалась. Проверь ошибки выше.")

if __name__ == "__main__":
    setup_and_build()