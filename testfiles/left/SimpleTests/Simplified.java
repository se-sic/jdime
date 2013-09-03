class foo {
  private void bar() {
      if (importModuleSourceFile == null) {
        importModuleSourceFile = searchJavaFile(importModuleRelativePath, false);
      }

      if ( importModuleSourceFile != null && pendingInputFiles.contains(importModuleSourceFile) || files.isEmpty() && !isStdLibModule && !importModuleRelativePath.endsWith("*") && importModuleSourceFile != null) {
               ;
      }
      
  }
}
