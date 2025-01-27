//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Loc;
import sric.compiler.ast.Stmt;

/**
 *
 * @author yangjiandong
 */
public abstract class CompilePass implements Visitor {
    protected CompilerLog log;
    
    public CompilePass(CompilerLog log) {
        this.log = log;
    }
    
    protected CompilerLog.CompilerErr err(String msg, Loc loc) {
        if (log == null) {
            System.err.println(msg + " at " + loc.toString());
        }
        return log.err(msg, loc);
    }
    
    public void visitUnit(FileUnit v) {
    }
    
    public void visitField(FieldDef v) {
    }
    
    public void visitFunc(FuncDef v) {
    }
    
    public void visitTypeDef(TypeDef v) {
        v.walkChildren(this);
    }
    
    public void visitTypeAlias(TypeAlias v) {
    }
    
    public void visitStmt(Stmt v) {
    }
    
    public void visitExpr(Expr v) {
    }

    @Override
    public void visit(AstNode node) {
        if (node instanceof FileUnit v) {
            this.visitUnit(v);
        }
        else if (node instanceof TypeDef v) {
            this.visitTypeDef(v);
        }
        else if (node instanceof FieldDef v) {
            this.visitField(v);
        }
        else if (node instanceof FuncDef v) {
            this.visitFunc(v);
        }
        else if (node instanceof Stmt v) {
            this.visitStmt(v);
        }
        else if (node instanceof Expr v) {
            this.visitExpr(v);
        }
        else if (node instanceof TypeAlias v) {
            this.visitTypeAlias(v);
        }
        else {
            err("Unknow AstNode type:" + node.getClass(), node.loc);
        }
    }
}
