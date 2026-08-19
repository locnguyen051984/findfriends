import os
import re

service_dir = r"d:\findfriends\user\src\main\java\com\phaithanhcong\user\service"
impl_dir = os.path.join(service_dir, "impl")
if not os.path.exists(impl_dir):
    os.makedirs(impl_dir)

java_files = [f for f in os.listdir(service_dir) if f.endswith("Service.java")]

method_pattern = re.compile(r'^\s*public\s+(?!class|interface)([\w<>, \?\[\]]+)\s+(\w+)\s*\((.*?)\)\s*(?:throws\s+[\w,\s]+)?\s*\{', re.MULTILINE)

for file in java_files:
    file_path = os.path.join(service_dir, file)
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    class_name = file.replace(".java", "")
    impl_name = class_name + "Impl"
    
    # Extract package and imports
    package_match = re.search(r'^package\s+[\w\.]+;', content, re.MULTILINE)
    package_decl = package_match.group(0) if package_match else ""
    
    imports = re.findall(r'^import\s+[\w\.]+;$', content, re.MULTILINE)
    import_decl = "\n".join(imports)
    
    # Find methods
    methods = method_pattern.findall(content)
    interface_methods = []
    for m in methods:
        return_type = m[0].strip()
        method_name = m[1].strip()
        args = m[2].strip()
        # Some methods might have generic return types, just use it raw
        interface_methods.append(f"    {return_type} {method_name}({args});")
    
    # Create Interface Content
    interface_content = f"{package_decl}\n\n{import_decl}\n\n"
    interface_content += f"public interface {class_name} {{\n"
    interface_content += "\n".join(interface_methods)
    interface_content += "\n}\n"
    
    # Modify Impl Content
    impl_content = content.replace(f"package com.phaithanhcong.user.service;", "package com.phaithanhcong.user.service.impl;")
    impl_content = impl_content.replace(f"public class {class_name} {{", f"public class {impl_name} implements {class_name} {{")
    
    # We need to import the interface in the impl class since it's in a different package
    # Actually, we can just add the import before the class declaration
    impl_content = re.sub(r'(public class)', f"import com.phaithanhcong.user.service.{class_name};\n\n\\1", impl_content)
    
    # Write Interface
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(interface_content)
        
    # Write Impl
    with open(os.path.join(impl_dir, impl_name + ".java"), "w", encoding="utf-8") as f:
        f.write(impl_content)

print("Done refactoring services.")
