package org.shimoon.lomboky;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import lombok.AccessLevel;
import lombok.ConfigurationKeys;
import lombok.core.AnnotationValues;
import lombok.core.configuration.CheckerFrameworkVersion;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;

import static lombok.core.handlers.HandlerUtil.handleFlagUsage;
import static lombok.javac.handlers.HandleConstructor.checkLegality;
import static lombok.javac.handlers.JavacHandlerUtil.*;

@AutoService(JavacAnnotationHandler.class)
public class HandleStaticConstructor extends JavacAnnotationHandler<StaticConstructor> {
    @Override
    public void handle(AnnotationValues<StaticConstructor> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        System.out.println("THIS IS LOMBOK!!!!!");
        handleFlagUsage(annotationNode, ConfigurationKeys.REQUIRED_ARGS_CONSTRUCTOR_FLAG_USAGE, "@RequiredArgsConstructor", ConfigurationKeys.ANY_CONSTRUCTOR_FLAG_USAGE, "any @xArgsConstructor");

        deleteAnnotationIfNeccessary(annotationNode, StaticConstructor.class);
        deleteImportFromCompilationUnit(annotationNode, "lombok.AccessLevel");
        JavacNode typeNode = annotationNode.up();
        if (!checkLegality(typeNode, annotationNode, StaticConstructor.class.getSimpleName())) return;
        generateStaticConstructor(true, typeNode, "of", AccessLevel.PUBLIC, false, List.nil(), annotationNode);
    }


    private void generateStaticConstructor(boolean staticConstrRequired, JavacNode typeNode, String staticName, AccessLevel level, boolean allToDefault, List<JavacNode> fields, JavacNode source) {
        if (staticConstrRequired) {
            JCTree.JCMethodDecl staticConstr = createStaticConstructor(staticName, level, typeNode, allToDefault ? List.<JavacNode>nil() : fields, source);
            injectMethod(typeNode, staticConstr);
        }
    }

    public JCTree.JCMethodDecl createStaticConstructor(String name, AccessLevel level, JavacNode typeNode, List<JavacNode> fields, JavacNode source) {
        JavacTreeMaker maker = typeNode.getTreeMaker();
        JCTree.JCClassDecl type = (JCTree.JCClassDecl) typeNode.get();

        JCTree.JCModifiers mods = maker.Modifiers(com.sun.tools.javac.code.Flags.STATIC | toJavacModifier(level));

        JCTree.JCExpression returnType, constructorType;

        ListBuffer<JCTree.JCTypeParameter> typeParams = new ListBuffer<JCTree.JCTypeParameter>();
        ListBuffer<JCTree.JCVariableDecl> params = new ListBuffer<JCTree.JCVariableDecl>();
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<JCTree.JCExpression>();

        if (!type.typarams.isEmpty()) {
            for (JCTree.JCTypeParameter param : type.typarams) {
                typeParams.append(maker.TypeParameter(param.name, param.bounds));
            }
        }
        List<JCTree.JCAnnotation> annsOnReturnType = List.nil();
        if (getCheckerFrameworkVersion(typeNode).generateUnique()) annsOnReturnType = List.of(maker.Annotation(genTypeRef(typeNode, CheckerFrameworkVersion.NAME__UNIQUE), List.<JCTree.JCExpression>nil()));
        returnType = namePlusTypeParamsToTypeReference(maker, typeNode, type.typarams, annsOnReturnType);
        constructorType = namePlusTypeParamsToTypeReference(maker, typeNode, type.typarams);

        for (JavacNode fieldNode : fields) {
            JCTree.JCVariableDecl field = (JCTree.JCVariableDecl) fieldNode.get();
            Name fieldName = removePrefixFromField(fieldNode);
            JCTree.JCExpression pType = cloneType(maker, field.vartype, source);
            List<JCTree.JCAnnotation> copyableAnnotations = findCopyableAnnotations(fieldNode);
            long flags = JavacHandlerUtil.addFinalIfNeeded(Flags.PARAMETER, typeNode.getContext());
            JCTree.JCVariableDecl param = maker.VarDef(maker.Modifiers(flags, copyableAnnotations), fieldName, pType, null);
            params.append(param);
            args.append(maker.Ident(fieldName));
        }
        JCTree.JCReturn returnStatement = maker.Return(maker.NewClass(null, List.<JCTree.JCExpression>nil(), constructorType, args.toList(), null));
        JCTree.JCBlock body = maker.Block(0, List.<JCTree.JCStatement>of(returnStatement));

        JCTree.JCMethodDecl methodDef = maker.MethodDef(mods, typeNode.toName(name), returnType, typeParams.toList(), params.toList(), List.<JCTree.JCExpression>nil(), body, null);
        createRelevantNonNullAnnotation(typeNode, methodDef);
        return recursiveSetGeneratedBy(methodDef, source);
    }
}
