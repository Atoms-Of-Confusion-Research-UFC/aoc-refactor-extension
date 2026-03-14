package br.ufc.aocrefactor.inspection;

import br.ufc.aocrefactor.quickfix.ReplaceConditionalOperatorQuickFix;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class ConditionalOperatorInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitConditionalExpression(
                    @NotNull PsiConditionalExpression expression) {
                super.visitConditionalExpression(expression);

                holder.registerProblem(
                        expression,
                        "Átomo de confusão: operador condicional (?:). Considere usar if-else.",
                        ProblemHighlightType.WARNING,
                        new ReplaceConditionalOperatorQuickFix()
                );
            }
        };
    }
}