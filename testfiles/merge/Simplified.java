class foo {
  private void bar() {
<<<<<<< left/Simplified.java
      if (importModuleSourceFile == null) {
        importModuleSourceFile = searchJavaFile(importModuleRelativePath, false);
      }

      if ( importModuleSourceFile != null && pendingInputFiles.contains(importModuleSourceFile) || files.isEmpty() && !isStdLibModule && !importModuleRelativePath.endsWith("*") && importModuleSourceFile != null) {
               ;
=======
    
      if (sourceFile != null && !modulePath.startsWith("org/sugarj") && !modulePath.endsWith("*")) {
        
        if (!success || pendingInputFiles.contains(sourceFile) || (classUri != null && new File(sourceFile).lastModified() > new File(classUri).lastModified())) {
		;
        }
>>>>>>> right/Simplified.java
      }
      
  }
}
