import os
import re

impl_dir = r"d:\findfriends\user\src\main\java\com\phaithanhcong\user\service\impl"

java_files = [f for f in os.listdir(impl_dir) if f.endswith(".java")]

for file in java_files:
    file_path = os.path.join(impl_dir, file)
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # The current content has:
    # @RequiredArgsConstructor
    # @Service
    # import com.phaithanhcong.user.service.UserService;
    # public class UserServiceImpl ...
    
    # We want to move the import statement to the top block.
    
    # Extract the misplaced import
    import_match = re.search(r'import com\.phaithanhcong\.user\.service\.[\w]+;', content)
    if import_match:
        import_stmt = import_match.group(0)
        content = content.replace(import_stmt, "")
        # Add it right after the package declaration
        content = re.sub(r'^(package[\s\w\.]+;)', f"\\1\n\n{import_stmt}", content, flags=re.MULTILINE)
        
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)

print("Fixed syntax in Impl classes.")
