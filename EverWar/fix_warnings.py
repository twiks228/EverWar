import os
import re

BASE = "EverWar/src/main/java/com/ever/war"

fixes = {
    # LanguageManager.java — убираем неиспользуемый импорт
    "config/LanguageManager.java": [
        ("import java.util.logging.Level;\n", ""),
    ],

    # ClanMenuGUI.java — убираем неиспользуемый импорт
    "gui/ClanMenuGUI.java": [
        ("import com.ever.war.utils.ColorUtil;\n", ""),
    ],

    # MembersGUI.java — убираем неиспользуемый импорт
    "gui/MembersGUI.java": [
        ("import java.util.Comparator;\n", ""),
    ],

    # GUIListener.java — убираем неиспользуемый импорт
    "listeners/GUIListener.java": [
        ("import org.bukkit.inventory.InventoryHolder;\n", ""),
    ],

    # PlayerListener.java — убираем неиспользуемый импорт
    "listeners/PlayerListener.java": [
        ("import com.ever.war.utils.ColorUtil;\n", ""),
    ],

    # ClanManager.java — убираем неиспользуемый импорт
    "managers/ClanManager.java": [
        ("import java.time.Instant;\n", ""),
    ],

    # ItemBuilder.java — убираем неиспользуемый импорт
    "utils/ItemBuilder.java": [
        ("import java.util.Arrays;\n", ""),
    ],

    # MessageUtil.java — убираем неиспользуемый импорт
    "utils/MessageUtil.java": [
        ("import net.md_5.bungee.api.chat.ComponentBuilder;\n", ""),
    ],

    # TimeUtil.java — убираем неиспользуемый импорт
    "utils/TimeUtil.java": [
        ("import java.time.Duration;\n", ""),
    ],
}

def apply_fixes():
    fixed = 0
    for rel_path, replacements in fixes.items():
        full_path = os.path.join(BASE, rel_path)
        if not os.path.exists(full_path):
            print(f"⚠️  Файл не найден: {full_path}")
            continue

        with open(full_path, "r", encoding="utf-8") as f:
            content = f.read()

        original = content
        for old, new in replacements:
            content = content.replace(old, new)

        if content != original:
            with open(full_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"✅ Исправлен: {rel_path}")
            fixed += 1
        else:
            print(f"⏭️  Без изменений: {rel_path}")

    print(f"\n✅ Исправлено файлов: {fixed}")

if __name__ == "__main__":
    apply_fixes()