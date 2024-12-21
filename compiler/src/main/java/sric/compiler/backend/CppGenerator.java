//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.backend;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Expr.*;
import sric.compiler.ast.Stmt;
import sric.compiler.ast.Stmt.*;
import sric.compiler.ast.Type;
import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Buildin;
import sric.compiler.ast.FConst;
import sric.compiler.ast.SModule;
import sric.compiler.ast.SModule.Depend;
import sric.compiler.ast.Token.TokenKind;
import static sric.compiler.ast.Token.TokenKind.eq;
import static sric.compiler.ast.Token.TokenKind.gt;
import static sric.compiler.ast.Token.TokenKind.gtEq;
import static sric.compiler.ast.Token.TokenKind.lt;
import static sric.compiler.ast.Token.TokenKind.ltEq;
import static sric.compiler.ast.Token.TokenKind.notEq;
import static sric.compiler.ast.Token.TokenKind.notSame;
import static sric.compiler.ast.Token.TokenKind.same;
import sric.compiler.ast.Type.*;

/**
 *
 * @author yangjiandong
 */
public class CppGenerator extends BaseGenerator {
    
    public boolean headMode = true;
    private SModule module;
    private TypeDef curStruct;
    
    private String curItName;
    
    private HashMap<TypeDef, Integer> emitState = new HashMap<>();
    
    public CppGenerator(CompilerLog log, String file, boolean headMode) throws IOException {
        super(log, file);
        this.headMode = headMode;
    }
    
    public CppGenerator(CompilerLog log, PrintStream writer) {
        super(log, writer);
    }
    
    private void printCommentInclude(TopLevelDef type) {
        if (type.isExtern() && type.comment != null) {
            for (Comment comment : type.comment.comments) {
               if (comment.content.startsWith("#")) {
                   print(comment.content);
                   newLine();
               }
            }
        }
    }
    
    private String filterSymbolName(String sym) {
        if (sym.equals("int")) {
            return "_int";
        }
        else if (sym.equals("float")) {
            return "_float";
        }
        return sym;
    }
    
    private String getSymbolName(TopLevelDef type) {
        String sym = getExternSymbol(type);
        if (sym != null) {
            return sym;
        }
        return filterSymbolName(type.name);
    }
    
    private String getExternSymbol(TopLevelDef type) {
        if ((type.flags & FConst.Extern) != 0 && type.comment != null) {
            for (Comment comment : type.comment.comments) {
               String key = "extern symbol:";
               if (comment.content.startsWith(key)) {
                   return comment.content.substring(key.length()).trim();
               }
            }
        }
        return null;
    }
    
    public void run(SModule module) {
        this.module = module;
        if (headMode) {
            String marcoName = module.name.toUpperCase()+"_H_";
            print("#ifndef ").print(marcoName).newLine();
            print("#define ").print(marcoName).newLine();

            newLine();
            print("#include \"sc_runtime.h\"").newLine();
            
            for (Depend d : module.depends) {
                print("#include \"");
                print(d.name).print(".h");
                print("\"").newLine();
            }
            newLine();
            
            /////////////////////////////////////////////////////////////
            for (FileUnit funit : module.fileUnits) {
                for (TypeAlias type : funit.typeAlias) {
                    printCommentInclude(type);
                }
                for (TypeDef type : funit.typeDefs) {
                    printCommentInclude(type);
                }
                for (FieldDef type : funit.fieldDefs) {
                    printCommentInclude(type);
                }
                for (FuncDef type : funit.funcDefs) {
                    printCommentInclude(type);
                }
            }
            
            /////////////////////////////////////////////////////////////
            print("namespace ");
            print(module.name);
            print(" {").newLine();
            
            this.indent();
            
            //types decleartion
            for (FileUnit funit : module.fileUnits) {
                for (TypeDef type : funit.typeDefs) {
                    if ((type.flags & FConst.ExternC) != 0) {
                        continue;
                    }
                    
                    printGenericParamDefs(type.generiParamDefs);
                    
                    if (type.isEnum()) {
                        print("enum ");
                    }
                    print("struct ");
                    print(getSymbolName(type)).print(";").newLine();
                }
            }
            
            this.unindent();
            newLine();
            print("} //ns").newLine();
            
            /////////////////////////////////////////////////////////////
            
//            print("extern \"C\" {");
//            this.indent();
//            
//            for (FileUnit funit : module.fileUnits) {
//                for (TypeDef type : funit.typeDefs) {
//                    if ((type.flags & FConst.ExternC) != 0) {
//                        print("struct ");
//                        print(getSymbolName(type)).print(";").newLine();
//                    }
//                }
//                for (FuncDef f : funit.funcDefs) {
//                    if ((f.flags & FConst.ExternC) != 0) {
//                        printFunc(f, false);
//                    }
//                }
//                for (FieldDef f : funit.fieldDefs) {
//                    if ((f.flags & FConst.ExternC) != 0) {
//                        visitField(f);
//                    }
//                }
//            }
//            
//            this.unindent();
//            newLine();
//            print("}").newLine();

            /////////////////////////////////////////////////////////////
            print("namespace ");
            print(module.name);
            print(" {").newLine();
            
            this.indent();
            
            module.walkChildren(this);

            this.unindent();
            
            newLine();
            print("} //ns").newLine();
            
            newLine();
            print("#endif //");
            print(marcoName).newLine();
        }
        else {
            print("#include \"");
            print(module.name).print(".h");
            print("\"").newLine();
            
            newLine();
            
            module.walkChildren(this);
            
            newLine();
            newLine();
            print("//////////////////////////////////////////// reflect");
            newLine();
            
            printReflection(module);
            
            newLine();
        }
    }
    
    private void reflectionTopLevelDef(TopLevelDef node, String varName) {
        print(varName).print(".flags = ").print(""+node.flags).print(";").newLine();
        print(varName).print(".name = \"").print(node.name).print("\";").newLine();
        
        if (node instanceof TypeDef td) {
            print(varName).print(".kind = ").print(""+td.kind.ordinal()).print(";").newLine();
        }
        
        if (node.comment != null) {
            for (var c : node.comment.comments) {
                if (c.type != TokenKind.cmdComment) {
                }
                
                print("{sric::RComment comment;");
                print("comment.type = ").print(c.type == TokenKind.cmdComment ? "0" : "1").print(";");
                print("comment.content = "); printStringLiteral(c.content); print(";");
                
                print(varName).print(".comments.add(&comment);}");
                this.newLine();
            }
        }
    }
    
    private void reflectParamDef(FieldDef f, String parentName) {
        print("{");
        print("sric::RField param;");
        print("param.name = \"").print(f.name).print("\";");
        print("param.fieldType = ");printStringLiteral(f.fieldType.toString());print(";");
        print("param.hasDefaultValue = ").print(f.initExpr == null ? "0" : "1").print(";");
        print(parentName).print(".params.add(&param);");
        print("}");
        this.newLine();
    }
    
    private void reflectFieldDef(FieldDef f, String parentName, boolean isEnumSlot) {
        print("{");
        this.indent();
        newLine();
        print("sric::RField f;").newLine();
        reflectionTopLevelDef(f, "f");
        
        String moduleName = this.module.name;
        if (f.isStatic()) {
            print("f.offset = 0;").newLine();
            print("f.pointer = &");print(moduleName);print("::");
            if (f.parent instanceof TypeDef td) {
                print(this.getSymbolName(td));
                print("::");
            }
            print(this.getSymbolName(f)).print(";").newLine();
        }
        else if (isEnumSlot) {
            print("f.offset = 0;").newLine();
            print("f.pointer = nullptr;").newLine();
        }
        else {
            print("f.offset = offsetof("); print(moduleName);print("::").print(this.getSymbolName((TopLevelDef)f.parent));
                print(",").print(this.getSymbolName(f)).print(");").newLine();
            print("f.pointer = nullptr;").newLine();
        }
        
        print("f.fieldType = ");printStringLiteral(f.fieldType.toString());print(";").newLine();
        print("f.hasDefaultValue = ").print(f.initExpr == null ? "0" : "1").print(";").newLine();
        
        print("f.enumValue = ").print(""+f._enumValue).print(";").newLine();
        
        print(parentName).print(".fields.add(&f);").newLine();
        
        this.unindent();
        print("}");
        newLine();
    }
    
    private void printMethodWrapFunc(FuncDef f) {
        this.printType(f.prototype.returnType);
        print(" ").print(this.module.name).print("_").print(this.getSymbolName((TopLevelDef)f.parent)).
                print("_").print(this.getSymbolName(f)).print("(");
        
        print(this.module.name).print("::").print(this.getSymbolName((TopLevelDef)f.parent)).print("* self");
        
        for (FieldDef p : f.prototype.paramDefs) {
            print(", ");
            printType(p.fieldType);
            print(" ").print(p.name);
        }
        print(") {").newLine();
        this.indent();
        
        if (!f.prototype.returnType.isVoid()) {
            print("return ");
        }
        
        print("self->").print(this.getSymbolName(f)).print("(");
        int i = 0;
        for (FieldDef p : f.prototype.paramDefs) {
            if (i>0) print(", ");
            print(p.name);
            ++i;
        }
        print(");").newLine();
        
        this.unindent();
        print("}").newLine();
    }
    
    private void reflectFuncDef(FuncDef f, String parentName) {
        print("{");
        this.indent();
        newLine();
        print("sric::RFunc f;").newLine();
        reflectionTopLevelDef(f, "f");
        
        String moduleName = this.module.name;
        if (f.isStatic()) {
            print("f.pointer = &");print(moduleName);print("::").print(this.getSymbolName(f)).print(";").newLine();
        }
        else {
            print("f.pointer = &");print(this.module.name).print("_").print(this.getSymbolName((TopLevelDef)f.parent)).
                print("_").print(this.getSymbolName(f)).print(";").newLine();
        }

        print("f.returnType = ");printStringLiteral(f.prototype.returnType.toString());print(";").newLine();

        if (f.prototype.paramDefs != null) {
            for (FieldDef p : f.prototype.paramDefs) {
                reflectParamDef(p, "f");
            }
        }
        
        if (f.generiParamDefs != null) {
            for (GenericParamDef p : f.generiParamDefs) {
                print("f.genericParams.add(");printStringLiteral(p.name); print(");");
                this.newLine();
            }
        }
        
        print(parentName).print(".funcs.add(&f);");

        this.unindent();
        newLine();
        print("}");
        newLine();
    }
    
    private void printReflection(SModule module) {
        //print method wrap
        for (FileUnit funit : module.fileUnits) {
            for (TypeDef type : funit.typeDefs) {
                if ((type.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                for (FuncDef f : type.funcDefs) {
                    printMethodWrapFunc(f);
                }
            }
        }
        
        print("void registReflection_").print(module.name).print("() {").newLine();
        this.indent();
        
        print("sric::RModule m;").newLine();
        print("m.name = \"").print(module.name).print("\";").newLine();
        print("m.version = \"").print(module.version).print("\";").newLine();
        
        for (FileUnit funit : module.fileUnits) {
            for (TypeDef type : funit.typeDefs) {
                
                if ((type.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                
                print("{");
                this.indent();
                newLine();
                print("sric::RType s;").newLine();
                reflectionTopLevelDef(type, "s");
                

                if (type.generiParamDefs != null) {
                    for (GenericParamDef p : type.generiParamDefs) {
                        print("s.genericParams.add(");printStringLiteral(p.name); print(");");
                        this.newLine();
                    }
                }

                if (type.inheritances != null) {
                    for (Type p : type.inheritances) {
                        print("s.inheritances.add(");printStringLiteral(p.toString()); print(");");
                        this.newLine();
                    }
                }

                for (FieldDef f : type.fieldDefs) {
                    reflectFieldDef(f, "s", type.isEnum());
                }

                for (FuncDef f : type.funcDefs) {
                    reflectFuncDef(f, "s");
                }
                
                
                this.unindent();
                print("}");
                newLine();
            }
            
            for (FieldDef f : funit.fieldDefs) {
                if ((f.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                reflectFieldDef(f, "m", false);
            }
            
            for (FuncDef f : funit.funcDefs) {
                if ((f.flags & FConst.Reflect) == 0 ) {
                    continue;
                }
                reflectFuncDef(f, "m");
            }
            
        }
        
        
        newLine();
        print("sric::registModule(&m);").newLine();
        
        this.unindent();
        print("}");
        newLine();
        
        print("SC_AUTO_REGIST_MODULE("+ module.name +");").newLine();
    }
    
    private void printType(Type type) {
        printType(type, true);
    }

    private void printType(Type type, boolean isRoot) {
        if (type == null) {
            print("auto");
            return;
        }
        
        if (type.resolvedAlias != null) {
            if (type.id.resolvedDef instanceof GenericParamDef) {
                //ok
            }
            else if (type.id.resolvedDef instanceof TypeAlias ta) {
                if (!ta.isExtern()) {
                    printType(type.resolvedAlias, isRoot);
                    return;
                }
            }
        }
        
        if (type.isImmutable && !type.id.name.equals(Buildin.pointerTypeName)) {
            print("const ");
        }
        
        if (type.isRefable) {
            print("sric::StackRefable<");
        }
        
        boolean printGenericParam = true;
        switch (type.id.name) {
            case "Void":
                print("void");
                break;
            case "Int":
                NumInfo intType = (NumInfo)type.detail;
                if (intType.size == 8 && intType.isUnsigned == false) {
                    print("char");
                }
                else {
                    if (intType.isUnsigned) {
                        print("u");
                    }
                    print("int"+intType.size+"_t");
                }
                break;
            case "Float":
                NumInfo floatType = (NumInfo)type.detail;
                if (floatType.size == 64) {
                    print("double");
                }
                else {
                    print("float");
                }
                break;
            case "Bool":
                print("bool");
                break;
            case Buildin.pointerTypeName:
                PointerInfo pt = (PointerInfo)type.detail;
                if (pt.pointerAttr == Type.PointerAttr.raw || pt.pointerAttr == Type.PointerAttr.inst) {
                    printType(type.genericArgs.get(0), false);
                    
                    print("*");
                    if (type.isImmutable) {
                        print(" const");
                    }
                }
                else {
                    if (type.isImmutable) {
                        print("const ");
                    }
                    if (pt.pointerAttr == Type.PointerAttr.own) {
                        print("sric::OwnPtr");
                    }
                    else if (pt.pointerAttr == Type.PointerAttr.ref) {
                        print("sric::RefPtr");
                    }
//                    else if (pt.pointerAttr == Type.PointerAttr.weak) {
//                        print("sric::WeakPtr");
//                    }
                    print("<");
                    printType(type.genericArgs.get(0), false);
                    print(">");
                }
                printGenericParam = false;
                break;
            case Buildin.arrayTypeName:
                ArrayInfo arrayType = (ArrayInfo)type.detail;
                print("sric::Array<");
                printType(type.genericArgs.get(0));
                print(", ");
                this.visit(arrayType.sizeExpr);
                print(">");
                printGenericParam = false;
                break;
            case Buildin.funcTypeName:
                FuncInfo ft = (FuncInfo)type.detail;
                print("std::function<");
                printType(ft.prototype.returnType);
                print("(");
                if (ft.prototype.paramDefs != null) {
                    int i = 0;
                    for (FieldDef p : ft.prototype.paramDefs) {
                        if (i > 0) print(", ");
                        printType(p.fieldType, false);
                        ++i;
                    }
                }
                print(")>");
                printGenericParam = false;
                break;
            default:
                printIdExpr(type.id);
                break;
        }

        if (printGenericParam && type.genericArgs != null) {
            print("<");
            int i= 0;
            for (Type p : type.genericArgs) {
                if (i > 0) {
                    print(", ");
                }
                printType(p, false);
                ++i;
            }
            print(" >");
        }
        
        if (type.isRefable) {
            print(" >");
        }
    }

    private void printIdExpr(IdExpr id) {
        if (id.resolvedDef instanceof TopLevelDef td) {
            String symbolName = getExternSymbol(td);
            if (symbolName != null) {
                print(symbolName);
                return;
            }
        }

        
        if (id.namespace != null) {
            printIdExpr(id.namespace);
            print("::");
        }
        else {
            if (id.resolvedDef instanceof TopLevelDef td) {
                if ((td.flags & FConst.ExternC) == 0 && td.parent instanceof FileUnit fu) {
                    print(fu.module.name);
                    print("::");
                }
            }
            else if (id.name.equals(TokenKind.superKeyword.symbol)) {
                printType(curStruct.inheritances.get(0));
                return;
            }
            else if (id.name.equals(TokenKind.dot.symbol)) {
                print(this.curItName);
                return;
            }
        }
        
        if (id.resolvedDef instanceof TopLevelDef td) {
            print(getSymbolName(td));
        }
        else {
            print(filterSymbolName(id.name));
        }
    }
    
    @Override
    public void visitTypeAlias(AstNode.TypeAlias v) {
        if (v.isExtern()) {
            return;
        }
        print("typedef ");
        printType(v.type);
        print(" ");
        print(getSymbolName(v));
        print(";");
        newLine();
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        if (v.isLocalVar) {
            if (printLocalFieldDefAsExpr(v)) {
                print(";").newLine();
            }
            return;
        }
        
        if (headMode && v.parent instanceof FileUnit) {
            if ((v.flags & FConst.ConstExpr) == 0) {
                print("extern ");
            }
        }
        
        if (headMode) {
            if (printLocalFieldDefAsExpr(v)) {
                print(";").newLine();
            }
        }
        else if (v.isStatic()) {
            if (printLocalFieldDefAsExpr(v)) {
                print(";").newLine();
            }
        }
    }
    
    boolean printLocalFieldDefAsExpr(AstNode.FieldDef v) {
        boolean isImpl = implMode();
        boolean isStatic = v.isStatic();//(v.flags & FConst.Static) != 0;
        
        boolean isConstExpr = false;
        if ((v.flags & FConst.ConstExpr) != 0) {
            if (isImpl) {
                return false;
            }
            print("constexpr ");
            isConstExpr = true;
        }
        
        printType(v.fieldType);
        print(" ");
        if (isStatic && isImpl && !v.isLocalVar) {
            if (v.parent instanceof TypeDef) {
                print(getSymbolName((TypeDef)v.parent));
                print("::");
            }
            else if (v.parent instanceof FileUnit unit) {
                print(unit.module.name);
                print("::");
            }
        }
        print(getSymbolName(v));
        
//        if (v.fieldType.isArray()) {
//            ArrayInfo arrayType = (ArrayInfo)v.fieldType.detail;
//            print("[");
//            this.visit(arrayType.sizeExpr);
//            print("]");
//        }
        
        boolean init = false;
        if (v.isLocalVar) {
            init = true;
        }
        else if (isConstExpr) {
            init = true;
        }
        else if (isStatic && isImpl) {
            init = true;
        }
        else if (!isStatic && !isImpl) {
            init = true;
        }
        
        if (init && v.initExpr != null) {
            if (v.initExpr instanceof Expr.WithBlockExpr wbe && wbe._storeVar != null) {
                print(" ");
                this.visit(v.initExpr);
            }
            else if (v.initExpr instanceof Expr.ArrayBlockExpr) {
                print(" ");
                this.visit(v.initExpr);
            }
            else {
                print(" = ");
                this.visit(v.initExpr);
            }
        }
        return true;
    }
    
    private boolean implMode() {
        return !headMode;
    }
    
    private void printGenericParamDefs(ArrayList<GenericParamDef> generiParamDefs) {
        if (generiParamDefs != null) {
            print("template ");
            print("<");
            int i = 0;
            for (var gp : generiParamDefs) {
                if (i > 0) print(", ");
                print("typename ");
                print(gp.name);
                ++i;
            }
            print(">").newLine();
        }
    }
    
    private boolean isEntryPoint(AstNode.FuncDef v) {
        if (v.parent instanceof FileUnit &&  v.name.equals("main")) {
            return true;
        }
        return false;
    }
    
    private void printFunc(AstNode.FuncDef v, boolean isOperator) {
        boolean inlined = (v.flags & FConst.Inline) != 0 || v.generiParamDefs != null;
        if (v.parent instanceof TypeDef sd) {
            if (sd.generiParamDefs != null) {
                inlined = true;
            }
        }
        if (implMode()) {
            if (v.code == null || inlined) {
                return;
            }
        }
        
        newLine();
        
        printGenericParamDefs(v.generiParamDefs);
        
        if (headMode) {
            if ((v.flags & FConst.Virtual) != 0 || (v.flags & FConst.Abstract) != 0) {
                print("virtual ");
            }
            if ((v.flags & FConst.Static) != 0) {
                print("static ");
            }
        }
        
//        if ((v.flags & FConst.Extern) != 0) {
//            print("extern ");
//        }
        
        printType(v.prototype.returnType);
        print(" ");
        if (implMode()) {
            if (v.parent instanceof TypeDef t) {
                if (t.parent instanceof FileUnit fu) {
                    if (fu.module != null) {
                        print(fu.module.name).print("::");
                    }
                }
                print(getSymbolName(t)).print("::");
            }
            else if (v.parent instanceof FileUnit fu) {
                if (fu.module != null && !isEntryPoint(v)) {
                    print(fu.module.name).print("::");
                }
            }
        }
        
        if (isOperator) {
            print("operator");
            switch (v.name) {
                case "plus":
                    print("+");
                    break;
//                case "set":
//                    print("[]");
//                    break;
                case "get":
                    print("[]");
                    break;
                case "minus":
                    print("-");
                    break;
                case "mult":
                    print("-");
                    break;
                case "div":
                    print("-");
                    break;
                case "compare":
                    print("<=>");
                    break;
                default:
                    break;
            }
        }
        else {
            print(getSymbolName(v));
        }
        
        printFuncPrototype(v.prototype, false, v.isStatic());
        
        if (v.code == null) {
            if ((v.flags & FConst.Abstract) != 0) {
                print(" = 0");
            }
            print(";");
        }
        else {
            if (implMode() || inlined) {
                print(" ");
                this.visit(v.code);
            }
            else {
                print(";");
            }
        }
    }
    
    @Override
    public void visitFunc(AstNode.FuncDef v) {
        if (v.isExtern()) {
            return;
        }
        if (isEntryPoint(v) && headMode) {
            return;
        }
        
        printFunc(v, false);
        if ((v.flags & FConst.Operator) != 0 && !v.name.equals("set") && !v.name.equals("compare")) {
            printFunc(v, true);
        }
    }
    
    private void printFuncPrototype(FuncPrototype prototype, boolean isLambda, boolean isStatic) {
        print("(");
        if (prototype != null && prototype.paramDefs != null) {
            int i = 0;
            for (FieldDef p : prototype.paramDefs) {
                if (i > 0) {
                    print(", ");
                }
                if (p.fieldType.isVarArgType()) {
                    print("...");
                }
                else {
                    printType(p.fieldType);
                    print(" ");
                    print(p.name);
                }
                if (p.initExpr != null) {
                    print(" = ");
                    this.visit(p.initExpr);
                }
                ++i;
            }
        }
        print(")");
        
        if (!isLambda && !isStatic && prototype.isThisImmutable()) {
            print(" const ");
        }
        
        if (isLambda && prototype != null) {
            if (prototype.returnType != null && !prototype.returnType.isVoid()) {
                print("->");
                printType(prototype.returnType);
            }
        }
    }

    @Override
    public void visitTypeDef(TypeDef v) {
        if (v.isExtern()) {
            return;
        }
        
        if (headMode) {
            if (!topoSort(v)) return;
        }
        

        if (implMode()) {
            //if (v instanceof StructDef sd) {
                curStruct =  v;
            //}
            v.walkChildren(this);
            curStruct =  null;
            return;
        }
        else {
            newLine();

            if (v.isEnum()) {
                print("enum struct ");
                print(getSymbolName(v));
                print(" {").newLine();
                indent();

                int i = 0;
                for (FieldDef f : v.fieldDefs) {
                    if (i > 0) {
                        print(",").newLine();
                    }
                    print(f.name);
                    if (f.initExpr != null) {
                        print(" = ");
                        this.visit(f.initExpr);
                    }
                    ++i;
                }
                newLine();

                unindent();
                print("};").newLine();
                return;
            }

            //if (v instanceof StructDef sd) {
                printGenericParamDefs(v.generiParamDefs);
            //}

            print("struct ");
            print(getSymbolName(v));

            //if (v instanceof StructDef sd) {
            if (v.inheritances != null) {
                int i = 0;
                for (Type inh : v.inheritances) {
                    if (i == 0) print(" : ");
                    else print(", ");
                    print("public ");
                    printType(inh);
                    ++i;
                }
            }
            
            if ((v.flags & FConst.Noncopyable) != 0) {
                if (v.inheritances != null) {
                    print(", public Noncopyable");
                }
                else {
                    print(" : public Noncopyable");
                }
            }
            //}

            print(" {").newLine();
            indent();

            v.walkChildren(this);

            newLine();
            
            if ((v.flags & FConst.Abstract) != 0 || (v.flags & FConst.Virtual) != 0) {
                print("virtual ~");
                print(getSymbolName(v));
                print("(){}");
            }
            
            unindent();
            newLine();
            
            print("};").newLine();
        
        }
    }

    private boolean topoSort(TypeDef v) {
        //Topo sort
        if (this.emitState.get(v) != null) {
            int state = this.emitState.get(v);
            if (state == 2) {
                return false;
            }
            err("Cyclic dependency", v.loc);
            return false;
        }
        this.emitState.put(v, 1);
        //if (v instanceof StructDef sd) {
        if (v.inheritances != null) {
            for (Type t : v.inheritances) {
                if (t.id.resolvedDef != null && t.id.resolvedDef instanceof TypeDef td) {
                    if (td.parent != null && ((FileUnit)td.parent).module == this.module) {
                        //if (td instanceof StructDef tds) {
                        if (td.originGenericTemplate != null) {
                            this.visitTypeDef(td.originGenericTemplate);
                            continue;
                        }
                        //}
                        this.visitTypeDef(td);
                    }
                }
            }
        }
        
        if (!v.isEnum()) {
            for (FieldDef f : v.fieldDefs) {
                if (!f.fieldType.isPointerType() && f.fieldType.id.resolvedDef != null && f.fieldType.id.resolvedDef instanceof TypeDef td) {
                    if (td.parent != null && td.parent instanceof FileUnit unit) {
                        if (unit.module == this.module) {
                            //if (td instanceof StructDef tds) {
                            if (td.originGenericTemplate != null) {
                                this.visitTypeDef(td.originGenericTemplate);
                                continue;
                            }
                            //}
                            this.visitTypeDef(td);
                        }
                    }
                }
            }
        }
        //}
        this.emitState.put(v, 2);
        return true;
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof Block bs) {
            print("{").newLine();
            indent();
            bs.walkChildren(this);
            unindent();
            print("}").newLine();
        }
        else if (v instanceof IfStmt ifs) {
            print("if (");
            this.visit(ifs.condition);
            print(") ");
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                print("else ");
                this.visit(ifs.elseBlock);
            }
        }
        else if (v instanceof LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof WhileStmt whiles) {
            print("while (");
            this.visit(whiles.condition);
            print(") ");
            this.visit(whiles.block);
        }
        else if (v instanceof ForStmt fors) {
            print("for (");
            if (fors.init != null) {
                if (fors.init instanceof LocalDefStmt varDef) {
                    printLocalFieldDefAsExpr(varDef.fieldDef);
                }
                else if (fors.init instanceof ExprStmt s) {
                    this.visit(s.expr);
                }
                else {
                    err("Unsupport for init stmt", fors.init.loc);
                }
            }
            print("; ");
            
            if (fors.condition != null) {
                this.visit(fors.condition);
            }
            print("; ");
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            print(") ");
            this.visit(fors.block);
        }
        else if (v instanceof SwitchStmt switchs) {
            print("switch (");
            this.visit(switchs.condition);
            print(") {").newLine();
            
            this.indent();
            
            for (CaseBlock cb : switchs.cases) {
                this.unindent();
                print("case ");
                this.visit(cb.caseExpr);
                print(":").newLine();
                this.indent();
                
                this.visit(cb.block);
                
                if (!cb.fallthrough) {
                    print("break;").newLine();
                }
            }
            
            if (switchs.defaultBlock != null) {
                this.unindent();
                print("default:").newLine();
                this.indent();
                this.visit(switchs.defaultBlock);
            }
 
            this.unindent();
            print("}").newLine();
        }
        else if (v instanceof ExprStmt exprs) {
            this.visit(exprs.expr);
            print(";").newLine();
        }
        else if (v instanceof JumpStmt jumps) {
            print(jumps.opToken.symbol).print(";").newLine();
        }
        else if (v instanceof UnsafeBlock bs) {
            print("/*unsafe*/ ");
            this.visit(bs.block);
        }
        else if (v instanceof ReturnStmt rets) {
            if (rets.expr != null) {
                print("return ");
                this.visit(rets.expr);
                print(";").newLine();
            }
            else {
                print("return;");
            }
        }
        else {
            err("Unkown stmt:"+v, v.loc);
        }
    }

    @Override
    public void visitExpr(Expr v) {
        int parentheses = 0;
        if (v.isStmt || v instanceof IdExpr || v instanceof LiteralExpr || v instanceof CallExpr || v instanceof GenericInstance 
                || v instanceof AccessExpr || v instanceof NonNullableExpr || v instanceof WithBlockExpr || v instanceof ArrayBlockExpr) {
            
        }
        else {
            print("(");
            parentheses++;
        }
        
        if (v.implicitTypeConvertTo != null && !v.implicitTypeConvertTo.isVarArgType()) {
            boolean ok = false;
            if (v.implicitStringConvert) {
                print("sric::strStatic(");
                parentheses++;
                ok = true;
            }
            else if (v.isPointerConvert) {
                if (v.resolvedType.detail instanceof Type.PointerInfo p1 && v.implicitTypeConvertTo.detail instanceof Type.PointerInfo p2) {
                    if (p1.pointerAttr == Type.PointerAttr.own && p2.pointerAttr == Type.PointerAttr.ref) {
                        print("sric::RefPtr<");
                        printType(v.implicitTypeConvertTo.genericArgs.get(0));
                        print(" >(");
                        parentheses++;
                        ok = true;
                    }
//                    else if (p1.pointerAttr == Type.PointerAttr.raw && p2.pointerAttr == Type.PointerAttr.ref) {
//                        print("sric::RefPtr<");
//                        printType(v.implicitTypeConvertTo.genericArgs.get(0));
//                        print(" >(");
//                        convertParentheses = true;
//                    }
                }
            }
            
            if (!ok) {
                print("(");
                printType(v.implicitTypeConvertTo);
                print(")");
            }
        }

        if (v.implicitDereference) {
            print("*");
        }
        if (v.implicitGetAddress) {
            print("sric::addressOf(");
            parentheses++;
        }
        
        if (v instanceof IdExpr e) {
            this.printIdExpr(e);
        }
        else if (v instanceof AccessExpr e) {
            if (e._addressOf && e.target.resolvedType != null) {
                if (e.target.resolvedType.detail instanceof Type.PointerInfo pinfo) {
                    print("sric::RefPtr<");
                    this.printType(e.resolvedType);
                    print(">(");
                    this.visit(e.target);
                    print(",");

    //                print("0");
                    print("(int)&(((");
                    this.printType(e.target.resolvedType.genericArgs.get(0));
                    print("*)0)->");
                    print(e.name);
                    print(")");

                    print(")");
                }
                else {
                    print("sric::RefPtr<");
                    this.printType(e.resolvedType);
                    print(">(&");
                    this.visit(e.target);
                    print(",");

    //                print("0");
                    print("(int)&(((");
                    boolean isRefable = e.target.resolvedType.isRefable;
                    e.target.resolvedType.isRefable = false;
                    this.printType(e.target.resolvedType);
                    e.target.resolvedType.isRefable = isRefable;
                    print("*)0)->");
                    print(e.name);
                    print(")");

                    print(")");
                }
            }
            else {
                boolean isNullable = false;
                if (e.target.resolvedType != null && e.target.resolvedType.detail instanceof Type.PointerInfo pinfo) {
                    if (pinfo.isNullable) {
                        print("nonNullable(");
                        isNullable = true;
                    }
                }

                this.visit(e.target);
                if (isNullable) {
                    print(")");
                }

                if (e.target instanceof IdExpr ide && ide.name.equals(TokenKind.superKeyword.symbol)) {
                    print("::");
                }
                else if (e.target.resolvedType != null && e.target.resolvedType.isPointerType()) {
                    print("->");
                }
                else if (e.target.resolvedType != null && e.target.resolvedType.isRefable) {
                    print("->");
                }
                else {
                    print(".");
                }
                print(e.name);
            }
        }
        else if (v instanceof LiteralExpr e) {
            printLiteral(e);
        }
        else if (v instanceof BinaryExpr e) {
            printBinaryExpr(e);
        }
        else if (v instanceof CallExpr e) {
            this.visit(e.target);
            print("(");
            if (e.args != null) {
                int i = 0;
                for (CallArg t : e.args) {
                    if (i > 0) print(", ");
                    this.visit(t.argExpr);
                    ++i;
                }
            }
            print(")");
        }
        else if (v instanceof UnaryExpr e) {
            if (e.opToken == TokenKind.amp) {
                if (!e._addressOfField) {
                //if (e._isRawAddressOf) {
                    print("&");
                    this.visit(e.operand);
                //}
//                else {
//                    print("sric::addressOf(");
//                    this.visit(e.operand);
//                    print(")");
//                }
                }
                else {
                    this.visit(e.operand);
                }
            }
            else if (e.opToken == TokenKind.moveKeyword) {
                print("std::move(");
                this.visit(e.operand);
                print(")");
            }
            else if (e.opToken == TokenKind.awaitKeyword) {
                print("co_await ");
                this.visit(e.operand);
                //print("");
            }
            else {
                print(e.opToken.symbol);
                this.visit(e.operand);
            }
        }
        else if (v instanceof TypeExpr e) {
            this.printType(e.type);
        }
        else if (v instanceof IndexExpr e) {
            if (e.resolvedOperator != null) {
                this.visit(e.target);
                print(".get(");
                this.visit(e.index);
                print(")");
            }
            else {
                this.visit(e.target);
                print("[");
                this.visit(e.index);
                print("]");
            }
        }
        else if (v instanceof GenericInstance e) {
            this.visit(e.target);
            print("<");
            int i = 0;
            for (Type t : e.genericArgs) {
                if (i > 0) print(", ");
                this.printType(t);
                ++i;
            }
            print(" >");
        }
        else if (v instanceof IfExpr e) {
            this.visit(e.condition);
            print("?");
            this.visit(e.trueExpr);
            print(":");
            this.visit(e.falseExpr);
        }
        else if (v instanceof Expr.WithBlockExpr e) {
            printWithBlockExpr(e);
        }
        else if (v instanceof Expr.ArrayBlockExpr e) {
            printArrayBlockExpr(e);
        }
        else if (v instanceof ClosureExpr e) {
            printClosureExpr(e);
        }
        else if (v instanceof NonNullableExpr e) {
            print("nonNullable(");
            this.visit(e.operand);
            print(")");
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
        
        while (parentheses > 0) {
            print(")");
            --parentheses;
        }
    }

    private void printBinaryExpr(BinaryExpr e) {
        if (e.opToken == TokenKind.asKeyword) {
            Type targetType = ((TypeExpr)e.rhs).type;
            boolean processed = false;
            if (targetType.isPointerType()) {
                if (targetType.detail instanceof Type.PointerInfo pinfo) {
                    
                    if (!pinfo.isNullable) {
                        print("nonNullable(");
                    }
                    
                    if (pinfo.pointerAttr != Type.PointerAttr.raw && targetType.genericArgs != null) {
                        this.visit(e.lhs);
                        print(".dynamicCastTo<");
                        printType(targetType.genericArgs.get(0));
                        print(" >()");
                        processed = true;
                    }
                    else {
                        print("dynamic_cast<");
                        printType(targetType);
                        print(" >(");
                        this.visit(e.lhs);
                        print(")");
                        processed = true;
                    }
                    
                    if (!pinfo.isNullable) {
                        print(")");
                    }
                }
            }
            if (!processed) {
                print("(");
                printType(targetType);
                print(")(");
                this.visit(e.lhs);
                print(")");
            }
        }
        else if (e.opToken == TokenKind.isKeyword) {
            Type targetType = ((TypeExpr)e.rhs).type;
            if (targetType.isPointerType()) {
                if (targetType.genericArgs != null) {
                    print("sric::ptrIs<");
                    printType(targetType.genericArgs.get(0));
                    print(" >(");
                    this.visit(e.lhs);
                    print(")");
                }
            }
            else {
                print(e.lhs.resolvedType.equals(targetType) ? "true" : "false");
            }
        }
        //index set operator: a[i] = b
        else if (e.opToken == TokenKind.assign && e.lhs instanceof IndexExpr iexpr) {
            
            if (iexpr.resolvedOperator != null) {
                this.visit(iexpr.target);
                print(".set(");
                this.visit(iexpr.index);
                print(", ");
                this.visit(e.rhs);
                print(")");
            }
            else {
                this.visit(iexpr.target);
                print("[");
                this.visit(iexpr.index);
                print("] = ");
                this.visit(e.rhs);
            }
        }
        else {
            if (e.resolvedOperator !=  null) {
                this.visit(e.lhs);
                print(".").print(e.resolvedOperator.name).print("(");
                this.visit(e.rhs);
                print(")");
                switch (e.opToken) {
                    case eq:
                        print(" == 0");
                        break;
                    case notEq:
                        print(" != 0");
                        break;
                    case lt:
                        print(" < 0");
                        break;
                    case gt:
                        print(" > 0");
                        break;
                    case ltEq:
                        print(" <= 0");
                        break;
                    case gtEq:
                        print(" >= 0");
                        break;
                    default:
                }
            }
            else {
                this.visit(e.lhs);
                print(" ");
                print(e.opToken.symbol);
                print(" ");
                this.visit(e.rhs);
            }
        }
    }
    
    void printLiteral(LiteralExpr e) {
        if (e.value == null) {
            if (e.nullPtrType != null && e.nullPtrType.detail instanceof PointerInfo pinfo) {
                if (pinfo.pointerAttr == Type.PointerAttr.own) {
                    print("sric::OwnPtr<");
                    printType(e.nullPtrType.genericArgs.get(0));
                    print(">()");
                }
                else if (pinfo.pointerAttr == Type.PointerAttr.ref) {
                    print("sric::RefPtr<");
                    printType(e.nullPtrType.genericArgs.get(0));
                    print(">()");
                }
            }
            else {
                print("nullptr");
            }
        }
        else if (e.value instanceof Long li) {
            print(li.toString());
        }
        else if (e.value instanceof Double li) {
            print(li.toString());
        }
        else if (e.value instanceof Boolean li) {
            print(li.toString());
        }
        else if (e.value instanceof String li) {
            printStringLiteral(li);
        }
    }
    
    void printStringLiteral(String li) {
        print("\"");
        for (int i=0; i<li.length(); ++i) {
            char c = li.charAt(i);
            printChar(c);
        }
        print("\"");
    }

    void printChar(char c) {
        switch (c) {
            case '\b':
                print("\\b");
                break;
            case '\n':
                print("\\n");
                break;
            case '\r':
                print("\\r");
                break;
            case '\t':
                print("\\t");
                break;
            case '\"':
                print("\\\"");
                break;
            case '\'':
                print("\\\'");
                break;
            case '\\':
                writer.print('\\');
                break;
            default:
                writer.print(c);
        }
    }
    
    private void printItBlockArgs(WithBlockExpr e, String varName) {
        if (e.block != null) {
            String savedName = curItName;
            curItName = varName;
            
            this.visit(e.block);
            
            curItName = savedName;
        }
    }
    
    void printWithBlockExpr(WithBlockExpr e) {
//        if (!e.target.isResolved()) {
//            return;
//        }
        
        String targetId = null;
        if (e.target instanceof Expr.IdExpr id) {
            if (id.namespace == null) {
                targetId = id.name;
            }
        }

        if (e._storeVar != null && e._storeVar.isLocalVar) {
            if (!e._isType) {
                print(" = ");
                this.visit(e.target);
            }
            else if (e.block.stmts.size() == 0) {
                print(" = {}");
                return;
            }
            print(";");
            
            printItBlockArgs(e, e._storeVar.name);
        }
        else if (targetId != null) {
            if (e._isType) {
                this.visit(e.target);
                print("();");
            }
            printItBlockArgs(e, targetId);
        }
        else if (e.target.isResolved()) {
            if (e._storeVar != null) {
                if (e.block.stmts.size() == 0) {
                    print(" = {}");
                    return;
                }
                print(" = ");
            }
            //[&]()->T{ T __t = alloc(); __t.name =1; return __t; }()
            print("[&]()->");
            printType(e.resolvedType);
            print("{");
            
            printType(e.resolvedType);
            print(" __t = ");
            this.visit(e.target);
            if (e._isType) {
                print("()");
            }
            print(";");
            
            printItBlockArgs(e, "__t");
            
            print("return __t;");
            
            print("}()");
        }
    }
    
    void printArrayBlockExpr(ArrayBlockExpr e) {
        if (e._storeVar != null) {
            print(";");
        }
        int i = 0;
        print("{");
        if (e.args != null) {
            for (Expr t : e.args) {
                print(e._storeVar.name);
                print("[");
                print(""+i);
                print("] = ");
                this.visit(t);
                print("; ");
                ++i;
            }
        }
        print("}");
    }
    
    void printClosureExpr(ClosureExpr expr) {
        print("[=");
        
//        int i = 0;
//        if (expr.defaultCapture != null) {
//            print(expr.defaultCapture.symbol);
//            ++i;
//        }
//        
//        for (Expr t : expr.captures) {
//            if (i > 0) print(", ");
//            this.visit(t);
//            ++i;
//        }
        print("]");
        
        this.printFuncPrototype(expr.prototype, true, false);
        
        this.visit(expr.code);
    }
}
