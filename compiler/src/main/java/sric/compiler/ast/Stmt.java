//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import sric.compiler.ast.AstNode.Block;
import sric.compiler.ast.AstNode.FieldDef;
import java.util.ArrayList;
import sric.compiler.ast.Token.TokenKind;

/**
 *
 * @author yangjiandong
 */
public abstract class Stmt extends AstNode {
    public static class NoOp extends Stmt {
        
    }
    public static class ExprStmt extends Stmt {
        public Expr expr;
    }
    public static class IfStmt extends Stmt {
        public Expr condition;      // test expression
        public Block block;     // block to execute if condition true
        public Block elseBlock;   // else clause or null
        
        public boolean allPathReturnValue() {
            if (block == null || elseBlock == null) {
                return false;
            }
            if (block.isLastReturnValue() && elseBlock.isLastReturnValue()) {
                return true;
            }
            return false;
        }
    }
    public static class WhileStmt extends Stmt {
        public Expr condition;     // loop condition
        public Block block;        // code to run inside loop
    }

    public static class ReturnStmt extends Stmt {
        public Expr expr;           // expr to return of null if void return
        //public boolean isLocal = false;
        public Type _funcReturnType;
        public boolean _isCoroutineRet = false;
    }
    public static class ThrowStmt extends Stmt {
        public Expr expr;
    }
    public static class ForStmt extends Stmt {
        public Stmt init;        // loop initialization
        public Expr condition;   // loop condition
        public Expr update;      // loop update
        public Block block;      // code to run inside loop
    }
    public static class CaseBlock extends AstNode {
        public boolean fallthrough = false;
        public Expr caseExpr;   // list of case target (literal expressions)
        public Block block;     // code to run for case
    }
    public static class SwitchStmt extends Stmt {
        public Expr condition;        // test expression
        public ArrayList<CaseBlock> cases = new ArrayList<CaseBlock>();          // list of case blocks
        public Block defaultBlock;
    }
    //break, continue
    public static class JumpStmt extends Stmt {
        public TokenKind opToken;
        public Stmt target;   // loop to continue
    }
    public static class TryStmt extends Stmt {
        public Block block;         // body of try block
        public ArrayList<Catch> catches;      // list of catch clauses
        public Block finallyBlock;  // body of finally block or null
    }
    public static class Catch extends AstNode {
        public FieldDef errVariable;// name of err local variable
        public Block block;         // body of catch block
    }
    public static class UnsafeBlock extends Stmt {
        public Block block;
    }
    public static class LocalDefStmt extends Stmt {
        public FieldDef fieldDef;
        public LocalDefStmt(FieldDef f) {
            fieldDef = f;
            this.loc = f.loc;
        }
    }
}
