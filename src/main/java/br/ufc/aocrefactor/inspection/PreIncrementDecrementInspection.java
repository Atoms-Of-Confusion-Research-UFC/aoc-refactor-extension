package br.ufc.aocrefactor.inspection;

import br.ufc.aocrefactor.quickfix.ReplacePreIncrementQuickFix;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class PreIncrementDecrementInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitPrefixExpression(@NotNull PsiPrefixExpression expression) {
                super.visitPrefixExpression(expression);

                IElementType op = expression.getOperationTokenType();
                if (!isPreIncrementOrDecrement(op)) return;

                PsiElement parent = expression.getParent();
                if (!isValidContext(parent)) return;

                String opName = op == JavaTokenType.PLUSPLUS
                        ? "pre-increment (++)" : "pre-decrement (--)";

                // Registra o problema no PAI para highlight cobrir a expressão inteira
                PsiElement highlightTarget = getHighlightTarget(parent, expression);

                holder.registerProblem(
                        highlightTarget,
                        "Confusion atom: " + opName + " operator may cause confusion. Consider separating the operation.",
                        ProblemHighlightType.WARNING,
                        new ReplacePreIncrementQuickFix()
                );
            }
        };
    }

    private boolean isPreIncrementOrDecrement(IElementType op) {
        return op == JavaTokenType.PLUSPLUS || op == JavaTokenType.MINUSMINUS;
    }

    private boolean isValidContext(PsiElement parent) {
        return parent instanceof PsiAssignmentExpression
                || parent instanceof PsiLocalVariable
                || parent instanceof PsiBinaryExpression
                || parent instanceof PsiPolyadicExpression
                || parent instanceof PsiExpressionList
                || parent instanceof PsiArrayAccessExpression
                || parent instanceof PsiReturnStatement;
    }

    private PsiElement getHighlightTarget(PsiElement parent, PsiPrefixExpression expression) {
        if (parent instanceof PsiAssignmentExpression assignment) {
            return assignment;
        }
        if (parent instanceof PsiLocalVariable localVar) {
            // Sobe para a LocalVariable inteira: "int b = ++a"
            return localVar;
        }
        if (parent instanceof PsiBinaryExpression binary) {
            return binary;
        }
        if (parent instanceof PsiPolyadicExpression polyadic) {
            return polyadic;
        }
        return expression;
    }
}