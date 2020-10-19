class foo {
  private void bar() {
    
      if (sourceFile != null && !modulePath.startsWith("org/sugarj") && !modulePath.endsWith("*")) {
        
        if (!success || pendingInputFiles.contains(sourceFile) || (classUri != null && new File(sourceFile).lastModified() > new File(classUri).lastModified())) {
		;
        }
      }
      
  }
}
