//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import sric.compiler.ast.AstNode.TypeDef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sric.compiler.ast.Expr.ClosureExpr;
import sric.compiler.ast.Expr.IdExpr;

/**
 *
 * @author yangjiandong
 */
public class Type extends AstNode {
    public IdExpr id;
    public ArrayList<Type> genericArgs = null;
    public Type resolvedAlias = null;
    
    public static enum PointerAttr {
        own, ref, raw
    };
    
    public boolean explicitImmutable = false;
    public boolean isImmutable = false;
    
    public TypeInfo detail = null;
    
    public static abstract class TypeInfo {
    }
    
    public static class FuncInfo extends TypeInfo {
        public FuncPrototype prototype;
        public FuncDef funcDef = null;
    }
    
    public static class PointerInfo extends TypeInfo {
        public PointerAttr pointerAttr = PointerAttr.ref;
        //** Is this is a nullable type (marked with trailing ?)
        public boolean isNullable = false;
    }
    
    public static class MetaTypeInfo extends TypeInfo {
        public Type type;
    }
    
    public static class ArrayInfo extends TypeInfo {
        public Expr sizeExpr;
        //public int size;
    }
    
    public static class NumInfo extends TypeInfo {
        //** primitive type sized. the Int32 size is 32
        public int size = 0;

        //unsigned int
        public boolean isUnsigned = false;
    }
    
    public Type(IdExpr id) {
        this.id = id;
        this.loc = id.loc;
    }
    
    public Type(Loc loc, String name) {
        this.loc = loc;
        this.id = new IdExpr(name);
        this.id.loc = loc;
    }
    
    public boolean isVoid() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Void");
    }
    
    public boolean isBool() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Bool");
    }
    
    public boolean isInt() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Int");
    }
    
    public boolean isNum() {
        return isInt() || isFloat();
    }
    
    public boolean isFloat() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Float");
    }
    
    public boolean isArray() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.arrayTypeName);
    }
    
    public boolean isMetaType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.metaTypeTypeName);
    }
    
    public boolean isPointerType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.pointerTypeName);
    }
    
    public boolean isRefableType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.refableTypeName);
    }
    
    public boolean isNullablePointerType() {
        if (!isPointerType()) {
            return false;
        }
        if (this.detail instanceof PointerInfo pinfo) {
            return pinfo.isNullable;
        }
        return true;
    }
    
    public boolean isRawPointerType() {
        if (!isPointerType()) {
            return false;
        }
        if (this.detail instanceof PointerInfo pinfo) {
            return pinfo.pointerAttr == PointerAttr.raw;
        }
        return false;
    }
    
    public boolean isNullType() {
        if (isPointerType()) {
            if (this.genericArgs == null) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isFuncType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.funcTypeName);
    }
    
    public boolean isVarArgType() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals(Buildin.varargTypeName);
    }
    
    public boolean fit(Type target) {
        if (this.resolvedAlias != null) {
            if (this.resolvedAlias.fit(target)) {
                return true;
            }
        }
        if (target.resolvedAlias != null) {
            if (this.fit(target.resolvedAlias)) {
                return true;
            }
        }
        
        if (target.isVarArgType()) {
            return true;
        }

        if (equals(target)) {
            return true;
        }
        if (this.isNullType() && target.detail instanceof PointerInfo a) {
            if (a.isNullable) {
                return true;
            }
            return false;
        }
        
        if (this.isRefableType() && this.genericArgs != null) {
            return this.genericArgs.get(0).fit(target);
        }
        else if (target.isRefableType() && target.genericArgs != null) {
            return this.fit(target.genericArgs.get(0));
        }
        
        //pointer fit
        if (this.detail instanceof PointerInfo e && target.detail instanceof PointerInfo a) {

            if (e.isNullable && !a.isNullable) {
                //error to nonnullable
                return false;
            }

            if ((e.pointerAttr != a.pointerAttr)) {
                if (e.pointerAttr == PointerAttr.own && (a.pointerAttr == PointerAttr.ref || a.pointerAttr == PointerAttr.raw)) {
                    //ok
                }
                else if (e.pointerAttr == PointerAttr.ref && (a.pointerAttr == PointerAttr.raw)) {
                    //ok
                }
                else {
                    return false;
                }
            }
            
            if (target.genericArgs != null && target.genericArgs.get(0).isVoid()) {
                //ok
            }
            else if (!genericArgsFit(target)) {
                return false;
            }
            

            if (this.id.resolvedDef == target.id.resolvedDef) {
                return true;
            }

            if (this.id.resolvedDef != null && target.id.resolvedDef != null) {
                if (this.id.resolvedDef instanceof StructDef sd && target.id.resolvedDef instanceof StructDef td) {
                    if (sd.originGenericTemplate == td.originGenericTemplate || sd == td.originGenericTemplate || sd.originGenericTemplate == td) {
                        return true;
                    }

                    if (sd.isInheriteFrom(td)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public boolean equals(Type target) {
        if (this == target) {
            return true;
        }
        
        if (this.resolvedAlias != null) {
            if (this.resolvedAlias.equals(target)) {
                return true;
            }
        }
        if (target.resolvedAlias != null) {
            if (this.equals(target.resolvedAlias)) {
                return true;
            }
        }
        
        if (!genericArgsEquals(target)) {
            return false;
        }
        
        if (this.isPointerType()) {
            if (this.detail instanceof PointerInfo e && target.detail instanceof PointerInfo a) {
                if ( (e.pointerAttr != a.pointerAttr) || (e.isNullable != a.isNullable))  {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else if (this.isArray()) {
            if (this.detail instanceof ArrayInfo e && target.detail instanceof ArrayInfo a) {
                if (e.sizeExpr != a.sizeExpr)  {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else if (this.isNum()) {
            if (this.detail instanceof NumInfo e && target.detail instanceof NumInfo a) {
                if ( (e.isUnsigned != a.isUnsigned))  {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        if (this.id.resolvedDef == target.id.resolvedDef) {
            return true;
        }
        
        if (this.id.resolvedDef != null && target.id.resolvedDef != null) {
            if (this.id.resolvedDef instanceof StructDef sd && target.id.resolvedDef instanceof StructDef td) {
                if (sd.originGenericTemplate == td.originGenericTemplate) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected boolean genericArgsEquals(Type target) {
        if (this.genericArgs != null || target.genericArgs != null) {
            if (this.genericArgs == null || target.genericArgs == null) {
                return false;
            }
            if (this.genericArgs.size() != target.genericArgs.size()) {
                return false;
            }
            for (int i=0; i<this.genericArgs.size(); ++i) {
                if (!this.genericArgs.get(i).equals(target.genericArgs.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean genericArgsFit(Type target) {
        if (this.genericArgs != null || target.genericArgs != null) {
            if (this.genericArgs == null || target.genericArgs == null) {
                return false;
            }
            if (this.genericArgs.size() != target.genericArgs.size()) {
                return false;
            }
            for (int i=0; i<this.genericArgs.size(); ++i) {
                Type from = this.genericArgs.get(i);
                Type to = target.genericArgs.get(i);
                boolean ok = false;
                if (from.fit(to)) {
                    ok = true;
                }
                
                if (from.isImmutable && !to.isImmutable) {
                    return false;
                }
                
                if (from.id.resolvedDef instanceof StructDef sd && to.id.resolvedDef instanceof TypeDef ttd) {
                    if (ttd instanceof StructDef td) {
                        if (sd.originGenericTemplate == td.originGenericTemplate || sd == td.originGenericTemplate || sd.originGenericTemplate == td) {
                            ok = true;
                        }
                    }

                    if (sd.isInheriteFrom(ttd)) {
                        ok = true;
                    }
                }
                if (!ok) {
                    return false;
                }
            }
        }
        return true;
    }
        
    public static Type funcType(Loc loc, FuncPrototype prototype) {
        Type type = new Type(loc, Buildin.funcTypeName);
        FuncInfo info = new FuncInfo();
        info.prototype = prototype;
        type.detail = info;
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(prototype.returnType);
        if (prototype.paramDefs != null) {
            for (ParamDef p : prototype.paramDefs) {
                type.genericArgs.add(p.paramType);
            }
        }
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type funcType(FuncDef f) {
        Type type = funcType(f.loc, f.prototype);
        ((FuncInfo)type.detail).funcDef = f;
        return type;
    }
    
    public static Type funcType(ClosureExpr f) {
        Type type = funcType(f.loc, f.prototype);
        return type;
    }
    
    public static Type voidType(Loc loc) {
        Type type = new Type(loc, "Void");
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type boolType(Loc loc) {
        Type type = new Type(loc, "Bool");
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type intType(Loc loc) {
        Type type = new Type(loc, "Int");
        NumInfo info = new NumInfo();
        info.size = 32;
        type.detail = info;
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type floatType(Loc loc) {
        Type type = new Type(loc, "Float");
        NumInfo info = new NumInfo();
        info.size = 64;
        type.detail = info;
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type strType(Loc loc) {
        Type type = intType(loc);
        ((NumInfo)type.detail).size = 8;
        type.isImmutable = true;
        return pointerType(loc, type, PointerAttr.raw, false);
    }
    
    public static Type nullType(Loc loc) {
        Type type = new Type(loc, Buildin.pointerTypeName);
        type.isImmutable = true;
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }

    public static Type arrayType(Loc loc, Type elemType, Expr size) {
        Type type = new Type(loc, Buildin.arrayTypeName);
        ArrayInfo info = new ArrayInfo();
        info.sizeExpr = size;
        type.detail = info;
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(elemType);
        
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type pointerType(Loc loc, Type elemType, PointerAttr pointerAttr, boolean nullable) {
        Type type = new Type(loc, Buildin.pointerTypeName);
        if (elemType != null) {
            type.genericArgs = new ArrayList<>();
            type.genericArgs.add(elemType);
        }
        PointerInfo info = new PointerInfo();
        info.pointerAttr = pointerAttr;
        info.isNullable = nullable;
        type.detail = info;
        
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type refableType(Loc loc, Type elemType) {
        Type type = new Type(loc, Buildin.refableTypeName);
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(elemType);
        
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type varArgType(Loc loc) {
        Type type = new Type(loc, Buildin.varargTypeName);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type metaType(Loc loc, Type type) {
        Type t = new Type(loc, Buildin.metaTypeTypeName);
        MetaTypeInfo info = new MetaTypeInfo();
        info.type = type;
        t.detail = info;
        if (type.id.resolvedDef == null && type.id.namespace == null) {
            type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        }
        return t;
    }
    
    @java.lang.Override
    public String toString() {
                
        if (this.isVarArgType()) {
            return Buildin.varargTypeName;
        }
        
        StringBuilder sb = new StringBuilder();

        
        if (this.isImmutable) {
            sb.append("const ");
        }
        else {
            sb.append("mut ");
        }
        
        if (isArray()) {
            ArrayInfo info = (ArrayInfo)this.detail;
            sb.append("[");
            if (info.sizeExpr != null) {
                sb.append(info.sizeExpr);
            }
            sb.append("]");
            sb.append(this.genericArgs.get(0).toString());
            return sb.toString();
        }
        else if (isNum()) {
            NumInfo info = (NumInfo)this.detail;
            if (info.isUnsigned) {
                sb.append("U");
            }

            sb.append(id.toString());

            if (info.size != 0) {
                sb.append(info.size);
            }
            return sb.toString();
        }
        else if (isPointerType()) {
            if (this.isNullType()) {
                sb.append("null");
            }
            else {
                PointerInfo info = (PointerInfo)this.detail;
                sb.append(info.pointerAttr).append("*");
                if (info.isNullable) sb.append("?");
                sb.append(" ");
                sb.append(this.genericArgs.get(0).toString());
            }
            return sb.toString();
        }
        else if (isFuncType()) {
            sb.append("fun");
            sb.append(((FuncInfo)this.detail).prototype.toString());
            return sb.toString();
        }
        else if (isMetaType()) {
            sb.append(((MetaTypeInfo)this.detail).type.toString());
            return sb.toString();
        }
        
        sb.append(id.toString());
        
        if (this.genericArgs != null) {
            sb.append("$<");
            int i = 0;
            for (Type t : this.genericArgs) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(t.toString());
                ++i;
            }
            sb.append(">");
        }
        
        return sb.toString();
    }

    public Type templateInstantiate(Map<GenericParamDef, Type> typeGenericArgs) {
        if (!(this.id.resolvedDef instanceof GenericParamDef g) && this.genericArgs == null) {
            return this;
        }
        
        Type nt = new Type(this.id);
        if (this.genericArgs != null) {
            nt.genericArgs = new ArrayList<Type>();
            for (int i=0; i<this.genericArgs.size(); ++i) {
                Type t = this.genericArgs.get(i).templateInstantiate(typeGenericArgs);
                nt.genericArgs.add(t);
            }
        }
        nt.detail = this.detail;
        if (this.id.resolvedDef instanceof GenericParamDef g) {
            if (typeGenericArgs.containsKey(g)) {
                Type at = typeGenericArgs.get(g);
                if (at != null) {
                    nt.id = at.id;
                    nt.detail = at.detail;
                    nt.genericArgs = at.genericArgs;
                    if (at.isImmutable) {
                        nt.isImmutable = true;
                    }
                }
            }
        }
        else if (this.id.resolvedDef instanceof StructDef sd) {
            this.id.resolvedDef = sd.makeInstance(typeGenericArgs);
        }
        return nt;
    }
    
    public Type toNonNullable() {
        if (!this.isPointerType()) {
            return this;
        }
        if (!((PointerInfo)this.detail).isNullable) {
            return this;
        }
        
        Type type = new Type(loc, "*");
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(this.genericArgs.get(0));
        PointerInfo info = new PointerInfo();
        info.pointerAttr = ((PointerInfo)this.detail).pointerAttr;
        info.isNullable = false;
        type.detail = info;
        
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public Type toImmutable() {
        if (this.isImmutable) {
            return this;
        }
        
        //shadow copy
        Type type = new Type(this.id);
        type.genericArgs = this.genericArgs;
        type.resolvedAlias = this.resolvedAlias;
        type.explicitImmutable = this.explicitImmutable;
        type.isImmutable = true;
        type.detail = this.detail;
        return type;
    }
}
