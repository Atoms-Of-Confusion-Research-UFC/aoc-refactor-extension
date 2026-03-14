package br.ufc.aocrefactor.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class ReplaceConditionalOperatorQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getName() {
        return "Substituir por if-else";
    }

    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {

        PsiConditionalExpression ternary =
                (PsiConditionalExpression) descriptor.getPsiElement();

        PsiExpression condition = ternary.getCondition();
        PsiExpression thenExpr  = ternary.getThenExpression();
        PsiExpression elseExpr  = ternary.getElseExpression();

        if (thenExpr == null || elseExpr == null) return;

        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(ternary, PsiStatement.class);
        if (parentStatement == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Caso 1: int x = cond ? a : b;
        if (parentStatement instanceof PsiDeclarationStatement declStmt) {
            PsiLocalVariable var = (PsiLocalVariable)
                    declStmt.getDeclaredElements()[0];

            String varName = var.getName();
            String varType = var.getType().getPresentableText();

            PsiStatement declaration = factory.createStatementFromText(
                    varType + " " + varName + ";", null);

            String ifElseText = String.format(
                    "if (%s) { %s = %s; } else { %s = %s; }",
                    condition.getText(),
                    varName, thenExpr.getText(),
                    varName, elseExpr.getText()
            );
            PsiStatement ifElse = factory.createStatementFromText(ifElseText, null);

            PsiElement parent = parentStatement.getParent();
            parent.addBefore(declaration, parentStatement);
            parent.addBefore(ifElse, parentStatement);
            parentStatement.delete();

            // Caso 2: return cond ? a : b;
        } else if (parentStatement instanceof PsiReturnStatement) {
            String ifElseText = String.format(
                    "if (%s) { return %s; } else { return %s; }",
                    condition.getText(),
                    thenExpr.getText(),
                    elseExpr.getText()
            );
            PsiStatement ifElse = factory.createStatementFromText(ifElseText, null);
            parentStatement.replace(ifElse);

            // Caso 3: genérico — ternário dentro de expressão (ex: println(cond ? a : b))
        } else {
            PsiType type = ternary.getType();
            String typeName = (type != null) ? type.getPresentableText() : "Object";

            String tempVar = "_temp";

            PsiStatement tempDecl = factory.createStatementFromText(
                    typeName + " " + tempVar + ";", null);

            String ifElseText = String.format(
                    "if (%s) { %s = %s; } else { %s = %s; }",
                    condition.getText(),
                    tempVar, thenExpr.getText(),
                    tempVar, elseExpr.getText()
            );
            PsiStatement ifElse = factory.createStatementFromText(ifElseText, null);

            PsiExpression tempVarExpr = factory.createExpressionFromText(tempVar, null);
            ternary.replace(tempVarExpr);

            PsiElement parent = parentStatement.getParent();
            parent.addBefore(tempDecl, parentStatement);
            parent.addBefore(ifElse, parentStatement);
        }
    }
}