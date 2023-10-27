This example shows how an AST in JDime can be represented as a CSV table.   Later this CSV table shall be used as an input for static program analyses designed in Datalog.  

Both input files are identical except the second assignment in the _foo()_ method.  

Example file **left**:
```
class DeletionInsertion {
    void foo() {
        int a  = 1;
        float b = 2;
    }
}
```

Example file **right**:  
```
class DeletionInsertion {
    void foo() {
        int a  = 1;
        int c = 3;
    }
}
```

**merged java code** by JDime (structured mode):  
```
class DeletionInsertion {
  void foo() {
    int a = 1;

<<<<<<< /left/DeletionInsertion.java
    float
=======
    int
>>>>>>> /right/DeletionInsertion.java
     
<<<<<<< /left/DeletionInsertion.java
    b = 2
=======
    c = 3
>>>>>>> /right//DeletionInsertion.java
    ;
  }
}
```

  **merged AST** by JDime (structured mode):  
```
(target:0) Program <(33, 169%)> (right:0) Program
└──┬(target:1) List <(32, 168%)> (right:1) List
   └──┬(target:2) CompilationUnit PackageDecl="" <(31, 167%)> (right:2) CompilationUnit PackageDecl=""
      ├───(target:3) List <(1, 100%)> (right:3) List
      └──┬(target:4) List <(29, 165%)> (right:4) List
         └──┬(target:5) ClassDecl ID="DeletionInsertion" <(28, 164%)> (right:5) ClassDecl ID="DeletionInsertion"
            ├──┬(target:6) Modifiers <(2, 133%)> (right:6) Modifiers
            │  └───(target:7) List <(1, 100%)> (right:7) List
            ├───(target:8) Opt <(1, 100%)> (right:8) Opt
            ├───(target:9) List <(1, 100%)> (right:9) List
            └──┬(target:10) List <(23, 158%)> (right:10) List
               └──┬(target:11) MethodDecl ID="foo" <(22, 157%)> (right:11) MethodDecl ID="foo"
                  ├──┬(target:12) Modifiers <(2, 133%)> (right:12) Modifiers
                  │  └───(target:13) List <(1, 100%)> (right:13) List
                  ├───(target:14) PrimitiveTypeAccess ID="void" Package="@primitive" <(1, 100%)> (right:14) PrimitiveTypeAccess ID="void" Package="@primitive"
                  ├───(target:15) List <(1, 100%)> (right:15) List
                  ├───(target:16) List <(1, 100%)> (right:16) List
                  └──┬(target:17) Opt <(16, 145%)> (right:17) Opt
                     └──┬(target:18) Block <(15, 142%)> (right:18) Block
                        └──┬(target:19) List <(14, 140%)> (right:19) List
                           ├──┬(target:20) VarDeclStmt <(9, 180%)> (right:20) VarDeclStmt
                           │  ├──┬(target:21) Modifiers <(2, 133%)> (right:21) Modifiers
                           │  │  └───(target:22) List <(1, 100%)> (right:22) List
                           │  ├───(target:23) PrimitiveTypeAccess ID="int" Package="@primitive" <(1, 100%)> (right:23) PrimitiveTypeAccess ID="int" Package="@primitive"
                           │  └──┬(target:24) List <(5, 166%)> (right:24) List
                           │     └──┬(target:25) VariableDeclarator ID="a" <(4, 160%)> (right:25) VariableDeclarator ID="a"
                           │        ├───(target:26) List <(1, 100%)> (right:26) List
                           │        └──┬(target:27) Opt <(2, 133%)> (right:27) Opt
                           │           └───(target:28) IntegerLiteral LITERAL="1" <(1, 100%)> (right:28) IntegerLiteral LITERAL="1"
                           └──┬(target:29) VarDeclStmt <(4, 80%)> (right:29) VarDeclStmt
                              ├──┬(target:30) Modifiers <(2, 133%)> (right:30) Modifiers
                              │  └───(target:31) List <(1, 100%)> (right:31) List
                              ├───(target:0) PrimitiveTypeAccess ID="float" Package="@primitive"
<<<<<<<
                                 (left:32) PrimitiveTypeAccess ID="float" Package="@primitive"
=======
                                 (right:32) PrimitiveTypeAccess ID="int" Package="@primitive"
>>>>>>>
                              └──┬(target:33) List <(1, 33%)> (right:33) List
                                 └──┬(target:0) VariableDeclarator ID="b"
<<<<<<<
                                    (left:34) VariableDeclarator ID="b"
                                        ├───(left:35) List
                                        └──┬(left:36) Opt
                                           └───(left:37) IntegerLiteral LITERAL="2"
=======
                                    (right:34) VariableDeclarator ID="c"
                                        ├───(right:35) List
                                        └──┬(right:36) Opt
                                           └───(right:37) IntegerLiteral LITERAL="3"
>>>>>>> 

```


**merged** AST output as **CSV** by JDime (structured mode):  
The key of each entry is a combination of the first two columns (_Nr,Side)_. 
- _Side_ is:
  - _target_ if two AST nodes from left and right have been matched
  - _left_ or _right_ if one side holds an additional statement which is inserted into the merged AST
  - _left_ or _right_ if two AST nodes cannot be matched and create a merge conflict (see line 32-37)
- _Type_ is the most important field, because it allows to query for types of nodes i.e. **VarStmt**
- _ID_ contains a more specific value for some nodes i.e. in line (25,target,VariableDeclarator,**a**,,,24,target
) that variable _a_ is getting assigned
- _Literal_ only contains static values i.e. (28,target,IntegerLiteral,,**1**,,27,target)
- _Package_ seems unnecessary to me atm it rarely holds information and if so it only says **@primitive**
- _(ParentNr, ParentSide)_ holds the parent key to keep the AST structure  
```
#(Nr,Side, Type, ID, Literal, Package, ParentNr, ParentSide)#
0,target,Program,,,,-1,target
1,target,List,,,,0,target
2,target,CompilationUnit,,,,1,target
3,target,List,,,,2,target
4,target,List,,,,2,target
5,target,ClassDecl,DeletionInsertion,,,4,target
6,target,Modifiers,,,,5,target
7,target,List,,,,6,target
8,target,Opt,,,,5,target
9,target,List,,,,5,target
10,target,List,,,,5,target
11,target,MethodDecl,foo,,,10,target
12,target,Modifiers,,,,11,target
13,target,List,,,,12,target
14,target,PrimitiveTypeAccess,void,,@primitive,11,target
15,target,List,,,,11,target
16,target,List,,,,11,target
17,target,Opt,,,,11,target
18,target,Block,,,,17,target
19,target,List,,,,18,target
20,target,VarDeclStmt,,,,19,target
21,target,Modifiers,,,,20,target
22,target,List,,,,21,target
23,target,PrimitiveTypeAccess,int,,@primitive,20,target
24,target,List,,,,20,target
25,target,VariableDeclarator,a,,,24,target
26,target,List,,,,25,target
27,target,Opt,,,,25,target
28,target,IntegerLiteral,,1,,27,target
29,target,VarDeclStmt,,,,19,target
30,target,Modifiers,,,,29,target
31,target,List,,,,30,target
32,left,PrimitiveTypeAccess,float,,@primitive,29,target
32,right,PrimitiveTypeAccess,int,,@primitive,29,target
33,target,List,,,,29,target
34,left,VariableDeclarator,b,,,33,target
35,left,List,,,,34,left
36,left,Opt,,,,34,left
37,left,IntegerLiteral,,2,,36,left
34,right,VariableDeclarator,c,,,33,target
35,right,List,,,,34,right
36,right,Opt,,,,34,right
37,right,IntegerLiteral,,3,,36,right
 ```

