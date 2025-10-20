import os
import glob
import re
import shutil

# --- 配置 ---

# 1. 脚本的根目录（假设你把此脚本放在 deskpet-main 文件夹下）
#    os.path.dirname(__file__) 会获取脚本所在的当前目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 2. 你的资源文件夹路径
RESOURCES_DIR = os.path.join(BASE_DIR, 'client', 'resources', 'BA', 'seia')

# 3. 需要处理的动画文件夹（从 0001.png 开始的）
FOLDERS_TO_PROCESS = ['jump','knockdown','skill','attack','headache','idle_normal', 'idle_ignore', 'idle_sad']


def safe_batch_rename_frames():
    """
    使用一个临时目录，安全地将从 '0001.png' 开始的序列帧重命名为从 '0000.png' 开始。
    """
    print(f"脚本启动，资源根目录: {RESOURCES_DIR}\n")

    if not os.path.exists(RESOURCES_DIR):
        print(f"!! 错误: 找不到资源目录，请检查路径配置: {RESOURCES_DIR}")
        return

    for folder_name in FOLDERS_TO_PROCESS:
        folder_path = os.path.join(RESOURCES_DIR, folder_name)
        # 定义一个临时目录的路径
        temp_dir_path = os.path.join(folder_path, 'temp_rename_dir')

        if not os.path.isdir(folder_path):
            print(f"-- 跳过: 找不到文件夹 '{folder_name}'")
            continue

        print(f"=== 正在处理文件夹: {folder_name} ===")

        # 0. 清理：如果上次脚本失败，临时目录可能存在
        if os.path.exists(temp_dir_path):
            print(f"  - 发现残留的临时目录，正在清理...")
            try:
                shutil.rmtree(temp_dir_path)
                print(f"  - 清理完毕。")
            except Exception as e:
                print(f"  - !! 清理临时目录失败: {e}")
                print(f"  - !! 请手动删除 '{temp_dir_path}' 文件夹后再试。")
                continue

        # 1. 创建临时目录
        try:
            os.makedirs(temp_dir_path)
            print(f"  - 1. 创建临时目录: {temp_dir_path}")
        except Exception as e:
            print(f"  - !! 无法创建临时目录: {e}")
            continue

        # 2. 查找所有 .png 文件
        file_pattern = os.path.join(folder_path, '[0-9][0-9][0-9][0-9].png')
        png_files = glob.glob(file_pattern)

        if not png_files:
            print("  - 没找到匹配 '0000.png' 格式的文件，跳过。")
            shutil.rmtree(temp_dir_path)  # 清理空目录
            continue

        # 检查是否已经重命名过
        if any(os.path.basename(f) == '0000.png' for f in png_files):
            print("  - 发现 '0000.png'，似乎已重命名过，跳过此文件夹。")
            shutil.rmtree(temp_dir_path)  # 清理空目录
            continue

        print(f"  - 2. 找到 {len(png_files)} 个文件，准备移动到临时目录并重命名...")
        total_moved_to_temp = 0

        # 3. 移动并重命名到临时目录
        #    (例如: .../idle_normal/0096.png -> .../idle_normal/temp_rename_dir/0095.png)
        for old_path in png_files:
            try:
                file_name = os.path.basename(old_path)
                number_str = file_name.split('.')[0]
                current_number = int(number_str)

                if current_number == 0:
                    continue  # 不处理 0000.png (虽然上面已经检查过了)

                new_number = current_number - 1
                new_file_name = str(new_number).zfill(4) + '.png'

                # 新路径是在临时目录里
                temp_new_path = os.path.join(temp_dir_path, new_file_name)

                # 使用 move (rename)
                shutil.move(old_path, temp_new_path)
                total_moved_to_temp += 1

            except ValueError:
                print(f"  - 跳过（非数字文件名）: {file_name}")
            except Exception as e:
                print(f"  - !! 移动到临时目录时出错: {e}")
                break

        print(f"  - 3. 成功移动 {total_moved_to_temp} 个文件到临时目录。")

        # 4. 从临时目录移回
        #    (例如: .../idle_normal/temp_rename_dir/0095.png -> .../idle_normal/0095.png)
        print(f"  - 4. 准备移回文件...")
        total_moved_back = 0
        temp_files = glob.glob(os.path.join(temp_dir_path, '*.png'))

        for temp_file_path in temp_files:
            try:
                file_name = os.path.basename(temp_file_path)
                final_path = os.path.join(folder_path, file_name)

                shutil.move(temp_file_path, final_path)
                total_moved_back += 1
            except Exception as e:
                print(f"  - !! 移回文件时出错: {e}")
                break

        print(f"  - 5. 成功移回 {total_moved_back} 个文件。")

        # 5. 删除临时目录
        try:
            shutil.rmtree(temp_dir_path)
            print(f"  - 6. 成功删除临时目录。")
        except Exception as e:
            print(f"  - !! 删除临时目录失败: {e}")

        print(f"--- {folder_name} 处理完毕 ---\n")

    print("===== 脚本执行完毕 =====")


# --- 运行 ---
if __name__ == "__main__":
    safe_batch_rename_frames()